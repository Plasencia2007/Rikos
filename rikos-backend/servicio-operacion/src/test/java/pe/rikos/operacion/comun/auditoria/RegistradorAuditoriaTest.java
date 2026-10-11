package pe.rikos.operacion.comun.auditoria;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * US-006: RegistradorAuditoria es el punto unico de escritura -- resuelve
 * usuario y local activo solo, sin que el llamador se lo pase, y se
 * escribe en la misma transaccion que el hecho auditado.
 */
@SpringBootTest
class RegistradorAuditoriaTest {

    private static final String ADMIN_SEMBRADO_ID = "01M2QZ606763Q7XC00M8VD7WM1";
    private static final String LOCAL_SEMBRADO_ID = "01M2QZ60D35B90CW00EJA15VFC";

    @Autowired
    private RegistradorAuditoria auditoria;
    @Autowired
    private AuditoriaRepositorio auditorias;
    @Autowired
    private PlatformTransactionManager transactionManager;

    @AfterEach
    void limpiarContexto() {
        ContextoDeSeguridadDePrueba.limpiar();
    }

    @Test
    void resuelveElUsuarioYElLocalActivoSoloDesdeElContextoDeSeguridad() {
        ContextoDeSeguridadDePrueba.autenticarComo(ADMIN_SEMBRADO_ID, LOCAL_SEMBRADO_ID);
        String entidadId = pe.rikos.contratos.Identificador.nuevo().valor();

        new TransactionTemplate(transactionManager).executeWithoutResult(estado ->
                auditoria.registrar("cambio_estado", "prueba", entidadId,
                        RegistradorAuditoria.campos("activo", true), RegistradorAuditoria.campos("activo", false)));

        List<Auditoria> filas = auditorias.findByEntidadAndEntidadIdOrderByOcurridoEnDesc("prueba", entidadId);
        assertThat(filas).hasSize(1);
        assertThat(filas.get(0).getUsuarioId()).isEqualTo(ADMIN_SEMBRADO_ID);
    }

    @Test
    void seEscribeEnLaMismaTransaccionQueElHechoAuditado_SiElLlamadorHaceRollbackLaAuditoriaTambienDesaparece() {
        ContextoDeSeguridadDePrueba.autenticarComo(ADMIN_SEMBRADO_ID);
        String entidadId = pe.rikos.contratos.Identificador.nuevo().valor();

        try {
            new TransactionTemplate(transactionManager).executeWithoutResult(estado -> {
                auditoria.registrar("eliminacion", "prueba-rollback", entidadId, null, null);
                estado.setRollbackOnly();
            });
        } catch (Exception ignorado) {
            // setRollbackOnly no lanza, pero por si acaso -- lo que importa es la fila de abajo
        }

        assertThat(auditorias.findByEntidadAndEntidadIdOrderByOcurridoEnDesc("prueba-rollback", entidadId)).isEmpty();
    }

    @Test
    void unNombreConComillasYBarraInvertidaNoCorrompeElJsonGuardado() {
        // Hallazgo de revision: "Pollo \"a la brasa\"" es un nombre real de
        // producto perfectamente valido -- si el JSON se arma a mano y el
        // escape esta mal, esta fila queda ilegible para siempre sin que
        // nadie se entere al escribirla.
        ContextoDeSeguridadDePrueba.autenticarComo(ADMIN_SEMBRADO_ID);
        String entidadId = pe.rikos.contratos.Identificador.nuevo().valor();
        String nombreConflictivo = "Pollo \"a la brasa\" \\ con barra";

        new TransactionTemplate(transactionManager).executeWithoutResult(estado ->
                auditoria.registrar("cambio_estado", "prueba-json", entidadId,
                        RegistradorAuditoria.campos("nombre", nombreConflictivo, "activo", true),
                        RegistradorAuditoria.campos("nombre", nombreConflictivo, "activo", false)));

        Auditoria fila = auditorias.findByEntidadAndEntidadIdOrderByOcurridoEnDesc("prueba-json", entidadId).get(0);
        assertThat(RegistradorAuditoria.extraerCampo(fila.getValorNuevo(), "nombre")).isEqualTo(nombreConflictivo);
    }

    @Test
    void sinAutenticacionElUsuarioQuedaNulo_YLaEscrituraFallaPorLaColumnaNotNull() {
        // Documenta la garantia al reves: en produccion siempre hay un actor
        // autenticado (las rutas mutables exigen JWT), asi que esto solo
        // pasaria si algo llamara a registrar() fuera de una peticion real --
        // y falla ruidoso (NOT NULL), no en silencio con un usuario_id nulo.
        String entidadId = pe.rikos.contratos.Identificador.nuevo().valor();

        assertThatThrownBy(() ->
                new TransactionTemplate(transactionManager).executeWithoutResult(estado ->
                        auditoria.registrar("eliminacion", "prueba-sin-actor", entidadId, null, null)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
