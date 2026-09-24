package pe.rikos.operacion.identidad.aplicacion;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pe.rikos.operacion.identidad.infraestructura.Permiso;
import pe.rikos.operacion.identidad.infraestructura.RolPermiso;
import pe.rikos.operacion.identidad.infraestructura.RolPermisoRepositorio;
import pe.rikos.operacion.identidad.infraestructura.Usuario;
import pe.rikos.operacion.identidad.infraestructura.UsuarioPermisoExcepcion;
import pe.rikos.operacion.identidad.infraestructura.UsuarioPermisoExcepcionRepositorio;
import pe.rikos.operacion.identidad.infraestructura.UsuarioRol;
import pe.rikos.operacion.identidad.infraestructura.UsuarioRolRepositorio;
import pe.rikos.operacion.identidad.infraestructura.Rol;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * 3.9, US-005 CA3, RN-013: "el permiso efectivo es la union de los
 * permisos de TODOS los roles del usuario, MAS las excepciones, y la
 * excepcion gana sobre el rol".
 */
@ExtendWith(MockitoExtension.class)
class ResolverPermisosEfectivosTest {

    private static final String USUARIO_ID = "usuario-1";

    @Mock
    private UsuarioRolRepositorio usuarioRoles;
    @Mock
    private RolPermisoRepositorio rolPermisos;
    @Mock
    private UsuarioPermisoExcepcionRepositorio excepciones;

    private ResolverPermisosEfectivos resolver;
    private Usuario usuario;
    private Rol rolCajero;
    private Permiso permisoVerVenta;
    private Permiso permisoAnularVenta;
    private Permiso permisoVerCostos;

    @BeforeEach
    void preparar() {
        resolver = new ResolverPermisosEfectivos(usuarioRoles, rolPermisos, excepciones);
        usuario = new Usuario(USUARIO_ID, "cajero1", "Nombre", "Apellido", null, null, null, "hash", null);
        rolCajero = new Rol("rol-cajero", "cajero", null, true);
        permisoVerVenta = new Permiso("p-ver-venta", "venta", "ver", "venta.ver", null);
        permisoAnularVenta = new Permiso("p-anular-venta", "venta", "anular", "venta.anular", null);
        permisoVerCostos = new Permiso("p-ver-costos", "recetas", "ver_costos", "recetas.ver_costos", null);
    }

    @Test
    void elEfectivoEsSoloLoDelRolCuandoNoHayExcepciones() {
        when(usuarioRoles.findByUsuarioId(USUARIO_ID))
                .thenReturn(List.of(new UsuarioRol(usuario, rolCajero, "actor")));
        when(rolPermisos.findByRolIdIn(List.of("rol-cajero")))
                .thenReturn(List.of(new RolPermiso(rolCajero, permisoVerVenta, "actor")));
        when(excepciones.findByUsuarioId(USUARIO_ID)).thenReturn(List.of());

        List<String> efectivos = resolver.resolver(USUARIO_ID);

        assertEquals(List.of("venta.ver"), efectivos);
    }

    @Test
    void unaExcepcionOtorgadaSumaUnPermisoQueElRolNoDa() {
        when(usuarioRoles.findByUsuarioId(USUARIO_ID))
                .thenReturn(List.of(new UsuarioRol(usuario, rolCajero, "actor")));
        when(rolPermisos.findByRolIdIn(List.of("rol-cajero")))
                .thenReturn(List.of(new RolPermiso(rolCajero, permisoVerVenta, "actor")));
        when(excepciones.findByUsuarioId(USUARIO_ID)).thenReturn(List.of(
                new UsuarioPermisoExcepcion(usuario, permisoVerCostos, true, "cubre licencia del gerente", "actor")));

        List<String> efectivos = resolver.resolver(USUARIO_ID);

        assertTrue(efectivos.contains("venta.ver"), "conserva lo que ya daba el rol");
        assertTrue(efectivos.contains("recetas.ver_costos"), "suma lo que otorga la excepcion");
        assertEquals(2, efectivos.size());
    }

    @Test
    void unaExcepcionRetiradaQuitaUnPermisoQueElRolSiDaba_laExcepcionGanaSobreElRol() {
        when(usuarioRoles.findByUsuarioId(USUARIO_ID))
                .thenReturn(List.of(new UsuarioRol(usuario, rolCajero, "actor")));
        when(rolPermisos.findByRolIdIn(List.of("rol-cajero"))).thenReturn(List.of(
                new RolPermiso(rolCajero, permisoVerVenta, "actor"),
                new RolPermiso(rolCajero, permisoAnularVenta, "actor")));
        // El rol cajero SI da venta.anular, pero a este cajero en particular
        // se le retiro por una excepcion puntual (ej. esta en capacitacion).
        when(excepciones.findByUsuarioId(USUARIO_ID)).thenReturn(List.of(
                new UsuarioPermisoExcepcion(usuario, permisoAnularVenta, false, "en capacitacion", "actor")));

        List<String> efectivos = resolver.resolver(USUARIO_ID);

        assertTrue(efectivos.contains("venta.ver"));
        assertFalse(efectivos.contains("venta.anular"),
                "la excepcion (otorgado=false) debe ganarle al rol, que si lo daba");
    }
}
