package pe.rikos.operacion.venta.aplicacion;

import pe.rikos.operacion.venta.infraestructura.Comanda;

import java.util.List;

/**
 * US-030: lo que la vista imprimible muestra. mesa es el numero de la mesa (null si es para llevar); mozo, el
 * usuario que abrio el pedido. Las lineas salen de la foto de la comanda (detalle_comanda), no del pedido de hoy.
 */
public record ComandaDetallada(Comanda comanda, String pedidoNumero, String canal, String mesa, String servicio,
                               String mozo, List<PersonaDeComanda> personas) {
}
