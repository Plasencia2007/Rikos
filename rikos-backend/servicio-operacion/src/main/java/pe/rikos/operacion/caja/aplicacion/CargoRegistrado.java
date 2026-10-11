package pe.rikos.operacion.caja.aplicacion;

/** Lo cargado a la cuenta de un pensionista en un cobro. superaTope: despues del cargo su deuda paso del tope (aviso, no bloqueo). */
public record CargoRegistrado(String pensionistaId, String nombre, long importe, boolean superaTope) {
}
