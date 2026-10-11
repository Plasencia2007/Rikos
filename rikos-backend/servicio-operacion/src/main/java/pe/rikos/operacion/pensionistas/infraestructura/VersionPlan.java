package pe.rikos.operacion.pensionistas.infraestructura;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;
import java.time.OffsetDateTime;

/**
 * Una version de los precios de un plan (RN-021). Una vez que rigio, no se
 * reescribe: se cierra (vigenteHasta) y se abre otra. La unica excepcion es
 * corregir, el mismo dia de inicio, una version que todavia no rigio o que
 * empezo hoy (CambiarPreciosPlan). Precios en centimos; null = sin precio.
 */
@Entity
@Table(name = "plan_tarifario_version")
public class VersionPlan {

    @Id
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(length = 26)
    private String id;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "plan_id", nullable = false, length = 26)
    private String planId;

    @Column(name = "precio_desayuno")
    private Long precioDesayuno;

    @Column(name = "precio_almuerzo")
    private Long precioAlmuerzo;

    @Column(name = "precio_cena")
    private Long precioCena;

    @Column(name = "vigente_desde", nullable = false)
    private LocalDate vigenteDesde;

    @Column(name = "vigente_hasta")
    private LocalDate vigenteHasta;

    @Column(name = "creado_en", nullable = false)
    private OffsetDateTime creadoEn;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "creado_por", length = 26)
    private String creadoPor;

    protected VersionPlan() {
    }

    public VersionPlan(String id, String planId, Long precioDesayuno, Long precioAlmuerzo, Long precioCena,
                       LocalDate vigenteDesde, String creadoPor) {
        this.id = id;
        this.planId = planId;
        this.precioDesayuno = precioDesayuno;
        this.precioAlmuerzo = precioAlmuerzo;
        this.precioCena = precioCena;
        this.vigenteDesde = vigenteDesde;
        this.creadoEn = OffsetDateTime.now();
        this.creadoPor = creadoPor;
    }

    public void cerrar(LocalDate ultimoDia) {
        this.vigenteHasta = ultimoDia;
    }

    public void corregirPrecios(Long precioDesayuno, Long precioAlmuerzo, Long precioCena) {
        this.precioDesayuno = precioDesayuno;
        this.precioAlmuerzo = precioAlmuerzo;
        this.precioCena = precioCena;
    }

    public boolean rigeEn(LocalDate fecha) {
        return !fecha.isBefore(vigenteDesde) && (vigenteHasta == null || !fecha.isAfter(vigenteHasta));
    }

    public String getId() {
        return id;
    }

    public String getPlanId() {
        return planId;
    }

    public Long getPrecioDesayuno() {
        return precioDesayuno;
    }

    public Long getPrecioAlmuerzo() {
        return precioAlmuerzo;
    }

    public Long getPrecioCena() {
        return precioCena;
    }

    public LocalDate getVigenteDesde() {
        return vigenteDesde;
    }

    public LocalDate getVigenteHasta() {
        return vigenteHasta;
    }

    public OffsetDateTime getCreadoEn() {
        return creadoEn;
    }
}
