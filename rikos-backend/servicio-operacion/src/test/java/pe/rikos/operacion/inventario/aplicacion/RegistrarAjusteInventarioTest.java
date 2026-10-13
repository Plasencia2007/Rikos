package pe.rikos.operacion.inventario.aplicacion;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pe.rikos.contratos.Resultado;
import pe.rikos.operacion.catalogo.infraestructura.CategoriaProducto;
import pe.rikos.operacion.catalogo.infraestructura.Motivo;
import pe.rikos.operacion.catalogo.infraestructura.MotivoRepositorio;
import pe.rikos.operacion.catalogo.infraestructura.Producto;
import pe.rikos.operacion.catalogo.infraestructura.ProductoRepositorio;
import pe.rikos.operacion.catalogo.infraestructura.UnidadMedida;
import pe.rikos.operacion.comun.auditoria.RegistradorAuditoria;
import pe.rikos.operacion.identidad.infraestructura.Empresa;
import pe.rikos.operacion.identidad.infraestructura.Local;
import pe.rikos.operacion.identidad.infraestructura.LocalRepositorio;
import pe.rikos.operacion.identidad.infraestructura.Usuario;
import pe.rikos.operacion.identidad.infraestructura.UsuarioRepositorio;
import pe.rikos.operacion.inventario.aplicacion.dto.DetalleAjusteDTO;
import pe.rikos.operacion.inventario.aplicacion.dto.RegistroAjusteComando;
import pe.rikos.operacion.inventario.infraestructura.Existencia;
import pe.rikos.operacion.inventario.infraestructura.ExistenciaRepositorio;
import pe.rikos.operacion.inventario.infraestructura.MovimientoInventario;
import pe.rikos.operacion.inventario.infraestructura.MovimientoInventarioRepositorio;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RegistrarAjusteInventarioTest {

    @Mock private LocalRepositorio locales;
    @Mock private ProductoRepositorio productos;
    @Mock private MotivoRepositorio motivos;
    @Mock private UsuarioRepositorio usuarios;
    @Mock private ExistenciaRepositorio existencias;
    @Mock private MovimientoInventarioRepositorio movimientos;
    @Mock private RegistradorAuditoria auditoria;

    private RegistrarAjusteInventario casoDeUso;

    private Local local;
    private Usuario usuario;
    private Producto producto;
    private UnidadMedida unidadKg;
    private Motivo motivoMerma;
    private Motivo motivoIngreso;

    @BeforeEach
    void setUp() throws Exception {
        casoDeUso = new RegistrarAjusteInventario(
                locales, productos, motivos, usuarios, existencias, movimientos, auditoria
        );

        Empresa empresa = new Empresa("01M2QZ600G59KP9900YCPTA3EM", "20123456789", "Rikos SAC", "Rikos", "Av Principal 123");
        local = new Local("01M2QZ60D35B90CW00EJA15VFC", empresa, "LOC-01", "San Isidro", "Av Conquistadores 100", "01M2QZ606763Q7XC00M8VD7WM1");
        usuario = new Usuario("01M2QZ606763Q7XC00M8VD7WM1", "admin", "Administrador", "Sistema", null, null, null, "hash", null);

        unidadKg = new UnidadMedida("01M2QZ600G59KP9900YCPTA3U1", "kg", "Kilogramos", "peso");
        CategoriaProducto catInsumo = new CategoriaProducto("01M2QZ600G59KP9900YCPTA3C1", "Verduras", "insumo", null, 1, "01M2QZ606763Q7XC00M8VD7WM1");
        producto = crearProducto("01M2QZ60PRD000000000000001", "AJI-AMARILLO", "Ají Amarillo", catInsumo, unidadKg, new BigDecimal("4.500000"));

        motivoMerma = new Motivo("01M2QZ60MOT000000000000001", "ajuste_inventario", "Merma / Vencimiento", true);
        motivoIngreso = new Motivo("01M2QZ60MOT000000000000002", "ajuste_inventario", "Sobrante de conteo", false);
    }

    @Test
    @DisplayName("Debe registrar ajuste positivo (incremento) exitosamente y actualizar existencia y Kardex")
    void ajustePositivo_incrementoExitoso() {
        RegistroAjusteComando comando = new RegistroAjusteComando(
                local.getId(), producto.getId(), motivoIngreso.getId(),
                "INCREMENTO", new BigDecimal("5.000000"), null, null, usuario.getId(), null
        );

        when(locales.findById(local.getId())).thenReturn(Optional.of(local));
        when(usuarios.findById(usuario.getId())).thenReturn(Optional.of(usuario));
        when(productos.findById(producto.getId())).thenReturn(Optional.of(producto));
        when(motivos.findById(motivoIngreso.getId())).thenReturn(Optional.of(motivoIngreso));

        Existencia ex = new Existencia(local.getId(), producto.getId(), new BigDecimal("10.000000"), BigDecimal.ZERO, null);
        when(existencias.findByLocalIdAndProductoId(local.getId(), producto.getId())).thenReturn(Optional.of(ex));

        when(movimientos.saveAndFlush(any(MovimientoInventario.class))).thenAnswer(i -> i.getArgument(0));

        Resultado<DetalleAjusteDTO, ErrorInventario> resultado = casoDeUso.ejecutar(comando);

        assertThat(resultado.esExito()).isTrue();
        DetalleAjusteDTO dto = resultado.valorOLanza();
        assertThat(dto.signo()).isEqualTo("entrada");
        assertThat(dto.cantidad()).isEqualByComparingTo("5.000000");
        assertThat(dto.stockAnterior()).isEqualByComparingTo("10.000000");
        assertThat(dto.stockNuevo()).isEqualByComparingTo("15.000000");
        assertThat(dto.costoUnitario()).isEqualByComparingTo("4.500000");
        assertThat(dto.costoTotal()).isEqualByComparingTo("22.500000");

        assertThat(ex.getStockActual()).isEqualByComparingTo("15.000000");

        ArgumentCaptor<MovimientoInventario> movCaptor = ArgumentCaptor.forClass(MovimientoInventario.class);
        verify(movimientos).saveAndFlush(movCaptor.capture());
        MovimientoInventario mov = movCaptor.getValue();
        assertThat(mov.getOrigen()).isEqualTo("ajuste");
        assertThat(mov.getSigno()).isEqualTo("entrada");
        assertThat(mov.getCantidad()).isEqualByComparingTo("5.000000");
    }

    @Test
    @DisplayName("Debe registrar ajuste negativo (merma) con observación obligatoria per motivo")
    void ajusteNegativo_disminucionExitosa() {
        RegistroAjusteComando comando = new RegistroAjusteComando(
                local.getId(), producto.getId(), motivoMerma.getId(),
                "DISMINUCION", new BigDecimal("2.000000"), null, "Producto en mal estado", usuario.getId(), null
        );

        when(locales.findById(local.getId())).thenReturn(Optional.of(local));
        when(usuarios.findById(usuario.getId())).thenReturn(Optional.of(usuario));
        when(productos.findById(producto.getId())).thenReturn(Optional.of(producto));
        when(motivos.findById(motivoMerma.getId())).thenReturn(Optional.of(motivoMerma));

        Existencia ex = new Existencia(local.getId(), producto.getId(), new BigDecimal("8.000000"), BigDecimal.ZERO, null);
        when(existencias.findByLocalIdAndProductoId(local.getId(), producto.getId())).thenReturn(Optional.of(ex));

        when(movimientos.saveAndFlush(any(MovimientoInventario.class))).thenAnswer(i -> i.getArgument(0));

        Resultado<DetalleAjusteDTO, ErrorInventario> resultado = casoDeUso.ejecutar(comando);

        assertThat(resultado.esExito()).isTrue();
        DetalleAjusteDTO dto = resultado.valorOLanza();
        assertThat(dto.signo()).isEqualTo("salida");
        assertThat(dto.cantidad()).isEqualByComparingTo("2.000000");
        assertThat(dto.stockAnterior()).isEqualByComparingTo("8.000000");
        assertThat(dto.stockNuevo()).isEqualByComparingTo("6.000000");
        assertThat(dto.observacion()).isEqualTo("Producto en mal estado");

        assertThat(ex.getStockActual()).isEqualByComparingTo("6.000000");
    }

    @Test
    @DisplayName("Debe permitir que ajuste negativo lleve a stock negativo per RN-057 sin error")
    void ajusteNegativo_permiteStockNegativoPerRN057() {
        RegistroAjusteComando comando = new RegistroAjusteComando(
                local.getId(), producto.getId(), motivoMerma.getId(),
                "DISMINUCION", new BigDecimal("5.000000"), null, "Faltante no explicado", usuario.getId(), null
        );

        when(locales.findById(local.getId())).thenReturn(Optional.of(local));
        when(usuarios.findById(usuario.getId())).thenReturn(Optional.of(usuario));
        when(productos.findById(producto.getId())).thenReturn(Optional.of(producto));
        when(motivos.findById(motivoMerma.getId())).thenReturn(Optional.of(motivoMerma));

        // Stock actual es 2.0 kg, ajuste negativo de 5.0 kg -> stock nuevo = -3.0 kg
        Existencia ex = new Existencia(local.getId(), producto.getId(), new BigDecimal("2.000000"), BigDecimal.ZERO, null);
        when(existencias.findByLocalIdAndProductoId(local.getId(), producto.getId())).thenReturn(Optional.of(ex));

        when(movimientos.saveAndFlush(any(MovimientoInventario.class))).thenAnswer(i -> i.getArgument(0));

        Resultado<DetalleAjusteDTO, ErrorInventario> resultado = casoDeUso.ejecutar(comando);

        assertThat(resultado.esExito()).isTrue();
        DetalleAjusteDTO dto = resultado.valorOLanza();
        assertThat(dto.stockNuevo()).isEqualByComparingTo("-3.000000");
        assertThat(ex.getStockActual()).isEqualByComparingTo("-3.000000");
    }

    @Test
    @DisplayName("Debe calcular variación automáticamente en modo REEMPLAZO_STOCK (conteo físico)")
    void ajusteReemplazoStock_calculaDiferenciaAutomaticamente() {
        // Stock anterior es 10 kg, el conteo real dice 7 kg -> salida de 3 kg
        RegistroAjusteComando comando = new RegistroAjusteComando(
                local.getId(), producto.getId(), motivoIngreso.getId(),
                "REEMPLAZO_STOCK", null, new BigDecimal("7.000000"), null, usuario.getId(), null
        );

        when(locales.findById(local.getId())).thenReturn(Optional.of(local));
        when(usuarios.findById(usuario.getId())).thenReturn(Optional.of(usuario));
        when(productos.findById(producto.getId())).thenReturn(Optional.of(producto));
        when(motivos.findById(motivoIngreso.getId())).thenReturn(Optional.of(motivoIngreso));

        Existencia ex = new Existencia(local.getId(), producto.getId(), new BigDecimal("10.000000"), BigDecimal.ZERO, null);
        when(existencias.findByLocalIdAndProductoId(local.getId(), producto.getId())).thenReturn(Optional.of(ex));

        when(movimientos.saveAndFlush(any(MovimientoInventario.class))).thenAnswer(i -> i.getArgument(0));

        Resultado<DetalleAjusteDTO, ErrorInventario> resultado = casoDeUso.ejecutar(comando);

        assertThat(resultado.esExito()).isTrue();
        DetalleAjusteDTO dto = resultado.valorOLanza();
        assertThat(dto.signo()).isEqualTo("salida");
        assertThat(dto.cantidad()).isEqualByComparingTo("3.000000");
        assertThat(dto.stockAnterior()).isEqualByComparingTo("10.000000");
        assertThat(dto.stockNuevo()).isEqualByComparingTo("7.000000");
        assertThat(ex.getStockActual()).isEqualByComparingTo("7.000000");
    }

    @Test
    @DisplayName("Debe fallar si motivo requiere observación y no se proporciona")
    void fallaSiMotivoRequiereObservacionYNoSeEnvia() {
        RegistroAjusteComando comando = new RegistroAjusteComando(
                local.getId(), producto.getId(), motivoMerma.getId(),
                "DISMINUCION", new BigDecimal("1.000000"), null, "   ", usuario.getId(), null
        );

        when(locales.findById(local.getId())).thenReturn(Optional.of(local));
        when(usuarios.findById(usuario.getId())).thenReturn(Optional.of(usuario));
        when(productos.findById(producto.getId())).thenReturn(Optional.of(producto));
        when(motivos.findById(motivoMerma.getId())).thenReturn(Optional.of(motivoMerma));

        Resultado<DetalleAjusteDTO, ErrorInventario> resultado = casoDeUso.ejecutar(comando);

        assertThat(resultado.esFracaso()).isTrue();
        assertThat(((Resultado.Fracaso<DetalleAjusteDTO, ErrorInventario>) resultado).error()).isEqualTo(ErrorInventario.OBSERVACION_REQUERIDA);
    }

    @Test
    @DisplayName("Debe fallar si motivo no es de tipo ajuste_inventario")
    void fallaSiMotivoNoEsDeTipoAjusteInventario() {
        Motivo motivoAnulacion = new Motivo("01M2QZ60MOT000000000000009", "anulacion", "Error de mozo", false);

        RegistroAjusteComando comando = new RegistroAjusteComando(
                local.getId(), producto.getId(), motivoAnulacion.getId(),
                "INCREMENTO", new BigDecimal("1.000000"), null, null, usuario.getId(), null
        );

        when(locales.findById(local.getId())).thenReturn(Optional.of(local));
        when(usuarios.findById(usuario.getId())).thenReturn(Optional.of(usuario));
        when(productos.findById(producto.getId())).thenReturn(Optional.of(producto));
        when(motivos.findById(motivoAnulacion.getId())).thenReturn(Optional.of(motivoAnulacion));

        Resultado<DetalleAjusteDTO, ErrorInventario> resultado = casoDeUso.ejecutar(comando);

        assertThat(resultado.esFracaso()).isTrue();
        assertThat(((Resultado.Fracaso<DetalleAjusteDTO, ErrorInventario>) resultado).error()).isEqualTo(ErrorInventario.MOTIVO_TIPO_INVALIDO);
    }

    private Producto crearProducto(String id, String codigo, String nombre, CategoriaProducto cat, UnidadMedida u, BigDecimal costoCompra) {
        Producto p = new Producto(id, codigo, nombre, cat, u, false, true, false, null, true, "01M2QZ606763Q7XC00M8VD7WM1");
        if (costoCompra != null) {
            p.registrarCostoDeCompra(costoCompra, java.time.LocalDate.of(2026, 10, 1), "01M2QZ606763Q7XC00M8VD7WM1");
        }
        return p;
    }
}
