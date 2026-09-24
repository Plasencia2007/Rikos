package pe.rikos.operacion.identidad.aplicacion;

import java.time.LocalTime;

/**
 * Seccion 2 de la ampliacion de US-007: "si hora_fin es menor o igual que
 * hora_inicio, la franja termina al dia siguiente". Un turno de 19:00 a
 * 00:30 es normal en este negocio y la comparacion ingenua de horas falla
 * justo ahi -- este es el unico lugar donde se resuelve esa aritmetica,
 * para que el calculo no se reescriba (y se rompa) en cada caso de uso.
 */
public record RangoHorario(LocalTime inicio, LocalTime fin) {

    public boolean cruzaMedianoche() {
        return !fin.isAfter(inicio);
    }

    /** Minutos desde medianoche que dura el turno, ya normalizado (1..1440). */
    private int duracionMinutos() {
        int inicioMin = inicio.toSecondOfDay() / 60;
        int finMin = fin.toSecondOfDay() / 60;
        int duracion = finMin - inicioMin;
        return duracion <= 0 ? duracion + 1440 : duracion;
    }

    public boolean contiene(LocalTime hora) {
        if (!cruzaMedianoche()) {
            return !hora.isBefore(inicio) && hora.isBefore(fin);
        }
        return !hora.isBefore(inicio) || hora.isBefore(fin);
    }

    /**
     * Solapamiento en un ciclo diario que se repite: cada rango se proyecta
     * dos veces sobre una linea de 48 horas (una copia normal y una
     * desplazada +24h) para que cualquier cruce de medianoche quede
     * representado como un intervalo comun, sin casos especiales.
     */
    public boolean seSolapaCon(RangoHorario otro) {
        int inicioA = inicio.toSecondOfDay() / 60;
        int finA = inicioA + duracionMinutos();
        int inicioB = otro.inicio.toSecondOfDay() / 60;
        int finB = inicioB + otro.duracionMinutos();

        for (int desplazamiento : new int[] {-1440, 0, 1440}) {
            int iB = inicioB + desplazamiento;
            int fB = finB + desplazamiento;
            if (inicioA < fB && iB < finA) {
                return true;
            }
        }
        return false;
    }
}
