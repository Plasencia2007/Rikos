package pe.rikos.operacion.venta.aplicacion;

/** US-028: pedidos, personas y lineas. */
public enum ErrorPedido {
    /** No existe, o es de otro local (no se distingue a proposito). */
    NO_ENCONTRADO,
    SIN_LOCAL_ACTIVO,
    CANAL_INVALIDO,
    MESA_REQUERIDA,
    MESA_NO_ENCONTRADA,
    /** Bloqueada, unida a otra o desactivada. */
    MESA_NO_DISPONIBLE,
    /** La mesa ya tiene un pedido en curso. */
    MESA_OCUPADA,
    /** Fuera de todo horario de servicio: el mozo elige uno. */
    SERVICIO_REQUERIDO,
    SERVICIO_INVALIDO,
    PERSONA_NO_ENCONTRADA,
    PENSIONISTA_NO_ENCONTRADO,
    PENSIONISTA_INACTIVO,
    PENSIONISTA_YA_EN_EL_PEDIDO,
    /** Asignar un pensionista, o quitar a la persona, cuando ya tiene lineas. */
    PERSONA_CON_LINEAS,
    PERSONA_NO_ES_PENSIONISTA,
    PRODUCTO_NO_ENCONTRADO,
    PRODUCTO_NO_VENDIBLE,
    /** Agotado en el local de la sesion. */
    PRODUCTO_AGOTADO,
    /** Ni el producto ni el motor dan un precio: nunca entra una linea con precio cero. */
    SIN_PRECIO,
    /** El plan del pensionista no tiene precio para esa comida. */
    PLAN_SIN_PRECIO,
    MENU_NO_PROGRAMADO,
    /** El menu del dia solo se pide en el almuerzo. */
    MENU_FUERA_DE_SERVICIO,
    COMPONENTE_OBLIGATORIO,
    OPCION_INVALIDA,
    /** El desayuno del plan solo en un pedido de desayuno; la cena del plan, en uno de cena. */
    PLAN_FUERA_DE_SERVICIO,
    CANTIDAD_INVALIDA,
    DESCRIPCION_OBLIGATORIA,
    PRECIO_INVALIDO,
    OBSERVACION_LARGA,
    LINEA_NO_ENCONTRADA,
    /** Una linea que ya salio a cocina no se quita ni se modifica aqui: se anula (entrega 3). */
    LINEA_YA_COMANDADA,
    /** Comandar sin nada nuevo que mandar a cocina. */
    SIN_LINEAS_PENDIENTES,
    /** El pedido ya no esta en curso (pendiente de cierre o cerrado). */
    PEDIDO_NO_COMANDABLE,
    COMANDA_NO_ENCONTRADA,
    LINEA_ANULADA,
    MOTIVO_NO_ENCONTRADO,
    /** Inactivo o de otro tipo que "anulacion". */
    MOTIVO_INVALIDO,
    /** El motivo elegido exige una observacion. */
    OBSERVACION_OBLIGATORIA,
    /** Al abrir: los comensales van de 1 a 30. */
    COMENSALES_INVALIDOS,
    /** Al abrir: el nombre del cliente pasa de 120 caracteres. */
    NOMBRE_DE_CLIENTE_LARGO,
    /** US-029: el pedido ya no esta en curso (pendiente de cierre o cerrado): no se reorganiza. */
    PEDIDO_NO_REORGANIZABLE,
    SIN_LINEAS_PARA_MOVER,
    /** Sprint 6b: un filtro de la lista de pedidos no es valido (situacion, canal, estado, fechas o pagina). */
    FILTRO_INVALIDO,
    /** Todas las lineas ya estan en el destino (o la mesa destino es la misma). */
    MISMO_DESTINO,
    /** La comida del plan solo puede estar en la cuenta de un pensionista. */
    SOLO_PARA_PENSIONISTA,
    /** La mesa del pedido tiene mesas unidas: se separan antes de mover el pedido. */
    TIENE_MESAS_UNIDAS,
    /** Terminar un pedido que ya no esta en curso (ya cerrado). */
    PEDIDO_NO_TERMINABLE,
    /** Un pedido cerrado no se modifica: ni lineas, ni personas, ni anulaciones. */
    PEDIDO_CERRADO,
    /** Sprint 7: una linea ya cobrada no se modifica, ni se anula, ni se traslada (la reapertura es de caja). */
    LINEA_COBRADA,
    /** Sprint 7: "Terminar pedido" solo cierra un pedido sin nada que cobrar; con lineas comandadas se cobran o se anulan. */
    PEDIDO_CON_LINEAS_POR_COBRAR,
    /** Sprint 8 (US-020): errores al vincular pre-ticket de autoservicio a una persona del pedido. */
    PRE_TICKET_REQUERIDO,
    PRE_TICKET_NO_ENCONTRADO,
    PRE_TICKET_DE_OTRO_LOCAL,
    PRE_TICKET_SERVICIO_DISTINTO,
    PRE_TICKET_VENCIDO,
    PRE_TICKET_YA_VINCULADO,
    PRE_TICKET_ANULADO,
    PRE_TICKET_CERRADO
}
