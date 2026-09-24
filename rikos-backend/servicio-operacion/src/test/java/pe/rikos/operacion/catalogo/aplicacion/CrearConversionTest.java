package pe.rikos.operacion.catalogo.aplicacion;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import pe.rikos.contratos.Identificador;
import pe.rikos.contratos.Resultado;
import pe.rikos.operacion.catalogo.infraestructura.*;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class CrearConversionTest {

    @Autowired
    private CrearConversion crearConversion;
    @Autowired
    private CategoriaProductoRepositorio categorias;
    @Autowired
    private UnidadMedidaRepositorio unidades;
    @Autowired
    private ProductoRepositorio productos;

    private Producto productoInsumo(String codigo) {
        CategoriaProducto raiz = categorias.save(new CategoriaProducto(
                Identificador.nuevo().valor(), "Insumos test conversion " + codigo, "insumo", null, 1, null));
        CategoriaProducto hoja = categorias.save(new CategoriaProducto(
                Identificador.nuevo().valor(), raiz, "Abarrotes test conversion " + codigo, 1, null));
        UnidadMedida l = unidades.save(new UnidadMedida(Identificador.nuevo().valor(), "l" + codigo, "Litro " + codigo,
                "volumen"));
        return productos.save(new Producto(Identificador.nuevo().valor(), codigo, "Producto " + codigo, hoja, l,
                false, true, false, null, true, null));
    }

    @Test
    void aplica_el_factor_correctamente() {
        Producto aceite = productoInsumo("ACE-001");
        UnidadMedida litro = aceite.getUnidadBase();
        UnidadMedida kg = unidades.save(new UnidadMedida(Identificador.nuevo().valor(), "kgconv", "Kilogramo conv",
                "masa"));

        Resultado<ConversionUnidad, ErrorCatalogo> resultado =
                crearConversion.ejecutar(aceite.getId(), litro.getId(), kg.getId(), new BigDecimal("0.92"), null);

        assertThat(resultado.esExito()).isTrue();
        BigDecimal resultadoConversion = new BigDecimal("2").multiply(resultado.valorOLanza().getFactor());
        assertThat(resultadoConversion).isEqualByComparingTo("1.84");
    }

    @Test
    void rechaza_un_par_de_unidades_repetido_para_el_mismo_producto() {
        Producto aceite = productoInsumo("ACE-002");
        UnidadMedida litro = aceite.getUnidadBase();
        UnidadMedida kg = unidades.save(new UnidadMedida(Identificador.nuevo().valor(), "kgconv2",
                "Kilogramo conv 2", "masa"));
        crearConversion.ejecutar(aceite.getId(), litro.getId(), kg.getId(), new BigDecimal("0.92"), null);

        Resultado<ConversionUnidad, ErrorCatalogo> resultado =
                crearConversion.ejecutar(aceite.getId(), litro.getId(), kg.getId(), new BigDecimal("0.95"), null);

        assertThat(resultado.esExito()).isFalse();
        assertThat(((Resultado.Fracaso<?, ErrorCatalogo>) resultado).error())
                .isEqualTo(ErrorCatalogo.PAR_UNIDADES_DUPLICADO);
    }

    @Test
    void rechaza_factor_cero_o_negativo() {
        Producto aceite = productoInsumo("ACE-003");
        UnidadMedida litro = aceite.getUnidadBase();
        UnidadMedida kg = unidades.save(new UnidadMedida(Identificador.nuevo().valor(), "kgconv3",
                "Kilogramo conv 3", "masa"));

        Resultado<ConversionUnidad, ErrorCatalogo> resultado =
                crearConversion.ejecutar(aceite.getId(), litro.getId(), kg.getId(), new BigDecimal("-1"), null);

        assertThat(resultado.esExito()).isFalse();
        assertThat(((Resultado.Fracaso<?, ErrorCatalogo>) resultado).error())
                .isEqualTo(ErrorCatalogo.FACTOR_INVALIDO);
    }
}
