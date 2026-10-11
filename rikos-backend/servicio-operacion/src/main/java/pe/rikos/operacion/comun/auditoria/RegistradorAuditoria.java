package pe.rikos.operacion.comun.auditoria;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import pe.rikos.contratos.Identificador;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * US-006: punto unico de escritura de auditoria (13.1: "esta tabla no se
 * modifica ni se borra jamas" -- Auditoria no tiene setters). Antes del
 * sprint 3 esto era codigo a medida para un solo evento (cambio_permiso);
 * ahora es transversal -- cualquier caso de uso que auditar algo llama
 * registrar(...) y no se preocupa por quien esta logueado ni en que local:
 * eso se resuelve aca, leyendo el SecurityContext que FiltroAutenticacionJwt
 * completo al validar el token (principal = usuarioId, details = local
 * activo).
 *
 * "Se escribe en la misma transaccion que el hecho auditado": este
 * servicio NO abre su propia transaccion (a proposito, sin @Transactional
 * propio) -- se ejecuta dentro de la transaccion del caso de uso que lo
 * llama. Si esa transaccion hace rollback, la fila de auditoria tambien
 * desaparece con ella.
 *
 * Serializacion con Jackson real (com.fasterxml.jackson.databind, version
 * 2.x): un intento anterior lo evito por una nota del sprint 1 que decia
 * que Jackson no estaba en el classpath de este modulo -- eso resulto ser
 * falso (revisar `mvn dependency:tree`: jackson-databind llega en scope
 * `compile` via `jjwt-jackson`) y ademas armar JSON a mano es fragil de
 * verdad: un producto llamado Pollo "a la brasa" o con una barra invertida
 * en el nombre corrompia silenciosamente la fila escrita, sin ningun error
 * al guardarla.
 */
@Service
public class RegistradorAuditoria {

    private final AuditoriaRepositorio auditorias;
    private final ObjectMapper mapeador;

    public RegistradorAuditoria(AuditoriaRepositorio auditorias) {
        this.auditorias = auditorias;
        this.mapeador = new ObjectMapper();
    }

    /** Version simple, sin motivo ni observacion -- la mayoria de los eventos de este sprint. */
    public void registrar(String evento, String entidad, String entidadId,
                           Map<String, Object> valorAnterior, Map<String, Object> valorNuevo) {
        registrar(evento, entidad, entidadId, valorAnterior, valorNuevo, null, null);
    }

    public void registrar(String evento, String entidad, String entidadId,
                           Map<String, Object> valorAnterior, Map<String, Object> valorNuevo,
                           String motivoId, String observacion) {
        auditorias.save(new Auditoria(Identificador.nuevo().valor(), localActivoActual(), usuarioActual(), evento,
                entidad, entidadId, aJson(valorAnterior), aJson(valorNuevo), motivoId, observacion));
    }

    /** US-004: unico caso que audita listas de codigos, no un mapa de campos -- se mantiene tal cual estaba. */
    public void registrarCambioPermiso(String usuarioId, String entidad, String entidadId,
                                        Collection<String> valorAnterior, Collection<String> valorNuevo) {
        auditorias.save(new Auditoria(Identificador.nuevo().valor(), null, usuarioId, "cambio_permiso", entidad,
                entidadId, aJson(valorAnterior), aJson(valorNuevo), null, null));
    }

    private String usuarioActual() {
        var autenticacion = SecurityContextHolder.getContext().getAuthentication();
        return autenticacion != null ? String.valueOf(autenticacion.getPrincipal()) : null;
    }

    private String localActivoActual() {
        var autenticacion = SecurityContextHolder.getContext().getAuthentication();
        if (autenticacion == null) return null;
        Object detalles = autenticacion.getDetails();
        return detalles != null ? String.valueOf(detalles) : null;
    }

    private String aJson(Object valor) {
        if (valor == null) return null;
        try {
            return mapeador.writeValueAsString(valor);
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            // Solo puede pasar con tipos que Jackson no sabe serializar -- los
            // llamadores de este servicio solo pasan Map<String,Object> de
            // valores simples y Collection<String> (ver campos(...)), asi que
            // esto es un error de programacion, no un dato de usuario invalido.
            throw new IllegalArgumentException("No se pudo serializar el valor de auditoria a JSON", e);
        }
    }

    /** Helper para armar los mapas de valores sin traer una libreria aparte. */
    public static Map<String, Object> campos(Object... paresClaveValor) {
        Map<String, Object> mapa = new LinkedHashMap<>();
        for (int i = 0; i < paresClaveValor.length; i += 2) {
            mapa.put((String) paresClaveValor[i], paresClaveValor[i + 1]);
        }
        return mapa;
    }

    private static final ObjectMapper LECTOR = new ObjectMapper();

    /**
     * Contraparte de aJson -- para que quien MUESTRE la bitacora (ADM-31)
     * pueda leer un campo simple del JSON guardado. null si la clave no
     * aparece o si el valor guardado no es un objeto JSON (p. ej. el arreglo
     * de codigos de registrarCambioPermiso).
     */
    public static String extraerCampo(String json, String clave) {
        if (json == null) return null;
        try {
            JsonNode nodo = LECTOR.readTree(json).get(clave);
            return (nodo == null || nodo.isNull()) ? null : nodo.asText();
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            return null;
        }
    }
}
