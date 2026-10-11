package pe.rikos.operacion.identidad.aplicacion;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.rikos.contratos.Resultado;
import pe.rikos.operacion.comun.auditoria.RegistradorAuditoria;
import pe.rikos.operacion.identidad.infraestructura.Permiso;
import pe.rikos.operacion.identidad.infraestructura.PermisoRepositorio;
import pe.rikos.operacion.identidad.infraestructura.Rol;
import pe.rikos.operacion.identidad.infraestructura.RolPermiso;
import pe.rikos.operacion.identidad.infraestructura.RolPermisoRepositorio;
import pe.rikos.operacion.identidad.infraestructura.RolRepositorio;

import java.util.List;

/**
 * Reemplaza el conjunto completo de permisos de un rol. 6: cada cambio
 * queda en `auditoria` con evento cambio_permiso, entidad "rol_permiso",
 * el id del rol como entidad_id (la relacion N-N no tiene id propio).
 */
@Service
public class AsignarPermisosARol {

    private final RolRepositorio roles;
    private final PermisoRepositorio permisos;
    private final RolPermisoRepositorio rolPermisos;
    private final RegistradorAuditoria auditoria;

    public AsignarPermisosARol(RolRepositorio roles, PermisoRepositorio permisos, RolPermisoRepositorio rolPermisos,
                                RegistradorAuditoria auditoria) {
        this.roles = roles;
        this.permisos = permisos;
        this.rolPermisos = rolPermisos;
        this.auditoria = auditoria;
    }

    @Transactional
    public Resultado<Void, ErrorRol> ejecutar(String rolId, List<String> permisosIds, String actorId) {
        var rolOptional = roles.findById(rolId);
        if (rolOptional.isEmpty()) {
            return Resultado.fracaso(ErrorRol.NO_ENCONTRADO);
        }
        Rol rol = rolOptional.get();

        List<Permiso> permisosEncontrados = permisos.findByIdIn(permisosIds);
        if (permisosEncontrados.size() != permisosIds.size()) {
            return Resultado.fracaso(ErrorRol.PERMISO_INVALIDO);
        }

        List<String> codigosAnteriores = rolPermisos.findByRolId(rolId).stream()
                .map(rp -> rp.getPermiso().getCodigo()).sorted().toList();

        // Mismo cuidado que GuardarHorarioSemanal (horarios de servicio):
        // Hibernate encola los DELETE derivados despues de los INSERT en el
        // flush normal, sin importar el orden en que se llaman. Sin este
        // flush explicito, re-guardar el mismo permiso choca contra la fila
        // vieja que todavia no se borro.
        rolPermisos.deleteByRolId(rolId);
        rolPermisos.flush();
        for (Permiso permiso : permisosEncontrados) {
            rolPermisos.save(new RolPermiso(rol, permiso, actorId));
        }

        List<String> codigosNuevos = permisosEncontrados.stream().map(Permiso::getCodigo).sorted().toList();
        auditoria.registrarCambioPermiso(actorId, "rol_permiso", rolId, codigosAnteriores, codigosNuevos);

        return Resultado.exito(null);
    }
}
