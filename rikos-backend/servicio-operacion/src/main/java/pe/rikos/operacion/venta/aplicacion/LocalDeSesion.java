package pe.rikos.operacion.venta.aplicacion;

import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

/**
 * El local activo de la sesion: FiltroAutenticacionJwt lo deja en los
 * "details" de la autenticacion (DT-047). El local nunca viaja como dato de la
 * peticion. (Misma idea que catalogo.LocalActivo y pensionistas.LocalDeSesion.)
 */
final class LocalDeSesion {

    private LocalDeSesion() {
    }

    static Optional<String> actual() {
        var autenticacion = SecurityContextHolder.getContext().getAuthentication();
        if (autenticacion == null || autenticacion.getDetails() == null) {
            return Optional.empty();
        }
        String valor = String.valueOf(autenticacion.getDetails());
        return valor.isBlank() ? Optional.empty() : Optional.of(valor);
    }
}
