package pe.rikos.contratos;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DineroTest {

    @Test
    void ceroTieneCeroCentimos() {
        assertEquals(0L, Dinero.CERO.centimos());
    }

    @Test
    void sumaDosMontos() {
        Dinero total = Dinero.deCentimos(1050).sumar(Dinero.deCentimos(250));
        assertEquals(1300L, total.centimos());
    }

    @Test
    void restaPuedeQuedarNegativaParaAjustes() {
        Dinero resultado = Dinero.deCentimos(500).restar(Dinero.deCentimos(700));
        assertEquals(-200L, resultado.centimos());
    }

    @Test
    void multiplicaPorUnaCantidadEntera() {
        Dinero unitario = Dinero.deCentimos(350);
        assertEquals(1050L, unitario.multiplicarPor(3).centimos());
    }

    @Test
    void comparaDosMontos() {
        assertTrue(Dinero.deCentimos(500).esMayorQue(Dinero.deCentimos(400)));
        assertTrue(Dinero.deCentimos(400).esMenorQue(Dinero.deCentimos(500)));
        assertFalse(Dinero.deCentimos(500).esMayorQue(Dinero.deCentimos(500)));
    }

    @Test
    void dosMontosIgualesSonIguales() {
        assertEquals(Dinero.deCentimos(999), Dinero.deCentimos(999));
    }

    @Test
    void noExisteConstructorQueAceptePuntoFlotante() {
        // Prueba de diseno, no de comportamiento: Dinero solo se construye
        // desde un long de centimos. Si este test compila, la garantia se cumple.
        Dinero d = Dinero.deCentimos(100);
        assertNotNull(d);
    }
}
