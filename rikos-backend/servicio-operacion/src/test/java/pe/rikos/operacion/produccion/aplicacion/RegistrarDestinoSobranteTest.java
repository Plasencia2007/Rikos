package pe.rikos.operacion.produccion.aplicacion;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pe.rikos.contratos.Resultado;
import pe.rikos.operacion.catalogo.infraestructura.*;
import pe.rikos.operacion.comun.auditoria.RegistradorAuditoria;
import pe.rikos.operacion.identidad.infraestructura.Local;
import pe.rikos.operacion.identidad.infraestructura.Usuario;
import pe.rikos.operacion.identidad.infraestructura.UsuarioRepositorio;
import pe.rikos.operacion.inventario.infraestructura.Existencia;
import pe.rikos.operacion.inventario.infraestructura.ExistenciaRepositorio;
import pe.rikos.operacion.inventario.infraestructura.MovimientoInventario;
import pe.rikos.operacion.inventario.infraestructura.MovimientoInventarioRepositorio;
import pe.rikos.operacion.produccion.aplicacion.dto.DetalleProduccionDTO;
import pe.rikos.operacion.produccion.aplicacion.dto.LiquidarSobranteComando;
import pe.rikos.operacion.produccion.dominio.ErrorProduccion;
import pe.rikos.operacion.produccion.dominio.Produccion;
import pe.rikos.operacion.produccion.infraestructura.ProduccionRepositorio;

import java.lang.reflect.Constructor;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RegistrarDestinoSobranteTest {

    @Mock private ProduccionRepositorio producciones;
    @Mock private ExistenciaRepositorio existencias;
    @Mock private MovimientoInventarioRepositorio movimientos;
    @Mock private MotivoRepositorio motivos;
    @Mock private UsuarioRepositorio usuarios;
    @Mock private RegistradorAuditoria auditoria;

    private RegistrarDestinoSobrante casoDeUso;

    private Local local;
    private Usuario usuario;
    private Producto subreceta;
    private Receta receta;
    private Produccion produccion;
    private Existencia existencia;

    @BeforeEach
    void setUp() throws Exception {
        casoDeUso = new RegistrarDestinoSobrante(
                producciones, existencias, movimientos, motivos, usuarios, auditoria
        );

        local = crearLocal("01M2QZ600G59KP9900YCPTA3L1", "San Isidro");
        usuario = crearUsuario("01M2QZ600G59KP9900YCPTA3U1", "Chef Mario");
        UnidadMedida unidadKg = new UnidadMedida("01M2QZ600G59KP9900YCPTA3M1", "kg", "Kilogramo", "masa");
        CategoriaProducto cat = new CategoriaProducto("01M2QZ600G59KP9900YCPTA3C1", "Salsas", "subreceta", null, 1, "actor-1");

        subreceta = crearProducto("01M2QZ600G59KP9900YCPTA3P1", "SUB-001", "Salsa Huancaína", cat, unidadKg, false, false, true, new BigDecimal("8.00"));
        receta = crearReceta("01M2QZ600G59KP9900YCPTA3R1", subreceta, new BigDecimal("5.000000"), 20);

        produccion = new Produccion(
                "01M2QZ600G59KP9900YCPTA3PR1",
                local,
                subreceta,
                receta,
                LocalDate.now(),
                "almuerzo",
                20,
                20,
                new BigDecimal("5.000000"),
                usuario
        );

        existencia = new Existencia(local.getId(), subreceta.getId(), null);
        existencia.actualizarStock(new BigDecimal("5.000000"), null);

        lenient().when(producciones.saveAndFlush(any(Produccion.class))).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(existencias.saveAndFlush(any(Existencia.class))).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(movimientos.saveAndFlush(any(MovimientoInventario.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void liquidarSobrante_conservar_noDescuentaInventario() {
        when(producciones.findById(produccion.getId())).thenReturn(Optional.of(produccion));

        LiquidarSobranteComando comando = new LiquidarSobranteComando(
                produccion.getId(),
                local.getId(),
                new BigDecimal("1.500000"),
                "conservar",
                null,
                "Se guardó en cámara de frío",
                usuario.getId()
        );

        Resultado<DetalleProduccionDTO, ErrorProduccion> res = casoDeUso.ejecutar(comando);

        assertThat(res.esExito()).isTrue();
        DetalleProduccionDTO dto = res.valorOLanza();
        assertThat(dto.sobrante()).isEqualByComparingTo("1.500000");
        assertThat(dto.destinoSobrante()).isEqualTo("conservar");

        // No debe generar movimiento de salida ni descontar existencia
        verify(movimientos, never()).saveAndFlush(any(MovimientoInventario.class));
    }

    @Test
    void liquidarSobrante_perdida_descuentaInventarioYRegistraMovimiento() {
        when(producciones.findById(produccion.getId())).thenReturn(Optional.of(produccion));
        when(existencias.findByLocalIdAndProductoId(local.getId(), subreceta.getId()))
                .thenReturn(Optional.of(existencia));

        LiquidarSobranteComando comando = new LiquidarSobranteComando(
                produccion.getId(),
                local.getId(),
                new BigDecimal("1.000000"),
                "perdida",
                null,
                "Se derramó en la mesa de emplatado",
                usuario.getId()
        );

        Resultado<DetalleProduccionDTO, ErrorProduccion> res = casoDeUso.ejecutar(comando);

        assertThat(res.esExito()).isTrue();
        DetalleProduccionDTO dto = res.valorOLanza();
        assertThat(dto.sobrante()).isEqualByComparingTo("1.000000");
        assertThat(dto.destinoSobrante()).isEqualTo("perdida");

        // Existencia se descuenta de 5.0 a 4.0
        assertThat(existencia.getStockActual()).isEqualByComparingTo("4.000000");

        // Movimiento de salida generado
        ArgumentCaptor<MovimientoInventario> captor = ArgumentCaptor.forClass(MovimientoInventario.class);
        verify(movimientos).saveAndFlush(captor.capture());
        MovimientoInventario mov = captor.getValue();
        assertThat(mov.getSigno()).isEqualTo("salida");
        assertThat(mov.getCantidad()).isEqualByComparingTo("1.000000");
        assertThat(mov.getOrigen()).isEqualTo("produccion");
    }

    @Test
    void liquidarSobrante_excedeRendimiento_retornaError() {
        when(producciones.findById(produccion.getId())).thenReturn(Optional.of(produccion));

        LiquidarSobranteComando comando = new LiquidarSobranteComando(
                produccion.getId(),
                local.getId(),
                new BigDecimal("10.000000"), // rendimiento era 5.0
                "perdida",
                null,
                "Error de digitación",
                usuario.getId()
        );

        Resultado<DetalleProduccionDTO, ErrorProduccion> res = casoDeUso.ejecutar(comando);

        assertThat(res.esFracaso()).isTrue();
        assertThat(((Resultado.Fracaso<DetalleProduccionDTO, ErrorProduccion>) res).error())
                .isEqualTo(ErrorProduccion.SOBRANTE_INVALIDO);
    }

    @Test
    void liquidarSobrante_yaRegistradoPreviamente_retornaConflicto() {
        produccion.liquidarSobrante(new BigDecimal("1.0"), "conservar", null);
        when(producciones.findById(produccion.getId())).thenReturn(Optional.of(produccion));

        LiquidarSobranteComando comando = new LiquidarSobranteComando(
                produccion.getId(),
                local.getId(),
                new BigDecimal("1.0"),
                "perdida",
                null,
                "Intento de re-liquidar",
                usuario.getId()
        );

        Resultado<DetalleProduccionDTO, ErrorProduccion> res = casoDeUso.ejecutar(comando);

        assertThat(res.esFracaso()).isTrue();
        assertThat(((Resultado.Fracaso<DetalleProduccionDTO, ErrorProduccion>) res).error())
                .isEqualTo(ErrorProduccion.SOBRANTE_YA_REGISTRADO);
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

    private Usuario crearUsuario(String id, String nombre) throws Exception {
        Constructor<Usuario> c = Usuario.class.getDeclaredConstructor();
        c.setAccessible(true);
        Usuario u = c.newInstance();
        inyectarCampo(u, "id", id);
        inyectarCampo(u, "nombreUsuario", "chef.mario");
        inyectarCampo(u, "nombres", nombre);
        inyectarCampo(u, "apellidos", "Lopez");
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

    private Receta crearReceta(String id, Producto prod, BigDecimal rendimiento, int porciones) throws Exception {
        Constructor<Receta> c = Receta.class.getDeclaredConstructor();
        c.setAccessible(true);
        Receta r = c.newInstance();
        inyectarCampo(r, "id", id);
        inyectarCampo(r, "producto", prod);
        inyectarCampo(r, "rendimiento", rendimiento);
        inyectarCampo(r, "porciones", porciones);
        inyectarCampo(r, "version", 1);
        inyectarCampo(r, "activo", true);
        return r;
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
