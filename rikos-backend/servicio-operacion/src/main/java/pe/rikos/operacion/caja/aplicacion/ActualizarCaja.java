package pe.rikos.operacion.caja.aplicacion;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.rikos.contratos.Resultado;
import pe.rikos.operacion.caja.infraestructura.Caja;
import pe.rikos.operacion.caja.infraestructura.CajaRepositorio;
import pe.rikos.operacion.comun.auditoria.RegistradorAuditoria;

/** Cambia el nombre y el codigo de una caja del local activo (Configuracion). Siguen siendo unicos en el local. */
@Service
public class ActualizarCaja {

    private final CajaRepositorio cajas;
    private final RegistradorAuditoria auditoria;

    public ActualizarCaja(CajaRepositorio cajas, RegistradorAuditoria auditoria) {
        this.cajas = cajas;
        this.auditoria = auditoria;
    }

    @Transactional
    public Resultado<Caja, ErrorCaja> ejecutar(String cajaId, String nombre, String codigo, String actorId) {
        String localId = LocalDeSesion.actual().orElse(null);
        if (localId == null) {
            return Resultado.fracaso(ErrorCaja.SIN_LOCAL_ACTIVO);
        }
        Caja caja = cajaId == null ? null : cajas.findByIdAndLocalId(cajaId, localId).orElse(null);
        if (caja == null) {
            return Resultado.fracaso(ErrorCaja.CAJA_NO_ENCONTRADA);
        }
        String limpio = nombre == null ? "" : nombre.strip();
        if (limpio.isEmpty() || limpio.length() > 60) {
            return Resultado.fracaso(ErrorCaja.NOMBRE_INVALIDO);
        }
        String codigoLimpio = codigo == null ? "" : codigo.strip().toUpperCase();
        if (codigoLimpio.isEmpty() || codigoLimpio.length() > CrearCaja.MAXIMO_CODIGO) {
            return Resultado.fracaso(ErrorCaja.CODIGO_INVALIDO);
        }
        if (cajas.existsByLocalIdAndNombreIgnoreCaseAndIdNot(localId, limpio, caja.getId())) {
            return Resultado.fracaso(ErrorCaja.NOMBRE_REPETIDO);
        }
        if (cajas.existsByLocalIdAndCodigoIgnoreCaseAndIdNot(localId, codigoLimpio, caja.getId())) {
            return Resultado.fracaso(ErrorCaja.CODIGO_REPETIDO);
        }
        var anterior = RegistradorAuditoria.campos("nombre", caja.getNombre(), "codigo", caja.getCodigo());
        caja.actualizar(limpio, codigoLimpio);
        cajas.save(caja);
        auditoria.registrar("caja_modificada", "caja", caja.getId(), anterior,
                RegistradorAuditoria.campos("nombre", limpio, "codigo", codigoLimpio));
        return Resultado.exito(caja);
    }
}
