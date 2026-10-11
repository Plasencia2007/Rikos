package pe.rikos.operacion.venta.aplicacion;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.core.Violations;
import pe.rikos.operacion.ServicioOperacionApplication;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * El modulo venta usa el menu y los agotados (catalogo), el motor de precios y los pensionistas
 * (pensionistas) y el servicio vigente (identidad). Cada paquete tiene que estar expuesto a proposito:
 * Modulith no permite el acceso libre entre modulos.
 */
class DependenciasDeVentaTest {

    @Test
    void venta_solo_usa_paquetes_expuestos_de_otros_modulos() {
        var violaciones = new java.util.ArrayList<String>();
        try {
            ApplicationModules.of(ServicioOperacionApplication.class).verify();
        } catch (Violations e) {
            e.getMessages().stream().filter(m -> m.contains("Module 'venta'")).forEach(violaciones::add);
        }
        assertThat(violaciones).isEmpty();
    }
}
