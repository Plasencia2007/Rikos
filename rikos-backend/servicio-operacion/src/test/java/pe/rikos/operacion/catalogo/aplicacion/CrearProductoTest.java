package pe.rikos.operacion.catalogo.aplicacion;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import pe.rikos.contratos.Resultado;
import pe.rikos.operacion.catalogo.infraestructura.CategoriaProducto;
import pe.rikos.operacion.catalogo.infraestructura.CategoriaProductoRepositorio;
import pe.rikos.operacion.catalogo.infraestructura.Producto;
import pe.rikos.operacion.catalogo.infraestructura.ProductoRepositorio;
import pe.rikos.operacion.catalogo.infraestructura.UnidadMedida;
import pe.rikos.operacion.catalogo.infraestructura.UnidadMedidaRepositorio;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class CrearProductoTest {

    @Autowired
    private CrearProducto crearProducto;
    @Autowired
    private RegistrarCostoManual registrarCostoManual;
    @Autowired
    private CategoriaProductoRepositorio categorias;
    @Autowired
    private UnidadMedidaRepositorio unidades;
    @Autowired
    private ProductoRepositorio productos;

    private CategoriaProducto hoja(String nombre) {
        CategoriaProducto raiz = categorias.save(
                new pe.rikos.operacion.catalogo.infraestructura.CategoriaProducto(
                        pe.rikos.contratos.Identificador.nuevo().valor(), "Raiz " + nombre, "insumo", null, 1, null));
        return categorias.save(new CategoriaProducto(
                pe.rikos.contratos.Identificador.nuevo().valor(), raiz, nombre, 1, null));
    }

    private CategoriaProducto raizConHijas(String nombre) {
        CategoriaProducto raiz = categorias.save(new CategoriaProducto(
                pe.rikos.contratos.Identificador.nuevo().valor(), nombre, "insumo", null, 1, null));
        categorias.save(new CategoriaProducto(
                pe.rikos.contratos.Identificador.nuevo().valor(), raiz, nombre + " hija", 1, null));
        return raiz;
    }

    private UnidadMedida unidad() {
        return unidades.save(new UnidadMedida(pe.rikos.contratos.Identificador.nuevo().valor(), "kgtest",
                "Kilogramo test", "masa"));
    }

    @Test
    void crea_un_producto_insumo_sobre_una_categoria_hoja() {
        CategoriaProducto hoja = hoja("Verduras test");
        UnidadMedida u = unidad();

        Resultado<Producto, ErrorCatalogo> resultado = crearProducto.ejecutar(
                "PROD-001", "Zanahoria", hoja.getId(), u.getId(), false, true, false, null, true, null);

        assertThat(resultado.esExito()).isTrue();
        assertThat(resultado.valorOLanza().isEsInsumo()).isTrue();
    }

    @Test
    void rechaza_un_producto_colgado_de_una_categoria_con_hijas() {
        CategoriaProducto raizConHijas = raizConHijas("Raiz con hijas test");
        UnidadMedida u = unidad();

        Resultado<Producto, ErrorCatalogo> resultado = crearProducto.ejecutar(
                "PROD-002", "Producto mal colgado", raizConHijas.getId(), u.getId(), false, true, false, null, true,
                null);

        assertThat(resultado.esExito()).isFalse();
        assertThat(((Resultado.Fracaso<?, ErrorCatalogo>) resultado).error())
                .isEqualTo(ErrorCatalogo.CATEGORIA_NO_ES_HOJA);
    }

    @Test
    void exige_precio_de_venta_si_es_vendible() {
        CategoriaProducto hoja = hoja("Guarnicion test");
        UnidadMedida u = unidad();

        Resultado<Producto, ErrorCatalogo> resultado = crearProducto.ejecutar(
                "PROD-003", "Arroz porcion", hoja.getId(), u.getId(), true, false, false, null, true, null);

        assertThat(resultado.esExito()).isFalse();
        assertThat(((Resultado.Fracaso<?, ErrorCatalogo>) resultado).error())
                .isEqualTo(ErrorCatalogo.PRECIO_VENTA_OBLIGATORIO);
    }

    @Test
    void el_costo_manual_queda_marcado_como_editable_y_el_de_compra_no() {
        CategoriaProducto hoja = hoja("Abarrotes test");
        UnidadMedida u = unidad();
        Producto producto = crearProducto.ejecutar(
                "PROD-004", "Aceite", hoja.getId(), u.getId(), false, true, false, null, true, null)
                .valorOLanza();

        Resultado<Producto, ErrorCatalogo> conCostoManual =
                registrarCostoManual.ejecutar(producto.getId(), new BigDecimal("12.500000"), null);

        assertThat(conCostoManual.esExito()).isTrue();
        Producto actualizado = conCostoManual.valorOLanza();
        assertThat(actualizado.getOrigenCosto()).isEqualTo("manual");
        assertThat(actualizado.costoEsEditableAMano()).isTrue();

        actualizado.registrarCostoDeCompra(new BigDecimal("13.000000"), java.time.LocalDate.now(), null);
        assertThat(actualizado.getOrigenCosto()).isEqualTo("compra");
        assertThat(actualizado.costoEsEditableAMano()).isFalse();
    }

    @Test
    void rechaza_un_codigo_con_espacios_o_formato_invalido() {
        CategoriaProducto hoja = hoja("Bebidas test formato");
        UnidadMedida u = unidad();

        Resultado<Producto, ErrorCatalogo> resultado = crearProducto.ejecutar(
                " ARROZ ", "Arroz con espacios en el codigo", hoja.getId(), u.getId(), false, true, false, null,
                true, null);

        assertThat(resultado.esExito()).isFalse();
        assertThat(((Resultado.Fracaso<?, ErrorCatalogo>) resultado).error())
                .isEqualTo(ErrorCatalogo.CODIGO_FORMATO_INVALIDO);
    }

    @Test
    void rechaza_una_categoria_inexistente_la_categoria_es_obligatoria() {
        UnidadMedida u = unidad();

        Resultado<Producto, ErrorCatalogo> resultado = crearProducto.ejecutar(
                "PROD-005", "Producto sin categoria valida", "no-existe-esta-categoria", u.getId(), false, true,
                false, null, true, null);

        assertThat(resultado.esExito()).isFalse();
        assertThat(((Resultado.Fracaso<?, ErrorCatalogo>) resultado).error())
                .isEqualTo(ErrorCatalogo.NO_ENCONTRADO);
    }

    @Test
    void desactivar_no_borra_el_producto_sigue_consultable_con_su_historial() {
        CategoriaProducto hoja = hoja("Lacteos test desactivar");
        UnidadMedida u = unidad();
        Producto producto = crearProducto.ejecutar(
                "PROD-006", "Leche evaporada", hoja.getId(), u.getId(), false, true, false, null, true, null)
                .valorOLanza();

        producto.desactivar(null);

        Producto encontrado = productos.findById(producto.getId()).orElseThrow();
        assertThat(encontrado.isActivo()).isFalse();
        assertThat(encontrado.getCodigo()).isEqualTo("PROD-006");
        assertThat(encontrado.getNombre()).isEqualTo("Leche evaporada");
    }
}
