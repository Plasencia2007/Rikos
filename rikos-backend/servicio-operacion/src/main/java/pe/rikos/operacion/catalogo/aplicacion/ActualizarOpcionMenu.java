package pe.rikos.operacion.catalogo.aplicacion;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.rikos.contratos.Resultado;
import pe.rikos.operacion.catalogo.infraestructura.OpcionMenu;
import pe.rikos.operacion.catalogo.infraestructura.OpcionMenuRepositorio;

/** US-013 CA2, CA5: cambia porciones estimadas y normal/extra de una opcion, salvo en un dia cerrado. */
@Service
public class ActualizarOpcionMenu {

    private final OpcionMenuRepositorio opciones;
    private final ConsultaMenu consulta;

    public ActualizarOpcionMenu(OpcionMenuRepositorio opciones, ConsultaMenu consulta) {
        this.opciones = opciones;
        this.consulta = consulta;
    }

    @Transactional
    public Resultado<OpcionMenu, ErrorProgramacion> ejecutar(String opcionId, String tipoMenu,
                                                             Integer porcionesEstimadas) {
        OpcionMenu opcion = opciones.findById(opcionId).orElse(null);
        if (opcion == null) {
            return Resultado.fracaso(ErrorProgramacion.NO_ENCONTRADO);
        }
        if (consulta.estaCerrado(opcion.getDia().getFecha())) {
            return Resultado.fracaso(ErrorProgramacion.DIA_CERRADO);
        }
        var validacion = ReglasOpcionMenu.validar(opcion.getComponente(), tipoMenu, porcionesEstimadas);
        if (validacion != null) {
            return Resultado.fracaso(validacion);
        }
        opcion.actualizar(tipoMenu, porcionesEstimadas);
        return Resultado.exito(opciones.save(opcion));
    }
}
