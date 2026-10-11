package pe.rikos.operacion.venta.aplicacion;

import java.util.List;

/**
 * Lo que trae el POS al agregar una linea. id: opcional, del cliente (idempotencia). productoId: solo para
 * PRODUCTO. componentes: solo para el menu. descripcion y precioManual: solo para EXTRA_LIBRE (el unico caso
 * en que el mozo pone un precio).
 */
public record SolicitudDeLinea(String id, String pedidoId, String personaId, TipoDeLinea tipo, String productoId,
                               int cantidad, String observacion, List<ComponenteElegido> componentes,
                               String descripcion, Long precioManual) {
}
