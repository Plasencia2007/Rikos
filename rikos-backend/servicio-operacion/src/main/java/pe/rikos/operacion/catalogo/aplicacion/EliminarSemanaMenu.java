package pe.rikos.operacion.catalogo.aplicacion;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.rikos.contratos.Resultado;
import pe.rikos.operacion.catalogo.infraestructura.DiaMenuRepositorio;
import pe.rikos.operacion.catalogo.infraestructura.OpcionMenuRepositorio;
import pe.rikos.operacion.catalogo.infraestructura.SemanaMenu;
import pe.rikos.operacion.catalogo.infraestructura.SemanaMenuRepositorio;

/**
 * US-013: descartar un borrador con sus dias y opciones. Una semana
 * confirmada no se borra: el POS ya pudo vender con ella.
 *
 * Se borra hijo por hijo aunque V22 tenga ON DELETE CASCADE: si Hibernate
 * tiene los dias cargados en la sesion y solo se borra la semana, al hacer
 * flush encuentra dias que apuntan a una semana borrada y revienta
 * (TransientPropertyValueException, encontrado en la prueba).
 */
@Service
public class EliminarSemanaMenu {

    private final SemanaMenuRepositorio semanas;
    private final DiaMenuRepositorio dias;
    private final OpcionMenuRepositorio opciones;

    public EliminarSemanaMenu(SemanaMenuRepositorio semanas, DiaMenuRepositorio dias,
                              OpcionMenuRepositorio opciones) {
        this.semanas = semanas;
        this.dias = dias;
        this.opciones = opciones;
    }

    @Transactional
    public Resultado<Void, ErrorProgramacion> ejecutar(String semanaId) {
        SemanaMenu semana = semanas.findById(semanaId).orElse(null);
        if (semana == null) {
            return Resultado.fracaso(ErrorProgramacion.NO_ENCONTRADO);
        }
        if (semana.isConfirmada()) {
            return Resultado.fracaso(ErrorProgramacion.SEMANA_CONFIRMADA);
        }
        opciones.deleteAll(opciones.findDeLaSemana(semanaId));
        dias.deleteAll(dias.findBySemanaId(semanaId));
        semanas.delete(semana);
        semanas.flush();
        return Resultado.<Void, ErrorProgramacion>exito(null);
    }
}
