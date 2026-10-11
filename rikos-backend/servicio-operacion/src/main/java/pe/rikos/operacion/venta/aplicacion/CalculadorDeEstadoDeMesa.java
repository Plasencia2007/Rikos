package pe.rikos.operacion.venta.aplicacion;

/**
 * Deriva el estado de una mesa, con esta prioridad: bloqueada, unida,
 * pendiente de cierre, ocupada, libre. Funcion pura: quien la llama le dice
 * que pedidos hay. Una mesa solo esta OCUPADA cuando su pedido se COMANDO (cambio de regla, 2026-10-01); un pedido
 * solo abierto la deja libre (el mapa la marca "tomando pedido").
 */
public final class CalculadorDeEstadoDeMesa {

    private CalculadorDeEstadoDeMesa() {
    }

    public static EstadoDeMesa calcular(boolean bloqueada, boolean unida, boolean pedidoPendienteDeCierre,
                                        boolean pedidoComandado) {
        if (bloqueada) {
            return EstadoDeMesa.BLOQUEADA;
        }
        if (unida) {
            return EstadoDeMesa.UNIDA;
        }
        if (pedidoPendienteDeCierre) {
            return EstadoDeMesa.PENDIENTE_CIERRE;
        }
        if (pedidoComandado) {
            return EstadoDeMesa.OCUPADA;
        }
        return EstadoDeMesa.LIBRE;
    }
}
