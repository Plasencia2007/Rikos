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
import java.util.Map;

/**
 * US-017 CA2, CA3 / RN-021: cambiar los precios de un plan.
 *
 * Nunca reescribe una version que ya rigio: cierra la anterior (el dia antes
 * de la nueva) y abre otra. La vigencia empieza hoy o despues -- un precio
 * "retroactivo" reescribiria lo que ya se cobro (CA3).
 *
 * Excepcion, para poder arreglar un error de tipeo sin esperar a manana: si la
 * nueva vigencia coincide con el inicio de la ultima version (que empezo hoy o
 * todavia no empieza), esa version se corrige en su lugar. Los consumos ya
 * cobrados no se ven afectados porque guardan su propio precio (RN-022), y el
 * cambio queda en la bitacora con el antes y el despues.
 */
@Service
public class CambiarPreciosPlan {

    private final PlanTarifarioRepositorio planes;
    private final VersionPlanRepositorio versiones;
    private final RegistradorAuditoria auditoria;
    private final Clock reloj;

    public CambiarPreciosPlan(PlanTarifarioRepositorio planes, VersionPlanRepositorio versiones,
                              RegistradorAuditoria auditoria, Clock reloj) {
        this.planes = planes;
        this.versiones = versiones;
        this.auditoria = auditoria;
        this.reloj = reloj;
    }

    @Transactional
    public Resultado<PlanTarifario, ErrorPlan> ejecutar(String planId, PreciosPlan precios, LocalDate vigenteDesde,
                                                        String actorId) {
        PlanTarifario plan = planes.findById(planId).orElse(null);
        if (plan == null) {
            return Resultado.fracaso(ErrorPlan.NO_ENCONTRADO);
        }
        ErrorPlan invalido = precios.validar();
        if (invalido != null) {
            return Resultado.fracaso(invalido);
        }
        LocalDate hoy = LocalDate.now(reloj);
        LocalDate desde = vigenteDesde == null ? hoy : vigenteDesde;
        VersionPlan ultima = versiones.findFirstByPlanIdOrderByVigenteDesdeDesc(planId).orElseThrow();
        if (desde.isBefore(hoy) || desde.isBefore(ultima.getVigenteDesde())) {
            return Resultado.fracaso(ErrorPlan.VIGENCIA_INVALIDA);
        }
        PreciosPlan actuales = PreciosPlan.de(ultima);
        if (actuales.equals(precios)) {
            return Resultado.fracaso(ErrorPlan.SIN_CAMBIOS);
        }
        Map<String, Object> antes = foto(plan.getNombre(), actuales, ultima.getVigenteDesde());
        if (desde.equals(ultima.getVigenteDesde())) {
            ultima.corregirPrecios(precios.desayuno(), precios.almuerzo(), precios.cena());
            versiones.save(ultima);
        } else {
            ultima.cerrar(desde.minusDays(1));
            // El indice unico parcial permite una sola version abierta por plan:
            // el cierre tiene que llegar a la base ANTES del insert de la nueva
            // (Hibernate ejecuta los INSERT antes que los UPDATE).
            versiones.saveAndFlush(ultima);
            versiones.save(new VersionPlan(Identificador.nuevo().valor(), planId, precios.desayuno(),
                    precios.almuerzo(), precios.cena(), desde, actorId));
        }
        auditoria.registrar("cambio_plan", "plan_tarifario", planId, antes, foto(plan.getNombre(), precios, desde));
        return Resultado.exito(plan);
    }

    /** Lo que se guarda en la bitacora: precios en centimos y desde cuando rigen. */
    static Map<String, Object> foto(String nombre, PreciosPlan precios, LocalDate desde) {
        return RegistradorAuditoria.campos("nombre", nombre, "desayuno", precios.desayuno(),
                "almuerzo", precios.almuerzo(), "cena", precios.cena(), "vigenteDesde", desde.toString());
    }
}
