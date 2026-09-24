package pe.rikos.operacion.identidad.aplicacion;

import java.util.List;

public record SesionIniciada(
        String token,
        String usuarioId,
        String nombreUsuario,
        List<String> roles,
        List<LocalResumen> locales,
        String localActivoId,
        /** Union de los permisos de todos los roles mas las excepciones, excepcion gana sobre rol (3.9). */
        List<String> permisos
) {
}
