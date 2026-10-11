package pe.rikos.operacion.catalogo.aplicacion;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import pe.rikos.contratos.Resultado;
import pe.rikos.operacion.catalogo.infraestructura.CategoriaProducto;
import pe.rikos.operacion.catalogo.infraestructura.CategoriaProductoRepositorio;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class ReordenarCategoriasTest {

    @Autowired
    private CrearCategoria crearCategoria;

    @Autowired
    private ReordenarCategorias reordenarCategorias;

    @Autowired
    private CategoriaProductoRepositorio categorias;

    @Test
    void guarda_el_nuevo_orden_de_las_hijas_de_una_categoria() {
        CategoriaProducto raiz =
                crearCategoria.ejecutar("Raiz orden A", "insumo", null, null, 1, null).valorOLanza();
        CategoriaProducto a = crearCategoria.ejecutar("A", "insumo", null, raiz.getId(), 1, null).valorOLanza();
        CategoriaProducto b = crearCategoria.ejecutar("B", "insumo", null, raiz.getId(), 2, null).valorOLanza();
        CategoriaProducto c = crearCategoria.ejecutar("C", "insumo", null, raiz.getId(), 3, null).valorOLanza();

        Resultado<List<CategoriaProducto>, ErrorCatalogo> resultado =
                reordenarCategorias.ejecutar(raiz.getId(), List.of(c.getId(), a.getId(), b.getId()), null);

        assertThat(resultado.esExito()).isTrue();
        List<CategoriaProducto> ordenadas = resultado.valorOLanza().stream()
                .sorted((x, y) -> Integer.compare(x.getOrden(), y.getOrden())).toList();
        assertThat(ordenadas).extracting(CategoriaProducto::getNombre).containsExactly("C", "A", "B");
        assertThat(ordenadas).extracting(CategoriaProducto::getOrden).containsExactly(1, 2, 3);
    }

    @Test
    void rechaza_una_lista_que_no_coincide_con_las_hermanas_reales() {
        CategoriaProducto raiz =
                crearCategoria.ejecutar("Raiz orden B", "insumo", null, null, 1, null).valorOLanza();
        crearCategoria.ejecutar("Hija unica", "insumo", null, raiz.getId(), 1, null);

        Resultado<List<CategoriaProducto>, ErrorCatalogo> resultado =
                reordenarCategorias.ejecutar(raiz.getId(), List.of("no-existe"), null);

        assertThat(resultado.esExito()).isFalse();
        assertThat(((Resultado.Fracaso<?, ErrorCatalogo>) resultado).error())
                .isEqualTo(ErrorCatalogo.NO_ENCONTRADO);
    }

    @Test
    void reordena_las_raices_cuando_no_se_manda_categoria_padre() {
        // El grupo de raices incluye las de la semilla, no solo las que
        // crea este test -- hay que mandar la lista completa, igual que
        // haria la pantalla real con lo que ya tiene cargado.
        List<String> idsActuales = new ArrayList<>();
        for (CategoriaProducto raiz : categorias.findByCategoriaPadreIdIsNullOrderByOrden()) {
            idsActuales.add(raiz.getId());
        }
        List<String> invertidos = new ArrayList<>(idsActuales);
        java.util.Collections.reverse(invertidos);

        Resultado<List<CategoriaProducto>, ErrorCatalogo> resultado =
                reordenarCategorias.ejecutar(null, invertidos, null);

        assertThat(resultado.esExito()).isTrue();
        CategoriaProducto primera = resultado.valorOLanza().stream()
                .filter(c -> c.getId().equals(invertidos.get(0))).findFirst().orElseThrow();
        assertThat(primera.getOrden()).isEqualTo(1);
    }
}
