package pe.rikos.operacion.pensionistas.api;

import io.swagger.v3.oas.annotations.Operation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.rikos.operacion.pensionistas.aplicacion.ConsultaCuenta;
import pe.rikos.operacion.pensionistas.aplicacion.ConsultaPlanes;
import pe.rikos.operacion.pensionistas.aplicacion.FilaDeCuentaCorriente;
import pe.rikos.operacion.pensionistas.aplicacion.TopeDeCredito;
import pe.rikos.operacion.pensionistas.infraestructura.PlanTarifario;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * La lista de cuentas corrientes de todos los pensionistas (pensionistas.ver):
 * saldo, deuda y tope de cada uno, la mayor deuda primero. El tope es un
 * recordatorio, no un limite.
 */
@RestController
@RequestMapping("/pensionistas/cuentas")
public class CuentasCorrientesController {

    private final ConsultaCuenta consulta;
    private final ConsultaPlanes planes;

    public CuentasCorrientesController(ConsultaCuenta consulta, ConsultaPlanes planes) {
        this.consulta = consulta;
        this.planes = planes;
    }

    /** Todo en centimos. tope.monto null = no se pudo calcular (ver su aviso). */
    public record FilaRespuesta(String pensionistaId, String dni, String nombres, String apellidos,
                                String planNombre, boolean activo, long saldo, long deuda, long saldoAFavor,
                                CuentaController.TopeRespuesta tope, Long creditoDisponible, boolean superaTope) {
    }

    @Operation(summary = "Las cuentas corrientes de todos los pensionistas, la mayor deuda primero")
    @GetMapping
    @Transactional(readOnly = true)
    public List<FilaRespuesta> listar() {
        Map<String, String> nombreDePlan = new HashMap<>();
        return consulta.todas().stream().map(f -> filaDe(f, nombreDePlan)).toList();
    }

    private FilaRespuesta filaDe(FilaDeCuentaCorriente f, Map<String, String> nombreDePlan) {
        var p = f.pensionista();
        var r = f.resumen();
        TopeDeCredito t = r.tope();
        String plan = nombreDePlan.computeIfAbsent(p.getPlanTarifarioId(),
                id -> planes.plan(id).map(PlanTarifario::getNombre).orElse(null));
        return new FilaRespuesta(p.getId(), p.getDni(), p.getNombres(), p.getApellidos(), plan, p.isActivo(),
                r.saldo(), r.deuda(), r.saldoAFavor(),
                new CuentaController.TopeRespuesta(t.monto(), t.origen().name().toLowerCase(),
                        t.aviso() == null ? null : t.aviso().name(), t.comidasContadas()),
                r.creditoDisponible(), r.superaTope());
    }
}
