package pe.rikos.operacion.venta.aplicacion;

import java.time.LocalDate;
import java.util.List;

/**
 * Lo que el POS ofrece en un pedido: el menu confirmado de hoy (solo en el almuerzo), por componente y con sus
 * opciones, y la carta sin los agotados del local. Precios en centimos; el del menu es el normal (al pensionista
 * lo decide el motor al agregar la linea).
 */
public record OfertaDelDia(LocalDate fecha, String servicio, boolean hayMenu, Long precioMenuNormal,
                           Long precioMenuExtra, List<ComponenteOfrecido> normal, List<ComponenteOfrecido> extra,
                           List<ProductoOfrecido> carta) {
}
