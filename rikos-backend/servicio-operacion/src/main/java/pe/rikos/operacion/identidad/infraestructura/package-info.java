/**
 * P-29 / HT-06: expuesto para GuardaSemillaDesarrollo (modulo comun), que
 * necesita Usuario/UsuarioRepositorio para desactivar el usuario semilla de
 * desarrollo si aparece en un arranque con perfil "nube". Mismo patron que
 * identidad.infraestructura.seguridad, expuesto para ConfiguracionSeguridad.
 */
@org.springframework.modulith.NamedInterface("infraestructura")
package pe.rikos.operacion.identidad.infraestructura;
