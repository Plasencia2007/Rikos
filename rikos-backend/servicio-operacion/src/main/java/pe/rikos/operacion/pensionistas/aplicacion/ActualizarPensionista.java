package pe.rikos.operacion.pensionistas.aplicacion;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.rikos.contratos.Resultado;
import pe.rikos.operacion.comun.auditoria.RegistradorAuditoria;
import pe.rikos.operacion.pensionistas.infraestructura.Pensionista;
import pe.rikos.operacion.pensionistas.infraestructura.PensionistaRepositorio;
import pe.rikos.operacion.pensionistas.infraestructura.PlanTarifarioRepositorio;
import pe.rikos.operacion.pensionistas.infraestructura.Responsable;
import pe.rikos.operacion.pensionistas.infraestructura.ResponsableRepositorio;

import static pe.rikos.operacion.pensionistas.aplicacion.ValidadorDePensionista.limpio;

/**
 * US-016: editar la ficha y el responsable. No toca el PIN ni el estado. Un
 * cambio de plan (RN-004) queda en la bitacora con el plan anterior y el nuevo.
 */
@Service
public class ActualizarPensionista {

    private final PensionistaRepositorio pensionistas;
    private final ResponsableRepositorio responsables;
    private final PlanTarifarioRepositorio planes;
    private final ValidadorDePensionista validador;
    private final RegistradorAuditoria auditoria;

    public ActualizarPensionista(PensionistaRepositorio pensionistas, ResponsableRepositorio responsables,
                                 PlanTarifarioRepositorio planes, ValidadorDePensionista validador,
                                 RegistradorAuditoria auditoria) {
        this.pensionistas = pensionistas;
        this.responsables = responsables;
        this.planes = planes;
        this.validador = validador;
        this.auditoria = auditoria;
    }

    static Pensionista.Ficha fichaDe(DatosPensionista d) {
        return new Pensionista.Ficha(d.dni(), d.nombres().trim(), d.apellidos().trim(), limpio(d.telefono()),
                limpio(d.correo()), limpio(d.institucion()), limpio(d.codigoEstudiante()), limpio(d.carrera()),
                limpio(d.ciclo()), d.planId(), d.comidas(), d.frecuenciaAbono());
    }

    @Transactional
    public Resultado<Pensionista, ErrorPensionista> ejecutar(String id, DatosPensionista datos, String actorId) {
        Pensionista pensionista = pensionistas.findById(id).orElse(null);
        if (pensionista == null) {
            return Resultado.fracaso(ErrorPensionista.NO_ENCONTRADO);
        }
        ErrorPensionista invalido = validador.validar(datos, id);
        if (invalido != null) {
            return Resultado.fracaso(invalido);
        }
        String planAnterior = pensionista.getPlanTarifarioId();
        pensionista.actualizarFicha(fichaDe(datos), actorId);
        pensionistas.save(pensionista);
        DatosResponsable r = datos.responsable();
        Responsable responsable = responsables.findByPensionistaId(id).orElseThrow();
        responsable.actualizar(r.nombreCompleto().trim(), r.dni(), r.telefono().trim(), r.correo().trim(),
                r.parentesco().trim(), r.direccion().trim());
        responsables.save(responsable);
        if (!planAnterior.equals(datos.planId())) {
            String nombre = pensionista.nombreCompleto();
            auditoria.registrar("cambio_plan", "pensionista", id,
                    RegistradorAuditoria.campos("nombre", nombre, "plan", nombreDelPlan(planAnterior)),
                    RegistradorAuditoria.campos("nombre", nombre, "plan", nombreDelPlan(datos.planId())));
        }
        return Resultado.exito(pensionista);
    }

    private String nombreDelPlan(String planId) {
        return planes.findById(planId).map(p -> p.getNombre()).orElse(planId);
    }
}
