package pe.rikos.operacion.identidad.aplicacion;

import org.junit.jupiter.api.Test;

import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.*;

class RangoHorarioTest {

    @Test
    void unTurnoQueCruzaMedianocheContieneHorasDeAmbosLados() {
        // 19:00 a 00:30, caso real y frecuente (seccion 2 de la ampliacion de US-007).
        RangoHorario cena = new RangoHorario(LocalTime.of(19, 0), LocalTime.of(0, 30));

        assertTrue(cena.cruzaMedianoche());
        assertTrue(cena.contiene(LocalTime.of(20, 0)), "20:00 deberia estar dentro del turno de cena");
        assertTrue(cena.contiene(LocalTime.of(0, 15)), "00:15 del dia siguiente deberia seguir siendo cena");
        assertFalse(cena.contiene(LocalTime.of(1, 0)), "01:00 ya cerro la cena");
        assertFalse(cena.contiene(LocalTime.of(18, 59)), "18:59 todavia no abrio");
    }

    @Test
    void unTurnoQueNoCruzaMedianocheSeComportaComoUnRangoNormal() {
        RangoHorario almuerzo = new RangoHorario(LocalTime.of(12, 0), LocalTime.of(15, 0));

        assertFalse(almuerzo.cruzaMedianoche());
        assertTrue(almuerzo.contiene(LocalTime.of(12, 0)));
        assertTrue(almuerzo.contiene(LocalTime.of(14, 59)));
        assertFalse(almuerzo.contiene(LocalTime.of(15, 0)));
        assertFalse(almuerzo.contiene(LocalTime.of(23, 0)));
    }

    @Test
    void doTurnosDelMismoDiaSinCruzarseNoSeSolapan() {
        RangoHorario almuerzo = new RangoHorario(LocalTime.of(12, 0), LocalTime.of(15, 0));
        RangoHorario cena = new RangoHorario(LocalTime.of(19, 0), LocalTime.of(23, 30));

        assertFalse(almuerzo.seSolapaCon(cena));
        assertFalse(cena.seSolapaCon(almuerzo));
    }

    @Test
    void unTurnoQueCruzaMedianocheSeSolapaConUnoQueEmpiezaTempranoAlDiaSiguiente() {
        // 23:30 a 01:30 (cena) se solapa con 01:00 a 02:00 (madrugada) entre 01:00 y 01:30.
        RangoHorario cena = new RangoHorario(LocalTime.of(23, 30), LocalTime.of(1, 30));
        RangoHorario madrugada = new RangoHorario(LocalTime.of(1, 0), LocalTime.of(2, 0));

        assertTrue(cena.seSolapaCon(madrugada));
        assertTrue(madrugada.seSolapaCon(cena));
    }

    @Test
    void turnosSinRelacionNoSeSolapan() {
        RangoHorario desayuno = new RangoHorario(LocalTime.of(7, 0), LocalTime.of(10, 0));
        RangoHorario cena = new RangoHorario(LocalTime.of(19, 0), LocalTime.of(23, 30));

        assertFalse(desayuno.seSolapaCon(cena));
    }
}
