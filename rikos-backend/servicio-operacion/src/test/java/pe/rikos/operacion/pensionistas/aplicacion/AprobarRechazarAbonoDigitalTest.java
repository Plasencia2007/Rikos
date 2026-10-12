package pe.rikos.operacion.pensionistas.aplicacion;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import pe.rikos.contratos.Identificador;
import pe.rikos.contratos.Resultado;
import pe.rikos.operacion.caja.aplicacion.ErrorAbono;
import pe.rikos.operacion.catalogo.infraestructura.Motivo;
import pe.rikos.operacion.catalogo.infraestructura.MotivoRepositorio;
import pe.rikos.operacion.comun.auditoria.ContextoDeSeguridadDePrueba;
import pe.rikos.operacion.pensionistas.infraestructura.Abono;
import pe.rikos.operacion.pensionistas.infraestructura.AbonoRepositorio;
import pe.rikos.operacion.pensionistas.infraestructura.MovimientoCuentaRepositorio;
import pe.rikos.operacion.pensionistas.infraestructura.Pensionista;
import pe.rikos.operacion.venta.aplicacion.PedidosBase;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class AprobarRechazarAbonoDigitalTest extends PedidosBase {

    @Autowired
    AprobarAbonoDigital aprobarAbono;
    @Autowired
    RechazarAbonoDigital rechazarAbono;
    @Autowired
    ConsultaAbonos consultaAbonos;
    @Autowired
    AbonoRepositorio abonos;
    @Autowired
    MovimientoCuentaRepositorio movimientosCuenta;
    @Autowired
    MotivoRepositorio motivos;
    @Autowired
    CargarSaldoInicial cargarSaldoInicial;
    @Autowired
    ConsultaCuenta consultaCuenta;

    Pensionista pensionista;
    String localId;
    Motivo motivoSimple;
    Motivo motivoConObs;

    @BeforeEach
    void setup() {
        ContextoDeSeguridadDePrueba.autenticarComo(ADMIN_SEMBRADO_ID);
        localId = nuevoLocal("ad");
        ContextoDeSeguridadDePrueba.autenticarComo(ADMIN_SEMBRADO_ID, localId);

        String planId = crearPlan.ejecutar("Plan Test Admin " + Identificador.nuevo().valor().substring(18), null,
                new PreciosPlan(800L, 1200L, 1000L), null, ADMIN_SEMBRADO_ID).valorOLanza().getId();

        pensionista = crearPensionista.ejecutar(new DatosPensionista("77889900", "Alumno", "Digital",
                "987111222", "alumno.dig@correo.com", "UNI", "20240099", "Industrial", "2", planId,
                Set.of(Comida.ALMUERZO), "semanal", new DatosResponsable("Padre Digital", "44556677", "987000111",
                "padre.dig@correo.com", "Padre", "Av. Peru 456")), ADMIN_SEMBRADO_ID).valorOLanza().pensionista();

        // Cargar deuda de S/ 80.00
        cargarSaldoInicial.ejecutar(pensionista.getId(), 8000L, Signo.CARGO, LocalDate.now(),
                "Deuda inicial", ADMIN_SEMBRADO_ID).valorOLanza();

        motivoSimple = motivos.findById("01M49A0MT0000000000000R001").orElseGet(() ->
                motivos.save(new Motivo("01M49A0MT0000000000000R001", "rechazo_abono",
                        "Operación no encontrada en cuenta bancaria", false)));

        motivoConObs = motivos.findById("01M49A0MT0000000000000R002").orElseGet(() ->
                motivos.save(new Motivo("01M49A0MT0000000000000R002", "rechazo_abono",
                        "Monto no coincide con el extracto bancario", true)));
    }

    private Abono crearAbonoPendiente(long monto, String medio, String numOp) {
        Abono a = Abono.digitalPendiente(
                Identificador.nuevo().valor(),
                pensionista.getId(),
                localId,
                medio,
                monto,
                LocalDate.now(),
                numOp,
                "BCP RIKO'S",
                null,
                Abono.ORIGEN_CAJA,
                ADMIN_SEMBRADO_ID,
                "Abono para aprobar",
                OffsetDateTime.now()
        );
        return abonos.save(a);
    }

    @Test
    void aprobar_abono_pendiente_acredita_saldo_y_cambia_a_aprobado() {
        Abono pendiente = crearAbonoPendiente(5000L, Abono.MEDIO_YAPE, "OP-112233");

        Resultado<AbonoAprobado, ErrorAbono> res = aprobarAbono.ejecutar(pendiente.getId(), ADMIN_SEMBRADO_ID);

        assertThat(res.esExito()).isTrue();
        AbonoAprobado aprobado = res.valorOLanza();
        assertThat(aprobado.nuevoSaldo()).isEqualTo(3000L); // Deuda se redujo de 8000 a 3000
        assertThat(aprobado.saldoAFavor()).isEqualTo(0L);

        Abono guardado = abonos.findById(pendiente.getId()).orElseThrow();
        assertThat(guardado.getEstado()).isEqualTo(Abono.ESTADO_APROBADO);
        assertThat(guardado.getAprobadoPor()).isEqualTo(ADMIN_SEMBRADO_ID);
        assertThat(guardado.getAprobadoEn()).isNotNull();

        var mov = movimientosCuenta.findByAbonoId(guardado.getId());
        assertThat(mov).isPresent();
        assertThat(mov.get().getSigno()).isEqualTo(Signo.CREDITO.name().toLowerCase());
        assertThat(mov.get().getImporte()).isEqualTo(5000L);
    }

    @Test
    void rechazar_abono_pendiente_con_motivo_no_altera_saldo_y_cambia_a_rechazado() {
        Abono pendiente = crearAbonoPendiente(3000L, Abono.MEDIO_PLIN, "OP-998877");

        Resultado<Abono, ErrorAbono> res = rechazarAbono.ejecutar(pendiente.getId(), motivoSimple.getId(), null, ADMIN_SEMBRADO_ID);

        assertThat(res.esExito()).isTrue();
        Abono rechazado = res.valorOLanza();
        assertThat(rechazado.getEstado()).isEqualTo(Abono.ESTADO_RECHAZADO);
        assertThat(rechazado.getAprobadoPor()).isEqualTo(ADMIN_SEMBRADO_ID);
        assertThat(rechazado.getMotivoRechazoId()).isEqualTo(motivoSimple.getId());

        // Saldo no se altera (CA3, CA4)
        var estadoCuenta = consultaCuenta.resumen(pensionista.getId()).orElseThrow();
        assertThat(estadoCuenta.deuda()).isEqualTo(8000L);

        var mov = movimientosCuenta.findByAbonoId(pendiente.getId());
        assertThat(mov).isEmpty();
    }

    @Test
    void rechazar_con_motivo_que_requiere_observacion_sin_observacion_falla() {
        Abono pendiente = crearAbonoPendiente(2000L, Abono.MEDIO_TRANSFERENCIA, "OP-554433");

        Resultado<Abono, ErrorAbono> res = rechazarAbono.ejecutar(pendiente.getId(), motivoConObs.getId(), "   ", ADMIN_SEMBRADO_ID);

        assertThat(res.esFracaso()).isTrue();
        assertThat(error(res)).isEqualTo(ErrorAbono.OBSERVACION_REQUERIDA);

        // Con observación válida sí aprueba el rechazo
        Resultado<Abono, ErrorAbono> resOk = rechazarAbono.ejecutar(pendiente.getId(), motivoConObs.getId(),
                "Comprobante figura por S/ 15 no S/ 20", ADMIN_SEMBRADO_ID);
        assertThat(resOk.esExito()).isTrue();
        assertThat(resOk.valorOLanza().getEstado()).isEqualTo(Abono.ESTADO_RECHAZADO);
    }

    @Test
    void aprobar_o_rechazar_abono_ya_procesado_falla() {
        Abono pendiente = crearAbonoPendiente(1000L, Abono.MEDIO_YAPE, "OP-332211");
        aprobarAbono.ejecutar(pendiente.getId(), ADMIN_SEMBRADO_ID).valorOLanza();

        // Intento 2: aprobar
        var resAprueba = aprobarAbono.ejecutar(pendiente.getId(), ADMIN_SEMBRADO_ID);
        assertThat(resAprueba.esFracaso()).isTrue();
        assertThat(error(resAprueba)).isEqualTo(ErrorAbono.ABONO_YA_PROCESADO);

        // Intento 3: rechazar
        var resRechaza = rechazarAbono.ejecutar(pendiente.getId(), motivoSimple.getId(), null, ADMIN_SEMBRADO_ID);
        assertThat(resRechaza.esFracaso()).isTrue();
        assertThat(error(resRechaza)).isEqualTo(ErrorAbono.ABONO_YA_PROCESADO);
    }

    @Test
    void abono_inexistente_falla() {
        var resApr = aprobarAbono.ejecutar("01NOEXISTE00000000000000000", ADMIN_SEMBRADO_ID);
        assertThat(error(resApr)).isEqualTo(ErrorAbono.ABONO_NO_ENCONTRADO);

        var resRec = rechazarAbono.ejecutar("01NOEXISTE00000000000000000", motivoSimple.getId(), null, ADMIN_SEMBRADO_ID);
        assertThat(error(resRec)).isEqualTo(ErrorAbono.ABONO_NO_ENCONTRADO);
    }

    @Test
    void consulta_abonos_lista_y_filtra_correctamente() {
        Abono pend1 = crearAbonoPendiente(1500L, Abono.MEDIO_YAPE, "OP-FILTRO-1");
        Abono pend2 = crearAbonoPendiente(2500L, Abono.MEDIO_PLIN, "OP-FILTRO-2");
        aprobarAbono.ejecutar(pend1.getId(), ADMIN_SEMBRADO_ID).valorOLanza();

        var pendientes = consultaAbonos.listar("pendiente", null);
        assertThat(pendientes).anyMatch(a -> a.id().equals(pend2.getId()));
        assertThat(pendientes).noneMatch(a -> a.id().equals(pend1.getId()));

        var aprobados = consultaAbonos.listar("aprobado", null);
        assertThat(aprobados).anyMatch(a -> a.id().equals(pend1.getId()));

        var busqueda = consultaAbonos.listar(null, "FILTRO-2");
        assertThat(busqueda).hasSize(1);
        assertThat(busqueda.get(0).id()).isEqualTo(pend2.getId());
    }

    private static ErrorAbono error(Resultado<?, ErrorAbono> res) {
        assertThat(res.esFracaso()).isTrue();
        return ((Resultado.Fracaso<?, ErrorAbono>) res).error();
    }
}
