# Sprint 6, entrega 1 — Mesas y mapa (US-027) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Que un mozo vea el mapa de mesas de su local con los cinco estados y pueda bloquear y liberar mesas con motivo, y que un administrador cree y edite las mesas de su local.

**Architecture:** Módulo nuevo `venta` (`pe.rikos.operacion.venta`, capas `api` / `aplicacion` / `infraestructura`). La mesa guarda solo datos propios (número, nombre, capacidad, activa, bloqueo con motivo, mesa principal); el **estado se deriva** en una función pura (`CalculadorDeEstadoDeMesa`) a partir de esos datos y de si hay pedido abierto o pendiente de cierre (en esta entrega aún no hay pedidos: ambos van en `false`). El local de la sesión sale del `SecurityContext`, nunca de la petición.

**Tech Stack:** Spring Boot 4.1.1, Spring Modulith 2.1.1, Hibernate 7, Flyway, PostgreSQL; React 19 + TypeScript + Vite.

**Spec:** `docs/producto/plan/sprint-06-vender-en-mesa.md` (secciones 3.2, 3.10, 4.1 y 5, entrega 1). Modelo: `docs/producto/modelo-de-datos/06-venta-pos-comandas--ep05.md` § 8.1.

## Global Constraints

- **`ULID` de 26 caracteres** para todo identificador: `Identificador.nuevo().valor()`. Ni autoincrement ni UUID.
- **`Resultado<T, E>`** (sealed) para todo fallo esperado, con un `enum` de error propio del caso de uso. No lanzar excepciones para errores de negocio.
- **Flyway manda:** migración nueva `V29__mesas.sql`; nunca se edita una ya aplicada; nunca `DROP TABLE`.
- **TDD real:** la prueba se escribe antes que el código.
- **`RegistradorAuditoria` resuelve usuario y local solo**; un test de un caso de uso auditado necesita `ContextoDeSeguridadDePrueba.autenticarComo(...)` en `@BeforeEach` y `limpiar()` en `@AfterEach`.
- **Spring Modulith** verifica límites: `ArquitecturaDeModulosTest` solo puede fallar por el ciclo conocido `comun ↔ identidad`; cualquier otra línea es un error mío.
- **`./mvnw spring-boot:run` desde el directorio del módulo**, nunca con `-pl`.
- **Frontend:** `npx tsc -b --force`, no `tsc --noEmit`. `npm run generar-tipos` necesita el backend en el 8081.
- **Nunca commitear** salvo que el usuario lo pida (regla del proyecto: los pasos "Commit" de este plan **no se ejecutan** sin ese pedido).
- **Texto de la interfaz en español**, con los cuatro estados (cargando, vacío, error, con datos).
- Comandos de prueba: `cd rikos-backend && ./mvnw -q -pl servicio-operacion -am test -Dtest=<Clase> -Dsurefire.failIfNoSpecifiedTests=false`; los resultados se leen de `servicio-operacion/target/surefire-reports/*.txt`.

## Review Focus

- Un mozo del **local A** nunca ve, edita ni bloquea una mesa del **local B** (debe dar "no encontrada", sin filtrar que existe).
- Dos mesas con el **mismo número en el mismo local** se rechazan; el mismo número en **locales distintos** se acepta.
- Bloquear una mesa **ya bloqueada**, liberar una que **no lo está**, o bloquear **sin motivo** (o con motivo en blanco) se rechazan con su código.
- Una mesa **inactiva** no aparece en el mapa (sí en la pantalla de administración) y no se puede bloquear.
- Una sesión **sin local activo** no puede crear ni bloquear ni ver el mapa.
- Un rol sin el permiso (cocina, motorizado; mozo para administrar mesas) recibe **403** en cada endpoint nuevo.

Cada línea tiene su prueba en la tarea que dueña el código (ver tareas 3, 4 y 5).

---

## File Structure

**Backend (`rikos-backend/servicio-operacion/src/main`)**
- `resources/db/migration/V29__mesas.sql` — tabla `mesa`, eventos de bitácora nuevos.
- `java/pe/rikos/operacion/venta/infraestructura/Mesa.java`, `MesaRepositorio.java` — entidad y acceso.
- `java/pe/rikos/operacion/venta/aplicacion/EstadoDeMesa.java`, `CalculadorDeEstadoDeMesa.java` — los cinco estados y su cálculo puro.
- `.../venta/aplicacion/ErrorMesa.java`, `LocalDeSesion.java`, `DatosMesa.java`.
- `.../venta/aplicacion/CrearMesa.java`, `ActualizarMesa.java`, `CambiarEstadoMesa.java`, `BloquearMesa.java`, `LiberarMesa.java`, `ConsultaMesas.java`.
- `.../venta/api/MesaController.java`.
- Modifica `identidad/aplicacion/SembradorCatalogoPermisos.java` (permisos y roles), `comun/seguridad/ConfiguracionSeguridad.java` (matchers), `identidad/aplicacion/ListarAuditoria.java` (frases).

**Tests (`src/test/java/pe/rikos/operacion/venta/...`)**
- `aplicacion/CalculadorDeEstadoDeMesaTest.java`, `aplicacion/GestionDeMesasTest.java`, `aplicacion/BloqueoDeMesasTest.java`, `aplicacion/MapaDeMesasTest.java`, `api/MesasAccesoPorRolTest.java`; extiende `identidad/aplicacion/ListarAuditoriaTest.java`.

**Frontend (`rikos-web/src`)**
- `api/cliente.ts` — tipos y funciones de mesas.
- `features/venta/PantallaMesas.tsx` (admin), `features/venta/MapaDeMesas.tsx` (POS), `features/venta/index.ts`.
- Modifica `apps/admin/index.tsx`, `apps/admin/BarraLateral.tsx`, `apps/pos/index.tsx`, `estilos/global.css`, `features/configuracion/...` no.

---

### Task 1: Los cinco estados y su cálculo puro

**Files:**
- Create: `servicio-operacion/src/main/java/pe/rikos/operacion/venta/aplicacion/EstadoDeMesa.java`
- Create: `servicio-operacion/src/main/java/pe/rikos/operacion/venta/aplicacion/CalculadorDeEstadoDeMesa.java`
- Test: `servicio-operacion/src/test/java/pe/rikos/operacion/venta/aplicacion/CalculadorDeEstadoDeMesaTest.java`

**Interfaces:**
- Produces: `enum EstadoDeMesa { LIBRE, OCUPADA, UNIDA, PENDIENTE_CIERRE, BLOQUEADA; String codigo() }` con códigos `libre`, `ocupada`, `unida`, `pendiente_cierre`, `bloqueada`.
- Produces: `CalculadorDeEstadoDeMesa.calcular(boolean bloqueada, boolean unida, boolean pedidoPendienteDeCierre, boolean pedidoAbierto) → EstadoDeMesa`. Prioridad: bloqueada, unida, pendiente de cierre, ocupada, libre.

- [ ] **Step 1: Write the failing test**

```java
package pe.rikos.operacion.venta.aplicacion;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** Los cinco estados de la mesa (RN-059) y su prioridad: bloqueada, unida, pendiente de cierre, ocupada, libre. */
class CalculadorDeEstadoDeMesaTest {

    private static EstadoDeMesa estado(boolean bloqueada, boolean unida, boolean pendienteCierre, boolean abierto) {
        return CalculadorDeEstadoDeMesa.calcular(bloqueada, unida, pendienteCierre, abierto);
    }

    @Test
    void sin_nada_esta_libre() {
        assertThat(estado(false, false, false, false)).isEqualTo(EstadoDeMesa.LIBRE);
    }

    @Test
    void con_un_pedido_abierto_esta_ocupada() {
        assertThat(estado(false, false, false, true)).isEqualTo(EstadoDeMesa.OCUPADA);
    }

    @Test
    void con_un_pedido_pendiente_de_cierre_esta_pendiente_de_cierre_aunque_tambien_haya_uno_abierto() {
        assertThat(estado(false, false, true, false)).isEqualTo(EstadoDeMesa.PENDIENTE_CIERRE);
        assertThat(estado(false, false, true, true)).isEqualTo(EstadoDeMesa.PENDIENTE_CIERRE);
    }

    @Test
    void unida_gana_sobre_pendiente_de_cierre_y_ocupada() {
        assertThat(estado(false, true, false, false)).isEqualTo(EstadoDeMesa.UNIDA);
        assertThat(estado(false, true, true, true)).isEqualTo(EstadoDeMesa.UNIDA);
    }

    @Test
    void bloqueada_gana_sobre_todo() {
        assertThat(estado(true, false, false, false)).isEqualTo(EstadoDeMesa.BLOQUEADA);
        assertThat(estado(true, true, true, true)).isEqualTo(EstadoDeMesa.BLOQUEADA);
    }

    @Test
    void los_codigos_son_los_del_modelo() {
        assertThat(EstadoDeMesa.values()).extracting(EstadoDeMesa::codigo)
                .containsExactly("libre", "ocupada", "unida", "pendiente_cierre", "bloqueada");
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd rikos-backend && ./mvnw -q -pl servicio-operacion -am test -Dtest=CalculadorDeEstadoDeMesaTest -Dsurefire.failIfNoSpecifiedTests=false`
Expected: compilation FAIL, `EstadoDeMesa` and `CalculadorDeEstadoDeMesa` do not exist.

- [ ] **Step 3: Write minimal implementation**

`EstadoDeMesa.java`:
```java
package pe.rikos.operacion.venta.aplicacion;

/**
 * RN-059: los cinco estados de una mesa. No se guardan: se derivan al
 * consultar (sprint 6, 3.2). El orden de las constantes es el del modelo.
 */
public enum EstadoDeMesa {

    LIBRE("libre"),
    OCUPADA("ocupada"),
    UNIDA("unida"),
    PENDIENTE_CIERRE("pendiente_cierre"),
    BLOQUEADA("bloqueada");

    private final String codigo;

    EstadoDeMesa(String codigo) {
        this.codigo = codigo;
    }

    public String codigo() {
        return codigo;
    }
}
```

`CalculadorDeEstadoDeMesa.java`:
```java
package pe.rikos.operacion.venta.aplicacion;

/**
 * Deriva el estado de una mesa, con esta prioridad: bloqueada, unida,
 * pendiente de cierre, ocupada, libre. Funcion pura: quien la llama le dice
 * que pedidos hay (en la entrega 1 todavia no hay pedidos: pasa false, false).
 */
public final class CalculadorDeEstadoDeMesa {

    private CalculadorDeEstadoDeMesa() {
    }

    public static EstadoDeMesa calcular(boolean bloqueada, boolean unida, boolean pedidoPendienteDeCierre,
                                        boolean pedidoAbierto) {
        if (bloqueada) {
            return EstadoDeMesa.BLOQUEADA;
        }
        if (unida) {
            return EstadoDeMesa.UNIDA;
        }
        if (pedidoPendienteDeCierre) {
            return EstadoDeMesa.PENDIENTE_CIERRE;
        }
        if (pedidoAbierto) {
            return EstadoDeMesa.OCUPADA;
        }
        return EstadoDeMesa.LIBRE;
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: same command as Step 2. Expected: `Tests run: 6, Failures: 0, Errors: 0` in `target/surefire-reports/*CalculadorDeEstadoDeMesaTest.txt`.

- [ ] **Step 5: Commit** (solo si el usuario lo pide)

```bash
git add servicio-operacion/src/main/java/pe/rikos/operacion/venta servicio-operacion/src/test/java/pe/rikos/operacion/venta
git commit -m "feat(venta): los cinco estados de la mesa y su calculo puro"
```

---

### Task 2: La tabla `mesa`, la entidad y los permisos nuevos

**Files:**
- Create: `servicio-operacion/src/main/resources/db/migration/V29__mesas.sql`
- Create: `.../venta/infraestructura/Mesa.java`, `.../venta/infraestructura/MesaRepositorio.java`
- Create: `.../venta/aplicacion/ErrorMesa.java`, `.../venta/aplicacion/LocalDeSesion.java`
- Modify: `.../identidad/aplicacion/SembradorCatalogoPermisos.java`
- Test: `.../venta/aplicacion/GestionDeMesasTest.java` (solo la parte de persistencia en esta tarea; el resto en la 3)

**Interfaces:**
- Produces: `Mesa` con constructor `Mesa(String id, String localId, String numero, String nombre, Integer capacidad, String creadoPor)` y métodos `actualizar(String numero, String nombre, Integer capacidad, String actorId)`, `cambiarEstado(boolean activo, String actorId)`, `bloquear(String motivo, String actorId)`, `liberar(String actorId)`, getters `getId, getLocalId, getNumero, getNombre, getCapacidad, isBloqueada, getMotivoBloqueo, getBloqueadaPor, getBloqueadaEn, getMesaPrincipalId, isActivo`.
- Produces: `MesaRepositorio extends JpaRepository<Mesa, String>`: `List<Mesa> findByLocalIdOrderByNumero(String localId)`, `Optional<Mesa> findByIdAndLocalId(String id, String localId)`, `boolean existsByLocalIdAndNumero(String localId, String numero)`, `boolean existsByLocalIdAndNumeroAndIdNot(String localId, String numero, String id)`.
- Produces: `enum ErrorMesa { NO_ENCONTRADA, SIN_LOCAL_ACTIVO, NUMERO_INVALIDO, NUMERO_DUPLICADO, CAPACIDAD_INVALIDA, MOTIVO_OBLIGATORIO, YA_BLOQUEADA, NO_BLOQUEADA, MESA_INACTIVA, MESA_NO_LIBRE }`.
- Produces: `LocalDeSesion.actual() → Optional<String>` (paquete, no público).
- Produces: permisos `venta.bloquear_mesa` y `venta.administrar`, sembrados en roles.

- [ ] **Step 1: Write the failing test**

Crear `GestionDeMesasTest.java` con el arranque y la primera prueba (más se agregan en la tarea 3):

```java
package pe.rikos.operacion.venta.aplicacion;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;
import jakarta.persistence.EntityManager;
import pe.rikos.contratos.Identificador;
import pe.rikos.operacion.comun.auditoria.ContextoDeSeguridadDePrueba;
import pe.rikos.operacion.identidad.aplicacion.CrearLocal;
import pe.rikos.operacion.venta.infraestructura.Mesa;
import pe.rikos.operacion.venta.infraestructura.MesaRepositorio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** US-027: crear y editar las mesas de un local. El numero es unico dentro del local. */
@SpringBootTest
@Transactional
class GestionDeMesasTest {

    static final String ADMIN_SEMBRADO_ID = "01M2QZ606763Q7XC00M8VD7WM1";

    @Autowired
    MesaRepositorio mesas;
    @Autowired
    CrearLocal crearLocal;
    @Autowired
    EntityManager em;

    String localA;
    String localB;

    @BeforeEach
    void preparar() {
        ContextoDeSeguridadDePrueba.autenticarComo(ADMIN_SEMBRADO_ID);
        localA = local("ma");
        localB = local("mb");
        ContextoDeSeguridadDePrueba.autenticarComo(ADMIN_SEMBRADO_ID, localA);
    }

    @AfterEach
    void limpiarContexto() {
        ContextoDeSeguridadDePrueba.limpiar();
    }

    String local(String prefijo) {
        String codigo = prefijo + Identificador.nuevo().valor().substring(20).toLowerCase();
        return crearLocal.ejecutar(codigo, "Local " + codigo, "Direccion", ADMIN_SEMBRADO_ID).valorOLanza().getId();
    }

    @Test
    void la_base_rechaza_dos_mesas_con_el_mismo_numero_en_un_local_pero_lo_permite_en_otro() {
        mesas.saveAndFlush(new Mesa(Identificador.nuevo().valor(), localA, "5", null, 4, ADMIN_SEMBRADO_ID));
        mesas.saveAndFlush(new Mesa(Identificador.nuevo().valor(), localB, "5", null, 4, ADMIN_SEMBRADO_ID));

        assertThatThrownBy(() -> mesas.saveAndFlush(
                new Mesa(Identificador.nuevo().valor(), localA, "5", null, 2, ADMIN_SEMBRADO_ID)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void la_base_rechaza_una_mesa_bloqueada_sin_motivo() {
        Mesa mesa = mesas.saveAndFlush(new Mesa(Identificador.nuevo().valor(), localA, "6", null, null,
                ADMIN_SEMBRADO_ID));
        em.clear();

        assertThatThrownBy(() -> jdbc("update mesa set bloqueada = true where id = '" + mesa.getId() + "'"))
                .hasMessageContaining("mesa_bloqueada_con_motivo");
    }

    private void jdbc(String sql) {
        em.createNativeQuery(sql).executeUpdate();
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./mvnw -q -pl servicio-operacion -am test -Dtest=GestionDeMesasTest -Dsurefire.failIfNoSpecifiedTests=false`
Expected: compilation FAIL, `Mesa` and `MesaRepositorio` do not exist.

- [ ] **Step 3: Write minimal implementation**

`V29__mesas.sql`:
```sql
-- Sprint 6, entrega 1 (US-027): las mesas de cada local.
-- Decisiones en docs/producto/plan/sprint-06-vender-en-mesa.md (3.2 y 4.1).
--
-- Difiere del modelo original: NO hay columna "estado". Libre, ocupada, unida y
-- pendiente de cierre se derivan al consultar (de los pedidos y de
-- mesa_principal_id); solo "bloqueada" es un dato propio de la mesa, con su
-- motivo, quien y cuando.
CREATE TABLE mesa (
    id                 CHAR(26) PRIMARY KEY,
    local_id           CHAR(26) NOT NULL REFERENCES local(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    numero             VARCHAR(10) NOT NULL,
    nombre             VARCHAR(60),
    capacidad          INTEGER,
    bloqueada          BOOLEAN NOT NULL DEFAULT FALSE,
    motivo_bloqueo     VARCHAR(250),
    bloqueada_por      CHAR(26) REFERENCES usuario(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    bloqueada_en       TIMESTAMPTZ,
    -- Si esta unida, la mesa que encabeza la union (se usa desde la entrega 4).
    mesa_principal_id  CHAR(26) REFERENCES mesa(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    activo             BOOLEAN NOT NULL DEFAULT TRUE,
    version            INTEGER NOT NULL DEFAULT 0,
    creado_en          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    creado_por         CHAR(26) REFERENCES usuario(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    actualizado_en     TIMESTAMPTZ,
    actualizado_por    CHAR(26) REFERENCES usuario(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT mesa_numero_unico_por_local UNIQUE (local_id, numero),
    CONSTRAINT mesa_capacidad_positiva CHECK (capacidad IS NULL OR capacidad > 0),
    CONSTRAINT mesa_bloqueada_con_motivo CHECK (NOT bloqueada OR (motivo_bloqueo IS NOT NULL AND btrim(motivo_bloqueo) <> '')),
    CONSTRAINT mesa_no_se_une_a_si_misma CHECK (mesa_principal_id IS NULL OR mesa_principal_id <> id)
);

CREATE INDEX ix_mesa_local ON mesa(local_id, numero);

ALTER TABLE auditoria DROP CONSTRAINT auditoria_evento_check;
ALTER TABLE auditoria ADD CONSTRAINT auditoria_evento_check
    CHECK (evento IN ('descuento', 'anulacion', 'reapertura', 'ajuste_inventario', 'transferencia',
                       'cambio_receta', 'cambio_precio', 'cambio_permiso', 'aprobacion_abono',
                       'rechazo_abono', 'cierre_caja', 'marcado_agotado', 'cambio_plan',
                       'cambio_estado', 'eliminacion', 'acceso_denegado', 'reactivacion',
                       'cambio_parametro', 'cambio_credencial', 'movimiento_cuenta', 'cambio_limite',
                       'cambio_mesa', 'bloqueo_mesa'));
```

`Mesa.java`:
```java
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
}
```

`MesaRepositorio.java`:
```java
package pe.rikos.operacion.venta.infraestructura;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MesaRepositorio extends JpaRepository<Mesa, String> {

    List<Mesa> findByLocalIdOrderByNumero(String localId);

    /** Una mesa de otro local no se encuentra: nunca se filtra que existe. */
    Optional<Mesa> findByIdAndLocalId(String id, String localId);

    boolean existsByLocalIdAndNumero(String localId, String numero);

    boolean existsByLocalIdAndNumeroAndIdNot(String localId, String numero, String id);
}
```

`ErrorMesa.java`:
```java
package pe.rikos.operacion.venta.aplicacion;

/** US-027: mesas y su bloqueo. */
public enum ErrorMesa {
    /** No existe, o es de otro local (no se distingue a proposito). */
    NO_ENCONTRADA,
    SIN_LOCAL_ACTIVO,
    /** Vacio o de mas de 10 caracteres. */
    NUMERO_INVALIDO,
    NUMERO_DUPLICADO,
    /** Si se da, es un entero mayor que cero. */
    CAPACIDAD_INVALIDA,
    /** Bloquear pide un motivo que no este en blanco. */
    MOTIVO_OBLIGATORIO,
    YA_BLOQUEADA,
    NO_BLOQUEADA,
    /** Una mesa desactivada no se bloquea. */
    MESA_INACTIVA,
    /** Solo una mesa libre se bloquea (con pedido abierto o unida, no). */
    MESA_NO_LIBRE
}
```

`LocalDeSesion.java`:
```java
package pe.rikos.operacion.venta.aplicacion;

import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

/**
 * El local activo de la sesion: FiltroAutenticacionJwt lo deja en los
 * "details" de la autenticacion (DT-047). El local nunca viaja como dato de la
 * peticion. (Misma idea que catalogo.LocalActivo y pensionistas.LocalDeSesion.)
 */
final class LocalDeSesion {

    private LocalDeSesion() {
    }

    static Optional<String> actual() {
        var autenticacion = SecurityContextHolder.getContext().getAuthentication();
        if (autenticacion == null || autenticacion.getDetails() == null) {
            return Optional.empty();
        }
        String valor = String.valueOf(autenticacion.getDetails());
        return valor.isBlank() ? Optional.empty() : Optional.of(valor);
    }
}
```

`SembradorCatalogoPermisos.java` — agregar al `CATALOGO`, junto a `venta.reabrir`:
```java
        new DefPermiso("venta", "bloquear_mesa", "Bloquear y liberar mesas"),
        new DefPermiso("venta", "administrar", "Crear y editar las mesas de un local"),
```
y en `PERMISOS_POR_ROL`: al **administrador de local** agregar `"venta.bloquear_mesa", "venta.administrar"`; al **cajero** agregar `"venta.bloquear_mesa"`; al **mozo** agregar `"venta.bloquear_mesa", "venta.modificar"` (queda `Set.of("catalogo.ver", "menu.ver", "venta.ver", "pensionistas.ver", "venta.crear", "venta.modificar", "venta.bloquear_mesa")`). El administrador central los recibe solo (tiene todo el catálogo).

- [ ] **Step 4: Run test to verify it passes**

Run: `./mvnw -q -pl servicio-operacion -am test -Dtest=GestionDeMesasTest,ArquitecturaDeModulosTest -Dsurefire.failIfNoSpecifiedTests=false`
Expected: `GestionDeMesasTest` `Tests run: 2, Failures: 0`. En `ArquitecturaDeModulosTest` **solo** la línea `- Cycle detected: Slice comun ->` (grep `^- ` del reporte).

- [ ] **Step 5: Commit** (solo si el usuario lo pide)

```bash
git add servicio-operacion/src
git commit -m "feat(venta): tabla mesa, entidad y permisos de mesas"
```

---

### Task 3: Crear, editar y activar mesas de mi local

**Files:**
- Create: `.../venta/aplicacion/DatosMesa.java`, `CrearMesa.java`, `ActualizarMesa.java`, `CambiarEstadoMesa.java`
- Modify: `servicio-operacion/src/test/java/pe/rikos/operacion/venta/aplicacion/GestionDeMesasTest.java`

**Interfaces:**
- Consumes: `MesaRepositorio`, `LocalDeSesion.actual()`, `ErrorMesa`, `RegistradorAuditoria.registrar(evento, entidad, id, anterior, nuevo)` y `RegistradorAuditoria.campos(...)`.
- Produces: `record DatosMesa(String numero, String nombre, Integer capacidad)`.
- Produces: `CrearMesa.ejecutar(DatosMesa datos, String actorId) → Resultado<Mesa, ErrorMesa>`; `ActualizarMesa.ejecutar(String id, DatosMesa datos, String actorId) → Resultado<Mesa, ErrorMesa>`; `CambiarEstadoMesa.ejecutar(String id, boolean activo, String actorId) → Resultado<Mesa, ErrorMesa>`. Todas actúan sobre el **local de la sesión**.

- [ ] **Step 1: Write the failing test**

Agregar al final de `GestionDeMesasTest` (antes del último `}`), con los `@Autowired` nuevos arriba de la clase:

```java
    @Autowired
    CrearMesa crearMesa;
    @Autowired
    ActualizarMesa actualizarMesa;
    @Autowired
    CambiarEstadoMesa cambiarEstadoMesa;

    private static ErrorMesa error(pe.rikos.contratos.Resultado<?, ErrorMesa> resultado) {
        assertThat(resultado.esExito()).isFalse();
        return ((pe.rikos.contratos.Resultado.Fracaso<?, ErrorMesa>) resultado).error();
    }

    @Test
    void crea_una_mesa_en_el_local_de_la_sesion_con_su_numero_nombre_y_capacidad() {
        Mesa mesa = crearMesa.ejecutar(new DatosMesa("  7 ", " Terraza 3 ", 6), ADMIN_SEMBRADO_ID).valorOLanza();

        assertThat(mesa.getLocalId()).isEqualTo(localA);
        assertThat(mesa.getNumero()).isEqualTo("7");
        assertThat(mesa.getNombre()).isEqualTo("Terraza 3");
        assertThat(mesa.getCapacidad()).isEqualTo(6);
        assertThat(mesa.isActivo()).isTrue();
        assertThat(mesa.isBloqueada()).isFalse();
    }

    @Test
    void rechaza_un_numero_vacio_muy_largo_o_repetido_en_el_mismo_local_pero_no_en_otro() {
        crearMesa.ejecutar(new DatosMesa("1", null, null), ADMIN_SEMBRADO_ID).valorOLanza();

        assertThat(error(crearMesa.ejecutar(new DatosMesa("   ", null, null), ADMIN_SEMBRADO_ID)))
                .isEqualTo(ErrorMesa.NUMERO_INVALIDO);
        assertThat(error(crearMesa.ejecutar(new DatosMesa("12345678901", null, null), ADMIN_SEMBRADO_ID)))
                .isEqualTo(ErrorMesa.NUMERO_INVALIDO);
        assertThat(error(crearMesa.ejecutar(new DatosMesa("1", null, null), ADMIN_SEMBRADO_ID)))
                .isEqualTo(ErrorMesa.NUMERO_DUPLICADO);

        ContextoDeSeguridadDePrueba.autenticarComo(ADMIN_SEMBRADO_ID, localB);
        assertThat(crearMesa.ejecutar(new DatosMesa("1", null, null), ADMIN_SEMBRADO_ID).esExito()).isTrue();
    }

    @Test
    void la_capacidad_si_se_da_es_mayor_que_cero() {
        for (int capacidad : new int[] {0, -3}) {
            assertThat(error(crearMesa.ejecutar(new DatosMesa("9", null, capacidad), ADMIN_SEMBRADO_ID)))
                    .isEqualTo(ErrorMesa.CAPACIDAD_INVALIDA);
        }
    }

    @Test
    void sin_local_activo_no_se_crea_nada() {
        ContextoDeSeguridadDePrueba.autenticarComo(ADMIN_SEMBRADO_ID);

        assertThat(error(crearMesa.ejecutar(new DatosMesa("2", null, null), ADMIN_SEMBRADO_ID)))
                .isEqualTo(ErrorMesa.SIN_LOCAL_ACTIVO);
    }

    @Test
    void edita_una_mesa_de_mi_local_y_no_puede_repetir_el_numero_de_otra() {
        Mesa uno = crearMesa.ejecutar(new DatosMesa("1", null, null), ADMIN_SEMBRADO_ID).valorOLanza();
        crearMesa.ejecutar(new DatosMesa("2", null, null), ADMIN_SEMBRADO_ID).valorOLanza();

        Mesa editada = actualizarMesa.ejecutar(uno.getId(), new DatosMesa("1", "Ventana", 4), ADMIN_SEMBRADO_ID)
                .valorOLanza();

        assertThat(editada.getNombre()).isEqualTo("Ventana");
        assertThat(editada.getCapacidad()).isEqualTo(4);
        assertThat(error(actualizarMesa.ejecutar(uno.getId(), new DatosMesa("2", null, null), ADMIN_SEMBRADO_ID)))
                .isEqualTo(ErrorMesa.NUMERO_DUPLICADO);
    }

    @Test
    void una_mesa_de_otro_local_no_se_encuentra_ni_se_edita_ni_se_desactiva() {
        ContextoDeSeguridadDePrueba.autenticarComo(ADMIN_SEMBRADO_ID, localB);
        Mesa deB = crearMesa.ejecutar(new DatosMesa("1", null, null), ADMIN_SEMBRADO_ID).valorOLanza();
        ContextoDeSeguridadDePrueba.autenticarComo(ADMIN_SEMBRADO_ID, localA);

        assertThat(error(actualizarMesa.ejecutar(deB.getId(), new DatosMesa("1", "X", null), ADMIN_SEMBRADO_ID)))
                .isEqualTo(ErrorMesa.NO_ENCONTRADA);
        assertThat(error(cambiarEstadoMesa.ejecutar(deB.getId(), false, ADMIN_SEMBRADO_ID)))
                .isEqualTo(ErrorMesa.NO_ENCONTRADA);
        assertThat(mesas.findById(deB.getId()).orElseThrow().isActivo()).isTrue();
    }

    @Test
    void desactivar_y_reactivar_es_idempotente_y_conserva_la_mesa() {
        Mesa mesa = crearMesa.ejecutar(new DatosMesa("3", null, null), ADMIN_SEMBRADO_ID).valorOLanza();

        assertThat(cambiarEstadoMesa.ejecutar(mesa.getId(), false, ADMIN_SEMBRADO_ID).valorOLanza().isActivo())
                .isFalse();
        assertThat(cambiarEstadoMesa.ejecutar(mesa.getId(), false, ADMIN_SEMBRADO_ID).esExito()).isTrue();
        assertThat(cambiarEstadoMesa.ejecutar(mesa.getId(), true, ADMIN_SEMBRADO_ID).valorOLanza().isActivo())
                .isTrue();
    }
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./mvnw -q -pl servicio-operacion -am test -Dtest=GestionDeMesasTest -Dsurefire.failIfNoSpecifiedTests=false`
Expected: compilation FAIL, `DatosMesa`, `CrearMesa`, `ActualizarMesa`, `CambiarEstadoMesa` do not exist.

- [ ] **Step 3: Write minimal implementation**

`DatosMesa.java`:
```java
package pe.rikos.operacion.venta.aplicacion;

/** Lo que se pide para crear o editar una mesa. nombre y capacidad son opcionales. */
public record DatosMesa(String numero, String nombre, Integer capacidad) {
}
```

`ValidadorDeMesa.java` (paquete, no público; evita repetir la validación entre crear y editar):
```java
package pe.rikos.operacion.venta.aplicacion;

import java.util.Optional;

/** Las reglas de un numero y de una capacidad, compartidas por CrearMesa y ActualizarMesa. */
final class ValidadorDeMesa {

    static final int LARGO_MAXIMO_NUMERO = 10;
    static final int LARGO_MAXIMO_NOMBRE = 60;

    private ValidadorDeMesa() {
    }

    /** Texto recortado; null si viene vacio. */
    static String limpio(String texto) {
        if (texto == null) {
            return null;
        }
        String recortado = texto.trim();
        return recortado.isEmpty() ? null : recortado;
    }

    static Optional<ErrorMesa> validar(DatosMesa datos) {
        String numero = limpio(datos.numero());
        if (numero == null || numero.length() > LARGO_MAXIMO_NUMERO) {
            return Optional.of(ErrorMesa.NUMERO_INVALIDO);
        }
        if (datos.capacidad() != null && datos.capacidad() <= 0) {
            return Optional.of(ErrorMesa.CAPACIDAD_INVALIDA);
        }
        return Optional.empty();
    }

    static String nombreLimpio(DatosMesa datos) {
        String nombre = limpio(datos.nombre());
        return nombre != null && nombre.length() > LARGO_MAXIMO_NOMBRE ? nombre.substring(0, LARGO_MAXIMO_NOMBRE) : nombre;
    }
}
```

`CrearMesa.java`:
```java
package pe.rikos.operacion.venta.aplicacion;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.rikos.contratos.Identificador;
import pe.rikos.contratos.Resultado;
import pe.rikos.operacion.comun.auditoria.RegistradorAuditoria;
import pe.rikos.operacion.venta.infraestructura.Mesa;
import pe.rikos.operacion.venta.infraestructura.MesaRepositorio;

/** US-027: el administrador crea una mesa en el local activo. Queda en la bitacora. */
@Service
public class CrearMesa {

    private final MesaRepositorio mesas;
    private final RegistradorAuditoria auditoria;

    public CrearMesa(MesaRepositorio mesas, RegistradorAuditoria auditoria) {
        this.mesas = mesas;
        this.auditoria = auditoria;
    }

    @Transactional
    public Resultado<Mesa, ErrorMesa> ejecutar(DatosMesa datos, String actorId) {
        String localId = LocalDeSesion.actual().orElse(null);
        if (localId == null) {
            return Resultado.fracaso(ErrorMesa.SIN_LOCAL_ACTIVO);
        }
        var invalido = ValidadorDeMesa.validar(datos);
        if (invalido.isPresent()) {
            return Resultado.fracaso(invalido.get());
        }
        String numero = ValidadorDeMesa.limpio(datos.numero());
        if (mesas.existsByLocalIdAndNumero(localId, numero)) {
            return Resultado.fracaso(ErrorMesa.NUMERO_DUPLICADO);
        }
        Mesa mesa = mesas.save(new Mesa(Identificador.nuevo().valor(), localId, numero,
                ValidadorDeMesa.nombreLimpio(datos), datos.capacidad(), actorId));
        auditoria.registrar("cambio_mesa", "mesa", mesa.getId(), null,
                RegistradorAuditoria.campos("numero", numero, "nombre", mesa.getNombre(), "capacidad",
                        mesa.getCapacidad()));
        return Resultado.exito(mesa);
    }
}
```

`ActualizarMesa.java`:
```java
package pe.rikos.operacion.venta.aplicacion;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.rikos.contratos.Resultado;
import pe.rikos.operacion.comun.auditoria.RegistradorAuditoria;
import pe.rikos.operacion.venta.infraestructura.Mesa;
import pe.rikos.operacion.venta.infraestructura.MesaRepositorio;

/** US-027: edita numero, nombre y capacidad de una mesa del local activo. Una mesa de otro local no se encuentra. */
@Service
public class ActualizarMesa {

    private final MesaRepositorio mesas;
    private final RegistradorAuditoria auditoria;

    public ActualizarMesa(MesaRepositorio mesas, RegistradorAuditoria auditoria) {
        this.mesas = mesas;
        this.auditoria = auditoria;
    }

    @Transactional
    public Resultado<Mesa, ErrorMesa> ejecutar(String id, DatosMesa datos, String actorId) {
        String localId = LocalDeSesion.actual().orElse(null);
        if (localId == null) {
            return Resultado.fracaso(ErrorMesa.SIN_LOCAL_ACTIVO);
        }
        Mesa mesa = mesas.findByIdAndLocalId(id, localId).orElse(null);
        if (mesa == null) {
            return Resultado.fracaso(ErrorMesa.NO_ENCONTRADA);
        }
        var invalido = ValidadorDeMesa.validar(datos);
        if (invalido.isPresent()) {
            return Resultado.fracaso(invalido.get());
        }
        String numero = ValidadorDeMesa.limpio(datos.numero());
        if (mesas.existsByLocalIdAndNumeroAndIdNot(localId, numero, id)) {
            return Resultado.fracaso(ErrorMesa.NUMERO_DUPLICADO);
        }
        var anterior = RegistradorAuditoria.campos("numero", mesa.getNumero(), "nombre", mesa.getNombre(),
                "capacidad", mesa.getCapacidad());
        mesa.actualizar(numero, ValidadorDeMesa.nombreLimpio(datos), datos.capacidad(), actorId);
        mesas.save(mesa);
        auditoria.registrar("cambio_mesa", "mesa", id, anterior,
                RegistradorAuditoria.campos("numero", mesa.getNumero(), "nombre", mesa.getNombre(), "capacidad",
                        mesa.getCapacidad()));
        return Resultado.exito(mesa);
    }
}
```

`CambiarEstadoMesa.java`:
```java
package pe.rikos.operacion.venta.aplicacion;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.rikos.contratos.Resultado;
import pe.rikos.operacion.comun.auditoria.RegistradorAuditoria;
import pe.rikos.operacion.venta.infraestructura.Mesa;
import pe.rikos.operacion.venta.infraestructura.MesaRepositorio;

/**
 * Desactivar una mesa la saca del mapa sin borrarla (tiene historial de
 * pedidos); reactivarla la devuelve. Idempotente. Queda en la bitacora.
 */
@Service
public class CambiarEstadoMesa {

    private final MesaRepositorio mesas;
    private final RegistradorAuditoria auditoria;

    public CambiarEstadoMesa(MesaRepositorio mesas, RegistradorAuditoria auditoria) {
        this.mesas = mesas;
        this.auditoria = auditoria;
    }

    @Transactional
    public Resultado<Mesa, ErrorMesa> ejecutar(String id, boolean activo, String actorId) {
        String localId = LocalDeSesion.actual().orElse(null);
        if (localId == null) {
            return Resultado.fracaso(ErrorMesa.SIN_LOCAL_ACTIVO);
        }
        Mesa mesa = mesas.findByIdAndLocalId(id, localId).orElse(null);
        if (mesa == null) {
            return Resultado.fracaso(ErrorMesa.NO_ENCONTRADA);
        }
        if (mesa.isActivo() == activo) {
            return Resultado.exito(mesa);
        }
        boolean estabaActiva = mesa.isActivo();
        mesa.cambiarEstado(activo, actorId);
        mesas.save(mesa);
        auditoria.registrar("cambio_estado", "mesa", id,
                RegistradorAuditoria.campos("activo", estabaActiva, "nombre", "Mesa " + mesa.getNumero()),
                RegistradorAuditoria.campos("activo", activo, "nombre", "Mesa " + mesa.getNumero()));
        return Resultado.exito(mesa);
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: same command as Step 2. Expected: `GestionDeMesasTest` `Tests run: 9, Failures: 0, Errors: 0`.

- [ ] **Step 5: Commit** (solo si el usuario lo pide)

```bash
git add servicio-operacion/src
git commit -m "feat(venta): crear, editar y activar mesas del local"
```

---

### Task 4: Bloquear, liberar y el mapa del local

**Files:**
- Create: `.../venta/aplicacion/BloquearMesa.java`, `LiberarMesa.java`, `ConsultaMesas.java`, `EstadoDeMesaConMesa.java` (registro de la fila del mapa)
- Test: `servicio-operacion/src/test/java/pe/rikos/operacion/venta/aplicacion/BloqueoDeMesasTest.java`, `MapaDeMesasTest.java`

**Interfaces:**
- Consumes: `MesaRepositorio`, `LocalDeSesion`, `CalculadorDeEstadoDeMesa`, `ErrorMesa`.
- Produces: `BloquearMesa.ejecutar(String id, String motivo, String actorId) → Resultado<Mesa, ErrorMesa>`; `LiberarMesa.ejecutar(String id, String actorId) → Resultado<Mesa, ErrorMesa>`.
- Produces: `record FilaDelMapa(Mesa mesa, EstadoDeMesa estado)` y `ConsultaMesas.mapa() → Resultado<List<FilaDelMapa>, ErrorMesa>` (solo activas del local de la sesión, por número) y `ConsultaMesas.todas() → Resultado<List<FilaDelMapa>, ErrorMesa>` (incluye inactivas, para administración).

- [ ] **Step 1: Write the failing tests**

`BloqueoDeMesasTest.java`:
```java
package pe.rikos.operacion.venta.aplicacion;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import pe.rikos.contratos.Identificador;
import pe.rikos.contratos.Resultado;
import pe.rikos.operacion.comun.auditoria.ContextoDeSeguridadDePrueba;
import pe.rikos.operacion.identidad.aplicacion.CrearLocal;
import pe.rikos.operacion.venta.infraestructura.Mesa;

import static org.assertj.core.api.Assertions.assertThat;

/** US-027 / RF-028: bloquear y liberar una mesa. Solo una mesa libre y activa; siempre con motivo. */
@SpringBootTest
@Transactional
class BloqueoDeMesasTest {

    static final String ADMIN_SEMBRADO_ID = "01M2QZ606763Q7XC00M8VD7WM1";

    @Autowired
    BloquearMesa bloquearMesa;
    @Autowired
    LiberarMesa liberarMesa;
    @Autowired
    CrearMesa crearMesa;
    @Autowired
    CambiarEstadoMesa cambiarEstadoMesa;
    @Autowired
    CrearLocal crearLocal;

    String localA;
    String localB;

    @BeforeEach
    void preparar() {
        ContextoDeSeguridadDePrueba.autenticarComo(ADMIN_SEMBRADO_ID);
        localA = local("ba");
        localB = local("bb");
        ContextoDeSeguridadDePrueba.autenticarComo(ADMIN_SEMBRADO_ID, localA);
    }

    @AfterEach
    void limpiarContexto() {
        ContextoDeSeguridadDePrueba.limpiar();
    }

    String local(String prefijo) {
        String codigo = prefijo + Identificador.nuevo().valor().substring(20).toLowerCase();
        return crearLocal.ejecutar(codigo, "Local " + codigo, "Direccion", ADMIN_SEMBRADO_ID).valorOLanza().getId();
    }

    Mesa mesa(String numero) {
        return crearMesa.ejecutar(new DatosMesa(numero, null, 4), ADMIN_SEMBRADO_ID).valorOLanza();
    }

    static ErrorMesa error(Resultado<?, ErrorMesa> resultado) {
        assertThat(resultado.esExito()).isFalse();
        return ((Resultado.Fracaso<?, ErrorMesa>) resultado).error();
    }

    @Test
    void bloquea_una_mesa_libre_con_su_motivo_quien_y_cuando() {
        Mesa mesa = mesa("1");

        Mesa bloqueada = bloquearMesa.ejecutar(mesa.getId(), "  Mesa rota  ", ADMIN_SEMBRADO_ID).valorOLanza();

        assertThat(bloqueada.isBloqueada()).isTrue();
        assertThat(bloqueada.getMotivoBloqueo()).isEqualTo("Mesa rota");
        assertThat(bloqueada.getBloqueadaPor()).isEqualTo(ADMIN_SEMBRADO_ID);
        assertThat(bloqueada.getBloqueadaEn()).isNotNull();
    }

    @Test
    void exige_un_motivo_que_no_este_en_blanco() {
        Mesa mesa = mesa("2");

        assertThat(error(bloquearMesa.ejecutar(mesa.getId(), null, ADMIN_SEMBRADO_ID)))
                .isEqualTo(ErrorMesa.MOTIVO_OBLIGATORIO);
        assertThat(error(bloquearMesa.ejecutar(mesa.getId(), "   ", ADMIN_SEMBRADO_ID)))
                .isEqualTo(ErrorMesa.MOTIVO_OBLIGATORIO);
    }

    @Test
    void no_bloquea_dos_veces_ni_libera_una_que_no_esta_bloqueada() {
        Mesa mesa = mesa("3");

        assertThat(error(liberarMesa.ejecutar(mesa.getId(), ADMIN_SEMBRADO_ID))).isEqualTo(ErrorMesa.NO_BLOQUEADA);
        bloquearMesa.ejecutar(mesa.getId(), "Reservada", ADMIN_SEMBRADO_ID).valorOLanza();
        assertThat(error(bloquearMesa.ejecutar(mesa.getId(), "Otra vez", ADMIN_SEMBRADO_ID)))
                .isEqualTo(ErrorMesa.YA_BLOQUEADA);
    }

    @Test
    void liberar_la_deja_libre_y_limpia_el_bloqueo() {
        Mesa mesa = mesa("4");
        bloquearMesa.ejecutar(mesa.getId(), "Reservada", ADMIN_SEMBRADO_ID).valorOLanza();

        Mesa libre = liberarMesa.ejecutar(mesa.getId(), ADMIN_SEMBRADO_ID).valorOLanza();

        assertThat(libre.isBloqueada()).isFalse();
        assertThat(libre.getMotivoBloqueo()).isNull();
        assertThat(libre.getBloqueadaPor()).isNull();
        assertThat(libre.getBloqueadaEn()).isNull();
    }

    @Test
    void una_mesa_inactiva_no_se_bloquea() {
        Mesa mesa = mesa("5");
        cambiarEstadoMesa.ejecutar(mesa.getId(), false, ADMIN_SEMBRADO_ID).valorOLanza();

        assertThat(error(bloquearMesa.ejecutar(mesa.getId(), "Motivo", ADMIN_SEMBRADO_ID)))
                .isEqualTo(ErrorMesa.MESA_INACTIVA);
    }

    @Test
    void una_mesa_de_otro_local_no_se_encuentra_para_bloquear_ni_liberar() {
        ContextoDeSeguridadDePrueba.autenticarComo(ADMIN_SEMBRADO_ID, localB);
        Mesa deB = mesa("1");
        ContextoDeSeguridadDePrueba.autenticarComo(ADMIN_SEMBRADO_ID, localA);

        assertThat(error(bloquearMesa.ejecutar(deB.getId(), "Motivo", ADMIN_SEMBRADO_ID)))
                .isEqualTo(ErrorMesa.NO_ENCONTRADA);
        assertThat(error(liberarMesa.ejecutar(deB.getId(), ADMIN_SEMBRADO_ID))).isEqualTo(ErrorMesa.NO_ENCONTRADA);
    }

    @Test
    void sin_local_activo_no_se_bloquea() {
        Mesa mesa = mesa("6");
        ContextoDeSeguridadDePrueba.autenticarComo(ADMIN_SEMBRADO_ID);

        assertThat(error(bloquearMesa.ejecutar(mesa.getId(), "Motivo", ADMIN_SEMBRADO_ID)))
                .isEqualTo(ErrorMesa.SIN_LOCAL_ACTIVO);
    }
}
```

`MapaDeMesasTest.java`:
```java
package pe.rikos.operacion.venta.aplicacion;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import pe.rikos.contratos.Identificador;
import pe.rikos.operacion.comun.auditoria.ContextoDeSeguridadDePrueba;
import pe.rikos.operacion.identidad.aplicacion.CrearLocal;
import pe.rikos.operacion.venta.infraestructura.Mesa;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** US-027 CA1 y CA2: el mapa muestra solo las mesas activas del local de la sesion, cada una con su estado. */
@SpringBootTest
@Transactional
class MapaDeMesasTest {

    static final String ADMIN_SEMBRADO_ID = "01M2QZ606763Q7XC00M8VD7WM1";

    @Autowired
    ConsultaMesas consulta;
    @Autowired
    CrearMesa crearMesa;
    @Autowired
    BloquearMesa bloquearMesa;
    @Autowired
    CambiarEstadoMesa cambiarEstadoMesa;
    @Autowired
    CrearLocal crearLocal;

    String localA;
    String localB;

    @BeforeEach
    void preparar() {
        ContextoDeSeguridadDePrueba.autenticarComo(ADMIN_SEMBRADO_ID);
        localA = local("pa");
        localB = local("pb");
        ContextoDeSeguridadDePrueba.autenticarComo(ADMIN_SEMBRADO_ID, localA);
    }

    @AfterEach
    void limpiarContexto() {
        ContextoDeSeguridadDePrueba.limpiar();
    }

    String local(String prefijo) {
        String codigo = prefijo + Identificador.nuevo().valor().substring(20).toLowerCase();
        return crearLocal.ejecutar(codigo, "Local " + codigo, "Direccion", ADMIN_SEMBRADO_ID).valorOLanza().getId();
    }

    Mesa mesa(String numero) {
        return crearMesa.ejecutar(new DatosMesa(numero, null, 4), ADMIN_SEMBRADO_ID).valorOLanza();
    }

    @Test
    void muestra_solo_las_mesas_del_local_de_la_sesion_por_numero_con_su_estado() {
        Mesa uno = mesa("1");
        Mesa dos = mesa("2");
        bloquearMesa.ejecutar(dos.getId(), "Reservada", ADMIN_SEMBRADO_ID).valorOLanza();
        ContextoDeSeguridadDePrueba.autenticarComo(ADMIN_SEMBRADO_ID, localB);
        mesa("1");
        ContextoDeSeguridadDePrueba.autenticarComo(ADMIN_SEMBRADO_ID, localA);

        List<FilaDelMapa> mapa = consulta.mapa().valorOLanza();

        assertThat(mapa).extracting(f -> f.mesa().getId()).containsExactly(uno.getId(), dos.getId());
        assertThat(mapa).extracting(FilaDelMapa::estado).containsExactly(EstadoDeMesa.LIBRE, EstadoDeMesa.BLOQUEADA);
        assertThat(mapa).allMatch(f -> f.mesa().getLocalId().equals(localA));
    }

    @Test
    void una_mesa_inactiva_no_sale_en_el_mapa_pero_si_en_la_lista_de_administracion() {
        Mesa uno = mesa("1");
        Mesa dos = mesa("2");
        cambiarEstadoMesa.ejecutar(dos.getId(), false, ADMIN_SEMBRADO_ID).valorOLanza();

        assertThat(consulta.mapa().valorOLanza()).extracting(f -> f.mesa().getId()).containsExactly(uno.getId());
        assertThat(consulta.todas().valorOLanza()).extracting(f -> f.mesa().getId())
                .containsExactly(uno.getId(), dos.getId());
    }

    @Test
    void los_numeros_se_ordenan_como_numeros_no_como_texto() {
        mesa("10");
        mesa("2");
        mesa("1");

        assertThat(consulta.mapa().valorOLanza()).extracting(f -> f.mesa().getNumero()).containsExactly("1", "2", "10");
    }

    @Test
    void un_local_sin_mesas_da_una_lista_vacia_y_sin_local_activo_da_error() {
        assertThat(consulta.mapa().valorOLanza()).isEmpty();

        ContextoDeSeguridadDePrueba.autenticarComo(ADMIN_SEMBRADO_ID);
        assertThat(consulta.mapa().esExito()).isFalse();
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `./mvnw -q -pl servicio-operacion -am test -Dtest=BloqueoDeMesasTest,MapaDeMesasTest -Dsurefire.failIfNoSpecifiedTests=false`
Expected: compilation FAIL, `BloquearMesa`, `LiberarMesa`, `ConsultaMesas`, `FilaDelMapa` do not exist.

- [ ] **Step 3: Write minimal implementation**

`FilaDelMapa.java`:
```java
package pe.rikos.operacion.venta.aplicacion;

import pe.rikos.operacion.venta.infraestructura.Mesa;

/** Una mesa con su estado derivado, tal como sale en el mapa. */
public record FilaDelMapa(Mesa mesa, EstadoDeMesa estado) {
}
```

`BloquearMesa.java`:
```java
package pe.rikos.operacion.venta.aplicacion;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.rikos.contratos.Resultado;
import pe.rikos.operacion.comun.auditoria.RegistradorAuditoria;
import pe.rikos.operacion.venta.infraestructura.Mesa;
import pe.rikos.operacion.venta.infraestructura.MesaRepositorio;

/**
 * RF-028 (resuelto en el sprint 6): mozo, cajero o administrador bloquean una
 * mesa LIBRE, siempre con motivo (texto libre: "mesa rota", "reservada"). Una
 * mesa unida a otra o inactiva no se bloquea. En la entrega 1 todavia no hay
 * pedidos, asi que "libre" solo mira la union; cuando existan pedidos
 * (entrega 2) tambien exigira que no tenga uno abierto.
 */
@Service
public class BloquearMesa {

    private final MesaRepositorio mesas;
    private final RegistradorAuditoria auditoria;

    public BloquearMesa(MesaRepositorio mesas, RegistradorAuditoria auditoria) {
        this.mesas = mesas;
        this.auditoria = auditoria;
    }

    @Transactional
    public Resultado<Mesa, ErrorMesa> ejecutar(String id, String motivo, String actorId) {
        String localId = LocalDeSesion.actual().orElse(null);
        if (localId == null) {
            return Resultado.fracaso(ErrorMesa.SIN_LOCAL_ACTIVO);
        }
        Mesa mesa = mesas.findByIdAndLocalId(id, localId).orElse(null);
        if (mesa == null) {
            return Resultado.fracaso(ErrorMesa.NO_ENCONTRADA);
        }
        String texto = ValidadorDeMesa.limpio(motivo);
        if (texto == null) {
            return Resultado.fracaso(ErrorMesa.MOTIVO_OBLIGATORIO);
        }
        if (!mesa.isActivo()) {
            return Resultado.fracaso(ErrorMesa.MESA_INACTIVA);
        }
        if (mesa.isBloqueada()) {
            return Resultado.fracaso(ErrorMesa.YA_BLOQUEADA);
        }
        if (mesa.getMesaPrincipalId() != null) {
            return Resultado.fracaso(ErrorMesa.MESA_NO_LIBRE);
        }
        mesa.bloquear(texto.length() > 250 ? texto.substring(0, 250) : texto, actorId);
        mesas.save(mesa);
        auditoria.registrar("bloqueo_mesa", "mesa", id,
                RegistradorAuditoria.campos("bloqueada", false, "nombre", "Mesa " + mesa.getNumero()),
                RegistradorAuditoria.campos("bloqueada", true, "motivo", mesa.getMotivoBloqueo(), "nombre",
                        "Mesa " + mesa.getNumero()));
        return Resultado.exito(mesa);
    }
}
```

`LiberarMesa.java`:
```java
package pe.rikos.operacion.venta.aplicacion;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.rikos.contratos.Resultado;
import pe.rikos.operacion.comun.auditoria.RegistradorAuditoria;
import pe.rikos.operacion.venta.infraestructura.Mesa;
import pe.rikos.operacion.venta.infraestructura.MesaRepositorio;

/** Quita el bloqueo de una mesa. Queda en la bitacora con el motivo que tenia. */
@Service
public class LiberarMesa {

    private final MesaRepositorio mesas;
    private final RegistradorAuditoria auditoria;

    public LiberarMesa(MesaRepositorio mesas, RegistradorAuditoria auditoria) {
        this.mesas = mesas;
        this.auditoria = auditoria;
    }

    @Transactional
    public Resultado<Mesa, ErrorMesa> ejecutar(String id, String actorId) {
        String localId = LocalDeSesion.actual().orElse(null);
        if (localId == null) {
            return Resultado.fracaso(ErrorMesa.SIN_LOCAL_ACTIVO);
        }
        Mesa mesa = mesas.findByIdAndLocalId(id, localId).orElse(null);
        if (mesa == null) {
            return Resultado.fracaso(ErrorMesa.NO_ENCONTRADA);
        }
        if (!mesa.isBloqueada()) {
            return Resultado.fracaso(ErrorMesa.NO_BLOQUEADA);
        }
        String motivoQueTenia = mesa.getMotivoBloqueo();
        mesa.liberar(actorId);
        mesas.save(mesa);
        auditoria.registrar("bloqueo_mesa", "mesa", id,
                RegistradorAuditoria.campos("bloqueada", true, "motivo", motivoQueTenia, "nombre",
                        "Mesa " + mesa.getNumero()),
                RegistradorAuditoria.campos("bloqueada", false, "nombre", "Mesa " + mesa.getNumero()));
        return Resultado.exito(mesa);
    }
}
```

`ConsultaMesas.java`:
```java
package pe.rikos.operacion.venta.aplicacion;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.rikos.contratos.Resultado;
import pe.rikos.operacion.venta.infraestructura.Mesa;
import pe.rikos.operacion.venta.infraestructura.MesaRepositorio;

import java.util.Comparator;
import java.util.List;

/**
 * US-027 CA1 y CA2: las mesas del local de la sesion, cada una con su estado
 * derivado. El mapa solo trae las activas; administracion trae todas. Los
 * numeros se ordenan como numeros cuando lo son ("2" antes que "10").
 */
@Service
public class ConsultaMesas {

    private static final Comparator<Mesa> POR_NUMERO = Comparator
            .comparing((Mesa m) -> !soloDigitos(m.getNumero()))
            .thenComparing(m -> soloDigitos(m.getNumero()) ? Long.parseLong(m.getNumero()) : 0L)
            .thenComparing(Mesa::getNumero);

    private final MesaRepositorio mesas;

    public ConsultaMesas(MesaRepositorio mesas) {
        this.mesas = mesas;
    }

    @Transactional(readOnly = true)
    public Resultado<List<FilaDelMapa>, ErrorMesa> mapa() {
        return listar(false);
    }

    @Transactional(readOnly = true)
    public Resultado<List<FilaDelMapa>, ErrorMesa> todas() {
        return listar(true);
    }

    private Resultado<List<FilaDelMapa>, ErrorMesa> listar(boolean incluirInactivas) {
        String localId = LocalDeSesion.actual().orElse(null);
        if (localId == null) {
            return Resultado.fracaso(ErrorMesa.SIN_LOCAL_ACTIVO);
        }
        List<FilaDelMapa> filas = mesas.findByLocalIdOrderByNumero(localId).stream()
                .filter(m -> incluirInactivas || m.isActivo())
                .sorted(POR_NUMERO)
                // Sin pedidos todavia (entrega 2): ninguna mesa esta ocupada ni pendiente de cierre.
                .map(m -> new FilaDelMapa(m, CalculadorDeEstadoDeMesa.calcular(m.isBloqueada(),
                        m.getMesaPrincipalId() != null, false, false)))
                .toList();
        return Resultado.exito(filas);
    }

    private static boolean soloDigitos(String numero) {
        return numero.matches("\\d{1,9}");
    }
}
```

- [ ] **Step 4: Run tests to verify they pass**

Run: same command as Step 2. Expected: `BloqueoDeMesasTest` `Tests run: 7`, `MapaDeMesasTest` `Tests run: 4`, sin fallos ni errores.

- [ ] **Step 5: Commit** (solo si el usuario lo pide)

```bash
git add servicio-operacion/src
git commit -m "feat(venta): bloquear, liberar y mapa de mesas del local"
```

---

### Task 5: La API, la seguridad y la bitácora

**Files:**
- Create: `.../venta/api/MesaController.java`
- Modify: `servicio-operacion/src/main/java/pe/rikos/operacion/comun/seguridad/ConfiguracionSeguridad.java` (matchers)
- Modify: `.../identidad/aplicacion/ListarAuditoria.java` (frases y etiqueta)
- Test: `servicio-operacion/src/test/java/pe/rikos/operacion/venta/api/MesasAccesoPorRolTest.java`; extiende `.../identidad/aplicacion/ListarAuditoriaTest.java`

**Interfaces:**
- Consumes: `CrearMesa`, `ActualizarMesa`, `CambiarEstadoMesa`, `BloquearMesa`, `LiberarMesa`, `ConsultaMesas`, `ErrorMesa`, `FilaDelMapa`.
- Produces (HTTP, todas del local de la sesión):
  - `GET /mesas` → 200 `[MesaRespuesta]` solo activas (`venta.ver`); `GET /mesas?todas=true` incluye inactivas (`venta.ver`).
  - `POST /mesas` body `{numero, nombre, capacidad}` → 201 `MesaRespuesta` (`venta.administrar`).
  - `PUT /mesas/{id}` mismo body → 200 (`venta.administrar`).
  - `POST /mesas/{id}/desactivar` y `/activar` → 200 `MesaRespuesta` (`venta.administrar`).
  - `POST /mesas/{id}/bloquear` body `{motivo}` → 200 (`venta.bloquear_mesa`); `POST /mesas/{id}/liberar` → 200 (`venta.bloquear_mesa`).
  - `MesaRespuesta`: `id, numero, nombre, capacidad, estado (libre|ocupada|unida|pendiente_cierre|bloqueada), motivoBloqueo, mesaPrincipalId, activo`.
  - Errores: `404 NO_ENCONTRADA`; `409 NUMERO_DUPLICADO, YA_BLOQUEADA, NO_BLOQUEADA, MESA_NO_LIBRE`; `422` para el resto (`NUMERO_INVALIDO, CAPACIDAD_INVALIDA, MOTIVO_OBLIGATORIO, MESA_INACTIVA, SIN_LOCAL_ACTIVO`). Cuerpo `{"error":"CODIGO"}`.

- [ ] **Step 1: Write the failing tests**

`MesasAccesoPorRolTest.java`:
```java
package pe.rikos.operacion.venta.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import pe.rikos.contratos.Identificador;
import pe.rikos.operacion.comun.auditoria.ContextoDeSeguridadDePrueba;
import pe.rikos.operacion.identidad.aplicacion.CrearLocal;
import pe.rikos.operacion.identidad.infraestructura.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * US-027 con los roles reales. Administradores: todo. Cajero y mozo: ven el
 * mapa y bloquean y liberan, pero no crean ni editan mesas. Cocina y
 * motorizado: nada de mesas.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class MesasAccesoPorRolTest {

    private static final ObjectMapper MAPEADOR = new ObjectMapper();
    private static final String ADMIN_SEMBRADO_ID = "01M2QZ606763Q7XC00M8VD7WM1";

    @Autowired
    MockMvc mvc;
    @Autowired
    UsuarioRepositorio usuarios;
    @Autowired
    RolRepositorio roles;
    @Autowired
    UsuarioRolRepositorio usuarioRoles;
    @Autowired
    UsuarioLocalRepositorio usuarioLocales;
    @Autowired
    LocalRepositorio locales;
    @Autowired
    PasswordEncoder passwordEncoder;
    @Autowired
    CrearLocal crearLocal;

    Local local;

    @BeforeEach
    void preparar() {
        ContextoDeSeguridadDePrueba.autenticarComo(ADMIN_SEMBRADO_ID);
        try {
            String codigo = "mr" + Identificador.nuevo().valor().substring(20).toLowerCase();
            local = locales.findById(crearLocal.ejecutar(codigo, "Local " + codigo, "Direccion", ADMIN_SEMBRADO_ID)
                    .valorOLanza().getId()).orElseThrow();
        } finally {
            ContextoDeSeguridadDePrueba.limpiar();
        }
    }

    String tokenPara(String nombreRol) throws Exception {
        String login = "mesrol-" + nombreRol.replace(" ", "") + "-" + Identificador.nuevo().valor().substring(18);
        Usuario usuario = usuarios.save(new Usuario(Identificador.nuevo().valor(), login, "Test", nombreRol,
                null, null, null, passwordEncoder.encode("clave12345"), null));
        Rol rol = roles.findAll().stream().filter(r -> r.getNombre().equals(nombreRol)).findFirst().orElseThrow();
        usuarioRoles.save(new UsuarioRol(usuario, rol, ADMIN_SEMBRADO_ID));
        usuarioLocales.save(new UsuarioLocal(usuario, local, true, ADMIN_SEMBRADO_ID));
        String respuesta = mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombreUsuario\":\"" + usuario.getNombreUsuario()
                                + "\",\"contrasena\":\"clave12345\"}"))
                .andReturn().getResponse().getContentAsString();
        return "Bearer " + MAPEADOR.readTree(respuesta).get("token").asText();
    }

    String crearMesaComo(String token, String numero) throws Exception {
        String cuerpo = mvc.perform(post("/mesas").header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"numero\":\"" + numero + "\",\"nombre\":\"Terraza\",\"capacidad\":4}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return MAPEADOR.readTree(cuerpo).get("id").asText();
    }

    @Test
    void administradores_hacen_todo_y_el_mapa_muestra_el_estado_derivado() throws Exception {
        for (String rol : new String[] {"administrador central", "administrador de local"}) {
            String token = tokenPara(rol);
            String numero = rol.startsWith("administrador c") ? "1" : "2";
            String id = crearMesaComo(token, numero);

            mvc.perform(put("/mesas/" + id).header("Authorization", token).contentType(MediaType.APPLICATION_JSON)
                            .content("{\"numero\":\"" + numero + "\",\"nombre\":\"Ventana\",\"capacidad\":6}"))
                    .andExpect(status().isOk());
            mvc.perform(post("/mesas/" + id + "/bloquear").header("Authorization", token)
                            .contentType(MediaType.APPLICATION_JSON).content("{\"motivo\":\"Mesa rota\"}"))
                    .andExpect(status().isOk());
            JsonNode mapa = MAPEADOR.readTree(mvc.perform(get("/mesas").header("Authorization", token))
                    .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
            JsonNode fila = null;
            for (JsonNode f : mapa) {
                if (f.get("id").asText().equals(id)) {
                    fila = f;
                }
            }
            assertThat(fila).isNotNull();
            assertThat(fila.get("estado").asText()).isEqualTo("bloqueada");
            assertThat(fila.get("motivoBloqueo").asText()).isEqualTo("Mesa rota");
            mvc.perform(post("/mesas/" + id + "/liberar").header("Authorization", token)).andExpect(status().isOk());
            mvc.perform(post("/mesas/" + id + "/desactivar").header("Authorization", token))
                    .andExpect(status().isOk());
            mvc.perform(post("/mesas/" + id + "/activar").header("Authorization", token)).andExpect(status().isOk());
        }
    }

    @Test
    void cajero_y_mozo_ven_el_mapa_y_bloquean_pero_no_crean_ni_editan() throws Exception {
        String admin = tokenPara("administrador de local");
        String id = crearMesaComo(admin, "1");
        for (String rol : new String[] {"cajero", "mozo"}) {
            String token = tokenPara(rol);
            mvc.perform(get("/mesas").header("Authorization", token)).andExpect(status().isOk());
            mvc.perform(post("/mesas/" + id + "/bloquear").header("Authorization", token)
                            .contentType(MediaType.APPLICATION_JSON).content("{\"motivo\":\"Reservada\"}"))
                    .andExpect(status().isOk());
            mvc.perform(post("/mesas/" + id + "/liberar").header("Authorization", token)).andExpect(status().isOk());

            mvc.perform(post("/mesas").header("Authorization", token).contentType(MediaType.APPLICATION_JSON)
                            .content("{\"numero\":\"9\"}"))
                    .andExpect(status().isForbidden());
            mvc.perform(put("/mesas/" + id).header("Authorization", token).contentType(MediaType.APPLICATION_JSON)
                            .content("{\"numero\":\"1\"}"))
                    .andExpect(status().isForbidden());
            mvc.perform(post("/mesas/" + id + "/desactivar").header("Authorization", token))
                    .andExpect(status().isForbidden());
            mvc.perform(post("/mesas/" + id + "/activar").header("Authorization", token))
                    .andExpect(status().isForbidden());
        }
    }

    @Test
    void cocina_y_motorizado_no_entran_a_las_mesas() throws Exception {
        String admin = tokenPara("administrador de local");
        String id = crearMesaComo(admin, "1");
        for (String rol : new String[] {"cocina", "motorizado"}) {
            String token = tokenPara(rol);
            mvc.perform(get("/mesas").header("Authorization", token)).andExpect(status().isForbidden());
            mvc.perform(post("/mesas/" + id + "/bloquear").header("Authorization", token)
                            .contentType(MediaType.APPLICATION_JSON).content("{\"motivo\":\"x\"}"))
                    .andExpect(status().isForbidden());
            mvc.perform(post("/mesas/" + id + "/liberar").header("Authorization", token))
                    .andExpect(status().isForbidden());
        }
    }

    @Test
    void los_errores_dan_codigos_claros() throws Exception {
        String token = tokenPara("administrador de local");
        String id = crearMesaComo(token, "1");

        mvc.perform(post("/mesas").header("Authorization", token).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"numero\":\"1\"}"))
                .andExpect(status().isConflict());
        mvc.perform(post("/mesas").header("Authorization", token).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"numero\":\"   \"}"))
                .andExpect(status().is(422));
        mvc.perform(post("/mesas").header("Authorization", token).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"numero\":\"8\",\"capacidad\":0}"))
                .andExpect(status().is(422));
        mvc.perform(post("/mesas/" + id + "/bloquear").header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"motivo\":\"  \"}"))
                .andExpect(status().is(422));
        mvc.perform(post("/mesas/" + id + "/liberar").header("Authorization", token))
                .andExpect(status().isConflict());
        mvc.perform(post("/mesas/no-existe/bloquear").header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"motivo\":\"x\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void una_mesa_de_otro_local_no_se_ve_ni_se_toca() throws Exception {
        String token = tokenPara("administrador de local");
        ContextoDeSeguridadDePrueba.autenticarComo(ADMIN_SEMBRADO_ID);
        Local otro;
        try {
            String codigo = "mo" + Identificador.nuevo().valor().substring(20).toLowerCase();
            otro = locales.findById(crearLocal.ejecutar(codigo, "Local " + codigo, "Direccion", ADMIN_SEMBRADO_ID)
                    .valorOLanza().getId()).orElseThrow();
        } finally {
            ContextoDeSeguridadDePrueba.limpiar();
        }
        Local propio = local;
        local = otro;
        String tokenDelOtro = tokenPara("administrador de local");
        String idDelOtro = crearMesaComo(tokenDelOtro, "1");
        local = propio;

        mvc.perform(post("/mesas/" + idDelOtro + "/bloquear").header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"motivo\":\"x\"}"))
                .andExpect(status().isNotFound());
        JsonNode mapa = MAPEADOR.readTree(mvc.perform(get("/mesas").header("Authorization", token))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        for (JsonNode f : mapa) {
            assertThat(f.get("id").asText()).isNotEqualTo(idDelOtro);
        }
    }
}
```

Extender `ListarAuditoriaTest` (al final de la clase, antes de la última `}`), con estos `@Autowired` nuevos junto a los demás: `pe.rikos.operacion.venta.aplicacion.CrearMesa crearMesa; pe.rikos.operacion.venta.aplicacion.BloquearMesa bloquearMesa; pe.rikos.operacion.venta.aplicacion.LiberarMesa liberarMesa; pe.rikos.operacion.venta.aplicacion.CambiarEstadoMesa cambiarEstadoMesa;`:
```java
    @Test
    void lasMesasSeVenEnEspanolEnLaBitacora() {
        String codigo = "bm" + pe.rikos.contratos.Identificador.nuevo().valor().substring(20).toLowerCase();
        String localId = crearLocalDePrueba.ejecutar(codigo, "Local " + codigo, "Dirección", ADMIN_SEMBRADO_ID)
                .valorOLanza().getId();
        ContextoDeSeguridadDePrueba.autenticarComo(ADMIN_SEMBRADO_ID, localId);

        var mesa = crearMesa.ejecutar(new pe.rikos.operacion.venta.aplicacion.DatosMesa("7", "Terraza", 4),
                ADMIN_SEMBRADO_ID).valorOLanza();
        bloquearMesa.ejecutar(mesa.getId(), "Mesa rota", ADMIN_SEMBRADO_ID);
        liberarMesa.ejecutar(mesa.getId(), ADMIN_SEMBRADO_ID);
        cambiarEstadoMesa.ejecutar(mesa.getId(), false, ADMIN_SEMBRADO_ID);

        var descripciones = listarAuditoria.ejecutar(new ListarAuditoria.Filtro(null, null, null, null, null),
                PageRequest.of(0, 100)).getContent().stream().filter(a -> mesa.getId().equals(a.entidadId()))
                .map(AuditoriaRespuesta::descripcion).toList();

        assertThat(descripciones).contains(
                "admin creó la mesa \"7\"",
                "admin bloqueó la mesa \"Mesa 7\": Mesa rota",
                "admin liberó la mesa \"Mesa 7\"",
                "admin desactivó la mesa \"Mesa 7\"");
    }
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `./mvnw -q -pl servicio-operacion -am test -Dtest=MesasAccesoPorRolTest,ListarAuditoriaTest -Dsurefire.failIfNoSpecifiedTests=false`
Expected: `MesasAccesoPorRolTest` falla (no hay endpoint: 404 o 403); `ListarAuditoriaTest.lasMesasSeVenEnEspanolEnLaBitacora` falla (frases genéricas).

- [ ] **Step 3: Write minimal implementation**

`MesaController.java`:
```java
package pe.rikos.operacion.venta.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import pe.rikos.contratos.Resultado;
import pe.rikos.operacion.comun.web.ErrorRespuesta;
import pe.rikos.operacion.venta.aplicacion.ActualizarMesa;
import pe.rikos.operacion.venta.aplicacion.BloquearMesa;
import pe.rikos.operacion.venta.aplicacion.CambiarEstadoMesa;
import pe.rikos.operacion.venta.aplicacion.ConsultaMesas;
import pe.rikos.operacion.venta.aplicacion.CrearMesa;
import pe.rikos.operacion.venta.aplicacion.DatosMesa;
import pe.rikos.operacion.venta.aplicacion.ErrorMesa;
import pe.rikos.operacion.venta.aplicacion.FilaDelMapa;
import pe.rikos.operacion.venta.aplicacion.LiberarMesa;
import pe.rikos.operacion.venta.infraestructura.Mesa;

import java.util.List;
import java.util.Map;

/**
 * US-027: las mesas del local de la sesion. Ver el mapa, venta.ver; bloquear
 * y liberar, venta.bloquear_mesa; crear, editar y activar, venta.administrar.
 * El local nunca viaja en la peticion: es siempre el activo del token.
 */
@RestController
@RequestMapping("/mesas")
public class MesaController {

    private final ConsultaMesas consulta;
    private final CrearMesa crearMesa;
    private final ActualizarMesa actualizarMesa;
    private final CambiarEstadoMesa cambiarEstado;
    private final BloquearMesa bloquearMesa;
    private final LiberarMesa liberarMesa;

    public MesaController(ConsultaMesas consulta, CrearMesa crearMesa, ActualizarMesa actualizarMesa,
                          CambiarEstadoMesa cambiarEstado, BloquearMesa bloquearMesa, LiberarMesa liberarMesa) {
        this.consulta = consulta;
        this.crearMesa = crearMesa;
        this.actualizarMesa = actualizarMesa;
        this.cambiarEstado = cambiarEstado;
        this.bloquearMesa = bloquearMesa;
        this.liberarMesa = liberarMesa;
    }

    public record MesaRequest(String numero, String nombre, Integer capacidad) {
    }

    public record BloqueoRequest(String motivo) {
    }

    /** estado: libre | ocupada | unida | pendiente_cierre | bloqueada. */
    public record MesaRespuesta(String id, String numero, String nombre, Integer capacidad, String estado,
                                String motivoBloqueo, String mesaPrincipalId, boolean activo) {
    }

    private static MesaRespuesta de(FilaDelMapa f) {
        Mesa m = f.mesa();
        return new MesaRespuesta(m.getId(), m.getNumero(), m.getNombre(), m.getCapacidad(), f.estado().codigo(),
                m.getMotivoBloqueo(), m.getMesaPrincipalId(), m.isActivo());
    }

    @Operation(summary = "El mapa de mesas del local activo (todas=true incluye las inactivas)")
    @ApiResponses({
            @ApiResponse(responseCode = "200"),
            @ApiResponse(responseCode = "422", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class)))
    })
    @GetMapping
    @Transactional(readOnly = true)
    public ResponseEntity<?> mapa(@RequestParam(defaultValue = "false") boolean todas) {
        Resultado<List<FilaDelMapa>, ErrorMesa> resultado = todas ? consulta.todas() : consulta.mapa();
        if (!resultado.esExito()) {
            return errorDe(resultado);
        }
        return ResponseEntity.ok(resultado.valorOLanza().stream().map(MesaController::de).toList());
    }

    @Operation(summary = "Crear una mesa en el local activo")
    @ApiResponses({
            @ApiResponse(responseCode = "201", content = @Content(schema = @Schema(implementation = MesaRespuesta.class))),
            @ApiResponse(responseCode = "409", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class))),
            @ApiResponse(responseCode = "422", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class)))
    })
    @PostMapping
    @Transactional
    public ResponseEntity<?> crear(@RequestBody MesaRequest cuerpo, @AuthenticationPrincipal String actorId) {
        Resultado<Mesa, ErrorMesa> resultado = crearMesa.ejecutar(
                new DatosMesa(cuerpo.numero(), cuerpo.nombre(), cuerpo.capacidad()), actorId);
        return resultado.esExito() ? ResponseEntity.status(HttpStatus.CREATED).body(unaMesa(resultado.valorOLanza()))
                : errorDe(resultado);
    }

    @Operation(summary = "Editar el numero, nombre y capacidad de una mesa")
    @ApiResponses({
            @ApiResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = MesaRespuesta.class))),
            @ApiResponse(responseCode = "404", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class))),
            @ApiResponse(responseCode = "409", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class))),
            @ApiResponse(responseCode = "422", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class)))
    })
    @PutMapping("/{id}")
    @Transactional
    public ResponseEntity<?> actualizar(@PathVariable String id, @RequestBody MesaRequest cuerpo,
                                        @AuthenticationPrincipal String actorId) {
        Resultado<Mesa, ErrorMesa> resultado = actualizarMesa.ejecutar(id,
                new DatosMesa(cuerpo.numero(), cuerpo.nombre(), cuerpo.capacidad()), actorId);
        return resultado.esExito() ? ResponseEntity.ok(unaMesa(resultado.valorOLanza())) : errorDe(resultado);
    }

    @Operation(summary = "Desactivar una mesa (sale del mapa, conserva su historial)")
    @PostMapping("/{id}/desactivar")
    @Transactional
    public ResponseEntity<?> desactivar(@PathVariable String id, @AuthenticationPrincipal String actorId) {
        Resultado<Mesa, ErrorMesa> resultado = cambiarEstado.ejecutar(id, false, actorId);
        return resultado.esExito() ? ResponseEntity.ok(unaMesa(resultado.valorOLanza())) : errorDe(resultado);
    }

    @Operation(summary = "Reactivar una mesa")
    @PostMapping("/{id}/activar")
    @Transactional
    public ResponseEntity<?> activar(@PathVariable String id, @AuthenticationPrincipal String actorId) {
        Resultado<Mesa, ErrorMesa> resultado = cambiarEstado.ejecutar(id, true, actorId);
        return resultado.esExito() ? ResponseEntity.ok(unaMesa(resultado.valorOLanza())) : errorDe(resultado);
    }

    @Operation(summary = "Bloquear una mesa libre, con motivo")
    @ApiResponses({
            @ApiResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = MesaRespuesta.class))),
            @ApiResponse(responseCode = "404", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class))),
            @ApiResponse(responseCode = "409", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class))),
            @ApiResponse(responseCode = "422", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class)))
    })
    @PostMapping("/{id}/bloquear")
    @Transactional
    public ResponseEntity<?> bloquear(@PathVariable String id, @RequestBody BloqueoRequest cuerpo,
                                      @AuthenticationPrincipal String actorId) {
        Resultado<Mesa, ErrorMesa> resultado = bloquearMesa.ejecutar(id, cuerpo.motivo(), actorId);
        return resultado.esExito() ? ResponseEntity.ok(unaMesa(resultado.valorOLanza())) : errorDe(resultado);
    }

    @Operation(summary = "Liberar una mesa bloqueada")
    @PostMapping("/{id}/liberar")
    @Transactional
    public ResponseEntity<?> liberar(@PathVariable String id, @AuthenticationPrincipal String actorId) {
        Resultado<Mesa, ErrorMesa> resultado = liberarMesa.ejecutar(id, actorId);
        return resultado.esExito() ? ResponseEntity.ok(unaMesa(resultado.valorOLanza())) : errorDe(resultado);
    }

    /** Devuelve la mesa con su estado recien derivado (la consulta ya sabe hacerlo). */
    private MesaRespuesta unaMesa(Mesa mesa) {
        return consulta.todas().valorOLanza().stream().filter(f -> f.mesa().getId().equals(mesa.getId()))
                .findFirst().map(MesaController::de).orElseThrow();
    }

    private ResponseEntity<?> errorDe(Resultado<?, ErrorMesa> resultado) {
        ErrorMesa e = ((Resultado.Fracaso<?, ErrorMesa>) resultado).error();
        HttpStatus estado = switch (e) {
            case NO_ENCONTRADA -> HttpStatus.NOT_FOUND;
            case NUMERO_DUPLICADO, YA_BLOQUEADA, NO_BLOQUEADA, MESA_NO_LIBRE -> HttpStatus.CONFLICT;
            default -> HttpStatus.UNPROCESSABLE_ENTITY;
        };
        return ResponseEntity.status(estado).body(Map.of("error", e.name()));
    }
}
```

`ConfiguracionSeguridad.java` — insertar **antes** del matcher `// US-018: la cuenta.` (los específicos primero):
```java
                // US-027: mesas. El mapa lo ve quien ve ventas; bloquear y liberar, quien
                // tiene venta.bloquear_mesa (el mozo tambien); crear, editar y activar, solo
                // quien administra las mesas del local.
                .requestMatchers(org.springframework.http.HttpMethod.GET, "/mesas/**", "/mesas")
                    .hasAuthority("PERM_venta.ver")
                .requestMatchers(org.springframework.http.HttpMethod.POST, "/mesas/*/bloquear", "/mesas/*/liberar")
                    .hasAuthority("PERM_venta.bloquear_mesa")
                .requestMatchers("/mesas/**", "/mesas")
                    .hasAuthority("PERM_venta.administrar")
```

`ListarAuditoria.java`:
1. En `etiquetaEntidad`, agregar `case "mesa" -> "la mesa";` junto a los demás.
2. En `humanizar`, agregar dentro del `switch (fila.getEvento())` antes del `default`:
```java
            case "cambio_mesa" -> actor + (fila.getValorAnterior() == null ? " creó la mesa " : " modificó la mesa ")
                    + "\"" + RegistradorAuditoria.extraerCampo(fila.getValorNuevo(), "numero") + "\"";
            case "bloqueo_mesa" -> "true".equals(RegistradorAuditoria.extraerCampo(fila.getValorNuevo(), "bloqueada"))
                    ? actor + " bloqueó la mesa " + referencia + ": "
                            + RegistradorAuditoria.extraerCampo(fila.getValorNuevo(), "motivo")
                    : actor + " liberó la mesa " + referencia;
```
(`referencia` ya usa el campo `nombre`, que las tareas 3 y 4 escriben como `"Mesa N"`. Para `cambio_mesa` el campo `nombre` es el nombre opcional, por eso esa frase usa `numero` directamente.)

- [ ] **Step 4: Run tests to verify they pass**

Run: `./mvnw -q -pl servicio-operacion -am test -Dtest=MesasAccesoPorRolTest,ListarAuditoriaTest,ArquitecturaDeModulosTest -Dsurefire.failIfNoSpecifiedTests=false`
Expected: `MesasAccesoPorRolTest` `Tests run: 5, Failures: 0`; `ListarAuditoriaTest` sin fallos (incluye la prueba nueva); `ArquitecturaDeModulosTest` con **solo** la línea `- Cycle detected: Slice comun ->`. Si `venta` aparece en una violación de módulo, exponer el paquete con `@NamedInterface` en su `package-info.java` (no se debería necesitar: `venta` solo usa `comun.auditoria`, ya expuesto).

- [ ] **Step 5: Commit** (solo si el usuario lo pide)

```bash
git add servicio-operacion/src
git commit -m "feat(venta): API de mesas, seguridad por rol y bitacora en espanol"
```

---

### Task 6: Pantalla de administración de mesas

**Files:**
- Modify: `rikos-web/src/api/cliente.ts` (tipos y funciones)
- Create: `rikos-web/src/features/venta/PantallaMesas.tsx`, `rikos-web/src/features/venta/index.ts`
- Modify: `rikos-web/src/apps/admin/index.tsx` (ruta), `rikos-web/src/apps/admin/BarraLateral.tsx` (ítem), `rikos-web/src/estilos/global.css`, `rikos-web/src/features/auditoria/PantallaAuditoria.tsx` (eventos)

**Interfaces:**
- Consumes: HTTP de la tarea 5 (tras `npm run generar-tipos`).
- Produces: `interface Mesa { id, numero, nombre|null, capacidad|null, estado: EstadoDeMesa, motivoBloqueo|null, mesaPrincipalId|null, activo }`, `type EstadoDeMesa = 'libre'|'ocupada'|'unida'|'pendiente_cierre'|'bloqueada'`, y funciones `listarMesas(token, todas)`, `crearMesa(token, datos)`, `actualizarMesa(token, id, datos)`, `desactivarMesa(token, id)`, `activarMesa(token, id)`, `bloquearMesa(token, id, motivo)`, `liberarMesa(token, id)`.

- [ ] **Step 1: Write the failing check**

No hay pruebas de componentes en este proyecto (el patrón es `tsc -b` + verificación en el navegador). El chequeo que falla primero es el compilador: agregar a `apps/admin/index.tsx` la ruta y el import de `PantallaMesas` **antes** de crear el archivo.

```tsx
import { PantallaMesas } from '../../features/venta'
// ...
<Route path="configuracion/mesas" element={<PantallaMesas />} />
```

- [ ] **Step 2: Run check to verify it fails**

Run: `cd rikos-web && npx tsc -b --force`
Expected: FAIL, `Cannot find module '../../features/venta'`.

- [ ] **Step 3: Write minimal implementation**

`api/cliente.ts` — agregar antes de `// ---- Disponibilidad por local (US-015)`:
```ts
// ---- Mesas (US-027) --------------------------------------------------------

export type EstadoDeMesa = 'libre' | 'ocupada' | 'unida' | 'pendiente_cierre' | 'bloqueada'

/** Siempre del local activo del token: el local nunca viaja en la peticion. El estado se deriva en el servidor. */
export interface Mesa {
  id: string
  numero: string
  nombre: string | null
  capacidad: number | null
  estado: EstadoDeMesa
  motivoBloqueo: string | null
  mesaPrincipalId: string | null
  activo: boolean
}

export interface DatosMesa {
  numero: string
  nombre: string | null
  capacidad: number | null
}

/** todas=true incluye las inactivas (administracion); el mapa del POS pide solo las activas. */
export const listarMesas = (token: string, todas = false) =>
  pedirAutenticado<Mesa[]>(`/api/mesas${todas ? '?todas=true' : ''}`, token)

export const crearMesa = (token: string, datos: DatosMesa) =>
  pedirAutenticado<Mesa>('/api/mesas', token, { method: 'POST', body: JSON.stringify(datos) })

export const actualizarMesa = (token: string, id: string, datos: DatosMesa) =>
  pedirAutenticado<Mesa>(`/api/mesas/${id}`, token, { method: 'PUT', body: JSON.stringify(datos) })

export const desactivarMesa = (token: string, id: string) =>
  pedirAutenticado<Mesa>(`/api/mesas/${id}/desactivar`, token, { method: 'POST' })

export const activarMesa = (token: string, id: string) =>
  pedirAutenticado<Mesa>(`/api/mesas/${id}/activar`, token, { method: 'POST' })

export const bloquearMesa = (token: string, id: string, motivo: string) =>
  pedirAutenticado<Mesa>(`/api/mesas/${id}/bloquear`, token, { method: 'POST', body: JSON.stringify({ motivo }) })

export const liberarMesa = (token: string, id: string) =>
  pedirAutenticado<Mesa>(`/api/mesas/${id}/liberar`, token, { method: 'POST' })
```

`features/venta/index.ts`:
```ts
export { PantallaMesas } from './PantallaMesas'
export { MapaDeMesas } from './MapaDeMesas'
```
(la línea de `MapaDeMesas` se activa en la tarea 7; mientras tanto va solo la de `PantallaMesas`).

`features/venta/PantallaMesas.tsx`: pantalla con el patrón de `PantallaMotivos`/`PantallaPensionistas`. Contenido exacto:
```tsx
import { useCallback, useEffect, useState } from 'react'
import { useSesion } from '../../comun/sesion/SesionContexto'
import {
  Aviso,
  Boton,
  Campo,
  EstadoCargando,
  EstadoError,
  EstadoVacio,
  IconoMas,
  Insignia,
  Modal,
} from '../../comun/componentes'
import {
  ErrorApi,
  actualizarMesa,
  activarMesa,
  crearMesa,
  desactivarMesa,
  listarMesas,
  type Mesa,
} from '../../api/cliente'

/**
 * US-027: administrar las mesas del local activo (crear, editar, activar y
 * desactivar). Solo quien tiene venta.administrar; el resto ve la lista.
 * Cambiar de local en el encabezado cambia de que local son las mesas.
 * Una mesa no se borra: tiene historial de pedidos; se desactiva.
 */

function mensajeDe(error: unknown, porDefecto: string): string {
  if (error instanceof ErrorApi) {
    switch (error.cuerpo?.error) {
      case 'NUMERO_INVALIDO':
        return 'El número es obligatorio y tiene hasta 10 caracteres.'
      case 'NUMERO_DUPLICADO':
        return 'Ya hay una mesa con ese número en este local.'
      case 'CAPACIDAD_INVALIDA':
        return 'La capacidad tiene que ser un número mayor que cero.'
      case 'SIN_LOCAL_ACTIVO':
        return 'Elige primero el local en el que estás trabajando.'
      case 'NO_ENCONTRADA':
        return 'La mesa ya no existe en este local.'
    }
    if (error.estado === 403) return 'No tienes permiso para hacer esto.'
  }
  return porDefecto
}

export function PantallaMesas() {
  const { sesion, tienePermiso } = useSesion()
  const token = sesion!.token
  const puedeAdministrar = tienePermiso('venta.administrar')

  const [mesas, setMesas] = useState<Mesa[]>([])
  const [cargando, setCargando] = useState(true)
  const [error, setError] = useState('')
  const [dialogo, setDialogo] = useState<{ mesa: Mesa | null } | null>(null)

  const cargar = useCallback(() => {
    setCargando(true)
    setError('')
    listarMesas(token, true)
      .then(setMesas)
      .catch((e) => setError(mensajeDe(e, 'No se pudieron cargar las mesas.')))
      .finally(() => setCargando(false))
  }, [token, sesion?.localActivoId])

  useEffect(cargar, [cargar])

  async function cambiarActiva(mesa: Mesa) {
    try {
      await (mesa.activo ? desactivarMesa(token, mesa.id) : activarMesa(token, mesa.id))
      cargar()
    } catch (e) {
      setError(mensajeDe(e, 'No se pudo cambiar la mesa.'))
    }
  }

  return (
    <section className="panel-admin">
      <div className="panel-admin-encabezado">
        <div>
          <p className="categorias-migaja">Configuración</p>
          <h1>Mesas</h1>
          <p className="panel-admin-subtitulo">
            Las mesas del local en el que estás trabajando. El mozo las ve en el mapa de mesas del POS.
          </p>
        </div>
        {puedeAdministrar && !cargando && !error && (
          <Boton onClick={() => setDialogo({ mesa: null })}>
            <IconoMas width={16} height={16} aria-hidden="true" /> Nueva mesa
          </Boton>
        )}
      </div>

      {!puedeAdministrar && <Aviso tono="informacion">Puedes ver las mesas, pero no modificarlas con tus permisos.</Aviso>}

      {cargando ? (
        <EstadoCargando />
      ) : error ? (
        <EstadoError mensaje={error} accion={<Boton onClick={cargar}>Reintentar</Boton>} />
      ) : mesas.length === 0 ? (
        <div className="panel-admin-tarjeta">
          <EstadoVacio
            titulo="Este local todavía no tiene mesas"
            descripcion="Crea las mesas del salón para que el mozo pueda verlas y tomar pedidos."
            accion={puedeAdministrar ? <Boton onClick={() => setDialogo({ mesa: null })}>Nueva mesa</Boton> : undefined}
          />
        </div>
      ) : (
        <div className="prod-tabla-tarjeta">
          <div className="prod-tabla-scroll">
            <table className="prod-tabla">
              <thead>
                <tr>
                  <th>Número</th>
                  <th>Nombre</th>
                  <th className="cta-num">Capacidad</th>
                  <th>Estado</th>
                  <th></th>
                </tr>
              </thead>
              <tbody>
                {mesas.map((m) => (
                  <tr key={m.id} className={`prod-fila ${m.activo ? '' : 'prod-fila-inactiva'}`}>
                    <td className="prod-codigo">{m.numero}</td>
                    <td>{m.nombre ?? <span className="prod-vacio">—</span>}</td>
                    <td className="cta-num">{m.capacidad ?? <span className="prod-vacio">—</span>}</td>
                    <td>
                      <Insignia tono={m.activo ? 'activo' : 'inactivo'} />
                      {m.estado === 'bloqueada' && <span className="cta-nota"> Bloqueada: {m.motivoBloqueo}</span>}
                    </td>
                    <td className="cta-acciones">
                      {puedeAdministrar && (
                        <>
                          <button type="button" className="plan-historial-boton" onClick={() => setDialogo({ mesa: m })}>
                            Editar
                          </button>
                          <button type="button" className="plan-historial-boton" onClick={() => cambiarActiva(m)}>
                            {m.activo ? 'Desactivar' : 'Activar'}
                          </button>
                        </>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          <div className="prod-pie">
            <span>{mesas.filter((m) => m.activo).length} activas de {mesas.length} mesas</span>
          </div>
        </div>
      )}

      {dialogo && (
        <DialogoMesa
          token={token}
          mesa={dialogo.mesa}
          onCerrar={() => setDialogo(null)}
          onGuardado={() => {
            setDialogo(null)
            cargar()
          }}
        />
      )}
    </section>
  )
}

function DialogoMesa({ token, mesa, onCerrar, onGuardado }: {
  token: string
  mesa: Mesa | null
  onCerrar: () => void
  onGuardado: () => void
}) {
  const [numero, setNumero] = useState(mesa?.numero ?? '')
  const [nombre, setNombre] = useState(mesa?.nombre ?? '')
  const [capacidad, setCapacidad] = useState(mesa?.capacidad != null ? String(mesa.capacidad) : '')
  const [guardando, setGuardando] = useState(false)
  const [error, setError] = useState('')

  function guardar() {
    const cap = capacidad.trim() === '' ? null : Number(capacidad)
    if (cap !== null && (!Number.isInteger(cap) || cap <= 0)) {
      setError('La capacidad tiene que ser un número entero mayor que cero.')
      return
    }
    if (numero.trim() === '') {
      setError('El número de la mesa es obligatorio.')
      return
    }
    setGuardando(true)
    setError('')
    const datos = { numero: numero.trim(), nombre: nombre.trim() || null, capacidad: cap }
    ;(mesa ? actualizarMesa(token, mesa.id, datos) : crearMesa(token, datos))
      .then(onGuardado)
      .catch((e) => setError(mensajeDe(e, 'No se pudo guardar la mesa.')))
      .finally(() => setGuardando(false))
  }

  return (
    <Modal
      abierto
      titulo={mesa ? `Editar mesa ${mesa.numero}` : 'Nueva mesa'}
      onCerrar={onCerrar}
      pie={
        <>
          <Boton variante="secundario" onClick={onCerrar}>
            Cancelar
          </Boton>
          <Boton cargando={guardando} onClick={guardar}>
            {mesa ? 'Guardar cambios' : 'Crear mesa'}
          </Boton>
        </>
      }
    >
      <div className="cta-dialogo">
        {error && <Aviso tono="error">{error}</Aviso>}
        <Campo etiqueta="Número" maxLength={10} value={numero} onChange={(e) => setNumero(e.target.value)} />
        <Campo
          etiqueta="Nombre"
          ayuda="Opcional. Por ejemplo: Terraza 3."
          maxLength={60}
          value={nombre}
          onChange={(e) => setNombre(e.target.value)}
        />
        <Campo
          etiqueta="Capacidad"
          ayuda="Opcional. Cuántas personas se sientan."
          inputMode="numeric"
          value={capacidad}
          onChange={(e) => setCapacidad(e.target.value)}
        />
      </div>
    </Modal>
  )
}
```

`apps/admin/BarraLateral.tsx` — en el grupo `Configuración`, después de `Parámetros de cobro`:
```tsx
          { etiqueta: 'Mesas', ruta: '/admin/configuracion/mesas' },
```

`features/auditoria/PantallaAuditoria.tsx` — en `EVENTOS`, después de `cambio_limite`:
```ts
  { valor: 'cambio_mesa', etiqueta: 'Cambio de mesa' },
  { valor: 'bloqueo_mesa', etiqueta: 'Mesa bloqueada o liberada' },
```

`estilos/global.css` no necesita reglas nuevas en esta tarea (reusa `cta-*`, `prod-*`, `plan-historial-boton`).

- [ ] **Step 4: Run check to verify it passes**

Run: `cd rikos-web && npx tsc -b --force && npm run lint 2>&1 | grep -i "venta"`
Expected: `tsc` sin salida; el lint sin líneas de `features/venta` (el error ajeno de `HorarioSemanal.tsx` no cuenta).
Luego, con el backend reiniciado (aplica V29) y `npm run generar-tipos` corrido: entrar como `admin`/`cambiar123`, ir a Configuración → Mesas, crear las mesas 1 a 6 del local activo, editar una, desactivar y reactivar otra, y comprobar el estado vacío (local sin mesas) y el error (detener el backend un momento).

- [ ] **Step 5: Commit** (solo si el usuario lo pide)

```bash
git add rikos-web/src
git commit -m "feat(venta): pantalla de administracion de mesas"
```

---

### Task 7: El mapa de mesas en el POS

**Files:**
- Create: `rikos-web/src/features/venta/MapaDeMesas.tsx`
- Modify: `rikos-web/src/features/venta/index.ts` (exportar `MapaDeMesas`), `rikos-web/src/apps/pos/index.tsx`, `rikos-web/src/estilos/global.css`

**Interfaces:**
- Consumes: `listarMesas`, `bloquearMesa`, `liberarMesa`, `type Mesa`, `type EstadoDeMesa` (tarea 6); `permisosDeUsuario(token, usuarioId)` para saber si puede bloquear.
- Produces: `MapaDeMesas({ token, usuarioId }: { token: string; usuarioId: string })`.

- [ ] **Step 1: Write the failing check**

En `apps/pos/index.tsx`, reemplazar el marcador `<div className="marcador">POS — por construir (US-027)</div>` por `<MapaDeMesas token={mozoActivo.token} usuarioId={mozoActivo.usuarioId} />` y agregar `import { MapaDeMesas } from '../../features/venta'` **antes** de crear el componente.

- [ ] **Step 2: Run check to verify it fails**

Run: `cd rikos-web && npx tsc -b --force`
Expected: FAIL, `MapaDeMesas` no se exporta de `features/venta`.

- [ ] **Step 3: Write minimal implementation**

`features/venta/MapaDeMesas.tsx`:
```tsx
import { useCallback, useEffect, useState } from 'react'
import { Aviso, Boton, Campo, EstadoCargando, EstadoError, EstadoVacio, Modal } from '../../comun/componentes'
import {
  ErrorApi,
  bloquearMesa,
  liberarMesa,
  listarMesas,
  permisosDeUsuario,
  type EstadoDeMesa,
  type Mesa,
} from '../../api/cliente'

/**
 * US-027: el mapa de mesas del POS. Muestra las mesas ACTIVAS del local del
 * token con su estado (libre, ocupada, unida, pendiente de cierre, bloqueada).
 * Se actualiza solo cada 15 s. El mozo puede bloquear una mesa libre (con
 * motivo) y liberar una bloqueada. Abrir un pedido desde la mesa llega en la
 * entrega 2. Operable con el dedo: cada mesa es un boton grande.
 */

const ETIQUETA: Record<EstadoDeMesa, string> = {
  libre: 'Libre',
  ocupada: 'Ocupada',
  unida: 'Unida',
  pendiente_cierre: 'Pendiente de cierre',
  bloqueada: 'Bloqueada',
}

const SEGUNDOS_ACTUALIZACION = 15

function mensajeDe(error: unknown, porDefecto: string): string {
  if (error instanceof ErrorApi) {
    switch (error.cuerpo?.error) {
      case 'MOTIVO_OBLIGATORIO':
        return 'Escribe el motivo del bloqueo.'
      case 'YA_BLOQUEADA':
        return 'Esa mesa ya estaba bloqueada.'
      case 'NO_BLOQUEADA':
        return 'Esa mesa ya estaba libre.'
      case 'MESA_NO_LIBRE':
        return 'Solo se puede bloquear una mesa libre.'
      case 'MESA_INACTIVA':
        return 'Esa mesa está desactivada.'
      case 'SIN_LOCAL_ACTIVO':
        return 'Elige primero el local en el que estás trabajando.'
    }
    if (error.estado === 403) return 'No tienes permiso para hacer esto.'
  }
  return porDefecto
}

export function MapaDeMesas({ token, usuarioId }: { token: string; usuarioId: string }) {
  const [mesas, setMesas] = useState<Mesa[] | null>(null)
  const [error, setError] = useState('')
  const [puedeBloquear, setPuedeBloquear] = useState(false)
  const [seleccionada, setSeleccionada] = useState<Mesa | null>(null)

  const cargar = useCallback(
    (silencioso = false) => {
      if (!silencioso) setMesas(null)
      setError('')
      listarMesas(token)
        .then(setMesas)
        .catch((e) => setError(mensajeDe(e, 'No se pudo cargar el mapa de mesas.')))
    },
    [token],
  )

  useEffect(() => {
    cargar()
    const reloj = window.setInterval(() => cargar(true), SEGUNDOS_ACTUALIZACION * 1000)
    return () => window.clearInterval(reloj)
  }, [cargar])

  useEffect(() => {
    permisosDeUsuario(token, usuarioId)
      .then((p) => setPuedeBloquear(p.includes('venta.bloquear_mesa')))
      .catch(() => setPuedeBloquear(true)) // el servidor decide: si falla la consulta, se deja intentar
  }, [token, usuarioId])

  if (error && !mesas) {
    return <EstadoError mensaje={error} accion={<Boton onClick={() => cargar()}>Reintentar</Boton>} />
  }
  if (!mesas) return <EstadoCargando />
  if (mesas.length === 0) {
    return (
      <EstadoVacio
        titulo="Este local todavía no tiene mesas"
        descripcion="Pídele al administrador que las cree en Configuración → Mesas."
      />
    )
  }

  const resumen = (['libre', 'ocupada', 'unida', 'pendiente_cierre', 'bloqueada'] as EstadoDeMesa[]).map((e) => ({
    estado: e,
    cantidad: mesas.filter((m) => m.estado === e).length,
  }))

  return (
    <section className="mapa-mesas">
      {error && <Aviso tono="error">{error}</Aviso>}
      <ul className="mapa-leyenda" aria-label="Resumen de mesas">
        {resumen.map((r) => (
          <li key={r.estado}>
            <span className={`mapa-punto mapa-${r.estado}`} aria-hidden="true" /> {ETIQUETA[r.estado]}: {r.cantidad}
          </li>
        ))}
      </ul>
      <div className="mapa-rejilla">
        {mesas.map((m) => (
          <button
            key={m.id}
            type="button"
            className={`mapa-mesa mapa-${m.estado}`}
            onClick={() => setSeleccionada(m)}
            aria-label={`Mesa ${m.numero}, ${ETIQUETA[m.estado]}`}
          >
            <strong>{m.numero}</strong>
            {m.nombre && <span className="mapa-nombre">{m.nombre}</span>}
            <span className="mapa-estado">{ETIQUETA[m.estado]}</span>
            {m.capacidad != null && <span className="mapa-capacidad">{m.capacidad} personas</span>}
          </button>
        ))}
      </div>

      {seleccionada && (
        <DialogoDeMesa
          token={token}
          mesa={seleccionada}
          puedeBloquear={puedeBloquear}
          onCerrar={() => setSeleccionada(null)}
          onCambio={() => {
            setSeleccionada(null)
            cargar(true)
          }}
        />
      )}
    </section>
  )
}

function DialogoDeMesa({ token, mesa, puedeBloquear, onCerrar, onCambio }: {
  token: string
  mesa: Mesa
  puedeBloquear: boolean
  onCerrar: () => void
  onCambio: () => void
}) {
  const [motivo, setMotivo] = useState('')
  const [trabajando, setTrabajando] = useState(false)
  const [error, setError] = useState('')

  function correr(accion: Promise<unknown>) {
    setTrabajando(true)
    setError('')
    accion
      .then(onCambio)
      .catch((e) => setError(mensajeDe(e, 'No se pudo hacer el cambio.')))
      .finally(() => setTrabajando(false))
  }

  function bloquear() {
    if (motivo.trim() === '') {
      setError('Escribe el motivo del bloqueo.')
      return
    }
    correr(bloquearMesa(token, mesa.id, motivo.trim()))
  }

  return (
    <Modal
      abierto
      titulo={`Mesa ${mesa.numero}${mesa.nombre ? ` · ${mesa.nombre}` : ''}`}
      onCerrar={onCerrar}
      pie={<Boton variante="secundario" onClick={onCerrar}>Cerrar</Boton>}
    >
      <div className="cta-dialogo">
        <p>
          Estado: <strong>{ETIQUETA[mesa.estado]}</strong>
          {mesa.capacidad != null ? ` · ${mesa.capacidad} personas` : ''}
        </p>
        {error && <Aviso tono="error">{error}</Aviso>}

        {mesa.estado === 'bloqueada' && (
          <>
            <Aviso>Bloqueada: {mesa.motivoBloqueo}</Aviso>
            {puedeBloquear && (
              <Boton cargando={trabajando} onClick={() => correr(liberarMesa(token, mesa.id))}>
                Liberar mesa
              </Boton>
            )}
          </>
        )}

        {mesa.estado === 'libre' && puedeBloquear && (
          <>
            <Campo
              etiqueta="Motivo del bloqueo"
              ayuda='Por ejemplo: "Mesa rota" o "Reservada".'
              maxLength={250}
              value={motivo}
              onChange={(e) => setMotivo(e.target.value)}
            />
            <Boton variante="secundario" cargando={trabajando} onClick={bloquear}>
              Bloquear mesa
            </Boton>
          </>
        )}

        {mesa.estado !== 'libre' && mesa.estado !== 'bloqueada' && (
          <p>Las mesas ocupadas, unidas o pendientes de cierre se atienden desde su pedido.</p>
        )}
      </div>
    </Modal>
  )
}
```

`features/venta/index.ts` queda:
```ts
export { PantallaMesas } from './PantallaMesas'
export { MapaDeMesas } from './MapaDeMesas'
```

`estilos/global.css` — agregar al final:
```css
/* US-027: mapa de mesas del POS */
.mapa-mesas { padding: var(--espacio-4); display: grid; gap: var(--espacio-4); }
.mapa-leyenda { display: flex; flex-wrap: wrap; gap: var(--espacio-4); list-style: none; margin: 0; padding: 0; font-size: 13px; color: var(--color-texto-secundario); }
.mapa-punto { display: inline-block; width: 10px; height: 10px; border-radius: 50%; margin-right: 4px; }
.mapa-rejilla { display: grid; grid-template-columns: repeat(auto-fill, minmax(140px, 1fr)); gap: var(--espacio-3); }
.mapa-mesa { min-height: 112px; border: 2px solid var(--color-separador); border-radius: 14px; padding: var(--espacio-3); display: grid; gap: 2px; align-content: center; justify-items: center; text-align: center; font: inherit; cursor: pointer; background: var(--color-superficie, #fff); color: inherit; }
.mapa-mesa strong { font-size: 28px; font-weight: 800; }
.mapa-nombre, .mapa-capacidad { font-size: 12px; color: var(--color-texto-secundario); }
.mapa-estado { font-size: 12px; font-weight: 700; }
.mapa-libre { border-color: #2e9e5b; }
.mapa-ocupada { border-color: #d98324; background: #fff5e8; }
.mapa-unida { border-color: #4a78c2; background: #eef3fb; }
.mapa-pendiente_cierre { border-color: #b3261e; background: #fdeeec; }
.mapa-bloqueada { border-color: #7a7a7a; background: #efefef; }
.mapa-punto.mapa-libre { background: #2e9e5b; }
.mapa-punto.mapa-ocupada { background: #d98324; }
.mapa-punto.mapa-unida { background: #4a78c2; }
.mapa-punto.mapa-pendiente_cierre { background: #b3261e; }
.mapa-punto.mapa-bloqueada { background: #7a7a7a; }
```

- [ ] **Step 4: Run check to verify it passes**

Run: `cd rikos-web && npx tsc -b --force && npm run lint 2>&1 | grep -i "venta\|apps/pos"`
Expected: `tsc` sin salida; el lint sin líneas de `features/venta` ni `apps/pos`.
Verificación en el navegador (`http://localhost:5173/pos`): entrar con el PIN de un mozo del local, ver el mapa con las mesas creadas en la tarea 6; bloquear una con motivo y comprobar que aparece "Bloqueada" en la rejilla y en la bitácora ("admin bloqueó la mesa…"); liberarla; comprobar en `resize_window` mobile (375), tablet (768) y escritorio que la rejilla se acomoda sin desborde horizontal de página, y el estado vacío con un local sin mesas.

- [ ] **Step 5: Commit** (solo si el usuario lo pide)

```bash
git add rikos-web/src
git commit -m "feat(venta): mapa de mesas en el POS con bloqueo y liberacion"
```

---

### Task 8: Cierre de la entrega — suite completa y documentos

**Files:**
- Create: `rikos-backend/doc/historias/US-027-mapa-de-mesas.md`
- Modify: `rikos-backend/doc/historias/README.md`, `docs/producto/plan/sprint-06-vender-en-mesa.md` (estado), `docs/producto/plan/README.md`, `ESTADO.md`, `docs/pendientes/README.md`

**Interfaces:**
- Consumes: todo lo anterior. Produces: la documentación de la historia y el estado del proyecto al día.

- [ ] **Step 1: Run the full suite**

Run: `cd rikos-backend && ./mvnw -q -pl servicio-operacion -am test -Dtest='!ArquitecturaDeModulosTest' -DfailIfNoTests=false; cd servicio-operacion/target/surefire-reports && grep -h "Tests run" $(ls *.txt | grep -v Arquitectura) | awk -F'[:,]' '{t+=$2; f+=$4; e+=$6} END {print "total",t,"fallos",f,"errores",e}'`
Expected: `fallos 0 errores 0` y un total igual a **325 + las pruebas nuevas** (6 + 9 + 7 + 4 + 5 + 1 = 32 → 357). Si el total no coincide, contar `@Test` con grep antes de escribirlo en los documentos.

- [ ] **Step 2: Verify modules and lint**

Run: `./mvnw -q -pl servicio-operacion -am test -Dtest=ArquitecturaDeModulosTest -Dsurefire.failIfNoSpecifiedTests=false; grep -n "^- " servicio-operacion/target/surefire-reports/*ArquitecturaDeModulosTest.txt | cut -c1-140` y `cd ../rikos-web && npx tsc -b --force && npm run lint 2>&1 | tail -8`
Expected: solo `- Cycle detected: Slice comun ->`; `tsc` limpio; el lint con únicamente el error ajeno de `HorarioSemanal.tsx`.

- [ ] **Step 3: Write the historia and update the documents**

`US-027-mapa-de-mesas.md` con las secciones del resto de historias: *Qué se pidió*, *Qué se construyó* (V29, módulo `venta`, casos de uso, endpoints, pantallas Mesas y mapa del POS, permisos nuevos, bitácora), *Qué se dejó fuera y por qué* (pedidos desde la mesa, unir mesas y estados ocupada/pendiente de cierre reales → entregas 2 y 4; HT-10), *Cómo se verificó* (tests con el conteo real, navegador en tres tamaños), *Bugs y tropiezos* (los reales de la sesión) y *Datos de desarrollo* (las mesas de ejemplo son inventadas). Actualizar el conteo de pruebas y marcar la entrega 1 ✅ en `plan/sprint-06-vender-en-mesa.md`, `plan/README.md` y `ESTADO.md`; agregar la línea de `US-027-mapa-de-mesas.md` en `historias/README.md`; anotar en `docs/pendientes/README.md` que `LocalDeSesion`, `catalogo.LocalActivo` y `pensionistas.LocalDeSesion` son tres copias de lo mismo y conviene unificarlas.

- [ ] **Step 4: Check the documents**

Run: `grep -n "357\|US-027" ESTADO.md rikos-backend/doc/historias/README.md docs/producto/plan/README.md | cut -c1-140`
Expected: el conteo real y las referencias a `US-027` aparecen en los cuatro archivos.

- [ ] **Step 5: Commit** (solo si el usuario lo pide)

```bash
git add docs rikos-backend/doc ESTADO.md
git commit -m "docs: cierra la entrega 1 del sprint 6 (mesas y mapa)"
```

---

## Self-review

**Cobertura de la especificación (secciones 3.2, 3.10, 4.1 y 5, entrega 1):**
- Mesa con datos propios y bloqueo con motivo/quién/cuándo, sin columna `estado` → Tareas 1 y 2 (V29, entidad).
- Cinco estados derivados con la prioridad de 3.2 → Tarea 1 (función pura, 6 pruebas) y Tarea 4 (`ConsultaMesas`).
- Crear/editar mesas de un local con `venta.administrar` → Tareas 3 y 5.
- Mapa del local de la sesión, solo activas (CA1, CA2) → Tarea 4 (`MapaDeMesasTest`) y Tarea 5 (rol y otro local).
- Bloquear/liberar por mozo, cajero y administradores, con motivo; solo mesa libre, no unida, no inactiva → Tareas 2 (permisos), 4 y 5.
- Responsive en celular, tablet y terminal (CA3) → Tarea 7 (rejilla `auto-fill` + verificación en tres tamaños).
- Bitácora de creación, edición, activar/desactivar y bloqueo → Tareas 3, 4 y 5 (`cambio_mesa`, `cambio_estado`, `bloqueo_mesa`).
- Permisos nuevos `venta.bloquear_mesa`, `venta.administrar` y `venta.modificar` para el mozo → Tarea 2.
- Corregir el modelo del bloque 6 → ya hecho al escribir la especificación (no se repite aquí).

**Desviación respecto a la especificación, con su razón:** la especificación decía "pantalla de configuración de mesas por local". La pantalla usa el **local activo de la sesión** (el administrador central lo cambia con el selector del encabezado), igual que el resto de pantallas por local (agotados): el local no viaja en la petición.

**Dejado para las entregas siguientes, a propósito:** "ocupada" y "pendiente de cierre" solo se ven cuando existan pedidos (entrega 2, que cambia `ConsultaMesas` para consultar pedidos); "unida" cuando se implemente unir mesas (entrega 4, que además decide el efecto de bloquear una mesa unida — hoy `BloquearMesa` rechaza con `MESA_NO_LIBRE` si tiene `mesaPrincipalId`); desactivar una mesa con pedido abierto no se valida todavía (se agrega con los pedidos).

**Escaneo de marcadores:** ninguna tarea contiene "TBD", "TODO" ni pasos sin código; los pasos de la Tarea 8 que describen documentos listan el contenido exacto de cada sección.

**Consistencia de tipos:** `ErrorMesa` (10 constantes) definido en la Tarea 2 y usado en 3, 4 y 5; `DatosMesa(numero, nombre, capacidad)` en 3 y 5; `FilaDelMapa(mesa, estado)` en 4 y 5; `ConsultaMesas.mapa()`/`todas()` devuelven `Resultado<List<FilaDelMapa>, ErrorMesa>` en 4 y 5; el JSON `MesaRespuesta` de la Tarea 5 coincide campo a campo con `interface Mesa` de la Tarea 6.

**Review Focus → prueba que lo cubre:** otro local no ve ni toca (`GestionDeMesasTest.una_mesa_de_otro_local…`, `BloqueoDeMesasTest.una_mesa_de_otro_local…`, `MesasAccesoPorRolTest.una_mesa_de_otro_local…`); número duplicado por local pero no entre locales (`GestionDeMesasTest` dos pruebas + restricción en base); motivo en blanco / ya bloqueada / no bloqueada (`BloqueoDeMesasTest`); mesa inactiva fuera del mapa y no bloqueable (`MapaDeMesasTest`, `BloqueoDeMesasTest`); sin local activo (`GestionDeMesasTest`, `BloqueoDeMesasTest`, `MapaDeMesasTest`); roles sin permiso (`MesasAccesoPorRolTest`).
