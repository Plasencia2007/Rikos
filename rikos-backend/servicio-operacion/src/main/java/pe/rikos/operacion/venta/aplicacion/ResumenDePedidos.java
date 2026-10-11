package pe.rikos.operacion.venta.aplicacion;

/** Los conteos del menu lateral y del inicio del POS. porCobrar = pedidos pendientes de cierre. */
public record ResumenDePedidos(long enCurso, long porCobrar) {
}
