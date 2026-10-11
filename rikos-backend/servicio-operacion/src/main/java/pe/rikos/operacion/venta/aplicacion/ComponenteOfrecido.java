package pe.rikos.operacion.venta.aplicacion;

import java.util.List;

/** Un componente del menu con las opciones programadas hoy. */
public record ComponenteOfrecido(String componente, List<OpcionOfrecida> opciones) {
}
