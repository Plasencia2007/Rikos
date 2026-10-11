package pe.rikos.operacion.catalogo.aplicacion;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.rikos.contratos.Resultado;
import pe.rikos.operacion.catalogo.infraestructura.Motivo;
import pe.rikos.operacion.catalogo.infraestructura.MotivoRepositorio;

/** US-012: edicion de descripcion/requiere_observacion. El tipo no se puede cambiar. */
@Service
public class ActualizarMotivo {

    private final MotivoRepositorio motivos;

    public ActualizarMotivo(MotivoRepositorio motivos) {
        this.motivos = motivos;
    }

    @Transactional
    public Resultado<Motivo, ErrorMotivo> ejecutar(String id, String descripcion, boolean requiereObservacion) {
        Motivo motivo = motivos.findById(id).orElse(null);
        if (motivo == null) {
            return Resultado.fracaso(ErrorMotivo.NO_ENCONTRADO);
        }
        if (!motivo.getDescripcion().equals(descripcion)
                && motivos.existsByTipoAndDescripcionAndActivoTrue(motivo.getTipo(), descripcion)) {
            return Resultado.fracaso(ErrorMotivo.DESCRIPCION_DUPLICADA);
        }
        motivo.actualizar(descripcion, requiereObservacion);
        return Resultado.exito(motivos.save(motivo));
    }
}
