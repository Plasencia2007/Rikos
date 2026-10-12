package pe.rikos.operacion.caja.aplicacion;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.rikos.contratos.Identificador;
import pe.rikos.contratos.Resultado;
import pe.rikos.operacion.caja.infraestructura.MovimientoCaja;
import pe.rikos.operacion.caja.infraestructura.MovimientoCajaRepositorio;
import pe.rikos.operacion.caja.infraestructura.TurnoCaja;
import pe.rikos.operacion.caja.infraestructura.TurnoCajaRepositorio;
import pe.rikos.operacion.comun.auditoria.RegistradorAuditoria;
import pe.rikos.operacion.identidad.infraestructura.Local;
import pe.rikos.operacion.identidad.infraestructura.LocalRepositorio;
import pe.rikos.operacion.identidad.infraestructura.Usuario;
import pe.rikos.operacion.identidad.infraestructura.UsuarioRepositorio;
import pe.rikos.operacion.pensionistas.aplicacion.AcreditarAbono;
import pe.rikos.operacion.pensionistas.aplicacion.ConsultaCuenta;
import pe.rikos.operacion.pensionistas.infraestructura.Abono;
import pe.rikos.operacion.pensionistas.infraestructura.AbonoRepositorio;
import pe.rikos.operacion.pensionistas.infraestructura.Pensionista;
import pe.rikos.operacion.pensionistas.infraestructura.PensionistaRepositorio;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Set;

/**
 * US-021: Registrar abonos del pensionista en caja (efectivo y digitales).
 * - Efectivo (RN-030, CA1): Aprobado de inmediato, entra en conciliacion del turno (movimiento_caja),
 *   y actualiza el saldo de la cuenta del pensionista (movimiento_cuenta).
 * - Digitales (Yape, Plin, transferencia) (RN-031, CA2): Queda pendiente hasta aprobacion por admin (US-024),
 *   sin movimiento_caja ni de cuenta.
 * - Parciales (RN-033, CA3): Se permite cualquier monto > 0.
 * - Inactivo (RN-034): Se acepta el abono para amortizar deuda.
 * - No genera venta tributaria (RN-035, CA4): Emite constancia interna.
 */
@Service
public class RegistrarAbonoEnCaja {

    private static final Set<String> MEDIOS_VALIDOS = Set.of(
            Abono.MEDIO_EFECTIVO, Abono.MEDIO_YAPE, Abono.MEDIO_PLIN, Abono.MEDIO_TRANSFERENCIA
    );

    private final TurnoCajaRepositorio turnos;
    private final MovimientoCajaRepositorio movimientosCaja;
    private final AbonoRepositorio abonos;
    private final PensionistaRepositorio pensionistas;
    private final AcreditarAbono acreditarAbono;
    private final ConsultaCuenta consultaCuenta;
    private final RegistradorAuditoria auditoria;
    private final UsuarioRepositorio usuarios;
    private final LocalRepositorio locales;

    public RegistrarAbonoEnCaja(TurnoCajaRepositorio turnos, MovimientoCajaRepositorio movimientosCaja,
                                AbonoRepositorio abonos, PensionistaRepositorio pensionistas,
                                AcreditarAbono acreditarAbono, ConsultaCuenta consultaCuenta,
                                RegistradorAuditoria auditoria, UsuarioRepositorio usuarios,
                                LocalRepositorio locales) {
        this.turnos = turnos;
        this.movimientosCaja = movimientosCaja;
        this.abonos = abonos;
        this.pensionistas = pensionistas;
        this.acreditarAbono = acreditarAbono;
        this.consultaCuenta = consultaCuenta;
        this.auditoria = auditoria;
        this.usuarios = usuarios;
        this.locales = locales;
    }

    @Transactional
    public Resultado<AbonoRegistrado, ErrorAbono> ejecutar(String cajeroId, SolicitudAbono solicitud) {
        String localId = LocalDeSesion.actual().orElse(null);
        if (localId == null) {
            return Resultado.fracaso(ErrorAbono.SIN_LOCAL_ACTIVO);
        }

        TurnoCaja turno = turnos.findByCajeroIdAndEstado(cajeroId, TurnoCaja.ABIERTO)
                .filter(t -> t.getLocalId().equals(localId))
                .orElse(null);
        if (turno == null) {
            return Resultado.fracaso(ErrorAbono.SIN_TURNO_ABIERTO);
        }

        if (solicitud.monto() == null || solicitud.monto() <= 0) {
            return Resultado.fracaso(ErrorAbono.MONTO_INVALIDO);
        }

        if (solicitud.medio() == null || !MEDIOS_VALIDOS.contains(solicitud.medio().toLowerCase())) {
            return Resultado.fracaso(ErrorAbono.MEDIO_INVALIDO);
        }

        String medio = solicitud.medio().toLowerCase();
        boolean esEfectivo = Abono.MEDIO_EFECTIVO.equals(medio);

        String numOp = solicitud.numeroOperacion() != null ? solicitud.numeroOperacion().strip() : null;
        if (!esEfectivo && (numOp == null || numOp.isBlank())) {
            return Resultado.fracaso(ErrorAbono.NUMERO_OPERACION_REQUERIDO);
        }

        Pensionista p = pensionistas.findById(solicitud.pensionistaId()).orElse(null);
        if (p == null) {
            return Resultado.fracaso(ErrorAbono.PENSIONISTA_NO_ENCONTRADO);
        }

        // RN-032: Alerta si el numero de operacion ya existe para ese medio (sin bloquear)
        boolean alertaDuplicado = false;
        if (numOp != null && !numOp.isBlank()) {
            alertaDuplicado = abonos.existsByNumeroOperacionAndMedio(numOp, medio);
        }

        // Saldo previo
        long saldoAnterior = consultaCuenta.resumen(p.getId())
                .map(r -> r.deuda())
                .orElse(0L);

        OffsetDateTime ahora = OffsetDateTime.now();
        String abonoId = Identificador.nuevo().valor();
        String cuentaDestino = solicitud.cuentaDestino() != null && !solicitud.cuentaDestino().isBlank()
                ? solicitud.cuentaDestino().strip() : null;
        String nota = solicitud.observacion() != null && !solicitud.observacion().isBlank()
                ? solicitud.observacion().strip() : null;

        Abono abono;
        if (esEfectivo) {
            abono = new Abono(abonoId, p.getId(), localId, medio, solicitud.monto(),
                    LocalDate.now(), null, null, null, Abono.ORIGEN_CAJA,
                    Abono.ESTADO_APROBADO, cajeroId, cajeroId, ahora, null, turno.getId(), nota, ahora);
            abonos.save(abono);

            // RN-069, RN-030: Movimiento de caja en efectivo para el turno
            movimientosCaja.save(new MovimientoCaja(Identificador.nuevo().valor(), turno.getId(), localId,
                    "abono_pensionista", "efectivo", solicitud.monto(), null, ahora, cajeroId));

            // Acreditar al libro de cuenta corriente
            acreditarAbono.ejecutar(null, p.getId(), localId, solicitud.monto(), abonoId,
                    "Abono en efectivo en caja - Turno " + turno.getId(),
                    ahora, cajeroId);

            // Auditoria
            auditoria.registrar("aprobacion_abono", "abono", abonoId, null,
                    RegistradorAuditoria.campos("monto", abono.getMonto(), "medio", "efectivo", "pensionista", p.getId()),
                    null, nota);
        } else {
            abono = new Abono(abonoId, p.getId(), localId, medio, solicitud.monto(),
                    LocalDate.now(), numOp, cuentaDestino, null, Abono.ORIGEN_CAJA,
                    Abono.ESTADO_PENDIENTE, cajeroId, null, null, null, turno.getId(), nota, ahora);
            abonos.save(abono);
        }

        // Saldo resultante
        var resPost = consultaCuenta.resumen(p.getId()).orElse(null);
        long nuevoSaldo = resPost != null ? resPost.deuda() : saldoAnterior;
        long saldoAFavor = resPost != null ? resPost.saldoAFavor() : 0L;

        String localNombre = locales.findById(localId).map(Local::getNombre).orElse("RIKO'S");
        String cajeroNombre = usuarios.findById(cajeroId)
                .map(u -> (u.getNombres() != null ? u.getNombres() + " " + u.getApellidos() : u.getNombreUsuario()).strip())
                .orElse("Cajero");
        String leyenda = "Constancia interna de abono a cuenta corriente. No constituye comprobante de pago.";

        var constancia = new AbonoRegistrado.ConstanciaAbono(localNombre, cajeroNombre, ahora, leyenda);

        return Resultado.exito(new AbonoRegistrado(abono, p.getNombreCompleto(), p.getDni(),
                saldoAnterior, nuevoSaldo, saldoAFavor, alertaDuplicado, constancia));
    }
}
