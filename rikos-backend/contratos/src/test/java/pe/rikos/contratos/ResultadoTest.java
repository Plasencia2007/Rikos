package pe.rikos.contratos;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ResultadoTest {

    @Test
    void unExitoEsExitoYNoFracaso() {
        Resultado<Integer, String> r = Resultado.exito(42);
        assertTrue(r.esExito());
        assertFalse(r.esFracaso());
    }

    @Test
    void unFracasoEsFracasoYNoExito() {
        Resultado<Integer, String> r = Resultado.fracaso("no encontrado");
        assertTrue(r.esFracaso());
        assertFalse(r.esExito());
    }

    @Test
    void valorOLanzaDevuelveElValorEnExito() {
        assertEquals(42, Resultado.<Integer, String>exito(42).valorOLanza());
    }

    @Test
    void valorOLanzaLanzaEnFracaso() {
        Resultado<Integer, String> r = Resultado.fracaso("no encontrado");
        assertThrows(IllegalStateException.class, r::valorOLanza);
    }

    @Test
    void mapTransformaElValorSoloEnExito() {
        Resultado<Integer, String> r = Resultado.<Integer, String>exito(2).map(v -> v * 10);
        assertEquals(20, r.valorOLanza());
    }

    @Test
    void mapNoTocaUnFracaso() {
        Resultado<Integer, String> r = Resultado.<Integer, String>fracaso("error").map(v -> v * 10);
        assertTrue(r.esFracaso());
    }

    @Test
    void mapErrorTransformaElErrorSoloEnFracaso() {
        Resultado<Integer, String> r = Resultado.<Integer, String>fracaso("error").mapError(String::toUpperCase);
        assertEquals("ERROR", ((Resultado.Fracaso<Integer, String>) r).error());
    }

    @Test
    void sePuedeUsarConSwitchDePatrones() {
        Resultado<Integer, String> r = Resultado.exito(7);
        String descripcion = switch (r) {
            case Resultado.Exito<Integer, String> e -> "ok:" + e.valor();
            case Resultado.Fracaso<Integer, String> f -> "error:" + f.error();
        };
        assertEquals("ok:7", descripcion);
    }
}
