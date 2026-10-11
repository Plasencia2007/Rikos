package pe.rikos.operacion.venta.aplicacion;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.rikos.contratos.Resultado;
import pe.rikos.operacion.catalogo.infraestructura.Producto;
import pe.rikos.operacion.catalogo.infraestructura.ProductoRepositorio;
import pe.rikos.operacion.identidad.infraestructura.Usuario;
import pe.rikos.operacion.identidad.infraestructura.UsuarioRepositorio;
import pe.rikos.operacion.venta.infraestructura.CuentaPersona;
import pe.rikos.operacion.venta.infraestructura.CuentaPersonaRepositorio;
import pe.rikos.operacion.venta.infraestructura.DetallePedido;
import pe.rikos.operacion.venta.infraestructura.DetallePedidoComponente;
import pe.rikos.operacion.venta.infraestructura.DetallePedidoComponenteRepositorio;
import pe.rikos.operacion.venta.infraestructura.DetallePedidoRepositorio;
import pe.rikos.operacion.venta.infraestructura.Mesa;
import pe.rikos.operacion.venta.infraestructura.MesaRepositorio;
import pe.rikos.operacion.venta.infraestructura.Pedido;
import pe.rikos.operacion.venta.infraestructura.PedidoRepositorio;
import pe.rikos.operacion.venta.infraestructura.Salon;
import pe.rikos.operacion.venta.infraestructura.SalonRepositorio;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Sprint 6b: la lista de pedidos del local de la sesion para el POS. "En curso" (abierto, comandado y pendiente de
 * cierre) trae las lineas para las tarjetas; "cerrados" (cerrado y anulado, que llegan con el sprint 7) es el historial y
 * hoy sale vacio. El total se deriva de las lineas no anuladas, por lote para toda la pagina. Solo lectura.
 */
@Service
public class ListaDePedidos {

    static final List<String> EN_CURSO = List.of("abierto", "comandado", "pendiente_cierre");
    static final List<String> CERRADOS = List.of("cerrado", "anulado");
    private static final List<String> CANALES = List.of("mesa", "para_llevar");
    private static final int TAMANO_MAXIMO = 100;

    private final PedidoRepositorio pedidos;
    private final CuentaPersonaRepositorio personas;
    private final DetallePedidoRepositorio lineas;
    private final DetallePedidoComponenteRepositorio componentes;
    private final ProductoRepositorio productos;
    private final MesaRepositorio mesas;
    private final SalonRepositorio salones;
    private final UsuarioRepositorio usuarios;

    public ListaDePedidos(PedidoRepositorio pedidos, CuentaPersonaRepositorio personas, DetallePedidoRepositorio lineas,
                          DetallePedidoComponenteRepositorio componentes, ProductoRepositorio productos,
                          MesaRepositorio mesas, SalonRepositorio salones, UsuarioRepositorio usuarios) {
        this.pedidos = pedidos;
        this.personas = personas;
        this.lineas = lineas;
        this.componentes = componentes;
        this.productos = productos;
        this.mesas = mesas;
        this.salones = salones;
        this.usuarios = usuarios;
    }

    @Transactional(readOnly = true)
    public Resultado<PaginaDePedidos, ErrorPedido> listar(FiltroDePedidos f) {
        String localId = LocalDeSesion.actual().orElse(null);
        if (localId == null) {
            return Resultado.fracaso(ErrorPedido.SIN_LOCAL_ACTIVO);
        }
        boolean enCurso = f.situacion() == null || f.situacion().isBlank() || "en_curso".equals(f.situacion());
        if (!enCurso && !"cerrados".equals(f.situacion())) {
            return Resultado.fracaso(ErrorPedido.FILTRO_INVALIDO);
        }
        List<String> delaSituacion = enCurso ? EN_CURSO : CERRADOS;
        boolean hayEstado = f.estado() != null && !f.estado().isBlank();
        if (hayEstado && !delaSituacion.contains(f.estado())) {
            return Resultado.fracaso(ErrorPedido.FILTRO_INVALIDO);
        }
        boolean hayCanal = f.canal() != null && !f.canal().isBlank();
        if (hayCanal && !CANALES.contains(f.canal())) {
            return Resultado.fracaso(ErrorPedido.FILTRO_INVALIDO);
        }
        if (f.desde() != null && f.hasta() != null && f.desde().isAfter(f.hasta())) {
            return Resultado.fracaso(ErrorPedido.FILTRO_INVALIDO);
        }
        if (f.pagina() < 0 || f.tamano() < 1 || f.tamano() > TAMANO_MAXIMO) {
            return Resultado.fracaso(ErrorPedido.FILTRO_INVALIDO);
        }

        String texto = f.q() == null || f.q().isBlank() ? "%"
                : "%" + f.q().trim().toLowerCase().replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";
        Page<Pedido> pagina = pedidos.buscar(localId, hayEstado ? List.of(f.estado()) : delaSituacion,
                hayCanal ? List.of(f.canal()) : CANALES, f.desde() != null ? f.desde() : LocalDate.of(1900, 1, 1),
                f.hasta() != null ? f.hasta() : LocalDate.of(2999, 12, 31), texto,
                PageRequest.of(f.pagina(), f.tamano()));

        return Resultado.exito(new PaginaDePedidos(resumir(pagina.getContent(), enCurso), f.pagina(), f.tamano(),
                pagina.getTotalElements(), pagina.getTotalPages()));
    }

    @Transactional(readOnly = true)
    public Resultado<ResumenDePedidos, ErrorPedido> resumen() {
        String localId = LocalDeSesion.actual().orElse(null);
        if (localId == null) {
            return Resultado.fracaso(ErrorPedido.SIN_LOCAL_ACTIVO);
        }
        return Resultado.exito(new ResumenDePedidos(pedidos.countByLocalIdAndEstadoIn(localId, EN_CURSO),
                pedidos.countByLocalIdAndEstadoIn(localId, List.of("pendiente_cierre"))));
    }

    /** Arma los resumenes de una pagina leyendo personas, lineas, mesas, salones y mozos por lote. */
    private List<PedidoResumen> resumir(List<Pedido> pagina, boolean conLineas) {
        if (pagina.isEmpty()) {
            return List.of();
        }
        List<String> pedidosIds = pagina.stream().map(Pedido::getId).toList();
        List<CuentaPersona> todasLasPersonas = personas.findByPedidoIdInOrderByOrden(pedidosIds);
        Map<String, Long> comensales = todasLasPersonas.stream()
                .collect(Collectors.groupingBy(CuentaPersona::getPedidoId, Collectors.counting()));
        Map<String, String> pedidoDePersona = todasLasPersonas.stream()
                .collect(Collectors.toMap(CuentaPersona::getId, CuentaPersona::getPedidoId));
        List<DetallePedido> todas = todasLasPersonas.isEmpty() ? List.of()
                : lineas.findByCuentaPersonaIdInOrderByCreadoEn(pedidoDePersona.keySet());
        List<DetallePedido> vigentes = todas.stream().filter(l -> !"anulado".equals(l.getEstado())).toList();

        Map<String, Long> totales = vigentes.stream().collect(Collectors.groupingBy(
                l -> pedidoDePersona.get(l.getCuentaPersonaId()),
                Collectors.summingLong(l -> (l.getPrecioUnitario() + l.getTaperUnitario()) * l.getCantidad())));

        Map<String, List<DetallePedidoComponente>> componentesPorLinea = Map.of();
        Map<String, String> nombresDeProducto = Map.of();
        if (conLineas && !vigentes.isEmpty()) {
            List<DetallePedidoComponente> filas = componentes
                    .findByDetallePedidoIdIn(vigentes.stream().map(DetallePedido::getId).toList());
            componentesPorLinea = filas.stream().collect(Collectors.groupingBy(DetallePedidoComponente::getDetallePedidoId));
            Set<String> productosIds = new HashSet<>();
            vigentes.stream().map(DetallePedido::getProductoId).filter(p -> p != null).forEach(productosIds::add);
            filas.stream().map(DetallePedidoComponente::getProductoId).forEach(productosIds::add);
            nombresDeProducto = productos.findAllById(productosIds).stream()
                    .collect(Collectors.toMap(Producto::getId, Producto::getNombre));
        }

        Map<String, Mesa> mesasPorId = mesas.findAllById(pagina.stream().map(Pedido::getMesaId)
                .filter(m -> m != null).collect(Collectors.toSet())).stream()
                .collect(Collectors.toMap(Mesa::getId, Function.identity()));
        Map<String, String> salonPorId = salones.findAllById(mesasPorId.values().stream().map(Mesa::getSalonId)
                .filter(s -> s != null).collect(Collectors.toSet())).stream()
                .collect(Collectors.toMap(Salon::getId, Salon::getNombre));
        Map<String, String> mozoPorId = usuarios.findAllById(pagina.stream().map(Pedido::getMozoId)
                .filter(m -> m != null).collect(Collectors.toSet())).stream()
                .collect(Collectors.toMap(Usuario::getId, Usuario::getNombreUsuario));

        List<PedidoResumen> resultado = new ArrayList<>();
        for (Pedido p : pagina) {
            Mesa mesa = p.getMesaId() == null ? null : mesasPorId.get(p.getMesaId());
            List<PedidoResumen.LineaDeLista> deLaLista = null;
            if (conLineas) {
                final Map<String, List<DetallePedidoComponente>> compPorLinea = componentesPorLinea;
                final Map<String, String> nombres = nombresDeProducto;
                deLaLista = vigentes.stream().filter(l -> p.getId().equals(pedidoDePersona.get(l.getCuentaPersonaId())))
                        .map(l -> new PedidoResumen.LineaDeLista(l.getCantidad(), nombreDe(l, nombres),
                                compPorLinea.getOrDefault(l.getId(), List.of()).stream()
                                        .map(c -> nombres.get(c.getProductoId())).toList(), l.getEstado()))
                        .toList();
            }
            resultado.add(new PedidoResumen(p.getId(), p.getNumero(), p.getCanal(), p.getServicio(), p.getEstado(),
                    mesa == null ? null : mesa.getId(), mesa == null ? null : mesa.getNumero(),
                    mesa == null ? null : mesa.getNombre(), mesa == null || mesa.getSalonId() == null ? null
                            : salonPorId.get(mesa.getSalonId()), p.getNombreCliente(),
                    comensales.getOrDefault(p.getId(), 0L).intValue(), mozoPorId.get(p.getMozoId()), p.getAbiertoEn(),
                    totales.getOrDefault(p.getId(), 0L), deLaLista));
        }
        return resultado;
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
