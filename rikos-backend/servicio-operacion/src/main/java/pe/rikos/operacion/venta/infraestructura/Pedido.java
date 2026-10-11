package pe.rikos.operacion.venta.infraestructura;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;
import java.time.OffsetDateTime;

/**
 * 8.2: un pedido de un local. Sin subtotal, total ni cargo de taper: se derivan al leer las lineas
 * (sprint 6, 4.1). Los estados de este sprint son abierto, comandado y pendiente_cierre.
 */
@Entity
@Table(name = "pedido")
public class Pedido {

    @Id
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(length = 26)
    private String id;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "local_id", nullable = false, length = 26)
    private String localId;

    @Column(nullable = false, length = 20)
    private String numero;

    @Column(name = "fecha_operativa", nullable = false)
    private LocalDate fechaOperativa;

    @Column(nullable = false, length = 12)
    private String canal;

    @Column(nullable = false, length = 10)
    private String servicio;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "mesa_id", length = 26)
    private String mesaId;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "mozo_id", length = 26)
    private String mozoId;

    @Column(nullable = false, length = 20)
    private String estado;

    @Column(name = "abierto_en", nullable = false)
    private OffsetDateTime abiertoEn;

    @Column(name = "nombre_cliente", length = 120)
    private String nombreCliente;

    @Column(name = "cerrado_en")
    private OffsetDateTime cerradoEn;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "cerrado_por", length = 26)
    private String cerradoPor;

    @Version
    @Column(nullable = false)
    private int version;

    protected Pedido() {
    }

    public Pedido(String id, String localId, String numero, LocalDate fechaOperativa, String canal, String servicio,
                  String mesaId, String mozoId) {
        this.id = id;
        this.localId = localId;
        this.numero = numero;
        this.fechaOperativa = fechaOperativa;
        this.canal = canal;
        this.servicio = servicio;
        this.mesaId = mesaId;
        this.mozoId = mozoId;
        this.estado = "abierto";
        this.abiertoEn = OffsetDateTime.now();
    }

    /** "Terminar pedido": cierre SIN cobro (provisional hasta el sprint 7, que lo completa con el cobro). */
    public void terminar(String actorId) {
        this.estado = "cerrado";
        this.cerradoEn = OffsetDateTime.now();
        this.cerradoPor = actorId;
    }

    public OffsetDateTime getCerradoEn() {
        return cerradoEn;
    }

    public String getCerradoPor() {
        return cerradoPor;
    }

    /** US-029: el pedido pasa a otra mesa (el caso de uso ya valido que esta libre). */
    public void moverAMesa(String mesaId) {
        this.mesaId = mesaId;
    }

    /** Opcional: a nombre de quien es el pedido, solo para reconocer la mesa ("Familia Garcia"). */
    public void nombrarCliente(String nombre) {
        this.nombreCliente = nombre;
    }

    public String getNombreCliente() {
        return nombreCliente;
    }

    /** El pedido ya mando algo a cocina. Seguir agregando lineas esta permitido: saldra otra comanda. */
    public void comandar() {
        this.estado = "comandado";
    }

    public String getId() {
        return id;
    }

    public String getLocalId() {
        return localId;
    }

    public String getNumero() {
        return numero;
    }

    public LocalDate getFechaOperativa() {
        return fechaOperativa;
    }

    public String getCanal() {
        return canal;
    }

    public String getServicio() {
        return servicio;
    }

    public String getMesaId() {
        return mesaId;
    }

    public String getMozoId() {
        return mozoId;
    }

    public String getEstado() {
        return estado;
    }

    public OffsetDateTime getAbiertoEn() {
        return abiertoEn;
    }
}
