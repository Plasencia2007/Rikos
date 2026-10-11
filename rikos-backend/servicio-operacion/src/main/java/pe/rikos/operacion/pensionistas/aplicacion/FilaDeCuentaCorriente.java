package pe.rikos.operacion.pensionistas.aplicacion;

import pe.rikos.operacion.pensionistas.infraestructura.Pensionista;

/** Una fila de la lista de cuentas corrientes: el pensionista y el resumen de su cuenta. */
public record FilaDeCuentaCorriente(Pensionista pensionista, ResumenDeCuenta resumen) {
}
