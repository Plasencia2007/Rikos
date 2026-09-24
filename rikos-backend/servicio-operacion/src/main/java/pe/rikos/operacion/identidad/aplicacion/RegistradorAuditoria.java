package pe.rikos.operacion.identidad.aplicacion;

import org.springframework.stereotype.Service;
import pe.rikos.contratos.Identificador;
import pe.rikos.operacion.identidad.infraestructura.Auditoria;
import pe.rikos.operacion.identidad.infraestructura.AuditoriaRepositorio;

import java.util.Collection;

/**
 * 13.1: "esta tabla no se modifica ni se borra jamas". Este servicio es el
 * unico punto de escritura -- Auditoria no tiene setters, solo constructor.
 *
 * valor_anterior/valor_nuevo se guardan como texto JSON. No se trae
 * Jackson para esto (no esta en el classpath de este modulo, y lo que
 * audita este sprint son listas de codigos de permiso, no estructuras
 * complejas) -- alcanza con un serializador minimo de arreglos de texto.
 */
@Service
public class RegistradorAuditoria {

    private final AuditoriaRepositorio auditorias;

    public RegistradorAuditoria(AuditoriaRepositorio auditorias) {
        this.auditorias = auditorias;
    }

    public void registrarCambioPermiso(String usuarioId, String entidad, String entidadId,
                                        Collection<String> valorAnterior, Collection<String> valorNuevo) {
        auditorias.save(new Auditoria(Identificador.nuevo().valor(), null, usuarioId, "cambio_permiso", entidad,
                entidadId, aJsonArray(valorAnterior), aJsonArray(valorNuevo), null, null));
    }

    private String aJsonArray(Collection<String> valores) {
        if (valores == null) return null;
        StringBuilder json = new StringBuilder("[");
        boolean primero = true;
        for (String valor : valores) {
            if (!primero) json.append(',');
            json.append('"').append(valor.replace("\\", "\\\\").replace("\"", "\\\"")).append('"');
            primero = false;
        }
        return json.append(']').toString();
    }
}
