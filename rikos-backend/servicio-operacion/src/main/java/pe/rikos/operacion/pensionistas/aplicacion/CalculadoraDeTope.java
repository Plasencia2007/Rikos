package pe.rikos.operacion.pensionistas.aplicacion;

import org.springframework.stereotype.Component;
import pe.rikos.operacion.comun.tiempo.ConfiguracionReloj;
import pe.rikos.operacion.pensionistas.infraestructura.Pensionista;
import pe.rikos.operacion.pensionistas.infraestructura.VersionPlan;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * El tope de credito (sprint 5, 3.8 y 3.10). Si esta fijado a mano, ese es. Si
 * no: para cada dia desde el inicio del mes (o desde el dia del alta, si fue este
 * mes) hasta fin de mes, y para cada comida habitual del pensionista, se suma el
 * precio de su plan de ESE dia -- solo si algun local sirve esa comida ese dia.
 *
 * Si falta un precio del plan en una comida que cuenta, o ningun local tiene
 * horarios, devuelve "sin calcular" con su aviso: un tope que subestima
 * mostraria un recordatorio falso.
 */
@Component
class CalculadoraDeTope {

    private final ServiciosDeAtencion atencion;
    private final ConsultaPlanes planes;

    CalculadoraDeTope(ServiciosDeAtencion atencion, ConsultaPlanes planes) {
        this.atencion = atencion;
        this.planes = planes;
    }

    TopeDeCredito calcular(Pensionista pensionista, LocalDate hoy) {
        if (pensionista.getLimiteCredito() != null) {
            return fijado(pensionista);
        }
        return calcular(pensionista, hoy,
                atencion.serviciosPorDia(inicioDe(pensionista, hoy), hoy.withDayOfMonth(hoy.lengthOfMonth())));
    }

    /**
     * Con los dias de atencion ya leidos (para calcular una lista entera sin
     * releer los horarios por cada pensionista): deben cubrir desde el primer
     * dia del mes hasta el ultimo.
     */
    TopeDeCredito calcular(Pensionista pensionista, LocalDate hoy, Map<LocalDate, Set<String>> dias) {
        if (pensionista.getLimiteCredito() != null) {
            return fijado(pensionista);
        }
        LocalDate inicio = inicioDe(pensionista, hoy);
        LocalDate fin = hoy.withDayOfMonth(hoy.lengthOfMonth());
        List<VersionPlan> versiones = planes.versiones(pensionista.getPlanTarifarioId());

        long total = 0;
        int contadas = 0;
        boolean sinPrecio = false;
        for (LocalDate dia = inicio; !dia.isAfter(fin); dia = dia.plusDays(1)) {
            Set<String> servicios = dias.getOrDefault(dia, Set.of());
            for (Comida comida : pensionista.getComidas()) {
                if (!servicios.contains(comida.codigo())) {
                    continue;
                }
                contadas++;
                Optional<Long> precio = precioEn(versiones, comida, dia);
                if (precio.isPresent()) {
                    total += precio.get();
                } else {
                    sinPrecio = true;
                }
            }
        }
        if (contadas == 0) {
            return new TopeDeCredito(null, TopeDeCredito.Origen.CALCULADO, TopeDeCredito.Aviso.SIN_DIAS_DE_ATENCION, 0);
        }
        if (sinPrecio) {
            return new TopeDeCredito(null, TopeDeCredito.Origen.CALCULADO, TopeDeCredito.Aviso.PLAN_SIN_PRECIO,
                    contadas);
        }
        return new TopeDeCredito(total, TopeDeCredito.Origen.CALCULADO, null, contadas);
    }

    private static TopeDeCredito fijado(Pensionista pensionista) {
        return new TopeDeCredito(pensionista.getLimiteCredito(), TopeDeCredito.Origen.FIJADO, null, 0);
    }

    /** Desde el inicio del mes, o desde el dia del alta si fue este mes. */
    private static LocalDate inicioDe(Pensionista pensionista, LocalDate hoy) {
        LocalDate altaEn = pensionista.getCreadoEn().atZoneSameInstant(ConfiguracionReloj.ZONA_DEL_RESTAURANTE)
                .toLocalDate();
        return altaEn.isAfter(hoy.withDayOfMonth(1)) ? altaEn : hoy.withDayOfMonth(1);
    }

    private static Optional<Long> precioEn(List<VersionPlan> versiones, Comida comida, LocalDate dia) {
        return versiones.stream().filter(v -> v.rigeEn(dia)).findFirst().map(v -> switch (comida) {
            case DESAYUNO -> v.getPrecioDesayuno();
            case ALMUERZO -> v.getPrecioAlmuerzo();
            case CENA -> v.getPrecioCena();
        });
    }
}
