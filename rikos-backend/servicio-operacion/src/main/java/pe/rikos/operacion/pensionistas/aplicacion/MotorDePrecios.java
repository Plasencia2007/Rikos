package pe.rikos.operacion.pensionistas.aplicacion;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import pe.rikos.contratos.Resultado;

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Cuanto se cobra por una linea (sprint 5, seccion 3.5). Es la unica pieza
 * que conoce estas reglas; la caja (sprint 7) la llama y no las repite.
 *
 * Las reglas, en el orden en que se aplican:
 *  1. Agregado (huevo frito, porcion adicional, plato de carta): precio
 *     normal, para todos. Sin tarifa de pensionista.
 *  2. Menu extra: precio completo, para todos (RN-023).
 *  3. Menu del dia: cliente normal -> precio normal. Pensionista con ticket
 *     -> precio de su plan para esa comida en esa fecha. Su segundo consumo
 *     de la misma comida ese dia va con el plan o a precio normal segun el
 *     parametro global.
 *  4. Para llevar / recojo / delivery: se suma el taper por cada menu (del
 *     dia o extra), a todos por igual. El agregado no lleva taper.
 *
 * Nunca inventa: si falta un precio devuelve null y un aviso. Cada consumo
 * cobrado guarda su propio precio (RN-022), asi que un cambio de plan no lo
 * toca; aqui el precio depende de la fecha del consumo.
 *
 * Sin estado ni efectos: no escribe nada.
 */
@Component
public class MotorDePrecios {

    private final ConsultaPlanes planes;
    private final ParametrosGlobales parametros;
    private final Clock reloj;

    public MotorDePrecios(ConsultaPlanes planes, ParametrosGlobales parametros, Clock reloj) {
        this.planes = planes;
        this.parametros = parametros;
        this.reloj = reloj;
    }

    @Transactional(readOnly = true)
    public Resultado<PrecioCalculado, ErrorPrecio> calcular(SolicitudDePrecio pedido) {
        if (pedido.planId() != null && planes.plan(pedido.planId()).isEmpty()) {
            return Resultado.fracaso(ErrorPrecio.PLAN_NO_ENCONTRADO);
        }
        LocalDate fecha = pedido.fecha() == null ? LocalDate.now(reloj) : pedido.fecha();
        List<AvisoPrecio> avisos = new ArrayList<>();
        List<String> explicacion = new ArrayList<>();

        Long precio = precioDeLaLinea(pedido, fecha, avisos, explicacion);
        Long taper = taperDeLaLinea(pedido, avisos, explicacion);
        return Resultado.exito(new PrecioCalculado(precio, taper, List.copyOf(avisos), List.copyOf(explicacion)));
    }

    private Long precioDeLaLinea(SolicitudDePrecio pedido, LocalDate fecha, List<AvisoPrecio> avisos,
                                 List<String> explicacion) {
        return switch (pedido.tipo()) {
            case AGREGADO -> {
                explicacion.add("Agregado: precio normal, sin tarifa de pensionista.");
                yield precioNormal(pedido, avisos);
            }
            case MENU_EXTRA -> {
                explicacion.add("Menú extra: precio completo para todos, sin tarifa del plan.");
                yield precioNormal(pedido, avisos);
            }
            case MENU_DEL_DIA -> precioDelMenuDelDia(pedido, fecha, avisos, explicacion);
        };
    }

    private Long precioDelMenuDelDia(SolicitudDePrecio pedido, LocalDate fecha, List<AvisoPrecio> avisos,
                                     List<String> explicacion) {
        if (pedido.planId() == null) {
            explicacion.add("Cliente sin ticket de pensionista: precio normal.");
            return precioNormal(pedido, avisos);
        }
        if (pedido.segundoDelDia() && parametros.reglaSegundoConsumo() == ReglaSegundoConsumo.PRECIO_NORMAL) {
            explicacion.add("Segundo " + pedido.comida().codigo()
                    + " del día: por la configuración va a precio normal, no al del plan.");
            return precioNormal(pedido, avisos);
        }
        Optional<Long> delPlan = planes.precio(pedido.planId(), pedido.comida(), fecha);
        if (delPlan.isEmpty()) {
            avisos.add(AvisoPrecio.PLAN_SIN_PRECIO);
            explicacion.add("El plan no tiene precio de " + pedido.comida().codigo() + " para esa fecha.");
            return null;
        }
        explicacion.add(pedido.segundoDelDia()
                ? "Segundo " + pedido.comida().codigo() + " del día: precio del plan del pensionista."
                : "Pensionista con ticket: precio de su plan para " + pedido.comida().codigo() + ".");
        return delPlan.get();
    }

    private Long precioNormal(SolicitudDePrecio pedido, List<AvisoPrecio> avisos) {
        if (pedido.precioNormal() == null) {
            avisos.add(AvisoPrecio.SIN_PRECIO_NORMAL);
            return null;
        }
        return pedido.precioNormal();
    }

    /** El taper va por menu, no por agregado, y solo para llevar / recojo / delivery. */
    private Long taperDeLaLinea(SolicitudDePrecio pedido, List<AvisoPrecio> avisos, List<String> explicacion) {
        if (!pedido.paraLlevar() || pedido.tipo() == TipoLinea.AGREGADO) {
            return null;
        }
        Optional<Long> monto = parametros.taperPrecio();
        if (monto.isEmpty()) {
            avisos.add(AvisoPrecio.TAPER_SIN_MONTO);
            explicacion.add("Para llevar: falta el monto del táper, no se cobra táper.");
            return null;
        }
        explicacion.add("Para llevar: se suma el táper por cada menú.");
        return monto.get();
    }
}
