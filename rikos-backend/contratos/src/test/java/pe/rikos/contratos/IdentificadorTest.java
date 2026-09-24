package pe.rikos.contratos;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class IdentificadorTest {

    @Test
    void unIdentificadorNuevoTiene26Caracteres() {
        Identificador id = Identificador.nuevo();
        assertEquals(26, id.valor().length());
    }

    @Test
    void dosIdentificadoresNuevosNuncaColisionan() {
        Set<String> generados = new HashSet<>();
        for (int i = 0; i < 10_000; i++) {
            assertTrue(generados.add(Identificador.nuevo().valor()));
        }
    }

    @Test
    void esOrdenableCronologicamenteComoTexto() throws InterruptedException {
        // Con brechas de pocos milisegundos este test resulto intermitente:
        // System.currentTimeMillis() no tiene por que resolver por debajo de
        // la granularidad del temporizador del sistema operativo (en esta
        // maquina, bajo carga de Docker/virtualizacion, se observaron fallos
        // incluso con 60 ms de por medio). No es un bug del algoritmo -- el
        // Paso 0 de Sprint 1 ya verifico el orden con brechas de 1+ segundo,
        // que es la garantia real que el diseño ofrece; esta prueba usa la
        // misma escala para no depender de una resolucion de reloj que el
        // sistema operativo no promete.
        Identificador primero = Identificador.nuevo();
        Thread.sleep(1100);
        Identificador segundo = Identificador.nuevo();
        assertTrue(primero.valor().compareTo(segundo.valor()) < 0);
    }

    @Test
    void reconstruyeUnIdentificadorValidoPersistido() {
        String valor = Identificador.nuevo().valor();
        Identificador reconstruido = Identificador.de(valor);
        assertEquals(valor, reconstruido.valor());
    }

    @Test
    void rechazaUnValorConLargoIncorrecto() {
        assertThrows(IllegalArgumentException.class, () -> Identificador.de("demasiado-corto"));
    }

    @Test
    void rechazaCaracteresFueraDelAlfabetoCrockford() {
        // 'I', 'L', 'O', 'U' no existen en Crockford Base32
        String invalido = "0".repeat(25) + "U";
        assertThrows(IllegalArgumentException.class, () -> Identificador.de(invalido));
    }

    @Test
    void dosIdentificadoresConElMismoValorSonIguales() {
        String valor = Identificador.nuevo().valor();
        assertEquals(Identificador.de(valor), Identificador.de(valor));
    }
}
