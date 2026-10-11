package pe.rikos.operacion.pensionistas.aplicacion;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.rikos.contratos.Identificador;
import pe.rikos.contratos.Resultado;
import pe.rikos.operacion.comun.auditoria.RegistradorAuditoria;
import pe.rikos.operacion.pensionistas.infraestructura.PlanTarifario;
import pe.rikos.operacion.pensionistas.infraestructura.PlanTarifarioRepositorio;
import pe.rikos.operacion.pensionistas.infraestructura.VersionPlan;
import pe.rikos.operacion.pensionistas.infraestructura.VersionPlanRepositorio;

import java.time.Clock;
import java.time.LocalDate;

/** US-017 CA1: un plan nuevo con sus tres precios, vigente desde hoy (o desde una fecha futura). */
@Service
public class CrearPlanTarifario {

    private final PlanTarifarioRepositorio planes;
    private final VersionPlanRepositorio versiones;
    private final RegistradorAuditoria auditoria;
    private final Clock reloj;

    public CrearPlanTarifario(PlanTarifarioRepositorio planes, VersionPlanRepositorio versiones,
                              RegistradorAuditoria auditoria, Clock reloj) {
        this.planes = planes;
        this.versiones = versiones;
        this.auditoria = auditoria;
        this.reloj = reloj;
    }

    @Transactional
    public Resultado<PlanTarifario, ErrorPlan> ejecutar(String nombre, String descripcion, PreciosPlan precios,
                                                        LocalDate vigenteDesde, String actorId) {
        String limpio = nombre == null ? "" : nombre.trim();
        if (limpio.isEmpty()) {
            return Resultado.fracaso(ErrorPlan.NOMBRE_OBLIGATORIO);
        }
        ErrorPlan invalido = precios.validar();
        if (invalido != null) {
            return Resultado.fracaso(invalido);
        }
        LocalDate hoy = LocalDate.now(reloj);
        LocalDate desde = vigenteDesde == null ? hoy : vigenteDesde;
        if (desde.isBefore(hoy)) {
            return Resultado.fracaso(ErrorPlan.VIGENCIA_INVALIDA);
        }
        if (planes.existsByNombre(limpio)) {
            return Resultado.fracaso(ErrorPlan.NOMBRE_DUPLICADO);
        }
        String descripcionLimpia = descripcion == null || descripcion.isBlank() ? null : descripcion.trim();
        PlanTarifario plan = planes.save(new PlanTarifario(Identificador.nuevo().valor(), limpio, descripcionLimpia,
                actorId));
        versiones.save(new VersionPlan(Identificador.nuevo().valor(), plan.getId(), precios.desayuno(),
                precios.almuerzo(), precios.cena(), desde, actorId));
        auditoria.registrar("cambio_plan", "plan_tarifario", plan.getId(), null,
                CambiarPreciosPlan.foto(plan.getNombre(), precios, desde));
        return Resultado.exito(plan);
    }
}
