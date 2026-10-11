package pe.rikos.operacion.venta.aplicacion;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.rikos.contratos.Resultado;
import pe.rikos.operacion.catalogo.infraestructura.Producto;
import pe.rikos.operacion.catalogo.infraestructura.ProductoRepositorio;
import pe.rikos.operacion.identidad.infraestructura.Usuario;
import pe.rikos.operacion.identidad.infraestructura.UsuarioRepositorio;
import pe.rikos.operacion.venta.infraestructura.Comanda;
import pe.rikos.operacion.venta.infraestructura.ComandaRepositorio;
import pe.rikos.operacion.venta.infraestructura.CuentaPersona;
import pe.rikos.operacion.venta.infraestructura.CuentaPersonaRepositorio;
import pe.rikos.operacion.venta.infraestructura.DetalleComanda;
import pe.rikos.operacion.venta.infraestructura.DetalleComandaRepositorio;
import pe.rikos.operacion.venta.infraestructura.DetallePedido;
import pe.rikos.operacion.venta.infraestructura.DetallePedidoComponente;
import pe.rikos.operacion.venta.infraestructura.DetallePedidoComponenteRepositorio;
import pe.rikos.operacion.venta.infraestructura.DetallePedidoRepositorio;
import pe.rikos.operacion.venta.infraestructura.Mesa;
import pe.rikos.operacion.venta.infraestructura.MesaRepositorio;
import pe.rikos.operacion.venta.infraestructura.Pedido;
import pe.rikos.operacion.venta.infraestructura.PedidoRepositorio;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/** US-030: las comandas de un pedido y el detalle imprimible de cada una. Todo del local de la sesion. */
@Service
public class ConsultaComandas {

    private final PedidoRepositorio pedidos;
    private final ComandaRepositorio comandas;
    private final DetalleComandaRepositorio detalles;
    private final DetallePedidoRepositorio lineas;
    private final DetallePedidoComponenteRepositorio componentes;
    private final CuentaPersonaRepositorio personas;
    private final ProductoRepositorio productos;
    private final MesaRepositorio mesas;
    private final UsuarioRepositorio usuarios;

    public ConsultaComandas(PedidoRepositorio pedidos, ComandaRepositorio comandas, DetalleComandaRepositorio detalles,
                            DetallePedidoRepositorio lineas, DetallePedidoComponenteRepositorio componentes,
                            CuentaPersonaRepositorio personas, ProductoRepositorio productos, MesaRepositorio mesas,
                            UsuarioRepositorio usuarios) {
        this.pedidos = pedidos;
        this.comandas = comandas;
        this.detalles = detalles;
        this.lineas = lineas;
        this.componentes = componentes;
        this.personas = personas;
        this.productos = productos;
        this.mesas = mesas;
        this.usuarios = usuarios;
    }

    @Transactional(readOnly = true)
    public Resultado<List<Comanda>, ErrorPedido> delPedido(String pedidoId) {
        String localId = LocalDeSesion.actual().orElse(null);
        if (localId == null) {
            return Resultado.fracaso(ErrorPedido.SIN_LOCAL_ACTIVO);
        }
        if (pedidos.findByIdAndLocalId(pedidoId, localId).isEmpty()) {
            return Resultado.fracaso(ErrorPedido.NO_ENCONTRADO);
        }
        return Resultado.exito(comandas.findByPedidoIdOrderByNumero(pedidoId));
    }

    @Transactional(readOnly = true)
    public Resultado<ComandaDetallada, ErrorPedido> detalle(String comandaId) {
        String localId = LocalDeSesion.actual().orElse(null);
        if (localId == null) {
            return Resultado.fracaso(ErrorPedido.SIN_LOCAL_ACTIVO);
        }
        Comanda comanda = comandas.findByIdAndLocalId(comandaId, localId).orElse(null);
        if (comanda == null) {
            return Resultado.fracaso(ErrorPedido.COMANDA_NO_ENCONTRADA);
        }
        Pedido pedido = pedidos.findById(comanda.getPedidoId()).orElseThrow();
        List<DetalleComanda> foto = detalles.findByComandaId(comandaId);
        Map<String, DetallePedido> lineasPorId = lineas
                .findAllById(foto.stream().map(DetalleComanda::getDetallePedidoId).toList()).stream()
                .collect(Collectors.toMap(DetallePedido::getId, Function.identity()));
        List<DetallePedidoComponente> filas = lineasPorId.isEmpty() ? List.of()
                : componentes.findByDetallePedidoIdIn(lineasPorId.keySet());

        Set<String> productosIds = new HashSet<>();
        lineasPorId.values().stream().map(DetallePedido::getProductoId).filter(p -> p != null)
                .forEach(productosIds::add);
        filas.stream().map(DetallePedidoComponente::getProductoId).forEach(productosIds::add);
        Map<String, String> nombres = productos.findAllById(productosIds).stream()
                .collect(Collectors.toMap(Producto::getId, Producto::getNombre));

        // Se agrupa por la etiqueta que la persona tenia AL COMANDAR (la foto), no por la persona actual de la linea:
        // si despues la linea se pasa a otra persona, la comanda que ya salio no cambia. Los grupos salen en el orden
        // de las personas del pedido; una etiqueta que ya no existe va al final.
        List<String> ordenDeEtiquetas = personas.findByPedidoIdOrderByOrden(pedido.getId()).stream()
                .map(CuentaPersona::getEtiqueta).toList();
        Map<String, List<LineaDeComanda>> porEtiqueta = new java.util.LinkedHashMap<>();
        for (DetalleComanda d : foto) {
            DetallePedido linea = lineasPorId.get(d.getDetallePedidoId());
            if (linea == null) {
                continue;
            }
            List<String> elegidos = filas.stream().filter(f -> f.getDetallePedidoId().equals(linea.getId()))
                    .map(f -> nombres.get(f.getProductoId())).toList();
            porEtiqueta.computeIfAbsent(d.getEtiquetaPersona(), e -> new ArrayList<>())
                    .add(new LineaDeComanda(d.getCantidad(), nombreDe(linea, nombres), elegidos,
                            linea.getObservacion()));
        }
        List<PersonaDeComanda> resultado = porEtiqueta.entrySet().stream()
                .sorted(java.util.Comparator.comparingInt(e -> {
                    int i = ordenDeEtiquetas.indexOf(e.getKey());
                    return i < 0 ? Integer.MAX_VALUE : i;
                }))
                .map(e -> new PersonaDeComanda(e.getKey(), e.getValue())).toList();
        String mesa = pedido.getMesaId() == null ? null
                : mesas.findById(pedido.getMesaId()).map(Mesa::getNumero).orElse(null);
        String mozo = pedido.getMozoId() == null ? null
                : usuarios.findById(pedido.getMozoId()).map(Usuario::getNombreUsuario).orElse(null);
        return Resultado.exito(new ComandaDetallada(comanda, pedido.getNumero(), pedido.getCanal(), mesa,
                pedido.getServicio(), mozo, resultado));
    }

    private static String nombreDe(DetallePedido linea, Map<String, String> nombres) {
        return switch (linea.getTipoLinea()) {
            case "plan_desayuno" -> "Desayuno del plan";
            case "plan_cena" -> "Cena del plan";
            case "extra_libre" -> linea.getDescripcion();
            default -> nombres.get(linea.getProductoId());
        };
    }
}
