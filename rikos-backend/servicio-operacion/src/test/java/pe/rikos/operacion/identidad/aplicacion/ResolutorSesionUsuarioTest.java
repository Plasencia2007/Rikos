package pe.rikos.operacion.identidad.aplicacion;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pe.rikos.operacion.identidad.infraestructura.Empresa;
import pe.rikos.operacion.identidad.infraestructura.Local;
import pe.rikos.operacion.identidad.infraestructura.LocalRepositorio;
import pe.rikos.operacion.identidad.infraestructura.Rol;
import pe.rikos.operacion.identidad.infraestructura.Usuario;
import pe.rikos.operacion.identidad.infraestructura.UsuarioLocal;
import pe.rikos.operacion.identidad.infraestructura.UsuarioLocalRepositorio;
import pe.rikos.operacion.identidad.infraestructura.UsuarioRol;
import pe.rikos.operacion.identidad.infraestructura.UsuarioRolRepositorio;
import pe.rikos.operacion.identidad.infraestructura.seguridad.JwtServicio;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * US-002: "si tiene varios locales, se elige el activo al entrar; si tiene
 * uno, se asigna solo".
 */
@ExtendWith(MockitoExtension.class)
class ResolutorSesionUsuarioTest {

    @Mock
    private UsuarioRolRepositorio usuarioRoles;
    @Mock
    private UsuarioLocalRepositorio usuarioLocales;
    @Mock
    private ResolverPermisosEfectivos resolverPermisosEfectivos;
    @Mock
    private JwtServicio jwtServicio;
    @Mock
    private LocalRepositorio localRepositorio;

    private Usuario usuario;
    private Empresa empresa;

    private ResolutorSesionUsuario resolutor() {
        return new ResolutorSesionUsuario(usuarioRoles, usuarioLocales, localRepositorio, resolverPermisosEfectivos,
                jwtServicio);
    }

    private Local nuevoLocal(String id, String codigo, String nombre) {
        return new Local(id, empresa, codigo, nombre, "direccion", "actor");
    }

    private void prepararComun() {
        empresa = new Empresa("empresa-1", "20123456789", "RIKOS SAC", "Riko's", "domicilio");
        usuario = new Usuario("usuario-1", "mozo1", "Nombre", "Apellido", null, null, null, "hash", null);
        when(usuarioRoles.findByUsuarioId("usuario-1"))
                .thenReturn(List.of(new UsuarioRol(usuario, new Rol("rol-1", "mozo", null, true), "actor")));
        when(resolverPermisosEfectivos.resolver("usuario-1")).thenReturn(List.of());
        when(jwtServicio.generar(anyString(), anyString(), anyList(), anyList(), anyList(), any()))
                .thenReturn("token-de-prueba");
    }

    @Test
    void conUnSoloLocalAsignadoSeUsaEseComoActivo() {
        prepararComun();
        Local unico = nuevoLocal("local-1", "sanisidro", "San Isidro");
        when(usuarioLocales.findByUsuarioId("usuario-1"))
                .thenReturn(List.of(new UsuarioLocal(usuario, unico, false, "actor")));

        SesionIniciada sesion = resolutor().construir(usuario, null);

        assertEquals("local-1", sesion.localActivoId());
        assertEquals(1, sesion.locales().size());
    }

    @Test
    void conVariosLocalesSeEligeElPrincipalComoActivo() {
        prepararComun();
        Local principal = nuevoLocal("local-1", "sanisidro", "San Isidro");
        Local otro = nuevoLocal("local-2", "miraflores", "Miraflores");
        when(usuarioLocales.findByUsuarioId("usuario-1")).thenReturn(List.of(
                new UsuarioLocal(usuario, otro, false, "actor"),
                new UsuarioLocal(usuario, principal, true, "actor")));

        SesionIniciada sesion = resolutor().construir(usuario, null);

        assertEquals("local-1", sesion.localActivoId());
        assertEquals(2, sesion.locales().size());
    }

    @Test
    void unLocalSugeridoSeUsaAunHabiendoUnPrincipalDistinto() {
        prepararComun();
        Local principal = nuevoLocal("local-1", "sanisidro", "San Isidro");
        Local otro = nuevoLocal("local-2", "miraflores", "Miraflores");
        when(usuarioLocales.findByUsuarioId("usuario-1")).thenReturn(List.of(
                new UsuarioLocal(usuario, principal, true, "actor"),
                new UsuarioLocal(usuario, otro, false, "actor")));

        SesionIniciada sesion = resolutor().construir(usuario, "local-2");

        assertEquals("local-2", sesion.localActivoId());
    }

    // El administrador central administra todo el negocio (PRD, roles): el
    // selector de local del encabezado le ofrece TODOS los locales activos,
    // no solo los que tiene asignados -- un local recien creado no aparecia
    // ahi y el selector quedaba deshabilitado con una sola opcion.
    @Test
    void elAdministradorCentralVeTodosLosLocalesActivosAunSinTenerlosAsignados() {
        empresa = new Empresa("empresa-1", "20123456789", "RIKOS SAC", "Riko's", "domicilio");
        usuario = new Usuario("usuario-1", "admin", "Nombre", "Apellido", null, null, null, "hash", null);
        when(usuarioRoles.findByUsuarioId("usuario-1")).thenReturn(
                List.of(new UsuarioRol(usuario, new Rol("rol-1", "administrador central", null, true), "actor")));
        when(resolverPermisosEfectivos.resolver("usuario-1")).thenReturn(List.of());
        when(jwtServicio.generar(anyString(), anyString(), anyList(), anyList(), anyList(), any()))
                .thenReturn("token-de-prueba");
        Local asignado = nuevoLocal("local-1", "sanisidro", "San Isidro");
        Local ajeno = nuevoLocal("local-2", "laera", "La Era");
        Local inactivo = nuevoLocal("local-3", "cerrado", "Cerrado");
        inactivo.desactivar("actor");
        when(usuarioLocales.findByUsuarioId("usuario-1"))
                .thenReturn(List.of(new UsuarioLocal(usuario, asignado, true, "actor")));
        when(localRepositorio.findAllByOrderByNombreAsc()).thenReturn(List.of(ajeno, inactivo, asignado));

        SesionIniciada sesion = resolutor().construir(usuario, null);

        assertEquals(List.of("local-2", "local-1"), sesion.locales().stream().map(LocalResumen::id).toList());
        assertEquals("local-1", sesion.localActivoId());
        assertTrue(sesion.locales().stream().filter(l -> l.id().equals("local-1")).findFirst().orElseThrow().esPrincipal());
    }

    @Test
    void ElRestoDeRolesSoloVeLosLocalesQueTieneAsignados() {
        prepararComun();
        Local asignado = nuevoLocal("local-1", "sanisidro", "San Isidro");
        when(usuarioLocales.findByUsuarioId("usuario-1"))
                .thenReturn(List.of(new UsuarioLocal(usuario, asignado, true, "actor")));

        SesionIniciada sesion = resolutor().construir(usuario, null);

        assertEquals(1, sesion.locales().size());
    }
}
