package pe.rikos.operacion.venta.aplicacion;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import pe.rikos.contratos.Identificador;
import pe.rikos.contratos.Resultado;
import pe.rikos.operacion.comun.auditoria.ContextoDeSeguridadDePrueba;
import pe.rikos.operacion.identidad.aplicacion.CrearLocal;
import pe.rikos.operacion.venta.infraestructura.Mesa;

import static org.assertj.core.api.Assertions.assertThat;

/** US-027 / RF-028: bloquear y liberar una mesa. Solo una mesa libre y activa; siempre con motivo. */
@SpringBootTest
@Transactional
class BloqueoDeMesasTest {

    static final String ADMIN_SEMBRADO_ID = "01M2QZ606763Q7XC00M8VD7WM1";

    @Autowired
    BloquearMesa bloquearMesa;
    @Autowired
    LiberarMesa liberarMesa;
    @Autowired
    CrearMesa crearMesa;
    @Autowired
    CambiarEstadoMesa cambiarEstadoMesa;
    @Autowired
    CrearLocal crearLocal;

    String localA;
    String localB;

    @BeforeEach
    void preparar() {
        ContextoDeSeguridadDePrueba.autenticarComo(ADMIN_SEMBRADO_ID);
        localA = local("ba");
        localB = local("bb");
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

    static ErrorMesa error(Resultado<?, ErrorMesa> resultado) {
        assertThat(resultado.esExito()).isFalse();
        return ((Resultado.Fracaso<?, ErrorMesa>) resultado).error();
    }

    @Test
    void bloquea_una_mesa_libre_con_su_motivo_quien_y_cuando() {
        Mesa mesa = mesa("1");

        Mesa bloqueada = bloquearMesa.ejecutar(mesa.getId(), "  Mesa rota  ", ADMIN_SEMBRADO_ID).valorOLanza();

        assertThat(bloqueada.isBloqueada()).isTrue();
        assertThat(bloqueada.getMotivoBloqueo()).isEqualTo("Mesa rota");
        assertThat(bloqueada.getBloqueadaPor()).isEqualTo(ADMIN_SEMBRADO_ID);
        assertThat(bloqueada.getBloqueadaEn()).isNotNull();
    }

    @Test
    void exige_un_motivo_que_no_este_en_blanco() {
        Mesa mesa = mesa("2");

        assertThat(error(bloquearMesa.ejecutar(mesa.getId(), null, ADMIN_SEMBRADO_ID)))
                .isEqualTo(ErrorMesa.MOTIVO_OBLIGATORIO);
        assertThat(error(bloquearMesa.ejecutar(mesa.getId(), "   ", ADMIN_SEMBRADO_ID)))
                .isEqualTo(ErrorMesa.MOTIVO_OBLIGATORIO);
    }

    @Test
    void no_bloquea_dos_veces_ni_libera_una_que_no_esta_bloqueada() {
        Mesa mesa = mesa("3");

        assertThat(error(liberarMesa.ejecutar(mesa.getId(), ADMIN_SEMBRADO_ID))).isEqualTo(ErrorMesa.NO_BLOQUEADA);
        bloquearMesa.ejecutar(mesa.getId(), "Reservada", ADMIN_SEMBRADO_ID).valorOLanza();
        assertThat(error(bloquearMesa.ejecutar(mesa.getId(), "Otra vez", ADMIN_SEMBRADO_ID)))
                .isEqualTo(ErrorMesa.YA_BLOQUEADA);
    }

    @Test
    void liberar_la_deja_libre_y_limpia_el_bloqueo() {
        Mesa mesa = mesa("4");
        bloquearMesa.ejecutar(mesa.getId(), "Reservada", ADMIN_SEMBRADO_ID).valorOLanza();

        Mesa libre = liberarMesa.ejecutar(mesa.getId(), ADMIN_SEMBRADO_ID).valorOLanza();

        assertThat(libre.isBloqueada()).isFalse();
        assertThat(libre.getMotivoBloqueo()).isNull();
        assertThat(libre.getBloqueadaPor()).isNull();
        assertThat(libre.getBloqueadaEn()).isNull();
    }

    @Test
    void una_mesa_inactiva_no_se_bloquea() {
        Mesa mesa = mesa("5");
        cambiarEstadoMesa.ejecutar(mesa.getId(), false, ADMIN_SEMBRADO_ID).valorOLanza();

        assertThat(error(bloquearMesa.ejecutar(mesa.getId(), "Motivo", ADMIN_SEMBRADO_ID)))
                .isEqualTo(ErrorMesa.MESA_INACTIVA);
    }

    @Test
    void una_mesa_de_otro_local_no_se_encuentra_para_bloquear_ni_liberar() {
        ContextoDeSeguridadDePrueba.autenticarComo(ADMIN_SEMBRADO_ID, localB);
        Mesa deB = mesa("1");
        ContextoDeSeguridadDePrueba.autenticarComo(ADMIN_SEMBRADO_ID, localA);

        assertThat(error(bloquearMesa.ejecutar(deB.getId(), "Motivo", ADMIN_SEMBRADO_ID)))
                .isEqualTo(ErrorMesa.NO_ENCONTRADA);
        assertThat(error(liberarMesa.ejecutar(deB.getId(), ADMIN_SEMBRADO_ID))).isEqualTo(ErrorMesa.NO_ENCONTRADA);
    }

    @Test
    void sin_local_activo_no_se_bloquea() {
        Mesa mesa = mesa("6");
        ContextoDeSeguridadDePrueba.autenticarComo(ADMIN_SEMBRADO_ID);

        assertThat(error(bloquearMesa.ejecutar(mesa.getId(), "Motivo", ADMIN_SEMBRADO_ID)))
                .isEqualTo(ErrorMesa.SIN_LOCAL_ACTIVO);
    }
}
