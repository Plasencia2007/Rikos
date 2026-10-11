package pe.rikos.operacion.venta.infraestructura;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;

/**
 * Un salon de un local: la zona donde estan sus mesas (terraza, salon principal, barra...). Guarda el
 * tamano y el color de piso del plano donde se ubican esas mesas.
 */
@Entity
@Table(name = "salon")
public class Salon {

    public static final int ANCHO_PLANO_POR_DEFECTO = 1200;
    public static final int ALTO_PLANO_POR_DEFECTO = 800;
    public static final String COLOR_PISO_POR_DEFECTO = "#F3EEDF";

    @Id
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(length = 26)
    private String id;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "local_id", nullable = false, length = 26)
    private String localId;

    @Column(nullable = false, length = 60)
    private String nombre;

    @Column(length = 250)
    private String descripcion;

    @Column(nullable = false, length = 20)
    private String icono;

    @Column(nullable = false, length = 20)
    private String color;

    @Column(nullable = false)
    private boolean activo = true;

    @Column(name = "ancho_plano", nullable = false)
    private int anchoPlano;

    @Column(name = "alto_plano", nullable = false)
    private int altoPlano;

    @Column(name = "color_piso", nullable = false, length = 7)
    private String colorPiso;

    @Version
    @Column(nullable = false)
    private int version;

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

    protected Salon() {
    }

    public Salon(String id, String localId, String nombre, String descripcion, String icono, String color,
                 int anchoPlano, int altoPlano, String colorPiso, String creadoPor) {
        this.id = id;
        this.localId = localId;
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.icono = icono;
        this.color = color;
        this.anchoPlano = anchoPlano;
        this.altoPlano = altoPlano;
        this.colorPiso = colorPiso;
        this.creadoEn = OffsetDateTime.now();
        this.creadoPor = creadoPor;
    }

    public void actualizar(String nombre, String descripcion, String icono, String color, int anchoPlano,
                           int altoPlano, String colorPiso, String actorId) {
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.icono = icono;
        this.color = color;
        this.anchoPlano = anchoPlano;
        this.altoPlano = altoPlano;
        this.colorPiso = colorPiso;
        tocar(actorId);
    }

    public void cambiarEstado(boolean activo, String actorId) {
        this.activo = activo;
        tocar(actorId);
    }

    private void tocar(String actorId) {
        this.actualizadoEn = OffsetDateTime.now();
        this.actualizadoPor = actorId;
    }

    public String getId() {
        return id;
    }

    public String getLocalId() {
        return localId;
    }

    public String getNombre() {
        return nombre;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public String getIcono() {
        return icono;
    }

    public String getColor() {
        return color;
    }

    public boolean isActivo() {
        return activo;
    }

    public int getAnchoPlano() {
        return anchoPlano;
    }

    public int getAltoPlano() {
        return altoPlano;
    }

    public String getColorPiso() {
        return colorPiso;
    }
}
