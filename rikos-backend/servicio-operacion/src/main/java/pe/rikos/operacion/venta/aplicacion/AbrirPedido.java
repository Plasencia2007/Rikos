package pe.rikos.operacion.venta.aplicacion;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.rikos.contratos.Identificador;
import pe.rikos.contratos.Resultado;
import pe.rikos.operacion.identidad.aplicacion.ResolverServicioVigente;
import pe.rikos.operacion.venta.infraestructura.CuentaPersona;
import pe.rikos.operacion.venta.infraestructura.CuentaPersonaRepositorio;
import pe.rikos.operacion.venta.infraestructura.Mesa;
import pe.rikos.operacion.venta.infraestructura.MesaRepositorio;
import pe.rikos.operacion.venta.infraestructura.Pedido;
import pe.rikos.operacion.venta.infraestructura.PedidoRepositorio;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

/**
 * US-028: abre un pedido en una mesa libre o para llevar (sin mesa). El servicio (desayuno, almuerzo o
 * cena) se toma del horario vigente del local; si no hay uno (fuera de horario), el mozo lo elige. El id
 * puede venir del cliente: reenviarlo no duplica (3.9, trabajo sin internet). Crea "Persona 1".
 */
@Service
public class AbrirPedido {

    /** Los estados en que un pedido sigue ocupando su mesa. */
    static final List<String> ESTADOS_EN_CURSO = List.of("abierto", "comandado", "pendiente_cierre");
    /** Un pedido cerrado (o anulado) no se modifica. */
    static boolean estaCerrado(Pedido pedido) {
        return "cerrado".equals(pedido.getEstado()) || "anulado".equals(pedido.getEstado());
    }

    private static final Set<String> SERVICIOS = Set.of("desayuno", "almuerzo", "cena");
    private static final int MAXIMO_DE_COMENSALES = 30;
    private static final int MAXIMO_NOMBRE_DE_CLIENTE = 120;

    private final PedidoRepositorio pedidos;
    private final CuentaPersonaRepositorio personas;
    private final MesaRepositorio mesas;
    private final ResolverServicioVigente servicioVigente;
    private final Clock reloj;

    public AbrirPedido(PedidoRepositorio pedidos, CuentaPersonaRepositorio personas, MesaRepositorio mesas,
                       ResolverServicioVigente servicioVigente, Clock reloj) {
        this.pedidos = pedidos;
        this.personas = personas;
        this.mesas = mesas;
        this.servicioVigente = servicioVigente;
        this.reloj = reloj;
    }

    /** Una sola persona y sin nombre de cliente. Con su propia transaccion: la llamada interna no pasa por el proxy. */
    @Transactional
    public Resultado<Pedido, ErrorPedido> ejecutar(String id, Canal canal, String mesaId, String servicio,
                                                   String mozoId) {
        return ejecutar(id, canal, mesaId, servicio, 1, null, mozoId);
    }

    /**
     * comensales (1 a 30) crea "Persona 1" a "Persona N"; nombreCliente (opcional, hasta 120) sirve para reconocer la
     * mesa ("Familia Garcia"): un texto en blanco cuenta como sin nombre.
     */
    @Transactional
    public Resultado<Pedido, ErrorPedido> ejecutar(String id, Canal canal, String mesaId, String servicio,
                                                   int comensales, String nombreCliente, String mozoId) {
        String localId = LocalDeSesion.actual().orElse(null);
        if (localId == null) {
            return Resultado.fracaso(ErrorPedido.SIN_LOCAL_ACTIVO);
        }
        if (comensales < 1 || comensales > MAXIMO_DE_COMENSALES) {
            return Resultado.fracaso(ErrorPedido.COMENSALES_INVALIDOS);
        }
        String nombre = nombreCliente == null || nombreCliente.isBlank() ? null : nombreCliente.trim();
        if (nombre != null && nombre.length() > MAXIMO_NOMBRE_DE_CLIENTE) {
            return Resultado.fracaso(ErrorPedido.NOMBRE_DE_CLIENTE_LARGO);
        }
        if (id != null) {
            var existente = pedidos.findById(id);
            if (existente.isPresent()) {
                return existente.get().getLocalId().equals(localId) ? Resultado.exito(existente.get())
                        : Resultado.fracaso(ErrorPedido.NO_ENCONTRADO);
            }
        }
        if (canal == Canal.PARA_LLEVAR && mesaId != null) {
            return Resultado.fracaso(ErrorPedido.CANAL_INVALIDO);
        }
        if (canal == Canal.MESA) {
            if (mesaId == null) {
                return Resultado.fracaso(ErrorPedido.MESA_REQUERIDA);
            }
            Mesa mesa = mesas.findByIdAndLocalId(mesaId, localId).orElse(null);
            if (mesa == null) {
                return Resultado.fracaso(ErrorPedido.MESA_NO_ENCONTRADA);
            }
            if (!mesa.isActivo() || mesa.isBloqueada() || mesa.getMesaPrincipalId() != null) {
                return Resultado.fracaso(ErrorPedido.MESA_NO_DISPONIBLE);
            }
            var enCurso = pedidos.findByMesaIdAndEstadoIn(mesaId, ESTADOS_EN_CURSO);
            if (enCurso.isPresent()) {
                // Un pedido abierto sin comandar no ocupa la mesa: quien llega despues entra a ESE pedido (idempotente).
                // Uno comandado o pendiente de cierre si: la mesa esta ocupada.
                return "abierto".equals(enCurso.get().getEstado()) ? Resultado.exito(enCurso.get())
                        : Resultado.fracaso(ErrorPedido.MESA_OCUPADA);
            }
        }
        // La zona es la del propio reloj (America/Lima, ConfiguracionReloj): asi tambien sirve con un reloj fijo de prueba.
        LocalDateTime ahora = LocalDateTime.now(reloj);
        String servicioDelPedido = servicio;
        if (servicioDelPedido == null) {
            servicioDelPedido = servicioVigente.resolver(localId, ahora).orElse(null);
            if (servicioDelPedido == null) {
                return Resultado.fracaso(ErrorPedido.SERVICIO_REQUERIDO);
            }
        } else if (!SERVICIOS.contains(servicioDelPedido)) {
            return Resultado.fracaso(ErrorPedido.SERVICIO_INVALIDO);
        }
        LocalDate hoy = ahora.toLocalDate();
        String numero = String.format("%04d", pedidos.countByLocalIdAndFechaOperativa(localId, hoy) + 1);
        Pedido pedido = pedidos.save(new Pedido(id != null ? id : Identificador.nuevo().valor(), localId, numero, hoy,
                canal.codigo(), servicioDelPedido, canal == Canal.MESA ? mesaId : null, mozoId));
        pedido.nombrarCliente(nombre);
        for (int orden = 1; orden <= comensales; orden++) {
            personas.save(new CuentaPersona(Identificador.nuevo().valor(), pedido.getId(), "Persona " + orden, orden));
        }
        return Resultado.exito(pedido);
    }
}
