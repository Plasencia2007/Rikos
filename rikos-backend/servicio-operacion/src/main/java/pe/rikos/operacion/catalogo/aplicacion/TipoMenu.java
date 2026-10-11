package pe.rikos.operacion.catalogo.aplicacion;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * US-014. Las reglas fijas del almuerzo, escritas en un solo lugar:
 *
 * - CA1: la composicion (entrada + segundo con guarnicion + refresco +
 *   postre). Los codigos son los de categoria_producto.componente_menu,
 *   que es lo que la programacion semanal (US-013) usa por opcion. El
 *   extra tiene la misma composicion; lo que cambia es que su segundo sale
 *   de las opciones marcadas como extra.
 * - RN-023: el extra se cobra completo, sin la tarifa reducida del plan
 *   del pensionista. La consulta el sprint 5.
 *
 * Es codigo y no una tabla a proposito: la especificacion no pide
 * cambiarlo, y una tabla se puede dejar incompleta.
 */
public enum TipoMenu {

    NORMAL("normal", true),
    EXTRA("extra", false);

    private static final List<String> COMPONENTES = List.of("entrada", "segundo", "guarnicion", "refresco", "postre");

    private final String codigo;
    private final boolean admiteTarifaReducida;

    TipoMenu(String codigo, boolean admiteTarifaReducida) {
        this.codigo = codigo;
        this.admiteTarifaReducida = admiteTarifaReducida;
    }

    /** El valor de producto.tipo_menu. */
    public String codigo() {
        return codigo;
    }

    public List<String> componentes() {
        return COMPONENTES;
    }

    /** RN-023. */
    public boolean admiteTarifaReducida() {
        return admiteTarifaReducida;
    }

    public static Optional<TipoMenu> desde(String codigo) {
        return Arrays.stream(values()).filter(t -> t.codigo.equals(codigo)).findFirst();
    }
}
