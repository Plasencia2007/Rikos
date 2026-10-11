package pe.rikos.operacion.venta.aplicacion;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.rikos.contratos.Resultado;
import pe.rikos.operacion.venta.infraestructura.Mesa;
import pe.rikos.operacion.venta.infraestructura.MesaRepositorio;
import pe.rikos.operacion.venta.infraestructura.Salon;
import pe.rikos.operacion.venta.infraestructura.SalonRepositorio;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** Los salones del local de la sesion, cada uno con cuantas mesas tiene y cuanta gente cabe. */
@Service
public class ConsultaSalones {

    private final SalonRepositorio salones;
    private final MesaRepositorio mesas;

    public ConsultaSalones(SalonRepositorio salones, MesaRepositorio mesas) {
        this.salones = salones;
        this.mesas = mesas;
    }

    @Transactional(readOnly = true)
    public Resultado<List<ResumenDeSalon>, ErrorSalon> listar() {
        String localId = LocalDeSesion.actual().orElse(null);
        if (localId == null) {
            return Resultado.fracaso(ErrorSalon.SIN_LOCAL_ACTIVO);
        }
        Map<String, List<Mesa>> porSalon = mesas.findByLocalIdOrderByNumero(localId).stream()
                .filter(m -> m.getSalonId() != null)
                .collect(Collectors.groupingBy(Mesa::getSalonId));
        List<ResumenDeSalon> resumenes = salones.findByLocalIdOrderByNombre(localId).stream()
                .map(s -> resumir(s, porSalon.getOrDefault(s.getId(), List.of())))
                .toList();
        return Resultado.exito(resumenes);
    }

    @Transactional(readOnly = true)
    public Resultado<ResumenDeSalon, ErrorSalon> obtener(String id) {
        String localId = LocalDeSesion.actual().orElse(null);
        if (localId == null) {
            return Resultado.fracaso(ErrorSalon.SIN_LOCAL_ACTIVO);
        }
        return salones.findByIdAndLocalId(id, localId)
                .map(s -> Resultado.<ResumenDeSalon, ErrorSalon>exito(resumir(s, mesas.findBySalonId(s.getId()))))
                .orElseGet(() -> Resultado.fracaso(ErrorSalon.NO_ENCONTRADO));
    }

    private static ResumenDeSalon resumir(Salon salon, List<Mesa> deSalon) {
        List<Mesa> activas = deSalon.stream().filter(Mesa::isActivo).toList();
        int capacidad = activas.stream().mapToInt(m -> m.getCapacidad() == null ? 0 : m.getCapacidad()).sum();
        return new ResumenDeSalon(salon, deSalon.size(), activas.size(), capacidad);
    }
}
