package pe.rikos.operacion.inventario.aplicacion;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pe.rikos.operacion.catalogo.aplicacion.CalculadoraCostoReceta;
import pe.rikos.operacion.catalogo.infraestructura.CategoriaProducto;
import pe.rikos.operacion.catalogo.infraestructura.IngredienteReceta;
import pe.rikos.operacion.catalogo.infraestructura.Producto;
import pe.rikos.operacion.catalogo.infraestructura.ProductoRepositorio;
import pe.rikos.operacion.catalogo.infraestructura.Receta;
import pe.rikos.operacion.catalogo.infraestructura.RecetaRepositorio;
import pe.rikos.operacion.catalogo.infraestructura.UnidadMedida;
import pe.rikos.operacion.identidad.infraestructura.Empresa;
import pe.rikos.operacion.identidad.infraestructura.Local;
import pe.rikos.operacion.identidad.infraestructura.LocalRepositorio;
import pe.rikos.operacion.identidad.infraestructura.Usuario;
import pe.rikos.operacion.identidad.infraestructura.UsuarioRepositorio;
import pe.rikos.operacion.inventario.infraestructura.Existencia;
import pe.rikos.operacion.inventario.infraestructura.ExistenciaRepositorio;
import pe.rikos.operacion.inventario.infraestructura.MovimientoInventario;
import pe.rikos.operacion.inventario.infraestructura.MovimientoInventarioRepositorio;
import pe.rikos.operacion.produccion.dominio.Produccion;
import pe.rikos.operacion.produccion.infraestructura.ProduccionRepositorio;
import pe.rikos.operacion.venta.infraestructura.DetallePedido;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DescontarInventarioPorComandaTest {

    @Mock private ProductoRepositorio productos;
    @Mock private RecetaRepositorio recetas;
    @Mock private ExistenciaRepositorio existencias;
    @Mock private MovimientoInventarioRepositorio movimientos;
    @Mock private LocalRepositorio locales;
    @Mock private UsuarioRepositorio usuarios;
    @Mock private ProduccionRepositorio producciones;
    @Mock private CalculadoraCostoReceta calculadoraCostoReceta;

    private DescontarInventarioPorComanda casoDeUso;

    private Local local;
    private Usuario usuario;
    private UnidadMedida unidadKg;
    private UnidadMedida unidadPorcion;

    private CategoriaProducto catPlatos;
    private CategoriaProducto catSubrecetas;
    private CategoriaProducto catInsumos;

    private Producto platoPapaHuancaina;
    private Producto subrecetaHuancaina;
    private Producto insumoPapa;
    private Producto insumoAji;

    private Receta recetaPapaHuancaina;
    private Receta recetaHuancaina;

    @BeforeEach
    void setUp() throws Exception {
        casoDeUso = new DescontarInventarioPorComanda(
                productos, recetas, existencias, movimientos, locales, usuarios, producciones, calculadoraCostoReceta
        );

        Empresa empresa = new Empresa("01EMP000000000000000000001", "20123456789", "Rikos SAC", "Rikos", "Av. Principal 123");
        local = new Local("01M2QZ600G59KP9900YCPTA3L1", empresa, "LI-01", "San Isidro", "Av. Principal", "actor-1");
        usuario = new Usuario("01M2QZ600G59KP9900YCPTA3U1", "mozo1", "Mozo", "Juan", null, null, null, "hash", null);

        unidadKg = new UnidadMedida("01M2QZ600G59KP9900YCPTA3M1", "kg", "Kilogramo", "masa");
        unidadPorcion = new UnidadMedida("01M2QZ600G59KP9900YCPTA3M2", "porc", "Porción", "unidad");

        catPlatos = new CategoriaProducto("01M2QZ600G59KP9900YCPTA3C1", "Platos", "plato", null, 1, "actor-1");
        catSubrecetas = new CategoriaProducto("01M2QZ600G59KP9900YCPTA3C2", "Salsas", "subreceta", null, 1, "actor-1");
        catInsumos = new CategoriaProducto("01M2QZ600G59KP9900YCPTA3C3", "Tubérculos", "insumo", null, 1, "actor-1");

        // Plato final: Papa a la Huancaína
        platoPapaHuancaina = new Producto("01M2QZ600G59KP9900YCPTA3P1", "PL-001", "Papa a la Huancaína",
                catPlatos, unidadPorcion, true, false, false, null, true, "actor-1");

        // Subreceta: Salsa Huancaína (esSubreceta = true)
        subrecetaHuancaina = new Producto("01M2QZ600G59KP9900YCPTA3P2", "SUB-001", "Salsa Huancaína",
                catSubrecetas, unidadKg, false, false, true, null, true, "actor-1");
        subrecetaHuancaina.registrarCostoDeCompra(new BigDecimal("3.00"), LocalDate.now(), "actor-1");

        // Insumo Papa Canchán
        insumoPapa = new Producto("01M2QZ600G59KP9900YCPTA3P3", "INS-001", "Papa Canchán",
                catInsumos, unidadKg, false, true, false, null, true, "actor-1");
        insumoPapa.registrarCostoDeCompra(new BigDecimal("2.50"), LocalDate.now(), "actor-1");

        // Insumo Ají Amarillo (insumo que compone la salsa huancaína)
        insumoAji = new Producto("01M2QZ600G59KP9900YCPTA3P4", "INS-002", "Ají Amarillo",
                catInsumos, unidadKg, false, true, false, null, true, "actor-1");

        // Receta de Papa a la Huancaína (para 10 porciones):
        // Requiere: 1.0 kg de Salsa Huancaína y 2.0 kg de Papa Canchán
        recetaPapaHuancaina = crearReceta("01M2QZ600G59KP9900YCPTA3R1", platoPapaHuancaina, new BigDecimal("10.000000"), 10);
        IngredienteReceta ingHuancaina = crearIngrediente("01M2QZ600G59KP9900YCPTA3I1", recetaPapaHuancaina, subrecetaHuancaina, new BigDecimal("1.000000"), unidadKg, 1);
        IngredienteReceta ingPapa = crearIngrediente("01M2QZ600G59KP9900YCPTA3I2", recetaPapaHuancaina, insumoPapa, new BigDecimal("2.000000"), unidadKg, 2);
        asignarIngredientes(recetaPapaHuancaina, List.of(ingHuancaina, ingPapa));

        // Receta de Salsa Huancaína: para 5 kg requiere 2 kg de ají
        recetaHuancaina = crearReceta("01M2QZ600G59KP9900YCPTA3R2", subrecetaHuancaina, new BigDecimal("5.000000"), 20);
        IngredienteReceta ingAji = crearIngrediente("01M2QZ600G59KP9900YCPTA3I3", recetaHuancaina, insumoAji, new BigDecimal("2.000000"), unidadKg, 1);
        asignarIngredientes(recetaHuancaina, List.of(ingAji));

        when(calculadoraCostoReceta.aUnidadBase(any(), any(), any())).thenAnswer(i -> i.getArgument(1));
    }

    @Test
    @DisplayName("CA2 / Caso Crítico 11: Descuenta la subreceta directamente sin re-descontar sus insumos base")
    void ventaPlatoFinal_descuentaSubrecetaDirectamente_sinDuplicarInsumos() {
        // Pedido de 2 porciones de Papa a la Huancaína
        DetallePedido linea = crearLineaPedido("lin-01", "cta-01", platoPapaHuancaina.getId(), 2);

        when(locales.findById(local.getId())).thenReturn(Optional.of(local));
        when(usuarios.findById(usuario.getId())).thenReturn(Optional.of(usuario));
        when(productos.findById(platoPapaHuancaina.getId())).thenReturn(Optional.of(platoPapaHuancaina));
        when(recetas.findByProductoIdAndActivoTrue(platoPapaHuancaina.getId())).thenReturn(Optional.of(recetaPapaHuancaina));

        // Existencias iniciales:
        // Salsa Huancaína: 5.0 kg
        Existencia exHuancaina = new Existencia(local.getId(), subrecetaHuancaina.getId(), new BigDecimal("5.000000"), BigDecimal.ZERO, null);
        when(existencias.findByLocalIdAndProductoId(local.getId(), subrecetaHuancaina.getId())).thenReturn(Optional.of(exHuancaina));

        // Papa: 10.0 kg
        Existencia exPapa = new Existencia(local.getId(), insumoPapa.getId(), new BigDecimal("10.000000"), BigDecimal.ZERO, null);
        when(existencias.findByLocalIdAndProductoId(local.getId(), insumoPapa.getId())).thenReturn(Optional.of(exPapa));

        when(movimientos.saveAndFlush(any())).thenAnswer(i -> i.getArgument(0));

        // Ejecutar descuento por comanda
        casoDeUso.descontar(local.getId(), "comanda-123", List.of(linea), usuario.getId());

        // Para 2 porciones:
        // Huancaína por porción = 1.0 / 10 = 0.1 kg -> Para 2 porciones = 0.2 kg
        // Stock nuevo Huancaína = 5.0 - 0.2 = 4.8 kg
        assertThat(exHuancaina.getStockActual()).isEqualByComparingTo("4.800000");

        // Papa por porción = 2.0 / 10 = 0.2 kg -> Para 2 porciones = 0.4 kg
        // Stock nuevo Papa = 10.0 - 0.4 = 9.6 kg
        assertThat(exPapa.getStockActual()).isEqualByComparingTo("9.600000");

        // Se deben haber guardado 2 movimientos de inventario: uno para la salsa y otro para la papa
        ArgumentCaptor<MovimientoInventario> captor = ArgumentCaptor.forClass(MovimientoInventario.class);
        verify(movimientos, times(2)).saveAndFlush(captor.capture());

        List<MovimientoInventario> movs = captor.getAllValues();
        assertThat(movs).extracting(MovimientoInventario::getOrigen).containsOnly("venta");
        assertThat(movs).extracting(MovimientoInventario::getSigno).containsOnly("salida");
        assertThat(movs).extracting(MovimientoInventario::getReferenciaId).containsOnly("comanda-123");

        // VERIFICACIÓN CLAVE DE CASO CRÍTICO 11:
        // En ningún momento se consultó la existencia del ají ni se descontó insumoAji
        verify(existencias, never()).findByLocalIdAndProductoId(eq(local.getId()), eq(insumoAji.getId()));
    }

    @Test
    @DisplayName("CA3: Incrementa el contador de porciones vendidas en la producción del día si existe")
    void ventaPlato_incrementaPorcionesVendidasEnProduccion() {
        DetallePedido linea = crearLineaPedido("lin-02", "cta-01", platoPapaHuancaina.getId(), 3);

        when(locales.findById(local.getId())).thenReturn(Optional.of(local));
        when(usuarios.findById(usuario.getId())).thenReturn(Optional.of(usuario));
        when(productos.findById(platoPapaHuancaina.getId())).thenReturn(Optional.of(platoPapaHuancaina));
        when(recetas.findByProductoIdAndActivoTrue(platoPapaHuancaina.getId())).thenReturn(Optional.of(recetaPapaHuancaina));

        Existencia exHuancaina = new Existencia(local.getId(), subrecetaHuancaina.getId(), new BigDecimal("5.000000"), BigDecimal.ZERO, null);
        when(existencias.findByLocalIdAndProductoId(local.getId(), subrecetaHuancaina.getId())).thenReturn(Optional.of(exHuancaina));
        Existencia exPapa = new Existencia(local.getId(), insumoPapa.getId(), new BigDecimal("10.000000"), BigDecimal.ZERO, null);
        when(existencias.findByLocalIdAndProductoId(local.getId(), insumoPapa.getId())).thenReturn(Optional.of(exPapa));

        // Producción existente del día para Papa a la Huancaína con 0 porciones vendidas
        Produccion prodDelDia = new Produccion(
                "prod-hoy-01", local, platoPapaHuancaina, recetaPapaHuancaina, LocalDate.now(),
                "almuerzo", 20, 20, new BigDecimal("20.000000"), usuario
        );
        when(producciones.findFirstByLocalIdAndProductoIdAndFechaOrderByCreadoEnDesc(eq(local.getId()), eq(platoPapaHuancaina.getId()), any(LocalDate.class)))
                .thenReturn(Optional.of(prodDelDia));

        casoDeUso.descontar(local.getId(), "comanda-456", List.of(linea), usuario.getId());

        // Debe haber sumado 3 porciones vendidas
        assertThat(prodDelDia.getPorcionesVendidas()).isEqualTo(3);
        verify(producciones).save(prodDelDia);
    }

    // Helpers
    private DetallePedido crearLineaPedido(String id, String cuentaPersonaId, String productoId, int cantidad) {
        return new DetallePedido(id, cuentaPersonaId, "producto", productoId, "Plato", cantidad, null, 1500L, 0L, "tarifa-1", null, false, "mozo1");
    }

    private Receta crearReceta(String id, Producto prod, BigDecimal rendimiento, int porciones) throws Exception {
        Constructor<Receta> ctor = Receta.class.getDeclaredConstructor();
        ctor.setAccessible(true);
        Receta r = ctor.newInstance();
        setField(r, "id", id);
        setField(r, "producto", prod);
        setField(r, "rendimiento", rendimiento);
        setField(r, "porciones", porciones);
        setField(r, "activo", true);
        return r;
    }

    private IngredienteReceta crearIngrediente(String id, Receta r, Producto prod, BigDecimal cantidad, UnidadMedida u, int orden) throws Exception {
        Constructor<IngredienteReceta> ctor = IngredienteReceta.class.getDeclaredConstructor(String.class, Receta.class, Producto.class, BigDecimal.class, UnidadMedida.class, int.class);
        ctor.setAccessible(true);
        return ctor.newInstance(id, r, prod, cantidad, u, orden);
    }

    private void asignarIngredientes(Receta r, List<IngredienteReceta> ingredientes) throws Exception {
        setField(r, "ingredientes", ingredientes);
    }

    private void setField(Object target, String fieldName, Object val) throws Exception {
        Field f = target.getClass().getDeclaredField(fieldName);
        f.setAccessible(true);
        f.set(target, val);
    }
}
