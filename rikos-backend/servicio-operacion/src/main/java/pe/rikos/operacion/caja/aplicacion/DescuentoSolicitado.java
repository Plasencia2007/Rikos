package pe.rikos.operacion.caja.aplicacion;

/**
 * Un descuento que pide el cajero para UNA linea que esta cobrando: tipo "monto" (centimos) o "porcentaje" (1 a 100),
 * con un motivo de tipo "descuento" (obligatorio) y una observacion (obligatoria si el motivo la exige).
 */
public record DescuentoSolicitado(String lineaId, String tipo, long valor, String motivoId, String observacion) {
}
