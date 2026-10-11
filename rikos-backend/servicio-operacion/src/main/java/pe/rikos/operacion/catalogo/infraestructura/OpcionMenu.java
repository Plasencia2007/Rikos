package pe.rikos.operacion.catalogo.infraestructura;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * 6.3 (US-013). componente se deriva de la categoria del producto al
 * programar y se guarda: la semana pasada sigue diciendo lo que dijo aunque
 * despues se reclasifique el plato. tipoMenu 'extra' solo en segundos.
 */
@Entity
@Table(name = "opcion_menu")
public class OpcionMenu {

    @Id
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(length = 26)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JdbcTypeCode(SqlTypes.CHAR)
    @JoinColumn(name = "dia_menu_id", nullable = false)
    private DiaMenu dia;

    @ManyToOne(fetch = FetchType.LAZY)
    @JdbcTypeCode(SqlTypes.CHAR)
    @JoinColumn(name = "producto_id", nullable = false)
    private Producto producto;

    @Column(nullable = false, length = 20)
    private String componente;

    @Column(name = "tipo_menu", nullable = false, length = 10)
    private String tipoMenu;

    @Column(name = "porciones_estimadas")
    private Integer porcionesEstimadas;

    @Column(nullable = false)
    private boolean activo = true;

    protected OpcionMenu() {
    }

    public OpcionMenu(String id, DiaMenu dia, Producto producto, String componente, String tipoMenu,
                      Integer porcionesEstimadas) {
        this.id = id;
        this.dia = dia;
        this.producto = producto;
        this.componente = componente;
        this.tipoMenu = tipoMenu;
        this.porcionesEstimadas = porcionesEstimadas;
    }

    public void actualizar(String tipoMenu, Integer porcionesEstimadas) {
        this.tipoMenu = tipoMenu;
        this.porcionesEstimadas = porcionesEstimadas;
    }

    public String getId() {
        return id;
    }

    public DiaMenu getDia() {
        return dia;
    }

    public Producto getProducto() {
        return producto;
    }

    public String getComponente() {
        return componente;
    }

    public String getTipoMenu() {
        return tipoMenu;
    }

    public boolean isActivo() {
        return activo;
    }

    public Integer getPorcionesEstimadas() {
        return porcionesEstimadas;
    }
}
