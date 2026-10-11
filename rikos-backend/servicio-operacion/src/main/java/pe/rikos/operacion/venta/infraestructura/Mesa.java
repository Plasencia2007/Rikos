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
 * 8.1: una mesa de un local. Guarda solo datos propios; el estado (libre,
 * ocupada, unida, pendiente de cierre, bloqueada) se deriva al consultar.
 */
@Entity
@Table(name = "mesa")
public class Mesa {

    @Id
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(length = 26)
    private String id;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "local_id", nullable = false, length = 26)
    private String localId;

    @Column(nullable = false, length = 10)
    private String numero;

    @Column(length = 60)
    private String nombre;

    private Integer capacidad;

    @Column(nullable = false)
    private boolean bloqueada;

    @Column(name = "motivo_bloqueo", length = 250)
    private String motivoBloqueo;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "bloqueada_por", length = 26)
    private String bloqueadaPor;

    @Column(name = "bloqueada_en")
    private OffsetDateTime bloqueadaEn;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "mesa_principal_id", length = 26)
    private String mesaPrincipalId;

    @Column(nullable = false)
    private boolean activo = true;

    /** El salon donde esta. Null solo en una fila insertada por fuera de CrearMesa (que siempre asigna uno). */
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "salon_id", length = 26)
    private String salonId;

    /** cuadrada | redonda | rectangular. */
    @Column(nullable = false, length = 12)
    private String forma = "cuadrada";

    @Column(nullable = false)
    private boolean vip;

    @Column(name = "zona_exterior", nullable = false)
    private boolean zonaExterior;

    @Column(name = "zona_fumadores", nullable = false)
    private boolean zonaFumadores;

    /** Lugar en el plano de su salon (esquina superior izquierda, en pixeles). */
    @Column(name = "pos_x")
    private Integer posX;

    @Column(name = "pos_y")
    private Integer posY;

    @Column(nullable = false)
    private int ancho = 84;

    @Column(nullable = false)
    private int alto = 84;

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

    protected Mesa() {
    }

    public Mesa(String id, String localId, String numero, String nombre, Integer capacidad, String creadoPor) {
        this.id = id;
        this.localId = localId;
        this.numero = numero;
        this.nombre = nombre;
        this.capacidad = capacidad;
        this.creadoEn = OffsetDateTime.now();
        this.creadoPor = creadoPor;
    }

    public void actualizar(String numero, String nombre, Integer capacidad, String actorId) {
        this.numero = numero;
        this.nombre = nombre;
        this.capacidad = capacidad;
        tocar(actorId);
    }

    /** Salon, forma y atributos de la mesa. El caso de uso ya los valido. */
    public void configurar(String salonId, String forma, boolean vip, boolean zonaExterior, boolean zonaFumadores,
                           int ancho, int alto, String actorId) {
        this.salonId = salonId;
        this.forma = forma;
        this.vip = vip;
        this.zonaExterior = zonaExterior;
        this.zonaFumadores = zonaFumadores;
        this.ancho = ancho;
        this.alto = alto;
        tocar(actorId);
    }

    /** Donde queda la mesa en el plano de su salon y cuanto mide. */
    public void ubicar(int x, int y, int ancho, int alto, String actorId) {
        this.posX = x;
        this.posY = y;
        this.ancho = ancho;
        this.alto = alto;
        tocar(actorId);
    }

    public void cambiarEstado(boolean activo, String actorId) {
        this.activo = activo;
        tocar(actorId);
    }

    /** El motivo ya viene validado (no vacio) por el caso de uso. */
    public void bloquear(String motivo, String actorId) {
        this.bloqueada = true;
        this.motivoBloqueo = motivo;
        this.bloqueadaPor = actorId;
        this.bloqueadaEn = OffsetDateTime.now();
        tocar(actorId);
    }

    public void liberar(String actorId) {
        this.bloqueada = false;
        this.motivoBloqueo = null;
        this.bloqueadaPor = null;
        this.bloqueadaEn = null;
        tocar(actorId);
    }

    /** La mesa se une a una principal: el pedido de la principal se opera por las dos. */
    public void unirA(String principalId, String actorId) {
        this.mesaPrincipalId = principalId;
        tocar(actorId);
    }

    /** Deja de estar unida: vuelve a ser una mesa independiente. */
    public void separar(String actorId) {
        this.mesaPrincipalId = null;
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

    public String getNumero() {
        return numero;
    }

    public String getNombre() {
        return nombre;
    }

    public Integer getCapacidad() {
        return capacidad;
    }

    public boolean isBloqueada() {
        return bloqueada;
    }

    public String getMotivoBloqueo() {
        return motivoBloqueo;
    }

    public String getBloqueadaPor() {
        return bloqueadaPor;
    }

    public OffsetDateTime getBloqueadaEn() {
        return bloqueadaEn;
    }

    public String getMesaPrincipalId() {
        return mesaPrincipalId;
    }

    public boolean isActivo() {
        return activo;
    }

    public String getSalonId() {
        return salonId;
    }

    public String getForma() {
        return forma;
    }

    public boolean isVip() {
        return vip;
    }

    public boolean isZonaExterior() {
        return zonaExterior;
    }

    public boolean isZonaFumadores() {
        return zonaFumadores;
    }

    public Integer getPosX() {
        return posX;
    }

    public Integer getPosY() {
        return posY;
    }

    public int getAncho() {
        return ancho;
    }

    public int getAlto() {
        return alto;
    }
}
