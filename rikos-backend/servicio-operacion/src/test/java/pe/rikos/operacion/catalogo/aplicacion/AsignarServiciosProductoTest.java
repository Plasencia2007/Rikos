package pe.rikos.operacion.catalogo.aplicacion;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import pe.rikos.contratos.Identificador;
import pe.rikos.contratos.Resultado;
import pe.rikos.operacion.catalogo.infraestructura.*;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class AsignarServiciosProductoTest {

    @Autowired
    private AsignarServiciosProducto asignarServicios;
    @Autowired
    private ProductoServicioRepositorio productoServicios;
    @Autowired
    private CategoriaProductoRepositorio categorias;
    @Autowired
    private UnidadMedidaRepositorio unidades;
    @Autowired
    private ProductoRepositorio productos;

    private Producto productoFijo() {
        CategoriaProducto raiz = categorias.save(new CategoriaProducto(
                Identificador.nuevo().valor(), "Platos fijos test", "producto_fijo", null, 1, null));
        CategoriaProducto hoja = categorias.save(new CategoriaProducto(
                Identificador.nuevo().valor(), raiz, "Bebidas calientes test", 1, null));
        UnidadMedida u = unidades.save(new UnidadMedida(Identificador.nuevo().valor(), "undtest", "Unidad test",
                "unidad"));
        return productos.save(new Producto(Identificador.nuevo().valor(), "TE-001", "Te", hoja, u,
                true, false, false, 500L, false, null));
    }

    private Producto productoComponenteMenu() {
        CategoriaProducto raiz = categorias.save(new CategoriaProducto(
                Identificador.nuevo().valor(), "Segundos test", "componente_menu", "segundo", 1, null));
        CategoriaProducto hoja = categorias.save(new CategoriaProducto(
                Identificador.nuevo().valor(), raiz, "Pollo test", 1, null));
        UnidadMedida u = unidades.save(new UnidadMedida(Identificador.nuevo().valor(), "undtest2", "Unidad test 2",
                "unidad"));
        return productos.save(new Producto(Identificador.nuevo().valor(), "PO-001", "Pollo a la plancha", hoja, u,
                true, false, false, 1500L, false, null));
    }

    @Test
    void asigna_desayuno_y_cena_a_un_te() {
        Producto te = productoFijo();

        Resultado<Void, ErrorCatalogo> resultado =
                asignarServicios.ejecutar(te.getId(), Set.of("desayuno", "cena"), null);

        assertThat(resultado.esExito()).isTrue();
        assertThat(productoServicios.findByProductoId(te.getId())).hasSize(2);
    }

    @Test
    void rechaza_asignar_servicios_a_un_componente_de_menu() {
        Producto pollo = productoComponenteMenu();

        Resultado<Void, ErrorCatalogo> resultado =
                asignarServicios.ejecutar(pollo.getId(), Set.of("almuerzo"), null);

        assertThat(resultado.esExito()).isFalse();
    }
}
