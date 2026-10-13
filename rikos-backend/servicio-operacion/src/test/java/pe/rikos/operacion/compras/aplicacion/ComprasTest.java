package pe.rikos.operacion.compras.aplicacion;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pe.rikos.contratos.Resultado;
import pe.rikos.operacion.catalogo.aplicacion.CalculadoraCostoReceta;
import pe.rikos.operacion.catalogo.infraestructura.CategoriaProducto;
import pe.rikos.operacion.catalogo.infraestructura.PresentacionCompra;
import pe.rikos.operacion.catalogo.infraestructura.PresentacionCompraRepositorio;
import pe.rikos.operacion.catalogo.infraestructura.Producto;
import pe.rikos.operacion.catalogo.infraestructura.ProductoRepositorio;
import pe.rikos.operacion.catalogo.infraestructura.Proveedor;
import pe.rikos.operacion.catalogo.infraestructura.ProveedorRepositorio;
import pe.rikos.operacion.catalogo.infraestructura.UnidadMedida;
import pe.rikos.operacion.compras.aplicacion.dto.FilaCompraDto;
import pe.rikos.operacion.compras.aplicacion.dto.ItemDetalleCompraDto;
import pe.rikos.operacion.compras.aplicacion.dto.SolicitudRegistroCompra;
import pe.rikos.operacion.compras.dominio.Compra;
import pe.rikos.operacion.compras.dominio.CompraRepositorio;
import pe.rikos.operacion.compras.dominio.DetalleCompraRepositorio;
import pe.rikos.operacion.compras.dominio.EstadoCompra;
import pe.rikos.operacion.compras.dominio.TipoDocumentoCompra;
import pe.rikos.operacion.comun.auditoria.RegistradorAuditoria;
import pe.rikos.operacion.identidad.infraestructura.Empresa;
import pe.rikos.operacion.identidad.infraestructura.Local;
import pe.rikos.operacion.identidad.infraestructura.LocalRepositorio;
import pe.rikos.operacion.identidad.infraestructura.UsuarioRepositorio;
import pe.rikos.operacion.inventario.infraestructura.Existencia;
import pe.rikos.operacion.inventario.infraestructura.ExistenciaRepositorio;
import pe.rikos.operacion.inventario.infraestructura.MovimientoInventario;
import pe.rikos.operacion.inventario.infraestructura.MovimientoInventarioRepositorio;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ComprasTest {

    @Mock private CompraRepositorio compras;
    @Mock private DetalleCompraRepositorio detalles;
    @Mock private LocalRepositorio locales;
    @Mock private ProveedorRepositorio proveedores;
    @Mock private ProductoRepositorio productos;
    @Mock private PresentacionCompraRepositorio presentaciones;
    @Mock private ExistenciaRepositorio existencias;
    @Mock private MovimientoInventarioRepositorio movimientos;
    @Mock private CalculadoraCostoReceta calculadoraCostoReceta;
    @Mock private RegistradorAuditoria auditoria;
    @Mock private UsuarioRepositorio usuarios;

    private RegistrarCompra registrarCompra;
    private ConfirmarCompra confirmarCompra;

    private Local local;
    private Proveedor proveedor;
    private UnidadMedida kg;
    private CategoriaProducto categoria;
    private Producto pollo;
    private PresentacionCompra sacoPollo;

    @BeforeEach
    void setUp() {
        registrarCompra = new RegistrarCompra(compras, detalles, locales, proveedores, productos, presentaciones,
                existencias, movimientos, usuarios, calculadoraCostoReceta, auditoria);
        confirmarCompra = new ConfirmarCompra(compras, detalles, productos, existencias, movimientos, usuarios,
                calculadoraCostoReceta, auditoria);

        Empresa empresa = new Empresa("01EMP000000000000000000001", "20123456789", "Rikos SAC", "Rikos", "Av. Principal 123");
        local = new Local("01M2QZ60D35B90CW00EJA15VFC", empresa, "LOC-01", "San Isidro", "Calle Los Olivos 123", "actor-1");
        proveedor = new Proveedor("01M2QZ60PRV000000000000001", "20123456789", "Avícola San Fernando S.A.",
                "San Fernando", "987654321", "ventas@sanfernando.pe", "Av. República de Panamá", "Juan Perez", "actor-1");

        kg = new UnidadMedida("01M2QZ60U00000000000000001", "kg", "Kilogramo", "masa");
        categoria = new CategoriaProducto("01M2QZ60C00000000000000001", "Carnes y Aves", "insumo", null, 1, "actor-1");
        pollo = new Producto("01M2QZ60P10000000000000001", "INS-POLLO", "Pechuga de Pollo", categoria, kg,
                false, true, false, null, true, "actor-1");

        // Presentación: Jaba de 20 kg
        sacoPollo = new PresentacionCompra("01M2QZ60PRS000000000000001", pollo, "Jaba 20kg", kg,
                new BigDecimal("20.000000"), true);

        when(compras.save(any())).thenAnswer(i -> i.getArgument(0));
        when(compras.saveAndFlush(any())).thenAnswer(i -> i.getArgument(0));
        when(movimientos.save(any())).thenAnswer(i -> i.getArgument(0));
        when(movimientos.saveAndFlush(any())).thenAnswer(i -> i.getArgument(0));
        when(existencias.save(any())).thenAnswer(i -> i.getArgument(0));
        when(existencias.saveAndFlush(any())).thenAnswer(i -> i.getArgument(0));
        when(productos.save(any())).thenAnswer(i -> i.getArgument(0));
        when(productos.saveAndFlush(any())).thenAnswer(i -> i.getArgument(0));
    }

    @Test
    void registrarCompraBorrador_conDetallesValidos_creaCompraEnBorradorSinAfectarStock() {
        when(locales.findById(local.getId())).thenReturn(Optional.of(local));
        when(proveedores.findById(proveedor.getId())).thenReturn(Optional.of(proveedor));
        when(productos.findById(pollo.getId())).thenReturn(Optional.of(pollo));
        when(presentaciones.findById(sacoPollo.getId())).thenReturn(Optional.of(sacoPollo));
        when(compras.existeComprobanteDuplicado(eq(proveedor.getId()), eq(TipoDocumentoCompra.FACTURA),
                eq("F001"), eq("000123"), eq(EstadoCompra.ANULADA), any())).thenReturn(false);
        when(compras.save(any(Compra.class))).thenAnswer(i -> i.getArgument(0));

        SolicitudRegistroCompra solicitud = new SolicitudRegistroCompra(
                proveedor.getId(),
                TipoDocumentoCompra.FACTURA,
                "F001",
                "000123",
                LocalDate.now(),
                "Compra semanal de pollo",
                false, // borrador
                List.of(new ItemDetalleCompraDto(pollo.getId(), sacoPollo.getId(), new BigDecimal("2"), 30000L)) // 2 jabas a S/ 300.00 c/u = S/ 600.00
        );

        Resultado<FilaCompraDto, ErrorCompra> resultado = registrarCompra.ejecutar(local.getId(), solicitud, "01M2QZ60USR000000000000001");

        assertThat(resultado.esExito()).isTrue();
        FilaCompraDto dto = resultado.valorOLanza();
        assertThat(dto.estado()).isEqualTo(EstadoCompra.BORRADOR);
        assertThat(dto.total()).isEqualTo(60000L); // 2 * 30000 céntimos
        assertThat(dto.detalles()).hasSize(1);
        assertThat(dto.detalles().getFirst().cantidadBase()).isEqualByComparingTo(new BigDecimal("40.000000")); // 2 jabas * 20kg = 40kg

        // Verificamos que NO se generó movimiento ni se alteró existencia en borrador
        verify(movimientos, never()).save(any());
        verify(existencias, never()).save(any());
        verify(calculadoraCostoReceta, never()).recalcularEnCascada(any());
    }

    @Test
    void registrarCompra_comprobanteDuplicadoMismoProveedor_retornaError() {
        when(locales.findById(local.getId())).thenReturn(Optional.of(local));
        when(proveedores.findById(proveedor.getId())).thenReturn(Optional.of(proveedor));
        when(compras.existeComprobanteDuplicado(eq(proveedor.getId()), eq(TipoDocumentoCompra.FACTURA),
                eq("F001"), eq("000123"), eq(EstadoCompra.ANULADA), any())).thenReturn(true);

        SolicitudRegistroCompra solicitud = new SolicitudRegistroCompra(
                proveedor.getId(),
                TipoDocumentoCompra.FACTURA,
                "F001",
                "000123",
                LocalDate.now(),
                null,
                false,
                List.of(new ItemDetalleCompraDto(pollo.getId(), null, BigDecimal.TEN, 1500L))
        );

        Resultado<FilaCompraDto, ErrorCompra> resultado = registrarCompra.ejecutar(local.getId(), solicitud, "01M2QZ60USR000000000000001");

        assertThat(resultado.esFracaso()).isTrue();
        assertThat(((Resultado.Fracaso<FilaCompraDto, ErrorCompra>) resultado).error()).isEqualTo(ErrorCompra.COMPROBANTE_DUPLICADO);
        verify(compras, never()).save(any());
    }

    @Test
    void confirmarCompra_aumentaStockEnUnidadBaseYGeneraKardex() {
        Compra compra = new Compra("01M2QZ60CMP000000000000001", local, proveedor, TipoDocumentoCompra.FACTURA,
                "F001", "000123", LocalDate.now(), 60000L, "Compra", "01M2QZ60USR000000000000001");

        // 2 jabas = 40 kg en unidad base a S/ 600.00 total -> S/ 15.00 por kg
        compra.agregarDetalle(new pe.rikos.operacion.compras.dominio.DetalleCompra(
                "01M2QZ60DTL000000000000001", pollo, sacoPollo, new BigDecimal("2"), new BigDecimal("40.000000"),
                30000L, 60000L
        ));

        Existencia existenciaActual = new Existencia(local.getId(), pollo.getId(), new BigDecimal("10.000000"));
        // Stock actual inicial = 5.000000
        existenciaActual.actualizarStock(new BigDecimal("5.000000"), null);

        when(compras.findByIdAndLocalId(compra.getId(), local.getId())).thenReturn(Optional.of(compra));
        when(existencias.findByLocalIdAndProductoId(local.getId(), pollo.getId())).thenReturn(Optional.of(existenciaActual));
        when(movimientos.save(any(MovimientoInventario.class))).thenAnswer(i -> i.getArgument(0));
        when(compras.save(any(Compra.class))).thenAnswer(i -> i.getArgument(0));

        Resultado<FilaCompraDto, ErrorCompra> resultado = confirmarCompra.ejecutar(local.getId(), compra.getId(), "01M2QZ60USR000000000000001");

        assertThat(resultado.esExito()).isTrue();
        assertThat(resultado.valorOLanza().estado()).isEqualTo(EstadoCompra.CONFIRMADA);

        // Existencia debe haber subido de 5 a 45 (5 + 40 kg)
        assertThat(existenciaActual.getStockActual()).isEqualByComparingTo(new BigDecimal("45.000000"));
        verify(existencias).saveAndFlush(existenciaActual);

        // Movimiento de kardex guardado con origen compra y signo entrada
        verify(movimientos).saveAndFlush(any(MovimientoInventario.class));

        // Costo de última compra de pollo actualizado: S/ 600.00 / 40 kg = S/ 15.000000
        assertThat(pollo.getCostoUltimaCompra()).isEqualByComparingTo(new BigDecimal("15.000000"));
        assertThat(pollo.getOrigenCosto()).isEqualTo("compra");
        assertThat(pollo.getFechaUltimaCompra()).isEqualTo(compra.getFechaDocumento());

        // Verificamos recálculo en cascada de recetas futuras
        verify(calculadoraCostoReceta).recalcularEnCascada(pollo.getId());
    }

    @Test
    void confirmarCompra_conStockNegativoPrevio_incrementaCorrectamente() {
        Compra compra = new Compra("01M2QZ60CMP000000000000002", local, proveedor, TipoDocumentoCompra.BOLETA,
                "B001", "000555", LocalDate.now(), 15000L, "Compra pollo", "01M2QZ60USR000000000000001");

        // 10 kg directos a S/ 15.00 c/u = S/ 150.00
        compra.agregarDetalle(new pe.rikos.operacion.compras.dominio.DetalleCompra(
                "01M2QZ60DTL000000000000002", pollo, null, BigDecimal.TEN, BigDecimal.TEN,
                1500L, 15000L
        ));

        // Existencia previa en negativo: -3.000000 (RN-050)
        Existencia existenciaNegativa = new Existencia(local.getId(), pollo.getId(), new BigDecimal("5.000000"));
        existenciaNegativa.actualizarStock(new BigDecimal("-3.000000"), null);

        when(compras.findByIdAndLocalId(compra.getId(), local.getId())).thenReturn(Optional.of(compra));
        when(existencias.findByLocalIdAndProductoId(local.getId(), pollo.getId())).thenReturn(Optional.of(existenciaNegativa));
        when(movimientos.save(any(MovimientoInventario.class))).thenAnswer(i -> i.getArgument(0));
        when(compras.save(any(Compra.class))).thenAnswer(i -> i.getArgument(0));

        Resultado<FilaCompraDto, ErrorCompra> resultado = confirmarCompra.ejecutar(local.getId(), compra.getId(), "01M2QZ60USR000000000000001");

        assertThat(resultado.esExito()).isTrue();
        // -3 + 10 = 7.000000
        assertThat(existenciaNegativa.getStockActual()).isEqualByComparingTo(new BigDecimal("7.000000"));
    }

    @Test
    void registrarCompraDirecta_confirmadaInmediatamente_ejecutaTodoEnUnSoloPaso() {
        when(locales.findById(local.getId())).thenReturn(Optional.of(local));
        when(proveedores.findById(proveedor.getId())).thenReturn(Optional.of(proveedor));
        when(productos.findById(pollo.getId())).thenReturn(Optional.of(pollo));
        when(compras.existeComprobanteDuplicado(any(), any(), any(), any(), any(), any())).thenReturn(false);
        when(compras.save(any(Compra.class))).thenAnswer(i -> i.getArgument(0));
        when(existencias.findByLocalIdAndProductoId(local.getId(), pollo.getId())).thenReturn(Optional.empty());
        when(movimientos.save(any(MovimientoInventario.class))).thenAnswer(i -> i.getArgument(0));

        SolicitudRegistroCompra solicitud = new SolicitudRegistroCompra(
                proveedor.getId(),
                TipoDocumentoCompra.BOLETA,
                "B001",
                "000999",
                LocalDate.now(),
                "Compra directa",
                true, // Confirmar de inmediato
                List.of(new ItemDetalleCompraDto(pollo.getId(), null, new BigDecimal("10.000000"), 1500L))
        );

        Resultado<FilaCompraDto, ErrorCompra> resultado = registrarCompra.ejecutar(local.getId(), solicitud, "01M2QZ60USR000000000000001");

        assertThat(resultado.esExito()).isTrue();
        assertThat(resultado.valorOLanza().estado()).isEqualTo(EstadoCompra.CONFIRMADA);
        verify(movimientos).saveAndFlush(any(MovimientoInventario.class));
        verify(existencias).saveAndFlush(any(Existencia.class));
        verify(calculadoraCostoReceta).recalcularEnCascada(pollo.getId());
    }
}
