package pe.rikos.operacion.venta.aplicacion;

import java.util.List;

/**
 * Una linea de la comanda tal como se mando a cocina: cantidad, nombre, componentes elegidos y nota. partes son los
 * mismos componentes con lo que es cada uno (entrada, segundo...), en el orden del menu; componentes queda por
 * compatibilidad con la hoja impresa.
 */
public record LineaDeComanda(int cantidad, String nombre, List<String> componentes, String observacion,
                             List<ComponenteDetallado> partes) {
}
