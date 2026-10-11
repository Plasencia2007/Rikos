package pe.rikos.operacion.catalogo.aplicacion;

public enum ErrorCatalogo {
    NOMBRE_DUPLICADO,
    NO_ENCONTRADO,
    CATEGORIA_PADRE_NO_ENCONTRADA,
    CATEGORIA_NO_ES_HOJA,
    NATURALEZA_INVALIDA,
    PADRE_NO_ES_RAIZ,
    PADRE_TIENE_PRODUCTOS,
    CATEGORIA_TIENE_SUBCATEGORIAS,
    CATEGORIA_TIENE_PRODUCTOS,
    CODIGO_DUPLICADO,
    CODIGO_FORMATO_INVALIDO,
    UNIDAD_OBLIGATORIA,
    PRECIO_VENTA_OBLIGATORIO,
    FACTOR_INVALIDO,
    PAR_UNIDADES_DUPLICADO,
    PREDETERMINADA_DUPLICADA,
    RUC_INVALIDO,
    RUC_DUPLICADO,
    TIENE_HISTORIAL,
    COSTO_YA_CONFIRMADO_POR_COMPRA,
    /** US-014: el menu normal y el extra no se borran, no se desactivan, y su precio y usos se cambian solo desde el menu. */
    PRODUCTO_DE_MENU,
    /** US-014: la categoria del menu solo tiene los dos productos del sistema. */
    CATEGORIA_RESERVADA
}
