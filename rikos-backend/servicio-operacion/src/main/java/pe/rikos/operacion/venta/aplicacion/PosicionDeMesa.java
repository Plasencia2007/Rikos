package pe.rikos.operacion.venta.aplicacion;

/** Donde queda una mesa en el plano de su salon (esquina superior izquierda) y cuanto mide, en pixeles. */
public record PosicionDeMesa(String mesaId, int x, int y, int ancho, int alto) {
}
