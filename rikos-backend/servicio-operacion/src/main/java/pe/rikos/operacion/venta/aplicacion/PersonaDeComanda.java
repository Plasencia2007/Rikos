package pe.rikos.operacion.venta.aplicacion;

import java.util.List;

/** Lo que se mando a cocina para una persona de la mesa (por su etiqueta: "Persona 1" o el nombre del pensionista). */
public record PersonaDeComanda(String etiqueta, List<LineaDeComanda> lineas) {
}
