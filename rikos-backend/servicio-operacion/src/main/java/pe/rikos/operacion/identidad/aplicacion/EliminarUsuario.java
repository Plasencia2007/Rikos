package pe.rikos.operacion.identidad.aplicacion;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.support.TransactionTemplate;
import pe.rikos.contratos.Resultado;
import pe.rikos.operacion.comun.auditoria.RegistradorAuditoria;
import pe.rikos.operacion.identidad.infraestructura.Usuario;
import pe.rikos.operacion.identidad.infraestructura.UsuarioRepositorio;

/**
 * Borrado real de un usuario -- distinto de {@link CambiarEstadoUsuario},
 * que es la via normal (US-005, 2.5) para sacarlo de circulacion sin perder
 * la atribucion de lo que ya hizo. Este caso de uso es la excepcion
 * explicita: solo procede si el usuario nunca se uso para nada.
 *
 * A diferencia de EliminarProducto/EliminarLocal (que chequean explicitamente
 * 1-2 tablas conocidas antes de borrar), un usuario puede ser referenciado
 * como creado_por/actualizado_por/asignado_por desde una decena de tablas
 * en todos los modulos del sistema (identidad, catalogo, y los que vengan).
 * Mantener esa lista a mano seria fragil -- una tabla nueva que agregue
 * creado_por rompe la garantia sin que nadie lo note. En su lugar, se
 * intenta el borrado y se captura la violacion de FK real de Postgres
 * (ON DELETE RESTRICT en todas esas columnas), que cubre automaticamente
 * cualquier tabla presente o futura.
 *
 * El borrado corre en su PROPIA transaccion (REQUIRES_NEW, via
 * TransactionTemplate) a proposito: si Postgres rechaza el DELETE por FK,
 * esa transaccion queda abortada y cualquier otro comando en ELLA fallaria
 * tambien -- incluida una lectura posterior del mismo usuario. Aislarla
 * evita que el fallo contamine la transaccion del llamador (un endpoint
 * HTTP normal, o -- el caso que expuso esto -- un test @Transactional que
 * sigue usando la misma conexion despues de intentar borrar).
 *
 * US-006: el registro de auditoria se escribe DENTRO de esa misma
 * transaccion REQUIRES_NEW -- si el DELETE se aborta por FK, la fila de
 * auditoria se aborta con el (no llega a intentarse: el catch corta antes).
 * Si se confirma, ambos se confirman juntos.
 */
@Service
public class EliminarUsuario {

    private final UsuarioRepositorio usuarios;
    private final RegistradorAuditoria auditoria;
    private final TransactionTemplate nuevaTransaccion;

    public EliminarUsuario(UsuarioRepositorio usuarios, RegistradorAuditoria auditoria,
                            PlatformTransactionManager transactionManager) {
        this.usuarios = usuarios;
        this.auditoria = auditoria;
        this.nuevaTransaccion = new TransactionTemplate(transactionManager);
        this.nuevaTransaccion.setPropagationBehavior(Propagation.REQUIRES_NEW.value());
    }

    public Resultado<Void, ErrorUsuario> ejecutar(String id) {
        Usuario usuario = usuarios.findById(id).orElse(null);
        if (usuario == null) {
            return Resultado.fracaso(ErrorUsuario.NO_ENCONTRADO);
        }
        try {
            nuevaTransaccion.executeWithoutResult(estado -> {
                usuarios.deleteById(id);
                usuarios.flush();
                auditoria.registrar("eliminacion", "usuario", id,
                        RegistradorAuditoria.campos("nombreUsuario", usuario.getNombreUsuario(),
                                "nombres", usuario.getNombres(), "apellidos", usuario.getApellidos()),
                        null);
            });
        } catch (DataIntegrityViolationException e) {
            return Resultado.fracaso(ErrorUsuario.TIENE_HISTORIAL);
        }
        return Resultado.<Void, ErrorUsuario>exito(null);
    }
}
