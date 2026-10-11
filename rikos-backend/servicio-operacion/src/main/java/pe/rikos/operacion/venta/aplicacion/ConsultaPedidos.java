package pe.rikos.operacion.venta.aplicacion;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.rikos.contratos.Resultado;
import pe.rikos.operacion.catalogo.aplicacion.ConsultaDisponibilidad;
import pe.rikos.operacion.catalogo.aplicacion.ConsultaMenu;
import pe.rikos.operacion.catalogo.aplicacion.TipoMenu;
import pe.rikos.operacion.catalogo.infraestructura.Motivo;
import pe.rikos.operacion.catalogo.infraestructura.MotivoRepositorio;
import pe.rikos.operacion.catalogo.infraestructura.OpcionMenu;
import pe.rikos.operacion.catalogo.infraestructura.Producto;
import pe.rikos.operacion.catalogo.infraestructura.ProductoRepositorio;
import pe.rikos.operacion.identidad.infraestructura.Usuario;
import pe.rikos.operacion.identidad.infraestructura.UsuarioRepositorio;
import pe.rikos.operacion.pensionistas.aplicacion.ParametrosGlobales;
import pe.rikos.operacion.venta.infraestructura.CuentaPersona;
import pe.rikos.operacion.venta.infraestructura.CuentaPersonaRepositorio;
import pe.rikos.operacion.venta.infraestructura.DetallePedido;
import pe.rikos.operacion.venta.infraestructura.DetallePedidoComponente;
import pe.rikos.operacion.venta.infraestructura.DetallePedidoComponenteRepositorio;
import pe.rikos.operacion.venta.infraestructura.DetallePedidoRepositorio;
import pe.rikos.operacion.venta.infraestructura.MesaRepositorio;
import pe.rikos.operacion.venta.infraestructura.Pedido;
import pe.rikos.operacion.venta.infraestructura.PedidoRepositorio;

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * US-028: lo que el POS lee de un pedido. Subtotal, taper y total NO se guardan: se derivan aqui de las lineas
 * (las anuladas no suman). Todo del local de la sesion: un pedido de otro local es "no encontrado".
 */
@Service
public class ConsultaPedidos {

    private final PedidoRepositorio pedidos;
    private final CuentaPersonaRepositorio personas;
    private final DetallePedidoRepositorio lineas;
    private final DetallePedidoComponenteRepositorio componentes;
    private final ProductoRepositorio productos;
    private final ConsultaMenu consultaMenu;
    private final ConsultaDisponibilidad disponibilidad;
    private final ParametrosGlobales parametros;
    private final Clock reloj;
    private final MotivoRepositorio motivos;
    private final MesaRepositorio mesas;
    private final UsuarioRepositorio usuarios;

    public ConsultaPedidos(PedidoRepositorio pedidos, CuentaPersonaRepositorio personas, DetallePedidoRepositorio lineas,
                           DetallePedidoComponenteRepositorio componentes, ProductoRepositorio productos,
                           ConsultaMenu consultaMenu, ConsultaDisponibilidad disponibilidad,
                           ParametrosGlobales parametros, Clock reloj, MotivoRepositorio motivos,
                           MesaRepositorio mesas, UsuarioRepositorio usuarios) {
        this.motivos = motivos;
        this.mesas = mesas;
        this.usuarios = usuarios;
        this.pedidos = pedidos;
        this.personas = personas;
        this.lineas = lineas;
        this.componentes = componentes;
        this.productos = productos;
        this.consultaMenu = consultaMenu;
        this.disponibilidad = disponibilidad;
        this.parametros = parametros;
        this.reloj = reloj;
    }

    @Transactional(readOnly = true)
    public Resultado<PedidoDetallado, ErrorPedido> pedido(String id) {
        String localId = LocalDeSesion.actual().orElse(null);
        if (localId == null) {
            return Resultado.fracaso(ErrorPedido.SIN_LOCAL_ACTIVO);
        }
        Pedido pedido = pedidos.findByIdAndLocalId(id, localId).orElse(null);
        return pedido == null ? Resultado.fracaso(ErrorPedido.NO_ENCONTRADO) : Resultado.exito(detallar(pedido));
    }

    /** El pedido en curso de una mesa del local de la sesion. */
    @Transactional(readOnly = true)
    public Resultado<PedidoDetallado, ErrorPedido> abiertoDeMesa(String mesaId) {
        String localId = LocalDeSesion.actual().orElse(null);
        if (localId == null) {
            return Resultado.fracaso(ErrorPedido.SIN_LOCAL_ACTIVO);
        }
        // Una mesa unida a otra se atiende por el pedido de su principal (US-029: el pedido se opera en conjunto).
        String mesaDelPedido = mesas.findByIdAndLocalId(mesaId == null ? "" : mesaId, localId)
                .map(m -> m.getMesaPrincipalId() != null ? m.getMesaPrincipalId() : m.getId()).orElse(mesaId);
        Pedido pedido = pedidos.findByMesaIdAndEstadoIn(mesaDelPedido, AbrirPedido.ESTADOS_EN_CURSO)
                .filter(p -> p.getLocalId().equals(localId)).orElse(null);
        return pedido == null ? Resultado.fracaso(ErrorPedido.NO_ENCONTRADO) : Resultado.exito(detallar(pedido));
    }

    private PedidoDetallado detallar(Pedido pedido) {
        List<CuentaPersona> delPedido = personas.findByPedidoIdOrderByOrden(pedido.getId());
        List<String> personasIds = delPedido.stream().map(CuentaPersona::getId).toList();
        List<DetallePedido> todas = personasIds.isEmpty() ? List.of()
                : lineas.findByCuentaPersonaIdInOrderByCreadoEn(personasIds);
        List<DetallePedidoComponente> filas = todas.isEmpty() ? List.of()
                : componentes.findByDetallePedidoIdIn(todas.stream().map(DetallePedido::getId).toList());

        Set<String> productosIds = new HashSet<>();
        todas.stream().map(DetallePedido::getProductoId).filter(p -> p != null).forEach(productosIds::add);
        filas.stream().map(DetallePedidoComponente::getProductoId).forEach(productosIds::add);
        Map<String, String> nombres = productos.findAllById(productosIds).stream()
                .collect(Collectors.toMap(Producto::getId, Producto::getNombre));

        Map<String, String> motivosDeAnulacion = motivos
                .findAllById(todas.stream().map(DetallePedido::getMotivoAnulacionId).filter(m -> m != null).toList())
                .stream().collect(Collectors.toMap(Motivo::getId, Motivo::getDescripcion));

        List<PersonaDetallada> resultado = new ArrayList<>();
        long subtotal = 0;
        long taper = 0;
        for (CuentaPersona persona : delPedido) {
            List<LineaDetallada> lineasDeLaPersona = new ArrayList<>();
            long subtotalPersona = 0;
            long taperPersona = 0;
            for (DetallePedido linea : todas) {
                if (!linea.getCuentaPersonaId().equals(persona.getId())) {
                    continue;
                }
                if (!"anulado".equals(linea.getEstado())) {
                    subtotalPersona += linea.getPrecioUnitario() * linea.getCantidad();
                    taperPersona += linea.getTaperUnitario() * linea.getCantidad();
                }
                List<ComponenteDetallado> deLaLinea = filas.stream()
                        .filter(f -> f.getDetallePedidoId().equals(linea.getId()))
                        .map(f -> new ComponenteDetallado(f.getComponente(), nombres.get(f.getProductoId()))).toList();
                lineasDeLaPersona.add(new LineaDetallada(linea, nombreDe(linea, nombres), deLaLinea,
                        motivosDeAnulacion.get(linea.getMotivoAnulacionId())));
            }
            resultado.add(new PersonaDetallada(persona, lineasDeLaPersona, subtotalPersona, taperPersona,
                    subtotalPersona + taperPersona));
            subtotal += subtotalPersona;
            taper += taperPersona;
        }
        return new PedidoDetallado(pedido, resultado, subtotal, taper, subtotal + taper, avisosDe(pedido, todas),
                pedido.getMozoId() == null ? null
                        : usuarios.findById(pedido.getMozoId()).map(Usuario::getNombreUsuario).orElse(null));
    }

    /**
     * Cosas que el POS debe mostrar. TAPER_SIN_MONTO: es un pedido para llevar con menus y el duenio no ha puesto el
     * monto del taper: se agrega sin cargo (spec 3.4) y se avisa, en vez de callar.
     */
    private List<String> avisosDe(Pedido pedido, List<DetallePedido> todas) {
        boolean llevaMenus = "para_llevar".equals(pedido.getCanal()) && todas.stream()
                .anyMatch(l -> !"anulado".equals(l.getEstado()) && !List.of("producto", "extra_libre").contains(l.getTipoLinea()));
        return llevaMenus && parametros.taperPrecio().isEmpty() ? List.of("TAPER_SIN_MONTO") : List.of();
    }

    private static String nombreDe(DetallePedido linea, Map<String, String> nombres) {
        return switch (linea.getTipoLinea()) {
            case "plan_desayuno" -> "Desayuno del plan";
            case "plan_cena" -> "Cena del plan";
            case "extra_libre" -> linea.getDescripcion();
            default -> nombres.get(linea.getProductoId());
        };
    }

    /** Lo que el POS ofrece en este pedido: menu de hoy (solo almuerzo) y carta sin los agotados del local. */
    @Transactional(readOnly = true)
    public Resultado<OfertaDelDia, ErrorPedido> oferta(String pedidoId) {
        String localId = LocalDeSesion.actual().orElse(null);
        if (localId == null) {
            return Resultado.fracaso(ErrorPedido.SIN_LOCAL_ACTIVO);
        }
        Pedido pedido = pedidos.findByIdAndLocalId(pedidoId, localId).orElse(null);
        if (pedido == null) {
            return Resultado.fracaso(ErrorPedido.NO_ENCONTRADO);
        }
        LocalDate hoy = LocalDate.now(reloj);
        List<OpcionMenu> delDia = "almuerzo".equals(pedido.getServicio())
                ? consultaMenu.menuConfirmadoDel(hoy).orElse(List.of()) : List.of();
        boolean hayMenu = !delDia.isEmpty();
        Long precioNormal = hayMenu ? productos.findByTipoMenu(TipoMenu.NORMAL.codigo())
                .map(Producto::getPrecioVenta).orElse(null) : null;
        Long precioExtra = hayMenu ? productos.findByTipoMenu(TipoMenu.EXTRA.codigo())
                .map(Producto::getPrecioVenta).orElse(null) : null;

        Set<String> agotados = disponibilidad.idsAgotadosDelLocalActivo();
        // La carta sale por grupo (entrada, segundo, extra, otros) y por nombre dentro de cada uno.
        List<ProductoOfrecido> carta = productos.carta().stream().filter(p -> !agotados.contains(p.getId()))
                .map(p -> new ProductoOfrecido(p.getId(), p.getNombre(), p.getPrecioVenta(), grupoDe(p.getCategoria()),
                        p.getCategoria().getNombre(), p.getImagen()))
                .sorted(java.util.Comparator.comparingInt((ProductoOfrecido p) -> ordenDelGrupo(p.grupo()))
                        .thenComparing(ProductoOfrecido::nombre, String.CASE_INSENSITIVE_ORDER))
                .toList();
        return Resultado.exito(new OfertaDelDia(hoy, pedido.getServicio(), hayMenu, precioNormal, precioExtra,
                hayMenu ? componentesDe(delDia, TipoMenu.NORMAL, agotados) : List.of(),
                hayMenu ? componentesDe(delDia, TipoMenu.EXTRA, agotados) : List.of(), carta));
    }

    /**
     * El grupo de un producto de la carta, segun la categoria RAIZ de su categoria (nunca por el nombre): las entradas, los
     * segundos y los extras. Lo demas (bebidas, postres, guarniciones) no tiene grupo.
     */
    private static String grupoDe(pe.rikos.operacion.catalogo.infraestructura.CategoriaProducto categoria) {
        var raiz = categoria;
        while (raiz.getCategoriaPadre() != null) {
            raiz = raiz.getCategoriaPadre();
        }
        if ("extra".equals(raiz.getNaturaleza())) {
            return "extra";
        }
        if ("componente_menu".equals(raiz.getNaturaleza())) {
            return switch (String.valueOf(raiz.getComponenteMenu())) {
                case "entrada" -> "entrada";
                case "segundo" -> "segundo";
                default -> null;
            };
        }
        return null;
    }

    private static int ordenDelGrupo(String grupo) {
        return grupo == null ? 3 : switch (grupo) {
            case "entrada" -> 0;
            case "segundo" -> 1;
            default -> 2;
        };
    }

    /** Mismo criterio que AgregarLinea: el segundo del extra sale de las opciones "extra"; el resto, de las "normal"; sin agotados. */
    private static List<ComponenteOfrecido> componentesDe(List<OpcionMenu> delDia, TipoMenu tipoMenu,
                                                          Set<String> agotados) {
        Map<String, List<OpcionOfrecida>> porComponente = new HashMap<>();
        for (OpcionMenu opcion : delDia) {
            String tipoDeOpcion = tipoMenu == TipoMenu.EXTRA && "segundo".equals(opcion.getComponente())
                    ? "extra" : "normal";
            if (opcion.isActivo() && opcion.getTipoMenu().equals(tipoDeOpcion)
                    && !agotados.contains(opcion.getProducto().getId())) {
                porComponente.computeIfAbsent(opcion.getComponente(), c -> new ArrayList<>())
                        .add(new OpcionOfrecida(opcion.getId(), opcion.getProducto().getNombre()));
            }
        }
        List<ComponenteOfrecido> resultado = new ArrayList<>();
        for (String componente : tipoMenu.componentes()) {
            List<OpcionOfrecida> opciones = porComponente.get(componente);
            if (opciones != null && !opciones.isEmpty()) {
                resultado.add(new ComponenteOfrecido(componente, opciones));
            }
        }
        return resultado;
    }
}
