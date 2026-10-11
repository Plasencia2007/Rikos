package pe.rikos.operacion.venta.aplicacion;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;
import pe.rikos.contratos.Identificador;
import pe.rikos.contratos.Resultado;
import pe.rikos.operacion.comun.auditoria.ContextoDeSeguridadDePrueba;
import pe.rikos.operacion.identidad.aplicacion.CrearLocal;
import pe.rikos.operacion.venta.infraestructura.Mesa;
import pe.rikos.operacion.venta.infraestructura.MesaRepositorio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** US-027: crear y editar las mesas de un local. El numero es unico dentro del local. */
@SpringBootTest
@Transactional
class GestionDeMesasTest {

    static final String ADMIN_SEMBRADO_ID = "01M2QZ606763Q7XC00M8VD7WM1";

    @Autowired
    MesaRepositorio mesas;
    @Autowired
    CrearLocal crearLocal;
    @Autowired
    EntityManager em;
    @Autowired
    CrearMesa crearMesa;
    @Autowired
    ActualizarMesa actualizarMesa;
    @Autowired
    CambiarEstadoMesa cambiarEstadoMesa;

    String localA;
    String localB;

    @BeforeEach
    void preparar() {
        ContextoDeSeguridadDePrueba.autenticarComo(ADMIN_SEMBRADO_ID);
        localA = local("ma");
        localB = local("mb");
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

    private static ErrorMesa error(Resultado<?, ErrorMesa> resultado) {
        assertThat(resultado.esExito()).isFalse();
        return ((Resultado.Fracaso<?, ErrorMesa>) resultado).error();
    }

    @Test
    void la_base_rechaza_dos_mesas_con_el_mismo_numero_en_un_local_pero_lo_permite_en_otro() {
        mesas.saveAndFlush(new Mesa(Identificador.nuevo().valor(), localA, "5", null, 4, ADMIN_SEMBRADO_ID));
        mesas.saveAndFlush(new Mesa(Identificador.nuevo().valor(), localB, "5", null, 4, ADMIN_SEMBRADO_ID));

        assertThatThrownBy(() -> mesas.saveAndFlush(
                new Mesa(Identificador.nuevo().valor(), localA, "5", null, 2, ADMIN_SEMBRADO_ID)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void la_base_rechaza_una_mesa_bloqueada_sin_motivo() {
        Mesa mesa = mesas.saveAndFlush(new Mesa(Identificador.nuevo().valor(), localA, "6", null, null,
                ADMIN_SEMBRADO_ID));
        em.clear();

        assertThatThrownBy(() -> em.createNativeQuery(
                "update mesa set bloqueada = true where id = '" + mesa.getId() + "'").executeUpdate())
                .hasMessageContaining("mesa_bloqueada_con_motivo");
    }

    @Test
    void crea_una_mesa_en_el_local_de_la_sesion_con_su_numero_nombre_y_capacidad() {
        Mesa mesa = crearMesa.ejecutar(new DatosMesa("  7 ", " Terraza 3 ", 6), ADMIN_SEMBRADO_ID).valorOLanza();

        assertThat(mesa.getLocalId()).isEqualTo(localA);
        assertThat(mesa.getNumero()).isEqualTo("7");
        assertThat(mesa.getNombre()).isEqualTo("Terraza 3");
        assertThat(mesa.getCapacidad()).isEqualTo(6);
        assertThat(mesa.isActivo()).isTrue();
        assertThat(mesa.isBloqueada()).isFalse();
    }

    @Test
    void rechaza_un_numero_vacio_muy_largo_o_repetido_en_el_mismo_local_pero_no_en_otro() {
        crearMesa.ejecutar(new DatosMesa("1", null, null), ADMIN_SEMBRADO_ID).valorOLanza();

        assertThat(error(crearMesa.ejecutar(new DatosMesa("   ", null, null), ADMIN_SEMBRADO_ID)))
                .isEqualTo(ErrorMesa.NUMERO_INVALIDO);
        assertThat(error(crearMesa.ejecutar(new DatosMesa("12345678901", null, null), ADMIN_SEMBRADO_ID)))
                .isEqualTo(ErrorMesa.NUMERO_INVALIDO);
        assertThat(error(crearMesa.ejecutar(new DatosMesa("1", null, null), ADMIN_SEMBRADO_ID)))
                .isEqualTo(ErrorMesa.NUMERO_DUPLICADO);

        ContextoDeSeguridadDePrueba.autenticarComo(ADMIN_SEMBRADO_ID, localB);
        assertThat(crearMesa.ejecutar(new DatosMesa("1", null, null), ADMIN_SEMBRADO_ID).esExito()).isTrue();
    }

    @Test
    void la_capacidad_si_se_da_es_mayor_que_cero() {
        for (int capacidad : new int[] {0, -3}) {
            assertThat(error(crearMesa.ejecutar(new DatosMesa("9", null, capacidad), ADMIN_SEMBRADO_ID)))
                    .isEqualTo(ErrorMesa.CAPACIDAD_INVALIDA);
        }
    }

    @Test
    void sin_local_activo_no_se_crea_nada() {
        ContextoDeSeguridadDePrueba.autenticarComo(ADMIN_SEMBRADO_ID);

        assertThat(error(crearMesa.ejecutar(new DatosMesa("2", null, null), ADMIN_SEMBRADO_ID)))
                .isEqualTo(ErrorMesa.SIN_LOCAL_ACTIVO);
    }

    @Test
    void edita_una_mesa_de_mi_local_y_no_puede_repetir_el_numero_de_otra() {
        Mesa uno = crearMesa.ejecutar(new DatosMesa("1", null, null), ADMIN_SEMBRADO_ID).valorOLanza();
        crearMesa.ejecutar(new DatosMesa("2", null, null), ADMIN_SEMBRADO_ID).valorOLanza();

        Mesa editada = actualizarMesa.ejecutar(uno.getId(), new DatosMesa("1", "Ventana", 4), ADMIN_SEMBRADO_ID)
                .valorOLanza();

        assertThat(editada.getNombre()).isEqualTo("Ventana");
        assertThat(editada.getCapacidad()).isEqualTo(4);
        assertThat(error(actualizarMesa.ejecutar(uno.getId(), new DatosMesa("2", null, null), ADMIN_SEMBRADO_ID)))
                .isEqualTo(ErrorMesa.NUMERO_DUPLICADO);
    }

    @Test
    void una_mesa_de_otro_local_no_se_encuentra_ni_se_edita_ni_se_desactiva() {
        ContextoDeSeguridadDePrueba.autenticarComo(ADMIN_SEMBRADO_ID, localB);
        Mesa deB = crearMesa.ejecutar(new DatosMesa("1", null, null), ADMIN_SEMBRADO_ID).valorOLanza();
        ContextoDeSeguridadDePrueba.autenticarComo(ADMIN_SEMBRADO_ID, localA);

        assertThat(error(actualizarMesa.ejecutar(deB.getId(), new DatosMesa("1", "X", null), ADMIN_SEMBRADO_ID)))
                .isEqualTo(ErrorMesa.NO_ENCONTRADA);
        assertThat(error(cambiarEstadoMesa.ejecutar(deB.getId(), false, ADMIN_SEMBRADO_ID)))
                .isEqualTo(ErrorMesa.NO_ENCONTRADA);
        assertThat(mesas.findById(deB.getId()).orElseThrow().isActivo()).isTrue();
    }

    @Test
    void desactivar_y_reactivar_es_idempotente_y_conserva_la_mesa() {
        Mesa mesa = crearMesa.ejecutar(new DatosMesa("3", null, null), ADMIN_SEMBRADO_ID).valorOLanza();

        assertThat(cambiarEstadoMesa.ejecutar(mesa.getId(), false, ADMIN_SEMBRADO_ID).valorOLanza().isActivo())
                .isFalse();
        assertThat(cambiarEstadoMesa.ejecutar(mesa.getId(), false, ADMIN_SEMBRADO_ID).esExito()).isTrue();
        assertThat(cambiarEstadoMesa.ejecutar(mesa.getId(), true, ADMIN_SEMBRADO_ID).valorOLanza().isActivo())
                .isTrue();
    }
}
