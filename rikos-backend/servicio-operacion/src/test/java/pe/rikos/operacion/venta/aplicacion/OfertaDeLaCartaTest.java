package pe.rikos.operacion.venta.aplicacion;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import pe.rikos.operacion.catalogo.infraestructura.Producto;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Sprint 6b, categorias del pedido: cada producto de la carta trae su `grupo` (entrada, segundo o extra, calculado desde
 * la categoria RAIZ, no por el nombre) y la `categoria` (la subcategoria), y la carta sale ordenada por grupo y nombre.
 */
@SpringBootTest
@Transactional
class OfertaDeLaCartaTest extends ComandasBase {

    /** Categorias sembradas (V15/V16): subcategorias de Entradas, Segundos y guisos, Extras, y una de platos fijos. */
    private static final String ENSALADAS = "01M39A0RZ0000000000000S002";
    private static final String POLLO = "01M39A0RZ0000000000000S004";
    private static final String SEGUNDOS_ESPECIALES = "01M39A0RZ0000000000000S012";
    private static final String JUGOS_Y_REFRESCOS = "01M39A0RZ0000000000000S018";
    /** Una categoria RAIZ: Postres (componente de menu "postre"). */
    private static final String POSTRES = "01M39A0RZ0000000000000R005";

    @Autowired
    ConsultaPedidos consulta;

    private List<ProductoOfrecido> carta() {
        return consulta.oferta(pedido.getId()).valorOLanza().carta();
    }

    private ProductoOfrecido deLaCarta(Producto producto) {
        return carta().stream().filter(p -> p.id().equals(producto.getId())).findFirst().orElseThrow();
    }

    @Test
    void un_producto_de_una_subcategoria_de_entradas_es_del_grupo_entrada_con_su_categoria() {
        Producto ensalada = plato("Ensalada", ENSALADAS);

        ProductoOfrecido ofrecido = deLaCarta(ensalada);

        assertThat(ofrecido.grupo()).isEqualTo("entrada");
        assertThat(ofrecido.categoria()).isEqualTo("Ensaladas");
        assertThat(ofrecido.precio()).isEqualTo(1500L);
    }

    @Test
    void un_producto_de_segundos_es_del_grupo_segundo_y_uno_de_extras_del_grupo_extra() {
        Producto arroz = plato("Arroz con pollo", POLLO);
        Producto chaufa = plato("Chaufa", SEGUNDOS_ESPECIALES);

        assertThat(deLaCarta(arroz).grupo()).isEqualTo("segundo");
        assertThat(deLaCarta(arroz).categoria()).isEqualTo("Pollo");
        assertThat(deLaCarta(chaufa).grupo()).isEqualTo("extra");
        assertThat(deLaCarta(chaufa).categoria()).isEqualTo("Segundos especiales");
    }

    @Test
    void las_bebidas_y_los_postres_no_son_de_ningun_grupo_pero_siguen_en_la_carta() {
        Producto jugo = plato("Jugo de maracuya", JUGOS_Y_REFRESCOS);
        // Un producto colgado DIRECTO de una categoria raiz (Postres) tampoco es de entrada, segundo ni extra.
        Producto mazamorra = plato("Mazamorra de prueba", POSTRES);

        assertThat(deLaCarta(jugo).grupo()).isNull();
        assertThat(deLaCarta(jugo).categoria()).isEqualTo("Jugos y refrescos");
        assertThat(deLaCarta(mazamorra).grupo()).isNull();
    }

    @Test
    void la_carta_trae_la_foto_del_producto_y_nula_si_no_tiene() {
        Producto conFoto = plato("Ceviche con foto", ENSALADAS);
        Producto sinFoto = plato("Ceviche sin foto", ENSALADAS);
        conFoto.setImagen("data:image/jpeg;base64,/9j/4AAQ");

        assertThat(deLaCarta(conFoto).imagen()).isEqualTo("data:image/jpeg;base64,/9j/4AAQ");
        assertThat(deLaCarta(sinFoto).imagen()).isNull();
    }

    @Test
    void el_grupo_sale_de_la_categoria_y_no_del_nombre() {
        // Se llama "Extra" pero cuelga de entradas; se llama "Entrada" pero cuelga de extras.
        Producto engano1 = plato("Extra de la casa", ENSALADAS);
        Producto engano2 = plato("Entrada falsa", SEGUNDOS_ESPECIALES);

        assertThat(deLaCarta(engano1).grupo()).isEqualTo("entrada");
        assertThat(deLaCarta(engano2).grupo()).isEqualTo("extra");
    }

    @Test
    void la_carta_sale_por_grupo_entrada_segundo_extra_otros_y_por_nombre_dentro_de_cada_uno() {
        Producto extraB = plato("Zz Milanesa", SEGUNDOS_ESPECIALES);
        Producto extraA = plato("Zz Chaufa", SEGUNDOS_ESPECIALES);
        Producto segundo = plato("Zz Ají de gallina", POLLO);
        Producto entradaB = plato("Zz Sopa", ENSALADAS);
        Producto entradaA = plato("Zz Ceviche", ENSALADAS);
        Producto otro = plato("Zz Gaseosa", JUGOS_Y_REFRESCOS);

        List<String> mios = carta().stream()
                .filter(p -> List.of(extraB, extraA, segundo, entradaB, entradaA, otro).stream()
                        .anyMatch(m -> m.getId().equals(p.id())))
                .map(p -> p.nombre().replaceAll(" PV-[A-Z0-9]+$", "")).toList();

        assertThat(mios).containsExactly("Zz Ceviche", "Zz Sopa", "Zz Ají de gallina", "Zz Chaufa", "Zz Milanesa",
                "Zz Gaseosa");
    }

    @Test
    void un_producto_sin_precio_o_agotado_no_sale_en_la_carta() {
        Producto sinPrecio = productoDeCarta("Sin precio", null);
        Producto conPrecio = plato("Con precio", ENSALADAS);

        assertThat(carta()).extracting(ProductoOfrecido::id).doesNotContain(sinPrecio.getId()).contains(conPrecio.getId());
    }

    @Test
    void el_menu_normal_y_el_extra_no_son_productos_de_la_carta() {
        Producto menu = productos.findByTipoMenu("normal").orElseThrow();

        assertThat(carta()).extracting(ProductoOfrecido::id).doesNotContain(menu.getId());
    }
}
