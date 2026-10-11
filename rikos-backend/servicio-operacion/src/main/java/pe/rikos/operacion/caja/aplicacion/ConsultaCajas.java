package pe.rikos.operacion.caja.aplicacion;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.rikos.contratos.Resultado;
import pe.rikos.operacion.caja.infraestructura.Caja;
import pe.rikos.operacion.caja.infraestructura.TurnoCaja;
import pe.rikos.operacion.caja.infraestructura.TurnoCajaRepositorio;
import pe.rikos.operacion.identidad.infraestructura.Usuario;
import pe.rikos.operacion.identidad.infraestructura.UsuarioRepositorio;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Las cajas del local activo con su turno abierto, y el turno abierto de un cajero. */
@Service
public class ConsultaCajas {

    private final CajaPorDefecto cajaPorDefecto;
    private final TurnoCajaRepositorio turnos;
    private final UsuarioRepositorio usuarios;

    public ConsultaCajas(CajaPorDefecto cajaPorDefecto, TurnoCajaRepositorio turnos, UsuarioRepositorio usuarios) {
        this.cajaPorDefecto = cajaPorDefecto;
        this.turnos = turnos;
        this.usuarios = usuarios;
    }

    /** Siembra "Caja 1" si el local no tiene ninguna: por eso escribe, aunque sea una consulta. */
    @Transactional
    public Resultado<List<FilaDeCaja>, ErrorCaja> listar() {
        String localId = LocalDeSesion.actual().orElse(null);
        if (localId == null) {
            return Resultado.fracaso(ErrorCaja.SIN_LOCAL_ACTIVO);
        }
        List<Caja> cajas = cajaPorDefecto.asegurar(localId, null);
        Map<String, TurnoCaja> abiertos = turnos
                .findByCajaIdInAndEstado(cajas.stream().map(Caja::getId).toList(), TurnoCaja.ABIERTO).stream()
                .collect(Collectors.toMap(TurnoCaja::getCajaId, Function.identity()));
        Map<String, String> nombres = usuarios
                .findAllById(abiertos.values().stream().map(TurnoCaja::getCajeroId).toList()).stream()
                .collect(Collectors.toMap(Usuario::getId, u -> nombreDe(u)));
        return Resultado.exito(cajas.stream().map(c -> {
            TurnoCaja turno = abiertos.get(c.getId());
            return new FilaDeCaja(c, turno, turno == null ? null : nombres.get(turno.getCajeroId()));
        }).toList());
    }

    /** El nombre con que se muestra al cajero en "Caja de ...": sus nombres, o su usuario si no tiene. */
    static String nombreDe(Usuario u) {
        return u.getNombres() == null || u.getNombres().isBlank() ? u.getNombreUsuario() : u.getNombres().strip();
    }

    /** El nombre de un cajero (para rotular su caja); nulo si no existe. */
    @Transactional(readOnly = true)
    public String nombreDelCajero(String cajeroId) {
        return usuarios.findById(cajeroId).map(ConsultaCajas::nombreDe).orElse(null);
    }

    /** El turno abierto de este cajero en el local activo, si tiene uno. */
    @Transactional(readOnly = true)
    public Resultado<Optional<TurnoCaja>, ErrorCaja> turnoActual(String cajeroId) {
        String localId = LocalDeSesion.actual().orElse(null);
        if (localId == null) {
            return Resultado.fracaso(ErrorCaja.SIN_LOCAL_ACTIVO);
        }
        return Resultado.exito(turnos.findByCajeroIdAndEstado(cajeroId, TurnoCaja.ABIERTO)
                .filter(t -> t.getLocalId().equals(localId)));
    }
}
