package pe.rikos.operacion.venta.aplicacion;

import pe.rikos.operacion.venta.infraestructura.CuentaPersona;

import java.util.List;

/** Una persona de la mesa con sus lineas y sus totales (derivados: las lineas anuladas no suman). Centimos. */
public record PersonaDetallada(CuentaPersona persona, List<LineaDetallada> lineas, long subtotal, long taper,
                               long total) {
}
