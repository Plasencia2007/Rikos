package pe.rikos.operacion.inventario.aplicacion;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import pe.rikos.contratos.Resultado;
import pe.rikos.operacion.catalogo.infraestructura.CategoriaProducto;
import pe.rikos.operacion.catalogo.infraestructura.Producto;
import pe.rikos.operacion.catalogo.infraestructura.UnidadMedida;
import pe.rikos.operacion.identidad.infraestructura.Empresa;
import pe.rikos.operacion.identidad.infraestructura.Local;
import pe.rikos.operacion.identidad.infraestructura.LocalRepositorio;
import pe.rikos.operacion.identidad.infraestructura.Usuario;
import pe.rikos.operacion.inventario.aplicacion.dto.FilaKardex;
import pe.rikos.operacion.inventario.aplicacion.dto.FiltroKardex;
import pe.rikos.operacion.inventario.aplicacion.dto.PaginaKardex;
import pe.rikos.operacion.inventario.infraestructura.MovimientoInventario;
import pe.rikos.operacion.inventario.infraestructura.MovimientoInventarioRepositorio;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KardexTest {

    @Mock
    private LocalRepositorio locales;

    @Mock
    private MovimientoInventarioRepositorio movimientos;

    private ConsultarKardex consultarKardex;

    private Local local;
    private Producto pollo;
    private UnidadMedida kg;
    private Usuario admin;

    @BeforeEach
    void setUp() {
        consultarKardex = new ConsultarKardex(locales, movimientos);

        Empresa empresa = new Empresa("01EMP000000000000000000001", "20123456789", "Rikos SAC", "Rikos", "Av Principal 123");
        local = new Local("01LOC000000000000000000001", empresa, "LOC-01", "Local San Isidro", "Av Central 456", "actor-1");

        CategoriaProducto categoria = new CategoriaProducto("01CAT000000000000000000001", "Carnes", "insumo", null, 1, "actor-1");
        kg = new UnidadMedida("01UNI000000000000000000001", "kg", "Kilogramos", "masa");

        pollo = new Producto("01PROD0000000000000000001", "INS-001", "Pechuga de Pollo", categoria, kg,
                false, true, false, null, true, "actor-1");

        admin = new Usuario("01USR000000000000000000001", "admin", "Carlos", "Alvarez",
                null, null, null, "hash", null);
    }

    @Test
    @DisplayName("CA1 & CA2: Consulta de Kardex exitosa con costos visibles cuando tiene permiso")
    void ca1_ca2_consultaKardexConCostosVisibles() {
        when(locales.findById(local.getId())).thenReturn(Optional.of(local));

        MovimientoInventario mov = new MovimientoInventario(
                "01MOV000000000000000000001",
                local,
                pollo,
                "compra",
                "entrada",
                new BigDecimal("10.000000"),
                kg,
                new BigDecimal("10.000000"),
                new BigDecimal("12.500000"), // Costo unitario S/ 12.50
                new BigDecimal("0.000000"),
                new BigDecimal("10.000000"),
                "compra",
                "01COM000000000000000000001",
                null,
                "Factura F001-1234",
                OffsetDateTime.now(),
                admin
        );

        Page<MovimientoInventario> paginaMovs = new PageImpl<>(List.of(mov));
        when(movimientos.findAll(any(Specification.class), any(Pageable.class))).thenReturn(paginaMovs);

        FiltroKardex filtro = new FiltroKardex(null, null, null, null, "desc", 0, 25);
        Resultado<PaginaKardex, ErrorInventario> res = consultarKardex.ejecutar(local.getId(), filtro, true);

        assertThat(res.esExito()).isTrue();
        PaginaKardex pagina = res.valorOLanza();
        assertThat(pagina.totalElementos()).isEqualTo(1);
        assertThat(pagina.elementos()).hasSize(1);

        FilaKardex fila = pagina.elementos().get(0);
        assertThat(fila.id()).isEqualTo("01MOV000000000000000000001");
        assertThat(fila.productoCodigo()).isEqualTo("INS-001");
        assertThat(fila.origen()).isEqualTo("compra");
        assertThat(fila.signo()).isEqualTo("entrada");
        assertThat(fila.cantidad()).isEqualByComparingTo("10.000000");
        assertThat(fila.stockAnterior()).isEqualByComparingTo("0.000000");
        assertThat(fila.stockNuevo()).isEqualByComparingTo("10.000000");
        assertThat(fila.diferencia()).isEqualByComparingTo("10.000000");
        assertThat(fila.costoUnitario()).isEqualByComparingTo("12.500000");
        assertThat(fila.costoTotal()).isEqualByComparingTo("125.00");
        assertThat(fila.registradoPorNombre()).isEqualTo("Carlos Alvarez");
    }

    @Test
    @DisplayName("CA2 & RN-012: Costos son estrictamente nulos cuando puedeVerCostos es false")
    void ca2_rn012_consultaKardexConCostosOcultos() {
        when(locales.findById(local.getId())).thenReturn(Optional.of(local));

        MovimientoInventario mov = new MovimientoInventario(
                "01MOV000000000000000000001",
                local,
                pollo,
                "compra",
                "entrada",
                new BigDecimal("10.000000"),
                kg,
                new BigDecimal("10.000000"),
                new BigDecimal("12.500000"),
                new BigDecimal("0.000000"),
                new BigDecimal("10.000000"),
                "compra",
                "01COM000000000000000000001",
                null,
                "Factura F001-1234",
                OffsetDateTime.now(),
                admin
        );

        Page<MovimientoInventario> paginaMovs = new PageImpl<>(List.of(mov));
        when(movimientos.findAll(any(Specification.class), any(Pageable.class))).thenReturn(paginaMovs);

        FiltroKardex filtro = new FiltroKardex(null, null, null, null, "desc", 0, 25);
        Resultado<PaginaKardex, ErrorInventario> res = consultarKardex.ejecutar(local.getId(), filtro, false);

        assertThat(res.esExito()).isTrue();
        FilaKardex fila = res.valorOLanza().elementos().get(0);
        assertThat(fila.costoUnitario()).isNull();
        assertThat(fila.costoTotal()).isNull();
        assertThat(fila.cantidad()).isEqualByComparingTo("10.000000");
    }

    @Test
    @DisplayName("CA3: Filtrado por producto, origen y fechas con salida negativa")
    void ca3_filtroPorProductoYOrigenConSalida() {
        when(locales.findById(local.getId())).thenReturn(Optional.of(local));

        MovimientoInventario salida = new MovimientoInventario(
                "01MOV000000000000000000002",
                local,
                pollo,
                "venta",
                "salida",
                new BigDecimal("2.500000"),
                kg,
                new BigDecimal("2.500000"),
                null,
                new BigDecimal("10.000000"),
                new BigDecimal("7.500000"),
                "pedido",
                "01PED000000000000000000001",
                null,
                "Venta de plato",
                OffsetDateTime.now(),
                admin
        );

        Page<MovimientoInventario> paginaMovs = new PageImpl<>(List.of(salida));
        when(movimientos.findAll(any(Specification.class), any(Pageable.class))).thenReturn(paginaMovs);

        FiltroKardex filtro = new FiltroKardex(pollo.getId(), "venta", LocalDate.now().minusDays(1), LocalDate.now(), "asc", 0, 10);
        Resultado<PaginaKardex, ErrorInventario> res = consultarKardex.ejecutar(local.getId(), filtro, true);

        assertThat(res.esExito()).isTrue();
        FilaKardex fila = res.valorOLanza().elementos().get(0);
        assertThat(fila.origen()).isEqualTo("venta");
        assertThat(fila.signo()).isEqualTo("salida");
        assertThat(fila.diferencia()).isEqualByComparingTo("-2.500000");
        assertThat(fila.stockAnterior()).isEqualByComparingTo("10.000000");
        assertThat(fila.stockNuevo()).isEqualByComparingTo("7.500000");
    }

    @Test
    @DisplayName("Local inexistente devuelve ErrorInventario.LOCAL_NO_ENCONTRADO")
    void localInexistente_devuelveError() {
        when(locales.findById("local-fantasma")).thenReturn(Optional.empty());

        Resultado<PaginaKardex, ErrorInventario> res = consultarKardex.ejecutar("local-fantasma",
                new FiltroKardex(null, null, null, null, null, null, null), true);

        assertThat(res.esFracaso()).isTrue();
        assertThat(((Resultado.Fracaso<PaginaKardex, ErrorInventario>) res).error())
                .isEqualTo(ErrorInventario.LOCAL_NO_ENCONTRADO);
    }
}
