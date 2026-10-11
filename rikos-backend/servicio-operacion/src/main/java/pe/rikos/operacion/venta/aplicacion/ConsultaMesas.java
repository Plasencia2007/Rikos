package pe.rikos.operacion.venta.aplicacion;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.rikos.contratos.Resultado;
import pe.rikos.operacion.venta.infraestructura.Mesa;
import pe.rikos.operacion.venta.infraestructura.MesaRepositorio;
import pe.rikos.operacion.venta.infraestructura.PedidoRepositorio;

import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * US-027 CA1 y CA2: las mesas del local de la sesion, cada una con su estado
 * derivado. El mapa solo trae las activas; administracion trae todas. Los
 * numeros se ordenan como numeros cuando lo son ("2" antes que "10").
 */
@Service
public class ConsultaMesas {

    private static final Comparator<Mesa> POR_NUMERO = Comparator
            .comparing((Mesa m) -> !soloDigitos(m.getNumero()))
            .thenComparing(m -> soloDigitos(m.getNumero()) ? Long.parseLong(m.getNumero()) : 0L)
            .thenComparing(Mesa::getNumero);

    private final MesaRepositorio mesas;
    private final PedidoRepositorio pedidos;

    public ConsultaMesas(MesaRepositorio mesas, PedidoRepositorio pedidos) {
        this.mesas = mesas;
        this.pedidos = pedidos;
    }

    @Transactional(readOnly = true)
    public Resultado<List<FilaDelMapa>, ErrorMesa> mapa() {
        return listar(false);
    }

    @Transactional(readOnly = true)
    public Resultado<List<FilaDelMapa>, ErrorMesa> todas() {
        return listar(true);
    }

    private Resultado<List<FilaDelMapa>, ErrorMesa> listar(boolean incluirInactivas) {
        String localId = LocalDeSesion.actual().orElse(null);
        if (localId == null) {
            return Resultado.fracaso(ErrorMesa.SIN_LOCAL_ACTIVO);
        }
        // Solo un pedido COMANDADO ocupa su mesa; uno pendiente de cierre la deja "pendiente de cierre"; uno solo abierto
        // la deja libre pero "tomando pedido" (cambio de regla del 2026-10-01).
        Set<String> conComandado = new HashSet<>();
        Set<String> conAbierto = new HashSet<>();
        Set<String> conPendiente = new HashSet<>();
        Map<String, OffsetDateTime> abiertaEn = new HashMap<>();
        pedidos.findByLocalIdAndEstadoIn(localId, AbrirPedido.ESTADOS_EN_CURSO).stream()
                .filter(p -> p.getMesaId() != null)
                .forEach(p -> {
                    switch (p.getEstado()) {
                        case "pendiente_cierre" -> conPendiente.add(p.getMesaId());
                        case "comandado" -> conComandado.add(p.getMesaId());
                        default -> conAbierto.add(p.getMesaId());
                    }
                    abiertaEn.put(p.getMesaId(), p.getAbiertoEn());
                });
        List<FilaDelMapa> filas = mesas.findByLocalIdOrderByNumero(localId).stream()
                .filter(m -> incluirInactivas || m.isActivo())
                .sorted(POR_NUMERO)
                .map(m -> {
                    EstadoDeMesa estado = CalculadorDeEstadoDeMesa.calcular(m.isBloqueada(),
                            m.getMesaPrincipalId() != null, conPendiente.contains(m.getId()),
                            conComandado.contains(m.getId()));
                    return new FilaDelMapa(m, estado, abiertaEn.get(m.getId()),
                            estado == EstadoDeMesa.LIBRE && conAbierto.contains(m.getId()));
                })
                .toList();
        return Resultado.exito(filas);
    }

    private static boolean soloDigitos(String numero) {
        return numero.matches("\\d{1,9}");
    }
}
