package pe.rikos.operacion.catalogo.aplicacion;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.rikos.contratos.Identificador;
import pe.rikos.contratos.Resultado;
import pe.rikos.operacion.catalogo.infraestructura.Motivo;
import pe.rikos.operacion.catalogo.infraestructura.MotivoRepositorio;

import java.util.Set;

/** US-012: alta de un motivo configurable dentro de uno de los tipos fijos (siete desde US-015). */
@Service
public class CrearMotivo {

    private static final Set<String> TIPOS_VALIDOS = Set.of("ajuste_inventario", "anulacion", "descuento",
            "rechazo_abono", "sobrante_produccion", "reapertura", "disponibilidad", "ajuste_cuenta");

    private final MotivoRepositorio motivos;

    public CrearMotivo(MotivoRepositorio motivos) {
        this.motivos = motivos;
    }

    @Transactional
    public Resultado<Motivo, ErrorMotivo> ejecutar(String tipo, String descripcion, boolean requiereObservacion) {
        if (!TIPOS_VALIDOS.contains(tipo)) {
            return Resultado.fracaso(ErrorMotivo.TIPO_INVALIDO);
        }
        if (motivos.existsByTipoAndDescripcionAndActivoTrue(tipo, descripcion)) {
            return Resultado.fracaso(ErrorMotivo.DESCRIPCION_DUPLICADA);
        }
        Motivo motivo = new Motivo(Identificador.nuevo().valor(), tipo, descripcion, requiereObservacion);
        return Resultado.exito(motivos.save(motivo));
    }
}
