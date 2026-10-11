package pe.rikos.operacion.identidad.aplicacion;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.rikos.contratos.Resultado;
import pe.rikos.operacion.identidad.infraestructura.Permiso;
import pe.rikos.operacion.identidad.infraestructura.PermisoRepositorio;
import pe.rikos.operacion.identidad.infraestructura.Rol;
import pe.rikos.operacion.identidad.infraestructura.RolRepositorio;

import java.util.List;
import java.util.Set;

/**
 * Los permisos con los que el sistema entrega un rol de fabrica ("restaurar predeterminados"
 * en la pantalla de edicion). Un rol personalizado no tiene predeterminados: lista vacia.
 * No modifica nada: la pantalla los carga en el formulario y el administrador decide guardar.
 */
@Service
public class ObtenerPermisosPredeterminados {

    private final RolRepositorio roles;
    private final PermisoRepositorio permisos;

    public ObtenerPermisosPredeterminados(RolRepositorio roles, PermisoRepositorio permisos) {
        this.roles = roles;
        this.permisos = permisos;
    }

    @Transactional(readOnly = true)
    public Resultado<List<String>, ErrorRol> ejecutar(String rolId) {
        Rol rol = roles.findById(rolId).orElse(null);
        if (rol == null) {
            return Resultado.fracaso(ErrorRol.NO_ENCONTRADO);
        }
        if (!rol.isEsSistema()) {
            return Resultado.exito(List.of());
        }
        Set<String> codigos = SembradorCatalogoPermisos.codigosPredeterminados(rol.getNombre());
        return Resultado.exito(permisos.findAll().stream()
                .filter(p -> codigos.contains(p.getCodigo()))
                .map(Permiso::getId)
                .toList());
    }
}
