package pe.rikos.operacion.pensionistas.aplicacion;

/**
 * Lo que se sabe de una cuenta, calculado al consultar (nada de esto se guarda).
 *
 * @param saldo             creditos - cargos, en centimos: negativo = deuda, positivo = a favor
 * @param deuda             lo que debe (0 si tiene saldo a favor); el saldo a favor se aplica primero (RN-018)
 * @param saldoAFavor       lo que tiene a favor (0 si debe)
 * @param creditoDisponible tope - deuda (RN-016); null si no hay tope calculable; puede ser negativo
 * @param superaTope        true si la deuda pasa del tope: solo para RECORDAR el pago, nunca bloquea
 */
public record ResumenDeCuenta(long saldo, long deuda, long saldoAFavor, TopeDeCredito tope, Long creditoDisponible,
                              boolean superaTope) {
}
