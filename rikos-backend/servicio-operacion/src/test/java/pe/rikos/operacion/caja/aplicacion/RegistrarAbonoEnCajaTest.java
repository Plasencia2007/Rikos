package pe.rikos.operacion.caja.aplicacion;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import pe.rikos.contratos.Identificador;
import pe.rikos.contratos.Resultado;
import pe.rikos.operacion.caja.infraestructura.MovimientoCaja;
import pe.rikos.operacion.caja.infraestructura.MovimientoCajaRepositorio;
import pe.rikos.operacion.caja.infraestructura.TurnoCaja;
import pe.rikos.operacion.caja.infraestructura.TurnoCajaRepositorio;
import pe.rikos.operacion.comun.auditoria.ContextoDeSeguridadDePrueba;
import pe.rikos.operacion.pensionistas.aplicacion.CambiarEstadoPensionista;
import pe.rikos.operacion.pensionistas.aplicacion.CargarSaldoInicial;
import pe.rikos.operacion.pensionistas.aplicacion.Comida;
import pe.rikos.operacion.pensionistas.aplicacion.ConsultaCuenta;
import pe.rikos.operacion.pensionistas.aplicacion.CrearPensionista;
import pe.rikos.operacion.pensionistas.aplicacion.CrearPlanTarifario;
import pe.rikos.operacion.pensionistas.aplicacion.DatosPensionista;
import pe.rikos.operacion.pensionistas.aplicacion.DatosResponsable;
import pe.rikos.operacion.pensionistas.aplicacion.PreciosPlan;
import pe.rikos.operacion.pensionistas.infraestructura.Abono;
import pe.rikos.operacion.pensionistas.infraestructura.AbonoRepositorio;
import pe.rikos.operacion.pensionistas.infraestructura.MovimientoCuenta;
import pe.rikos.operacion.pensionistas.infraestructura.MovimientoCuentaRepositorio;
import pe.rikos.operacion.pensionistas.infraestructura.Pensionista;
import pe.rikos.operacion.pensionistas.infraestructura.PensionistaRepositorio;

import pe.rikos.operacion.venta.aplicacion.PedidosBase;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class RegistrarAbonoEnCajaTest extends PedidosBase {

    @Autowired
    RegistrarAbonoEnCaja registrarAbono;
    @Autowired
    AbrirTurno abrirTurno;
    @Autowired
    ConsultaCajas consultaCajas;
    @Autowired
    TurnoCajaRepositorio turnos;
    @Autowired
    ConsultaCierre consultaCierre;
    @Autowired
    AbonoRepositorio abonos;
    @Autowired
    MovimientoCajaRepositorio movimientosCaja;
    @Autowired
    MovimientoCuentaRepositorio movimientosCuenta;
    @Autowired
    PensionistaRepositorio pensionistas;
    @Autowired
    CargarSaldoInicial cargarSaldoInicial;
    @Autowired
    CambiarEstadoPensionista cambiarEstadoPensionista;
    @Autowired
    ConsultaCuenta consultaCuenta;
    @Autowired
    CrearPlanTarifario crearPlan;
    @Autowired
    CrearPensionista crearPensionista;

    TurnoCaja turno;
    Pensionista pensionista;
    String localId;

    @BeforeEach
    void setup() {
        ContextoDeSeguridadDePrueba.autenticarComo(ADMIN_SEMBRADO_ID);
        localId = nuevoLocal("ab");
        ContextoDeSeguridadDePrueba.autenticarComo(ADMIN_SEMBRADO_ID, localId);

        turnos.findByCajeroIdAndEstado(ADMIN_SEMBRADO_ID, TurnoCaja.ABIERTO).ifPresent(t -> {
            t.cerrar(ADMIN_SEMBRADO_ID, java.time.OffsetDateTime.now());
            turnos.save(t);
        });

        var caja = consultaCajas.listar().valorOLanza().get(0).caja();
        turno = abrirTurno.ejecutar(caja.getId(), 10000L, ADMIN_SEMBRADO_ID, "Turno test").valorOLanza();

        String planId = crearPlan.ejecutar("Plan Test Abonos " + Identificador.nuevo().valor().substring(18), null,
                new PreciosPlan(800L, 1200L, 1000L), null, ADMIN_SEMBRADO_ID).valorOLanza().getId();
        pensionista = crearPensionista.ejecutar(new DatosPensionista("88776655", "Estudiante", "Prueba",
                "987654321", "alumno@correo.com", "UNI", "20240001", "Sistemas", "3", planId,
                Set.of(Comida.ALMUERZO), "semanal", new DatosResponsable("Padre Test", "11223344", "999888777",
                "padre@correo.com", "Padre", "Av. Universitaria 123")), ADMIN_SEMBRADO_ID).valorOLanza().pensionista();

        // Cargar saldo inicial con deuda de S/ 80.00 (8000 centimos)
        cargarSaldoInicial.ejecutar(pensionista.getId(), 8000L, pe.rikos.operacion.pensionistas.aplicacion.Signo.CARGO, LocalDate.now(),
                "Deuda inicial", ADMIN_SEMBRADO_ID).valorOLanza();
    }

    @Test
    void abono_en_efectivo_queda_confirmado_genera_movimiento_de_caja_y_cuenta_y_entra_en_conciliacion() {
        var solicitud = new SolicitudAbono(pensionista.getId(), "efectivo", 5000L, null, null, "Abono efectivo parcial");

        Resultado<AbonoRegistrado, ErrorAbono> res = registrarAbono.ejecutar(ADMIN_SEMBRADO_ID, solicitud);

        assertThat(res.esExito()).isTrue();
        AbonoRegistrado registrado = res.valorOLanza();
        assertThat(registrado.pensionistaDni()).isEqualTo("88776655");
        assertThat(registrado.saldoAnterior()).isEqualTo(8000L);
        assertThat(registrado.nuevoSaldo()).isEqualTo(3000L);
        assertThat(registrado.saldoAFavor()).isEqualTo(0L);
        assertThat(registrado.alertaDuplicado()).isFalse();

        Abono abono = abonos.findById(registrado.abono().getId()).orElseThrow();
        assertThat(abono.getEstado()).isEqualTo(Abono.ESTADO_APROBADO);
        assertThat(abono.getMedio()).isEqualTo(Abono.MEDIO_EFECTIVO);
        assertThat(abono.getMonto()).isEqualTo(5000L);
        assertThat(abono.getAprobadoPor()).isEqualTo(ADMIN_SEMBRADO_ID);
        assertThat(abono.getTurnoCajaId()).isEqualTo(turno.getId());

        // Movimiento de caja: registrado como abono_pensionista en efectivo
        List<MovimientoCaja> movsCaja = movimientosCaja.findByTurnoCajaId(turno.getId());
        assertThat(movsCaja).anyMatch(m -> "abono_pensionista".equals(m.getTipo())
                && "efectivo".equals(m.getMedio()) && m.getImporte() == 5000L && m.getPagoId() == null);

        // Movimiento en libro de cuenta corriente: registrado como credito
        var movCuenta = movimientosCuenta.findByAbonoId(abono.getId());
        assertThat(movCuenta).isPresent();
        assertThat(movCuenta.get().getTipo()).isEqualTo("abono");
        assertThat(movCuenta.get().getSigno()).isEqualTo("credito");
        assertThat(movCuenta.get().getImporte()).isEqualTo(5000L);

        // Conciliacion del turno: el efectivo esperado debe ser fondo inicial (10000) + abono (5000) = 15000
        ResumenDeTurno resumen = consultaCierre.resumenDe(turno);
        assertThat(resumen.efectivoCobrado()).isEqualTo(5000L);
        assertThat(resumen.efectivoEsperado()).isEqualTo(15000L);
    }

    @Test
    void abono_digital_queda_pendiente_y_no_altera_caja_ni_saldo() {
        var solicitud = new SolicitudAbono(pensionista.getId(), "yape", 3000L, "1829304", "Yape RIKO'S", "Abono digital");

        Resultado<AbonoRegistrado, ErrorAbono> res = registrarAbono.ejecutar(ADMIN_SEMBRADO_ID, solicitud);

        assertThat(res.esExito()).isTrue();
        AbonoRegistrado registrado = res.valorOLanza();
        assertThat(registrado.saldoAnterior()).isEqualTo(8000L);
        assertThat(registrado.nuevoSaldo()).isEqualTo(8000L); // No cambia el saldo

        Abono abono = abonos.findById(registrado.abono().getId()).orElseThrow();
        assertThat(abono.getEstado()).isEqualTo(Abono.ESTADO_PENDIENTE);
        assertThat(abono.getMedio()).isEqualTo(Abono.MEDIO_YAPE);
        assertThat(abono.getNumeroOperacion()).isEqualTo("1829304");
        assertThat(abono.getCuentaDestino()).isEqualTo("Yape RIKO'S");
        assertThat(abono.getAprobadoPor()).isNull();

        // No genera movimiento de caja
        List<MovimientoCaja> movsCaja = movimientosCaja.findByTurnoCajaId(turno.getId());
        assertThat(movsCaja).noneMatch(m -> "abono_pensionista".equals(m.getTipo()));

        // No genera movimiento en el libro de cuenta
        assertThat(movimientosCuenta.findByAbonoId(abono.getId())).isEmpty();

        // Conciliacion de caja: no se altera el efectivo
        ResumenDeTurno resumen = consultaCierre.resumenDe(turno);
        assertThat(resumen.efectivoCobrado()).isEqualTo(0L);
        assertThat(resumen.efectivoEsperado()).isEqualTo(10000L);
    }

    @Test
    void abono_digital_con_numero_operacion_repetido_alerta_sin_bloquear() {
        var sol1 = new SolicitudAbono(pensionista.getId(), "yape", 2000L, "OP-DUPLICADA", "Yape RIKO'S", "Primero");
        var res1 = registrarAbono.ejecutar(ADMIN_SEMBRADO_ID, sol1);
        assertThat(res1.esExito()).isTrue();
        assertThat(res1.valorOLanza().alertaDuplicado()).isFalse();

        var sol2 = new SolicitudAbono(pensionista.getId(), "yape", 1000L, "OP-DUPLICADA", "Yape RIKO'S", "Segundo");
        var res2 = registrarAbono.ejecutar(ADMIN_SEMBRADO_ID, sol2);
        assertThat(res2.esExito()).isTrue();
        assertThat(res2.valorOLanza().alertaDuplicado()).isTrue();
    }

    @Test
    void rechaza_si_no_hay_turno_abierto() {
        var solicitud = new SolicitudAbono(pensionista.getId(), "efectivo", 5000L, null, null, null);
        Resultado<AbonoRegistrado, ErrorAbono> res = registrarAbono.ejecutar("01ROTRO0000000000000000000", solicitud);
        assertThat(res.esFracaso()).isTrue();
        assertThat(error(res)).isEqualTo(ErrorAbono.SIN_TURNO_ABIERTO);
    }

    @Test
    void rechaza_si_monto_es_invalido() {
        var solCero = new SolicitudAbono(pensionista.getId(), "efectivo", 0L, null, null, null);
        assertThat(error(registrarAbono.ejecutar(ADMIN_SEMBRADO_ID, solCero))).isEqualTo(ErrorAbono.MONTO_INVALIDO);

        var solNegativo = new SolicitudAbono(pensionista.getId(), "efectivo", -500L, null, null, null);
        assertThat(error(registrarAbono.ejecutar(ADMIN_SEMBRADO_ID, solNegativo))).isEqualTo(ErrorAbono.MONTO_INVALIDO);

        var solNulo = new SolicitudAbono(pensionista.getId(), "efectivo", null, null, null, null);
        assertThat(error(registrarAbono.ejecutar(ADMIN_SEMBRADO_ID, solNulo))).isEqualTo(ErrorAbono.MONTO_INVALIDO);
    }

    @Test
    void rechaza_si_medio_digital_no_tiene_numero_operacion() {
        var solicitud = new SolicitudAbono(pensionista.getId(), "yape", 2000L, "   ", "Yape RIKO'S", null);
        Resultado<AbonoRegistrado, ErrorAbono> res = registrarAbono.ejecutar(ADMIN_SEMBRADO_ID, solicitud);
        assertThat(res.esFracaso()).isTrue();
        assertThat(error(res)).isEqualTo(ErrorAbono.NUMERO_OPERACION_REQUERIDO);
    }

    @Test
    void rechaza_si_pensionista_no_existe() {
        var solicitud = new SolicitudAbono("01NOEXISTE00000000000000000", "efectivo", 2000L, null, null, null);
        Resultado<AbonoRegistrado, ErrorAbono> res = registrarAbono.ejecutar(ADMIN_SEMBRADO_ID, solicitud);
        assertThat(res.esFracaso()).isTrue();
        assertThat(error(res)).isEqualTo(ErrorAbono.PENSIONISTA_NO_ENCONTRADO);
    }

    @Test
    void permite_abonar_a_pensionista_inactivo() {
        cambiarEstadoPensionista.ejecutar(pensionista.getId(), false, ADMIN_SEMBRADO_ID);

        var solicitud = new SolicitudAbono(pensionista.getId(), "efectivo", 4000L, null, null, "Saldando deuda");
        Resultado<AbonoRegistrado, ErrorAbono> res = registrarAbono.ejecutar(ADMIN_SEMBRADO_ID, solicitud);

        assertThat(res.esExito()).isTrue();
        assertThat(res.valorOLanza().nuevoSaldo()).isEqualTo(4000L); // Deuda se redujo de 8000 a 4000
    }

    private static ErrorAbono error(Resultado<?, ErrorAbono> res) {
        assertThat(res.esFracaso()).isTrue();
        return ((Resultado.Fracaso<?, ErrorAbono>) res).error();
    }
}
