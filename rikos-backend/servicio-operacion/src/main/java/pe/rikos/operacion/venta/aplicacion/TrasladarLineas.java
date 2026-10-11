package pe.rikos.operacion.venta.aplicacion;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.rikos.contratos.Identificador;
import pe.rikos.contratos.Resultado;
import pe.rikos.operacion.catalogo.aplicacion.TipoMenu;
import pe.rikos.operacion.catalogo.infraestructura.Producto;
import pe.rikos.operacion.catalogo.infraestructura.ProductoRepositorio;
import pe.rikos.operacion.comun.auditoria.RegistradorAuditoria;
import pe.rikos.operacion.venta.infraestructura.CuentaPersona;
import pe.rikos.operacion.venta.infraestructura.CuentaPersonaRepositorio;
import pe.rikos.operacion.venta.infraestructura.DetallePedido;
import pe.rikos.operacion.venta.infraestructura.DetallePedidoRepositorio;
import pe.rikos.operacion.venta.infraestructura.Pedido;
import pe.rikos.operacion.venta.infraestructura.PedidoRepositorio;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * US-029: pasa lineas de una persona a otra del mismo pedido (o a una persona nueva: dividir la cuenta). Todo o nada:
 * primero se valida y se recalcula cada linea y solo despues se mueve la primera, asi un error no deja la cuenta a
 * medias. El precio depende de la persona solo en el menu normal y en la comida del plan (CA5); la carta, el menu
 * extra y el extra libre conservan el que se guardo al agregarlos. Una linea comandada tambien se mueve: la comanda
 * que ya salio no cambia porque guarda la etiqueta de la persona tal como era.
 */
@Service
public class TrasladarLineas {

    private final PedidoRepositorio pedidos;
    private final CuentaPersonaRepositorio personas;
    private final DetallePedidoRepositorio lineas;
    private final ProductoRepositorio productos;
    private final CalculadorDePrecioDeLinea calculador;
    private final RegistradorAuditoria auditoria;

    public TrasladarLineas(PedidoRepositorio pedidos, CuentaPersonaRepositorio personas,
                           DetallePedidoRepositorio lineas, ProductoRepositorio productos,
                           CalculadorDePrecioDeLinea calculador, RegistradorAuditoria auditoria) {
        this.pedidos = pedidos;
        this.personas = personas;
        this.lineas = lineas;
        this.productos = productos;
        this.calculador = calculador;
        this.auditoria = auditoria;
    }

    /** personaDestinoId null = una persona nueva. */
    @Transactional
    public Resultado<Pedido, ErrorPedido> ejecutar(String pedidoId, List<String> lineasIds, String personaDestinoId,
                                                   String actorId) {
        String localId = LocalDeSesion.actual().orElse(null);
        if (localId == null) {
            return Resultado.fracaso(ErrorPedido.SIN_LOCAL_ACTIVO);
        }
        Pedido pedido = pedidos.findByIdAndLocalId(pedidoId, localId).orElse(null);
        if (pedido == null) {
            return Resultado.fracaso(ErrorPedido.NO_ENCONTRADO);
        }
        if (!List.of("abierto", "comandado").contains(pedido.getEstado())) {
            return Resultado.fracaso(ErrorPedido.PEDIDO_NO_REORGANIZABLE);
        }
        Set<String> pedidas = lineasIds == null ? Set.of() : new LinkedHashSet<>(lineasIds);
        if (pedidas.isEmpty()) {
            return Resultado.fracaso(ErrorPedido.SIN_LINEAS_PARA_MOVER);
        }
        if (pedidas.contains(null)) {
            // Un id nulo en la lista es una linea que no existe, no un error interno.
            return Resultado.fracaso(ErrorPedido.LINEA_NO_ENCONTRADA);
        }

        List<CuentaPersona> delPedido = personas.findByPedidoIdOrderByOrden(pedidoId);
        Map<String, CuentaPersona> porId = delPedido.stream()
                .collect(Collectors.toMap(CuentaPersona::getId, p -> p, (a, b) -> a, LinkedHashMap::new));
        List<DetallePedido> aMover = lineas.findAllById(pedidas);
        if (aMover.size() != pedidas.size() || aMover.stream().anyMatch(l -> !porId.containsKey(l.getCuentaPersonaId()))) {
            // Una linea inexistente, de otro pedido o de otro local: no se mueve ninguna y no se dice cual.
            return Resultado.fracaso(ErrorPedido.LINEA_NO_ENCONTRADA);
        }
        if (aMover.stream().anyMatch(l -> "anulado".equals(l.getEstado()))) {
            return Resultado.fracaso(ErrorPedido.LINEA_ANULADA);
        }

        CuentaPersona destino;
        boolean destinoNuevo = personaDestinoId == null;
        if (destinoNuevo) {
            int orden = delPedido.stream().mapToInt(CuentaPersona::getOrden).max().orElse(0) + 1;
            destino = new CuentaPersona(Identificador.nuevo().valor(), pedidoId, "Persona " + orden, orden);
        } else {
            destino = porId.get(personaDestinoId);
            if (destino == null) {
                return Resultado.fracaso(ErrorPedido.PERSONA_NO_ENCONTRADA);
            }
        }
        List<DetallePedido> cambian = aMover.stream()
                .filter(l -> !l.getCuentaPersonaId().equals(destino.getId())).toList();
        if (cambian.isEmpty()) {
            return Resultado.fracaso(ErrorPedido.MISMO_DESTINO);
        }

        // 1) Validar y recalcular TODO antes de tocar nada.
        boolean destinoPensionista = destino.getPensionistaId() != null;
        boolean paraLlevar = "para_llevar".equals(pedido.getCanal());
        Map<String, PrecioDeLinea> nuevos = new LinkedHashMap<>();
        // Comidas del plan que ya vienen en ESTA operacion hacia el mismo pensionista: cuentan para el segundo del dia.
        int consumosPrevios = 0;
        for (DetallePedido linea : cambian) {
            TipoDeLinea tipo = TipoDeLinea.desde(linea.getTipoLinea()).orElseThrow();
            boolean delPlan = tipo == TipoDeLinea.PLAN_DESAYUNO || tipo == TipoDeLinea.PLAN_CENA;
            if (delPlan && !destinoPensionista) {
                return Resultado.fracaso(ErrorPedido.SOLO_PARA_PENSIONISTA);
            }
            boolean deMenu = tipo == TipoDeLinea.MENU_NORMAL || tipo == TipoDeLinea.MENU_EXTRA;
            if (destinoPensionista && (delPlan || deMenu) && linea.getCantidad() != 1) {
                return Resultado.fracaso(ErrorPedido.CANTIDAD_INVALIDA);
            }
            // El precio se recalcula solo si cambia quien paga de otra manera: entre dos clientes normales la linea
            // conserva el precio con el que se agrego (3.8: dividir conserva el detalle y el precio).
            boolean cambiaElCobro = porId.get(linea.getCuentaPersonaId()).getPensionistaId() != null || destinoPensionista;
            if ((tipo == TipoDeLinea.MENU_NORMAL && cambiaElCobro) || delPlan) {
                Long precioNormal = tipo == TipoDeLinea.MENU_NORMAL
                        ? productos.findByTipoMenu(TipoMenu.NORMAL.codigo()).map(Producto::getPrecioVenta).orElse(null)
                        : null;
                Resultado<PrecioDeLinea, ErrorPedido> precio = calculador.calcular(tipo, pedido.getServicio(),
                        precioNormal, destino, paraLlevar, pedido.getFechaOperativa(), consumosPrevios);
                if (!precio.esExito()) {
                    return Resultado.fracaso(((Resultado.Fracaso<?, ErrorPedido>) precio).error());
                }
                nuevos.put(linea.getId(), precio.valorOLanza());
                if (destinoPensionista) {
                    consumosPrevios++;
                }
            }
        }

        // Cuanto cambia el total por el recalculo (centimos): queda en la bitacora, junto con lo que se movio.
        long ajuste = cambian.stream().filter(l -> nuevos.containsKey(l.getId()))
                .mapToLong(l -> (nuevos.get(l.getId()).precio() - l.getPrecioUnitario()) * l.getCantidad()).sum();

        // Las etiquetas de origen se leen ANTES de mover (despues la linea ya esta en otra persona).
        Set<String> desde = new LinkedHashSet<>();
        cambian.forEach(l -> desde.add(porId.get(l.getCuentaPersonaId()).getEtiqueta()));

        // 2) Mover.
        if (destinoNuevo) {
            personas.save(destino);
        }
        for (DetallePedido linea : cambian) {
            PrecioDeLinea p = nuevos.get(linea.getId());
            if (p == null) {
                linea.pasarA(destino.getId());
            } else {
                linea.pasarA(destino.getId(), p.precio(), p.taperUnitario(), p.tarifaOrigen(), p.planId(),
                        p.segundoDelDia());
            }
        }
        lineas.saveAll(cambian);

        auditoria.registrar("transferencia", "pedido", pedido.getId(), null,
                RegistradorAuditoria.campos("tipo", "traslado_de_lineas", "nombre", "Pedido " + pedido.getNumero(),
                        "productos", cambian.size(), "desde", String.join(", ", desde), "hacia", destino.getEtiqueta(), "ajuste", ajuste));
        return Resultado.exito(pedido);
    }
}
