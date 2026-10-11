package pe.rikos.operacion;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

/**
 * HT-06: sin esto, la separacion en modulos (identidad, catalogo, comun...)
 * existe solo en el diagrama -- nada impide que un modulo importe un
 * paquete interno de otro "solo por esta vez". Esta prueba lo hace
 * imposible de ignorar: si algo cruza un limite que no esta expuesto por un
 * @NamedInterface, el build falla ACA, no tres meses despues cuando ya
 * nadie recuerda por que se hizo.
 */
class ArquitecturaDeModulosTest {

    private static final ApplicationModules MODULOS = ApplicationModules.of(ServicioOperacionApplication.class);

    @Test
    void losModulosRespetanSusLimites() {
        MODULOS.verify();
    }

    @Test
    void imprimeElMapaDeModulosParaReferencia() {
        // No es una asercion -- correr con -Dtest=...#imprimeElMapaDeModulosParaReferencia
        // muestra que modulo expone que, util para revisar antes de agregar
        // un @NamedInterface nuevo.
        System.out.println(MODULOS.toString());
    }
}
