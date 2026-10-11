package pe.rikos.operacion.venta.aplicacion;

/**
 * Lo que se pide para crear o editar una mesa. nombre y capacidad son opcionales. salonId, forma y medidas
 * tambien: sin salon va al "Principal" del local, sin forma es cuadrada y sin medidas mide 84 x 84
 * (150 x 78 si es rectangular).
 */
public record DatosMesa(String numero, String nombre, Integer capacidad, String salonId, String forma, boolean vip,
                        boolean zonaExterior, boolean zonaFumadores, Integer ancho, Integer alto) {

    /** Numero, nombre y capacidad: como se creaban las mesas antes de los salones. */
    public DatosMesa(String numero, String nombre, Integer capacidad) {
        this(numero, nombre, capacidad, null, null, false, false, false, null, null);
    }
}
