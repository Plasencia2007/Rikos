package pe.rikos.operacion.venta.aplicacion;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import pe.rikos.contratos.Identificador;
import pe.rikos.operacion.comun.auditoria.ContextoDeSeguridadDePrueba;
import pe.rikos.operacion.identidad.aplicacion.CrearLocal;
import pe.rikos.operacion.venta.infraestructura.Mesa;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** US-027 CA1 y CA2: el mapa muestra solo las mesas activas del local de la sesion, cada una con su estado. */
@SpringBootTest
@Transactional
class MapaDeMesasTest {

    static final String ADMIN_SEMBRADO_ID = "01M2QZ606763Q7XC00M8VD7WM1";

    @Autowired
    ConsultaMesas consulta;
    @Autowired
    CrearMesa crearMesa;
    @Autowired
    BloquearMesa bloquearMesa;
    @Autowired
    CambiarEstadoMesa cambiarEstadoMesa;
    @Autowired
    CrearLocal crearLocal;

    String localA;
    String localB;

    @BeforeEach
    void preparar() {
        ContextoDeSeguridadDePrueba.autenticarComo(ADMIN_SEMBRADO_ID);
        localA = local("pa");
        localB = local("pb");
        ContextoDeSeguridadDePrueba.autenticarComo(ADMIN_SEMBRADO_ID, localA);
    }

    @AfterEach
    void limpiarContexto() {
        ContextoDeSeguridadDePrueba.limpiar();
    }

    String local(String prefijo) {
        String codigo = prefijo + Identificador.nuevo().valor().substring(20).toLowerCase();
        return crearLocal.ejecutar(codigo, "Local " + codigo, "Direccion", ADMIN_SEMBRADO_ID).valorOLanza().getId();
    }

    Mesa mesa(String numero) {
        return crearMesa.ejecutar(new DatosMesa(numero, null, 4), ADMIN_SEMBRADO_ID).valorOLanza();
    }

    @Test
    void muestra_solo_las_mesas_del_local_de_la_sesion_por_numero_con_su_estado() {
        Mesa uno = mesa("1");
        Mesa dos = mesa("2");
        bloquearMesa.ejecutar(dos.getId(), "Reservada", ADMIN_SEMBRADO_ID).valorOLanza();
        ContextoDeSeguridadDePrueba.autenticarComo(ADMIN_SEMBRADO_ID, localB);
        mesa("1");
        ContextoDeSeguridadDePrueba.autenticarComo(ADMIN_SEMBRADO_ID, localA);

        List<FilaDelMapa> mapa = consulta.mapa().valorOLanza();

        assertThat(mapa).extracting(f -> f.mesa().getId()).containsExactly(uno.getId(), dos.getId());
        assertThat(mapa).extracting(FilaDelMapa::estado).containsExactly(EstadoDeMesa.LIBRE, EstadoDeMesa.BLOQUEADA);
        assertThat(mapa).allMatch(f -> f.mesa().getLocalId().equals(localA));
    }

    @Test
    void una_mesa_inactiva_no_sale_en_el_mapa_pero_si_en_la_lista_de_administracion() {
        Mesa uno = mesa("1");
        Mesa dos = mesa("2");
        cambiarEstadoMesa.ejecutar(dos.getId(), false, ADMIN_SEMBRADO_ID).valorOLanza();

        assertThat(consulta.mapa().valorOLanza()).extracting(f -> f.mesa().getId()).containsExactly(uno.getId());
        assertThat(consulta.todas().valorOLanza()).extracting(f -> f.mesa().getId())
                .containsExactly(uno.getId(), dos.getId());
    }

    @Test
    void los_numeros_se_ordenan_como_numeros_no_como_texto() {
        mesa("10");
        mesa("2");
        mesa("1");

        assertThat(consulta.mapa().valorOLanza()).extracting(f -> f.mesa().getNumero()).containsExactly("1", "2", "10");
    }

    @Test
    void un_local_sin_mesas_da_una_lista_vacia_y_sin_local_activo_da_error() {
        assertThat(consulta.mapa().valorOLanza()).isEmpty();

        ContextoDeSeguridadDePrueba.autenticarComo(ADMIN_SEMBRADO_ID);
        assertThat(consulta.mapa().esExito()).isFalse();
    }
}
