package pe.rikos.operacion.caja.aplicacion;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.rikos.contratos.Identificador;
import pe.rikos.contratos.Resultado;
import pe.rikos.operacion.caja.infraestructura.Caja;
import pe.rikos.operacion.caja.infraestructura.CajaRepositorio;
import pe.rikos.operacion.comun.auditoria.RegistradorAuditoria;

import java.util.List;

/**
 * Registra una caja en el local activo (Configuracion). Lleva un nombre y un codigo unicos en el local; si no se escribe el codigo se
 * asigna el siguiente (CAJ-02, CAJ-03...). El local nace con "Caja Principal" (CAJ-01), que es la principal; las nuevas no lo son.
 */
@Service
public class CrearCaja {

    static final int MAXIMO_CODIGO = 20;

    private final CajaRepositorio cajas;
    private final CajaPorDefecto cajaPorDefecto;
    private final RegistradorAuditoria auditoria;

    public CrearCaja(CajaRepositorio cajas, CajaPorDefecto cajaPorDefecto, RegistradorAuditoria auditoria) {
        this.cajas = cajas;
        this.cajaPorDefecto = cajaPorDefecto;
        this.auditoria = auditoria;
    }

    /** Con el codigo asignado solo. */
    public Resultado<Caja, ErrorCaja> ejecutar(String nombre, String actorId) {
        return ejecutar(nombre, null, actorId);
    }

    @Transactional
    public Resultado<Caja, ErrorCaja> ejecutar(String nombre, String codigo, String actorId) {
        String localId = LocalDeSesion.actual().orElse(null);
        if (localId == null) {
            return Resultado.fracaso(ErrorCaja.SIN_LOCAL_ACTIVO);
        }
        String limpio = nombre == null ? "" : nombre.strip();
        if (limpio.isEmpty() || limpio.length() > 60) {
            return Resultado.fracaso(ErrorCaja.NOMBRE_INVALIDO);
        }
        String codigoLimpio = codigo == null ? "" : codigo.strip().toUpperCase();
        if (codigoLimpio.length() > MAXIMO_CODIGO) {
            return Resultado.fracaso(ErrorCaja.CODIGO_INVALIDO);
        }
        // Se asegura la caja sembrada primero: si no, "Caja Principal" se crearia despues y chocaria con el nombre.
        List<Caja> existentes = cajaPorDefecto.asegurar(localId, actorId);
        if (cajas.existsByLocalIdAndNombreIgnoreCase(localId, limpio)) {
            return Resultado.fracaso(ErrorCaja.NOMBRE_REPETIDO);
        }
        if (codigoLimpio.isEmpty()) {
            codigoLimpio = siguienteCodigo(localId, existentes.size());
        } else if (cajas.existsByLocalIdAndCodigoIgnoreCase(localId, codigoLimpio)) {
            return Resultado.fracaso(ErrorCaja.CODIGO_REPETIDO);
        }
        Caja caja = cajas.save(new Caja(Identificador.nuevo().valor(), localId, limpio, codigoLimpio, actorId));
        auditoria.registrar("caja_creada", "caja", caja.getId(), null,
                RegistradorAuditoria.campos("nombre", limpio, "codigo", codigoLimpio));
        return Resultado.exito(caja);
    }

    /** CAJ-NN con el primer numero libre a partir de la cantidad de cajas que ya hay. */
    private String siguienteCodigo(String localId, int existentes) {
        for (int n = existentes + 1; ; n++) {
            String candidato = "CAJ-" + (n < 10 ? "0" : "") + n;
            if (!cajas.existsByLocalIdAndCodigoIgnoreCase(localId, candidato)) {
                return candidato;
            }
        }
    }
}
