package pe.rikos.operacion.inventario.aplicacion;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pe.rikos.contratos.Resultado;
import pe.rikos.operacion.catalogo.infraestructura.CategoriaProducto;
import pe.rikos.operacion.catalogo.infraestructura.Producto;
import pe.rikos.operacion.catalogo.infraestructura.ProductoRepositorio;
import pe.rikos.operacion.catalogo.infraestructura.UnidadMedida;
import pe.rikos.operacion.comun.auditoria.RegistradorAuditoria;
import pe.rikos.operacion.identidad.infraestructura.Empresa;
import pe.rikos.operacion.identidad.infraestructura.Local;
import pe.rikos.operacion.identidad.infraestructura.LocalRepositorio;
import pe.rikos.operacion.inventario.aplicacion.dto.FilaExistencia;
import pe.rikos.operacion.inventario.aplicacion.dto.ResumenAlertasInventario;
import pe.rikos.operacion.inventario.infraestructura.Existencia;
import pe.rikos.operacion.inventario.infraestructura.ExistenciaRepositorio;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExistenciasTest {

    @Mock
    private LocalRepositorio locales;

    @Mock
    private ProductoRepositorio productos;

    @Mock
    private ExistenciaRepositorio existencias;

    @Mock
    private RegistradorAuditoria auditoria;

    private ConsultarExistencias consultarExistencias;
    private ConfigurarStockMinimo configurarStockMinimo;

    private Local local;
    private Producto pollo;
    private Producto arroz;

    @BeforeEach
    void setUp() {
        consultarExistencias = new ConsultarExistencias(locales, productos, existencias);
        configurarStockMinimo = new ConfigurarStockMinimo(locales, productos, existencias, auditoria);

        Empresa empresa = new Empresa("01EMP000000000000000000001", "20123456789", "Rikos SAC", "Rikos", "Av. Principal 123");
        local = new Local("01LOC000000000000000000001", empresa, "LOC-01", "Local Central", "Av Central 456", "actor-1");

        CategoriaProducto catCarnes = new CategoriaProducto("01CAT000000000000000000001", "Carnes", "insumo", null, 1, "actor-1");
        UnidadMedida kg = new UnidadMedida("01UNI000000000000000000001", "kg", "Kilogramos", "masa");

        pollo = new Producto("01PROD0000000000000000001", "POL-01", "Pollo Entero", catCarnes, kg,
                false, true, false, null, true, "actor-1");

        arroz = new Producto("01PROD0000000000000000002", "ARR-01", "Arroz Costeño", catCarnes, kg,
                false, true, false, null, true, "actor-1");
    }

    @Test
    @DisplayName("CA1: Proyección on-demand de existencias con stock 0 cuando no hay registro en BD")
    void ca1_proyeccionOnDemand() {
        when(locales.findById("01LOC000000000000000000001")).thenReturn(Optional.of(local));
        when(productos.findByActivoTrueOrderByNombreAsc()).thenReturn(List.of(pollo, arroz));
        when(existencias.findByLocalId("01LOC000000000000000000001")).thenReturn(List.of());

        Resultado<List<FilaExistencia>, ErrorInventario> res = consultarExistencias.ejecutar("01LOC000000000000000000001", null);

        assertThat(res.esExito()).isTrue();
        List<FilaExistencia> filas = res.valorOLanza();
        assertThat(filas).hasSize(2);

        FilaExistencia filaPollo = filas.stream().filter(f -> f.productoId().equals(pollo.getId())).findFirst().orElseThrow();
        assertThat(filaPollo.stockActual()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(filaPollo.stockMinimo()).isNull();
        assertThat(filaPollo.estadoAlerta()).isEqualTo("sin_stock");
    }

    @Test
    @DisplayName("CA2: Configurar stock mínimo válido persiste y actualiza la existencia")
    void ca2_configurarStockMinimoValido() {
        when(locales.findById(local.getId())).thenReturn(Optional.of(local));
        when(productos.findById(pollo.getId())).thenReturn(Optional.of(pollo));
        when(existencias.findByLocalIdAndProductoId(local.getId(), pollo.getId())).thenReturn(Optional.empty());
        when(existencias.save(any(Existencia.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BigDecimal nuevoMinimo = new BigDecimal("15.500000");
        Resultado<FilaExistencia, ErrorInventario> res = configurarStockMinimo.ejecutar(local.getId(), pollo.getId(), nuevoMinimo, "actor-1");

        assertThat(res.esExito()).isTrue();
        FilaExistencia fila = res.valorOLanza();
        assertThat(fila.stockMinimo()).isEqualByComparingTo(nuevoMinimo);
        assertThat(fila.estadoAlerta()).isEqualTo("bajo_minimo"); // stockActual es 0, menor a 15.5
        verify(existencias).save(any(Existencia.class));
    }

    @Test
    @DisplayName("CA2: Rechazar stock mínimo negativo con error de validación")
    void ca2_rechazarStockMinimoNegativo() {
        when(locales.findById(local.getId())).thenReturn(Optional.of(local));
        when(productos.findById(pollo.getId())).thenReturn(Optional.of(pollo));

        BigDecimal minimoNegativo = new BigDecimal("-1.000000");
        Resultado<FilaExistencia, ErrorInventario> res = configurarStockMinimo.ejecutar(local.getId(), pollo.getId(), minimoNegativo, "actor-1");

        assertThat(res.esFracaso()).isTrue();
        assertThat(((Resultado.Fracaso<FilaExistencia, ErrorInventario>) res).error()).isEqualTo(ErrorInventario.STOCK_MINIMO_NEGATIVO);
    }

    @Test
    @DisplayName("CA3: Alerta 'bajo_minimo' cuando el stock actual es menor o igual al stock mínimo")
    void ca3_alertaBajoMinimo() {
        Existencia ePollo = new Existencia(local.getId(), pollo.getId(), new BigDecimal("8.000000"), new BigDecimal("10.000000"), null);

        when(locales.findById(local.getId())).thenReturn(Optional.of(local));
        when(productos.findByActivoTrueOrderByNombreAsc()).thenReturn(List.of(pollo));
        when(existencias.findByLocalId(local.getId())).thenReturn(List.of(ePollo));

        Resultado<List<FilaExistencia>, ErrorInventario> res = consultarExistencias.ejecutar(local.getId(), null);

        assertThat(res.esExito()).isTrue();
        FilaExistencia fila = res.valorOLanza().get(0);
        assertThat(fila.stockActual()).isEqualByComparingTo(new BigDecimal("8.000000"));
        assertThat(fila.stockMinimo()).isEqualByComparingTo(new BigDecimal("10.000000"));
        assertThat(fila.estadoAlerta()).isEqualTo("bajo_minimo");
    }

    @Test
    @DisplayName("CA3: Estado 'normal' cuando el stock actual supera el mínimo configurado")
    void ca3_estadoNormal() {
        Existencia ePollo = new Existencia(local.getId(), pollo.getId(), new BigDecimal("25.000000"), new BigDecimal("10.000000"), null);

        when(locales.findById(local.getId())).thenReturn(Optional.of(local));
        when(productos.findByActivoTrueOrderByNombreAsc()).thenReturn(List.of(pollo));
        when(existencias.findByLocalId(local.getId())).thenReturn(List.of(ePollo));

        Resultado<List<FilaExistencia>, ErrorInventario> res = consultarExistencias.ejecutar(local.getId(), null);

        assertThat(res.esExito()).isTrue();
        FilaExistencia fila = res.valorOLanza().get(0);
        assertThat(fila.estadoAlerta()).isEqualTo("normal");
    }

    @Test
    @DisplayName("CA4: Alerta 'negativo' cuando el stock actual cae por debajo de cero")
    void ca4_alertaStockNegativoPermitido() {
        Existencia ePollo = new Existencia(local.getId(), pollo.getId(), new BigDecimal("-3.500000"), new BigDecimal("10.000000"), null);

        when(locales.findById(local.getId())).thenReturn(Optional.of(local));
        when(productos.findByActivoTrueOrderByNombreAsc()).thenReturn(List.of(pollo));
        when(existencias.findByLocalId(local.getId())).thenReturn(List.of(ePollo));

        Resultado<List<FilaExistencia>, ErrorInventario> res = consultarExistencias.ejecutar(local.getId(), null);

        assertThat(res.esExito()).isTrue();
        FilaExistencia fila = res.valorOLanza().get(0);
        assertThat(fila.stockActual()).isEqualByComparingTo(new BigDecimal("-3.500000"));
        assertThat(fila.estadoAlerta()).isEqualTo("negativo");
    }

    @Test
    @DisplayName("Filtros y Resumen: resumen de métricas y filtrado por estado de alerta")
    void filtrosYResumen() {
        Existencia ePollo = new Existencia(local.getId(), pollo.getId(), new BigDecimal("5.000000"), new BigDecimal("10.000000"), null); // bajo_minimo
        Existencia eArroz = new Existencia(local.getId(), arroz.getId(), new BigDecimal("-2.000000"), null, null); // negativo

        when(locales.findById(local.getId())).thenReturn(Optional.of(local));
        when(productos.findByActivoTrueOrderByNombreAsc()).thenReturn(List.of(pollo, arroz));
        when(existencias.findByLocalId(local.getId())).thenReturn(List.of(ePollo, eArroz));

        // Resumen
        Resultado<ResumenAlertasInventario, ErrorInventario> resResumen = consultarExistencias.resumen(local.getId());
        assertThat(resResumen.esExito()).isTrue();
        ResumenAlertasInventario resumen = resResumen.valorOLanza();
        assertThat(resumen.totalProductos()).isEqualTo(2);
        assertThat(resumen.totalBajoMinimo()).isEqualTo(1);
        assertThat(resumen.totalNegativos()).isEqualTo(1);
        assertThat(resumen.totalSinStock()).isEqualTo(0);

        // Filtro bajo mínimo
        Resultado<List<FilaExistencia>, ErrorInventario> resBajoMinimo = consultarExistencias.ejecutar(local.getId(), "bajo_minimo");
        assertThat(resBajoMinimo.valorOLanza()).hasSize(1);
        assertThat(resBajoMinimo.valorOLanza().get(0).productoId()).isEqualTo(pollo.getId());

        // Filtro negativo
        Resultado<List<FilaExistencia>, ErrorInventario> resNegativo = consultarExistencias.ejecutar(local.getId(), "negativo");
        assertThat(resNegativo.valorOLanza()).hasSize(1);
        assertThat(resNegativo.valorOLanza().get(0).productoId()).isEqualTo(arroz.getId());
    }
}
