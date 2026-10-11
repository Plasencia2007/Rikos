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
import pe.rikos.operacion.venta.infraestructura.MesaRepositorio;
import pe.rikos.operacion.venta.infraestructura.Salon;
import pe.rikos.operacion.venta.infraestructura.SalonRepositorio;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Salones y mesas: un salon agrupa las mesas de un local (terraza, salon principal, barra...) y guarda
 * el tamano de su plano; cada mesa lleva su forma, sus atributos y su lugar en ese plano.
 */
@SpringBootTest
@Transactional
class GestionDeSalonesTest {

    static final String ADMIN_SEMBRADO_ID = "01M2QZ606763Q7XC00M8VD7WM1";

    @Autowired
    CrearLocal crearLocal;
    @Autowired
    CrearSalon crearSalon;
    @Autowired
    ActualizarSalon actualizarSalon;
    @Autowired
    CambiarEstadoSalon cambiarEstadoSalon;
    @Autowired
    EliminarSalon eliminarSalon;
    @Autowired
    ConsultaSalones consultaSalones;
    @Autowired
    GuardarPlano guardarPlano;
    @Autowired
    CrearMesa crearMesa;
    @Autowired
    ActualizarMesa actualizarMesa;
    @Autowired
    MesaRepositorio mesas;
    @Autowired
    SalonRepositorio salones;

    String localA;
    String localB;

    @BeforeEach
    void preparar() {
        ContextoDeSeguridadDePrueba.autenticarComo(ADMIN_SEMBRADO_ID);
        localA = local("sa");
        localB = local("sb");
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

    static DatosSalon terraza() {
        return new DatosSalon("Terraza", "Area exterior", "terraza", "ambar", null, null, null);
    }

    Salon salon(String nombre) {
        return crearSalon.ejecutar(new DatosSalon(nombre, null, "cubiertos", "azul", null, null, null),
                ADMIN_SEMBRADO_ID).valorOLanza();
    }

    static <E extends Enum<E>> E error(Resultado<?, E> resultado) {
        assertThat(resultado.esExito()).isFalse();
        return ((Resultado.Fracaso<?, E>) resultado).error();
    }

    @Test
    void crea_un_salon_en_el_local_activo_con_el_plano_por_defecto() {
        Salon s = crearSalon.ejecutar(terraza(), ADMIN_SEMBRADO_ID).valorOLanza();

        assertThat(s.getLocalId()).isEqualTo(localA);
        assertThat(s.isActivo()).isTrue();
        assertThat(s.getAnchoPlano()).isEqualTo(1200);
        assertThat(s.getAltoPlano()).isEqualTo(800);
        assertThat(s.getColorPiso()).isEqualTo("#F3EEDF");
    }

    @Test
    void el_nombre_es_obligatorio_y_unico_dentro_del_local_pero_se_repite_en_otro_local() {
        salon("Terraza");

        assertThat(error(crearSalon.ejecutar(terraza(), ADMIN_SEMBRADO_ID))).isEqualTo(ErrorSalon.NOMBRE_DUPLICADO);
        assertThat(error(crearSalon.ejecutar(new DatosSalon("  ", null, "copa", "verde", null, null, null),
                ADMIN_SEMBRADO_ID))).isEqualTo(ErrorSalon.NOMBRE_INVALIDO);

        ContextoDeSeguridadDePrueba.autenticarComo(ADMIN_SEMBRADO_ID, localB);
        assertThat(crearSalon.ejecutar(terraza(), ADMIN_SEMBRADO_ID).esExito()).isTrue();
    }

    @Test
    void rechaza_un_icono_un_color_o_un_plano_que_no_existe() {
        assertThat(error(crearSalon.ejecutar(new DatosSalon("X", null, "cohete", "verde", null, null, null),
                ADMIN_SEMBRADO_ID))).isEqualTo(ErrorSalon.VALOR_INVALIDO);
        assertThat(error(crearSalon.ejecutar(new DatosSalon("X", null, "copa", "fucsia", null, null, null),
                ADMIN_SEMBRADO_ID))).isEqualTo(ErrorSalon.VALOR_INVALIDO);
        assertThat(error(crearSalon.ejecutar(new DatosSalon("X", null, "copa", "verde", 0, 800, null),
                ADMIN_SEMBRADO_ID))).isEqualTo(ErrorSalon.VALOR_INVALIDO);
        assertThat(error(crearSalon.ejecutar(new DatosSalon("X", null, "copa", "verde", 1200, 800, "rojo"),
                ADMIN_SEMBRADO_ID))).isEqualTo(ErrorSalon.VALOR_INVALIDO);
    }

    @Test
    void un_salon_de_otro_local_no_se_encuentra() {
        Salon deB;
        ContextoDeSeguridadDePrueba.autenticarComo(ADMIN_SEMBRADO_ID, localB);
        deB = salon("Barra");
        ContextoDeSeguridadDePrueba.autenticarComo(ADMIN_SEMBRADO_ID, localA);

        assertThat(error(actualizarSalon.ejecutar(deB.getId(), terraza(), ADMIN_SEMBRADO_ID)))
                .isEqualTo(ErrorSalon.NO_ENCONTRADO);
        assertThat(error(cambiarEstadoSalon.ejecutar(deB.getId(), false, ADMIN_SEMBRADO_ID)))
                .isEqualTo(ErrorSalon.NO_ENCONTRADO);
    }

    @Test
    void una_mesa_sin_salon_va_al_salon_principal_del_local_que_se_crea_una_sola_vez() {
        Mesa a = crearMesa.ejecutar(new DatosMesa("1", null, 4), ADMIN_SEMBRADO_ID).valorOLanza();
        Mesa b = crearMesa.ejecutar(new DatosMesa("2", null, 4), ADMIN_SEMBRADO_ID).valorOLanza();

        assertThat(a.getSalonId()).isNotNull().isEqualTo(b.getSalonId());
        assertThat(salones.findById(a.getSalonId()).orElseThrow().getNombre()).isEqualTo("Principal");
    }

    @Test
    void crea_una_mesa_en_un_salon_con_su_forma_sus_atributos_y_un_lugar_en_el_plano() {
        Salon s = crearSalon.ejecutar(terraza(), ADMIN_SEMBRADO_ID).valorOLanza();

        Mesa m = crearMesa.ejecutar(new DatosMesa("T1", "Terraza 1", 6, s.getId(), "redonda", true, true, false,
                null, null), ADMIN_SEMBRADO_ID).valorOLanza();

        assertThat(m.getSalonId()).isEqualTo(s.getId());
        assertThat(m.getForma()).isEqualTo("redonda");
        assertThat(m.isVip()).isTrue();
        assertThat(m.isZonaExterior()).isTrue();
        assertThat(m.isZonaFumadores()).isFalse();
        assertThat(m.getPosX()).isNotNull();
        assertThat(m.getPosY()).isNotNull();
    }

    @Test
    void las_mesas_nuevas_de_un_salon_no_se_montan_unas_sobre_otras() {
        Salon s = crearSalon.ejecutar(terraza(), ADMIN_SEMBRADO_ID).valorOLanza();
        Mesa a = crearMesa.ejecutar(new DatosMesa("A", null, 4, s.getId(), "cuadrada", false, false, false, null, null),
                ADMIN_SEMBRADO_ID).valorOLanza();
        Mesa b = crearMesa.ejecutar(new DatosMesa("B", null, 4, s.getId(), "cuadrada", false, false, false, null, null),
                ADMIN_SEMBRADO_ID).valorOLanza();

        assertThat(a.getPosX() == b.getPosX() && a.getPosY() == b.getPosY()).isFalse();
    }

    @Test
    void rechaza_una_mesa_en_un_salon_de_otro_local_una_forma_desconocida_o_medidas_no_positivas() {
        ContextoDeSeguridadDePrueba.autenticarComo(ADMIN_SEMBRADO_ID, localB);
        Salon deB = salon("Barra");
        ContextoDeSeguridadDePrueba.autenticarComo(ADMIN_SEMBRADO_ID, localA);
        Salon deA = salon("Terraza");

        assertThat(error(crearMesa.ejecutar(new DatosMesa("1", null, 4, deB.getId(), "cuadrada", false, false, false,
                null, null), ADMIN_SEMBRADO_ID))).isEqualTo(ErrorMesa.SALON_NO_ENCONTRADO);
        assertThat(error(crearMesa.ejecutar(new DatosMesa("2", null, 4, deA.getId(), "hexagonal", false, false, false,
                null, null), ADMIN_SEMBRADO_ID))).isEqualTo(ErrorMesa.FORMA_INVALIDA);
        assertThat(error(crearMesa.ejecutar(new DatosMesa("3", null, 4, deA.getId(), "redonda", false, false, false,
                0, 80), ADMIN_SEMBRADO_ID))).isEqualTo(ErrorMesa.MEDIDAS_INVALIDAS);
    }

    @Test
    void editar_una_mesa_puede_cambiarla_de_salon_y_de_forma() {
        Salon terraza = crearSalon.ejecutar(terraza(), ADMIN_SEMBRADO_ID).valorOLanza();
        Salon barra = salon("Barra");
        Mesa m = crearMesa.ejecutar(new DatosMesa("T1", null, 4, terraza.getId(), "cuadrada", false, false, false,
                null, null), ADMIN_SEMBRADO_ID).valorOLanza();

        Mesa editada = actualizarMesa.ejecutar(m.getId(), new DatosMesa("T1", "Ventana", 8, barra.getId(), "rectangular",
                true, false, true, null, null), ADMIN_SEMBRADO_ID).valorOLanza();

        assertThat(editada.getSalonId()).isEqualTo(barra.getId());
        assertThat(editada.getForma()).isEqualTo("rectangular");
        assertThat(editada.isVip()).isTrue();
        assertThat(editada.isZonaFumadores()).isTrue();
        assertThat(editada.getCapacidad()).isEqualTo(8);
    }

    @Test
    void un_salon_con_mesas_no_se_elimina_uno_vacio_si() {
        Salon conMesas = crearSalon.ejecutar(terraza(), ADMIN_SEMBRADO_ID).valorOLanza();
        crearMesa.ejecutar(new DatosMesa("T1", null, 4, conMesas.getId(), "cuadrada", false, false, false, null, null),
                ADMIN_SEMBRADO_ID).valorOLanza();
        Salon vacio = salon("Jardin");

        assertThat(error(eliminarSalon.ejecutar(conMesas.getId(), ADMIN_SEMBRADO_ID))).isEqualTo(ErrorSalon.TIENE_MESAS);
        assertThat(eliminarSalon.ejecutar(vacio.getId(), ADMIN_SEMBRADO_ID).esExito()).isTrue();
        assertThat(salones.findById(vacio.getId())).isEmpty();
    }

    @Test
    void desactivar_un_salon_lo_saca_de_la_lista_de_activos_y_lo_conserva() {
        Salon s = salon("Terraza");

        cambiarEstadoSalon.ejecutar(s.getId(), false, ADMIN_SEMBRADO_ID).valorOLanza();

        assertThat(salones.findById(s.getId()).orElseThrow().isActivo()).isFalse();
    }

    @Test
    void la_lista_de_salones_cuenta_mesas_y_capacidad() {
        Salon s = crearSalon.ejecutar(terraza(), ADMIN_SEMBRADO_ID).valorOLanza();
        crearMesa.ejecutar(new DatosMesa("1", null, 4, s.getId(), "redonda", false, false, false, null, null),
                ADMIN_SEMBRADO_ID).valorOLanza();
        Mesa inactiva = crearMesa.ejecutar(new DatosMesa("2", null, 6, s.getId(), "redonda", false, false, false, null,
                null), ADMIN_SEMBRADO_ID).valorOLanza();
        mesas.findById(inactiva.getId()).orElseThrow().cambiarEstado(false, ADMIN_SEMBRADO_ID);
        mesas.flush();

        List<ResumenDeSalon> lista = consultaSalones.listar().valorOLanza();

        ResumenDeSalon r = lista.stream().filter(x -> x.salon().getId().equals(s.getId())).findFirst().orElseThrow();
        assertThat(r.mesas()).isEqualTo(2);
        assertThat(r.mesasActivas()).isEqualTo(1);
        assertThat(r.capacidad()).isEqualTo(4);
    }

    @Test
    void guarda_la_posicion_y_las_medidas_de_las_mesas_del_plano() {
        Salon s = crearSalon.ejecutar(terraza(), ADMIN_SEMBRADO_ID).valorOLanza();
        Mesa m = crearMesa.ejecutar(new DatosMesa("T1", null, 4, s.getId(), "redonda", false, false, false, null, null),
                ADMIN_SEMBRADO_ID).valorOLanza();

        guardarPlano.ejecutar(s.getId(), List.of(new PosicionDeMesa(m.getId(), 380, 120, 90, 90)), ADMIN_SEMBRADO_ID)
                .valorOLanza();

        Mesa releida = mesas.findById(m.getId()).orElseThrow();
        assertThat(releida.getPosX()).isEqualTo(380);
        assertThat(releida.getPosY()).isEqualTo(120);
        assertThat(releida.getAncho()).isEqualTo(90);
        assertThat(releida.getAlto()).isEqualTo(90);
    }

    @Test
    void el_plano_rechaza_una_mesa_de_otro_salon_o_medidas_no_positivas_y_no_guarda_nada() {
        Salon s = crearSalon.ejecutar(terraza(), ADMIN_SEMBRADO_ID).valorOLanza();
        Salon otro = salon("Barra");
        Mesa propia = crearMesa.ejecutar(new DatosMesa("T1", null, 4, s.getId(), "redonda", false, false, false, null,
                null), ADMIN_SEMBRADO_ID).valorOLanza();
        Mesa ajena = crearMesa.ejecutar(new DatosMesa("B1", null, 4, otro.getId(), "redonda", false, false, false, null,
                null), ADMIN_SEMBRADO_ID).valorOLanza();
        int xAntes = propia.getPosX();

        assertThat(error(guardarPlano.ejecutar(s.getId(), List.of(new PosicionDeMesa(propia.getId(), 10, 10, 84, 84),
                new PosicionDeMesa(ajena.getId(), 20, 20, 84, 84)), ADMIN_SEMBRADO_ID)))
                .isEqualTo(ErrorSalon.MESA_NO_ES_DEL_SALON);
        assertThat(error(guardarPlano.ejecutar(s.getId(), List.of(new PosicionDeMesa(propia.getId(), 10, 10, 0, 84)),
                ADMIN_SEMBRADO_ID))).isEqualTo(ErrorSalon.VALOR_INVALIDO);
        assertThat(mesas.findById(propia.getId()).orElseThrow().getPosX()).isEqualTo(xAntes);
    }
}
