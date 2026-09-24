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
class CrearPresentacionTest {

    @Autowired
    private CrearPresentacion crearPresentacion;
    @Autowired
    private CategoriaProductoRepositorio categorias;
    @Autowired
    private UnidadMedidaRepositorio unidades;
    @Autowired
    private ProductoRepositorio productos;

    private Producto productoInsumo() {
        CategoriaProducto raiz = categorias.save(new CategoriaProducto(
                Identificador.nuevo().valor(), "Insumos test presentacion", "insumo", null, 1, null));
        CategoriaProducto hoja = categorias.save(new CategoriaProducto(
                Identificador.nuevo().valor(), raiz, "Abarrotes test presentacion", 1, null));
        UnidadMedida kg = unidades.save(new UnidadMedida(Identificador.nuevo().valor(), "kgpres", "Kilogramo pres",
                "masa"));
        return productos.save(new Producto(Identificador.nuevo().valor(), "ARR-001", "Arroz", hoja, kg,
                false, true, false, null, true, null));
    }

    @Test
    void el_factor_se_calcula_correctamente_1_saco_50_kg() {
        Producto arroz = productoInsumo();
        UnidadMedida saco = unidades.save(new UnidadMedida(Identificador.nuevo().valor(), "sacopres", "Saco pres",
                "masa"));

        Resultado<PresentacionCompra, ErrorCatalogo> resultado = crearPresentacion.ejecutar(
                arroz.getId(), "Saco 50 kg", saco.getId(), new BigDecimal("50"), true, null);

        assertThat(resultado.esExito()).isTrue();
        assertThat(resultado.valorOLanza().getFactorAUnidadBase()).isEqualByComparingTo("50");
    }

    @Test
    void rechaza_un_factor_cero_o_negativo() {
        Producto arroz = productoInsumo();
        UnidadMedida saco = unidades.save(new UnidadMedida(Identificador.nuevo().valor(), "sacopres2",
                "Saco pres 2", "masa"));

        Resultado<PresentacionCompra, ErrorCatalogo> resultado = crearPresentacion.ejecutar(
                arroz.getId(), "Saco invalido", saco.getId(), new BigDecimal("0"), true, null);

        assertThat(resultado.esExito()).isFalse();
        assertThat(((Resultado.Fracaso<?, ErrorCatalogo>) resultado).error())
                .isEqualTo(ErrorCatalogo.FACTOR_INVALIDO);
    }

    @Test
    void rechaza_una_segunda_predeterminada_activa_para_el_mismo_producto() {
        Producto arroz = productoInsumo();
        UnidadMedida saco = unidades.save(new UnidadMedida(Identificador.nuevo().valor(), "sacopres3",
                "Saco pres 3", "masa"));
        UnidadMedida bolsa = unidades.save(new UnidadMedida(Identificador.nuevo().valor(), "bolsapres",
                "Bolsa pres", "masa"));
        crearPresentacion.ejecutar(arroz.getId(), "Saco 50 kg", saco.getId(), new BigDecimal("50"), true, null);

        Resultado<PresentacionCompra, ErrorCatalogo> resultado = crearPresentacion.ejecutar(
                arroz.getId(), "Bolsa 5 kg", bolsa.getId(), new BigDecimal("5"), true, null);

        assertThat(resultado.esExito()).isFalse();
        assertThat(((Resultado.Fracaso<?, ErrorCatalogo>) resultado).error())
                .isEqualTo(ErrorCatalogo.PREDETERMINADA_DUPLICADA);
    }
}
