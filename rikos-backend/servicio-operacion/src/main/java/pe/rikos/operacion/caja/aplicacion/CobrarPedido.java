package pe.rikos.operacion.caja.aplicacion;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.rikos.contratos.Identificador;
import pe.rikos.contratos.Resultado;
import pe.rikos.operacion.caja.infraestructura.Cobro;
import pe.rikos.operacion.caja.infraestructura.CobroCargo;
import pe.rikos.operacion.caja.infraestructura.CobroCargoRepositorio;
import pe.rikos.operacion.caja.infraestructura.CobroLinea;
import pe.rikos.operacion.caja.infraestructura.CobroLineaRepositorio;
import pe.rikos.operacion.caja.infraestructura.CobroRepositorio;
import pe.rikos.operacion.caja.infraestructura.MovimientoCaja;
import pe.rikos.operacion.caja.infraestructura.MovimientoCajaRepositorio;
import pe.rikos.operacion.caja.infraestructura.Pago;
import pe.rikos.operacion.caja.infraestructura.PagoRepositorio;
import pe.rikos.operacion.caja.infraestructura.TurnoCaja;
import pe.rikos.operacion.caja.infraestructura.TurnoCajaRepositorio;
import pe.rikos.operacion.catalogo.infraestructura.Motivo;
import pe.rikos.operacion.catalogo.infraestructura.MotivoRepositorio;
import pe.rikos.operacion.catalogo.infraestructura.Producto;
import pe.rikos.operacion.catalogo.infraestructura.ProductoRepositorio;
import pe.rikos.operacion.catalogo.infraestructura.Receta;
import pe.rikos.operacion.catalogo.infraestructura.RecetaRepositorio;
import pe.rikos.operacion.comun.auditoria.RegistradorAuditoria;
import pe.rikos.operacion.pensionistas.aplicacion.ConsultaCuenta;
import pe.rikos.operacion.pensionistas.aplicacion.ErrorCuenta;
import pe.rikos.operacion.pensionistas.aplicacion.RegistrarConsumo;
import pe.rikos.operacion.pensionistas.aplicacion.ResumenDeCuenta;
import pe.rikos.operacion.pensionistas.infraestructura.MovimientoCuenta;
import pe.rikos.operacion.pensionistas.infraestructura.Pensionista;
import pe.rikos.operacion.pensionistas.infraestructura.PensionistaRepositorio;
import pe.rikos.operacion.venta.aplicacion.CerrarPedido;
import pe.rikos.operacion.venta.infraestructura.CuentaPersona;
import pe.rikos.operacion.venta.infraestructura.CuentaPersonaRepositorio;
import pe.rikos.operacion.venta.infraestructura.DetallePedido;
import pe.rikos.operacion.venta.infraestructura.DetallePedidoComponente;
import pe.rikos.operacion.venta.infraestructura.DetallePedidoComponenteRepositorio;
import pe.rikos.operacion.venta.infraestructura.DetallePedidoRepositorio;
import pe.rikos.operacion.venta.infraestructura.Pedido;
import pe.rikos.operacion.venta.infraestructura.PedidoRepositorio;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * US-033: cobra LINEAS de un pedido en el turno abierto del cajero. Es lo que completa al "Terminar pedido" del sprint 6.
 *
 * <p>Las lineas de clientes normales se pagan en medios (efectivo con vuelto, yape, plin, tarjeta); las de un pensionista
 * se cargan a su cuenta corriente y no piden medios (RN-015). Todo ocurre en una sola transaccion: el cobro, el snapshot
 * de cada linea (precio, descuento, costo de la receta, utilidad), los pagos, los movimientos de caja, los cargos y el
 * cierre del pedido si ya no queda nada por cobrar (la mesa queda libre porque su estado se deriva de los pedidos).
 */
@Service
public class CobrarPedido {

    private static final Set<String> MEDIOS = Set.of("efectivo", "yape", "plin", "tarjeta");
    private static final ObjectMapper JSON = new ObjectMapper();
    /** S/ 1'000,000.00 en centimos: un tope contra montos descomunales que desbordarian las sumas. */
    private static final long MONTO_MAXIMO = 100_000_000L;

    private final PedidoRepositorio pedidos;
    private final CuentaPersonaRepositorio personas;
    private final DetallePedidoRepositorio lineas;
    private final DetallePedidoComponenteRepositorio componentes;
    private final TurnoCajaRepositorio turnos;
    private final CobroRepositorio cobros;
    private final CobroLineaRepositorio cobroLineas;
    private final PagoRepositorio pagos;
    private final MovimientoCajaRepositorio movimientos;
    private final CobroCargoRepositorio cargos;
    private final ProductoRepositorio productos;
    private final RecetaRepositorio recetas;
    private final MotivoRepositorio motivos;
    private final PensionistaRepositorio pensionistas;
    private final RegistrarConsumo registrarConsumo;
    private final ConsultaCuenta consultaCuenta;
    private final CerrarPedido cerrarPedido;
    private final RegistradorAuditoria auditoria;

    public CobrarPedido(PedidoRepositorio pedidos, CuentaPersonaRepositorio personas, DetallePedidoRepositorio lineas,
                        DetallePedidoComponenteRepositorio componentes, TurnoCajaRepositorio turnos,
                        CobroRepositorio cobros, CobroLineaRepositorio cobroLineas, PagoRepositorio pagos,
                        MovimientoCajaRepositorio movimientos, CobroCargoRepositorio cargos,
                        ProductoRepositorio productos, RecetaRepositorio recetas, MotivoRepositorio motivos,
                        PensionistaRepositorio pensionistas,
                        RegistrarConsumo registrarConsumo, ConsultaCuenta consultaCuenta, CerrarPedido cerrarPedido,
                        RegistradorAuditoria auditoria) {
        this.pedidos = pedidos;
        this.personas = personas;
        this.lineas = lineas;
        this.componentes = componentes;
        this.turnos = turnos;
        this.cobros = cobros;
        this.cobroLineas = cobroLineas;
        this.pagos = pagos;
        this.movimientos = movimientos;
        this.cargos = cargos;
        this.productos = productos;
        this.recetas = recetas;
        this.motivos = motivos;
        this.pensionistas = pensionistas;
        this.registrarConsumo = registrarConsumo;
        this.consultaCuenta = consultaCuenta;
        this.cerrarPedido = cerrarPedido;
        this.auditoria = auditoria;
    }

    /** Cobra sin descuentos. */
    public Resultado<CobroRegistrado, ErrorCobro> ejecutar(String pedidoId, List<String> lineaIds,
                                                           List<PagoSolicitado> pagosPedidos, String cajeroId) {
        return ejecutar(pedidoId, lineaIds, pagosPedidos, List.of(), cajeroId);
    }

    @Transactional
    public Resultado<CobroRegistrado, ErrorCobro> ejecutar(String pedidoId, List<String> lineaIds,
                                                           List<PagoSolicitado> pagosPedidos,
                                                           List<DescuentoSolicitado> descuentosPedidos, String cajeroId) {
        String localId = LocalDeSesion.actual().orElse(null);
        if (localId == null) {
            return Resultado.fracaso(ErrorCobro.SIN_LOCAL_ACTIVO);
        }
        // Con bloqueo de escritura: dos cajeros cobrando partes del mismo pedido se turnan (auditoria CAJ-03).
        Pedido pedido = pedidoId == null ? null : pedidos.bloquearPorIdYLocal(pedidoId, localId).orElse(null);
        if (pedido == null) {
            return Resultado.fracaso(ErrorCobro.PEDIDO_NO_ENCONTRADO);
        }
        if ("cerrado".equals(pedido.getEstado())) {
            return Resultado.fracaso(ErrorCobro.PEDIDO_CERRADO);
        }
        TurnoCaja turno = turnos.findByCajeroIdAndEstado(cajeroId, TurnoCaja.ABIERTO)
                .filter(t -> t.getLocalId().equals(localId)).orElse(null);
        if (turno == null) {
            return Resultado.fracaso(ErrorCobro.SIN_TURNO_ABIERTO);
        }

        // ---- las lineas ----
        List<String> pedidas = lineaIds == null ? List.of() : lineaIds.stream().distinct().toList();
        if (pedidas.isEmpty()) {
            return Resultado.fracaso(ErrorCobro.SIN_LINEAS);
        }
        List<CuentaPersona> delPedido = personas.findByPedidoIdOrderByOrden(pedidoId);
        Map<String, CuentaPersona> personaPorId = delPedido.stream()
                .collect(Collectors.toMap(CuentaPersona::getId, Function.identity()));
        List<DetallePedido> todas = lineas.findByCuentaPersonaIdInOrderByCreadoEn(personaPorId.keySet());
        Map<String, DetallePedido> lineaPorId = todas.stream()
                .collect(Collectors.toMap(DetallePedido::getId, Function.identity()));
        List<DetallePedido> aCobrar = new ArrayList<>();
        for (String id : pedidas) {
            DetallePedido linea = lineaPorId.get(id);
            if (linea == null) {
                return Resultado.fracaso(ErrorCobro.LINEA_NO_ENCONTRADA);
            }
            switch (linea.getEstado()) {
                case "cobrado" -> {
                    return Resultado.fracaso(ErrorCobro.LINEA_YA_COBRADA);
                }
                case "anulado" -> {
                    return Resultado.fracaso(ErrorCobro.LINEA_ANULADA);
                }
                case "pendiente" -> {
                    return Resultado.fracaso(ErrorCobro.LINEA_SIN_COMANDAR);
                }
                default -> aCobrar.add(linea);
            }
        }

        // ---- los descuentos (US-034): por linea, con motivo; se validan antes de calcular nada ----
        Resultado<Map<String, DescuentoAplicado>, ErrorCobro> descuentos = validarDescuentos(descuentosPedidos, aCobrar);
        if (descuentos.esFracaso()) {
            return Resultado.fracaso(((Resultado.Fracaso<Map<String, DescuentoAplicado>, ErrorCobro>) descuentos).error());
        }
        Map<String, DescuentoAplicado> descuentoPorLinea = descuentos.valorOLanza();

        long totalMedios = 0;
        long totalCuentas = 0;
        Map<String, Long> porPensionista = new LinkedHashMap<>();
        for (DetallePedido l : aCobrar) {
            long importe = importeDe(l) - descuentoPorLinea.getOrDefault(l.getId(), DescuentoAplicado.NINGUNO).monto();
            CuentaPersona persona = personaPorId.get(l.getCuentaPersonaId());
            if (persona.getPensionistaId() != null) {
                totalCuentas += importe;
                porPensionista.merge(persona.getPensionistaId(), importe, Long::sum);
            } else {
                totalMedios += importe;
            }
        }

        if (totalMedios + totalCuentas <= 0) {
            return Resultado.fracaso(ErrorCobro.TOTAL_EN_CERO);
        }

        // ---- los pagos (se validan antes de escribir nada) ----
        List<PagoSolicitado> pagosDados = pagosPedidos == null ? List.of() : pagosPedidos;
        Resultado<Long, ErrorCobro> vuelto = validarPagos(pagosDados, totalMedios);
        if (vuelto.esFracaso()) {
            return Resultado.fracaso(((Resultado.Fracaso<Long, ErrorCobro>) vuelto).error());
        }

        // ---- el cargo a las cuentas: un pensionista desactivado no consume ----
        Map<String, Pensionista> pensionistasPorId = pensionistas.findAllById(porPensionista.keySet()).stream()
                .collect(Collectors.toMap(Pensionista::getId, Function.identity()));
        for (String pensionistaId : porPensionista.keySet()) {
            Pensionista p = pensionistasPorId.get(pensionistaId);
            if (p == null || !p.isActivo()) {
                return Resultado.fracaso(ErrorCobro.PENSIONISTA_INACTIVO);
            }
        }

        // ---- todo valido: se escribe ----
        OffsetDateTime ahora = OffsetDateTime.now();
        Cobro cobro = cobros.save(new Cobro(Identificador.nuevo().valor(), pedidoId, localId, turno.getId(), cajeroId, ahora,
                totalMedios + totalCuentas, totalMedios, totalCuentas, vuelto.valorOLanza()));

        Map<String, String> nombres = nombresDeProductos(aCobrar);
        for (DetallePedido l : aCobrar) {
            CuentaPersona persona = personaPorId.get(l.getCuentaPersonaId());
            DescuentoAplicado descuento = descuentoPorLinea.getOrDefault(l.getId(), DescuentoAplicado.NINGUNO);
            cobroLineas.save(snapshotDe(cobro, l, persona, nombres, descuento));
            if (descuento.monto() > 0) {
                auditoria.registrar("descuento", "detalle_pedido", l.getId(),
                        RegistradorAuditoria.campos("importe", importeDe(l)),
                        RegistradorAuditoria.campos("nombre", "Pedido " + pedido.getNumero(), "producto",
                                nombres.getOrDefault(l.getProductoId(), String.valueOf(l.getDescripcion())), "tipo",
                                descuento.tipo(), "valor", descuento.valor(), "descuento", descuento.monto(), "importe",
                                importeDe(l) - descuento.monto(), "motivo", descuento.motivoDescripcion()),
                        descuento.motivoId(), descuento.observacion());
            }
            l.cobrar();
            lineas.save(l);
        }

        List<Pago> pagosGuardados = new ArrayList<>();
        // El efectivo absorbe el vuelto: lo digital entra completo y el efectivo entra por lo que falta.
        long digital = pagosDados.stream().filter(p -> !"efectivo".equals(p.medio())).mapToLong(PagoSolicitado::monto).sum();
        long efectivoQueEntra = totalMedios - digital;
        for (PagoSolicitado dado : pagosDados) {
            boolean efectivo = "efectivo".equals(dado.medio());
            long importe = efectivo ? efectivoQueEntra : dado.monto();
            if (efectivo && importe <= 0) {
                continue;
            }
            String referencia = dado.referencia() == null || dado.referencia().isBlank() ? null : dado.referencia().strip();
            Pago pago = pagos.save(new Pago(Identificador.nuevo().valor(), cobro.getId(), dado.medio(), importe,
                    efectivo ? dado.monto() : null, referencia));
            pagosGuardados.add(pago);
            movimientos.save(new MovimientoCaja(Identificador.nuevo().valor(), turno.getId(), localId, "venta", dado.medio(),
                    importe, pago.getId(), ahora, cajeroId));
            if (efectivo) {
                efectivoQueEntra = 0;
            }
        }

        List<CargoRegistrado> cargosHechos = new ArrayList<>();
        for (var entrada : porPensionista.entrySet()) {
            String pensionistaId = entrada.getKey();
            String movimientoId = Identificador.nuevo().valor();
            Resultado<MovimientoCuenta, ErrorCuenta> consumo = registrarConsumo.ejecutar(movimientoId, pensionistaId, localId,
                    entrada.getValue(), pedidoId, ahora, cajeroId);
            if (consumo.esFracaso()) {
                // Ya se valido que esta activo; si aun asi falla, se deshace todo el cobro.
                throw new IllegalStateException("No se pudo cargar el consumo a la cuenta: " + consumo);
            }
            boolean supera = consultaCuenta.resumen(pensionistaId).map(ResumenDeCuenta::superaTope).orElse(false);
            cargos.save(new CobroCargo(Identificador.nuevo().valor(), cobro.getId(), pensionistaId, movimientoId,
                    entrada.getValue(), supera));
            cargosHechos.add(new CargoRegistrado(pensionistaId, pensionistasPorId.get(pensionistaId).nombreCompleto(),
                    entrada.getValue(), supera));
        }

        boolean cerrado = CerrarPedido.sinNadaPorCobrar(todas);
        if (cerrado) {
            cerrarPedido.cerrar(pedido, cajeroId);
        }
        auditoria.registrar("cobro_registrado", "pedido", pedido.getId(), null,
                RegistradorAuditoria.campos("nombre", "Pedido " + pedido.getNumero(), "total", cobro.getTotal(),
                        "medios", totalMedios, "cuentas", totalCuentas, "lineas", aCobrar.size(),
                        "pedidoCerrado", cerrado));
        return Resultado.exito(new CobroRegistrado(cobro, pagosGuardados, cargosHechos, cerrado));
    }

    /** Devuelve el vuelto, o el error del primer pago que no cuadra. */
    private static Resultado<Long, ErrorCobro> validarPagos(List<PagoSolicitado> dados, long totalMedios) {
        Set<String> vistos = new HashSet<>();
        for (PagoSolicitado p : dados) {
            if (p == null || p.medio() == null || !MEDIOS.contains(p.medio())) {
                return Resultado.fracaso(ErrorCobro.MEDIO_INVALIDO);
            }
            // Un pago por medio: dos en efectivo harian ambiguo cual fue lo recibido (auditoria CAJ-01).
            if (!vistos.add(p.medio())) {
                return Resultado.fracaso(ErrorCobro.MEDIO_REPETIDO);
            }
            if (p.monto() <= 0 || p.monto() > MONTO_MAXIMO) {
                return Resultado.fracaso(ErrorCobro.MONTO_INVALIDO);
            }
            if (p.referencia() != null && p.referencia().strip().length() > 50) {
                return Resultado.fracaso(ErrorCobro.REFERENCIA_INVALIDA);
            }
        }
        if (totalMedios == 0) {
            return dados.isEmpty() ? Resultado.exito(0L) : Resultado.fracaso(ErrorCobro.PAGO_SIN_NADA_QUE_PAGAR);
        }
        long digital = dados.stream().filter(p -> !"efectivo".equals(p.medio())).mapToLong(PagoSolicitado::monto).sum();
        long efectivo = dados.stream().filter(p -> "efectivo".equals(p.medio())).mapToLong(PagoSolicitado::monto).sum();
        if (digital > totalMedios) {
            return Resultado.fracaso(ErrorCobro.PAGO_EXCEDE_EL_TOTAL);
        }
        if (digital + efectivo < totalMedios) {
            return Resultado.fracaso(ErrorCobro.PAGO_INSUFICIENTE);
        }
        // Lo digital mas lo recibido en efectivo pasa del total solo si hay efectivo: ese exceso es el vuelto.
        return Resultado.exito(digital + efectivo - totalMedios);
    }

    /** Un descuento ya validado y calculado, en centimos. */
    private record DescuentoAplicado(long monto, String tipo, long valor, String motivoId, String motivoDescripcion,
                                     String observacion) {
        static final DescuentoAplicado NINGUNO = new DescuentoAplicado(0, null, 0, null, null, null);
    }

    /** Valida los descuentos pedidos y devuelve, por linea, lo que se descuenta. */
    private Resultado<Map<String, DescuentoAplicado>, ErrorCobro> validarDescuentos(List<DescuentoSolicitado> pedidos,
                                                                                    List<DetallePedido> aCobrar) {
        Map<String, DescuentoAplicado> resultado = new LinkedHashMap<>();
        if (pedidos == null) {
            return Resultado.exito(resultado);
        }
        Map<String, DetallePedido> cobrables = aCobrar.stream()
                .collect(Collectors.toMap(DetallePedido::getId, Function.identity()));
        for (DescuentoSolicitado d : pedidos) {
            if (d == null || d.lineaId() == null || !cobrables.containsKey(d.lineaId()) || resultado.containsKey(d.lineaId())) {
                return Resultado.fracaso(ErrorCobro.DESCUENTO_INVALIDO);
            }
            boolean monto = "monto".equals(d.tipo());
            if (!monto && !"porcentaje".equals(d.tipo())) {
                return Resultado.fracaso(ErrorCobro.DESCUENTO_INVALIDO);
            }
            long bruto = importeDe(cobrables.get(d.lineaId()));
            if (d.valor() <= 0 || (!monto && d.valor() > 100)) {
                return Resultado.fracaso(ErrorCobro.DESCUENTO_INVALIDO);
            }
            long descuento = monto ? d.valor() : (bruto * d.valor() + 50) / 100;
            if (descuento <= 0 || descuento > bruto) {
                return Resultado.fracaso(ErrorCobro.DESCUENTO_INVALIDO);
            }
            Motivo motivo = d.motivoId() == null ? null : motivos.findById(d.motivoId()).orElse(null);
            if (motivo == null) {
                return Resultado.fracaso(ErrorCobro.MOTIVO_NO_ENCONTRADO);
            }
            if (!motivo.isActivo() || !"descuento".equals(motivo.getTipo())) {
                return Resultado.fracaso(ErrorCobro.MOTIVO_INVALIDO);
            }
            String nota = d.observacion() == null || d.observacion().isBlank() ? null : d.observacion().strip();
            if (motivo.isRequiereObservacion() && nota == null) {
                return Resultado.fracaso(ErrorCobro.OBSERVACION_OBLIGATORIA);
            }
            if (nota != null && nota.length() > 250) {
                return Resultado.fracaso(ErrorCobro.OBSERVACION_OBLIGATORIA);
            }
            resultado.put(d.lineaId(), new DescuentoAplicado(descuento, d.tipo(), d.valor(), motivo.getId(),
                    motivo.getDescripcion(), nota));
        }
        return Resultado.exito(resultado);
    }

    /** Lo que vale la linea ANTES del descuento: precio y taper por cantidad. */
    private static long importeDe(DetallePedido l) {
        return (l.getPrecioUnitario() + l.getTaperUnitario()) * l.getCantidad();
    }

    private Map<String, String> nombresDeProductos(List<DetallePedido> aCobrar) {
        Set<String> ids = new HashSet<>();
        aCobrar.forEach(l -> {
            if (l.getProductoId() != null) {
                ids.add(l.getProductoId());
            }
        });
        componentes.findByDetallePedidoIdIn(aCobrar.stream().map(DetallePedido::getId).toList())
                .forEach(c -> ids.add(c.getProductoId()));
        return productos.findAllById(ids).stream().collect(Collectors.toMap(Producto::getId, Producto::getNombre));
    }

    private CobroLinea snapshotDe(Cobro cobro, DetallePedido l, CuentaPersona persona, Map<String, String> nombres,
                                  DescuentoAplicado descuento) {
        long importe = importeDe(l) - descuento.monto();
        List<DetallePedidoComponente> partes = componentes.findByDetallePedidoIdIn(List.of(l.getId()));
        BigDecimal costoUnitario = null;
        String composicion = null;
        if (!partes.isEmpty()) {
            // Un menu: su costo es la suma de la receta de cada componente; si a uno le falta costo, el costo queda nulo.
            List<Map<String, Object>> detalle = new ArrayList<>();
            BigDecimal suma = BigDecimal.ZERO;
            boolean completo = true;
            for (DetallePedidoComponente c : partes) {
                Receta receta = recetas.findByProductoIdAndActivoTrue(c.getProductoId()).orElse(null);
                BigDecimal costo = receta == null ? null : receta.getCostoPorPorcion();
                Map<String, Object> fila = new LinkedHashMap<>();
                fila.put("componente", c.getComponente());
                fila.put("productoId", c.getProductoId());
                fila.put("nombre", nombres.get(c.getProductoId()));
                fila.put("recetaId", receta == null ? null : receta.getId());
                fila.put("costoPorPorcion", costo);
                detalle.add(fila);
                if (costo == null) {
                    completo = false;
                } else {
                    suma = suma.add(costo);
                }
            }
            costoUnitario = completo ? suma : null;
            composicion = comoJson(detalle);
        } else if (l.getProductoId() != null) {
            Receta receta = recetas.findByProductoIdAndActivoTrue(l.getProductoId()).orElse(null);
            costoUnitario = receta == null ? null : receta.getCostoPorPorcion();
            if (receta != null) {
                Map<String, Object> fila = new LinkedHashMap<>();
                fila.put("recetaId", receta.getId());
                fila.put("costoPorPorcion", costoUnitario);
                composicion = comoJson(List.of(fila));
            }
        }
        // El costo y la utilidad van en soles (como los costos del catalogo); los precios, en centimos.
        BigDecimal utilidad = costoUnitario == null ? null
                : BigDecimal.valueOf(importe).movePointLeft(2)
                        .subtract(costoUnitario.multiply(BigDecimal.valueOf(l.getCantidad())))
                        .setScale(6, RoundingMode.HALF_UP);
        String nombre = switch (l.getTipoLinea()) {
            case "plan_desayuno" -> "Desayuno del plan";
            case "plan_cena" -> "Cena del plan";
            case "extra_libre" -> l.getDescripcion();
            default -> l.getProductoId() == null ? "Producto" : nombres.getOrDefault(l.getProductoId(), "Producto");
        };
        return new CobroLinea(Identificador.nuevo().valor(), cobro.getId(), l.getId(), l.getCuentaPersonaId(),
                persona.getPensionistaId(), persona.getPensionistaId() == null ? "medios" : "cuenta", l.getProductoId(),
                l.getTipoLinea(), nombre, l.getCantidad(), l.getPrecioUnitario(), l.getTaperUnitario(), descuento.monto(), importe,
                costoUnitario, utilidad, composicion, descuento.monto() > 0 ? descuento.motivoId() : null,
                descuento.monto() > 0 ? descuento.observacion() : null);
    }

    private static String comoJson(Object valor) {
        try {
            return JSON.writeValueAsString(valor);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }
}
