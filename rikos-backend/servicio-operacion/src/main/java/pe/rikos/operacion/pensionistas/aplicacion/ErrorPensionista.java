package pe.rikos.operacion.pensionistas.aplicacion;

/** US-016: ficha del pensionista. */
public enum ErrorPensionista {
    NO_ENCONTRADO,
    /** El DNI no tiene exactamente ocho digitos. */
    DNI_INVALIDO,
    /** Otro pensionista de la empresa ya tiene ese DNI (RN-036). */
    DNI_DUPLICADO,
    NOMBRES_OBLIGATORIOS,
    PLAN_NO_ENCONTRADO,
    /** Al menos una comida habitual: con ellas se calcula el tope. */
    SIN_COMIDAS,
    FRECUENCIA_INVALIDA,
    /** El responsable es obligatorio (US-016 CA2). */
    RESPONSABLE_OBLIGATORIO,
    /** Falta alguno de sus datos: nombre, telefono, correo, parentesco o direccion. */
    RESPONSABLE_INCOMPLETO,
    RESPONSABLE_DNI_INVALIDO
}
