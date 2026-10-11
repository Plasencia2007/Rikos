package pe.rikos.operacion.venta.aplicacion;

import pe.rikos.operacion.venta.infraestructura.Salon;

/** Un salon con lo que la lista necesita: cuantas mesas tiene, cuantas activas y la capacidad de las activas. */
public record ResumenDeSalon(Salon salon, int mesas, int mesasActivas, int capacidad) {
}
