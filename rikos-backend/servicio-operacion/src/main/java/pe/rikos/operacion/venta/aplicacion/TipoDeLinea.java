package pe.rikos.operacion.venta.aplicacion;

import java.util.Arrays;
import java.util.Optional;

/**
 * Que se esta vendiendo en una linea del pedido. Los codigos son los del CHECK de detalle_pedido.tipo_linea.
 * Ojo: no es pensionistas.TipoLinea (lo que le pregunta la caja al motor de precios); aqui hay mas tipos.
 */
public enum TipoDeLinea {

    MENU_NORMAL("menu_normal"),
    MENU_EXTRA("menu_extra"),
    /** La comida fija del plan del pensionista en el desayuno: sin producto de la carta (spec 3.11). */
    PLAN_DESAYUNO("plan_desayuno"),
    PLAN_CENA("plan_cena"),
    PRODUCTO("producto"),
    /** El unico caso en que el mozo pone un precio (spec 3.11): queda marcado como manual y en la bitacora. */
    EXTRA_LIBRE("extra_libre");

    private final String codigo;

    TipoDeLinea(String codigo) {
        this.codigo = codigo;
    }

    public String codigo() {
        return codigo;
    }

    public static Optional<TipoDeLinea> desde(String codigo) {
        return Arrays.stream(values()).filter(t -> t.codigo.equals(codigo)).findFirst();
    }
}
