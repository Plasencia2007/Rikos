package pe.rikos.operacion.catalogo.aplicacion;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import pe.rikos.contratos.Identificador;
import pe.rikos.contratos.Resultado;
import pe.rikos.operacion.catalogo.infraestructura.CategoriaProducto;
import pe.rikos.operacion.catalogo.infraestructura.CategoriaProductoRepositorio;
import pe.rikos.operacion.catalogo.infraestructura.Producto;
import pe.rikos.operacion.catalogo.infraestructura.ProductoRepositorio;
import pe.rikos.operacion.catalogo.infraestructura.UnidadMedida;
import pe.rikos.operacion.catalogo.infraestructura.UnidadMedidaRepositorio;
import pe.rikos.operacion.comun.auditoria.ContextoDeSeguridadDePrueba;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class EliminarCategoriaTest {

    private static final String ADMIN_SEMBRADO_ID = "01M2QZ606763Q7XC00M8VD7WM1";

    @BeforeEach
    void autenticar() {
        ContextoDeSeguridadDePrueba.autenticarComo(ADMIN_SEMBRADO_ID);
    }

    @AfterEach
    void limpiarContexto() {
        ContextoDeSeguridadDePrueba.limpiar();
    }

    @Autowired
    private CrearCategoria crearCategoria;

    @Autowired
    private EliminarCategoria eliminarCategoria;

    @Autowired
    private CategoriaProductoRepositorio categorias;

    @Autowired
    private ProductoRepositorio productos;

    @Autowired
    private UnidadMedidaRepositorio unidades;

    @Test
    void elimina_una_raiz_vacia() {
        CategoriaProducto raiz =
                crearCategoria.ejecutar("Raiz eliminar A", "insumo", null, null, 1, null).valorOLanza();

        Resultado<Void, ErrorCatalogo> resultado = eliminarCategoria.ejecutar(raiz.getId());

        assertThat(resultado.esExito()).isTrue();
        assertThat(categorias.findById(raiz.getId())).isEmpty();
    }

    @Test
    void elimina_una_subcategoria_vacia() {
        CategoriaProducto raiz =
                crearCategoria.ejecutar("Raiz eliminar B", "insumo", null, null, 1, null).valorOLanza();
        CategoriaProducto hija =
                crearCategoria.ejecutar("Hija eliminar B", null, null, raiz.getId(), 1, null).valorOLanza();

        Resultado<Void, ErrorCatalogo> resultado = eliminarCategoria.ejecutar(hija.getId());

        assertThat(resultado.esExito()).isTrue();
        assertThat(categorias.findById(hija.getId())).isEmpty();
        // La raiz sigue ahi, solo se borro la hija.
        assertThat(categorias.findById(raiz.getId())).isPresent();
    }

    @Test
    void rechaza_eliminar_una_raiz_con_subcategorias() {
        CategoriaProducto raiz =
                crearCategoria.ejecutar("Raiz eliminar C", "insumo", null, null, 1, null).valorOLanza();
        crearCategoria.ejecutar("Hija eliminar C", null, null, raiz.getId(), 1, null);

        Resultado<Void, ErrorCatalogo> resultado = eliminarCategoria.ejecutar(raiz.getId());

        assertThat(resultado.esExito()).isFalse();
        assertThat(((Resultado.Fracaso<?, ErrorCatalogo>) resultado).error())
                .isEqualTo(ErrorCatalogo.CATEGORIA_TIENE_SUBCATEGORIAS);
        assertThat(categorias.findById(raiz.getId())).isPresent();
    }

    @Test
    void rechaza_eliminar_una_categoria_con_productos() {
        CategoriaProducto raiz =
                crearCategoria.ejecutar("Raiz eliminar D", "insumo", null, null, 1, null).valorOLanza();
        UnidadMedida unidad = unidades.save(new UnidadMedida(Identificador.nuevo().valor(), "unt-elim-d",
                "Unidad eliminar D", "unidad"));
        productos.save(new Producto(Identificador.nuevo().valor(), "COD-ELIM-D", "Producto eliminar D", raiz,
                unidad, false, true, false, null, true, null));

        Resultado<Void, ErrorCatalogo> resultado = eliminarCategoria.ejecutar(raiz.getId());

        assertThat(resultado.esExito()).isFalse();
        assertThat(((Resultado.Fracaso<?, ErrorCatalogo>) resultado).error())
                .isEqualTo(ErrorCatalogo.CATEGORIA_TIENE_PRODUCTOS);
        assertThat(categorias.findById(raiz.getId())).isPresent();
    }

    @Test
    void rechaza_eliminar_una_categoria_inexistente() {
        Resultado<Void, ErrorCatalogo> resultado = eliminarCategoria.ejecutar("no-existe");

        assertThat(resultado.esExito()).isFalse();
        assertThat(((Resultado.Fracaso<?, ErrorCatalogo>) resultado).error())
                .isEqualTo(ErrorCatalogo.NO_ENCONTRADO);
    }
}
