package pe.rikos.operacion.venta.aplicacion;

import pe.rikos.operacion.venta.infraestructura.DetallePedido;

import java.util.List;

/**
 * Una linea con el nombre que se muestra: el producto, "Desayuno del plan", "Cena del plan" o la descripcion del
 * extra. motivoAnulacion: la descripcion del motivo si la linea esta anulada (null si no).
 */
public record LineaDetallada(DetallePedido linea, String productoNombre, List<ComponenteDetallado> componentes,
                             String motivoAnulacion) {
}
