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
class EliminarProductoTest {

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
    private CrearProducto crearProducto;
    @Autowired
    private CrearPresentacion crearPresentacion;
    @Autowired
    private EliminarProducto eliminarProducto;
    @Autowired
    private CategoriaProductoRepositorio categorias;
    @Autowired
    private UnidadMedidaRepositorio unidades;
    @Autowired
    private ProductoRepositorio productos;

    private CategoriaProducto hoja(String nombre) {
        CategoriaProducto raiz = categorias.save(
                new CategoriaProducto(Identificador.nuevo().valor(), "Raiz " + nombre, "insumo", null, 1, null));
        return categorias.save(new CategoriaProducto(Identificador.nuevo().valor(), raiz, nombre, 1, null));
    }

    private UnidadMedida unidad(String codigo) {
        return unidades.save(
                new UnidadMedida(Identificador.nuevo().valor(), codigo, "Unidad " + codigo + " test", "masa"));
    }

    @Test
    void borra_un_producto_que_nunca_se_uso() {
        CategoriaProducto hoja = hoja("Abarrotes eliminar test");
        UnidadMedida u = unidad("kgelim1");
        Producto producto = crearProducto.ejecutar("ELIM-001", "Producto sin uso", hoja.getId(), u.getId(), false,
                true, false, null, true, null).valorOLanza();

        Resultado<Void, ErrorCatalogo> resultado = eliminarProducto.ejecutar(producto.getId());

        assertThat(resultado.esExito()).isTrue();
        assertThat(productos.findById(producto.getId())).isEmpty();
    }

    @Test
    void rechaza_borrar_un_producto_con_presentaciones_de_compra() {
        CategoriaProducto hoja = hoja("Abarrotes eliminar test 2");
        UnidadMedida u = unidad("kgelim2");
        Producto producto = crearProducto.ejecutar("ELIM-002", "Producto con presentacion", hoja.getId(), u.getId(),
                false, true, false, null, true, null).valorOLanza();
        crearPresentacion.ejecutar(producto.getId(), "Saco 50 kg", u.getId(), new java.math.BigDecimal("50"), false,
                null);

        Resultado<Void, ErrorCatalogo> resultado = eliminarProducto.ejecutar(producto.getId());

        assertThat(resultado.esExito()).isFalse();
        assertThat(((Resultado.Fracaso<?, ErrorCatalogo>) resultado).error())
                .isEqualTo(ErrorCatalogo.TIENE_HISTORIAL);
        assertThat(productos.findById(producto.getId())).isPresent();
    }

    @Test
    void falla_con_no_encontrado_si_el_producto_no_existe() {
        Resultado<Void, ErrorCatalogo> resultado = eliminarProducto.ejecutar("no-existe");

        assertThat(resultado.esExito()).isFalse();
        assertThat(((Resultado.Fracaso<?, ErrorCatalogo>) resultado).error())
                .isEqualTo(ErrorCatalogo.NO_ENCONTRADO);
    }
}
