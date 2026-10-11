package pe.rikos.operacion.pensionistas.aplicacion;

/**
 * El tope de credito: un RECORDATORIO de cobro, no un limite (RN-017). Nunca
 * bloquea un consumo.
 *
 * @param monto           centimos; null = no se pudo calcular (ver aviso), nunca un numero que subestime
 * @param origen          CALCULADO (por dias de atencion y comidas) o FIJADO a mano
 * @param aviso           por que no hay monto, si no lo hay
 * @param comidasContadas cuantas comidas (dia x comida) entraron en el calculo; 0 si esta fijado
 */
public record TopeDeCredito(Long monto, Origen origen, Aviso aviso, int comidasContadas) {

    public enum Origen {
        CALCULADO, FIJADO
    }

    public enum Aviso {
        /** El plan no tiene precio de alguna comida que el pensionista suele tomar. */
        PLAN_SIN_PRECIO,
        /** Ningun local tiene horarios ese mes: no hay dias que contar. */
        SIN_DIAS_DE_ATENCION
    }
}
