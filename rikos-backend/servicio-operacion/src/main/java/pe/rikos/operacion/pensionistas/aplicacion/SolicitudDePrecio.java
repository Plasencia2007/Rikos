package pe.rikos.operacion.pensionistas.aplicacion;

import java.time.LocalDate;

/**
 * Lo que la caja le pregunta al motor. Todo en centimos.
 *
 * @param precioNormal   el precio de lista del producto; null si todavia no tiene
 * @param planId         el plan del pensionista SI trae ticket vinculado a esa persona; null para un cliente
 *                       normal (o un pensionista sin ticket: se le trata como cliente normal)
 * @param segundoDelDia  true si es el segundo consumo de esa comida ese dia (solo importa con plan)
 * @param paraLlevar     true en para llevar, recojo y delivery
 * @param fecha          el dia del consumo (define la version del plan); null = hoy
 */
public record SolicitudDePrecio(TipoLinea tipo, Comida comida, Long precioNormal, String planId,
                                boolean segundoDelDia, boolean paraLlevar, LocalDate fecha) {
}
