package pe.rikos.operacion.catalogo.aplicacion;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import pe.rikos.contratos.Resultado;
import pe.rikos.operacion.catalogo.infraestructura.CategoriaProducto;
import pe.rikos.operacion.catalogo.infraestructura.CategoriaProductoRepositorio;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class CrearCategoriaTest {

    @Autowired
    private CrearCategoria crearCategoria;

    @Autowired
    private ActualizarCategoria actualizarCategoria;

    @Autowired
    private CategoriaProductoRepositorio categorias;

    @Test
    void crea_una_raiz_con_su_naturaleza_y_componente_menu() {
        Resultado<CategoriaProducto, ErrorCatalogo> resultado =
                crearCategoria.ejecutar("Raiz test A", "componente_menu", "postre", null, 1, null);

        assertThat(resultado.esExito()).isTrue();
        CategoriaProducto raiz = resultado.valorOLanza();
        assertThat(raiz.getNivel()).isEqualTo(1);
        assertThat(raiz.getComponenteMenu()).isEqualTo("postre");
    }

    @Test
    void una_subcategoria_hereda_naturaleza_del_padre_e_ignora_la_que_se_le_mande() {
        CategoriaProducto raiz =
                crearCategoria.ejecutar("Raiz test B", "insumo", null, null, 1, null).valorOLanza();

        Resultado<CategoriaProducto, ErrorCatalogo> resultado =
                crearCategoria.ejecutar("Hija test B", "producto_fijo", null, raiz.getId(), 1, null);

        assertThat(resultado.esExito()).isTrue();
        assertThat(resultado.valorOLanza().getNaturaleza()).isEqualTo("insumo");
    }

    @Test
    void rechaza_una_subcategoria_de_padre_inexistente() {
        Resultado<CategoriaProducto, ErrorCatalogo> resultado =
                crearCategoria.ejecutar("Hija huerfana", "insumo", null, "no-existe", 1, null);

        assertThat(resultado.esExito()).isFalse();
        assertThat(((Resultado.Fracaso<?, ErrorCatalogo>) resultado).error())
                .isEqualTo(ErrorCatalogo.CATEGORIA_PADRE_NO_ENCONTRADA);
    }

    @Test
    void rechaza_naturaleza_invalida() {
        Resultado<CategoriaProducto, ErrorCatalogo> resultado =
                crearCategoria.ejecutar("Raiz mala", "no-existe-esta-naturaleza", null, null, 1, null);

        assertThat(resultado.esExito()).isFalse();
        assertThat(((Resultado.Fracaso<?, ErrorCatalogo>) resultado).error())
                .isEqualTo(ErrorCatalogo.NATURALEZA_INVALIDA);
    }

    @Test
    void rechaza_nombre_duplicado_dentro_del_mismo_padre() {
        CategoriaProducto raiz =
                crearCategoria.ejecutar("Raiz test C", "insumo", null, null, 1, null).valorOLanza();
        crearCategoria.ejecutar("Repetida", "insumo", null, raiz.getId(), 1, null);

        Resultado<CategoriaProducto, ErrorCatalogo> resultado =
                crearCategoria.ejecutar("Repetida", "insumo", null, raiz.getId(), 2, null);

        assertThat(resultado.esExito()).isFalse();
        assertThat(((Resultado.Fracaso<?, ErrorCatalogo>) resultado).error())
                .isEqualTo(ErrorCatalogo.NOMBRE_DUPLICADO);
    }
}
