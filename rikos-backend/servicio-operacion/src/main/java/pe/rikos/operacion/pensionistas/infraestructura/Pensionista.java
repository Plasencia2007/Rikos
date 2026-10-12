package pe.rikos.operacion.pensionistas.infraestructura;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import pe.rikos.operacion.pensionistas.aplicacion.Comida;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.EnumSet;
import java.util.Set;

/**
 * 7.3 (US-016). El DNI es unico en toda la empresa (RN-036). El PIN y la
 * contrasena del portal son credenciales distintas y se guardan cifradas
 * (RN-090): ninguna se puede leer de vuelta. limiteCredito nulo = el tope se
 * calcula (entrega 4); con valor = fijado a mano.
 */
@Entity
@Table(name = "pensionista")
public class Pensionista {

    @Id
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(length = 26)
    private String id;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(nullable = false, length = 8)
    private String dni;

    @Column(nullable = false, length = 120)
    private String nombres;

    @Column(nullable = false, length = 120)
    private String apellidos;

    @Column(length = 20)
    private String telefono;

    @Column(length = 160)
    private String correo;

    @Column(length = 150)
    private String institucion;

    @Column(name = "codigo_estudiante", length = 30)
    private String codigoEstudiante;

    @Column(length = 120)
    private String carrera;

    @Column(length = 20)
    private String ciclo;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "plan_tarifario_id", nullable = false, length = 26)
    private String planTarifarioId;

    @Column(name = "come_desayuno", nullable = false)
    private boolean comeDesayuno;

    @Column(name = "come_almuerzo", nullable = false)
    private boolean comeAlmuerzo;

    @Column(name = "come_cena", nullable = false)
    private boolean comeCena;

    @Column(name = "frecuencia_abono", length = 10)
    private String frecuenciaAbono;

    @Column(name = "limite_credito")
    private Long limiteCredito;

    @Column(name = "hash_pin", nullable = false, length = 255)
    private String hashPin;

    @Column(name = "hash_contrasena_portal", length = 255)
    private String hashContrasenaPortal;

    @Column(name = "intentos_fallidos", nullable = false)
    private int intentosFallidos = 0;

    @Column(name = "bloqueado_hasta")
    private OffsetDateTime bloqueadoHasta;

    @Column(nullable = false)
    private boolean activo = true;

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

    protected Pensionista() {
    }

    public Pensionista(String id, String hashPin, String creadoPor) {
        this.id = id;
        this.hashPin = hashPin;
        this.creadoEn = OffsetDateTime.now();
        this.creadoPor = creadoPor;
    }

    /** Los datos que edita la ficha (todo salvo credenciales y estado). */
    public record Ficha(String dni, String nombres, String apellidos, String telefono, String correo,
                        String institucion, String codigoEstudiante, String carrera, String ciclo,
                        String planTarifarioId, Set<Comida> comidas, String frecuenciaAbono) {
    }

    public void actualizarFicha(Ficha f, String actorId) {
        this.dni = f.dni();
        this.nombres = f.nombres();
        this.apellidos = f.apellidos();
        this.telefono = f.telefono();
        this.correo = f.correo();
        this.institucion = f.institucion();
        this.codigoEstudiante = f.codigoEstudiante();
        this.carrera = f.carrera();
        this.ciclo = f.ciclo();
        this.planTarifarioId = f.planTarifarioId();
        this.comeDesayuno = f.comidas().contains(Comida.DESAYUNO);
        this.comeAlmuerzo = f.comidas().contains(Comida.ALMUERZO);
        this.comeCena = f.comidas().contains(Comida.CENA);
        this.frecuenciaAbono = f.frecuenciaAbono();
        tocar(actorId);
    }

    public void cambiarPin(String hashPin, String actorId) {
        this.hashPin = hashPin;
        tocar(actorId);
    }

    public void cambiarEstado(boolean activo, String actorId) {
        this.activo = activo;
        tocar(actorId);
    }

    /** Nulo = el tope se calcula; con valor = fijado a mano. */
    public void fijarLimite(Long limite, String actorId) {
        this.limiteCredito = limite;
        tocar(actorId);
    }

    private void tocar(String actorId) {
        this.actualizadoEn = OffsetDateTime.now();
        this.actualizadoPor = actorId;
    }

    /** Las comidas con las que se calcula el tope de credito (no limitan lo que puede consumir). */
    public Set<Comida> getComidas() {
        Set<Comida> comidas = EnumSet.noneOf(Comida.class);
        if (comeDesayuno) comidas.add(Comida.DESAYUNO);
        if (comeAlmuerzo) comidas.add(Comida.ALMUERZO);
        if (comeCena) comidas.add(Comida.CENA);
        return comidas;
    }

    public String nombreCompleto() {
        return nombres + " " + apellidos;
    }

    public String getId() {
        return id;
    }

    public String getDni() {
        return dni;
    }

    public String getNombres() {
        return nombres;
    }

    public String getApellidos() {
        return apellidos;
    }

    public String getTelefono() {
        return telefono;
    }

    public String getCorreo() {
        return correo;
    }

    public String getInstitucion() {
        return institucion;
    }

    public String getCodigoEstudiante() {
        return codigoEstudiante;
    }

    public String getCarrera() {
        return carrera;
    }

    public String getCiclo() {
        return ciclo;
    }

    public String getPlanTarifarioId() {
        return planTarifarioId;
    }

    public String getFrecuenciaAbono() {
        return frecuenciaAbono;
    }

    public Long getLimiteCredito() {
        return limiteCredito;
    }

    public String getHashPin() {
        return hashPin;
    }

    public String getHashContrasenaPortal() {
        return hashContrasenaPortal;
    }

    public boolean isActivo() {
        return activo;
    }

    public OffsetDateTime getCreadoEn() {
        return creadoEn;
    }

    public void registrarIntentoFallido(int umbralIntentos, Duration duracionBloqueo) {
        registrarIntentoFallido(umbralIntentos, duracionBloqueo, OffsetDateTime.now());
    }

    public void registrarIntentoFallido(int umbralIntentos, Duration duracionBloqueo, OffsetDateTime momento) {
        this.intentosFallidos++;
        if (this.intentosFallidos >= umbralIntentos) {
            this.bloqueadoHasta = momento.plus(duracionBloqueo);
        }
    }

    public void registrarIntentoExitoso() {
        this.intentosFallidos = 0;
        this.bloqueadoHasta = null;
    }

    public boolean estaBloqueado() {
        return estaBloqueado(OffsetDateTime.now());
    }

    public boolean estaBloqueado(OffsetDateTime momento) {
        return bloqueadoHasta != null && bloqueadoHasta.isAfter(momento);
    }

    public long segundosRestantesBloqueo() {
        return segundosRestantesBloqueo(OffsetDateTime.now());
    }

    public long segundosRestantesBloqueo(OffsetDateTime momento) {
        if (!estaBloqueado(momento)) {
            return 0;
        }
        return Math.max(0, Duration.between(momento, bloqueadoHasta).toSeconds());
    }

    public String getNombreCompleto() {
        String n = nombres != null ? nombres.trim() : "";
        String a = apellidos != null ? apellidos.trim() : "";
        return (n + " " + a).trim();
    }

    public int getIntentosFallidos() {
        return intentosFallidos;
    }

    public OffsetDateTime getBloqueadoHasta() {
        return bloqueadoHasta;
    }
}
