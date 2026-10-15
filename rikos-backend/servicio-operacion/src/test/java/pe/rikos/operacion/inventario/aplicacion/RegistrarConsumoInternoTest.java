package pe.rikos.operacion.inventario.aplicacion;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import pe.rikos.contratos.Resultado;
import pe.rikos.operacion.catalogo.infraestructura.*;
import pe.rikos.operacion.comun.auditoria.RegistradorAuditoria;
import pe.rikos.operacion.identidad.aplicacion.ObtenerPermisosUsuario;
import pe.rikos.operacion.identidad.infraestructura.*;
import pe.rikos.operacion.inventario.aplicacion.dto.FilaConsumoInternoDTO;
import pe.rikos.operacion.inventario.aplicacion.dto.RegistroConsumoInternoComando;
import pe.rikos.operacion.inventario.infraestructura.ConsumoInterno;
import pe.rikos.operacion.inventario.infraestructura.ConsumoInternoRepositorio;
import pe.rikos.operacion.inventario.infraestructura.Existencia;
import pe.rikos.operacion.inventario.infraestructura.ExistenciaRepositorio;
import pe.rikos.operacion.inventario.infraestructura.MovimientoInventario;
import pe.rikos.operacion.inventario.infraestructura.MovimientoInventarioRepositorio;

import java.lang.reflect.Constructor;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RegistrarConsumoInternoTest {

    @Mock private ConsumoInternoRepositorio consumos;
    @Mock private LocalRepositorio locales;
    @Mock private UsuarioRepositorio usuarios;
    @Mock private ProductoRepositorio productos;
    @Mock private ExistenciaRepositorio existencias;
    @Mock private MovimientoInventarioRepositorio movimientos;
    @Mock private UsuarioRolRepositorio usuarioRoles;
    @Mock private ObtenerPermisosUsuario obtenerPermisosUsuario;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private RegistradorAuditoria auditoria;

    private RegistrarConsumoInterno casoDeUso;

    private Local local;
    private Usuario mozo;
    private Usuario supervisor;
    private Producto platoSeco;
    private Producto insumoPapas;
    private Existencia existenciaPlato;

    @BeforeEach
    void setUp() throws Exception {
        casoDeUso = new RegistrarConsumoInterno(
                consumos, locales, usuarios, productos, existencias, movimientos,
                usuarioRoles, obtenerPermisosUsuario, passwordEncoder, auditoria
        );

        local = crearLocal("01M2QZ600G59KP9900YCPTA3L1", "San Isidro");
        mozo = crearUsuario("01M2QZ600G59KP9900YCPTA3U1", "Juan", "Perez", "mozo.juan");
        supervisor = crearUsuario("01M2QZ600G59KP9900YCPTA3U2", "Carlos", "Admin", "admin.carlos");

        UnidadMedida unidadPlato = new UnidadMedida("01M2QZ600G59KP9900YCPTA3M1", "plato", "Plato", "unidad");
        UnidadMedida unidadKg = new UnidadMedida("01M2QZ600G59KP9900YCPTA3M2", "kg", "Kilogramo", "masa");

        CategoriaProducto catPlatos = new CategoriaProducto("01M2QZ600G59KP9900YCPTA3C1", "Platos de Fondo", "plato", null, 1, "actor-1");
        CategoriaProducto catInsumo = new CategoriaProducto("01M2QZ600G59KP9900YCPTA3C2", "Tubérculos", "insumo", null, 1, "actor-1");

        platoSeco = crearProducto("01M2QZ600G59KP9900YCPTA3P1", "PLA-001", "Seco de Res con Frijoles", catPlatos, unidadPlato, true, false, false, new BigDecimal("12.50"));
        insumoPapas = crearProducto("01M2QZ600G59KP9900YCPTA3P2", "INS-005", "Papa Amarilla Cruda", catInsumo, unidadKg, false, true, false, new BigDecimal("2.50"));

        existenciaPlato = new Existencia(local.getId(), platoSeco.getId(), null);
        existenciaPlato.actualizarStock(new BigDecimal("15.000000"), null);

        lenient().when(locales.findById(local.getId())).thenReturn(Optional.of(local));
        lenient().when(usuarios.findById(mozo.getId())).thenReturn(Optional.of(mozo));
        lenient().when(productos.findById(platoSeco.getId())).thenReturn(Optional.of(platoSeco));
        lenient().when(productos.findById(insumoPapas.getId())).thenReturn(Optional.of(insumoPapas));
        lenient().when(existencias.findByLocalIdAndProductoId(local.getId(), platoSeco.getId())).thenReturn(Optional.of(existenciaPlato));

        lenient().when(consumos.saveAndFlush(any(ConsumoInterno.class))).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(existencias.saveAndFlush(any(Existencia.class))).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(movimientos.saveAndFlush(any(MovimientoInventario.class))).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(usuarioRoles.findByUsuarioId(any())).thenReturn(List.of());
    }

    @Test
    void primeraComida_sinCobro_descuentaInventarioYRegistraMovimiento() {
        when(consumos.countByLocalIdAndUsuarioIdAndFechaAndServicio(local.getId(), mozo.getId(), LocalDate.now(), "almuerzo"))
                .thenReturn(0L);

        RegistroConsumoInternoComando comando = new RegistroConsumoInternoComando(
                local.getId(),
                mozo.getId(),
                LocalDate.now(),
                "almuerzo",
                "Turno mañana",
                platoSeco.getId(),
                BigDecimal.ONE,
                "Almuerzo reglamentario",
                mozo.getId(),
                null
        );

        Resultado<FilaConsumoInternoDTO, ErrorInventario> res = casoDeUso.ejecutar(comando);

        assertThat(res.esExito()).isTrue();
        FilaConsumoInternoDTO dto = res.valorOLanza();
        assertThat(dto.requirioAutorizacion()).isFalse();
        assertThat(dto.cantidad()).isEqualByComparingTo("1.000000");
        assertThat(dto.costoTotal()).isEqualByComparingTo("12.5000");

        // Existencia se descuenta de 15.0 a 14.0
        assertThat(existenciaPlato.getStockActual()).isEqualByComparingTo("14.000000");

        // Movimiento de salida generado
        ArgumentCaptor<MovimientoInventario> captor = ArgumentCaptor.forClass(MovimientoInventario.class);
        verify(movimientos).saveAndFlush(captor.capture());
        MovimientoInventario mov = captor.getValue();
        assertThat(mov.getSigno()).isEqualTo("salida");
        assertThat(mov.getOrigen()).isEqualTo("consumo_interno");
    }

    @Test
    void segundaComida_sinAutorizacion_fallaConError() {
        // Ya consumió 1 comida en el mismo almuerzo
        when(consumos.countByLocalIdAndUsuarioIdAndFechaAndServicio(local.getId(), mozo.getId(), LocalDate.now(), "almuerzo"))
                .thenReturn(1L);

        RegistroConsumoInternoComando comando = new RegistroConsumoInternoComando(
                local.getId(),
                mozo.getId(),
                LocalDate.now(),
                "almuerzo",
                "Turno mañana",
                platoSeco.getId(),
                BigDecimal.ONE,
                "Segunda ración sin autorización",
                mozo.getId(),
                null // sin PIN de supervisor
        );

        Resultado<FilaConsumoInternoDTO, ErrorInventario> res = casoDeUso.ejecutar(comando);

        assertThat(res.esFracaso()).isTrue();
        assertThat(((Resultado.Fracaso<FilaConsumoInternoDTO, ErrorInventario>) res).error())
                .isEqualTo(ErrorInventario.SEGUNDA_COMIDA_REQUIERE_AUTORIZACION);
    }

    @Test
    void segundaComida_conPinSupervisorValido_autorizaYRegistra() throws Exception {
        when(consumos.countByLocalIdAndUsuarioIdAndFechaAndServicio(local.getId(), mozo.getId(), LocalDate.now(), "almuerzo"))
                .thenReturn(1L);

        inyectarCampo(supervisor, "hashPin", "hash_bcrypt_1234");
        when(usuarios.findAll()).thenReturn(List.of(mozo, supervisor));
        when(passwordEncoder.matches("1234", "hash_bcrypt_1234")).thenReturn(true);

        Rol rolAdmin = new Rol("01M2QZ600G59KP9900YCPTA3R1", "administrador de local", "Admin local", false, "azul");
        UsuarioRol ur = new UsuarioRol(supervisor, rolAdmin, "actor-1");
        lenient().when(usuarioRoles.findByUsuarioId(supervisor.getId())).thenReturn(List.of(ur));

        RegistroConsumoInternoComando comando = new RegistroConsumoInternoComando(
                local.getId(),
                mozo.getId(),
                LocalDate.now(),
                "almuerzo",
                "Turno tarde",
                platoSeco.getId(),
                BigDecimal.ONE,
                "Segunda ración autorizada por turno doble",
                mozo.getId(),
                "1234"
        );

        Resultado<FilaConsumoInternoDTO, ErrorInventario> res = casoDeUso.ejecutar(comando);

        assertThat(res.esExito()).isTrue();
        FilaConsumoInternoDTO dto = res.valorOLanza();
        assertThat(dto.requirioAutorizacion()).isTrue();
        assertThat(dto.autorizadoPorId()).isEqualTo(supervisor.getId());
    }

    @Test
    void productoInsumoCrudo_noPermiteConsumo() {
        RegistroConsumoInternoComando comando = new RegistroConsumoInternoComando(
                local.getId(),
                mozo.getId(),
                LocalDate.now(),
                "almuerzo",
                "Turno mañana",
                insumoPapas.getId(), // insumo crudo
                BigDecimal.ONE,
                "Intento de comer papas crudas",
                mozo.getId(),
                null
        );

        Resultado<FilaConsumoInternoDTO, ErrorInventario> res = casoDeUso.ejecutar(comando);

        assertThat(res.esFracaso()).isTrue();
        assertThat(((Resultado.Fracaso<FilaConsumoInternoDTO, ErrorInventario>) res).error())
                .isEqualTo(ErrorInventario.PRODUCTO_NO_ES_COMIDA);
    }

    private Local crearLocal(String id, String nombre) throws Exception {
        Constructor<Local> c = Local.class.getDeclaredConstructor();
        c.setAccessible(true);
        Local l = c.newInstance();
        inyectarCampo(l, "id", id);
        inyectarCampo(l, "nombre", nombre);
        inyectarCampo(l, "codigo", "LOC-01");
        inyectarCampo(l, "activo", true);
        return l;
    }

    private Usuario crearUsuario(String id, String nombres, String apellidos, String username) throws Exception {
        Constructor<Usuario> c = Usuario.class.getDeclaredConstructor();
        c.setAccessible(true);
        Usuario u = c.newInstance();
        inyectarCampo(u, "id", id);
        inyectarCampo(u, "nombreUsuario", username);
        inyectarCampo(u, "nombres", nombres);
        inyectarCampo(u, "apellidos", apellidos);
        inyectarCampo(u, "activo", true);
        return u;
    }

    private Producto crearProducto(String id, String codigo, String nombre, CategoriaProducto cat,
                                   UnidadMedida um, boolean esVendible, boolean esInsumo, boolean esSubreceta,
                                   BigDecimal costoUltimaCompra) throws Exception {
        Constructor<Producto> c = Producto.class.getDeclaredConstructor();
        c.setAccessible(true);
        Producto p = c.newInstance();
        inyectarCampo(p, "id", id);
        inyectarCampo(p, "codigo", codigo);
        inyectarCampo(p, "nombre", nombre);
        inyectarCampo(p, "categoria", cat);
        inyectarCampo(p, "unidadBase", um);
        inyectarCampo(p, "esVendible", esVendible);
        inyectarCampo(p, "esInsumo", esInsumo);
        inyectarCampo(p, "esSubreceta", esSubreceta);
        inyectarCampo(p, "costoUltimaCompra", costoUltimaCompra);
        inyectarCampo(p, "activo", true);
        return p;
    }

    private void inyectarCampo(Object target, String campo, Object valor) throws Exception {
        Class<?> clazz = target.getClass();
        while (clazz != null) {
            try {
                java.lang.reflect.Field f = clazz.getDeclaredField(campo);
                f.setAccessible(true);
                f.set(target, valor);
                return;
            } catch (NoSuchFieldException e) {
                clazz = clazz.getSuperclass();
            }
        }
    }
}
