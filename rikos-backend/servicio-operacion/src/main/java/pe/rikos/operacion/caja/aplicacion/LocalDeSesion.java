package pe.rikos.operacion.caja.aplicacion;

import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

/** El local activo de la sesion (DT-047): nunca viaja como dato de la peticion. */
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
