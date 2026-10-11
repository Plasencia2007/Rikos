package pe.rikos.operacion.catalogo.aplicacion;

/** US-013: programacion semanal del almuerzo. */
public enum ErrorProgramacion {
    NO_ENCONTRADO,
    /** La semana empieza siempre un lunes. */
    NO_ES_LUNES,
    SEMANA_YA_EXISTE,
    /** No se crea una semana que ya termino. */
    SEMANA_PASADA,
    /** La fecha no pertenece a la semana. */
    FECHA_FUERA_DE_SEMANA,
    /** Los dias anteriores a hoy son historia de lo que se sirvio. */
    DIA_CERRADO,
    PRODUCTO_NO_ENCONTRADO,
    /** Solo productos activos de una categoria de componente de menu (entrada, segundo...). */
    PRODUCTO_NO_ES_DEL_MENU,
    TIPO_MENU_INVALIDO,
    /** El menu extra solo cambia el segundo. */
    EXTRA_SOLO_EN_SEGUNDO,
    /** El mismo producto ya esta programado ese dia. */
    OPCION_REPETIDA,
    PORCIONES_INVALIDAS,
    YA_CONFIRMADA,
    SEMANA_VACIA,
    /** Una semana confirmada no se borra: el POS ya pudo usarla. */
    SEMANA_CONFIRMADA
}
