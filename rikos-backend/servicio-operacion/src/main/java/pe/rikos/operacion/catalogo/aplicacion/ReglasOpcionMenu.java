package pe.rikos.operacion.catalogo.aplicacion;

/** Validaciones comunes a agregar y editar una opcion (US-013). */
final class ReglasOpcionMenu {

    private ReglasOpcionMenu() {
    }

    /** null si todo esta bien. */
    static ErrorProgramacion validar(String componente, String tipoMenu, Integer porcionesEstimadas) {
        if (TipoMenu.desde(tipoMenu).isEmpty()) {
            return ErrorProgramacion.TIPO_MENU_INVALIDO;
        }
        // El extra solo cambia el segundo (US-014): la entrada, el refresco y
        // el postre son los del dia para los dos menus.
        if (TipoMenu.EXTRA.codigo().equals(tipoMenu) && !"segundo".equals(componente)) {
            return ErrorProgramacion.EXTRA_SOLO_EN_SEGUNDO;
        }
        if (porcionesEstimadas != null && porcionesEstimadas <= 0) {
            return ErrorProgramacion.PORCIONES_INVALIDAS;
        }
        return null;
    }
}
