package pe.rikos.operacion.pensionistas.aplicacion;

/** Los tipos de movimiento del libro. Los codigos son los del CHECK de movimiento_cuenta. */
public enum TipoMovimiento {

    CONSUMO("consumo"),
    ABONO("abono"),
    AJUSTE("ajuste"),
    /** Del cierre mensual (sprint 13). */
    ARRASTRE("arrastre"),
    /** Lo que el pensionista debe o tiene a favor al empezar en el sistema. */
    SALDO_INICIAL("saldo_inicial");

    private final String codigo;

    TipoMovimiento(String codigo) {
        this.codigo = codigo;
    }

    public String codigo() {
        return codigo;
    }
}
