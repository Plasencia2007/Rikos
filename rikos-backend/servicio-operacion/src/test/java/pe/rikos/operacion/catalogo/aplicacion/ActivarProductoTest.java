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
import pe.rikos.operacion.catalogo.infraestructura.UnidadMedida;
import pe.rikos.operacion.catalogo.infraestructura.UnidadMedidaRepositorio;
import pe.rikos.operacion.comun.auditoria.ContextoDeSeguridadDePrueba;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class ActivarProductoTest {

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
    private DesactivarProducto desactivarProducto;
    @Autowired
    private ActivarProducto activarProducto;
    @Autowired
    private CategoriaProductoRepositorio categorias;
    @Autowired
    private UnidadMedidaRepositorio unidades;

    private CategoriaProducto hoja(String nombre) {
        CategoriaProducto raiz = categorias.save(
                new CategoriaProducto(Identificador.nuevo().valor(), "Raiz " + nombre, "insumo", null, 1, null));
        return categorias.save(new CategoriaProducto(Identificador.nuevo().valor(), raiz, nombre, 1, null));
    }

    private UnidadMedida unidad() {
        return unidades.save(
                new UnidadMedida(Identificador.nuevo().valor(), "kgact", "Kilogramo activar test", "masa"));
    }

    @Test
    void reactiva_un_producto_previamente_desactivado() {
        CategoriaProducto hoja = hoja("Abarrotes activar test");
        UnidadMedida u = unidad();
        Producto producto = crearProducto.ejecutar("ACT-001", "Producto de prueba", hoja.getId(), u.getId(), false,
                true, false, null, true, null).valorOLanza();
        desactivarProducto.ejecutar(producto.getId(), null);

        Resultado<Producto, ErrorCatalogo> resultado = activarProducto.ejecutar(producto.getId(), null);

        assertThat(resultado.esExito()).isTrue();
        assertThat(resultado.valorOLanza().isActivo()).isTrue();
    }

    @Test
    void falla_con_no_encontrado_si_el_producto_no_existe() {
        Resultado<Producto, ErrorCatalogo> resultado = activarProducto.ejecutar("no-existe", null);

        assertThat(resultado.esExito()).isFalse();
        assertThat(((Resultado.Fracaso<?, ErrorCatalogo>) resultado).error())
                .isEqualTo(ErrorCatalogo.NO_ENCONTRADO);
    }
}
