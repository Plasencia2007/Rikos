package pe.rikos.operacion.catalogo.aplicacion;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.rikos.contratos.Resultado;
import pe.rikos.operacion.catalogo.infraestructura.Motivo;
import pe.rikos.operacion.catalogo.infraestructura.MotivoRepositorio;
import pe.rikos.operacion.comun.auditoria.RegistradorAuditoria;

/**
 * US-012: activar/desactivar un motivo. Se audita como "cambio_estado" sobre
 * la entidad "motivo", con "nombre" = descripcion del motivo -- misma
 * convencion que ActivarProducto/DesactivarProducto, para que la bitacora
 * (ADM-31) pueda mostrar "activo/desactivo el motivo "X"" sin ir a buscarlo
 * a otro modulo.
 */
@Service
public class CambiarEstadoMotivo {

    private final MotivoRepositorio motivos;
    private final RegistradorAuditoria auditoria;

    public CambiarEstadoMotivo(MotivoRepositorio motivos, RegistradorAuditoria auditoria) {
        this.motivos = motivos;
        this.auditoria = auditoria;
    }

    @Transactional
    public Resultado<Motivo, ErrorMotivo> ejecutar(String id, boolean activo) {
        Motivo motivo = motivos.findById(id).orElse(null);
        if (motivo == null) {
            return Resultado.fracaso(ErrorMotivo.NO_ENCONTRADO);
        }
        boolean estabaActivo = motivo.isActivo();
        // Sin un motivo activo de disponibilidad no se podria volver a habilitar un producto agotado.
        if (!activo && estabaActivo && "disponibilidad".equals(motivo.getTipo())
                && motivos.countByTipoAndActivoTrue("disponibilidad") <= 1) {
            return Resultado.fracaso(ErrorMotivo.ULTIMO_DE_DISPONIBILIDAD);
        }
        if (activo) {
            motivo.activar();
        } else {
            motivo.desactivar();
        }
        Motivo guardado = motivos.save(motivo);
        auditoria.registrar("cambio_estado", "motivo", id,
                RegistradorAuditoria.campos("activo", estabaActivo, "nombre", motivo.getDescripcion()),
                RegistradorAuditoria.campos("activo", activo, "nombre", motivo.getDescripcion()));
        return Resultado.exito(guardado);
    }
}
