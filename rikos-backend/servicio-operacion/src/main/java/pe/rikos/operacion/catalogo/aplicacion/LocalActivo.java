package pe.rikos.operacion.catalogo.aplicacion;

import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

/**
 * El local activo de la sesion: FiltroAutenticacionJwt lo deja en los
 * "details" de la autenticacion (DT-047), igual que lo lee
 * RegistradorAuditoria. Primer uso fuera de la auditoria (US-015): la
 * disponibilidad es por local y el local nunca viaja como dato de la
 * peticion, asi nadie marca un agotado en un local donde no esta.
 */
final class LocalActivo {

    private LocalActivo() {
    }

    static Optional<String> actual() {
        var autenticacion = SecurityContextHolder.getContext().getAuthentication();
        if (autenticacion == null || autenticacion.getDetails() == null) {
            return Optional.empty();
        }
        String valor = String.valueOf(autenticacion.getDetails());
        return valor.isBlank() ? Optional.empty() : Optional.of(valor);
    }

    static Optional<String> usuario() {
        var autenticacion = SecurityContextHolder.getContext().getAuthentication();
        return autenticacion == null ? Optional.empty() : Optional.of(String.valueOf(autenticacion.getPrincipal()));
    }
}
