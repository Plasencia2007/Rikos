package pe.rikos.operacion.catalogo.aplicacion;

/**
 * US-010. A diferencia de ErrorCatalogo, varios de estos errores son sobre
 * UN ingrediente de la lista -- FalloReceta lleva cual, para que la
 * pantalla marque la fila en vez de decir "algo fallo en la receta".
 */
public enum ErrorReceta {
    NO_ENCONTRADO,
    PRODUCTO_NO_ENCONTRADO,
    /** Solo un producto vendible o una subreceta tiene receta (CA1). */
    PRODUCTO_NO_ADMITE_RECETA,
    RECETA_YA_EXISTE,
    SIN_INGREDIENTES,
    RENDIMIENTO_INVALIDO,
    PORCIONES_INVALIDAS,
    CANTIDAD_INVALIDA,
    /** El ingrediente no es insumo ni subreceta, o esta desactivado. */
    INGREDIENTE_INVALIDO,
    INGREDIENTE_REPETIDO,
    UNIDAD_NO_ENCONTRADA,
    /** La unidad del ingrediente no es su unidad base y no hay conversion_unidad para ese producto. */
    SIN_CONVERSION,
    /** §5.2: la receta se contendria a si misma, directa o indirectamente. */
    CICLO
}
