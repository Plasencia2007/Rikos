package pe.rikos.operacion.caja.aplicacion;

import pe.rikos.operacion.caja.infraestructura.CierreCaja;

/** El cierre y si hay alerta: cualquier diferencia desde S/ 0,01, a favor o en contra (RN-070). Nunca bloquea el cierre. */
public record CierreRegistrado(CierreCaja cierre, boolean alerta) {
}
