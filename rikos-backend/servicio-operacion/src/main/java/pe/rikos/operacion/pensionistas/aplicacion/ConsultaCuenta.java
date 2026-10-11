package pe.rikos.operacion.pensionistas.aplicacion;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.rikos.operacion.pensionistas.infraestructura.CuentaPensionistaRepositorio;
import pe.rikos.operacion.pensionistas.infraestructura.MovimientoCuenta;
import pe.rikos.operacion.pensionistas.infraestructura.MovimientoCuentaRepositorio;
import pe.rikos.operacion.pensionistas.infraestructura.Pensionista;
import pe.rikos.operacion.pensionistas.infraestructura.PensionistaRepositorio;

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Optional;

/** Lo que se ve de una cuenta: el resumen (saldo, deuda, tope) y el libro. Solo lectura. */
@Service
public class ConsultaCuenta {

    private final PensionistaRepositorio pensionistas;
    private final CuentaPensionistaRepositorio cuentas;
    private final MovimientoCuentaRepositorio movimientos;
    private final CalculadoraDeTope calculadora;
    private final ServiciosDeAtencion atencion;
    private final Clock reloj;

    public ConsultaCuenta(PensionistaRepositorio pensionistas, CuentaPensionistaRepositorio cuentas,
                          MovimientoCuentaRepositorio movimientos, CalculadoraDeTope calculadora,
                          ServiciosDeAtencion atencion, Clock reloj) {
        this.pensionistas = pensionistas;
        this.cuentas = cuentas;
        this.movimientos = movimientos;
        this.calculadora = calculadora;
        this.atencion = atencion;
        this.reloj = reloj;
    }

    @Transactional(readOnly = true)
    public Optional<ResumenDeCuenta> resumen(String pensionistaId) {
        return resumen(pensionistaId, LocalDate.now(reloj));
    }

    @Transactional(readOnly = true)
    public Optional<ResumenDeCuenta> resumen(String pensionistaId, LocalDate hoy) {
        var pensionista = pensionistas.findById(pensionistaId).orElse(null);
        var cuenta = cuentas.findByPensionistaId(pensionistaId).orElse(null);
        if (pensionista == null || cuenta == null) {
            return Optional.empty();
        }
        long saldo = movimientos.saldo(cuenta.getId());
        return Optional.of(armar(saldo, calculadora.calcular(pensionista, hoy)));
    }

    /** Del mas reciente al mas antiguo, segun cuando ocurrio. */
    @Transactional(readOnly = true)
    public List<MovimientoCuenta> movimientos(String pensionistaId) {
        return cuentas.findByPensionistaId(pensionistaId)
                .map(c -> movimientos.findByCuentaIdOrderByOcurridoEnDescRegistradoEnDesc(c.getId()))
                .orElse(List.of());
    }

    /**
     * Todas las cuentas (activos e inactivos), la mayor deuda primero y luego
     * por apellido. Los saldos salen de una sola consulta y los dias de
     * atencion se leen una sola vez para toda la lista.
     */
    @Transactional(readOnly = true)
    public List<FilaDeCuentaCorriente> todas() {
        LocalDate hoy = LocalDate.now(reloj);
        Map<String, Long> saldoPorCuenta = new HashMap<>();
        for (Object[] fila : movimientos.saldos()) {
            saldoPorCuenta.put((String) fila[0], ((Number) fila[1]).longValue());
        }
        Map<String, String> cuentaPorPensionista = new HashMap<>();
        cuentas.findAll().forEach(c -> cuentaPorPensionista.put(c.getPensionistaId(), c.getId()));
        List<Pensionista> todos = pensionistas.findAllByOrderByApellidosAscNombresAsc();
        Map<LocalDate, Set<String>> dias = todos.stream().anyMatch(p -> p.getLimiteCredito() == null)
                ? atencion.serviciosPorDia(hoy.withDayOfMonth(1), hoy.withDayOfMonth(hoy.lengthOfMonth()))
                : Map.of();

        List<FilaDeCuentaCorriente> filas = new ArrayList<>(todos.size());
        for (Pensionista p : todos) {
            long saldo = saldoPorCuenta.getOrDefault(cuentaPorPensionista.get(p.getId()), 0L);
            filas.add(new FilaDeCuentaCorriente(p, armar(saldo, calculadora.calcular(p, hoy, dias))));
        }
        // La lista ya viene por apellido y el orden es estable: solo se sube a quien mas debe.
        filas.sort(Comparator.comparingLong((FilaDeCuentaCorriente f) -> f.resumen().deuda()).reversed());
        return filas;
    }

    private static ResumenDeCuenta armar(long saldo, TopeDeCredito tope) {
        long deuda = Math.max(0, -saldo);
        Long disponible = tope.monto() == null ? null : tope.monto() - deuda;
        boolean supera = tope.monto() != null && deuda > tope.monto();
        return new ResumenDeCuenta(saldo, deuda, Math.max(0, saldo), tope, disponible, supera);
    }
}
