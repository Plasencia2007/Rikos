package pe.rikos.operacion.pensionistas.aplicacion;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.rikos.operacion.identidad.aplicacion.ResolverServicioVigente;
import pe.rikos.operacion.identidad.infraestructura.LocalRepositorio;
import pe.rikos.operacion.comun.tiempo.ConfiguracionReloj;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;

@Service
public class ConsultarContextoKiosco {
    public record LocalDisponible(String codigo, String nombre) { }
    public record Contexto(List<LocalDisponible> locales, String localCodigo, String localNombre,
                           String estado, ResolverServicioVigente.Franja servicioActual,
                           ResolverServicioVigente.Franja siguienteServicio, OffsetDateTime horaServidor, int longitudPin) { }
    private final LocalRepositorio locales;
    private final ResolverServicioVigente horarios;
    private final Clock reloj;
    public ConsultarContextoKiosco(LocalRepositorio locales, ResolverServicioVigente horarios, Clock reloj) {
        this.locales = locales; this.horarios = horarios; this.reloj = reloj;
    }
    @Transactional(readOnly = true)
    public Contexto consultar(String codigo) {
        var disponibles = locales.findAllByOrderByNombreAsc().stream().filter(l -> l.isActivo())
                .map(l -> new LocalDisponible(l.getCodigo(), l.getNombre())).toList();
        var ahora = OffsetDateTime.now(reloj).atZoneSameInstant(ConfiguracionReloj.ZONA_DEL_RESTAURANTE);
        var local = codigo == null || codigo.isBlank() ? null : locales.findByCodigo(codigo).filter(l -> l.isActivo()).orElse(null);
        if (local == null) return new Contexto(disponibles, null, null, "sin_local", null, null,
                ahora.toOffsetDateTime(), GeneradorDePin.LONGITUD);
        var actual = horarios.franjaVigente(local.getId(), ahora.toLocalDateTime());
        var siguiente = actual.isEmpty() ? horarios.siguienteFranja(local.getId(), ahora.toLocalDateTime()).orElse(null) : null;
        return new Contexto(disponibles, local.getCodigo(), local.getNombre(), actual.isPresent() ? "en_atencion" : "fuera_de_horario",
                actual.orElse(null), siguiente, ahora.toOffsetDateTime(), GeneradorDePin.LONGITUD);
    }
}
