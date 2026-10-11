package pe.rikos.operacion.catalogo.infraestructura;

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
 * 6.1 (US-013). Solo guarda 'borrador' o 'confirmado': "en servicio" y
 * "pasada" se calculan por fecha (estadoEn), nunca se guardan -- un proceso
 * que cambie el estado a medianoche puede no correr si el nodo del local se
 * reinicia por un corte de luz (DT-039).
 */
@Entity
@Table(name = "semana_menu")
public class SemanaMenu {

    public static final String BORRADOR = "borrador";
    public static final String CONFIRMADO = "confirmado";

    @Id
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(length = 26)
    private String id;

    @Column(name = "fecha_inicio", nullable = false)
    private LocalDate fechaInicio;

    @Column(name = "fecha_fin", nullable = false)
    private LocalDate fechaFin;

    @Column(nullable = false, length = 12)
    private String estado = BORRADOR;

    @Column(name = "confirmada_en")
    private OffsetDateTime confirmadaEn;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "confirmada_por", length = 26)
    private String confirmadaPor;

    @Version
    @Column(nullable = false)
    private int version;

    @Column(name = "creado_en", nullable = false)
    private OffsetDateTime creadoEn;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "creado_por", length = 26)
    private String creadoPor;

    protected SemanaMenu() {
    }

    /** fechaInicio debe ser lunes: lo valida CrearSemanaMenu, y el CHECK de V22 por si acaso. */
    public SemanaMenu(String id, LocalDate fechaInicio, String creadoPor) {
        this.id = id;
        this.fechaInicio = fechaInicio;
        this.fechaFin = fechaInicio.plusDays(6);
        this.creadoEn = OffsetDateTime.now();
        this.creadoPor = creadoPor;
    }

    public void confirmar(String actorId) {
        this.estado = CONFIRMADO;
        this.confirmadaEn = OffsetDateTime.now();
        this.confirmadaPor = actorId;
    }

    public boolean isConfirmada() {
        return CONFIRMADO.equals(estado);
    }

    public boolean contiene(LocalDate fecha) {
        return !fecha.isBefore(fechaInicio) && !fecha.isAfter(fechaFin);
    }

    /**
     * El estado que ve la gente: 'borrador', 'confirmado', 'en_servicio'
     * (confirmada y hoy cae dentro) o 'pasada' (ya termino, en cualquier
     * estado guardado).
     */
    public String estadoEn(LocalDate hoy) {
        if (fechaFin.isBefore(hoy)) {
            return "pasada";
        }
        if (isConfirmada() && contiene(hoy)) {
            return "en_servicio";
        }
        return estado;
    }

    public String getId() {
        return id;
    }

    public LocalDate getFechaInicio() {
        return fechaInicio;
    }

    public LocalDate getFechaFin() {
        return fechaFin;
    }

    public String getEstado() {
        return estado;
    }

    public OffsetDateTime getConfirmadaEn() {
        return confirmadaEn;
    }
}
