package pe.rikos.operacion.venta.aplicacion;

import org.springframework.stereotype.Component;
import pe.rikos.contratos.Resultado;
import pe.rikos.operacion.catalogo.aplicacion.TipoMenu;
import pe.rikos.operacion.catalogo.infraestructura.Producto;
import pe.rikos.operacion.catalogo.infraestructura.ProductoRepositorio;
import pe.rikos.operacion.comun.auditoria.RegistradorAuditoria;
import pe.rikos.operacion.venta.infraestructura.CuentaPersona;
import pe.rikos.operacion.venta.infraestructura.DetallePedido;
import pe.rikos.operacion.venta.infraestructura.DetallePedidoRepositorio;
import pe.rikos.operacion.venta.infraestructura.Pedido;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Identificar a un pensionista en una persona que YA tiene productos (AsignarPensionista y VincularPreTicket): sus
 * lineas se recalculan con las mismas reglas que al pasarlas a un pensionista (US-029 CA5, ver TrasladarLineas). El menu
 * normal y la comida del plan van al precio de su plan (con "segundo del dia" y taper); la carta, el menu extra y el
 * extra libre conservan su precio. Un menu o una comida del plan de varias unidades no puede ser de un pensionista
 * (CANTIDAD_INVALIDA) y una persona con algo ya cobrado no cambia de tipo (LINEA_COBRADA). Primero se calcula todo
 * sin tocar nada ({@link #preparar}); solo si se puede, el caso de uso asigna al pensionista y {@link #aplicar} guarda.
 */
@Component
class RecalculoAlIdentificar {

    /** Las lineas vigentes de la persona y el precio nuevo de las que cambian (por id de linea). */
    record Recalculo(List<DetallePedido> lineas, Map<String, PrecioDeLinea> precios) {
    }

    private final DetallePedidoRepositorio lineas;
    private final ProductoRepositorio productos;
    private final CalculadorDePrecioDeLinea calculador;
    private final RegistradorAuditoria auditoria;

    RecalculoAlIdentificar(DetallePedidoRepositorio lineas, ProductoRepositorio productos,
                           CalculadorDePrecioDeLinea calculador, RegistradorAuditoria auditoria) {
        this.lineas = lineas;
        this.productos = productos;
        this.calculador = calculador;
        this.auditoria = auditoria;
    }

    Resultado<Recalculo, ErrorPedido> preparar(Pedido pedido, CuentaPersona persona, String pensionistaId) {
        List<DetallePedido> vigentes = lineas.findByCuentaPersonaIdInOrderByCreadoEn(List.of(persona.getId())).stream()
                .filter(l -> !"anulado".equals(l.getEstado())).toList();
        if (vigentes.stream().anyMatch(l -> "cobrado".equals(l.getEstado()))) {
            return Resultado.fracaso(ErrorPedido.LINEA_COBRADA);
        }
        // La persona tal como quedaria, sin tocar la guardada: el calculador solo mira a quien cobra.
        CuentaPersona comoPensionista = new CuentaPersona(persona.getId(), persona.getPedidoId(), persona.getEtiqueta(),
                persona.getOrden());
        comoPensionista.asignarPensionista(pensionistaId, persona.getEtiqueta());
        boolean paraLlevar = "para_llevar".equals(pedido.getCanal());
        Map<String, PrecioDeLinea> nuevos = new LinkedHashMap<>();
        // Sus comidas del plan aun no cuentan como del pensionista en la base: se cuentan aqui para el segundo del dia.
        int consumosPrevios = 0;
        for (DetallePedido linea : vigentes) {
            TipoDeLinea tipo = TipoDeLinea.desde(linea.getTipoLinea()).orElseThrow();
            boolean delPlan = tipo == TipoDeLinea.PLAN_DESAYUNO || tipo == TipoDeLinea.PLAN_CENA;
            boolean deMenu = tipo == TipoDeLinea.MENU_NORMAL || tipo == TipoDeLinea.MENU_EXTRA;
            if ((delPlan || deMenu) && linea.getCantidad() != 1) {
                return Resultado.fracaso(ErrorPedido.CANTIDAD_INVALIDA);
            }
            if (tipo == TipoDeLinea.MENU_NORMAL || delPlan) {
                Long precioNormal = tipo == TipoDeLinea.MENU_NORMAL
                        ? productos.findByTipoMenu(TipoMenu.NORMAL.codigo()).map(Producto::getPrecioVenta).orElse(null)
                        : null;
                Resultado<PrecioDeLinea, ErrorPedido> precio = calculador.calcular(tipo, pedido.getServicio(),
                        precioNormal, comoPensionista, paraLlevar, pedido.getFechaOperativa(), consumosPrevios);
                if (!precio.esExito()) {
                    return Resultado.fracaso(((Resultado.Fracaso<?, ErrorPedido>) precio).error());
                }
                nuevos.put(linea.getId(), precio.valorOLanza());
                consumosPrevios++;
            }
        }
        return Resultado.exito(new Recalculo(vigentes, nuevos));
    }

    /** Guarda los precios nuevos (la persona ya es del pensionista) y deja en la bitacora cuanto cambio el total. */
    void aplicar(Pedido pedido, CuentaPersona persona, Recalculo recalculo) {
        if (recalculo.lineas().isEmpty()) {
            return;
        }
        long ajuste = 0;
        for (DetallePedido linea : recalculo.lineas()) {
            PrecioDeLinea p = recalculo.precios().get(linea.getId());
            if (p != null) {
                ajuste += (p.precio() - linea.getPrecioUnitario()) * linea.getCantidad();
                linea.pasarA(persona.getId(), p.precio(), p.taperUnitario(), p.tarifaOrigen(), p.planId(), p.segundoDelDia());
            }
        }
        lineas.saveAll(recalculo.lineas());
        auditoria.registrar("transferencia", "pedido", pedido.getId(), null,
                RegistradorAuditoria.campos("tipo", "identificacion_de_pensionista", "nombre", "Pedido " + pedido.getNumero(),
                        "persona", persona.getEtiqueta(), "productos", recalculo.lineas().size(), "ajuste", ajuste));
    }
}
