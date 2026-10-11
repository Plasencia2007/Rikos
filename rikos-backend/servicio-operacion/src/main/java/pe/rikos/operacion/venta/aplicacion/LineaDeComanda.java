package pe.rikos.operacion.venta.aplicacion;

import java.util.List;

/** Una linea de la comanda tal como se mando a cocina: cantidad, nombre, componentes elegidos y nota. */
public record LineaDeComanda(int cantidad, String nombre, List<String> componentes, String observacion) {
}
