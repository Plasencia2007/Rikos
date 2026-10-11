package pe.rikos.operacion.caja.aplicacion;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.rikos.contratos.Identificador;
import pe.rikos.contratos.Resultado;
import pe.rikos.operacion.caja.infraestructura.Caja;
import pe.rikos.operacion.caja.infraestructura.CajaRepositorio;
import pe.rikos.operacion.comun.auditoria.RegistradorAuditoria;

/** Agrega una caja al local activo (el local nace con "Caja 1"; un local con 2 o 3 cajas las crea con esto). */
@Service
public class CrearCaja {

    private final CajaRepositorio cajas;
    private final CajaPorDefecto cajaPorDefecto;
    private final RegistradorAuditoria auditoria;

    public CrearCaja(CajaRepositorio cajas, CajaPorDefecto cajaPorDefecto, RegistradorAuditoria auditoria) {
        this.cajas = cajas;
        this.cajaPorDefecto = cajaPorDefecto;
        this.auditoria = auditoria;
    }

    @Transactional
    public Resultado<Caja, ErrorCaja> ejecutar(String nombre, String actorId) {
        String localId = LocalDeSesion.actual().orElse(null);
        if (localId == null) {
            return Resultado.fracaso(ErrorCaja.SIN_LOCAL_ACTIVO);
        }
        String limpio = nombre == null ? "" : nombre.strip();
        if (limpio.isEmpty() || limpio.length() > 60) {
            return Resultado.fracaso(ErrorCaja.NOMBRE_INVALIDO);
        }
        // Se asegura la caja sembrada primero: si no, "Caja 1" se crearia despues y chocaria con el nombre.
        cajaPorDefecto.asegurar(localId, actorId);
        if (cajas.existsByLocalIdAndNombreIgnoreCase(localId, limpio)) {
            return Resultado.fracaso(ErrorCaja.NOMBRE_REPETIDO);
        }
        Caja caja = cajas.save(new Caja(Identificador.nuevo().valor(), localId, limpio, actorId));
        auditoria.registrar("caja_creada", "caja", caja.getId(), null, RegistradorAuditoria.campos("nombre", limpio));
        return Resultado.exito(caja);
    }
}
