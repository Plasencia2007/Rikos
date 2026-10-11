package pe.rikos.operacion.venta.aplicacion;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.rikos.contratos.Resultado;
import pe.rikos.operacion.comun.auditoria.RegistradorAuditoria;
import pe.rikos.operacion.venta.infraestructura.Mesa;
import pe.rikos.operacion.venta.infraestructura.MesaRepositorio;
import pe.rikos.operacion.venta.infraestructura.Salon;
import pe.rikos.operacion.venta.infraestructura.SalonRepositorio;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Guarda de una vez el plano de un salon: donde queda cada mesa y cuanto mide. Todo o nada: si una mesa no
 * es de ese salon o una medida no vale, no se guarda ninguna.
 */
@Service
public class GuardarPlano {

    private final SalonRepositorio salones;
    private final MesaRepositorio mesas;
    private final RegistradorAuditoria auditoria;

    public GuardarPlano(SalonRepositorio salones, MesaRepositorio mesas, RegistradorAuditoria auditoria) {
        this.salones = salones;
        this.mesas = mesas;
        this.auditoria = auditoria;
    }

    @Transactional
    public Resultado<Void, ErrorSalon> ejecutar(String salonId, List<PosicionDeMesa> posiciones, String actorId) {
        String localId = LocalDeSesion.actual().orElse(null);
        if (localId == null) {
            return Resultado.fracaso(ErrorSalon.SIN_LOCAL_ACTIVO);
        }
        Salon salon = salones.findByIdAndLocalId(salonId, localId).orElse(null);
        if (salon == null) {
            return Resultado.fracaso(ErrorSalon.NO_ENCONTRADO);
        }
        Map<String, Mesa> delSalon = mesas.findBySalonId(salonId).stream()
                .collect(Collectors.toMap(Mesa::getId, Function.identity()));
        for (PosicionDeMesa p : posiciones) {
            if (!delSalon.containsKey(p.mesaId())) {
                return Resultado.fracaso(ErrorSalon.MESA_NO_ES_DEL_SALON);
            }
            if (p.ancho() <= 0 || p.alto() <= 0 || p.x() < 0 || p.y() < 0) {
                return Resultado.fracaso(ErrorSalon.VALOR_INVALIDO);
            }
        }
        for (PosicionDeMesa p : posiciones) {
            delSalon.get(p.mesaId()).ubicar(p.x(), p.y(), p.ancho(), p.alto(), actorId);
        }
        mesas.saveAll(delSalon.values());
        auditoria.registrar("cambio_mesa", "salon", salonId, null,
                RegistradorAuditoria.campos("nombre", salon.getNombre(), "accion", "plano_guardado", "mesas",
                        posiciones.size()));
        return Resultado.<Void, ErrorSalon>exito(null);
    }
}
