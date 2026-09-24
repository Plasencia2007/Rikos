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

import java.time.OffsetDateTime;

/**
 * 4.1: arbol de dos niveles con autorreferencia. naturaleza y
 * componente_menu solo se fijan en la raiz (nivel 1); una subcategoria los
 * hereda sin poder contradecir a su padre -- el caso de uso valida eso,
 * no esta entidad.
 */
@Entity
@Table(name = "categoria_producto")
public class CategoriaProducto {

    @Id
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(length = 26)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JdbcTypeCode(SqlTypes.CHAR)
    @JoinColumn(name = "categoria_padre_id")
    private CategoriaProducto categoriaPadre;

    @Column(nullable = false, length = 80)
    private String nombre;

    @Column(nullable = false, length = 20)
    private String naturaleza;

    @Column(name = "componente_menu", length = 20)
    private String componenteMenu;

    @Column(nullable = false)
    private int nivel;

    @Column(nullable = false)
    private int orden;

    @Column(nullable = false)
    private boolean activo = true;

    @Column(name = "creado_en", nullable = false)
    private OffsetDateTime creadoEn;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "creado_por", length = 26)
    private String creadoPor;

    @Column(name = "actualizado_en")
    private OffsetDateTime actualizadoEn;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "actualizado_por", length = 26)
    private String actualizadoPor;

    protected CategoriaProducto() {
    }

    /** Raiz: categoriaPadre null, naturaleza y componenteMenu propios. */
    public CategoriaProducto(String id, String nombre, String naturaleza, String componenteMenu, int orden,
                              String creadoPor) {
        this.id = id;
        this.categoriaPadre = null;
        this.nombre = nombre;
        this.naturaleza = naturaleza;
        this.componenteMenu = componenteMenu;
        this.nivel = 1;
        this.orden = orden;
        this.creadoEn = OffsetDateTime.now();
        this.creadoPor = creadoPor;
    }

    /** Subcategoria: hereda naturaleza y componenteMenu del padre. */
    public CategoriaProducto(String id, CategoriaProducto padre, String nombre, int orden, String creadoPor) {
        this.id = id;
        this.categoriaPadre = padre;
        this.nombre = nombre;
        this.naturaleza = padre.naturaleza;
        this.componenteMenu = padre.componenteMenu;
        this.nivel = 2;
        this.orden = orden;
        this.creadoEn = OffsetDateTime.now();
        this.creadoPor = creadoPor;
    }

    public void actualizar(String nombre, int orden, String actualizadoPor) {
        this.nombre = nombre;
        this.orden = orden;
        this.actualizadoEn = OffsetDateTime.now();
        this.actualizadoPor = actualizadoPor;
    }

    public void desactivar(String actualizadoPor) {
        this.activo = false;
        this.actualizadoEn = OffsetDateTime.now();
        this.actualizadoPor = actualizadoPor;
    }

    public String getId() {
        return id;
    }

    public CategoriaProducto getCategoriaPadre() {
        return categoriaPadre;
    }

    public String getNombre() {
        return nombre;
    }

    public String getNaturaleza() {
        return naturaleza;
    }

    public String getComponenteMenu() {
        return componenteMenu;
    }

    public int getNivel() {
        return nivel;
    }

    public int getOrden() {
        return orden;
    }

    public boolean isActivo() {
        return activo;
    }
}
