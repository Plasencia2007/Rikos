package pe.rikos.operacion.catalogo.aplicacion;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import pe.rikos.operacion.catalogo.infraestructura.OpcionMenu;
import pe.rikos.operacion.catalogo.infraestructura.OpcionMenuRepositorio;
import pe.rikos.operacion.catalogo.infraestructura.SemanaMenu;
import pe.rikos.operacion.catalogo.infraestructura.SemanaMenuRepositorio;

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * US-013: lecturas de la programacion semanal y las reglas que dependen de
 * la fecha -- "hoy" en la zona del restaurante, los dias cerrados, la
 * advertencia por repeticion y el menu que ve el POS.
 */
@Component
public class ConsultaMenu {

    /** RN-041: se compara con las tres fechas de calendario anteriores, cruzando semanas. */
    static final int DIAS_DE_REPETICION = 3;

    /** fecha: el dia que repite; repetidoEl: el dia anterior (el mas cercano) en que ya se sirvio. */
    public record Advertencia(LocalDate fecha, String productoId, String productoNombre, LocalDate repetidoEl) {
    }

    private final SemanaMenuRepositorio semanas;
    private final OpcionMenuRepositorio opciones;
    private final Clock reloj;

    public ConsultaMenu(SemanaMenuRepositorio semanas, OpcionMenuRepositorio opciones, Clock reloj) {
        this.semanas = semanas;
        this.opciones = opciones;
        this.reloj = reloj;
    }

    public LocalDate hoy() {
        return LocalDate.now(reloj);
    }

    /** Decision con el usuario: un dia anterior a hoy ya es historia de lo que se sirvio. */
    public boolean estaCerrado(LocalDate fecha) {
        return fecha.isBefore(hoy());
    }

    @Transactional(readOnly = true)
    public Optional<SemanaMenu> semana(String id) {
        return semanas.findById(id);
    }

    @Transactional(readOnly = true)
    public List<OpcionMenu> opcionesDeLaSemana(String semanaId) {
        return opciones.findDeLaSemana(semanaId);
    }

    /**
     * CA3 / RN-041: cada segundo de la semana que ya se sirvio (en el normal
     * o en el extra) en alguna de las tres fechas anteriores. Solo avisa:
     * nada en el sistema lo convierte en un bloqueo.
     */
    @Transactional(readOnly = true)
    public List<Advertencia> advertenciasDeLaSemana(String semanaId) {
        SemanaMenu semana = semanas.findById(semanaId).orElse(null);
        if (semana == null) {
            return List.of();
        }
        List<OpcionMenu> segundos = opciones.findSegundosEntre(
                semana.getFechaInicio().minusDays(DIAS_DE_REPETICION), semana.getFechaFin());
        List<Advertencia> advertencias = new ArrayList<>();
        for (OpcionMenu actual : segundos) {
            LocalDate fecha = actual.getDia().getFecha();
            if (!semana.contiene(fecha)) {
                continue;
            }
            segundos.stream()
                    .filter(anterior -> anterior.getProducto().getId().equals(actual.getProducto().getId()))
                    .map(anterior -> anterior.getDia().getFecha())
                    .filter(f -> f.isBefore(fecha) && !f.isBefore(fecha.minusDays(DIAS_DE_REPETICION)))
                    .max(Comparator.naturalOrder())
                    .ifPresent(repetidoEl -> advertencias.add(new Advertencia(fecha, actual.getProducto().getId(),
                            actual.getProducto().getNombre(), repetidoEl)));
        }
        // Un mismo producto como normal y extra el mismo dia no se repite
        // (uq_opcion_menu_dia_producto), asi que no hay duplicados que limpiar.
        advertencias.sort(Comparator.comparing(Advertencia::fecha).thenComparing(Advertencia::productoNombre));
        return advertencias;
    }

    /**
     * CA4 / CA6: lo que el POS ofrece ese dia. Vacio (Optional.empty) si la
     * semana no existe o no esta confirmada -- un borrador no existe para el
     * POS. Lista vacia si la semana esta confirmada pero ese dia no hay
     * almuerzo.
     */
    @Transactional(readOnly = true)
    public Optional<List<OpcionMenu>> menuConfirmadoDel(LocalDate fecha) {
        LocalDate lunes = fecha.minusDays(fecha.getDayOfWeek().getValue() - 1L);
        return semanas.findByFechaInicio(lunes)
                .filter(SemanaMenu::isConfirmada)
                .map(s -> opciones.findDelDia(fecha));
    }
}
