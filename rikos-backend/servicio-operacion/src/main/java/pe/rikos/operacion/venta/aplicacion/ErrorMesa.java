package pe.rikos.operacion.venta.aplicacion;

/** US-027: mesas y su bloqueo. */
public enum ErrorMesa {
    /** No existe, o es de otro local (no se distingue a proposito). */
    NO_ENCONTRADA,
    SIN_LOCAL_ACTIVO,
    /** Vacio o de mas de 10 caracteres. */
    NUMERO_INVALIDO,
    NUMERO_DUPLICADO,
    /** Si se da, es un entero mayor que cero. */
    CAPACIDAD_INVALIDA,
    /** Bloquear pide un motivo que no este en blanco. */
    MOTIVO_OBLIGATORIO,
    YA_BLOQUEADA,
    NO_BLOQUEADA,
    /** Una mesa desactivada no se bloquea. */
    MESA_INACTIVA,
    /** Solo una mesa libre se bloquea (con pedido en curso o unida, no); tampoco se desactiva una con pedido. */
    MESA_NO_LIBRE,
    /** El salon no existe o es de otro local. */
    SALON_NO_ENCONTRADO,
    /** Solo cuadrada, redonda o rectangular. */
    FORMA_INVALIDA,
    /** Ancho o alto que no es un entero mayor que cero. */
    MEDIDAS_INVALIDAS,
    /** US-029: la principal no sirve como tal (inactiva, bloqueada, unida a otra o la misma mesa). */
    PRINCIPAL_INVALIDA,
    YA_UNIDA,
    NO_ESTA_UNIDA,
    /** Una principal con mesas unidas no se bloquea, no se desactiva ni pasa a ser secundaria. */
    TIENE_MESAS_UNIDAS,
    /** La mesa tiene un pedido abierto sin comandar: primero hay que terminarlo (no se bloquea, desactiva ni une como secundaria). */
    TIENE_PEDIDO_ABIERTO
}
