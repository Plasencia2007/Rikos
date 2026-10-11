package pe.rikos.operacion.catalogo.aplicacion;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.rikos.contratos.Resultado;
import pe.rikos.operacion.catalogo.infraestructura.OpcionMenu;
import pe.rikos.operacion.catalogo.infraestructura.OpcionMenuRepositorio;

/**
 * US-013 CA5: saca una opcion del dia (borrado real: nadie la vendio
 * todavia, el dia no esta cerrado). Un dia cerrado no se toca -- ya es
 * historia de lo que se sirvio.
 */
@Service
public class QuitarOpcionMenu {

    private final OpcionMenuRepositorio opciones;
    private final ConsultaMenu consulta;

    public QuitarOpcionMenu(OpcionMenuRepositorio opciones, ConsultaMenu consulta) {
        this.opciones = opciones;
        this.consulta = consulta;
    }

    @Transactional
    public Resultado<Void, ErrorProgramacion> ejecutar(String opcionId) {
        OpcionMenu opcion = opciones.findById(opcionId).orElse(null);
        if (opcion == null) {
            return Resultado.fracaso(ErrorProgramacion.NO_ENCONTRADO);
        }
        if (consulta.estaCerrado(opcion.getDia().getFecha())) {
            return Resultado.fracaso(ErrorProgramacion.DIA_CERRADO);
        }
        opciones.delete(opcion);
        return Resultado.<Void, ErrorProgramacion>exito(null);
    }
}
