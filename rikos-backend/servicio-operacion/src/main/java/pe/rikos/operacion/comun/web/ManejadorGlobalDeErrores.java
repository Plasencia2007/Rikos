package pe.rikos.operacion.comun.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;
import java.util.UUID;

/**
 * Antes de esto, un error interno inesperado (una excepcion que ningun
 * caso de uso preveia, no uno de los `Resultado.fracaso` normales) no
 * tenia ningun manejador propio -- caia en la pagina de error generica
 * de Spring Boot. No se confundia con un 403 (Spring Security solo
 * traduce AccessDeniedException/AuthenticationException, nunca un
 * RuntimeException cualquiera), pero tampoco quedaba registrado de forma
 * util: el cliente veia un 500 sin cuerpo util para el diagnostico, y en
 * el log no habia una referencia facil de correlacionar con ese pedido.
 *
 * AccessDeniedException/AuthenticationException se dejan pasar tal cual
 * -- las sigue manejando Spring Security, no esta clase -- para no pisar
 * el 401/403 real con un 500 generico.
 */
@RestControllerAdvice
public class ManejadorGlobalDeErrores {

    private static final Logger log = LoggerFactory.getLogger(ManejadorGlobalDeErrores.class);

    @ExceptionHandler({AccessDeniedException.class, AuthenticationException.class})
    public void dejarPasarErroresDeSeguridad(Exception excepcion) throws Exception {
        throw excepcion;
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> manejarErrorInesperado(Exception excepcion) {
        String referencia = UUID.randomUUID().toString().substring(0, 8);
        log.error("Error interno no previsto [{}]", referencia, excepcion);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "ERROR_INTERNO", "referencia", referencia));
    }
}
