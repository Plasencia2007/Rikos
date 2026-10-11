package pe.rikos.operacion.venta.aplicacion;

/**
 * RN-059: los cinco estados de una mesa. No se guardan: se derivan al
 * consultar (sprint 6, 3.2). El orden de las constantes es el del modelo.
 */
public enum EstadoDeMesa {

    LIBRE("libre"),
    OCUPADA("ocupada"),
    UNIDA("unida"),
    PENDIENTE_CIERRE("pendiente_cierre"),
    BLOQUEADA("bloqueada");

    private final String codigo;

    EstadoDeMesa(String codigo) {
        this.codigo = codigo;
    }

    public String codigo() {
        return codigo;
    }
}
