package pe.rikos.operacion.catalogo.aplicacion;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import pe.rikos.contratos.Resultado;
import pe.rikos.operacion.catalogo.infraestructura.Motivo;
import pe.rikos.operacion.catalogo.infraestructura.MotivoRepositorio;
import pe.rikos.operacion.comun.auditoria.Auditoria;
import pe.rikos.operacion.comun.auditoria.AuditoriaRepositorio;
import pe.rikos.operacion.comun.auditoria.ContextoDeSeguridadDePrueba;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class CambiarEstadoMotivoTest {

    private static final String ADMIN_SEMBRADO_ID = "01M2QZ606763Q7XC00M8VD7WM1";

    @Autowired
    private CrearMotivo crearMotivo;

    @Autowired
    private CambiarEstadoMotivo cambiarEstadoMotivo;

    @Autowired
    private AuditoriaRepositorio auditorias;

    @Autowired
    private MotivoRepositorio motivos;

    @BeforeEach
    void autenticar() {
        ContextoDeSeguridadDePrueba.autenticarComo(ADMIN_SEMBRADO_ID);
    }

    @AfterEach
    void limpiarContexto() {
        ContextoDeSeguridadDePrueba.limpiar();
    }

    @Test
    void desactiva_un_motivo_activo() {
        Motivo motivo = crearMotivo.ejecutar("ajuste_inventario", "Conteo ciclico", false).valorOLanza();

        Resultado<Motivo, ErrorMotivo> resultado = cambiarEstadoMotivo.ejecutar(motivo.getId(), false);

        assertThat(resultado.esExito()).isTrue();
        assertThat(resultado.valorOLanza().isActivo()).isFalse();
    }

    @Test
    void activa_un_motivo_desactivado() {
        Motivo motivo = crearMotivo.ejecutar("ajuste_inventario", "Conteo anual", false).valorOLanza();
        cambiarEstadoMotivo.ejecutar(motivo.getId(), false);

        Resultado<Motivo, ErrorMotivo> resultado = cambiarEstadoMotivo.ejecutar(motivo.getId(), true);

        assertThat(resultado.esExito()).isTrue();
        assertThat(resultado.valorOLanza().isActivo()).isTrue();
    }

    /** Sin un motivo activo de disponibilidad nadie podria volver a habilitar un producto agotado (V23). */
    @Test
    void no_deja_desactivar_el_ultimo_motivo_activo_de_disponibilidad() {
        crearMotivo.ejecutar("disponibilidad", "Motivo extra de prueba", false).valorOLanza();
        List<Motivo> activos = motivos.findByTipoOrderByDescripcionAsc("disponibilidad").stream()
                .filter(Motivo::isActivo).toList();
        for (Motivo m : activos.subList(0, activos.size() - 1)) {
            assertThat(cambiarEstadoMotivo.ejecutar(m.getId(), false).esExito()).isTrue();
        }
        Motivo ultimo = activos.get(activos.size() - 1);

        Resultado<Motivo, ErrorMotivo> resultado = cambiarEstadoMotivo.ejecutar(ultimo.getId(), false);

        assertThat(resultado.esExito()).isFalse();
        assertThat(((Resultado.Fracaso<?, ErrorMotivo>) resultado).error())
                .isEqualTo(ErrorMotivo.ULTIMO_DE_DISPONIBILIDAD);
        assertThat(motivos.findById(ultimo.getId()).orElseThrow().isActivo()).isTrue();
    }

    @Test
    void rechaza_cambiar_estado_de_un_motivo_inexistente() {
        Resultado<Motivo, ErrorMotivo> resultado = cambiarEstadoMotivo.ejecutar("no-existe", false);

        assertThat(resultado.esExito()).isFalse();
        assertThat(((Resultado.Fracaso<?, ErrorMotivo>) resultado).error())
                .isEqualTo(ErrorMotivo.NO_ENCONTRADO);
    }

    @Test
    void queda_una_fila_de_auditoria_de_cambio_de_estado_sobre_la_entidad_motivo() {
        Motivo motivo = crearMotivo.ejecutar("anulacion", "Producto agotado", false).valorOLanza();

        cambiarEstadoMotivo.ejecutar(motivo.getId(), false);

        List<Auditoria> filas = auditorias.findByEntidadAndEntidadIdOrderByOcurridoEnDesc("motivo", motivo.getId());
        assertThat(filas).hasSize(1);
        Auditoria fila = filas.get(0);
        assertThat(fila.getEvento()).isEqualTo("cambio_estado");
        assertThat(fila.getEntidad()).isEqualTo("motivo");
        assertThat(fila.getUsuarioId()).isEqualTo(ADMIN_SEMBRADO_ID);
    }
}
