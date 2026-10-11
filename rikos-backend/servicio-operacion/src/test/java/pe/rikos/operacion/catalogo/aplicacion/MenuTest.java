package pe.rikos.operacion.catalogo.aplicacion;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import pe.rikos.contratos.Resultado;
import pe.rikos.operacion.catalogo.infraestructura.CategoriaProducto;
import pe.rikos.operacion.catalogo.infraestructura.CategoriaProductoRepositorio;
import pe.rikos.operacion.catalogo.infraestructura.Producto;
import pe.rikos.operacion.catalogo.infraestructura.ProductoRepositorio;
import pe.rikos.operacion.comun.auditoria.Auditoria;
import pe.rikos.operacion.comun.auditoria.AuditoriaRepositorio;
import pe.rikos.operacion.comun.auditoria.ContextoDeSeguridadDePrueba;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * US-014. Los dos productos del menu los siembra V21 -- estas pruebas
 * corren contra esa semilla, no contra datos armados aca, porque lo que se
 * prueba justamente es que existan y esten protegidos.
 */
@SpringBootTest
@Transactional
class MenuTest {

    private static final String ADMIN_SEMBRADO_ID = "01M2QZ606763Q7XC00M8VD7WM1";

    @Autowired
    private CambiarPrecioMenu cambiarPrecioMenu;
    @Autowired
    private ActualizarProducto actualizarProducto;
    @Autowired
    private DesactivarProducto desactivarProducto;
    @Autowired
    private EliminarProducto eliminarProducto;
    @Autowired
    private CrearProducto crearProducto;
    @Autowired
    private ProductoRepositorio productos;
    @Autowired
    private CategoriaProductoRepositorio categorias;
    @Autowired
    private AuditoriaRepositorio auditorias;

    @BeforeEach
    void autenticar() {
        ContextoDeSeguridadDePrueba.autenticarComo(ADMIN_SEMBRADO_ID);
    }

    @AfterEach
    void limpiarContexto() {
        ContextoDeSeguridadDePrueba.limpiar();
    }

    private Producto menu(TipoMenu tipo) {
        return productos.findByTipoMenu(tipo.codigo()).orElseThrow();
    }

    @Test
    void existen_exactamente_un_menu_normal_y_un_menu_extra_vendibles() {
        assertThat(menu(TipoMenu.NORMAL).isEsVendible()).isTrue();
        assertThat(menu(TipoMenu.EXTRA).isEsVendible()).isTrue();
        assertThat(menu(TipoMenu.NORMAL).getId()).isNotEqualTo(menu(TipoMenu.EXTRA).getId());
    }

    @Test
    void la_composicion_es_entrada_segundo_con_guarnicion_refresco_y_postre() {
        assertThat(TipoMenu.NORMAL.componentes())
                .containsExactly("entrada", "segundo", "guarnicion", "refresco", "postre");
        assertThat(TipoMenu.EXTRA.componentes()).isEqualTo(TipoMenu.NORMAL.componentes());
    }

    @Test
    void solo_el_menu_normal_admite_la_tarifa_reducida_del_pensionista() {
        assertThat(TipoMenu.NORMAL.admiteTarifaReducida()).isTrue();
        assertThat(TipoMenu.EXTRA.admiteTarifaReducida()).isFalse();
    }

    @Test
    void cambia_el_precio_y_lo_deja_en_la_bitacora() {
        Resultado<Producto, ErrorMenu> resultado = cambiarPrecioMenu.ejecutar("extra", 1800L, ADMIN_SEMBRADO_ID);

        assertThat(resultado.esExito()).isTrue();
        assertThat(menu(TipoMenu.EXTRA).getPrecioVenta()).isEqualTo(1800L);
        List<Auditoria> filas = auditorias.findByEntidadAndEntidadIdOrderByOcurridoEnDesc("producto",
                menu(TipoMenu.EXTRA).getId());
        assertThat(filas).anyMatch(f -> f.getEvento().equals("cambio_precio") && f.getValorNuevo().contains("1800"));
    }

    @Test
    void rechaza_un_precio_nulo_cero_o_negativo() {
        for (Long precio : new Long[] {null, 0L, -500L}) {
            Resultado<Producto, ErrorMenu> resultado = cambiarPrecioMenu.ejecutar("normal", precio, ADMIN_SEMBRADO_ID);
            assertThat(((Resultado.Fracaso<?, ErrorMenu>) resultado).error()).isEqualTo(ErrorMenu.PRECIO_INVALIDO);
        }
    }

    @Test
    void rechaza_un_tipo_de_menu_que_no_existe() {
        Resultado<Producto, ErrorMenu> resultado = cambiarPrecioMenu.ejecutar("ejecutivo", 1500L, ADMIN_SEMBRADO_ID);

        assertThat(((Resultado.Fracaso<?, ErrorMenu>) resultado).error()).isEqualTo(ErrorMenu.TIPO_INVALIDO);
    }

    @Test
    void no_se_puede_desactivar_ni_borrar_un_producto_de_menu() {
        Producto normal = menu(TipoMenu.NORMAL);

        assertThat(error(desactivarProducto.ejecutar(normal.getId(), ADMIN_SEMBRADO_ID)))
                .isEqualTo(ErrorCatalogo.PRODUCTO_DE_MENU);
        assertThat(error(eliminarProducto.ejecutar(normal.getId()))).isEqualTo(ErrorCatalogo.PRODUCTO_DE_MENU);
        assertThat(productos.findById(normal.getId()).orElseThrow().isActivo()).isTrue();
    }

    @Test
    void la_ficha_de_producto_no_cambia_el_precio_ni_los_usos_del_menu() {
        Producto normal = menu(TipoMenu.NORMAL);
        cambiarPrecioMenu.ejecutar("normal", 1200L, ADMIN_SEMBRADO_ID);

        var otroPrecio = new Producto.DatosActualizacion(normal.getNombre(), normal.getCategoria(),
                normal.getUnidadBase(), true, false, false, 900L, false);
        var yaNoVendible = new Producto.DatosActualizacion(normal.getNombre(), normal.getCategoria(),
                normal.getUnidadBase(), false, true, false, 1200L, false);

        assertThat(error(actualizarProducto.ejecutar(normal.getId(), otroPrecio, ADMIN_SEMBRADO_ID)))
                .isEqualTo(ErrorCatalogo.PRODUCTO_DE_MENU);
        assertThat(error(actualizarProducto.ejecutar(normal.getId(), yaNoVendible, ADMIN_SEMBRADO_ID)))
                .isEqualTo(ErrorCatalogo.PRODUCTO_DE_MENU);
        assertThat(menu(TipoMenu.NORMAL).getPrecioVenta()).isEqualTo(1200L);
    }

    @Test
    void la_ficha_de_producto_si_puede_renombrar_el_menu_aunque_no_tenga_precio_todavia() {
        Producto extra = menu(TipoMenu.EXTRA);
        var renombrado = new Producto.DatosActualizacion("Menú especial", extra.getCategoria(), extra.getUnidadBase(),
                true, false, false, extra.getPrecioVenta(), false);

        Resultado<Producto, ErrorCatalogo> resultado = actualizarProducto.ejecutar(extra.getId(), renombrado,
                ADMIN_SEMBRADO_ID);

        assertThat(resultado.esExito()).isTrue();
        assertThat(menu(TipoMenu.EXTRA).getNombre()).isEqualTo("Menú especial");
    }

    @Test
    void no_se_crean_otros_productos_en_la_categoria_del_menu() {
        CategoriaProducto categoriaMenu = menu(TipoMenu.NORMAL).getCategoria();

        Resultado<Producto, ErrorCatalogo> resultado = crearProducto.ejecutar("MENU-OTRO", "Menú ejecutivo",
                categoriaMenu.getId(), menu(TipoMenu.NORMAL).getUnidadBase().getId(), true, false, false, 2500L,
                false, ADMIN_SEMBRADO_ID);

        assertThat(error(resultado)).isEqualTo(ErrorCatalogo.CATEGORIA_RESERVADA);
    }

    @Test
    void otro_producto_no_se_puede_mover_a_la_categoria_del_menu() {
        CategoriaProducto categoriaMenu = menu(TipoMenu.NORMAL).getCategoria();
        CategoriaProducto platosDeFondo = categorias.findById("01M39A0RZ0000000000000S015").orElseThrow();
        Producto lomo = crearProducto.ejecutar("LOMO-MT", "Lomo saltado", platosDeFondo.getId(),
                menu(TipoMenu.NORMAL).getUnidadBase().getId(), true, false, false, 2200L, false,
                ADMIN_SEMBRADO_ID).valorOLanza();

        var alMenu = new Producto.DatosActualizacion("Lomo saltado", categoriaMenu, lomo.getUnidadBase(), true, false,
                false, 2200L, false);

        assertThat(error(actualizarProducto.ejecutar(lomo.getId(), alMenu, ADMIN_SEMBRADO_ID)))
                .isEqualTo(ErrorCatalogo.CATEGORIA_RESERVADA);
    }

    private static ErrorCatalogo error(Resultado<?, ErrorCatalogo> resultado) {
        assertThat(resultado.esExito()).isFalse();
        return ((Resultado.Fracaso<?, ErrorCatalogo>) resultado).error();
    }
}
