package pe.rikos.operacion.identidad.aplicacion;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.rikos.contratos.Resultado;
import pe.rikos.operacion.identidad.infraestructura.Permiso;
import pe.rikos.operacion.identidad.infraestructura.PermisoRepositorio;
import pe.rikos.operacion.identidad.infraestructura.Usuario;
import pe.rikos.operacion.identidad.infraestructura.UsuarioPermisoExcepcion;
import pe.rikos.operacion.identidad.infraestructura.UsuarioPermisoExcepcionRepositorio;
import pe.rikos.operacion.identidad.infraestructura.UsuarioRepositorio;

import java.util.List;
import java.util.Map;

/**
 * 3.9, US-005 CA3, RN-013: permisos concedidos o retirados a un usuario
 * concreto, por encima de su rol. Reemplaza el conjunto completo de
 * excepciones del usuario, igual criterio que AsignarPermisosARol.
 */
@Service
public class AsignarExcepcionesPermiso {

    public record Excepcion(String permisoId, boolean otorgado, String motivo) {
    }

    private final UsuarioRepositorio usuarios;
    private final PermisoRepositorio permisos;
    private final UsuarioPermisoExcepcionRepositorio excepciones;
    private final RegistradorAuditoria auditoria;

    public AsignarExcepcionesPermiso(UsuarioRepositorio usuarios, PermisoRepositorio permisos,
                                      UsuarioPermisoExcepcionRepositorio excepciones,
                                      RegistradorAuditoria auditoria) {
        this.usuarios = usuarios;
        this.permisos = permisos;
        this.excepciones = excepciones;
        this.auditoria = auditoria;
    }

    @Transactional
    public Resultado<Void, ErrorUsuario> ejecutar(String usuarioId, List<Excepcion> nuevasExcepciones,
                                                    String actorId) {
        var usuarioOptional = usuarios.findById(usuarioId);
        if (usuarioOptional.isEmpty()) {
            return Resultado.fracaso(ErrorUsuario.NO_ENCONTRADO);
        }
        Usuario usuario = usuarioOptional.get();

        List<String> permisosIds = nuevasExcepciones.stream().map(Excepcion::permisoId).toList();
        List<Permiso> permisosEncontrados = permisos.findByIdIn(permisosIds);
        if (permisosEncontrados.size() != permisosIds.size()) {
            return Resultado.fracaso(ErrorUsuario.PERMISO_INVALIDO);
        }
        Map<String, Permiso> permisoPorId = permisosEncontrados.stream()
                .collect(java.util.stream.Collectors.toMap(Permiso::getId, p -> p));

        List<String> anteriores = excepciones.findByUsuarioId(usuarioId).stream()
                .map(e -> e.getPermiso().getCodigo() + ":" + (e.isOtorgado() ? "otorgado" : "retirado")).toList();

        excepciones.deleteAll(excepciones.findByUsuarioId(usuarioId));
        excepciones.flush();
        for (Excepcion excepcion : nuevasExcepciones) {
            Permiso permiso = permisoPorId.get(excepcion.permisoId());
            excepciones.save(new UsuarioPermisoExcepcion(usuario, permiso, excepcion.otorgado(),
                    excepcion.motivo(), actorId));
        }

        List<String> nuevas = nuevasExcepciones.stream()
                .map(e -> permisoPorId.get(e.permisoId()).getCodigo() + ":" + (e.otorgado() ? "otorgado" : "retirado"))
                .toList();
        auditoria.registrarCambioPermiso(actorId, "usuario_permiso_excepcion", usuarioId, anteriores, nuevas);

        return Resultado.exito(null);
    }
}
