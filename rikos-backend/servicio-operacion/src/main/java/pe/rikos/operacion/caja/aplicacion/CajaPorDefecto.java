package pe.rikos.operacion.caja.aplicacion;

import org.springframework.stereotype.Component;
import pe.rikos.contratos.Identificador;
import pe.rikos.operacion.caja.infraestructura.Caja;
import pe.rikos.operacion.caja.infraestructura.CajaRepositorio;

import java.util.List;

/** "Caja 1": la caja sembrada de un local. Se crea la primera vez que un local pide sus cajas y no tiene ninguna. */
@Component
class CajaPorDefecto {

    static final String NOMBRE = "Caja 1";

    private final CajaRepositorio cajas;

    CajaPorDefecto(CajaRepositorio cajas) {
        this.cajas = cajas;
    }

    List<Caja> asegurar(String localId, String actorId) {
        List<Caja> existentes = cajas.findByLocalIdOrderByNombre(localId);
        if (!existentes.isEmpty()) {
            return existentes;
        }
        return List.of(cajas.save(new Caja(Identificador.nuevo().valor(), localId, NOMBRE, actorId)));
    }
}
