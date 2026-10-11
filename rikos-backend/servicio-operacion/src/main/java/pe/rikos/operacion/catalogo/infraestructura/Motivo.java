package pe.rikos.operacion.catalogo.infraestructura;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * US-012: motivos configurables (ajuste de inventario, anulacion, descuento,
 * rechazo de abono, sobrante de produccion, reapertura). Tabla sin sembrado
 * inicial a proposito -- RF-005 (los valores concretos) todavia no esta
 * definido por el dueno del negocio; esta entidad solo da el CRUD.
 *
 * No tiene columnas de auditoria de actor (creado_por/actualizado_por): el
 * esquema de motivo (V13) no las define, a diferencia de otras entidades de
 * catalogo -- por eso activar/desactivar no reciben actorId, solo togglean
 * el campo.
 */
@Entity
@Table(name = "motivo")
public class Motivo {

    @Id
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(length = 26)
    private String id;

    @Column(nullable = false, length = 30)
    private String tipo;

    @Column(nullable = false, length = 120)
    private String descripcion;

    @Column(name = "requiere_observacion", nullable = false)
    private boolean requiereObservacion;

    @Column(nullable = false)
    private boolean activo = true;

    protected Motivo() {
    }

    public Motivo(String id, String tipo, String descripcion, boolean requiereObservacion) {
        this.id = id;
        this.tipo = tipo;
        this.descripcion = descripcion;
        this.requiereObservacion = requiereObservacion;
        this.activo = true;
    }

    public void actualizar(String descripcion, boolean requiereObservacion) {
        this.descripcion = descripcion;
        this.requiereObservacion = requiereObservacion;
    }

    public void activar() {
        this.activo = true;
    }

    public void desactivar() {
        this.activo = false;
    }

    public String getId() {
        return id;
    }

    public String getTipo() {
        return tipo;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public boolean isRequiereObservacion() {
        return requiereObservacion;
    }

    public boolean isActivo() {
        return activo;
    }
}
