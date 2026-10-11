/**
 * Expuesto fuera del modulo identidad a proposito: FiltroAutenticacionJwt
 * y JwtServicio son piezas de seguridad transversal que ConfiguracionSeguridad
 * (modulo comun) necesita conectar al filtro HTTP de toda la API.
 */
@org.springframework.modulith.NamedInterface("seguridad")
package pe.rikos.operacion.identidad.infraestructura.seguridad;
