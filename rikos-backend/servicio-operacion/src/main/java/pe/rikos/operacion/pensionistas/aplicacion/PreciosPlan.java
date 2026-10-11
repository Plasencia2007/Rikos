package pe.rikos.operacion.pensionistas.aplicacion;

import pe.rikos.operacion.pensionistas.infraestructura.VersionPlan;

/** Los tres precios de un plan, en centimos. null = todavia sin precio (el motor de precios no inventa uno). */
public record PreciosPlan(Long desayuno, Long almuerzo, Long cena) {

    public static PreciosPlan de(VersionPlan v) {
        return new PreciosPlan(v.getPrecioDesayuno(), v.getPrecioAlmuerzo(), v.getPrecioCena());
    }

    /** null si son validos; si no, el error. Cada precio, si existe, es mayor que cero. */
    ErrorPlan validar() {
        for (Long precio : new Long[] {desayuno, almuerzo, cena}) {
            if (precio != null && precio <= 0) {
                return ErrorPlan.PRECIO_INVALIDO;
            }
        }
        return null;
    }
}
