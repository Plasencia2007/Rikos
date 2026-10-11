/**
 * US-006: servicio transversal de auditoria. Expuesto a proposito -- todo
 * modulo que registra un evento (identidad, catalogo, y los que sigan)
 * necesita llamar a RegistradorAuditoria.registrar(...), igual que
 * identidad.infraestructura.seguridad expone FiltroAutenticacionJwt.
 */
@org.springframework.modulith.NamedInterface("auditoria")
package pe.rikos.operacion.comun.auditoria;
