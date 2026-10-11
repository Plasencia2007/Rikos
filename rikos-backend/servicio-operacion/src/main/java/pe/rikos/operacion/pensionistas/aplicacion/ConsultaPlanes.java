package pe.rikos.operacion.pensionistas.aplicacion;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import pe.rikos.operacion.pensionistas.infraestructura.PlanTarifario;
import pe.rikos.operacion.pensionistas.infraestructura.PlanTarifarioRepositorio;
import pe.rikos.operacion.pensionistas.infraestructura.VersionPlan;
import pe.rikos.operacion.pensionistas.infraestructura.VersionPlanRepositorio;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Lecturas de planes. precio(...) es lo que el motor de precios (entrega 2)
 * y la caja usan: el precio de una comida en una fecha, o vacio si el plan
 * todavia no lo tiene -- nunca cero.
 */
@Component
public class ConsultaPlanes {

    private final PlanTarifarioRepositorio planes;
    private final VersionPlanRepositorio versiones;

    public ConsultaPlanes(PlanTarifarioRepositorio planes, VersionPlanRepositorio versiones) {
        this.planes = planes;
        this.versiones = versiones;
    }

    @Transactional(readOnly = true)
    public List<PlanTarifario> planes() {
        return planes.findAllByOrderByNombreAsc();
    }

    @Transactional(readOnly = true)
    public Optional<PlanTarifario> plan(String planId) {
        return planes.findById(planId);
    }

    /** Historial completo, la version mas nueva primero. */
    @Transactional(readOnly = true)
    public List<VersionPlan> versiones(String planId) {
        return versiones.findByPlanIdOrderByVigenteDesdeDesc(planId);
    }

    @Transactional(readOnly = true)
    public Optional<VersionPlan> versionVigente(String planId, LocalDate fecha) {
        return versiones.findByPlanIdOrderByVigenteDesdeDesc(planId).stream().filter(v -> v.rigeEn(fecha)).findFirst();
    }

    @Transactional(readOnly = true)
    public Optional<Long> precio(String planId, Comida comida, LocalDate fecha) {
        return versionVigente(planId, fecha).map(v -> switch (comida) {
            case DESAYUNO -> v.getPrecioDesayuno();
            case ALMUERZO -> v.getPrecioAlmuerzo();
            case CENA -> v.getPrecioCena();
        });
    }
}
