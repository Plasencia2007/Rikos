package pe.rikos.operacion.comun.auditoria;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

/**
 * RegistradorAuditoria resuelve el usuario/local activo leyendo
 * SecurityContextHolder (poblado en produccion por FiltroAutenticacionJwt).
 * Los tests que llaman un caso de uso directamente, sin pasar por MockMvc,
 * no tienen ese filtro -- sin esto, cualquier operacion auditada fallaria
 * por violar la columna NOT NULL de auditoria.usuario_id.
 */
public final class ContextoDeSeguridadDePrueba {

    private ContextoDeSeguridadDePrueba() {
    }

    public static void autenticarComo(String usuarioId) {
        autenticarComo(usuarioId, null);
    }

    public static void autenticarComo(String usuarioId, String localActivoId) {
        var autenticacion = new UsernamePasswordAuthenticationToken(usuarioId, null, List.of());
        autenticacion.setDetails(localActivoId);
        SecurityContextHolder.getContext().setAuthentication(autenticacion);
    }

    public static void limpiar() {
        SecurityContextHolder.clearContext();
    }
}
