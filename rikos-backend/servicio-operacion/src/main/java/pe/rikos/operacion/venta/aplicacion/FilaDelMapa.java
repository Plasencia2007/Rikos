package pe.rikos.operacion.venta.aplicacion;

import pe.rikos.operacion.venta.infraestructura.Mesa;

import java.time.OffsetDateTime;

/**
 * Una mesa con su estado derivado, tal como sale en el mapa. abiertaEn: cuando se abrio el pedido en curso de la mesa
 * (para mostrar cuanto lleva); null si no tiene pedido. tomandoPedido: la mesa esta LIBRE pero tiene un pedido abierto
 * sin comandar (alguien esta tomando el pedido): el mapa la marca para que nadie la ofrezca a otro cliente.
 */
public record FilaDelMapa(Mesa mesa, EstadoDeMesa estado, OffsetDateTime abiertaEn, boolean tomandoPedido) {
}
