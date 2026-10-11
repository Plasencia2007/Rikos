package pe.rikos.operacion.identidad.aplicacion;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Transactional;
import pe.rikos.contratos.Resultado;
import pe.rikos.operacion.identidad.infraestructura.Usuario;
import pe.rikos.operacion.identidad.infraestructura.UsuarioLocalRepositorio;
import pe.rikos.operacion.identidad.infraestructura.UsuarioRepositorio;

/**
 * Hallazgo de revision (US-006): el sidebar deja "cambiar de local" sin
 * volver a pedir token -- solo actualiza estado local de React. El JWT (y
 * por lo tanto RegistradorAuditoria.localActivoActual(), que lee el claim
 * "localActivo" del token) queda con el local del login hasta el proximo
 * cierre de sesion. Cualquier accion auditada despues de cambiar de local
 * se registraba contra el local viejo.
 *
 * Esto reemite el token con el nuevo local activo -- la unica forma real
 * de que el claim quede al dia sin mover "local activo" a cada peticion
 * individual (la alternativa que se descarto: tocaria todos los casos de
 * uso que hoy reciben el actor solo, no el local).
 */
@Service
@Transactional(readOnly = true)
public class CambiarLocalActivo {

    private final UsuarioRepositorio usuarios;
    private final UsuarioLocalRepositorio usuarioLocales;
    private final ResolutorSesionUsuario resolutorSesion;

    public CambiarLocalActivo(UsuarioRepositorio usuarios, UsuarioLocalRepositorio usuarioLocales,
                               ResolutorSesionUsuario resolutorSesion) {
        this.usuarios = usuarios;
        this.usuarioLocales = usuarioLocales;
        this.resolutorSesion = resolutorSesion;
    }

    // Transaccion obligatoria: construir la sesion lee los roles del usuario
    // (relacion perezosa) y open-in-view esta apagado (DT-049). Sin esto,
    // cambiar de local daba 500 -- encontrado en vivo en el sprint 4, ver
    // CambiarLocalActivoSinTransaccionTest.
    @Transactional(readOnly = true)
    public Resultado<SesionIniciada, FalloAutenticacion> ejecutar(String usuarioId, String localId) {
        Usuario usuario = usuarios.findById(usuarioId).orElse(null);
        if (usuario == null || !usuario.isActivo()) {
            return Resultado.fracaso(FalloAutenticacion.de(ErrorAutenticacion.USUARIO_INACTIVO));
        }
        // Asignado al local, o administrador central con el local activo.
        boolean asignado = usuarioLocales.existsByUsuarioIdAndLocalId(usuarioId, localId);
        if (!resolutorSesion.puedeOperarEn(usuario, localId, asignado)) {
            return Resultado.fracaso(FalloAutenticacion.de(ErrorAutenticacion.LOCAL_NO_ASIGNADO));
        }
        return Resultado.exito(resolutorSesion.construir(usuario, localId));
    }
}
