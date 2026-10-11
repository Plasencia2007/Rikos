package pe.rikos.operacion.identidad.aplicacion;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.rikos.contratos.Resultado;
import pe.rikos.operacion.comun.auditoria.RegistradorAuditoria;
import pe.rikos.operacion.identidad.infraestructura.Rol;
import pe.rikos.operacion.identidad.infraestructura.RolRepositorio;
import pe.rikos.operacion.identidad.infraestructura.UsuarioRolRepositorio;

/**
 * Borrado real de un rol -- solo procede si (a) no es uno de los seis roles
 * de sistema (3.4: no se pueden renombrar ni borrar) y (b) ningun usuario
 * lo tiene asignado todavia (usuario_rol referencia rol con ON DELETE
 * RESTRICT). rol_permiso es CASCADE -- sus permisos configurados se borran
 * solos con el rol, no hace falta chequearlos aparte.
 *
 * US-006: se audita con evento "eliminacion".
 */
@Service
public class EliminarRol {

    private final RolRepositorio roles;
    private final UsuarioRolRepositorio usuarioRoles;
    private final RegistradorAuditoria auditoria;

    public EliminarRol(RolRepositorio roles, UsuarioRolRepositorio usuarioRoles, RegistradorAuditoria auditoria) {
        this.roles = roles;
        this.usuarioRoles = usuarioRoles;
        this.auditoria = auditoria;
    }

    @Transactional
    public Resultado<Void, ErrorRol> ejecutar(String id) {
        Rol rol = roles.findById(id).orElse(null);
        if (rol == null) {
            return Resultado.fracaso(ErrorRol.NO_ENCONTRADO);
        }
        if (rol.isEsSistema()) {
            return Resultado.fracaso(ErrorRol.ES_SISTEMA);
        }
        if (usuarioRoles.existsByRolId(id)) {
            return Resultado.fracaso(ErrorRol.TIENE_HISTORIAL);
        }
        roles.deleteById(id);
        auditoria.registrar("eliminacion", "rol", id,
                RegistradorAuditoria.campos("nombre", rol.getNombre(), "descripcion", rol.getDescripcion()), null);
        return Resultado.<Void, ErrorRol>exito(null);
    }
}
