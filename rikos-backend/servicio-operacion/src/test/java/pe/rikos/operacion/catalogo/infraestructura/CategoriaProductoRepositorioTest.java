package pe.rikos.operacion.catalogo.infraestructura;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import pe.rikos.contratos.Identificador;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class CategoriaProductoRepositorioTest {

    @Autowired
    private CategoriaProductoRepositorio categorias;

    @Test
    void una_categoria_sin_hijas_activas_es_hoja() {
        CategoriaProducto raiz = categorias.save(
                new CategoriaProducto(Identificador.nuevo().valor(), "Raiz de prueba", "insumo", null, 1, null));

        assertThat(categorias.existsByCategoriaPadreIdAndActivoTrue(raiz.getId())).isFalse();

        CategoriaProducto hija = categorias.save(
                new CategoriaProducto(Identificador.nuevo().valor(), raiz, "Hija de prueba", 1, null));

        assertThat(categorias.existsByCategoriaPadreIdAndActivoTrue(raiz.getId())).isTrue();

        hija.desactivar(null);
        categorias.save(hija);
        categorias.flush();

        assertThat(categorias.existsByCategoriaPadreIdAndActivoTrue(raiz.getId())).isFalse();
    }

    @Test
    void una_subcategoria_hereda_naturaleza_y_componente_menu_del_padre() {
        CategoriaProducto raiz = categorias.save(new CategoriaProducto(
                Identificador.nuevo().valor(), "Entradas de prueba", "componente_menu", "entrada", 1, null));
        CategoriaProducto hija = categorias.save(
                new CategoriaProducto(Identificador.nuevo().valor(), raiz, "Sopas de prueba", 1, null));

        assertThat(hija.getNaturaleza()).isEqualTo("componente_menu");
        assertThat(hija.getComponenteMenu()).isEqualTo("entrada");
    }
}
