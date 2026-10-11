package pe.rikos.operacion.venta.aplicacion;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import pe.rikos.contratos.Resultado;
import pe.rikos.operacion.pensionistas.aplicacion.AvisoPrecio;
import pe.rikos.operacion.pensionistas.aplicacion.Comida;
import pe.rikos.operacion.pensionistas.aplicacion.ConsultaPensionistas;
import pe.rikos.operacion.pensionistas.aplicacion.MotorDePrecios;
import pe.rikos.operacion.pensionistas.aplicacion.ParametrosGlobales;
import pe.rikos.operacion.pensionistas.aplicacion.PrecioCalculado;
import pe.rikos.operacion.pensionistas.aplicacion.ReglaSegundoConsumo;
import pe.rikos.operacion.pensionistas.aplicacion.SolicitudDePrecio;
import pe.rikos.operacion.pensionistas.aplicacion.TipoLinea;
import pe.rikos.operacion.pensionistas.infraestructura.Pensionista;
import pe.rikos.operacion.venta.infraestructura.CuentaPersona;
import pe.rikos.operacion.venta.infraestructura.DetallePedidoRepositorio;

import java.time.LocalDate;

/**
 * Traduce una linea del pedido a la pregunta que entiende el motor de precios del sprint 5 y devuelve el
 * precio, el taper por unidad, de donde salio y si fue el segundo consumo del dia. El mozo nunca escribe un
 * precio: solo el "extra libre" trae el suyo, que aqui entra como precio normal de un agregado.
 */
@Component
public class CalculadorDePrecioDeLinea {

    private final MotorDePrecios motor;
    private final ParametrosGlobales parametros;
    private final DetallePedidoRepositorio lineas;
    private final ConsultaPensionistas consultaPensionistas;

    public CalculadorDePrecioDeLinea(MotorDePrecios motor, ParametrosGlobales parametros,
                                     DetallePedidoRepositorio lineas, ConsultaPensionistas consultaPensionistas) {
        this.motor = motor;
        this.parametros = parametros;
        this.lineas = lineas;
        this.consultaPensionistas = consultaPensionistas;
    }

    @Transactional(readOnly = true)
    public Resultado<PrecioDeLinea, ErrorPedido> calcular(TipoDeLinea tipo, String servicio, Long precioNormal,
                                                          CuentaPersona persona, boolean paraLlevar, LocalDate hoy) {
        return calcular(tipo, servicio, precioNormal, persona, paraLlevar, hoy, 0);
    }

    /**
     * consumosPrevios: comidas del plan que el pensionista va a tener ADEMAS de las que ya estan guardadas, porque se
     * estan pasando a su cuenta en la misma operacion (TrasladarLineas): asi el segundo menu que llega junto con el
     * primero tambien se marca como segundo del dia.
     */
    @Transactional(readOnly = true)
    public Resultado<PrecioDeLinea, ErrorPedido> calcular(TipoDeLinea tipo, String servicio, Long precioNormal,
                                                          CuentaPersona persona, boolean paraLlevar, LocalDate hoy,
                                                          int consumosPrevios) {
        TipoLinea tipoMotor = switch (tipo) {
            case PRODUCTO, EXTRA_LIBRE -> TipoLinea.AGREGADO;
            case MENU_EXTRA -> TipoLinea.MENU_EXTRA;
            case MENU_NORMAL, PLAN_DESAYUNO, PLAN_CENA -> TipoLinea.MENU_DEL_DIA;
        };
        String planId = null;
        boolean segundo = false;
        if (tipoMotor == TipoLinea.MENU_DEL_DIA && persona.getPensionistaId() != null) {
            Pensionista pensionista = consultaPensionistas.pensionista(persona.getPensionistaId()).orElse(null);
            planId = pensionista == null ? null : pensionista.getPlanTarifarioId();
            segundo = lineas.consumosDelPlanDelDia(persona.getPensionistaId(), hoy, servicio) + consumosPrevios > 0;
        }
        // La regla "el segundo va a precio normal" es del menu del almuerzo. El desayuno y la cena del plan no
        // tienen un precio normal (no son un producto de la carta): su segundo se cobra igual al plan.
        boolean segundoAlMotor = segundo && tipo == TipoDeLinea.MENU_NORMAL;
        Comida comida = Comida.desde(servicio).orElse(Comida.ALMUERZO);
        var respuesta = motor.calcular(new SolicitudDePrecio(tipoMotor, comida, precioNormal, planId, segundoAlMotor,
                paraLlevar, hoy));
        if (!respuesta.esExito()) {
            return Resultado.fracaso(ErrorPedido.SIN_PRECIO);
        }
        PrecioCalculado calculado = respuesta.valorOLanza();
        if (calculado.avisos().contains(AvisoPrecio.PLAN_SIN_PRECIO)) {
            return Resultado.fracaso(ErrorPedido.PLAN_SIN_PRECIO);
        }
        if (calculado.precio() == null) {
            return Resultado.fracaso(ErrorPedido.SIN_PRECIO);
        }
        // El origen se decide por la REGLA que aplico el motor, no comparando montos: un plan que cuesta lo mismo
        // que el precio normal sigue siendo del plan.
        boolean cobradoAlPlan = planId != null && !(segundoAlMotor
                && parametros.reglaSegundoConsumo() == ReglaSegundoConsumo.PRECIO_NORMAL);
        String origen = switch (tipo) {
            case EXTRA_LIBRE -> "manual";
            case MENU_EXTRA -> "extra";
            case PRODUCTO -> "publico";
            default -> cobradoAlPlan ? "plan" : "publico";
        };
        return Resultado.exito(new PrecioDeLinea(calculado.precio(), calculado.taper() == null ? 0L : calculado.taper(),
                origen, "plan".equals(origen) ? planId : null, segundo && planId != null));
    }
}
