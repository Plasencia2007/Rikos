package pe.rikos.operacion.caja.aplicacion;

import pe.rikos.operacion.caja.infraestructura.TurnoCaja;

/**
 * Lo que el cajero ve ANTES de contar (RN-067): el fondo, lo cobrado por medio y el efectivo que debe haber en la caja. Todo en
 * centimos. efectivoEsperado = fondo inicial + efectivo cobrado (el vuelto no cuenta y los cobros reabiertos tampoco).
 */
public record ResumenDeTurno(TurnoCaja turno, long fondoInicial, long efectivoCobrado, long efectivoEsperado, long totalYape,
                             long totalPlin, long totalTarjeta, long cobros) {
}
