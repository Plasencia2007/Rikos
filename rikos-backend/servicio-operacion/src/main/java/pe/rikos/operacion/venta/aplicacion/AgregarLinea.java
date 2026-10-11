package pe.rikos.operacion.venta.aplicacion;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.rikos.contratos.Identificador;
import pe.rikos.contratos.Resultado;
import pe.rikos.operacion.catalogo.aplicacion.ConsultaDisponibilidad;
import pe.rikos.operacion.catalogo.aplicacion.ConsultaMenu;
import pe.rikos.operacion.catalogo.aplicacion.TipoMenu;
import pe.rikos.operacion.catalogo.infraestructura.OpcionMenu;
import pe.rikos.operacion.catalogo.infraestructura.Producto;
import pe.rikos.operacion.catalogo.infraestructura.ProductoRepositorio;
import pe.rikos.operacion.comun.auditoria.RegistradorAuditoria;
import pe.rikos.operacion.venta.infraestructura.CuentaPersona;
import pe.rikos.operacion.venta.infraestructura.CuentaPersonaRepositorio;
import pe.rikos.operacion.venta.infraestructura.DetallePedido;
import pe.rikos.operacion.venta.infraestructura.DetallePedidoComponente;
import pe.rikos.operacion.venta.infraestructura.DetallePedidoComponenteRepositorio;
import pe.rikos.operacion.venta.infraestructura.DetallePedidoRepositorio;
import pe.rikos.operacion.venta.infraestructura.Pedido;
import pe.rikos.operacion.venta.infraestructura.PedidoRepositorio;

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * US-028: agrega una linea a la persona de un pedido. El precio lo decide el motor (CalculadorDePrecioDeLinea);
 * el mozo solo trae un precio en el "extra libre" (3.11), que queda marcado como manual y en la bitacora. Las
 * reglas de cada tipo de linea estan en la especificacion (3.4 y 3.5) y en el plan.
 */
@Service
public class AgregarLinea {

    private static final int LARGO_MAXIMO_OBSERVACION = 250;
    private static final int LARGO_MAXIMO_DESCRIPCION = 120;

    private final PedidoRepositorio pedidos;
    private final CuentaPersonaRepositorio personas;
    private final DetallePedidoRepositorio lineas;
    private final DetallePedidoComponenteRepositorio componentes;
    private final ProductoRepositorio productos;
    private final ConsultaMenu consultaMenu;
    private final ConsultaDisponibilidad disponibilidad;
    private final CalculadorDePrecioDeLinea calculador;
    private final RegistradorAuditoria auditoria;
    private final Clock reloj;

    public AgregarLinea(PedidoRepositorio pedidos, CuentaPersonaRepositorio personas, DetallePedidoRepositorio lineas,
                        DetallePedidoComponenteRepositorio componentes, ProductoRepositorio productos,
                        ConsultaMenu consultaMenu, ConsultaDisponibilidad disponibilidad,
                        CalculadorDePrecioDeLinea calculador, RegistradorAuditoria auditoria, Clock reloj) {
        this.pedidos = pedidos;
        this.personas = personas;
        this.lineas = lineas;
        this.componentes = componentes;
        this.productos = productos;
        this.consultaMenu = consultaMenu;
        this.disponibilidad = disponibilidad;
        this.calculador = calculador;
        this.auditoria = auditoria;
        this.reloj = reloj;
    }

    @Transactional
    public Resultado<DetallePedido, ErrorPedido> ejecutar(SolicitudDeLinea s, String actorId) {
        String localId = LocalDeSesion.actual().orElse(null);
        if (localId == null) {
            return Resultado.fracaso(ErrorPedido.SIN_LOCAL_ACTIVO);
        }
        Pedido pedido = pedidos.findByIdAndLocalId(s.pedidoId(), localId).orElse(null);
        if (pedido == null) {
            return Resultado.fracaso(ErrorPedido.NO_ENCONTRADO);
        }
        if (AbrirPedido.estaCerrado(pedido)) {
            return Resultado.fracaso(ErrorPedido.PEDIDO_CERRADO);
        }
        CuentaPersona persona = personas.findByIdAndPedidoId(s.personaId(), s.pedidoId()).orElse(null);
        if (persona == null) {
            return Resultado.fracaso(ErrorPedido.PERSONA_NO_ENCONTRADA);
        }
        if (s.id() != null) {
            // Igual que en AgregarPersona: un id de linea que ya existe en otra persona no se reutiliza (un merge la
            // moveria de persona, recalcularia su precio y la volveria a "pendiente").
            var existente = lineas.findById(s.id());
            if (existente.isPresent()) {
                return existente.get().getCuentaPersonaId().equals(persona.getId()) ? Resultado.exito(existente.get())
                        : Resultado.fracaso(ErrorPedido.NO_ENCONTRADO);
            }
        }
        if (s.cantidad() < 1) {
            return Resultado.fracaso(ErrorPedido.CANTIDAD_INVALIDA);
        }
        String observacion = s.observacion() == null || s.observacion().isBlank() ? null : s.observacion().trim();
        if (observacion != null && observacion.length() > LARGO_MAXIMO_OBSERVACION) {
            return Resultado.fracaso(ErrorPedido.OBSERVACION_LARGA);
        }
        boolean pensionista = persona.getPensionistaId() != null;
        boolean paraLlevar = "para_llevar".equals(pedido.getCanal());
        LocalDate hoy = LocalDate.now(reloj);

        String productoId = null;
        String descripcion = null;
        Long precioNormal = null;
        List<DetallePedidoComponente> filas = new ArrayList<>();
        String idLinea = s.id() != null ? s.id() : Identificador.nuevo().valor();

        switch (s.tipo()) {
            case PRODUCTO -> {
                Producto producto = productos.findById(s.productoId() == null ? "" : s.productoId()).orElse(null);
                if (producto == null) {
                    return Resultado.fracaso(ErrorPedido.PRODUCTO_NO_ENCONTRADO);
                }
                if (!producto.isEsVendible() || !producto.isActivo() || producto.getTipoMenu() != null) {
                    return Resultado.fracaso(ErrorPedido.PRODUCTO_NO_VENDIBLE);
                }
                if (disponibilidad.estaAgotado(localId, producto.getId())) {
                    return Resultado.fracaso(ErrorPedido.PRODUCTO_AGOTADO);
                }
                productoId = producto.getId();
                precioNormal = producto.getPrecioVenta();
            }
            case MENU_NORMAL, MENU_EXTRA -> {
                if (!"almuerzo".equals(pedido.getServicio())) {
                    return Resultado.fracaso(ErrorPedido.MENU_FUERA_DE_SERVICIO);
                }
                if (pensionista && s.cantidad() != 1) {
                    return Resultado.fracaso(ErrorPedido.CANTIDAD_INVALIDA);
                }
                TipoMenu tipoMenu = s.tipo() == TipoDeLinea.MENU_EXTRA ? TipoMenu.EXTRA : TipoMenu.NORMAL;
                List<OpcionMenu> delDia = consultaMenu.menuConfirmadoDel(hoy).orElse(List.of());
                if (delDia.isEmpty()) {
                    return Resultado.fracaso(ErrorPedido.MENU_NO_PROGRAMADO);
                }
                Producto productoMenu = productos.findByTipoMenu(tipoMenu.codigo()).orElse(null);
                if (productoMenu == null) {
                    return Resultado.fracaso(ErrorPedido.PRODUCTO_NO_ENCONTRADO);
                }
                Resultado<List<OpcionMenu>, ErrorPedido> elegidas = elegirComponentes(delDia, tipoMenu, s.componentes());
                if (!elegidas.esExito()) {
                    return Resultado.fracaso(((Resultado.Fracaso<?, ErrorPedido>) elegidas).error());
                }
                for (OpcionMenu opcion : elegidas.valorOLanza()) {
                    filas.add(new DetallePedidoComponente(Identificador.nuevo().valor(), idLinea,
                            opcion.getComponente(), opcion.getProducto().getId(), opcion.getId()));
                }
                productoId = productoMenu.getId();
                precioNormal = productoMenu.getPrecioVenta();
            }
            case PLAN_DESAYUNO, PLAN_CENA -> {
                if (!pensionista) {
                    return Resultado.fracaso(ErrorPedido.PERSONA_NO_ES_PENSIONISTA);
                }
                String servicioDelPlan = s.tipo() == TipoDeLinea.PLAN_DESAYUNO ? "desayuno" : "cena";
                if (!servicioDelPlan.equals(pedido.getServicio())) {
                    return Resultado.fracaso(ErrorPedido.PLAN_FUERA_DE_SERVICIO);
                }
                if (s.cantidad() != 1) {
                    return Resultado.fracaso(ErrorPedido.CANTIDAD_INVALIDA);
                }
            }
            case EXTRA_LIBRE -> {
                descripcion = s.descripcion() == null ? "" : s.descripcion().trim();
                if (descripcion.isEmpty()) {
                    return Resultado.fracaso(ErrorPedido.DESCRIPCION_OBLIGATORIA);
                }
                if (descripcion.length() > LARGO_MAXIMO_DESCRIPCION) {
                    descripcion = descripcion.substring(0, LARGO_MAXIMO_DESCRIPCION);
                }
                if (s.precioManual() == null || s.precioManual() <= 0) {
                    return Resultado.fracaso(ErrorPedido.PRECIO_INVALIDO);
                }
                precioNormal = s.precioManual();
            }
        }

        Resultado<PrecioDeLinea, ErrorPedido> precio = calculador.calcular(s.tipo(), pedido.getServicio(),
                precioNormal, persona, paraLlevar, hoy);
        if (!precio.esExito()) {
            return Resultado.fracaso(((Resultado.Fracaso<?, ErrorPedido>) precio).error());
        }
        PrecioDeLinea p = precio.valorOLanza();
        DetallePedido linea = lineas.save(new DetallePedido(idLinea, persona.getId(), s.tipo().codigo(), productoId,
                descripcion, s.cantidad(), observacion, p.precio(), p.taperUnitario(), p.tarifaOrigen(), p.planId(),
                p.segundoDelDia(), actorId));
        filas.forEach(componentes::save);
        if (s.tipo() == TipoDeLinea.EXTRA_LIBRE) {
            auditoria.registrar("precio_manual", "detalle_pedido", linea.getId(), null,
                    RegistradorAuditoria.campos("descripcion", descripcion, "precio", p.precio(), "cantidad",
                            s.cantidad(), "nombre", "Pedido " + pedido.getNumero()));
        }
        return Resultado.exito(linea);
    }

    /**
     * Un componente con una sola opcion DISPONIBLE se asigna solo; con varias, el mozo tiene que traer la elegida y
     * esta tiene que ser de hoy y de ese componente. Los componentes sin opciones ese dia no se piden. Una opcion
     * agotada en el local no se ofrece ni se acepta (RN-066); si todas las de un componente estan agotadas, no se
     * puede pedir el menu. El segundo del menu extra sale de las opciones "extra"; todo lo demas, de las "normal".
     * Una eleccion sin opcion (null) cuenta como "sin eleccion"; un componente repetido o desconocido se rechaza.
     */
    private Resultado<List<OpcionMenu>, ErrorPedido> elegirComponentes(List<OpcionMenu> delDia, TipoMenu tipoMenu,
                                                                       List<ComponenteElegido> elegidos) {
        Map<String, String> eleccion = new HashMap<>();
        for (ComponenteElegido e : elegidos == null ? List.<ComponenteElegido>of() : elegidos) {
            if (e.componente() == null || !tipoMenu.componentes().contains(e.componente())
                    || eleccion.containsKey(e.componente())) {
                return Resultado.fracaso(ErrorPedido.OPCION_INVALIDA);
            }
            eleccion.put(e.componente(), e.opcionMenuId());
        }
        Set<String> agotados = disponibilidad.idsAgotadosDelLocalActivo();
        List<OpcionMenu> resultado = new ArrayList<>();
        for (String componente : tipoMenu.componentes()) {
            String tipoDeOpcion = tipoMenu == TipoMenu.EXTRA && "segundo".equals(componente) ? "extra" : "normal";
            List<OpcionMenu> deHoy = delDia.stream()
                    .filter(o -> o.isActivo() && o.getComponente().equals(componente)
                            && o.getTipoMenu().equals(tipoDeOpcion))
                    .toList();
            String elegida = eleccion.get(componente);
            if (deHoy.isEmpty()) {
                if (elegida != null) {
                    return Resultado.fracaso(ErrorPedido.OPCION_INVALIDA);
                }
                continue;
            }
            List<OpcionMenu> disponibles = deHoy.stream()
                    .filter(o -> !agotados.contains(o.getProducto().getId())).toList();
            if (disponibles.isEmpty()) {
                return Resultado.fracaso(ErrorPedido.PRODUCTO_AGOTADO);
            }
            if (elegida != null) {
                OpcionMenu opcion = deHoy.stream().filter(o -> o.getId().equals(elegida)).findFirst().orElse(null);
                if (opcion == null) {
                    return Resultado.fracaso(ErrorPedido.OPCION_INVALIDA);
                }
                if (agotados.contains(opcion.getProducto().getId())) {
                    return Resultado.fracaso(ErrorPedido.PRODUCTO_AGOTADO);
                }
                resultado.add(opcion);
            } else if (disponibles.size() == 1) {
                resultado.add(disponibles.get(0));
            } else {
                return Resultado.fracaso(ErrorPedido.COMPONENTE_OBLIGATORIO);
            }
        }
        return Resultado.exito(resultado);
    }
}
