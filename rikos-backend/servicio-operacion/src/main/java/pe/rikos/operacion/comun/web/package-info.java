/**
 * P-22 / HT-06: ErrorRespuesta es el DTO compartido para el patron
 * {"error": "ENUM_NAME"} que devuelven casi todos los controladores del
 * sistema (catalogo, identidad) al documentar sus respuestas de error para
 * OpenAPI (@Schema(implementation = ErrorRespuesta.class)). Expuesto a
 * proposito, igual que comun.auditoria e identidad.infraestructura.seguridad.
 */
@org.springframework.modulith.NamedInterface("web")
package pe.rikos.operacion.comun.web;
