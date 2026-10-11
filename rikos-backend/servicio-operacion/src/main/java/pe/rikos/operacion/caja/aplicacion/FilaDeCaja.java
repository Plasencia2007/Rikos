package pe.rikos.operacion.caja.aplicacion;

import pe.rikos.operacion.caja.infraestructura.Caja;
import pe.rikos.operacion.caja.infraestructura.TurnoCaja;

/** Una caja con su turno abierto (nulo si esta cerrada) y el nombre de usuario de quien lo abrio. */
public record FilaDeCaja(Caja caja, TurnoCaja turnoAbierto, String cajeroNombre) {
}
