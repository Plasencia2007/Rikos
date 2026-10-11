package pe.rikos.operacion.comun.web;

/**
 * Forma comun de los cuerpos de error que devuelven los controladores REST
 * cuando un caso de uso falla: {@code {"error": "NOMBRE_DEL_ENUM"}}.
 *
 * Antes de esto, los metodos de los controladores declaraban
 * {@code ResponseEntity<?>} y devolvian este mapa "a mano", lo que hacia
 * invisible la forma real de la respuesta para springdoc/OpenAPI (y por lo
 * tanto para el generador de tipos del frontend). Este record solo se usa
 * como {@code @Schema(implementation = ErrorRespuesta.class)} en las
 * anotaciones de los endpoints -- no reemplaza el {@code Map.of("error", ...)}
 * que sigue devolviendo el codigo real.
 */
public record ErrorRespuesta(String error) {
}
