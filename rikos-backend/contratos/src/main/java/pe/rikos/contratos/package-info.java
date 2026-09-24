/**
 * Contratos compartidos entre servicio-operacion y servicio-facturacion.
 *
 * Aqui viven unicamente los eventos y DTOs que viajan entre servicios.
 * Regla: Java puro. Nada de Spring, JPA ni anotaciones de framework.
 *
 * Si un tipo solo lo usa un servicio, NO va aqui.
 */
package pe.rikos.contratos;
