package pe.rikos.operacion.identidad.infraestructura;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Duration;
import java.time.OffsetDateTime;

@Entity
@Table(name = "usuario")
public class Usuario {

    @Id
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(length = 26)
    private String id;

    @Column(name = "nombre_usuario", nullable = false, length = 60)
    private String nombreUsuario;

    @Column(nullable = false, length = 120)
    private String nombres;

    @Column(nullable = false, length = 120)
    private String apellidos;

    @Column(length = 8)
    private String dni;

    @Column(length = 160)
    private String correo;

    @Column(length = 20)
    private String telefono;

    @Column(name = "hash_contrasena", nullable = false, length = 255)
    private String hashContrasena;

    @Column(name = "hash_pin", length = 255)
    private String hashPin;

    @Column(name = "intentos_fallidos", nullable = false)
    private int intentosFallidos;

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

    protected Usuario() {
    }

    public Usuario(String id, String nombreUsuario, String nombres, String apellidos, String dni, String correo,
                    String telefono, String hashContrasena, String creadoPor) {
        this.id = id;
        this.nombreUsuario = nombreUsuario;
        this.nombres = nombres;
        this.apellidos = apellidos;
        this.dni = dni;
        this.correo = correo;
        this.telefono = telefono;
        this.hashContrasena = hashContrasena;
        this.activo = true;
        this.creadoEn = OffsetDateTime.now();
        this.creadoPor = creadoPor;
    }

    public void actualizarDatos(String nombres, String apellidos, String dni, String correo, String telefono,
                                 String actualizadoPor) {
        this.nombres = nombres;
        this.apellidos = apellidos;
        this.dni = dni;
        this.correo = correo;
        this.telefono = telefono;
        this.actualizadoEn = OffsetDateTime.now();
        this.actualizadoPor = actualizadoPor;
    }

    public void asignarPin(String hashPin, String actualizadoPor) {
        this.hashPin = hashPin;
        this.actualizadoEn = OffsetDateTime.now();
        this.actualizadoPor = actualizadoPor;
    }

    public void desactivar(String actualizadoPor) {
        this.activo = false;
        this.actualizadoEn = OffsetDateTime.now();
        this.actualizadoPor = actualizadoPor;
    }

    public void activar(String actualizadoPor) {
        this.activo = true;
        this.actualizadoEn = OffsetDateTime.now();
        this.actualizadoPor = actualizadoPor;
    }

    /**
     * RN-091 / US-003 CA3: el umbral y la duracion del bloqueo son
     * configuracion (RF-006 sigue pendiente de definicion), nunca constantes.
     */
    public void registrarIntentoFallido(int umbralIntentos, Duration duracionBloqueo) {
        this.intentosFallidos++;
        if (this.intentosFallidos >= umbralIntentos) {
            this.bloqueadoHasta = OffsetDateTime.now().plus(duracionBloqueo);
        }
    }

    public void registrarIntentoExitoso() {
        this.intentosFallidos = 0;
        this.bloqueadoHasta = null;
    }

    public boolean estaBloqueado() {
        return bloqueadoHasta != null && bloqueadoHasta.isAfter(OffsetDateTime.now());
    }

    /** Para mostrar en pantalla cuanto falta; 0 si no esta bloqueado. */
    public long segundosRestantesBloqueo() {
        if (!estaBloqueado()) {
            return 0;
        }
        return Duration.between(OffsetDateTime.now(), bloqueadoHasta).getSeconds();
    }

    public String getId() {
        return id;
    }

    public String getNombreUsuario() {
        return nombreUsuario;
    }

    public String getNombres() {
        return nombres;
    }

    public String getApellidos() {
        return apellidos;
    }

    public String getDni() {
        return dni;
    }

    public String getCorreo() {
        return correo;
    }

    public String getTelefono() {
        return telefono;
    }

    public String getHashContrasena() {
        return hashContrasena;
    }

    public String getHashPin() {
        return hashPin;
    }

    public int getIntentosFallidos() {
        return intentosFallidos;
    }

    public OffsetDateTime getBloqueadoHasta() {
        return bloqueadoHasta;
    }

    public boolean isActivo() {
        return activo;
    }

    public int getVersion() {
        return version;
    }
}
