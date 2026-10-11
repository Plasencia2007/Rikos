package pe.rikos.operacion.venta.aplicacion;

import org.springframework.stereotype.Component;
import pe.rikos.contratos.Identificador;
import pe.rikos.operacion.venta.infraestructura.Salon;
import pe.rikos.operacion.venta.infraestructura.SalonRepositorio;

/** El salon "Principal" de un local: donde va una mesa cuando no se elige otro. Se crea la primera vez que hace falta. */
@Component
class SalonPorDefecto {

    static final String NOMBRE = "Principal";

    private final SalonRepositorio salones;

    SalonPorDefecto(SalonRepositorio salones) {
        this.salones = salones;
    }

    Salon deLocal(String localId, String actorId) {
        return salones.findByLocalIdAndNombre(localId, NOMBRE).orElseGet(() -> salones.save(new Salon(
                Identificador.nuevo().valor(), localId, NOMBRE, null, "cubiertos", "verde", Salon.ANCHO_PLANO_POR_DEFECTO,
                Salon.ALTO_PLANO_POR_DEFECTO, Salon.COLOR_PISO_POR_DEFECTO, actorId)));
    }
}
