package pe.rikos.operacion.identidad.aplicacion;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.rikos.contratos.Resultado;
import pe.rikos.operacion.identidad.infraestructura.DiaEspecialRepositorio;

/**
 * Borrado real: dia_especial no esta en la lista de "nunca se borra" de 2.5
 * (es una excepcion de calendario, no un dato economico). Sus franjas se
 * van con el por el ON DELETE CASCADE de V11.
 */
@Service
public class EliminarDiaEspecial {

    private final DiaEspecialRepositorio diasEspeciales;

    public EliminarDiaEspecial(DiaEspecialRepositorio diasEspeciales) {
        this.diasEspeciales = diasEspeciales;
    }

    @Transactional
    public Resultado<Void, ErrorHorario> ejecutar(String id) {
        if (diasEspeciales.findById(id).isEmpty()) {
            return Resultado.fracaso(ErrorHorario.NO_ENCONTRADO);
        }
        diasEspeciales.deleteById(id);
        return Resultado.<Void, ErrorHorario>exito(null);
    }
}
