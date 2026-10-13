package pe.rikos.operacion.produccion.aplicacion;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pe.rikos.operacion.catalogo.infraestructura.CategoriaProducto;
import pe.rikos.operacion.catalogo.infraestructura.IngredienteReceta;
import pe.rikos.operacion.catalogo.infraestructura.Producto;
import pe.rikos.operacion.catalogo.infraestructura.ProductoRepositorio;
import pe.rikos.operacion.catalogo.infraestructura.Receta;
import pe.rikos.operacion.catalogo.infraestructura.RecetaRepositorio;
import pe.rikos.operacion.catalogo.infraestructura.UnidadMedida;
import pe.rikos.contratos.Resultado;
import pe.rikos.operacion.identidad.infraestructura.Empresa;
import pe.rikos.operacion.identidad.infraestructura.Local;
import pe.rikos.operacion.identidad.infraestructura.LocalRepositorio;
import pe.rikos.operacion.identidad.infraestructura.Usuario;
import pe.rikos.operacion.identidad.infraestructura.UsuarioRepositorio;
import pe.rikos.operacion.inventario.infraestructura.Existencia;
import pe.rikos.operacion.inventario.infraestructura.ExistenciaRepositorio;
import pe.rikos.operacion.inventario.infraestructura.MovimientoInventario;
import pe.rikos.operacion.inventario.infraestructura.MovimientoInventarioRepositorio;
import pe.rikos.operacion.produccion.aplicacion.dto.DetalleProduccionDTO;
import pe.rikos.operacion.produccion.aplicacion.dto.RegistroProduccionComando;
import pe.rikos.operacion.produccion.dominio.ErrorProduccion;
import pe.rikos.operacion.produccion.dominio.Produccion;
import pe.rikos.operacion.produccion.infraestructura.ProduccionRepositorio;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RegistrarProduccionTest {

    @Mock private ProduccionRepositorio producciones;
    @Mock private ProductoRepositorio productos;
    @Mock private RecetaRepositorio recetas;
    @Mock private LocalRepositorio locales;
    @Mock private UsuarioRepositorio usuarios;
    @Mock private ExistenciaRepositorio existencias;
    @Mock private MovimientoInventarioRepositorio movimientos;

    private RegistrarProduccion casoDeUso;

    private Local local;
    private Usuario usuario;
    private UnidadMedida unidadKg;
    private CategoriaProducto catSubreceta;
    private CategoriaProducto catInsumo;
    private Producto subrecetaHuancaina;
    private Producto insumoAji;
    private Producto insumoLeche;
    private Receta recetaHuancaina;

    @BeforeEach
    void setUp() throws Exception {
        casoDeUso = new RegistrarProduccion(
                producciones, productos, recetas, locales, usuarios, existencias, movimientos
        );

        local = crearLocal("01M2QZ600G59KP9900YCPTA3L1", "San Isidro");
        usuario = crearUsuario("01M2QZ600G59KP9900YCPTA3U1", "Cocinero Pepe");
        unidadKg = new UnidadMedida("01M2QZ600G59KP9900YCPTA3M1", "kg", "Kilogramo", "masa");

        catSubreceta = new CategoriaProducto("01M2QZ600G59KP9900YCPTA3C1", "Salsas", "subreceta", null, 1, "actor-1");
        catInsumo = new CategoriaProducto("01M2QZ600G59KP9900YCPTA3C2", "Verduras", "insumo", null, 1, "actor-1");

        subrecetaHuancaina = crearProducto("01M2QZ600G59KP9900YCPTA3P1", "SUB-001", "Salsa Huancaína", catSubreceta, unidadKg, false, false, true, new BigDecimal("8.00"));
        insumoAji = crearProducto("01M2QZ600G59KP9900YCPTA3P2", "INS-001", "Ají Amarillo", catInsumo, unidadKg, false, true, false, new BigDecimal("4.00"));
        insumoLeche = crearProducto("01M2QZ600G59KP9900YCPTA3P3", "INS-002", "Leche Evaporada", catInsumo, unidadKg, false, true, false, new BigDecimal("6.00"));

        // Receta base: rendimiento 5.0 kg para 20 porciones
        recetaHuancaina = crearReceta("01M2QZ600G59KP9900YCPTA3R1", subrecetaHuancaina, new BigDecimal("5.000000"), 20);

        // Ingredientes: 2 kg de ají y 1 kg de leche para la tanda base de 5 kg
        IngredienteReceta ing1 = crearIngrediente("01M2QZ600G59KP9900YCPTA3I1", recetaHuancaina, insumoAji, new BigDecimal("2.000000"), unidadKg, 1);
        IngredienteReceta ing2 = crearIngrediente("01M2QZ600G59KP9900YCPTA3I2", recetaHuancaina, insumoLeche, new BigDecimal("1.000000"), unidadKg, 2);
        asignarIngredientes(recetaHuancaina, List.of(ing1, ing2));
    }

    @Test
    @DisplayName("Debe producir subreceta escalando insumos proporcionalmente al rendimiento obtenido, actualizando stock y Kardex")
    void produccionExitosa_escalaInsumos_eIngresaSubreceta() {
        // Tanda doble: 10 kg en lugar de 5 kg (factor = 2.0)
        RegistroProduccionComando comando = new RegistroProduccionComando(
                local.getId(), subrecetaHuancaina.getId(), recetaHuancaina.getId(),
                LocalDate.of(2026, 10, 13), "almuerzo", 40, 40, new BigDecimal("10.000000"), usuario.getId()
        );

        when(locales.findById(local.getId())).thenReturn(Optional.of(local));
        when(usuarios.findById(usuario.getId())).thenReturn(Optional.of(usuario));
        when(productos.findById(subrecetaHuancaina.getId())).thenReturn(Optional.of(subrecetaHuancaina));
        when(recetas.findById(recetaHuancaina.getId())).thenReturn(Optional.of(recetaHuancaina));

        // Existencias iniciales:
        // Ají: 10 kg
        Existencia exAji = new Existencia(local.getId(), insumoAji.getId(), new BigDecimal("10.000000"), BigDecimal.ZERO, null);
        when(existencias.findByLocalIdAndProductoId(local.getId(), insumoAji.getId())).thenReturn(Optional.of(exAji));

        // Leche: 5 kg
        Existencia exLeche = new Existencia(local.getId(), insumoLeche.getId(), new BigDecimal("5.000000"), BigDecimal.ZERO, null);
        when(existencias.findByLocalIdAndProductoId(local.getId(), insumoLeche.getId())).thenReturn(Optional.of(exLeche));

        // Subreceta inicial: 0 kg
        Existencia exHuancaina = new Existencia(local.getId(), subrecetaHuancaina.getId(), BigDecimal.ZERO, BigDecimal.ZERO, null);
        when(existencias.findByLocalIdAndProductoId(local.getId(), subrecetaHuancaina.getId())).thenReturn(Optional.of(exHuancaina));

        when(movimientos.saveAndFlush(any(MovimientoInventario.class))).thenAnswer(inv -> inv.getArgument(0));
        when(producciones.save(any(Produccion.class))).thenAnswer(inv -> inv.getArgument(0));

        // Ejecutar
        Resultado<DetalleProduccionDTO, ErrorProduccion> resultado = casoDeUso.ejecutar(comando);

        assertThat(resultado.esExito()).isTrue();
        DetalleProduccionDTO dto = resultado.valorOLanza();
        assertThat(dto.productoNombre()).isEqualTo("Salsa Huancaína");
        assertThat(dto.rendimientoObtenido()).isEqualByComparingTo("10.000000");
        assertThat(dto.porcionesPreparadas()).isEqualTo(40);

        // Verificar descuento de insumos:
        // Ají: 10 - (2 * 2.0) = 6 kg
        assertThat(exAji.getStockActual()).isEqualByComparingTo("6.000000");
        // Leche: 5 - (1 * 2.0) = 3 kg
        assertThat(exLeche.getStockActual()).isEqualByComparingTo("3.000000");

        // Verificar entrada de subreceta:
        // Huancaína: 0 + 10 = 10 kg
        assertThat(exHuancaina.getStockActual()).isEqualByComparingTo("10.000000");

        // Verificar movimientos Kardex: 2 salidas y 1 entrada
        ArgumentCaptor<MovimientoInventario> movCaptor = ArgumentCaptor.forClass(MovimientoInventario.class);
        verify(movimientos, times(3)).saveAndFlush(movCaptor.capture());

        List<MovimientoInventario> movs = movCaptor.getAllValues();
        assertThat(movs).extracting(MovimientoInventario::getOrigen).containsOnly("produccion");
        assertThat(movs).extracting(MovimientoInventario::getSigno).containsExactly("salida", "salida", "entrada");

        // Costo consumido: 4 kg aji * 4.00 = 16.00 + 2 kg leche * 6.00 = 12.00 -> Total = 28.00 soles.
        // Costo unitario real por kg: 28.00 / 10 kg = 2.80 soles/kg.
        MovimientoInventario movEntrada = movs.get(2);
        assertThat(movEntrada.getCostoTotal()).isEqualByComparingTo("28.000000");
        assertThat(movEntrada.getCostoUnitario()).isEqualByComparingTo("2.800000");

        // Verificar que producto actualizó su costo de última compra per RN-047 / D5
        verify(productos).save(subrecetaHuancaina);
        assertThat(subrecetaHuancaina.getCostoUltimaCompra()).isEqualByComparingTo("2.800000");
    }

    @Test
    @DisplayName("Debe permitir stock negativo con alerta per RN-057 cuando un insumo no alcanza")
    void produccionConInsumoInsuficiente_permiteStockNegativoSinError() {
        RegistroProduccionComando comando = new RegistroProduccionComando(
                local.getId(), subrecetaHuancaina.getId(), recetaHuancaina.getId(),
                LocalDate.of(2026, 10, 13), "almuerzo", 20, 20, new BigDecimal("5.000000"), usuario.getId()
        );

        when(locales.findById(local.getId())).thenReturn(Optional.of(local));
        when(usuarios.findById(usuario.getId())).thenReturn(Optional.of(usuario));
        when(productos.findById(subrecetaHuancaina.getId())).thenReturn(Optional.of(subrecetaHuancaina));
        when(recetas.findById(recetaHuancaina.getId())).thenReturn(Optional.of(recetaHuancaina));

        // Ají solo tiene 0.5 kg, pero se necesitan 2.0 kg -> Saldo resultante = -1.5 kg
        Existencia exAji = new Existencia(local.getId(), insumoAji.getId(), new BigDecimal("0.500000"), BigDecimal.ZERO, null);
        when(existencias.findByLocalIdAndProductoId(local.getId(), insumoAji.getId())).thenReturn(Optional.of(exAji));

        Existencia exLeche = new Existencia(local.getId(), insumoLeche.getId(), new BigDecimal("1.000000"), BigDecimal.ZERO, null);
        when(existencias.findByLocalIdAndProductoId(local.getId(), insumoLeche.getId())).thenReturn(Optional.of(exLeche));

        Existencia exHuancaina = new Existencia(local.getId(), subrecetaHuancaina.getId(), BigDecimal.ZERO, BigDecimal.ZERO, null);
        when(existencias.findByLocalIdAndProductoId(local.getId(), subrecetaHuancaina.getId())).thenReturn(Optional.of(exHuancaina));

        when(movimientos.saveAndFlush(any(MovimientoInventario.class))).thenAnswer(inv -> inv.getArgument(0));
        when(producciones.save(any(Produccion.class))).thenAnswer(inv -> inv.getArgument(0));

        Resultado<DetalleProduccionDTO, ErrorProduccion> resultado = casoDeUso.ejecutar(comando);

        assertThat(resultado.esExito()).isTrue();
        // El stock pasa a saldo negativo per RN-057 sin bloquear la operación
        assertThat(exAji.getStockActual()).isEqualByComparingTo("-1.500000");
    }

    @Test
    @DisplayName("Debe fallar si el producto no tiene receta activa")
    void fallaSiNoTieneRecetaActiva() {
        RegistroProduccionComando comando = new RegistroProduccionComando(
                local.getId(), subrecetaHuancaina.getId(), null,
                LocalDate.of(2026, 10, 13), "almuerzo", 20, 20, new BigDecimal("5.000000"), usuario.getId()
        );

        when(locales.findById(local.getId())).thenReturn(Optional.of(local));
        when(usuarios.findById(usuario.getId())).thenReturn(Optional.of(usuario));
        when(productos.findById(subrecetaHuancaina.getId())).thenReturn(Optional.of(subrecetaHuancaina));
        when(recetas.findByProductoIdAndActivoTrue(subrecetaHuancaina.getId())).thenReturn(Optional.empty());

        Resultado<DetalleProduccionDTO, ErrorProduccion> resultado = casoDeUso.ejecutar(comando);

        assertThat(resultado.esFracaso()).isTrue();
        assertThat(((Resultado.Fracaso<DetalleProduccionDTO, ErrorProduccion>) resultado).error()).isEqualTo(ErrorProduccion.RECETA_NO_ENCONTRADA);
    }

    // Helpers para instanciar entidades del dominio
    private Local crearLocal(String id, String nombre) {
        Empresa empresa = new Empresa("01EMP000000000000000000001", "20123456789", "Rikos SAC", "Rikos", "Av. Principal 123");
        return new Local(id, empresa, "LI-01", nombre, "Av. Principal", "actor-1");
    }

    private Usuario crearUsuario(String id, String nombres) {
        return new Usuario(id, "pepe", nombres, "Cocinero", null, null, null, "hash", null);
    }

    private Producto crearProducto(String id, String codigo, String nombre, CategoriaProducto cat,
                                   UnidadMedida unidad, boolean vendible, boolean insumo, boolean subreceta,
                                   BigDecimal costo) {
        Producto p = new Producto(id, codigo, nombre, cat, unidad, vendible, insumo, subreceta, null, true, "actor-1");
        if (costo != null) {
            p.registrarCostoDeCompra(costo, LocalDate.of(2026, 10, 1), "actor-1");
        }
        return p;
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
