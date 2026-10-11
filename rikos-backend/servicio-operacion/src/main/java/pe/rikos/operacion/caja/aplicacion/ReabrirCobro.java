package pe.rikos.operacion.caja.aplicacion;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.rikos.contratos.Resultado;
import pe.rikos.operacion.caja.infraestructura.Cobro;
import pe.rikos.operacion.caja.infraestructura.CobroCargo;
import pe.rikos.operacion.caja.infraestructura.CobroCargoRepositorio;
import pe.rikos.operacion.caja.infraestructura.CobroLinea;
import pe.rikos.operacion.caja.infraestructura.CobroLineaRepositorio;
import pe.rikos.operacion.caja.infraestructura.CobroRepositorio;
import pe.rikos.operacion.caja.infraestructura.TurnoCaja;
import pe.rikos.operacion.caja.infraestructura.TurnoCajaRepositorio;
import pe.rikos.operacion.catalogo.infraestructura.Motivo;
import pe.rikos.operacion.catalogo.infraestructura.MotivoRepositorio;
import pe.rikos.operacion.comun.auditoria.RegistradorAuditoria;
import pe.rikos.operacion.pensionistas.aplicacion.ErrorCuenta;
import pe.rikos.operacion.pensionistas.aplicacion.RegistrarAjuste;
import pe.rikos.operacion.pensionistas.aplicacion.Signo;
import pe.rikos.operacion.pensionistas.infraestructura.MovimientoCuenta;
import pe.rikos.operacion.venta.aplicacion.AbrirPedido;
import pe.rikos.operacion.venta.infraestructura.DetallePedido;
import pe.rikos.operacion.venta.infraestructura.DetallePedidoRepositorio;
import pe.rikos.operacion.venta.infraestructura.Pedido;
import pe.rikos.operacion.venta.infraestructura.PedidoRepositorio;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * US-035: reabre un cobro MIENTRAS su turno de origen siga abierto (RN-073). Las lineas del cobro vuelven a "comandado" y se
 * pueden cobrar de nuevo, el pedido se reabre si el cobro lo habia cerrado (la mesa se ocupa otra vez), el cargo al
 * pensionista se revierte con un ajuste a favor y el dinero del cobro deja de contar en el turno. El cobro no se borra: queda
 * revertido con quien, cuando y por que. Con el turno cerrado no se reabre: la correccion es una operacion compensatoria
 * (sprint 11, con el comprobante electronico). El permiso (caja.reabrir) lo exige el controlador.
 */
@Service
public class ReabrirCobro {

    /** El motivo de ajuste de cuenta sembrado en V28: "Correccion de cobro". */
    static final String MOTIVO_AJUSTE_CORRECCION = "01M39A0MT0000000000000D002";

    private final CobroRepositorio cobros;
    private final CobroLineaRepositorio cobroLineas;
    private final CobroCargoRepositorio cargos;
    private final TurnoCajaRepositorio turnos;
    private final PedidoRepositorio pedidos;
    private final DetallePedidoRepositorio lineas;
    private final MotivoRepositorio motivos;
    private final RegistrarAjuste registrarAjuste;
    private final RegistradorAuditoria auditoria;

    public ReabrirCobro(CobroRepositorio cobros, CobroLineaRepositorio cobroLineas, CobroCargoRepositorio cargos,
                        TurnoCajaRepositorio turnos, PedidoRepositorio pedidos, DetallePedidoRepositorio lineas,
                        MotivoRepositorio motivos, RegistrarAjuste registrarAjuste, RegistradorAuditoria auditoria) {
        this.cobros = cobros;
        this.cobroLineas = cobroLineas;
        this.cargos = cargos;
        this.turnos = turnos;
        this.pedidos = pedidos;
        this.lineas = lineas;
        this.motivos = motivos;
        this.registrarAjuste = registrarAjuste;
        this.auditoria = auditoria;
    }

    @Transactional
    public Resultado<Cobro, ErrorCobro> ejecutar(String cobroId, String motivoId, String observacion, String actorId) {
        String localId = LocalDeSesion.actual().orElse(null);
        if (localId == null) {
            return Resultado.fracaso(ErrorCobro.SIN_LOCAL_ACTIVO);
        }
        Cobro cobro = cobroId == null ? null : cobros.findByIdAndLocalId(cobroId, localId).orElse(null);
        if (cobro == null) {
            return Resultado.fracaso(ErrorCobro.COBRO_NO_ENCONTRADO);
        }
        if (cobro.estaRevertido()) {
            return Resultado.fracaso(ErrorCobro.COBRO_YA_REVERTIDO);
        }
        TurnoCaja turno = turnos.findById(cobro.getTurnoCajaId()).orElse(null);
        if (turno == null || !TurnoCaja.ABIERTO.equals(turno.getEstado())) {
            return Resultado.fracaso(ErrorCobro.TURNO_CERRADO);
        }
        Motivo motivo = motivoId == null ? null : motivos.findById(motivoId).orElse(null);
        if (motivo == null) {
            return Resultado.fracaso(ErrorCobro.MOTIVO_NO_ENCONTRADO);
        }
        if (!motivo.isActivo() || !"reapertura".equals(motivo.getTipo())) {
            return Resultado.fracaso(ErrorCobro.MOTIVO_INVALIDO);
        }
        String nota = observacion == null || observacion.isBlank() ? null : observacion.strip();
        if (motivo.isRequiereObservacion() && nota == null) {
            return Resultado.fracaso(ErrorCobro.OBSERVACION_OBLIGATORIA);
        }
        if (nota != null && nota.length() > 250) {
            return Resultado.fracaso(ErrorCobro.OBSERVACION_OBLIGATORIA);
        }

        // El pedido se toma con bloqueo: no puede cobrarse otra cosa mientras se reabre.
        Pedido pedido = pedidos.bloquearPorIdYLocal(cobro.getPedidoId(), localId).orElse(null);
        if (pedido == null) {
            return Resultado.fracaso(ErrorCobro.PEDIDO_NO_ENCONTRADO);
        }
        boolean estabaCerrado = "cerrado".equals(pedido.getEstado());
        if (estabaCerrado && pedido.getMesaId() != null
                && pedidos.findByMesaIdAndEstadoIn(pedido.getMesaId(), AbrirPedido.ESTADOS_EN_CURSO).isPresent()) {
            return Resultado.fracaso(ErrorCobro.MESA_OCUPADA);
        }

        OffsetDateTime ahora = OffsetDateTime.now();
        List<CobroLinea> delCobro = cobroLineas.findByCobroId(cobro.getId());
        for (CobroLinea cl : delCobro) {
            cl.dejarDeSerVigente();
            cobroLineas.save(cl);
            DetallePedido linea = lineas.findById(cl.getDetallePedidoId()).orElseThrow();
            linea.reabrirCobro();
            lineas.save(linea);
        }
        // El cargo al pensionista se devuelve con un ajuste a favor (el libro es de solo agregado).
        for (CobroCargo cargo : cargos.findByCobroId(cobro.getId())) {
            Resultado<MovimientoCuenta, ErrorCuenta> ajuste = registrarAjuste.ejecutar(cargo.getPensionistaId(),
                    cargo.getImporte(), Signo.CREDITO, MOTIVO_AJUSTE_CORRECCION,
                    "Reapertura del pedido " + pedido.getNumero(), actorId);
            if (ajuste.esFracaso()) {
                throw new IllegalStateException("No se pudo revertir el cargo al pensionista: " + ajuste);
            }
        }
        cobro.revertir(actorId, motivo.getId(), nota, ahora);
        cobros.save(cobro);
        if (estabaCerrado) {
            pedido.reabrir();
            pedidos.save(pedido);
        }
        auditoria.registrar("reapertura", "pedido", pedido.getId(),
                RegistradorAuditoria.campos("estado", estabaCerrado ? "cerrado" : pedido.getEstado(), "cobrado", cobro.getTotal()),
                RegistradorAuditoria.campos("nombre", "Pedido " + pedido.getNumero(), "estado", pedido.getEstado(),
                        "cobroRevertido", cobro.getTotal(), "lineas", delCobro.size(), "motivo", motivo.getDescripcion()),
                motivo.getId(), nota);
        return Resultado.exito(cobro);
    }
}
