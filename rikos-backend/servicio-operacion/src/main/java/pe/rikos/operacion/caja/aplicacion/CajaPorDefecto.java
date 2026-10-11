package pe.rikos.operacion.caja.aplicacion;

import org.springframework.stereotype.Component;
import pe.rikos.contratos.Identificador;
import pe.rikos.operacion.caja.infraestructura.Caja;
import pe.rikos.operacion.caja.infraestructura.CajaRepositorio;

import java.util.List;

/** "Caja Principal" (CAJ-01): la caja sembrada de un local, que es la principal. Se crea la primera vez que un local pide sus cajas y no tiene ninguna. */
@Component
class CajaPorDefecto {

    static final String NOMBRE = "Caja Principal";
    static final String CODIGO = "CAJ-01";

    private final CajaRepositorio cajas;

    CajaPorDefecto(CajaRepositorio cajas) {
        this.cajas = cajas;
    }

    List<Caja> asegurar(String localId, String actorId) {
        List<Caja> existentes = cajas.findByLocalIdOrderByPrincipalDescCodigoAsc(localId);
        if (!existentes.isEmpty()) {
            return existentes;
        }
        // Inserccion que tolera la carrera de dos primeras consultas (auditoria CAJ-05): si la otra gano, esta no hace nada.
        cajas.insertarSiNoExiste(Identificador.nuevo().valor(), localId, NOMBRE, CODIGO, actorId);
        return cajas.findByLocalIdOrderByPrincipalDescCodigoAsc(localId);
    }
}
