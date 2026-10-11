package pe.rikos.operacion.pensionistas.aplicacion;

import java.util.List;

/**
 * La respuesta del motor.
 *
 * @param precio      por unidad, en centimos; null = no se pudo calcular (ver avisos), nunca 0
 * @param taper       por menu, en centimos; null = no aplica o no hay monto (ver avisos)
 * @param avisos      datos que faltan; vacia si todo esta completo
 * @param explicacion por que ese precio, en espanol, para mostrarlo en la caja
 */
public record PrecioCalculado(Long precio, Long taper, List<AvisoPrecio> avisos, List<String> explicacion) {

    /** Precio mas taper por unidad; null si no hay precio. */
    public Long total() {
        return precio == null ? null : precio + (taper == null ? 0L : taper);
    }
}
