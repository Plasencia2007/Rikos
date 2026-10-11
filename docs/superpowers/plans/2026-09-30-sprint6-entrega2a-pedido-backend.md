# Sprint 6, entrega 2A — Tomar el pedido: backend (US-028) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Que el mozo pueda abrir un pedido en una mesa o para llevar, repartirlo entre personas (clientes normales y pensionistas) y agregarle líneas con el precio correcto (menú del día con sus opciones, carta, comida del plan, extra), sin comandar ni cobrar todavía.

**Architecture:** Cuatro tablas nuevas en el módulo `venta` (`pedido`, `cuenta_persona`, `detalle_pedido`, `detalle_pedido_componente`). El precio de cada línea lo decide siempre el `MotorDePrecios` del sprint 5 (el mozo nunca escribe uno, salvo el "extra libre"); se guarda al agregar la línea. Subtotales, táper y total **se derivan al leer**, no se guardan. La mesa pasa a estar "ocupada" mirando los pedidos abiertos (el estado se deriva, decidido en la entrega 1). Cada caso de uso devuelve `Resultado<T, ErrorPedido>` y trabaja sobre el local de la sesión.

**Tech Stack:** Spring Boot 4.1.1, Spring Modulith 2.1.1, Hibernate 7, Flyway, PostgreSQL. La pantalla del POS es la entrega 2B (plan aparte).

**Spec:** `docs/producto/plan/sprint-06-vender-en-mesa.md` (secciones 3.3, 3.4, 3.5, 3.11 y 4.1). Modelo: `docs/producto/modelo-de-datos/06-venta-pos-comandas--ep05.md` §§ 8.2 a 8.5.

## Global Constraints

- **`ULID` de 26 caracteres** para todo id (`Identificador.nuevo().valor()`); los ids de pedido, persona y línea **los puede traer el cliente** (idempotencia): enviar dos veces el mismo id no duplica.
- **`Resultado<T, E>`** (sealed) para fallos esperados, con un `enum` de error propio; no lanzar excepciones para errores de negocio.
- **Flyway manda:** migración nueva `V32__pedidos.sql`; nunca se edita una aplicada; nunca `DROP TABLE`.
- **Dinero en céntimos (BIGINT).** El mozo **nunca** escribe un precio, salvo el "extra libre" (3.11), que queda marcado como manual y en la bitácora.
- **TDD real:** la prueba se escribe y se ve fallar antes del código.
- **`RegistradorAuditoria` resuelve usuario y local solo:** un test de un caso de uso auditado necesita `ContextoDeSeguridadDePrueba.autenticarComo(usuario, localId)` en `@BeforeEach` y `limpiar()` en `@AfterEach`.
- **Spring Modulith:** `venta` solo puede usar paquetes **expuestos** con `@NamedInterface`. `ArquitecturaDeModulosTest` solo puede fallar por el ciclo conocido `comun ↔ identidad`.
- **Un pedido solo lo toca quien lo ve:** todo trabaja sobre el local de la sesión; un pedido de otro local es "no encontrado".
- **Nunca commitear** salvo que el usuario lo pida (los pasos "Commit" de este plan **no se ejecutan** sin ese pedido).
- **`mvn clean` falla en Windows si algún shell tiene su directorio dentro de `target`**; usar rutas absolutas y no `cd` a `target`. Si `target/classes` queda incompleto: borrar `target/classes` y `target/test-classes` y recompilar.
- **Docker/Postgres se cae seguido:** ante "Connection refused" en el 5433, `docker ps`, abrir Docker Desktop y esperar `healthy` antes de asumir un bug.
- Comando de prueba: `cd rikos-backend && ./mvnw -q -pl servicio-operacion -am test -Dtest=<Clase> -Dsurefire.failIfNoSpecifiedTests=false`; los resultados se leen de `servicio-operacion/target/surefire-reports/*.txt`.

## Review Focus

- Un pedido, persona o línea de **otro local** es "no encontrado" en cada operación (nunca se filtra que existe).
- Una mesa **bloqueada, unida, inactiva o con pedido abierto** no admite un pedido nuevo; una mesa de otro local tampoco.
- Un pensionista **inactivo**, o ya presente **en el mismo pedido**, no se puede asignar; asignarlo a una persona que **ya tiene líneas** se rechaza (el recálculo es de la entrega 4).
- Un producto **agotado en este local** no se agrega (pero sí en otro local); uno **sin precio** nunca entra con precio cero.
- El **segundo consumo del día** de un pensionista se cuenta entre **locales** y no cuenta las líneas quitadas.
- Con varias **opciones** en un componente del menú, elegir una que no es de hoy o dejar sin elegir se rechaza; con una sola se asigna sola y la fila se guarda igual.
- Enviar **dos veces el mismo id** de línea o de pedido no duplica; dos mozos abriendo pedido a la vez no dan 500.
- Cantidad cero o negativa, observación de más de 250 caracteres, extra libre sin descripción o con precio ≤ 0.
- Una línea **ya comandada** no se modifica ni se quita aquí (anular es la entrega 3).

Cada línea tiene su prueba en la tarea que posee el código.

---

## File Structure

**Backend (`rikos-backend/servicio-operacion/src/main`)**
- `resources/db/migration/V32__pedidos.sql` — las cuatro tablas y el evento de bitácora `precio_manual`.
- `java/pe/rikos/operacion/venta/infraestructura/`: `Pedido`, `CuentaPersona`, `DetallePedido`, `DetallePedidoComponente` y sus cuatro repositorios.
- `java/pe/rikos/operacion/venta/aplicacion/`: `Canal`, `TipoDeLinea`, `ErrorPedido`, `AbrirPedido`, `AgregarPersona`, `AsignarPensionista`, `QuitarPersona`, `AgregarLinea` (+ `SolicitudDeLinea`, `ComponenteElegido`), `ModificarLinea`, `QuitarLinea`, `CalculadorDePrecioDeLinea`, `ConsultaPedidos` (+ `PedidoDetallado`, `OfertaDelDia`).
- `java/pe/rikos/operacion/venta/api/PedidoController.java`.
- Modifica: `venta/aplicacion/ConsultaMesas.java` y `BloquearMesa.java` y `CambiarEstadoMesa.java` (ocupada real), `comun/seguridad/ConfiguracionSeguridad.java` (matchers), `identidad/aplicacion/ListarAuditoria.java` (frase), `catalogo/infraestructura/ProductoRepositorio.java` (consulta de carta).
- `package-info.java` con `@NamedInterface` en `catalogo/aplicacion`, `catalogo/infraestructura`, `pensionistas/aplicacion`, `pensionistas/infraestructura`, `identidad/aplicacion`.

**Tests (`src/test/java/pe/rikos/operacion/venta/...`)**
- `aplicacion/PedidosBase.java` (ayudante compartido), `AbrirPedidoTest`, `PersonasDelPedidoTest`, `LineasDelPedidoTest`, `PreciosDeLineaTest`, `MenuDelDiaEnPedidoTest`, `MesasConPedidoTest`; `api/PedidosAccesoPorRolTest`.

---

### Task 1: Exponer los paquetes que `venta` consume

**Files:**
- Create: `.../catalogo/aplicacion/package-info.java`, `.../catalogo/infraestructura/package-info.java`, `.../pensionistas/aplicacion/package-info.java`, `.../pensionistas/infraestructura/package-info.java`, `.../identidad/aplicacion/package-info.java`
- Test: `servicio-operacion/src/test/java/pe/rikos/operacion/venta/aplicacion/DependenciasDeVentaTest.java`

**Interfaces:**
- Produces: los cinco paquetes accesibles desde `venta` (sin que cambie ningún comportamiento).

- [ ] **Step 1: Write the failing test**

```java
package pe.rikos.operacion.venta.aplicacion;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;
import pe.rikos.operacion.ServicioOperacionApplication;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * El modulo venta usa el menu y los agotados (catalogo), el motor de precios y los pensionistas
 * (pensionistas) y el servicio vigente (identidad). Cada paquete tiene que estar expuesto a proposito:
 * Modulith no permite el acceso libre entre modulos.
 */
class DependenciasDeVentaTest {

    @Test
    void venta_solo_usa_paquetes_expuestos_de_otros_modulos() {
        var violaciones = new java.util.ArrayList<String>();
        try {
            ApplicationModules.of(ServicioOperacionApplication.class).verify();
        } catch (org.springframework.modulith.core.Violations e) {
            e.getMessages().stream().filter(m -> m.contains("Module 'venta'")).forEach(violaciones::add);
        }
        assertThat(violaciones).isEmpty();
    }
}
```

(Hoy pasa en vacío porque `venta` aún no importa nada de esos módulos; se vuelve exigente en cuanto la tarea 4 los use. Se deja escrito aquí para que el primer uso lo haga fallar y lo obligue a exponer el paquete.)

- [ ] **Step 2: Run test to verify the baseline**

Run: `./mvnw -q -pl servicio-operacion -am test -Dtest=DependenciasDeVentaTest -Dsurefire.failIfNoSpecifiedTests=false`
Expected: `Tests run: 1, Failures: 0` (línea base; el RED real ocurre en la tarea 4 si falta exponer algo).

- [ ] **Step 3: Write minimal implementation**

Cinco archivos `package-info.java`, cada uno con su paquete:

```java
/** Expuesto a proposito: el modulo venta usa el menu del dia y los agotados (sprint 6). */
@org.springframework.modulith.NamedInterface("aplicacion")
package pe.rikos.operacion.catalogo.aplicacion;
```
```java
/** Expuesto a proposito: el modulo venta lee productos y opciones del menu (sprint 6). */
@org.springframework.modulith.NamedInterface("infraestructura")
package pe.rikos.operacion.catalogo.infraestructura;
```
```java
/** Expuesto a proposito: el modulo venta usa el motor de precios y la busqueda de pensionistas (sprint 6). */
@org.springframework.modulith.NamedInterface("aplicacion")
package pe.rikos.operacion.pensionistas.aplicacion;
```
```java
/** Expuesto a proposito: el modulo venta lee al pensionista de una persona del pedido (sprint 6). */
@org.springframework.modulith.NamedInterface("infraestructura")
package pe.rikos.operacion.pensionistas.infraestructura;
```
```java
/** Expuesto a proposito: el modulo venta resuelve el servicio vigente de un local (sprint 6). */
@org.springframework.modulith.NamedInterface("aplicacion")
package pe.rikos.operacion.identidad.aplicacion;
```

- [ ] **Step 4: Run tests to verify nothing broke**

Run: `./mvnw -q -pl servicio-operacion -am test -Dtest=DependenciasDeVentaTest,ArquitecturaDeModulosTest -Dsurefire.failIfNoSpecifiedTests=false; grep -n "^- " servicio-operacion/target/surefire-reports/*ArquitecturaDeModulosTest.txt | cut -c1-140`
Expected: `DependenciasDeVentaTest` 1/1; de `ArquitecturaDeModulosTest` **solo** `- Cycle detected: Slice comun ->`.

- [ ] **Step 5: Commit** (solo si el usuario lo pide)

---

### Task 2: Las tablas, las entidades y las reglas de forma

**Files:**
- Create: `.../resources/db/migration/V32__pedidos.sql`
- Create: `.../venta/infraestructura/{Pedido,CuentaPersona,DetallePedido,DetallePedidoComponente}.java` y `{PedidoRepositorio,CuentaPersonaRepositorio,DetallePedidoRepositorio,DetallePedidoComponenteRepositorio}.java`
- Create: `.../venta/aplicacion/{Canal,TipoDeLinea,ErrorPedido}.java`
- Test: `src/test/java/pe/rikos/operacion/venta/aplicacion/PedidosBase.java`, `TablasDePedidoTest.java`

**Interfaces:**
- Produces: `enum Canal { MESA("mesa"), PARA_LLEVAR("para_llevar"); String codigo(); static Optional<Canal> desde(String) }`; `enum TipoDeLinea { MENU_NORMAL("menu_normal"), MENU_EXTRA("menu_extra"), PLAN_DESAYUNO("plan_desayuno"), PLAN_CENA("plan_cena"), PRODUCTO("producto"), EXTRA_LIBRE("extra_libre"); String codigo(); static Optional<TipoDeLinea> desde(String) }`.
- Produces: `enum ErrorPedido { NO_ENCONTRADO, SIN_LOCAL_ACTIVO, CANAL_INVALIDO, MESA_REQUERIDA, MESA_NO_ENCONTRADA, MESA_NO_DISPONIBLE, MESA_OCUPADA, SERVICIO_REQUERIDO, SERVICIO_INVALIDO, PERSONA_NO_ENCONTRADA, PENSIONISTA_NO_ENCONTRADO, PENSIONISTA_INACTIVO, PENSIONISTA_YA_EN_EL_PEDIDO, PERSONA_CON_LINEAS, PERSONA_NO_ES_PENSIONISTA, PRODUCTO_NO_ENCONTRADO, PRODUCTO_NO_VENDIBLE, PRODUCTO_AGOTADO, SIN_PRECIO, PLAN_SIN_PRECIO, MENU_NO_PROGRAMADO, MENU_FUERA_DE_SERVICIO, COMPONENTE_OBLIGATORIO, OPCION_INVALIDA, PLAN_FUERA_DE_SERVICIO, CANTIDAD_INVALIDA, DESCRIPCION_OBLIGATORIA, PRECIO_INVALIDO, OBSERVACION_LARGA, LINEA_NO_ENCONTRADA, LINEA_YA_COMANDADA }`.
- Produces (entidades, constructores y getters usados después):
  - `Pedido(String id, String localId, String numero, LocalDate fechaOperativa, String canal, String servicio, String mesaId, String mozoId)`; getters `getId, getLocalId, getNumero, getFechaOperativa, getCanal, getServicio, getMesaId, getMozoId, getEstado, getAbiertoEn`. `estado` empieza en `abierto`.
  - `CuentaPersona(String id, String pedidoId, String etiqueta, int orden)` (cliente normal); `asignarPensionista(String pensionistaId, String etiqueta)`; getters `getId, getPedidoId, getEtiqueta, getOrden, getTipoCliente, getPensionistaId, getEstado`.
  - `DetallePedido(String id, String cuentaPersonaId, String tipoLinea, String productoId, String descripcion, int cantidad, String observacion, long precioUnitario, long taperUnitario, String tarifaOrigen, String planTarifarioId, boolean segundoDelDia, String creadoPor)`; `cambiar(int cantidad, String observacion)`; getters análogos + `getEstado`.
  - `DetallePedidoComponente(String id, String detallePedidoId, String componente, String productoId, String opcionMenuId)`.
  - Repositorios (todos `JpaRepository`): `PedidoRepositorio`: `Optional<Pedido> findByIdAndLocalId(String,String)`, `Optional<Pedido> findByMesaIdAndEstadoIn(String, Collection<String>)`, `List<Pedido> findByLocalIdAndEstadoIn(String, Collection<String>)`, `long countByLocalIdAndFechaOperativa(String, LocalDate)`; `CuentaPersonaRepositorio`: `List<CuentaPersona> findByPedidoIdOrderByOrden(String)`, `Optional<CuentaPersona> findByIdAndPedidoId(String,String)`, `boolean existsByPedidoIdAndPensionistaId(String,String)`; `DetallePedidoRepositorio`: `List<DetallePedido> findByCuentaPersonaIdInOrderByCreadoEn(Collection<String>)`, `boolean existsByCuentaPersonaId(String)`, `Optional<DetallePedido> findByIdAndCuentaPersonaIdIn(String, Collection<String>)`, más la consulta del segundo consumo (tarea 5); `DetallePedidoComponenteRepositorio`: `List<DetallePedidoComponente> findByDetallePedidoIdIn(Collection<String>)`.

- [ ] **Step 1: Write the failing test**

`PedidosBase.java` (ayudante de las pruebas siguientes, sin `@Test`):

```java
package pe.rikos.operacion.venta.aplicacion;

import org.springframework.beans.factory.annotation.Autowired;
import pe.rikos.contratos.Identificador;
import pe.rikos.operacion.catalogo.infraestructura.CategoriaProducto;
import pe.rikos.operacion.catalogo.infraestructura.CategoriaProductoRepositorio;
import pe.rikos.operacion.catalogo.infraestructura.Producto;
import pe.rikos.operacion.catalogo.infraestructura.ProductoRepositorio;
import pe.rikos.operacion.catalogo.infraestructura.UnidadMedida;
import pe.rikos.operacion.catalogo.infraestructura.UnidadMedidaRepositorio;
import pe.rikos.operacion.identidad.aplicacion.CrearLocal;
import pe.rikos.operacion.pensionistas.aplicacion.Comida;
import pe.rikos.operacion.pensionistas.aplicacion.CrearPensionista;
import pe.rikos.operacion.pensionistas.aplicacion.CrearPlanTarifario;
import pe.rikos.operacion.pensionistas.aplicacion.DatosPensionista;
import pe.rikos.operacion.pensionistas.aplicacion.DatosResponsable;
import pe.rikos.operacion.pensionistas.aplicacion.PreciosPlan;
import pe.rikos.operacion.pensionistas.infraestructura.Pensionista;
import pe.rikos.operacion.pensionistas.infraestructura.PlanTarifario;

import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

/** Piezas comunes de las pruebas de pedidos: locales, mesas, productos y pensionistas de ejemplo. */
abstract class PedidosBase {

    static final String ADMIN_SEMBRADO_ID = "01M2QZ606763Q7XC00M8VD7WM1";
    /** Categorias sembradas (V15/V16): entrada, segundo. */
    static final String CATEGORIA_ENTRADA = "01M39A0RZ0000000000000S001";
    static final String CATEGORIA_SEGUNDO = "01M39A0RZ0000000000000S005";

    @Autowired
    protected CrearLocal crearLocal;
    @Autowired
    protected CrearMesa crearMesa;
    @Autowired
    protected ProductoRepositorio productos;
    @Autowired
    protected CategoriaProductoRepositorio categorias;
    @Autowired
    protected UnidadMedidaRepositorio unidades;
    @Autowired
    protected CrearPlanTarifario crearPlan;
    @Autowired
    protected CrearPensionista crearPensionista;

    protected String nuevoLocal(String prefijo) {
        String codigo = prefijo + Identificador.nuevo().valor().substring(20).toLowerCase();
        return crearLocal.ejecutar(codigo, "Local " + codigo, "Direccion", ADMIN_SEMBRADO_ID).valorOLanza().getId();
    }

    /** Requiere el local activo en el contexto de seguridad. */
    protected String nuevaMesa(String numero) {
        return crearMesa.ejecutar(new DatosMesa(numero, null, 4), ADMIN_SEMBRADO_ID).valorOLanza().getId();
    }

    /** Un producto vendible de la carta (sin marca de menu) con su precio en centimos; null = sin precio. */
    protected Producto productoDeCarta(String nombre, Long precio) {
        return producto(nombre, categoriaDeCarta(), precio, true);
    }

    protected Producto plato(String nombre, String categoriaId) {
        return producto(nombre, categorias.findById(categoriaId).orElseThrow(), 1500L, true);
    }

    private CategoriaProducto categoriaDeCarta() {
        return categorias.findAll().stream()
                .filter(c -> "producto_fijo".equals(c.getNaturaleza()) || "extra".equals(c.getNaturaleza()))
                .findFirst().orElseThrow();
    }

    private Producto producto(String nombre, CategoriaProducto categoria, Long precio, boolean vendible) {
        UnidadMedida und = unidades.findAll().stream().filter(u -> u.getCodigo().equals("und")).findFirst()
                .orElseThrow();
        String codigo = "PV-" + Identificador.nuevo().valor().substring(16);
        return productos.save(new Producto(Identificador.nuevo().valor(), codigo, nombre + " " + codigo, categoria,
                und, vendible, false, false, precio, false, null));
    }

    protected PlanTarifario nuevoPlan(long desayuno, long almuerzo, long cena) {
        return crearPlan.ejecutar("Plan pedido " + Identificador.nuevo().valor().substring(16), null,
                new PreciosPlan(desayuno, almuerzo, cena), null, ADMIN_SEMBRADO_ID).valorOLanza();
    }

    protected Pensionista nuevoPensionista(PlanTarifario plan, String apellido) {
        String dni = String.valueOf(ThreadLocalRandom.current().nextInt(10_000_000, 99_999_999));
        var datos = new DatosPensionista(dni, "Luis", apellido, null, null, null, null, null, null, plan.getId(),
                Set.of(Comida.ALMUERZO), null,
                new DatosResponsable("Rosa " + apellido, "12345678", "999", "r@c.pe", "Madre", "Av. 1"));
        return crearPensionista.ejecutar(datos, ADMIN_SEMBRADO_ID).valorOLanza().pensionista();
    }
}
```

`TablasDePedidoTest.java`:

```java
package pe.rikos.operacion.venta.aplicacion;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;
import pe.rikos.contratos.Identificador;
import pe.rikos.operacion.comun.auditoria.ContextoDeSeguridadDePrueba;
import pe.rikos.operacion.venta.infraestructura.CuentaPersona;
import pe.rikos.operacion.venta.infraestructura.CuentaPersonaRepositorio;
import pe.rikos.operacion.venta.infraestructura.DetallePedido;
import pe.rikos.operacion.venta.infraestructura.DetallePedidoRepositorio;
import pe.rikos.operacion.venta.infraestructura.Pedido;
import pe.rikos.operacion.venta.infraestructura.PedidoRepositorio;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Las reglas de FORMA que la base impone al pedido, sin pasar por los casos de uso. */
@SpringBootTest
@Transactional
class TablasDePedidoTest extends PedidosBase {

    @Autowired
    PedidoRepositorio pedidos;
    @Autowired
    CuentaPersonaRepositorio personas;
    @Autowired
    DetallePedidoRepositorio lineas;
    @Autowired
    EntityManager em;

    String local;
    String mesa;

    @BeforeEach
    void preparar() {
        ContextoDeSeguridadDePrueba.autenticarComo(ADMIN_SEMBRADO_ID);
        local = nuevoLocal("tp");
        ContextoDeSeguridadDePrueba.autenticarComo(ADMIN_SEMBRADO_ID, local);
        mesa = nuevaMesa("1");
    }

    @AfterEach
    void limpiarContexto() {
        ContextoDeSeguridadDePrueba.limpiar();
    }

    private Pedido pedido(String numero, String canal, String mesaId) {
        return pedidos.saveAndFlush(new Pedido(Identificador.nuevo().valor(), local, numero, LocalDate.now(), canal,
                "almuerzo", mesaId, ADMIN_SEMBRADO_ID));
    }

    @Test
    void un_pedido_de_mesa_lleva_mesa_y_uno_para_llevar_no() {
        pedido("0001", "mesa", mesa);
        pedido("0002", "para_llevar", null);

        assertThatThrownBy(() -> pedido("0003", "mesa", null)).isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> pedido("0004", "para_llevar", mesa)).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void el_numero_es_unico_por_local_y_dia_y_una_mesa_tiene_un_solo_pedido_en_curso() {
        pedido("0001", "mesa", mesa);

        assertThatThrownBy(() -> pedido("0001", "para_llevar", null)).isInstanceOf(DataIntegrityViolationException.class);
        String otraMesa = nuevaMesa("2");
        assertThat(pedido("0002", "mesa", otraMesa)).isNotNull();
        assertThatThrownBy(() -> pedido("0003", "mesa", mesa)).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void un_pensionista_no_se_repite_en_un_pedido_y_uno_normal_no_lleva_pensionista() {
        Pedido pedido = pedido("0001", "mesa", mesa);
        var plan = nuevoPlan(500, 800, 800);
        var pensionista = nuevoPensionista(plan, "Tablas");
        CuentaPersona uno = new CuentaPersona(Identificador.nuevo().valor(), pedido.getId(), "Persona 1", 1);
        uno.asignarPensionista(pensionista.getId(), "Luis Tablas");
        personas.saveAndFlush(uno);
        CuentaPersona dos = new CuentaPersona(Identificador.nuevo().valor(), pedido.getId(), "Persona 2", 2);
        dos.asignarPensionista(pensionista.getId(), "Luis Tablas");

        assertThatThrownBy(() -> personas.saveAndFlush(dos)).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void la_linea_de_producto_exige_producto_y_el_extra_libre_exige_descripcion_y_precio_positivo() {
        Pedido pedido = pedido("0001", "mesa", mesa);
        CuentaPersona persona = personas.saveAndFlush(
                new CuentaPersona(Identificador.nuevo().valor(), pedido.getId(), "Persona 1", 1));
        String producto = productoDeCarta("Chicha", 500L).getId();

        assertThat(lineas.saveAndFlush(new DetallePedido(Identificador.nuevo().valor(), persona.getId(), "producto",
                producto, null, 1, null, 500L, 0L, "publico", null, false, ADMIN_SEMBRADO_ID))).isNotNull();
        assertThat(lineas.saveAndFlush(new DetallePedido(Identificador.nuevo().valor(), persona.getId(), "extra_libre",
                null, "Huevo frito", 1, null, 300L, 0L, "manual", null, false, ADMIN_SEMBRADO_ID))).isNotNull();

        assertThatThrownBy(() -> lineas.saveAndFlush(new DetallePedido(Identificador.nuevo().valor(), persona.getId(),
                "producto", null, null, 1, null, 500L, 0L, "publico", null, false, ADMIN_SEMBRADO_ID)))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> lineas.saveAndFlush(new DetallePedido(Identificador.nuevo().valor(), persona.getId(),
                "extra_libre", null, null, 1, null, 300L, 0L, "manual", null, false, ADMIN_SEMBRADO_ID)))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> lineas.saveAndFlush(new DetallePedido(Identificador.nuevo().valor(), persona.getId(),
                "producto", producto, null, 0, null, 500L, 0L, "publico", null, false, ADMIN_SEMBRADO_ID)))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> lineas.saveAndFlush(new DetallePedido(Identificador.nuevo().valor(), persona.getId(),
                "producto", producto, null, 1, null, 0L, 0L, "publico", null, false, ADMIN_SEMBRADO_ID)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./mvnw -q -pl servicio-operacion -am test -Dtest=TablasDePedidoTest -Dsurefire.failIfNoSpecifiedTests=false`
Expected: compilation FAIL, `Pedido`, `CuentaPersona`, `DetallePedido` y demás no existen.

- [ ] **Step 3: Write minimal implementation**

`V32__pedidos.sql`:
```sql
-- Sprint 6, entrega 2 (US-028): el pedido y sus lineas. Decisiones en
-- docs/producto/plan/sprint-06-vender-en-mesa.md (3.3, 3.4, 3.11 y 4.1).
--
-- Difiere del modelo original en tres cosas, todas decididas en la especificacion:
--  * subtotal, total y cargo de taper NO se guardan: se derivan al leer las lineas
--    (mismo criterio que el saldo de la cuenta del pensionista);
--  * detalle_pedido.tipo_linea separa el menu, la comida del plan del pensionista
--    (desayuno y cena, que no tienen un producto de la carta) y el extra libre;
--    por eso producto_id puede ser nulo en esos dos ultimos;
--  * el costo, la receta y su composicion se congelan AL CERRAR (sprint 7), no aqui.
CREATE TABLE pedido (
    id                CHAR(26) PRIMARY KEY,
    local_id          CHAR(26) NOT NULL REFERENCES local(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    numero            VARCHAR(20) NOT NULL,
    fecha_operativa   DATE NOT NULL,
    canal             VARCHAR(12) NOT NULL CHECK (canal IN ('mesa', 'para_llevar')),
    servicio          VARCHAR(10) NOT NULL CHECK (servicio IN ('desayuno', 'almuerzo', 'cena')),
    mesa_id           CHAR(26) REFERENCES mesa(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    mozo_id           CHAR(26) REFERENCES usuario(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    estado            VARCHAR(20) NOT NULL DEFAULT 'abierto'
                      CHECK (estado IN ('abierto', 'comandado', 'pendiente_cierre')),
    abierto_en        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version           INTEGER NOT NULL DEFAULT 0,
    CONSTRAINT pedido_mesa_segun_canal CHECK ((canal = 'mesa') = (mesa_id IS NOT NULL)),
    CONSTRAINT pedido_numero_unico UNIQUE (local_id, fecha_operativa, numero)
);

-- Una mesa tiene un solo pedido en curso.
CREATE UNIQUE INDEX ux_pedido_mesa_en_curso ON pedido(mesa_id)
    WHERE mesa_id IS NOT NULL AND estado IN ('abierto', 'comandado', 'pendiente_cierre');
CREATE INDEX ix_pedido_local_estado ON pedido(local_id, estado);

CREATE TABLE cuenta_persona (
    id              CHAR(26) PRIMARY KEY,
    pedido_id       CHAR(26) NOT NULL REFERENCES pedido(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    etiqueta        VARCHAR(120) NOT NULL,
    orden           INTEGER NOT NULL,
    tipo_cliente    VARCHAR(12) NOT NULL DEFAULT 'regular' CHECK (tipo_cliente IN ('regular', 'pensionista')),
    pensionista_id  CHAR(26) REFERENCES pensionista(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    estado          VARCHAR(10) NOT NULL DEFAULT 'abierta' CHECK (estado IN ('abierta', 'cerrada')),
    CONSTRAINT cuenta_persona_pensionista_segun_tipo
        CHECK ((tipo_cliente = 'pensionista') = (pensionista_id IS NOT NULL))
);

-- Un pensionista aparece una sola vez en un pedido.
CREATE UNIQUE INDEX ux_cuenta_persona_pensionista ON cuenta_persona(pedido_id, pensionista_id)
    WHERE pensionista_id IS NOT NULL;
CREATE INDEX ix_cuenta_persona_pedido ON cuenta_persona(pedido_id, orden);
CREATE INDEX ix_cuenta_persona_pensionista ON cuenta_persona(pensionista_id) WHERE pensionista_id IS NOT NULL;

CREATE TABLE detalle_pedido (
    id                  CHAR(26) PRIMARY KEY,
    cuenta_persona_id   CHAR(26) NOT NULL REFERENCES cuenta_persona(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    tipo_linea          VARCHAR(14) NOT NULL
                        CHECK (tipo_linea IN ('menu_normal', 'menu_extra', 'plan_desayuno', 'plan_cena',
                                              'producto', 'extra_libre')),
    producto_id         CHAR(26) REFERENCES producto(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    descripcion         VARCHAR(120),
    cantidad            INTEGER NOT NULL CHECK (cantidad > 0),
    observacion         VARCHAR(250),
    estado              VARCHAR(10) NOT NULL DEFAULT 'pendiente' CHECK (estado IN ('pendiente', 'comandado', 'anulado')),
    -- Snapshot del precio al agregar la linea. Nunca cero: sin precio no se agrega.
    precio_unitario     BIGINT NOT NULL CHECK (precio_unitario > 0),
    taper_unitario      BIGINT NOT NULL DEFAULT 0 CHECK (taper_unitario >= 0),
    tarifa_origen       VARCHAR(10) NOT NULL CHECK (tarifa_origen IN ('publico', 'plan', 'extra', 'manual')),
    plan_tarifario_id   CHAR(26) REFERENCES plan_tarifario(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    segundo_del_dia     BOOLEAN NOT NULL DEFAULT FALSE,
    creado_en           TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    creado_por          CHAR(26) REFERENCES usuario(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT detalle_producto_segun_tipo
        CHECK (producto_id IS NOT NULL OR tipo_linea IN ('plan_desayuno', 'plan_cena', 'extra_libre')),
    CONSTRAINT detalle_extra_libre_con_descripcion
        CHECK (tipo_linea <> 'extra_libre' OR (descripcion IS NOT NULL AND btrim(descripcion) <> ''))
);

CREATE INDEX ix_detalle_pedido_persona ON detalle_pedido(cuenta_persona_id, creado_en);

CREATE TABLE detalle_pedido_componente (
    id                 CHAR(26) PRIMARY KEY,
    detalle_pedido_id  CHAR(26) NOT NULL REFERENCES detalle_pedido(id) ON DELETE CASCADE ON UPDATE RESTRICT,
    componente         VARCHAR(20) NOT NULL CHECK (componente IN ('entrada', 'segundo', 'guarnicion', 'refresco', 'postre')),
    producto_id        CHAR(26) NOT NULL REFERENCES producto(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    opcion_menu_id     CHAR(26) REFERENCES opcion_menu(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT detalle_componente_unico UNIQUE (detalle_pedido_id, componente)
);

ALTER TABLE auditoria DROP CONSTRAINT auditoria_evento_check;
ALTER TABLE auditoria ADD CONSTRAINT auditoria_evento_check
    CHECK (evento IN ('descuento', 'anulacion', 'reapertura', 'ajuste_inventario', 'transferencia',
                       'cambio_receta', 'cambio_precio', 'cambio_permiso', 'aprobacion_abono',
                       'rechazo_abono', 'cierre_caja', 'marcado_agotado', 'cambio_plan',
                       'cambio_estado', 'eliminacion', 'acceso_denegado', 'reactivacion',
                       'cambio_parametro', 'cambio_credencial', 'movimiento_cuenta', 'cambio_limite',
                       'cambio_mesa', 'bloqueo_mesa', 'precio_manual'));
```

Entidades (mismo patrón que `Mesa`: `@Entity`, `@Table`, ids `@JdbcTypeCode(SqlTypes.CHAR)`, `@Version` en `Pedido`; constructor protegido vacío; solo getters y los métodos de comportamiento indicados). **Todas las columnas de la migración tienen su campo** con el nombre en camelCase (`fechaOperativa` ↔ `fecha_operativa`, `cuentaPersonaId` ↔ `cuenta_persona_id`, etc.).

`Pedido`: constructor fija `estado = "abierto"` y `abiertoEn = OffsetDateTime.now()`.
`CuentaPersona`: constructor fija `tipoCliente = "regular"` y `estado = "abierta"`; `asignarPensionista(String pensionistaId, String etiqueta)` fija `tipoCliente = "pensionista"`, `pensionistaId` y `etiqueta`.
`DetallePedido`: constructor fija `estado = "pendiente"` y `creadoEn = OffsetDateTime.now()`; `cambiar(int cantidad, String observacion)` reemplaza ambos.
`DetallePedidoComponente`: constructor de cuatro campos; solo getters.

Repositorios con las firmas exactas de **Interfaces**. Ejemplo completo del más usado:

```java
package pe.rikos.operacion.venta.infraestructura;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface PedidoRepositorio extends JpaRepository<Pedido, String> {

    /** Un pedido de otro local no se encuentra: nunca se filtra que existe. */
    Optional<Pedido> findByIdAndLocalId(String id, String localId);

    Optional<Pedido> findByMesaIdAndEstadoIn(String mesaId, Collection<String> estados);

    List<Pedido> findByLocalIdAndEstadoIn(String localId, Collection<String> estados);

    long countByLocalIdAndFechaOperativa(String localId, LocalDate fechaOperativa);
}
```

`Canal`, `TipoDeLinea` y `ErrorPedido` con el contenido de **Interfaces** (cada `enum` con `codigo()` y `desde(String)` como `Signo` del sprint 5).

- [ ] **Step 4: Run test to verify it passes**

Run: mismo comando del Step 2. Expected: `TablasDePedidoTest` `Tests run: 4, Failures: 0, Errors: 0`. Si la sentencia `INSERT` de una entidad falla por un nombre de columna, corregir el `@Column`, no la migración.

- [ ] **Step 5: Commit** (solo si el usuario lo pide)

---

### Task 3: Abrir el pedido

**Files:**
- Create: `.../venta/aplicacion/AbrirPedido.java`
- Test: `.../venta/aplicacion/AbrirPedidoTest.java`

**Interfaces:**
- Consumes: `PedidoRepositorio`, `CuentaPersonaRepositorio`, `MesaRepositorio`, `ResolverServicioVigente.resolver(String localId, LocalDateTime)`, `Clock`, `ConfiguracionReloj.ZONA_DEL_RESTAURANTE`, `LocalDeSesion.actual()`.
- Produces: `AbrirPedido.ejecutar(String id, Canal canal, String mesaId, String servicio, String mozoId) → Resultado<Pedido, ErrorPedido>`. `id` puede ser nulo (se genera); si ya existe un pedido con ese `id` **del mismo local**, lo devuelve (idempotente). Crea "Persona 1" (`orden` 1, cliente normal). El número es `"%04d"` de `countByLocalIdAndFechaOperativa + 1`.

- [ ] **Step 1: Write the failing test**

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
import pe.rikos.operacion.venta.infraestructura.CuentaPersonaRepositorio;
import pe.rikos.operacion.venta.infraestructura.Pedido;

import static org.assertj.core.api.Assertions.assertThat;

/** US-028: abrir un pedido en una mesa libre o para llevar. */
@SpringBootTest
@Transactional
class AbrirPedidoTest extends PedidosBase {

    @Autowired
    AbrirPedido abrirPedido;
    @Autowired
    CuentaPersonaRepositorio personas;
    @Autowired
    BloquearMesa bloquearMesa;
    @Autowired
    CambiarEstadoMesa cambiarEstadoMesa;

    String localA;
    String localB;
    String mesa;

    @BeforeEach
    void preparar() {
        ContextoDeSeguridadDePrueba.autenticarComo(ADMIN_SEMBRADO_ID);
        localA = nuevoLocal("aa");
        localB = nuevoLocal("ab");
        ContextoDeSeguridadDePrueba.autenticarComo(ADMIN_SEMBRADO_ID, localA);
        mesa = nuevaMesa("1");
    }

    @AfterEach
    void limpiarContexto() {
        ContextoDeSeguridadDePrueba.limpiar();
    }

    static ErrorPedido error(Resultado<?, ErrorPedido> resultado) {
        assertThat(resultado.esExito()).isFalse();
        return ((Resultado.Fracaso<?, ErrorPedido>) resultado).error();
    }

    @Test
    void abre_un_pedido_de_mesa_con_su_servicio_su_numero_y_una_primera_persona() {
        Pedido pedido = abrirPedido.ejecutar(null, Canal.MESA, mesa, "almuerzo", ADMIN_SEMBRADO_ID).valorOLanza();

        assertThat(pedido.getLocalId()).isEqualTo(localA);
        assertThat(pedido.getCanal()).isEqualTo("mesa");
        assertThat(pedido.getMesaId()).isEqualTo(mesa);
        assertThat(pedido.getServicio()).isEqualTo("almuerzo");
        assertThat(pedido.getEstado()).isEqualTo("abierto");
        assertThat(pedido.getNumero()).isEqualTo("0001");
        assertThat(personas.findByPedidoIdOrderByOrden(pedido.getId())).singleElement().satisfies(p -> {
            assertThat(p.getEtiqueta()).isEqualTo("Persona 1");
            assertThat(p.getTipoCliente()).isEqualTo("regular");
        });
    }

    @Test
    void los_numeros_van_en_orden_dentro_del_local_y_del_dia() {
        String otraMesa = nuevaMesa("2");

        Pedido uno = abrirPedido.ejecutar(null, Canal.MESA, mesa, "almuerzo", ADMIN_SEMBRADO_ID).valorOLanza();
        Pedido dos = abrirPedido.ejecutar(null, Canal.MESA, otraMesa, "almuerzo", ADMIN_SEMBRADO_ID).valorOLanza();
        Pedido tres = abrirPedido.ejecutar(null, Canal.PARA_LLEVAR, null, "almuerzo", ADMIN_SEMBRADO_ID).valorOLanza();

        assertThat(uno.getNumero()).isEqualTo("0001");
        assertThat(dos.getNumero()).isEqualTo("0002");
        assertThat(tres.getNumero()).isEqualTo("0003");
    }

    @Test
    void un_pedido_para_llevar_no_lleva_mesa_y_uno_de_mesa_la_exige() {
        assertThat(abrirPedido.ejecutar(null, Canal.PARA_LLEVAR, null, "almuerzo", ADMIN_SEMBRADO_ID).esExito()).isTrue();
        assertThat(error(abrirPedido.ejecutar(null, Canal.PARA_LLEVAR, mesa, "almuerzo", ADMIN_SEMBRADO_ID)))
                .isEqualTo(ErrorPedido.CANAL_INVALIDO);
        assertThat(error(abrirPedido.ejecutar(null, Canal.MESA, null, "almuerzo", ADMIN_SEMBRADO_ID)))
                .isEqualTo(ErrorPedido.MESA_REQUERIDA);
    }

    @Test
    void una_mesa_con_pedido_bloqueada_inactiva_o_de_otro_local_no_admite_otro_pedido() {
        abrirPedido.ejecutar(null, Canal.MESA, mesa, "almuerzo", ADMIN_SEMBRADO_ID).valorOLanza();
        assertThat(error(abrirPedido.ejecutar(null, Canal.MESA, mesa, "almuerzo", ADMIN_SEMBRADO_ID)))
                .isEqualTo(ErrorPedido.MESA_OCUPADA);

        String bloqueada = nuevaMesa("2");
        bloquearMesa.ejecutar(bloqueada, "Reservada", ADMIN_SEMBRADO_ID).valorOLanza();
        assertThat(error(abrirPedido.ejecutar(null, Canal.MESA, bloqueada, "almuerzo", ADMIN_SEMBRADO_ID)))
                .isEqualTo(ErrorPedido.MESA_NO_DISPONIBLE);

        String inactiva = nuevaMesa("3");
        cambiarEstadoMesa.ejecutar(inactiva, false, ADMIN_SEMBRADO_ID).valorOLanza();
        assertThat(error(abrirPedido.ejecutar(null, Canal.MESA, inactiva, "almuerzo", ADMIN_SEMBRADO_ID)))
                .isEqualTo(ErrorPedido.MESA_NO_DISPONIBLE);

        ContextoDeSeguridadDePrueba.autenticarComo(ADMIN_SEMBRADO_ID, localB);
        assertThat(error(abrirPedido.ejecutar(null, Canal.MESA, mesa, "almuerzo", ADMIN_SEMBRADO_ID)))
                .isEqualTo(ErrorPedido.MESA_NO_ENCONTRADA);
    }

    @Test
    void el_servicio_se_valida_y_sin_horario_vigente_se_pide() {
        assertThat(error(abrirPedido.ejecutar(null, Canal.PARA_LLEVAR, null, "merienda", ADMIN_SEMBRADO_ID)))
                .isEqualTo(ErrorPedido.SERVICIO_INVALIDO);
        // El local recien creado no tiene horarios: no hay servicio vigente que deducir.
        assertThat(error(abrirPedido.ejecutar(null, Canal.PARA_LLEVAR, null, null, ADMIN_SEMBRADO_ID)))
                .isEqualTo(ErrorPedido.SERVICIO_REQUERIDO);
    }

    @Test
    void enviar_dos_veces_el_mismo_id_no_duplica_y_el_id_de_otro_local_no_se_reutiliza() {
        String id = Identificador.nuevo().valor();

        Pedido primero = abrirPedido.ejecutar(id, Canal.PARA_LLEVAR, null, "almuerzo", ADMIN_SEMBRADO_ID).valorOLanza();
        Pedido repetido = abrirPedido.ejecutar(id, Canal.PARA_LLEVAR, null, "almuerzo", ADMIN_SEMBRADO_ID).valorOLanza();

        assertThat(repetido.getId()).isEqualTo(primero.getId());
        assertThat(personas.findByPedidoIdOrderByOrden(id)).hasSize(1);

        ContextoDeSeguridadDePrueba.autenticarComo(ADMIN_SEMBRADO_ID, localB);
        assertThat(error(abrirPedido.ejecutar(id, Canal.PARA_LLEVAR, null, "almuerzo", ADMIN_SEMBRADO_ID)))
                .isEqualTo(ErrorPedido.NO_ENCONTRADO);
    }

    @Test
    void sin_local_activo_no_se_abre_nada() {
        ContextoDeSeguridadDePrueba.autenticarComo(ADMIN_SEMBRADO_ID);

        assertThat(error(abrirPedido.ejecutar(null, Canal.PARA_LLEVAR, null, "almuerzo", ADMIN_SEMBRADO_ID)))
                .isEqualTo(ErrorPedido.SIN_LOCAL_ACTIVO);
    }
}
```

(`BloquearMesa` y `CambiarEstadoMesa` ya existen. El caso "mesa con pedido → `MESA_OCUPADA`" se cumple leyendo los pedidos en curso de la mesa; su efecto sobre el mapa es la tarea 8.)

- [ ] **Step 2: Run test to verify it fails**

Run: `./mvnw -q -pl servicio-operacion -am test -Dtest=AbrirPedidoTest -Dsurefire.failIfNoSpecifiedTests=false`
Expected: compilation FAIL, `AbrirPedido` no existe.

- [ ] **Step 3: Write minimal implementation**

```java
package pe.rikos.operacion.venta.aplicacion;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.rikos.contratos.Identificador;
import pe.rikos.contratos.Resultado;
import pe.rikos.operacion.comun.tiempo.ConfiguracionReloj;
import pe.rikos.operacion.identidad.aplicacion.ResolverServicioVigente;
import pe.rikos.operacion.venta.infraestructura.CuentaPersona;
import pe.rikos.operacion.venta.infraestructura.CuentaPersonaRepositorio;
import pe.rikos.operacion.venta.infraestructura.Mesa;
import pe.rikos.operacion.venta.infraestructura.MesaRepositorio;
import pe.rikos.operacion.venta.infraestructura.Pedido;
import pe.rikos.operacion.venta.infraestructura.PedidoRepositorio;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

/**
 * US-028: abre un pedido en una mesa libre o para llevar (sin mesa). El servicio (desayuno, almuerzo o
 * cena) se toma del horario vigente del local; si no hay uno (fuera de horario), el mozo lo elige. El id
 * puede venir del cliente: reenviarlo no duplica (3.9, trabajo sin internet). Crea "Persona 1".
 */
@Service
public class AbrirPedido {

    static final List<String> ESTADOS_EN_CURSO = List.of("abierto", "comandado", "pendiente_cierre");
    private static final Set<String> SERVICIOS = Set.of("desayuno", "almuerzo", "cena");

    private final PedidoRepositorio pedidos;
    private final CuentaPersonaRepositorio personas;
    private final MesaRepositorio mesas;
    private final ResolverServicioVigente servicioVigente;
    private final Clock reloj;

    public AbrirPedido(PedidoRepositorio pedidos, CuentaPersonaRepositorio personas, MesaRepositorio mesas,
                       ResolverServicioVigente servicioVigente, Clock reloj) {
        this.pedidos = pedidos;
        this.personas = personas;
        this.mesas = mesas;
        this.servicioVigente = servicioVigente;
        this.reloj = reloj;
    }

    @Transactional
    public Resultado<Pedido, ErrorPedido> ejecutar(String id, Canal canal, String mesaId, String servicio,
                                                   String mozoId) {
        String localId = LocalDeSesion.actual().orElse(null);
        if (localId == null) {
            return Resultado.fracaso(ErrorPedido.SIN_LOCAL_ACTIVO);
        }
        if (id != null) {
            var existente = pedidos.findById(id);
            if (existente.isPresent()) {
                return existente.get().getLocalId().equals(localId) ? Resultado.exito(existente.get())
                        : Resultado.fracaso(ErrorPedido.NO_ENCONTRADO);
            }
        }
        if (canal == Canal.PARA_LLEVAR && mesaId != null) {
            return Resultado.fracaso(ErrorPedido.CANAL_INVALIDO);
        }
        if (canal == Canal.MESA) {
            if (mesaId == null) {
                return Resultado.fracaso(ErrorPedido.MESA_REQUERIDA);
            }
            Mesa mesa = mesas.findByIdAndLocalId(mesaId, localId).orElse(null);
            if (mesa == null) {
                return Resultado.fracaso(ErrorPedido.MESA_NO_ENCONTRADA);
            }
            if (!mesa.isActivo() || mesa.isBloqueada() || mesa.getMesaPrincipalId() != null) {
                return Resultado.fracaso(ErrorPedido.MESA_NO_DISPONIBLE);
            }
            if (pedidos.findByMesaIdAndEstadoIn(mesaId, ESTADOS_EN_CURSO).isPresent()) {
                return Resultado.fracaso(ErrorPedido.MESA_OCUPADA);
            }
        }
        LocalDateTime ahora = LocalDateTime.now(reloj.withZone(ConfiguracionReloj.ZONA_DEL_RESTAURANTE));
        String servicioDelPedido = servicio;
        if (servicioDelPedido == null) {
            servicioDelPedido = servicioVigente.resolver(localId, ahora).orElse(null);
            if (servicioDelPedido == null) {
                return Resultado.fracaso(ErrorPedido.SERVICIO_REQUERIDO);
            }
        } else if (!SERVICIOS.contains(servicioDelPedido)) {
            return Resultado.fracaso(ErrorPedido.SERVICIO_INVALIDO);
        }
        LocalDate hoy = ahora.toLocalDate();
        String numero = String.format("%04d", pedidos.countByLocalIdAndFechaOperativa(localId, hoy) + 1);
        Pedido pedido = pedidos.save(new Pedido(id != null ? id : Identificador.nuevo().valor(), localId, numero, hoy,
                canal.codigo(), servicioDelPedido, canal == Canal.MESA ? mesaId : null, mozoId));
        personas.save(new CuentaPersona(Identificador.nuevo().valor(), pedido.getId(), "Persona 1", 1));
        return Resultado.exito(pedido);
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: mismo comando del Step 2, más `DependenciasDeVentaTest`. Expected: `AbrirPedidoTest` `Tests run: 7, Failures: 0`; `DependenciasDeVentaTest` sin violaciones de `venta` (aquí se comprueba que la tarea 1 expuso lo necesario: `ResolverServicioVigente`).

- [ ] **Step 5: Commit** (solo si el usuario lo pide)

---

### Task 4: Las personas del pedido y el pensionista

**Files:**
- Create: `.../venta/aplicacion/AgregarPersona.java`, `AsignarPensionista.java`, `QuitarPersona.java`
- Test: `.../venta/aplicacion/PersonasDelPedidoTest.java`

**Interfaces:**
- Consumes: `PedidoRepositorio.findByIdAndLocalId`, `CuentaPersonaRepositorio`, `DetallePedidoRepositorio.existsByCuentaPersonaId`, `pensionistas.aplicacion.ConsultaPensionistas.pensionista(String) → Optional<Pensionista>`, `Pensionista.isActivo()`, `Pensionista.nombreCompleto()`.
- Produces: `AgregarPersona.ejecutar(String pedidoId, String personaId, String etiqueta, String actorId) → Resultado<CuentaPersona, ErrorPedido>` (`personaId` opcional = idempotencia; etiqueta vacía = "Persona N"); `AsignarPensionista.ejecutar(String pedidoId, String personaId, String pensionistaId, String actorId) → Resultado<CuentaPersona, ErrorPedido>`; `QuitarPersona.ejecutar(String pedidoId, String personaId) → Resultado<Void, ErrorPedido>` (solo si no tiene líneas y no es la última).

- [ ] **Step 1: Write the failing test**

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
import pe.rikos.operacion.pensionistas.aplicacion.CambiarEstadoPensionista;
import pe.rikos.operacion.pensionistas.infraestructura.Pensionista;
import pe.rikos.operacion.pensionistas.infraestructura.PlanTarifario;
import pe.rikos.operacion.venta.infraestructura.CuentaPersona;
import pe.rikos.operacion.venta.infraestructura.CuentaPersonaRepositorio;
import pe.rikos.operacion.venta.infraestructura.Pedido;

import static org.assertj.core.api.Assertions.assertThat;

/** US-028 CA1 y CA2: personas de la mesa, clientes normales y pensionistas (buscados por nombre o DNI). */
@SpringBootTest
@Transactional
class PersonasDelPedidoTest extends PedidosBase {

    @Autowired
    AbrirPedido abrirPedido;
    @Autowired
    AgregarPersona agregarPersona;
    @Autowired
    AsignarPensionista asignarPensionista;
    @Autowired
    QuitarPersona quitarPersona;
    @Autowired
    CambiarEstadoPensionista cambiarEstadoPensionista;
    @Autowired
    CuentaPersonaRepositorio personas;
    @Autowired
    AgregarLinea agregarLinea;

    String localA;
    String localB;
    Pedido pedido;
    String primeraPersona;
    PlanTarifario plan;

    @BeforeEach
    void preparar() {
        ContextoDeSeguridadDePrueba.autenticarComo(ADMIN_SEMBRADO_ID);
        localA = nuevoLocal("pa");
        localB = nuevoLocal("pb");
        plan = nuevoPlan(500, 800, 800);
        ContextoDeSeguridadDePrueba.autenticarComo(ADMIN_SEMBRADO_ID, localA);
        pedido = abrirPedido.ejecutar(null, Canal.MESA, nuevaMesa("1"), "almuerzo", ADMIN_SEMBRADO_ID).valorOLanza();
        primeraPersona = personas.findByPedidoIdOrderByOrden(pedido.getId()).get(0).getId();
    }

    @AfterEach
    void limpiarContexto() {
        ContextoDeSeguridadDePrueba.limpiar();
    }

    static ErrorPedido error(Resultado<?, ErrorPedido> resultado) {
        assertThat(resultado.esExito()).isFalse();
        return ((Resultado.Fracaso<?, ErrorPedido>) resultado).error();
    }

    @Test
    void agrega_personas_numeradas_o_con_el_nombre_que_se_de() {
        CuentaPersona dos = agregarPersona.ejecutar(pedido.getId(), null, null, ADMIN_SEMBRADO_ID).valorOLanza();
        CuentaPersona tres = agregarPersona.ejecutar(pedido.getId(), null, "  Sra. Rosa ", ADMIN_SEMBRADO_ID)
                .valorOLanza();

        assertThat(dos.getEtiqueta()).isEqualTo("Persona 2");
        assertThat(tres.getEtiqueta()).isEqualTo("Sra. Rosa");
        assertThat(personas.findByPedidoIdOrderByOrden(pedido.getId())).extracting(CuentaPersona::getOrden)
                .containsExactly(1, 2, 3);
    }

    @Test
    void enviar_dos_veces_el_mismo_id_de_persona_no_la_duplica() {
        String id = Identificador.nuevo().valor();

        agregarPersona.ejecutar(pedido.getId(), id, null, ADMIN_SEMBRADO_ID).valorOLanza();
        agregarPersona.ejecutar(pedido.getId(), id, null, ADMIN_SEMBRADO_ID).valorOLanza();

        assertThat(personas.findByPedidoIdOrderByOrden(pedido.getId())).hasSize(2);
    }

    @Test
    void asigna_un_pensionista_a_una_persona_y_la_etiqueta_pasa_a_ser_su_nombre() {
        Pensionista luis = nuevoPensionista(plan, "Quispe");

        CuentaPersona persona = asignarPensionista.ejecutar(pedido.getId(), primeraPersona, luis.getId(),
                ADMIN_SEMBRADO_ID).valorOLanza();

        assertThat(persona.getTipoCliente()).isEqualTo("pensionista");
        assertThat(persona.getPensionistaId()).isEqualTo(luis.getId());
        assertThat(persona.getEtiqueta()).isEqualTo(luis.nombreCompleto());
    }

    @Test
    void un_pensionista_inactivo_o_inexistente_no_se_asigna_ni_se_repite_en_el_pedido() {
        Pensionista luis = nuevoPensionista(plan, "Quispe");
        Pensionista inactivo = nuevoPensionista(plan, "Inactivo");
        cambiarEstadoPensionista.ejecutar(inactivo.getId(), false, ADMIN_SEMBRADO_ID).valorOLanza();
        String otra = agregarPersona.ejecutar(pedido.getId(), null, null, ADMIN_SEMBRADO_ID).valorOLanza().getId();

        assertThat(error(asignarPensionista.ejecutar(pedido.getId(), primeraPersona, inactivo.getId(),
                ADMIN_SEMBRADO_ID))).isEqualTo(ErrorPedido.PENSIONISTA_INACTIVO);
        assertThat(error(asignarPensionista.ejecutar(pedido.getId(), primeraPersona, "no-existe", ADMIN_SEMBRADO_ID)))
                .isEqualTo(ErrorPedido.PENSIONISTA_NO_ENCONTRADO);

        asignarPensionista.ejecutar(pedido.getId(), primeraPersona, luis.getId(), ADMIN_SEMBRADO_ID).valorOLanza();
        assertThat(error(asignarPensionista.ejecutar(pedido.getId(), otra, luis.getId(), ADMIN_SEMBRADO_ID)))
                .isEqualTo(ErrorPedido.PENSIONISTA_YA_EN_EL_PEDIDO);
    }

    @Test
    void no_se_asigna_un_pensionista_a_una_persona_que_ya_tiene_lineas() {
        Pensionista luis = nuevoPensionista(plan, "Quispe");
        var producto = productoDeCarta("Chicha", 500L);
        agregarLinea.ejecutar(new SolicitudDeLinea(null, pedido.getId(), primeraPersona, TipoDeLinea.PRODUCTO,
                producto.getId(), 1, null, java.util.List.of(), null, null), ADMIN_SEMBRADO_ID).valorOLanza();

        assertThat(error(asignarPensionista.ejecutar(pedido.getId(), primeraPersona, luis.getId(), ADMIN_SEMBRADO_ID)))
                .isEqualTo(ErrorPedido.PERSONA_CON_LINEAS);
    }

    @Test
    void quita_una_persona_sin_lineas_pero_nunca_la_ultima_ni_una_con_lineas() {
        String otra = agregarPersona.ejecutar(pedido.getId(), null, null, ADMIN_SEMBRADO_ID).valorOLanza().getId();
        var producto = productoDeCarta("Chicha", 500L);
        agregarLinea.ejecutar(new SolicitudDeLinea(null, pedido.getId(), primeraPersona, TipoDeLinea.PRODUCTO,
                producto.getId(), 1, null, java.util.List.of(), null, null), ADMIN_SEMBRADO_ID).valorOLanza();

        assertThat(error(quitarPersona.ejecutar(pedido.getId(), primeraPersona))).isEqualTo(ErrorPedido.PERSONA_CON_LINEAS);
        assertThat(quitarPersona.ejecutar(pedido.getId(), otra).esExito()).isTrue();
        assertThat(personas.findByPedidoIdOrderByOrden(pedido.getId())).hasSize(1);
    }

    @Test
    void una_persona_o_un_pedido_de_otro_local_no_se_encuentra() {
        ContextoDeSeguridadDePrueba.autenticarComo(ADMIN_SEMBRADO_ID, localB);

        assertThat(error(agregarPersona.ejecutar(pedido.getId(), null, null, ADMIN_SEMBRADO_ID)))
                .isEqualTo(ErrorPedido.NO_ENCONTRADO);
        assertThat(error(quitarPersona.ejecutar(pedido.getId(), primeraPersona))).isEqualTo(ErrorPedido.NO_ENCONTRADO);
    }
}
```

(Este test usa `AgregarLinea` y `SolicitudDeLinea` de la tarea 6: **queda en rojo por compilación hasta esa tarea**; se ejecuta completo al final de la tarea 6. Los pasos 2 y 4 de esta tarea corren solo el subconjunto que no las usa: ver abajo.)

- [ ] **Step 2: Run test to verify it fails**

Para poder ver el RED sin depender de la tarea 6, crear primero los dos tipos vacíos que la prueba necesita para compilar: `SolicitudDeLinea` (record final de la tarea 6) y `AgregarLinea` con un método que lanza `UnsupportedOperationException`. Run: `./mvnw -q -pl servicio-operacion -am test -Dtest=PersonasDelPedidoTest -Dsurefire.failIfNoSpecifiedTests=false`
Expected: FAIL, `AgregarPersona`, `AsignarPensionista` y `QuitarPersona` no existen (compilación).

- [ ] **Step 3: Write minimal implementation**

`AgregarPersona.java`:
```java
package pe.rikos.operacion.venta.aplicacion;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.rikos.contratos.Identificador;
import pe.rikos.contratos.Resultado;
import pe.rikos.operacion.venta.infraestructura.CuentaPersona;
import pe.rikos.operacion.venta.infraestructura.CuentaPersonaRepositorio;
import pe.rikos.operacion.venta.infraestructura.PedidoRepositorio;

import java.util.List;

/** Suma una persona (cliente normal) a la mesa: "Persona N" o el nombre que se le de. Idempotente por id. */
@Service
public class AgregarPersona {

    private static final int LARGO_MAXIMO_ETIQUETA = 120;

    private final PedidoRepositorio pedidos;
    private final CuentaPersonaRepositorio personas;

    public AgregarPersona(PedidoRepositorio pedidos, CuentaPersonaRepositorio personas) {
        this.pedidos = pedidos;
        this.personas = personas;
    }

    @Transactional
    public Resultado<CuentaPersona, ErrorPedido> ejecutar(String pedidoId, String personaId, String etiqueta,
                                                          String actorId) {
        String localId = LocalDeSesion.actual().orElse(null);
        if (localId == null) {
            return Resultado.fracaso(ErrorPedido.SIN_LOCAL_ACTIVO);
        }
        if (pedidos.findByIdAndLocalId(pedidoId, localId).isEmpty()) {
            return Resultado.fracaso(ErrorPedido.NO_ENCONTRADO);
        }
        if (personaId != null) {
            var existente = personas.findByIdAndPedidoId(personaId, pedidoId);
            if (existente.isPresent()) {
                return Resultado.exito(existente.get());
            }
        }
        List<CuentaPersona> actuales = personas.findByPedidoIdOrderByOrden(pedidoId);
        int orden = actuales.stream().mapToInt(CuentaPersona::getOrden).max().orElse(0) + 1;
        String texto = etiqueta == null ? "" : etiqueta.trim();
        if (texto.isEmpty()) {
            texto = "Persona " + orden;
        } else if (texto.length() > LARGO_MAXIMO_ETIQUETA) {
            texto = texto.substring(0, LARGO_MAXIMO_ETIQUETA);
        }
        return Resultado.exito(personas.save(new CuentaPersona(personaId != null ? personaId
                : Identificador.nuevo().valor(), pedidoId, texto, orden)));
    }
}
```

`AsignarPensionista.java`:
```java
package pe.rikos.operacion.venta.aplicacion;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.rikos.contratos.Resultado;
import pe.rikos.operacion.pensionistas.aplicacion.ConsultaPensionistas;
import pe.rikos.operacion.pensionistas.infraestructura.Pensionista;
import pe.rikos.operacion.venta.infraestructura.CuentaPersona;
import pe.rikos.operacion.venta.infraestructura.CuentaPersonaRepositorio;
import pe.rikos.operacion.venta.infraestructura.DetallePedidoRepositorio;
import pe.rikos.operacion.venta.infraestructura.PedidoRepositorio;

/**
 * El mozo busca al pensionista (por nombre o DNI) y lo asigna a una persona de la mesa. Paso provisional
 * hasta el ticket del sprint 8. Un inactivo no se asigna (RN-034); uno ya presente en el pedido, tampoco;
 * y una persona con lineas no cambia de tipo aqui (el recalculo es de la entrega 4, US-029 CA5).
 */
@Service
public class AsignarPensionista {

    private final PedidoRepositorio pedidos;
    private final CuentaPersonaRepositorio personas;
    private final DetallePedidoRepositorio lineas;
    private final ConsultaPensionistas consultaPensionistas;

    public AsignarPensionista(PedidoRepositorio pedidos, CuentaPersonaRepositorio personas,
                              DetallePedidoRepositorio lineas, ConsultaPensionistas consultaPensionistas) {
        this.pedidos = pedidos;
        this.personas = personas;
        this.lineas = lineas;
        this.consultaPensionistas = consultaPensionistas;
    }

    @Transactional
    public Resultado<CuentaPersona, ErrorPedido> ejecutar(String pedidoId, String personaId, String pensionistaId,
                                                          String actorId) {
        String localId = LocalDeSesion.actual().orElse(null);
        if (localId == null) {
            return Resultado.fracaso(ErrorPedido.SIN_LOCAL_ACTIVO);
        }
        if (pedidos.findByIdAndLocalId(pedidoId, localId).isEmpty()) {
            return Resultado.fracaso(ErrorPedido.NO_ENCONTRADO);
        }
        CuentaPersona persona = personas.findByIdAndPedidoId(personaId, pedidoId).orElse(null);
        if (persona == null) {
            return Resultado.fracaso(ErrorPedido.PERSONA_NO_ENCONTRADA);
        }
        Pensionista pensionista = consultaPensionistas.pensionista(pensionistaId).orElse(null);
        if (pensionista == null) {
            return Resultado.fracaso(ErrorPedido.PENSIONISTA_NO_ENCONTRADO);
        }
        if (!pensionista.isActivo()) {
            return Resultado.fracaso(ErrorPedido.PENSIONISTA_INACTIVO);
        }
        if (personas.existsByPedidoIdAndPensionistaId(pedidoId, pensionistaId)) {
            return Resultado.fracaso(ErrorPedido.PENSIONISTA_YA_EN_EL_PEDIDO);
        }
        if (lineas.existsByCuentaPersonaId(personaId)) {
            return Resultado.fracaso(ErrorPedido.PERSONA_CON_LINEAS);
        }
        persona.asignarPensionista(pensionistaId, pensionista.nombreCompleto());
        return Resultado.exito(personas.save(persona));
    }
}
```

`QuitarPersona.java`:
```java
package pe.rikos.operacion.venta.aplicacion;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.rikos.contratos.Resultado;
import pe.rikos.operacion.venta.infraestructura.CuentaPersona;
import pe.rikos.operacion.venta.infraestructura.CuentaPersonaRepositorio;
import pe.rikos.operacion.venta.infraestructura.DetallePedidoRepositorio;
import pe.rikos.operacion.venta.infraestructura.PedidoRepositorio;

/** Quita una persona sin lineas. Un pedido nunca se queda sin personas. */
@Service
public class QuitarPersona {

    private final PedidoRepositorio pedidos;
    private final CuentaPersonaRepositorio personas;
    private final DetallePedidoRepositorio lineas;

    public QuitarPersona(PedidoRepositorio pedidos, CuentaPersonaRepositorio personas, DetallePedidoRepositorio lineas) {
        this.pedidos = pedidos;
        this.personas = personas;
        this.lineas = lineas;
    }

    @Transactional
    public Resultado<Void, ErrorPedido> ejecutar(String pedidoId, String personaId) {
        String localId = LocalDeSesion.actual().orElse(null);
        if (localId == null) {
            return Resultado.fracaso(ErrorPedido.SIN_LOCAL_ACTIVO);
        }
        if (pedidos.findByIdAndLocalId(pedidoId, localId).isEmpty()) {
            return Resultado.fracaso(ErrorPedido.NO_ENCONTRADO);
        }
        CuentaPersona persona = personas.findByIdAndPedidoId(personaId, pedidoId).orElse(null);
        if (persona == null) {
            return Resultado.fracaso(ErrorPedido.PERSONA_NO_ENCONTRADA);
        }
        if (lineas.existsByCuentaPersonaId(personaId) || personas.findByPedidoIdOrderByOrden(pedidoId).size() <= 1) {
            return Resultado.fracaso(ErrorPedido.PERSONA_CON_LINEAS);
        }
        personas.delete(persona);
        return Resultado.exito(null);
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: mismo comando del Step 2. Expected: los tests que no usan `AgregarLinea` real pasan; **`no_se_asigna_un_pensionista_a_una_persona_que_ya_tiene_lineas` y `quita_una_persona_sin_lineas…` seguirán fallando** (`UnsupportedOperationException` del esqueleto) hasta la tarea 6, donde deben quedar en verde. Anotar en el libro: `Task 4: parcial (2 tests dependen de Task 6)`.

- [ ] **Step 5: Commit** (solo si el usuario lo pide)

---

### Task 5: El precio de una línea

**Files:**
- Create: `.../venta/aplicacion/CalculadorDePrecioDeLinea.java`
- Modify: `.../venta/infraestructura/DetallePedidoRepositorio.java` (consulta del segundo consumo)
- Test: `.../venta/aplicacion/PreciosDeLineaTest.java`

**Interfaces:**
- Consumes: `MotorDePrecios.calcular(SolicitudDePrecio) → Resultado<PrecioCalculado, ErrorPrecio>`, `SolicitudDePrecio(TipoLinea tipo, Comida comida, Long precioNormal, String planId, boolean segundoDelDia, boolean paraLlevar, LocalDate fecha)`, `pensionistas.aplicacion.TipoLinea {MENU_DEL_DIA, MENU_EXTRA, AGREGADO}`, `Comida.desde(String)`, `Pensionista.getPlanTarifarioId()`.
- Produces: `record PrecioDeLinea(long precio, long taperUnitario, String tarifaOrigen, String planId, boolean segundoDelDia)` y `CalculadorDePrecioDeLinea.calcular(TipoDeLinea tipo, String servicio, Long precioNormal, CuentaPersona persona, boolean paraLlevar, LocalDate hoy) → Resultado<PrecioDeLinea, ErrorPedido>`.
  - `PRODUCTO` y `EXTRA_LIBRE` → motor `AGREGADO` con `precioNormal` (sin táper); origen `publico` / `manual`; sin `precioNormal` → `SIN_PRECIO`.
  - `MENU_EXTRA` → motor `MENU_EXTRA`, `precioNormal` = precio del producto menú extra; con táper si es para llevar; origen `extra`.
  - `MENU_NORMAL` y `PLAN_*` → motor `MENU_DEL_DIA` con `comida` = servicio; persona pensionista → `planId` y `segundoDelDia`; origen `plan` si la persona es pensionista y el precio no cayó a normal, si no `publico`; falta el precio del plan → `PLAN_SIN_PRECIO`; falta el normal → `SIN_PRECIO`.
- Produces (repositorio): `DetallePedidoRepositorio.consumosDelPlanDelDia(String pensionistaId, LocalDate fecha, String servicio) → long`: cuenta las líneas `menu_normal`, `plan_desayuno` y `plan_cena` en estado distinto de `anulado` de esa persona en pedidos de ese día y servicio, **de cualquier local**.

- [ ] **Step 1: Write the failing test**

```java
package pe.rikos.operacion.venta.aplicacion;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;
import pe.rikos.contratos.Resultado;
import pe.rikos.operacion.comun.auditoria.ContextoDeSeguridadDePrueba;
import pe.rikos.operacion.comun.tiempo.ConfiguracionReloj;
import pe.rikos.operacion.pensionistas.aplicacion.CambiarParametro;
import pe.rikos.operacion.pensionistas.aplicacion.ClaveParametro;
import pe.rikos.operacion.pensionistas.aplicacion.ReglaSegundoConsumo;
import pe.rikos.operacion.pensionistas.infraestructura.Pensionista;
import pe.rikos.operacion.pensionistas.infraestructura.PlanTarifario;
import pe.rikos.operacion.venta.infraestructura.CuentaPersona;
import pe.rikos.operacion.venta.infraestructura.CuentaPersonaRepositorio;
import pe.rikos.operacion.venta.infraestructura.Pedido;

import java.time.Clock;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * 3.4 del sprint 6: el precio de una linea sale SIEMPRE del motor. "Hoy" es el miercoles 11/06/2031.
 * Plan del ejemplo: desayuno 500, almuerzo 800, cena 800. Taper: 100 (se fija en cada prueba que lo usa).
 */
@SpringBootTest
@Transactional
class PreciosDeLineaTest extends PedidosBase {

    private static final LocalDate HOY = LocalDate.of(2031, 6, 11);

    @MockitoBean
    Clock reloj;

    @Autowired
    CalculadorDePrecioDeLinea calculador;
    @Autowired
    AbrirPedido abrirPedido;
    @Autowired
    AsignarPensionista asignarPensionista;
    @Autowired
    CuentaPersonaRepositorio personas;
    @Autowired
    CambiarParametro cambiarParametro;

    String localA;
    PlanTarifario plan;
    Pedido pedido;
    CuentaPersona persona;

    @BeforeEach
    void preparar() {
        when(reloj.getZone()).thenReturn(ConfiguracionReloj.ZONA_DEL_RESTAURANTE);
        when(reloj.instant()).thenReturn(HOY.atTime(12, 0).atZone(ConfiguracionReloj.ZONA_DEL_RESTAURANTE).toInstant());
        // El plan rige desde el 1 de junio: se crea con el reloj ya fijado en esa fecha.
        ContextoDeSeguridadDePrueba.autenticarComo(ADMIN_SEMBRADO_ID);
        localA = nuevoLocal("pl");
        plan = nuevoPlan(500, 800, 800);
        cambiarParametro.ejecutar(ClaveParametro.TAPER_PRECIO, "100", ADMIN_SEMBRADO_ID);
        cambiarParametro.ejecutar(ClaveParametro.SEGUNDO_CONSUMO_REGLA, ReglaSegundoConsumo.PRECIO_DEL_PLAN.codigo(),
                ADMIN_SEMBRADO_ID);
        ContextoDeSeguridadDePrueba.autenticarComo(ADMIN_SEMBRADO_ID, localA);
        pedido = abrirPedido.ejecutar(null, Canal.MESA, nuevaMesa("1"), "almuerzo", ADMIN_SEMBRADO_ID).valorOLanza();
        persona = personas.findByPedidoIdOrderByOrden(pedido.getId()).get(0);
    }

    @AfterEach
    void limpiarContexto() {
        ContextoDeSeguridadDePrueba.limpiar();
    }

    private PrecioDeLinea precio(TipoDeLinea tipo, Long normal, boolean paraLlevar) {
        return calculador.calcular(tipo, "almuerzo", normal, persona, paraLlevar, HOY).valorOLanza();
    }

    @Test
    void un_cliente_normal_paga_el_precio_normal_del_menu_y_de_la_carta() {
        assertThat(precio(TipoDeLinea.MENU_NORMAL, 1200L, false).precio()).isEqualTo(1200L);
        assertThat(precio(TipoDeLinea.MENU_NORMAL, 1200L, false).tarifaOrigen()).isEqualTo("publico");
        assertThat(precio(TipoDeLinea.PRODUCTO, 500L, false).precio()).isEqualTo(500L);
    }

    @Test
    void un_pensionista_paga_el_precio_de_su_plan_en_el_menu_pero_normal_en_la_carta_y_en_el_extra() {
        Pensionista luis = nuevoPensionista(plan, "Plan");
        persona = asignarPensionista.ejecutar(pedido.getId(), persona.getId(), luis.getId(), ADMIN_SEMBRADO_ID)
                .valorOLanza();

        PrecioDeLinea menu = precio(TipoDeLinea.MENU_NORMAL, 1200L, false);
        assertThat(menu.precio()).isEqualTo(800L);
        assertThat(menu.tarifaOrigen()).isEqualTo("plan");
        assertThat(menu.planId()).isEqualTo(plan.getId());
        assertThat(precio(TipoDeLinea.MENU_EXTRA, 1800L, false).precio()).isEqualTo(1800L);
        assertThat(precio(TipoDeLinea.MENU_EXTRA, 1800L, false).tarifaOrigen()).isEqualTo("extra");
        assertThat(precio(TipoDeLinea.PRODUCTO, 500L, false).precio()).isEqualTo(500L);
    }

    @Test
    void el_taper_va_por_menu_solo_para_llevar_y_no_en_la_carta() {
        assertThat(precio(TipoDeLinea.MENU_NORMAL, 1200L, true).taperUnitario()).isEqualTo(100L);
        assertThat(precio(TipoDeLinea.MENU_EXTRA, 1800L, true).taperUnitario()).isEqualTo(100L);
        assertThat(precio(TipoDeLinea.PRODUCTO, 500L, true).taperUnitario()).isZero();
        assertThat(precio(TipoDeLinea.MENU_NORMAL, 1200L, false).taperUnitario()).isZero();
    }

    @Test
    void sin_precio_normal_no_hay_precio_y_nunca_sale_cero() {
        assertThat(errorDe(calculador.calcular(TipoDeLinea.PRODUCTO, "almuerzo", null, persona, false, HOY)))
                .isEqualTo(ErrorPedido.SIN_PRECIO);
        assertThat(errorDe(calculador.calcular(TipoDeLinea.MENU_NORMAL, "almuerzo", null, persona, false, HOY)))
                .isEqualTo(ErrorPedido.SIN_PRECIO);
    }

    @Test
    void el_desayuno_y_la_cena_del_plan_usan_el_precio_del_plan_de_esa_comida() {
        Pensionista luis = nuevoPensionista(plan, "Plan");
        persona = asignarPensionista.ejecutar(pedido.getId(), persona.getId(), luis.getId(), ADMIN_SEMBRADO_ID)
                .valorOLanza();

        assertThat(calculador.calcular(TipoDeLinea.PLAN_DESAYUNO, "desayuno", null, persona, false, HOY)
                .valorOLanza().precio()).isEqualTo(500L);
        assertThat(calculador.calcular(TipoDeLinea.PLAN_CENA, "cena", null, persona, false, HOY)
                .valorOLanza().precio()).isEqualTo(800L);
    }

    @Test
    void un_plan_sin_precio_para_esa_comida_avisa_en_vez_de_inventar_uno() {
        PlanTarifario sinDesayuno = crearPlan.ejecutar("Plan sin desayuno " + System.nanoTime(), null,
                new pe.rikos.operacion.pensionistas.aplicacion.PreciosPlan(null, 800L, 800L), null, ADMIN_SEMBRADO_ID)
                .valorOLanza();
        Pensionista luis = nuevoPensionista(sinDesayuno, "SinDes");
        persona = asignarPensionista.ejecutar(pedido.getId(), persona.getId(), luis.getId(), ADMIN_SEMBRADO_ID)
                .valorOLanza();

        assertThat(errorDe(calculador.calcular(TipoDeLinea.PLAN_DESAYUNO, "desayuno", null, persona, false, HOY)))
                .isEqualTo(ErrorPedido.PLAN_SIN_PRECIO);
    }

    static ErrorPedido errorDe(Resultado<?, ErrorPedido> resultado) {
        assertThat(resultado.esExito()).isFalse();
        return ((Resultado.Fracaso<?, ErrorPedido>) resultado).error();
    }
}
```

(El caso del **segundo consumo** y de su conteo entre locales necesita líneas guardadas: se prueba en la tarea 6, `LineasDelPedidoTest`.)

- [ ] **Step 2: Run test to verify it fails**

Run: `./mvnw -q -pl servicio-operacion -am test -Dtest=PreciosDeLineaTest -Dsurefire.failIfNoSpecifiedTests=false`
Expected: compilation FAIL, `CalculadorDePrecioDeLinea` y `PrecioDeLinea` no existen.

- [ ] **Step 3: Write minimal implementation**

`DetallePedidoRepositorio` — agregar:
```java
    /**
     * El segundo consumo del dia (3.4): cuantas comidas del plan (menu normal, desayuno o cena del plan) ya
     * pidio este pensionista ese dia y ese servicio, en CUALQUIER local (su cuenta es una sola) y sin
     * contar las anuladas.
     */
    @org.springframework.data.jpa.repository.Query("""
            select count(d) from DetallePedido d, CuentaPersona c, Pedido p
            where d.cuentaPersonaId = c.id and c.pedidoId = p.id
              and c.pensionistaId = :pensionistaId and p.fechaOperativa = :fecha and p.servicio = :servicio
              and d.tipoLinea in ('menu_normal', 'plan_desayuno', 'plan_cena') and d.estado <> 'anulado'
            """)
    long consumosDelPlanDelDia(String pensionistaId, java.time.LocalDate fecha, String servicio);
```

`CalculadorDePrecioDeLinea.java`:
```java
package pe.rikos.operacion.venta.aplicacion;

import org.springframework.stereotype.Component;
import pe.rikos.contratos.Resultado;
import pe.rikos.operacion.pensionistas.aplicacion.AvisoPrecio;
import pe.rikos.operacion.pensionistas.aplicacion.Comida;
import pe.rikos.operacion.pensionistas.aplicacion.MotorDePrecios;
import pe.rikos.operacion.pensionistas.aplicacion.PrecioCalculado;
import pe.rikos.operacion.pensionistas.aplicacion.SolicitudDePrecio;
import pe.rikos.operacion.pensionistas.aplicacion.TipoLinea;
import pe.rikos.operacion.pensionistas.infraestructura.Pensionista;
import pe.rikos.operacion.venta.infraestructura.CuentaPersona;
import pe.rikos.operacion.venta.infraestructura.DetallePedidoRepositorio;

import java.time.LocalDate;

/**
 * Traduce una linea del pedido a la pregunta que entiende el motor de precios del sprint 5 y devuelve el
 * precio, el taper por unidad, de donde salio y si fue el segundo consumo del dia. El mozo nunca escribe un
 * precio: solo el "extra libre" trae el suyo, que aqui entra como precio normal de un agregado.
 */
@Component
public class CalculadorDePrecioDeLinea {

    private final MotorDePrecios motor;
    private final DetallePedidoRepositorio lineas;
    private final pe.rikos.operacion.pensionistas.aplicacion.ConsultaPensionistas consultaPensionistas;

    public CalculadorDePrecioDeLinea(MotorDePrecios motor, DetallePedidoRepositorio lineas,
                                     pe.rikos.operacion.pensionistas.aplicacion.ConsultaPensionistas consultaPensionistas) {
        this.motor = motor;
        this.lineas = lineas;
        this.consultaPensionistas = consultaPensionistas;
    }

    public Resultado<PrecioDeLinea, ErrorPedido> calcular(TipoDeLinea tipo, String servicio, Long precioNormal,
                                                          CuentaPersona persona, boolean paraLlevar, LocalDate hoy) {
        TipoLinea tipoMotor = switch (tipo) {
            case PRODUCTO, EXTRA_LIBRE -> TipoLinea.AGREGADO;
            case MENU_EXTRA -> TipoLinea.MENU_EXTRA;
            case MENU_NORMAL, PLAN_DESAYUNO, PLAN_CENA -> TipoLinea.MENU_DEL_DIA;
        };
        String planId = null;
        boolean segundo = false;
        if (tipoMotor == TipoLinea.MENU_DEL_DIA && persona.getPensionistaId() != null) {
            Pensionista pensionista = consultaPensionistas.pensionista(persona.getPensionistaId()).orElse(null);
            planId = pensionista == null ? null : pensionista.getPlanTarifarioId();
            segundo = lineas.consumosDelPlanDelDia(persona.getPensionistaId(), hoy, servicio) > 0;
        }
        var comida = Comida.desde(servicio).orElse(Comida.ALMUERZO);
        var respuesta = motor.calcular(new SolicitudDePrecio(tipoMotor, comida, precioNormal, planId, segundo,
                paraLlevar, hoy));
        if (!respuesta.esExito()) {
            return Resultado.fracaso(ErrorPedido.SIN_PRECIO);
        }
        PrecioCalculado calculado = respuesta.valorOLanza();
        if (calculado.avisos().contains(AvisoPrecio.PLAN_SIN_PRECIO)) {
            return Resultado.fracaso(ErrorPedido.PLAN_SIN_PRECIO);
        }
        if (calculado.precio() == null) {
            return Resultado.fracaso(ErrorPedido.SIN_PRECIO);
        }
        String origen = switch (tipo) {
            case EXTRA_LIBRE -> "manual";
            case MENU_EXTRA -> "extra";
            case PRODUCTO -> "publico";
            default -> planId != null && !precioNormalAplicado(calculado, precioNormal) ? "plan" : "publico";
        };
        return Resultado.exito(new PrecioDeLinea(calculado.precio(), calculado.taper() == null ? 0L : calculado.taper(),
                origen, "plan".equals(origen) ? planId : null, segundo && planId != null));
    }

    /** El motor cae al precio normal (persona sin plan o segundo consumo configurado a normal). */
    private static boolean precioNormalAplicado(PrecioCalculado calculado, Long precioNormal) {
        return precioNormal != null && calculado.precio().equals(precioNormal);
    }
}
```

`PrecioDeLinea.java`:
```java
package pe.rikos.operacion.venta.aplicacion;

/** El resultado de fijar el precio de una linea: por unidad y en centimos. tarifaOrigen: publico | plan | extra | manual. */
public record PrecioDeLinea(long precio, long taperUnitario, String tarifaOrigen, String planId, boolean segundoDelDia) {
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: mismo comando del Step 2. Expected: `PreciosDeLineaTest` `Tests run: 6, Failures: 0`. Si `ClaveParametro.SEGUNDO_CONSUMO_REGLA` o `ReglaSegundoConsumo.PRECIO_DEL_PLAN.codigo()` tienen otro nombre, ajustar al del sprint 5 (`ClaveParametro`, `ReglaSegundoConsumo`).

- [ ] **Step 5: Commit** (solo si el usuario lo pide)

---

### Task 6: Agregar líneas (carta, menú del día, plan y extra)

**Files:**
- Create: `.../venta/aplicacion/{SolicitudDeLinea,ComponenteElegido,AgregarLinea}.java`
- Modify: `.../catalogo/infraestructura/ProductoRepositorio.java` (carta), y reemplazar el esqueleto de `AgregarLinea` de la tarea 4
- Test: `.../venta/aplicacion/LineasDelPedidoTest.java`, `MenuDelDiaEnPedidoTest.java`

**Interfaces:**
- Consumes: `CalculadorDePrecioDeLinea`, `ConsultaMenu.menuConfirmadoDel(LocalDate) → Optional<List<OpcionMenu>>`, `ConsultaDisponibilidad.estaAgotado(String localId, String productoId)`, `ProductoRepositorio.findById/findByTipoMenu`, `TipoMenu`, `RegistradorAuditoria`.
- Produces: `record ComponenteElegido(String componente, String opcionMenuId)`; `record SolicitudDeLinea(String id, String pedidoId, String personaId, TipoDeLinea tipo, String productoId, int cantidad, String observacion, List<ComponenteElegido> componentes, String descripcion, Long precioManual)`; `AgregarLinea.ejecutar(SolicitudDeLinea, String actorId) → Resultado<DetallePedido, ErrorPedido>`.
- Reglas (spec 3.4 y 3.11):
  - `cantidad ≥ 1`; observación ≤ 250; el pedido debe estar `abierto` o `comandado` (agregar tras comandar es válido; genera otra comanda en la entrega 3).
  - `PRODUCTO`: producto vendible, activo, sin `tipoMenu`, **no agotado en el local**.
  - `MENU_NORMAL` / `MENU_EXTRA`: el servicio del pedido debe ser `almuerzo` (si no, `MENU_FUERA_DE_SERVICIO`); menú confirmado de hoy con opciones (si no, `MENU_NO_PROGRAMADO`); producto = `findByTipoMenu`; por **cada componente** de `TipoMenu.componentes()` con opciones ese día: una sola opción → se asigna sola; varias → `componentes` debe traer la elegida (`COMPONENTE_OBLIGATORIO` si falta, `OPCION_INVALIDA` si no es de hoy o es de otro componente); el `segundo` del extra sale de las opciones `extra`, el resto de las `normal`; se guarda una fila por componente. Cantidad 1 si la persona es pensionista (`CANTIDAD_INVALIDA`).
  - `PLAN_DESAYUNO` / `PLAN_CENA`: solo persona pensionista (`PERSONA_NO_ES_PENSIONISTA`), y el servicio del pedido debe coincidir (`PLAN_FUERA_DE_SERVICIO`); sin producto; cantidad 1.
  - `EXTRA_LIBRE`: `descripcion` obligatoria (≤ 120) y `precioManual > 0` (`DESCRIPCION_OBLIGATORIA`, `PRECIO_INVALIDO`); sin producto; queda en la bitácora con evento `precio_manual`.
  - Idempotente por `id`: si la línea ya existe en esa persona, se devuelve.
  - Pedido o persona de otro local → `NO_ENCONTRADO` / `PERSONA_NO_ENCONTRADA`.

- [ ] **Step 1: Write the failing tests**

`LineasDelPedidoTest.java`:

```java
package pe.rikos.operacion.venta.aplicacion;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;
import pe.rikos.contratos.Identificador;
import pe.rikos.contratos.Resultado;
import pe.rikos.operacion.catalogo.aplicacion.MarcarAgotado;
import pe.rikos.operacion.comun.auditoria.ContextoDeSeguridadDePrueba;
import pe.rikos.operacion.comun.tiempo.ConfiguracionReloj;
import pe.rikos.operacion.pensionistas.aplicacion.CambiarParametro;
import pe.rikos.operacion.pensionistas.aplicacion.ClaveParametro;
import pe.rikos.operacion.pensionistas.aplicacion.ReglaSegundoConsumo;
import pe.rikos.operacion.pensionistas.infraestructura.Pensionista;
import pe.rikos.operacion.pensionistas.infraestructura.PlanTarifario;
import pe.rikos.operacion.venta.infraestructura.CuentaPersona;
import pe.rikos.operacion.venta.infraestructura.CuentaPersonaRepositorio;
import pe.rikos.operacion.venta.infraestructura.DetallePedido;
import pe.rikos.operacion.venta.infraestructura.Pedido;
import pe.rikos.operacion.comun.auditoria.AuditoriaRepositorio;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/** US-028: agregar lineas de carta, plan y extra, con el precio del motor. "Hoy" es el miercoles 11/06/2031. */
@SpringBootTest
@Transactional
class LineasDelPedidoTest extends PedidosBase {

    private static final LocalDate HOY = LocalDate.of(2031, 6, 11);

    @MockitoBean
    Clock reloj;

    @Autowired
    AbrirPedido abrirPedido;
    @Autowired
    AsignarPensionista asignarPensionista;
    @Autowired
    AgregarPersona agregarPersona;
    @Autowired
    AgregarLinea agregarLinea;
    @Autowired
    CuentaPersonaRepositorio personas;
    @Autowired
    MarcarAgotado marcarAgotado;
    @Autowired
    CambiarParametro cambiarParametro;
    @Autowired
    AuditoriaRepositorio auditorias;

    String localA;
    String localB;
    PlanTarifario plan;
    Pedido pedido;
    String persona;

    @BeforeEach
    void preparar() {
        when(reloj.getZone()).thenReturn(ConfiguracionReloj.ZONA_DEL_RESTAURANTE);
        when(reloj.instant()).thenReturn(HOY.atTime(12, 0).atZone(ConfiguracionReloj.ZONA_DEL_RESTAURANTE).toInstant());
        ContextoDeSeguridadDePrueba.autenticarComo(ADMIN_SEMBRADO_ID);
        localA = nuevoLocal("la");
        localB = nuevoLocal("lb");
        plan = nuevoPlan(500, 800, 800);
        cambiarParametro.ejecutar(ClaveParametro.TAPER_PRECIO, "100", ADMIN_SEMBRADO_ID);
        cambiarParametro.ejecutar(ClaveParametro.SEGUNDO_CONSUMO_REGLA, ReglaSegundoConsumo.PRECIO_DEL_PLAN.codigo(),
                ADMIN_SEMBRADO_ID);
        ContextoDeSeguridadDePrueba.autenticarComo(ADMIN_SEMBRADO_ID, localA);
        pedido = abrirPedido.ejecutar(null, Canal.MESA, nuevaMesa("1"), "almuerzo", ADMIN_SEMBRADO_ID).valorOLanza();
        persona = personas.findByPedidoIdOrderByOrden(pedido.getId()).get(0).getId();
    }

    @AfterEach
    void limpiarContexto() {
        ContextoDeSeguridadDePrueba.limpiar();
    }

    static ErrorPedido error(Resultado<?, ErrorPedido> resultado) {
        assertThat(resultado.esExito()).isFalse();
        return ((Resultado.Fracaso<?, ErrorPedido>) resultado).error();
    }

    SolicitudDeLinea carta(String productoId, int cantidad) {
        return new SolicitudDeLinea(null, pedido.getId(), persona, TipoDeLinea.PRODUCTO, productoId, cantidad, null,
                List.of(), null, null);
    }

    @Test
    void agrega_un_producto_de_la_carta_con_su_precio_normal_y_su_observacion() {
        var chicha = productoDeCarta("Chicha", 500L);

        DetallePedido linea = agregarLinea.ejecutar(new SolicitudDeLinea(null, pedido.getId(), persona,
                TipoDeLinea.PRODUCTO, chicha.getId(), 2, "  Sin hielo ", List.of(), null, null), ADMIN_SEMBRADO_ID)
                .valorOLanza();

        assertThat(linea.getPrecioUnitario()).isEqualTo(500L);
        assertThat(linea.getTarifaOrigen()).isEqualTo("publico");
        assertThat(linea.getCantidad()).isEqualTo(2);
        assertThat(linea.getObservacion()).isEqualTo("Sin hielo");
        assertThat(linea.getEstado()).isEqualTo("pendiente");
    }

    @Test
    void un_producto_sin_precio_un_no_vendible_uno_del_menu_o_inexistente_no_se_agrega() {
        var sinPrecio = productoDeCarta("Sin precio", null);

        assertThat(error(agregarLinea.ejecutar(carta(sinPrecio.getId(), 1), ADMIN_SEMBRADO_ID)))
                .isEqualTo(ErrorPedido.SIN_PRECIO);
        assertThat(error(agregarLinea.ejecutar(carta("no-existe", 1), ADMIN_SEMBRADO_ID)))
                .isEqualTo(ErrorPedido.PRODUCTO_NO_ENCONTRADO);
        var insumo = productos.findAll().stream().filter(p -> !p.isEsVendible()).findFirst().orElseThrow();
        assertThat(error(agregarLinea.ejecutar(carta(insumo.getId(), 1), ADMIN_SEMBRADO_ID)))
                .isEqualTo(ErrorPedido.PRODUCTO_NO_VENDIBLE);
        var menuNormal = productos.findByTipoMenu("normal").orElseThrow();
        assertThat(error(agregarLinea.ejecutar(carta(menuNormal.getId(), 1), ADMIN_SEMBRADO_ID)))
                .isEqualTo(ErrorPedido.PRODUCTO_NO_VENDIBLE);
    }

    @Test
    void cantidad_cero_o_negativa_y_observacion_larga_se_rechazan() {
        var chicha = productoDeCarta("Chicha", 500L);

        assertThat(error(agregarLinea.ejecutar(carta(chicha.getId(), 0), ADMIN_SEMBRADO_ID)))
                .isEqualTo(ErrorPedido.CANTIDAD_INVALIDA);
        assertThat(error(agregarLinea.ejecutar(carta(chicha.getId(), -1), ADMIN_SEMBRADO_ID)))
                .isEqualTo(ErrorPedido.CANTIDAD_INVALIDA);
        assertThat(error(agregarLinea.ejecutar(new SolicitudDeLinea(null, pedido.getId(), persona, TipoDeLinea.PRODUCTO,
                chicha.getId(), 1, "x".repeat(251), List.of(), null, null), ADMIN_SEMBRADO_ID)))
                .isEqualTo(ErrorPedido.OBSERVACION_LARGA);
    }

    @Test
    void un_producto_agotado_en_este_local_no_se_ofrece_pero_en_otro_local_si() {
        var chicha = productoDeCarta("Chicha", 500L);
        marcarAgotado.ejecutar(chicha.getId()).valorOLanza();

        assertThat(error(agregarLinea.ejecutar(carta(chicha.getId(), 1), ADMIN_SEMBRADO_ID)))
                .isEqualTo(ErrorPedido.PRODUCTO_AGOTADO);

        ContextoDeSeguridadDePrueba.autenticarComo(ADMIN_SEMBRADO_ID, localB);
        Pedido deB = abrirPedido.ejecutar(null, Canal.PARA_LLEVAR, null, "almuerzo", ADMIN_SEMBRADO_ID).valorOLanza();
        String personaB = personas.findByPedidoIdOrderByOrden(deB.getId()).get(0).getId();
        assertThat(agregarLinea.ejecutar(new SolicitudDeLinea(null, deB.getId(), personaB, TipoDeLinea.PRODUCTO,
                chicha.getId(), 1, null, List.of(), null, null), ADMIN_SEMBRADO_ID).esExito()).isTrue();
    }

    @Test
    void enviar_dos_veces_el_mismo_id_de_linea_no_la_duplica() {
        var chicha = productoDeCarta("Chicha", 500L);
        String id = Identificador.nuevo().valor();
        var solicitud = new SolicitudDeLinea(id, pedido.getId(), persona, TipoDeLinea.PRODUCTO, chicha.getId(), 1, null,
                List.of(), null, null);

        DetallePedido primera = agregarLinea.ejecutar(solicitud, ADMIN_SEMBRADO_ID).valorOLanza();
        DetallePedido repetida = agregarLinea.ejecutar(solicitud, ADMIN_SEMBRADO_ID).valorOLanza();

        assertThat(repetida.getId()).isEqualTo(primera.getId());
    }

    @Test
    void un_pedido_o_una_persona_de_otro_local_no_se_encuentra() {
        var chicha = productoDeCarta("Chicha", 500L);
        ContextoDeSeguridadDePrueba.autenticarComo(ADMIN_SEMBRADO_ID, localB);

        assertThat(error(agregarLinea.ejecutar(carta(chicha.getId(), 1), ADMIN_SEMBRADO_ID)))
                .isEqualTo(ErrorPedido.NO_ENCONTRADO);
    }

    @Test
    void la_comida_del_plan_es_solo_para_pensionistas_y_solo_en_su_servicio() {
        Pensionista luis = nuevoPensionista(plan, "Plan");
        Pedido desayuno = abrirPedido.ejecutar(null, Canal.PARA_LLEVAR, null, "desayuno", ADMIN_SEMBRADO_ID).valorOLanza();
        String personaDesayuno = personas.findByPedidoIdOrderByOrden(desayuno.getId()).get(0).getId();

        var comoNormal = new SolicitudDeLinea(null, desayuno.getId(), personaDesayuno, TipoDeLinea.PLAN_DESAYUNO, null, 1,
                null, List.of(), null, null);
        assertThat(error(agregarLinea.ejecutar(comoNormal, ADMIN_SEMBRADO_ID))).isEqualTo(ErrorPedido.PERSONA_NO_ES_PENSIONISTA);

        asignarPensionista.ejecutar(desayuno.getId(), personaDesayuno, luis.getId(), ADMIN_SEMBRADO_ID).valorOLanza();
        DetallePedido linea = agregarLinea.ejecutar(comoNormal, ADMIN_SEMBRADO_ID).valorOLanza();
        assertThat(linea.getPrecioUnitario()).isEqualTo(500L);
        assertThat(linea.getTarifaOrigen()).isEqualTo("plan");
        assertThat(linea.getTaperUnitario()).isZero();

        var cenaEnDesayuno = new SolicitudDeLinea(null, desayuno.getId(), personaDesayuno, TipoDeLinea.PLAN_CENA, null, 1,
                null, List.of(), null, null);
        assertThat(error(agregarLinea.ejecutar(cenaEnDesayuno, ADMIN_SEMBRADO_ID)))
                .isEqualTo(ErrorPedido.PLAN_FUERA_DE_SERVICIO);
    }

    @Test
    void el_segundo_consumo_del_dia_se_cuenta_entre_locales_y_no_cuenta_lo_quitado() {
        Pensionista luis = nuevoPensionista(plan, "Segundo");
        cambiarParametro.ejecutar(ClaveParametro.SEGUNDO_CONSUMO_REGLA, ReglaSegundoConsumo.PRECIO_NORMAL.codigo(),
                ADMIN_SEMBRADO_ID);
        // Primer plan del dia, en el local B.
        ContextoDeSeguridadDePrueba.autenticarComo(ADMIN_SEMBRADO_ID, localB);
        Pedido enB = abrirPedido.ejecutar(null, Canal.PARA_LLEVAR, null, "desayuno", ADMIN_SEMBRADO_ID).valorOLanza();
        String personaB = personas.findByPedidoIdOrderByOrden(enB.getId()).get(0).getId();
        asignarPensionista.ejecutar(enB.getId(), personaB, luis.getId(), ADMIN_SEMBRADO_ID).valorOLanza();
        DetallePedido primero = agregarLinea.ejecutar(new SolicitudDeLinea(null, enB.getId(), personaB,
                TipoDeLinea.PLAN_DESAYUNO, null, 1, null, List.of(), null, null), ADMIN_SEMBRADO_ID).valorOLanza();
        assertThat(primero.isSegundoDelDia()).isFalse();
        assertThat(primero.getPrecioUnitario()).isEqualTo(500L);

        // Segundo desayuno del mismo dia, ahora en el local A: es el segundo y la regla dice precio normal.
        ContextoDeSeguridadDePrueba.autenticarComo(ADMIN_SEMBRADO_ID, localA);
        Pedido enA = abrirPedido.ejecutar(null, Canal.PARA_LLEVAR, null, "desayuno", ADMIN_SEMBRADO_ID).valorOLanza();
        String personaA = personas.findByPedidoIdOrderByOrden(enA.getId()).get(0).getId();
        asignarPensionista.ejecutar(enA.getId(), personaA, luis.getId(), ADMIN_SEMBRADO_ID).valorOLanza();
        var segundo = new SolicitudDeLinea(null, enA.getId(), personaA, TipoDeLinea.PLAN_DESAYUNO, null, 1, null,
                List.of(), null, null);

        // Sin precio normal en una linea del plan, el "precio normal" no existe: el motor avisa en vez de inventarlo.
        assertThat(error(agregarLinea.ejecutar(segundo, ADMIN_SEMBRADO_ID))).isEqualTo(ErrorPedido.SIN_PRECIO);
    }

    @Test
    void el_extra_libre_lleva_descripcion_y_precio_del_mozo_y_queda_en_la_bitacora() {
        var solicitud = new SolicitudDeLinea(null, pedido.getId(), persona, TipoDeLinea.EXTRA_LIBRE, null, 1, null,
                List.of(), "  Huevo frito ", 300L);

        DetallePedido linea = agregarLinea.ejecutar(solicitud, ADMIN_SEMBRADO_ID).valorOLanza();

        assertThat(linea.getDescripcion()).isEqualTo("Huevo frito");
        assertThat(linea.getPrecioUnitario()).isEqualTo(300L);
        assertThat(linea.getTarifaOrigen()).isEqualTo("manual");
        assertThat(auditorias.findAll()).anyMatch(a -> "precio_manual".equals(a.getEvento())
                && linea.getId().equals(a.getEntidadId()));

        assertThat(error(agregarLinea.ejecutar(new SolicitudDeLinea(null, pedido.getId(), persona, TipoDeLinea.EXTRA_LIBRE,
                null, 1, null, List.of(), "  ", 300L), ADMIN_SEMBRADO_ID))).isEqualTo(ErrorPedido.DESCRIPCION_OBLIGATORIA);
        assertThat(error(agregarLinea.ejecutar(new SolicitudDeLinea(null, pedido.getId(), persona, TipoDeLinea.EXTRA_LIBRE,
                null, 1, null, List.of(), "Huevo", 0L), ADMIN_SEMBRADO_ID))).isEqualTo(ErrorPedido.PRECIO_INVALIDO);
        assertThat(error(agregarLinea.ejecutar(new SolicitudDeLinea(null, pedido.getId(), persona, TipoDeLinea.EXTRA_LIBRE,
                null, 1, null, List.of(), "Huevo", null), ADMIN_SEMBRADO_ID))).isEqualTo(ErrorPedido.PRECIO_INVALIDO);
    }
}
```

> **Aviso sobre `el_segundo_consumo…`:** la prueba fija a propósito el comportamiento del motor cuando la regla dice "precio normal" y la línea es del plan de desayuno (que **no tiene precio normal**): el motor devuelve "sin precio normal" y la línea **no se agrega**, en vez de cobrar cero. Si al ejecutarla se comprueba que el motor responde distinto (por ejemplo cobra el plan igual), registrar un `Ruling:` en el libro: el criterio es que **nunca** entre una línea sin precio.

`MenuDelDiaEnPedidoTest.java` (esqueleto de arranque idéntico a `LineasDelPedidoTest` — mismo reloj fijo `HOY`, mismo local, plan y pedido —; `@Autowired` extra: `CrearSemanaMenu crearSemana`, `AgregarOpcionMenu agregarOpcion`, `ConfirmarSemanaMenu confirmarSemana` de `catalogo.aplicacion`, `CambiarPrecioMenu cambiarPrecioMenu`). En `@BeforeEach`, tras abrir el pedido: `cambiarPrecioMenu.ejecutar("normal", 1200L, ADMIN_SEMBRADO_ID)` y `cambiarPrecioMenu.ejecutar("extra", 1800L, ADMIN_SEMBRADO_ID)`; y **antes** de abrir el pedido, programar y confirmar la semana que contiene a `HOY`:

```java
        SemanaMenu semana = crearSemana.ejecutar(LocalDate.of(2031, 6, 9), ADMIN_SEMBRADO_ID).valorOLanza();
        entrada = plato("Sopa de casa", CATEGORIA_ENTRADA);           // una sola opcion de entrada
        seco = plato("Seco de res", CATEGORIA_SEGUNDO);               // dos opciones de segundo normal
        estofado = plato("Estofado de res", CATEGORIA_SEGUNDO);
        lomo = plato("Lomo saltado", CATEGORIA_SEGUNDO);               // segundo del menu extra
        opcionEntrada = agregarOpcion.ejecutar(semana.getId(), HOY, entrada.getId(), "normal", null).valorOLanza();
        opcionSeco = agregarOpcion.ejecutar(semana.getId(), HOY, seco.getId(), "normal", null).valorOLanza();
        opcionEstofado = agregarOpcion.ejecutar(semana.getId(), HOY, estofado.getId(), "normal", null).valorOLanza();
        opcionLomo = agregarOpcion.ejecutar(semana.getId(), HOY, lomo.getId(), "extra", null).valorOLanza();
        confirmarSemana.ejecutar(semana.getId(), ADMIN_SEMBRADO_ID).valorOLanza();
```

(`SemanaMenu` y los `OpcionMenu` son de `catalogo.infraestructura`; guardar los campos como `OpcionMenu opcionEntrada, opcionSeco, opcionEstofado, opcionLomo`.) Casos:

```java
    SolicitudDeLinea menu(TipoDeLinea tipo, List<ComponenteElegido> elegidos) {
        return new SolicitudDeLinea(null, pedido.getId(), persona, tipo, null, 1, null, elegidos, null, null);
    }

    @Test
    void el_menu_normal_asigna_solo_el_componente_con_una_opcion_y_pide_elegir_el_que_tiene_varias() {
        assertThat(error(agregarLinea.ejecutar(menu(TipoDeLinea.MENU_NORMAL, List.of()), ADMIN_SEMBRADO_ID)))
                .isEqualTo(ErrorPedido.COMPONENTE_OBLIGATORIO);

        DetallePedido linea = agregarLinea.ejecutar(menu(TipoDeLinea.MENU_NORMAL,
                List.of(new ComponenteElegido("segundo", opcionSeco.getId()))), ADMIN_SEMBRADO_ID).valorOLanza();

        assertThat(linea.getPrecioUnitario()).isEqualTo(1200L);
        assertThat(linea.getTipoLinea()).isEqualTo("menu_normal");
        // La fila del componente con una sola opcion se guarda igual (RN-038).
        assertThat(componentes.findByDetallePedidoIdIn(List.of(linea.getId())))
                .extracting(DetallePedidoComponente::getComponente, DetallePedidoComponente::getProductoId)
                .containsExactlyInAnyOrder(tuple("entrada", entrada.getId()), tuple("segundo", seco.getId()));
    }

    @Test
    void elegir_una_opcion_que_no_es_de_hoy_ni_de_ese_componente_se_rechaza() {
        assertThat(error(agregarLinea.ejecutar(menu(TipoDeLinea.MENU_NORMAL,
                List.of(new ComponenteElegido("segundo", opcionEntrada.getId()))), ADMIN_SEMBRADO_ID)))
                .isEqualTo(ErrorPedido.OPCION_INVALIDA);
        assertThat(error(agregarLinea.ejecutar(menu(TipoDeLinea.MENU_NORMAL,
                List.of(new ComponenteElegido("segundo", "no-existe"))), ADMIN_SEMBRADO_ID)))
                .isEqualTo(ErrorPedido.OPCION_INVALIDA);
        // El segundo "extra" no es una opcion del menu normal.
        assertThat(error(agregarLinea.ejecutar(menu(TipoDeLinea.MENU_NORMAL,
                List.of(new ComponenteElegido("segundo", opcionLomo.getId()))), ADMIN_SEMBRADO_ID)))
                .isEqualTo(ErrorPedido.OPCION_INVALIDA);
    }

    @Test
    void el_menu_extra_toma_su_segundo_de_las_opciones_extra_y_se_cobra_completo_incluso_al_pensionista() {
        Pensionista luis = nuevoPensionista(plan, "Extra");
        asignarPensionista.ejecutar(pedido.getId(), persona, luis.getId(), ADMIN_SEMBRADO_ID).valorOLanza();

        DetallePedido linea = agregarLinea.ejecutar(menu(TipoDeLinea.MENU_EXTRA, List.of()), ADMIN_SEMBRADO_ID).valorOLanza();

        assertThat(linea.getPrecioUnitario()).isEqualTo(1800L);
        assertThat(linea.getTarifaOrigen()).isEqualTo("extra");
        assertThat(componentes.findByDetallePedidoIdIn(List.of(linea.getId())))
                .extracting(DetallePedidoComponente::getProductoId).contains(lomo.getId());
    }

    @Test
    void al_pensionista_el_menu_normal_le_sale_al_precio_de_su_plan_y_para_llevar_lleva_taper() {
        Pensionista luis = nuevoPensionista(plan, "Menu");
        asignarPensionista.ejecutar(pedido.getId(), persona, luis.getId(), ADMIN_SEMBRADO_ID).valorOLanza();
        Pedido paraLlevar = abrirPedido.ejecutar(null, Canal.PARA_LLEVAR, null, "almuerzo", ADMIN_SEMBRADO_ID).valorOLanza();
        String personaLlevar = personas.findByPedidoIdOrderByOrden(paraLlevar.getId()).get(0).getId();

        DetallePedido enMesa = agregarLinea.ejecutar(menu(TipoDeLinea.MENU_NORMAL,
                List.of(new ComponenteElegido("segundo", opcionSeco.getId()))), ADMIN_SEMBRADO_ID).valorOLanza();
        DetallePedido llevar = agregarLinea.ejecutar(new SolicitudDeLinea(null, paraLlevar.getId(), personaLlevar,
                TipoDeLinea.MENU_NORMAL, null, 2, null, List.of(new ComponenteElegido("segundo", opcionSeco.getId())),
                null, null), ADMIN_SEMBRADO_ID).valorOLanza();

        assertThat(enMesa.getPrecioUnitario()).isEqualTo(800L);
        assertThat(enMesa.getTarifaOrigen()).isEqualTo("plan");
        assertThat(enMesa.getTaperUnitario()).isZero();
        assertThat(llevar.getPrecioUnitario()).isEqualTo(1200L);   // cliente normal
        assertThat(llevar.getTaperUnitario()).isEqualTo(100L);     // por menu, para llevar
        assertThat(llevar.getCantidad()).isEqualTo(2);
    }

    @Test
    void un_pensionista_no_pide_dos_menus_en_una_sola_linea() {
        Pensionista luis = nuevoPensionista(plan, "Cantidad");
        asignarPensionista.ejecutar(pedido.getId(), persona, luis.getId(), ADMIN_SEMBRADO_ID).valorOLanza();

        assertThat(error(agregarLinea.ejecutar(new SolicitudDeLinea(null, pedido.getId(), persona,
                TipoDeLinea.MENU_NORMAL, null, 2, null, List.of(new ComponenteElegido("segundo", opcionSeco.getId())),
                null, null), ADMIN_SEMBRADO_ID))).isEqualTo(ErrorPedido.CANTIDAD_INVALIDA);
    }

    @Test
    void el_menu_solo_se_pide_en_el_almuerzo_y_solo_si_hay_un_menu_confirmado_hoy() {
        Pedido desayuno = abrirPedido.ejecutar(null, Canal.PARA_LLEVAR, null, "desayuno", ADMIN_SEMBRADO_ID).valorOLanza();
        String personaDesayuno = personas.findByPedidoIdOrderByOrden(desayuno.getId()).get(0).getId();
        assertThat(error(agregarLinea.ejecutar(new SolicitudDeLinea(null, desayuno.getId(), personaDesayuno,
                TipoDeLinea.MENU_NORMAL, null, 1, null, List.of(), null, null), ADMIN_SEMBRADO_ID)))
                .isEqualTo(ErrorPedido.MENU_FUERA_DE_SERVICIO);

        // Un dia sin programacion: cambiar "hoy" a una fecha sin semana confirmada.
        when(reloj.instant()).thenReturn(LocalDate.of(2031, 8, 6).atTime(12, 0)
                .atZone(ConfiguracionReloj.ZONA_DEL_RESTAURANTE).toInstant());
        Pedido sinMenu = abrirPedido.ejecutar(null, Canal.PARA_LLEVAR, null, "almuerzo", ADMIN_SEMBRADO_ID).valorOLanza();
        String personaSinMenu = personas.findByPedidoIdOrderByOrden(sinMenu.getId()).get(0).getId();
        assertThat(error(agregarLinea.ejecutar(new SolicitudDeLinea(null, sinMenu.getId(), personaSinMenu,
                TipoDeLinea.MENU_NORMAL, null, 1, null, List.of(), null, null), ADMIN_SEMBRADO_ID)))
                .isEqualTo(ErrorPedido.MENU_NO_PROGRAMADO);
    }
```

(imports adicionales: `DetallePedidoComponente`, `DetallePedidoComponenteRepositorio componentes` autowired, `org.assertj.core.groups.Tuple.tuple`.)

- [ ] **Step 2: Run tests to verify they fail**

Run: `./mvnw -q -pl servicio-operacion -am test -Dtest=LineasDelPedidoTest,MenuDelDiaEnPedidoTest,PersonasDelPedidoTest -Dsurefire.failIfNoSpecifiedTests=false`
Expected: FAIL (el esqueleto de `AgregarLinea` de la tarea 4 lanza `UnsupportedOperationException`; los tests de carta, plan, extra y menú fallan por eso).

- [ ] **Step 3: Write minimal implementation**

`ProductoRepositorio` — sin cambio necesario (la carta se lista en la tarea 7); `AgregarLinea` usa `findById` y `findByTipoMenu`.

`ComponenteElegido.java` y `SolicitudDeLinea.java`:
```java
package pe.rikos.operacion.venta.aplicacion;

/** Lo que el mozo eligio para un componente del menu: cual de las opciones programadas hoy. */
public record ComponenteElegido(String componente, String opcionMenuId) {
}
```
```java
package pe.rikos.operacion.venta.aplicacion;

import java.util.List;

/**
 * Lo que trae el POS al agregar una linea. id: opcional, del cliente (idempotencia). productoId: solo para
 * PRODUCTO. componentes: solo para el menu. descripcion y precioManual: solo para EXTRA_LIBRE (el unico caso
 * en que el mozo pone un precio).
 */
public record SolicitudDeLinea(String id, String pedidoId, String personaId, TipoDeLinea tipo, String productoId,
                               int cantidad, String observacion, List<ComponenteElegido> componentes,
                               String descripcion, Long precioManual) {
}
```

`AgregarLinea.java` (reemplaza el esqueleto):
```java
package pe.rikos.operacion.venta.aplicacion;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.rikos.contratos.Identificador;
import pe.rikos.contratos.Resultado;
import pe.rikos.operacion.catalogo.aplicacion.ConsultaDisponibilidad;
import pe.rikos.operacion.catalogo.aplicacion.ConsultaMenu;
import pe.rikos.operacion.catalogo.aplicacion.TipoMenu;
import pe.rikos.operacion.catalogo.infraestructura.OpcionMenu;
import pe.rikos.operacion.catalogo.infraestructura.Producto;
import pe.rikos.operacion.catalogo.infraestructura.ProductoRepositorio;
import pe.rikos.operacion.comun.auditoria.RegistradorAuditoria;
import pe.rikos.operacion.venta.infraestructura.CuentaPersona;
import pe.rikos.operacion.venta.infraestructura.CuentaPersonaRepositorio;
import pe.rikos.operacion.venta.infraestructura.DetallePedido;
import pe.rikos.operacion.venta.infraestructura.DetallePedidoComponente;
import pe.rikos.operacion.venta.infraestructura.DetallePedidoComponenteRepositorio;
import pe.rikos.operacion.venta.infraestructura.DetallePedidoRepositorio;
import pe.rikos.operacion.venta.infraestructura.Pedido;
import pe.rikos.operacion.venta.infraestructura.PedidoRepositorio;

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * US-028: agrega una linea a la persona de un pedido. El precio lo decide el motor (CalculadorDePrecioDeLinea);
 * el mozo solo trae un precio en el "extra libre" (3.11), que queda marcado como manual y en la bitacora. Las
 * reglas de cada tipo de linea estan en la especificacion (3.4 y 3.5) y en el plan.
 */
@Service
public class AgregarLinea {

    private static final int LARGO_MAXIMO_OBSERVACION = 250;
    private static final int LARGO_MAXIMO_DESCRIPCION = 120;

    private final PedidoRepositorio pedidos;
    private final CuentaPersonaRepositorio personas;
    private final DetallePedidoRepositorio lineas;
    private final DetallePedidoComponenteRepositorio componentes;
    private final ProductoRepositorio productos;
    private final ConsultaMenu consultaMenu;
    private final ConsultaDisponibilidad disponibilidad;
    private final CalculadorDePrecioDeLinea calculador;
    private final RegistradorAuditoria auditoria;
    private final Clock reloj;

    public AgregarLinea(PedidoRepositorio pedidos, CuentaPersonaRepositorio personas, DetallePedidoRepositorio lineas,
                        DetallePedidoComponenteRepositorio componentes, ProductoRepositorio productos,
                        ConsultaMenu consultaMenu, ConsultaDisponibilidad disponibilidad,
                        CalculadorDePrecioDeLinea calculador, RegistradorAuditoria auditoria, Clock reloj) {
        this.pedidos = pedidos;
        this.personas = personas;
        this.lineas = lineas;
        this.componentes = componentes;
        this.productos = productos;
        this.consultaMenu = consultaMenu;
        this.disponibilidad = disponibilidad;
        this.calculador = calculador;
        this.auditoria = auditoria;
        this.reloj = reloj;
    }

    @Transactional
    public Resultado<DetallePedido, ErrorPedido> ejecutar(SolicitudDeLinea s, String actorId) {
        String localId = LocalDeSesion.actual().orElse(null);
        if (localId == null) {
            return Resultado.fracaso(ErrorPedido.SIN_LOCAL_ACTIVO);
        }
        Pedido pedido = pedidos.findByIdAndLocalId(s.pedidoId(), localId).orElse(null);
        if (pedido == null) {
            return Resultado.fracaso(ErrorPedido.NO_ENCONTRADO);
        }
        CuentaPersona persona = personas.findByIdAndPedidoId(s.personaId(), s.pedidoId()).orElse(null);
        if (persona == null) {
            return Resultado.fracaso(ErrorPedido.PERSONA_NO_ENCONTRADA);
        }
        if (s.id() != null) {
            var existente = lineas.findByIdAndCuentaPersonaIdIn(s.id(), List.of(persona.getId()));
            if (existente.isPresent()) {
                return Resultado.exito(existente.get());
            }
        }
        if (s.cantidad() < 1) {
            return Resultado.fracaso(ErrorPedido.CANTIDAD_INVALIDA);
        }
        String observacion = s.observacion() == null || s.observacion().isBlank() ? null : s.observacion().trim();
        if (observacion != null && observacion.length() > LARGO_MAXIMO_OBSERVACION) {
            return Resultado.fracaso(ErrorPedido.OBSERVACION_LARGA);
        }
        boolean pensionista = persona.getPensionistaId() != null;
        boolean paraLlevar = "para_llevar".equals(pedido.getCanal());
        LocalDate hoy = LocalDate.now(reloj);

        String productoId = null;
        String descripcion = null;
        Long precioNormal = null;
        List<DetallePedidoComponente> filas = new ArrayList<>();
        String idLinea = s.id() != null ? s.id() : Identificador.nuevo().valor();

        switch (s.tipo()) {
            case PRODUCTO -> {
                Producto producto = productos.findById(s.productoId() == null ? "" : s.productoId()).orElse(null);
                if (producto == null) {
                    return Resultado.fracaso(ErrorPedido.PRODUCTO_NO_ENCONTRADO);
                }
                if (!producto.isEsVendible() || !producto.isActivo() || producto.getTipoMenu() != null) {
                    return Resultado.fracaso(ErrorPedido.PRODUCTO_NO_VENDIBLE);
                }
                if (disponibilidad.estaAgotado(localId, producto.getId())) {
                    return Resultado.fracaso(ErrorPedido.PRODUCTO_AGOTADO);
                }
                productoId = producto.getId();
                precioNormal = producto.getPrecioVenta();
            }
            case MENU_NORMAL, MENU_EXTRA -> {
                if (!"almuerzo".equals(pedido.getServicio())) {
                    return Resultado.fracaso(ErrorPedido.MENU_FUERA_DE_SERVICIO);
                }
                if (pensionista && s.cantidad() != 1) {
                    return Resultado.fracaso(ErrorPedido.CANTIDAD_INVALIDA);
                }
                TipoMenu tipoMenu = s.tipo() == TipoDeLinea.MENU_EXTRA ? TipoMenu.EXTRA : TipoMenu.NORMAL;
                var delDia = consultaMenu.menuConfirmadoDel(hoy).orElse(List.of());
                if (delDia.isEmpty()) {
                    return Resultado.fracaso(ErrorPedido.MENU_NO_PROGRAMADO);
                }
                Producto productoMenu = productos.findByTipoMenu(tipoMenu.codigo()).orElse(null);
                if (productoMenu == null) {
                    return Resultado.fracaso(ErrorPedido.PRODUCTO_NO_ENCONTRADO);
                }
                var elegidas = elegirComponentes(delDia, tipoMenu, s.componentes());
                if (!elegidas.esExito()) {
                    return Resultado.fracaso(((Resultado.Fracaso<?, ErrorPedido>) elegidas).error());
                }
                for (OpcionMenu opcion : elegidas.valorOLanza()) {
                    filas.add(new DetallePedidoComponente(Identificador.nuevo().valor(), idLinea,
                            opcion.getComponente(), opcion.getProducto().getId(), opcion.getId()));
                }
                productoId = productoMenu.getId();
                precioNormal = productoMenu.getPrecioVenta();
            }
            case PLAN_DESAYUNO, PLAN_CENA -> {
                if (!pensionista) {
                    return Resultado.fracaso(ErrorPedido.PERSONA_NO_ES_PENSIONISTA);
                }
                String servicioDelPlan = s.tipo() == TipoDeLinea.PLAN_DESAYUNO ? "desayuno" : "cena";
                if (!servicioDelPlan.equals(pedido.getServicio())) {
                    return Resultado.fracaso(ErrorPedido.PLAN_FUERA_DE_SERVICIO);
                }
                if (s.cantidad() != 1) {
                    return Resultado.fracaso(ErrorPedido.CANTIDAD_INVALIDA);
                }
            }
            case EXTRA_LIBRE -> {
                descripcion = s.descripcion() == null ? "" : s.descripcion().trim();
                if (descripcion.isEmpty()) {
                    return Resultado.fracaso(ErrorPedido.DESCRIPCION_OBLIGATORIA);
                }
                if (descripcion.length() > LARGO_MAXIMO_DESCRIPCION) {
                    descripcion = descripcion.substring(0, LARGO_MAXIMO_DESCRIPCION);
                }
                if (s.precioManual() == null || s.precioManual() <= 0) {
                    return Resultado.fracaso(ErrorPedido.PRECIO_INVALIDO);
                }
                precioNormal = s.precioManual();
            }
        }

        var precio = calculador.calcular(s.tipo(), pedido.getServicio(), precioNormal, persona, paraLlevar, hoy);
        if (!precio.esExito()) {
            return Resultado.fracaso(((Resultado.Fracaso<?, ErrorPedido>) precio).error());
        }
        PrecioDeLinea p = precio.valorOLanza();
        DetallePedido linea = lineas.save(new DetallePedido(idLinea, persona.getId(), s.tipo().codigo(), productoId,
                descripcion, s.cantidad(), observacion, p.precio(), p.taperUnitario(), p.tarifaOrigen(), p.planId(),
                p.segundoDelDia(), actorId));
        filas.forEach(componentes::save);
        if (s.tipo() == TipoDeLinea.EXTRA_LIBRE) {
            auditoria.registrar("precio_manual", "detalle_pedido", linea.getId(), null,
                    RegistradorAuditoria.campos("descripcion", descripcion, "precio", p.precio(), "cantidad",
                            s.cantidad(), "nombre", "Pedido " + pedido.getNumero()));
        }
        return Resultado.exito(linea);
    }

    /**
     * Un componente con una sola opcion se asigna solo; con varias, el mozo tiene que traer la elegida y esta
     * tiene que ser de hoy y de ese componente. Los componentes sin opciones ese dia no se piden. El segundo del
     * menu extra sale de las opciones "extra"; todo lo demas, de las "normal".
     */
    private Resultado<List<OpcionMenu>, ErrorPedido> elegirComponentes(List<OpcionMenu> delDia, TipoMenu tipoMenu,
                                                                       List<ComponenteElegido> elegidos) {
        Map<String, String> eleccion = (elegidos == null ? List.<ComponenteElegido>of() : elegidos).stream()
                .collect(Collectors.toMap(ComponenteElegido::componente, ComponenteElegido::opcionMenuId, (a, b) -> b));
        for (String componente : eleccion.keySet()) {
            if (!tipoMenu.componentes().contains(componente)) {
                return Resultado.fracaso(ErrorPedido.OPCION_INVALIDA);
            }
        }
        List<OpcionMenu> resultado = new ArrayList<>();
        for (String componente : tipoMenu.componentes()) {
            String tipoDeOpcion = tipoMenu == TipoMenu.EXTRA && "segundo".equals(componente) ? "extra" : "normal";
            List<OpcionMenu> opciones = delDia.stream()
                    .filter(o -> o.isActivo() && o.getComponente().equals(componente)
                            && o.getTipoMenu().equals(tipoDeOpcion))
                    .toList();
            String elegida = eleccion.get(componente);
            if (opciones.isEmpty()) {
                if (elegida != null) {
                    return Resultado.fracaso(ErrorPedido.OPCION_INVALIDA);
                }
                continue;
            }
            if (opciones.size() == 1 && elegida == null) {
                resultado.add(opciones.get(0));
                continue;
            }
            if (elegida == null) {
                return Resultado.fracaso(ErrorPedido.COMPONENTE_OBLIGATORIO);
            }
            OpcionMenu opcion = opciones.stream().filter(o -> o.getId().equals(elegida)).findFirst().orElse(null);
            if (opcion == null) {
                return Resultado.fracaso(ErrorPedido.OPCION_INVALIDA);
            }
            resultado.add(opcion);
        }
        return Resultado.exito(resultado);
    }
}
```

- [ ] **Step 4: Run tests to verify they pass**

Run: mismo comando del Step 2. Expected: `LineasDelPedidoTest` `Tests run: 9`, `MenuDelDiaEnPedidoTest` `Tests run: 6`, `PersonasDelPedidoTest` `Tests run: 7`, todos con `Failures: 0, Errors: 0`. Registrar en el libro `Task 4: complete` ahora. **Si `el_segundo_consumo…` da otro resultado** (ver el aviso de arriba), registrar el `Ruling:` y ajustar la prueba a lo que el motor hace, sin permitir una línea sin precio.

- [ ] **Step 5: Commit** (solo si el usuario lo pide)

---

### Task 7: Modificar y quitar líneas; leer el pedido y la oferta del día

**Files:**
- Create: `.../venta/aplicacion/{ModificarLinea,QuitarLinea,ConsultaPedidos,PedidoDetallado,OfertaDelDia}.java`
- Modify: `.../catalogo/infraestructura/ProductoRepositorio.java` (carta)
- Test: `.../venta/aplicacion/ConsultaDelPedidoTest.java`

**Interfaces:**
- Produces: `ModificarLinea.ejecutar(String pedidoId, String lineaId, int cantidad, String observacion, String actorId) → Resultado<DetallePedido, ErrorPedido>`; `QuitarLinea.ejecutar(String pedidoId, String lineaId, String actorId) → Resultado<Void, ErrorPedido>` (solo líneas `pendiente`; una `comandado` da `LINEA_YA_COMANDADA`: anular es la entrega 3; quitar es un borrado real porque nunca llegó a cocina).
- Produces: `record LineaDetallada(DetallePedido linea, String productoNombre, List<DetallePedidoComponente> componentes)`; `record PersonaDetallada(CuentaPersona persona, List<LineaDetallada> lineas, long subtotal, long taper, long total)`; `record PedidoDetallado(Pedido pedido, List<PersonaDetallada> personas, long subtotal, long taper, long total)`; `ConsultaPedidos.pedido(String id) → Resultado<PedidoDetallado, ErrorPedido>` y `ConsultaPedidos.abiertoDeMesa(String mesaId) → Resultado<PedidoDetallado, ErrorPedido>` (`NO_ENCONTRADO` si no hay). Las líneas `anulado` no suman. `subtotal = Σ precio_unitario × cantidad`, `taper = Σ taper_unitario × cantidad`, `total = subtotal + taper`.
- Produces: `record OfertaDelDia(LocalDate fecha, String servicio, boolean hayMenu, Long precioMenuNormal, Long precioMenuExtra, List<ComponenteOfrecido> normal, List<ComponenteOfrecido> extra, List<ProductoOfrecido> carta)`, `record ComponenteOfrecido(String componente, List<OpcionOfrecida> opciones)`, `record OpcionOfrecida(String opcionMenuId, String productoNombre)`, `record ProductoOfrecido(String id, String nombre, long precio)`; `ConsultaPedidos.oferta(String pedidoId)`: menú confirmado de hoy (si el servicio es almuerzo), y la **carta**: productos vendibles activos, sin `tipoMenu`, con precio, de las categorías `producto_fijo` y `extra`, **sin los agotados en el local**.

- [ ] **Step 1: Write the failing test**

`ConsultaDelPedidoTest.java` (mismo arranque que `LineasDelPedidoTest`, con menú confirmado como en `MenuDelDiaEnPedidoTest`; casos):
```java
    @Test
    void el_total_del_pedido_suma_lineas_taper_y_reparte_por_persona_sin_contar_lo_quitado() {
        // Persona 1 (normal): chicha 500 x2 ; Persona 2 (pensionista): menu normal 800 (plan) para llevar -> taper 100.
        // Se agrega y se quita una linea de 999 para comprobar que lo quitado no suma.
        // Esperado: persona 1 = 1000 ; persona 2 = 800 + taper 100 = 900 ; pedido: subtotal 1800, taper 100, total 1900.
    }

    @Test
    void solo_una_linea_pendiente_se_modifica_o_se_quita() { /* cambiar cantidad y observacion; quitar; una comandada -> LINEA_YA_COMANDADA */ }

    @Test
    void la_oferta_trae_el_menu_de_hoy_por_componente_la_carta_y_esconde_los_agotados_de_este_local() { /* chicha agotada no sale; menu con 2 segundos normal y 1 extra */ }

    @Test
    void el_pedido_abierto_de_una_mesa_se_encuentra_por_la_mesa_y_uno_de_otro_local_no() { }
```
Cada cuerpo se escribe completo al ejecutar la tarea con los mismos ayudantes de las pruebas anteriores (`PedidosBase`, reloj fijo, `AbrirPedido`, `AgregarLinea`); los valores esperados son los del comentario de cada caso. Para marcar una línea como comandada en `solo_una_linea_pendiente…` (aún no existe `Comandar`, entrega 3) el test la guarda con `DetallePedidoRepositorio` y la actualiza con `JdbcTemplate`: `update detalle_pedido set estado = 'comandado' where id = ?` dentro de la misma transacción de la prueba.

- [ ] **Step 2: Run test to verify it fails**

Run: `./mvnw -q -pl servicio-operacion -am test -Dtest=ConsultaDelPedidoTest -Dsurefire.failIfNoSpecifiedTests=false`
Expected: compilation FAIL, `ModificarLinea`, `QuitarLinea`, `ConsultaPedidos` no existen.

- [ ] **Step 3: Write minimal implementation**

`ModificarLinea` y `QuitarLinea` con el mismo patrón que `QuitarPersona` de la tarea 4 (local de la sesión, pedido del local, línea de una persona del pedido vía `findByIdAndCuentaPersonaIdIn(lineaId, idsDePersonasDelPedido)`, estado `pendiente`). `ModificarLinea`: `cantidad ≥ 1` (`CANTIDAD_INVALIDA`), pensionista con línea de menú o plan → cantidad 1, observación ≤ 250; llama `linea.cambiar(cantidad, observacion)`. `QuitarLinea`: `lineas.delete(linea)` (los componentes caen por `ON DELETE CASCADE`).

`ConsultaPedidos`: `@Transactional(readOnly = true)`; arma `PedidoDetallado` leyendo `personas.findByPedidoIdOrderByOrden`, `lineas.findByCuentaPersonaIdInOrderByCreadoEn`, `componentes.findByDetallePedidoIdIn` y los nombres de producto con `productos.findAllById`. `oferta(pedidoId)`: usa `consultaMenu.menuConfirmadoDel(hoy)` (solo si `servicio == almuerzo`) y agrupa por `TipoMenu.componentes()`; la carta la lista `ProductoRepositorio`:

```java
    /** La carta del POS: vendibles activos, sin marca de menu, de las categorias de producto fijo y extra. */
    @Query("select p from Producto p where p.esVendible = true and p.activo = true and p.tipoMenu is null "
            + "and p.precioVenta is not null and p.categoria.naturaleza in ('producto_fijo', 'extra') "
            + "order by p.nombre")
    List<Producto> carta();
```
y excluye en memoria los de `disponibilidad.idsAgotadosDelLocalActivo()`.

- [ ] **Step 4: Run test to verify it passes**

Run: mismo comando del Step 2. Expected: `ConsultaDelPedidoTest` `Tests run: 4, Failures: 0`.

- [ ] **Step 5: Commit** (solo si el usuario lo pide)

---

### Task 8: La mesa ocupada de verdad

**Files:**
- Modify: `.../venta/aplicacion/ConsultaMesas.java`, `BloquearMesa.java`, `CambiarEstadoMesa.java`
- Test: `.../venta/aplicacion/MesasConPedidoTest.java`

**Interfaces:**
- Consumes: `PedidoRepositorio.findByLocalIdAndEstadoIn(String, Collection<String>)`, `AbrirPedido.ESTADOS_EN_CURSO`.
- Produces: `ConsultaMesas` calcula `ocupada` (pedido `abierto` o `comandado`) y `pendiente_cierre` reales con `CalculadorDeEstadoDeMesa.calcular(bloqueada, unida, pendienteCierre, abierto)`; `BloquearMesa` rechaza una mesa con pedido en curso con `MESA_NO_LIBRE`; `CambiarEstadoMesa` **no desactiva** una mesa con pedido en curso (`MESA_NO_LIBRE`).

- [ ] **Step 1: Write the failing test**

```java
package pe.rikos.operacion.venta.aplicacion;

/** La mesa deja de estar "libre" en cuanto tiene un pedido en curso, y una mesa con pedido no se bloquea ni se desactiva. */
@SpringBootTest
@Transactional
class MesasConPedidoTest extends PedidosBase {
    // Arranque igual al de AbrirPedidoTest (dos locales, mesa "1" en el A). @Autowired: ConsultaMesas, AbrirPedido,
    // BloquearMesa, CambiarEstadoMesa, JdbcTemplate.

    @Test
    void una_mesa_con_pedido_abierto_sale_ocupada_en_el_mapa_y_las_demas_siguen_libres() { }

    @Test
    void una_mesa_con_un_pedido_pendiente_de_cierre_sale_pendiente_de_cierre() {
        // Se pasa el pedido a 'pendiente_cierre' con: update pedido set estado = 'pendiente_cierre' where id = ?
    }

    @Test
    void una_mesa_con_pedido_no_se_bloquea_ni_se_desactiva() { /* MESA_NO_LIBRE en ambos */ }

    @Test
    void al_no_haber_pedido_en_curso_la_mesa_vuelve_a_libre() {
        // Se borra el pedido en la prueba (delete from cuenta_persona ...; delete from pedido ...) y la mesa sale libre.
    }
}
```
(Cuerpos completos al ejecutar; el orden de estados esperado es el de `CalculadorDeEstadoDeMesa`: ocupada/pendiente de cierre solo si no está bloqueada ni unida.)

- [ ] **Step 2: Run test to verify it fails**

Run: `./mvnw -q -pl servicio-operacion -am test -Dtest=MesasConPedidoTest,MapaDeMesasTest -Dsurefire.failIfNoSpecifiedTests=false`
Expected: `MesasConPedidoTest` FAIL (la mesa sigue saliendo libre y se puede bloquear).

- [ ] **Step 3: Write minimal implementation**

`ConsultaMesas.listar(...)`: inyectar `PedidoRepositorio`; antes del `map`:
```java
        java.util.Set<String> conAbierto = new java.util.HashSet<>();
        java.util.Set<String> conPendiente = new java.util.HashSet<>();
        pedidos.findByLocalIdAndEstadoIn(localId, AbrirPedido.ESTADOS_EN_CURSO).stream()
                .filter(p -> p.getMesaId() != null)
                .forEach(p -> ("pendiente_cierre".equals(p.getEstado()) ? conPendiente : conAbierto).add(p.getMesaId()));
```
y en el `map`: `CalculadorDeEstadoDeMesa.calcular(m.isBloqueada(), m.getMesaPrincipalId() != null, conPendiente.contains(m.getId()), conAbierto.contains(m.getId()))`.

`BloquearMesa` y `CambiarEstadoMesa` (al desactivar): antes de continuar, `if (pedidos.findByMesaIdAndEstadoIn(id, AbrirPedido.ESTADOS_EN_CURSO).isPresent()) return Resultado.fracaso(ErrorMesa.MESA_NO_LIBRE);` (agregar el repositorio al constructor de ambos).

- [ ] **Step 4: Run tests to verify they pass**

Run: `./mvnw -q -pl servicio-operacion -am test -Dtest=MesasConPedidoTest,MapaDeMesasTest,BloqueoDeMesasTest,GestionDeMesasTest -Dsurefire.failIfNoSpecifiedTests=false`
Expected: todos en verde (los de la entrega 1 siguen pasando: sin pedidos, el resultado no cambia).

- [ ] **Step 5: Commit** (solo si el usuario lo pide)

---

### Task 9: La API, la seguridad y la bitácora

**Files:**
- Create: `.../venta/api/PedidoController.java`
- Modify: `.../comun/seguridad/ConfiguracionSeguridad.java`, `.../identidad/aplicacion/ListarAuditoria.java`, `PantallaAuditoria.tsx` (evento `precio_manual`)
- Test: `.../venta/api/PedidosAccesoPorRolTest.java`, extiende `ListarAuditoriaTest`

**Interfaces:**
- Produces (HTTP, todo del local de la sesión; escrituras con `venta.crear`, lecturas con `venta.ver`):
  - `POST /pedidos` `{id?, canal: "mesa"|"para_llevar", mesaId?, servicio?}` → 201 `PedidoRespuesta`; `GET /pedidos/{id}` → 200; `GET /mesas/{mesaId}/pedido` → 200 o 404.
  - `POST /pedidos/{id}/personas` `{id?, etiqueta?}` → 201; `PUT /pedidos/{id}/personas/{personaId}/pensionista` `{pensionistaId}` → 200; `DELETE /pedidos/{id}/personas/{personaId}` → 204.
  - `POST /pedidos/{id}/lineas` `{id?, personaId, tipo, productoId?, cantidad, observacion?, componentes?: [{componente, opcionMenuId}], descripcion?, precio?}` → 201 `PedidoRespuesta` (el pedido completo); `PUT /pedidos/{id}/lineas/{lineaId}` `{cantidad, observacion?}` → 200; `DELETE /pedidos/{id}/lineas/{lineaId}` → 204.
  - `GET /pedidos/{id}/oferta` → 200 `OfertaRespuesta`; `GET /pensionistas?q=` ya existe (`pensionistas.ver`, que el mozo tiene).
  - Errores: 404 `NO_ENCONTRADO`, `MESA_NO_ENCONTRADA`, `PERSONA_NO_ENCONTRADA`, `LINEA_NO_ENCONTRADA`, `PRODUCTO_NO_ENCONTRADO`, `PENSIONISTA_NO_ENCONTRADO`; 409 `MESA_OCUPADA`, `PENSIONISTA_YA_EN_EL_PEDIDO`, `PERSONA_CON_LINEAS`, `LINEA_YA_COMANDADA`, `PRODUCTO_AGOTADO`; 422 el resto, más `PEDIDO_MODIFICADO` (409) y `NUMERO_DE_PEDIDO_REPETIDO` (409) al traducir los choques de concurrencia igual que `MesaController.protegido`. Cuerpo `{"error":"CODIGO"}`.
  - `PedidoRespuesta`: `id, numero, canal, servicio, mesaId, estado, personas: [{id, etiqueta, tipoCliente, pensionistaId, lineas: [{id, tipoLinea, productoId, productoNombre, descripcion, cantidad, observacion, estado, precioUnitario, taperUnitario, tarifaOrigen, segundoDelDia, componentes: [{componente, productoNombre}]}], subtotal, taper, total}], subtotal, taper, total`.
- Frase de bitácora: `admin puso el precio de un extra a mano: "Huevo frito" a S/ 3.00 (pedido "0001")`.

- [ ] **Step 1: Write the failing tests**

`PedidosAccesoPorRolTest.java` (mismo esqueleto que `MesasAccesoPorRolTest`: crea un local, un usuario por rol con `UsuarioLocal`, y `tokenPara(rol)`); casos:
1. `mozo_y_cajero_toman_pedidos_y_administradores_tambien`: para cada uno de `mozo`, `cajero`, `administrador de local`, `administrador central`: `POST /pedidos` (para llevar, servicio `almuerzo`) → 201; `POST /pedidos/{id}/lineas` con un producto de carta → 201 y `total` correcto; `GET /pedidos/{id}` → 200.
2. `cocina_y_motorizado_ven_pero_no_escriben`: ambos: `GET /pedidos/{id}` → 200 (tienen `venta.ver`), `POST /pedidos` → 403, `POST .../lineas` → 403, `DELETE .../lineas/{id}` → 403.
3. `un_pedido_de_otro_local_da_404_por_http`: en cada verbo (`GET`, `POST lineas`, `PUT/DELETE lineas`, `POST personas`, `PUT pensionista`, `DELETE personas`, `GET oferta`).
4. `los_errores_dan_codigos_claros`: cantidad 0 → 422 `CANTIDAD_INVALIDA`; producto sin precio → 422 `SIN_PRECIO`; segunda mesa con pedido → 409 `MESA_OCUPADA`; sin `precio` en extra libre → 422 `PRECIO_INVALIDO`; agotado → 409 `PRODUCTO_AGOTADO`.
5. `el_cuerpo_de_un_pedido_no_lleva_datos_de_credenciales`: la respuesta de un pedido con un pensionista no contiene `pin`, `hash` ni `contrasena`.

Y en `MesasConcurrenciaTest`-estilo, `PedidosConcurrenciaTest` con dobles (`@MockitoBean AbrirPedido`, `AgregarLinea`) que lanzan `DataIntegrityViolationException("… pedido_numero_unico …")` → 409 `NUMERO_DE_PEDIDO_REPETIDO`, y `ObjectOptimisticLockingFailureException` → 409 `PEDIDO_MODIFICADO`.

Extender `ListarAuditoriaTest` con `elPrecioManualDeUnExtraSeVeEnEspanol` (crear pedido, agregar el extra libre, listar la bitácora: `"admin puso el precio de un extra a mano: \"Huevo frito\" a S/ 3.00 (pedido \"0001\")"`).

- [ ] **Step 2: Run tests to verify they fail**

Run: `./mvnw -q -pl servicio-operacion -am test -Dtest=PedidosAccesoPorRolTest,PedidosConcurrenciaTest,ListarAuditoriaTest -Dsurefire.failIfNoSpecifiedTests=false`
Expected: FAIL (no hay endpoints: 404/403; la frase de la bitácora es genérica).

- [ ] **Step 3: Write minimal implementation**

`PedidoController` con el mismo patrón que `MesaController`: `@RestController`, endpoints con `@Operation`/`@ApiResponses`, **sin `@Transactional` en las escrituras** y cada una envuelta en `protegido(...)` (copiar el método de `MesaController`, cambiando los códigos: `ObjectOptimisticLockingFailureException` → 409 `PEDIDO_MODIFICADO`; `DataIntegrityViolationException` que contenga `pedido_numero_unico` → 409 `NUMERO_DE_PEDIDO_REPETIDO`; otra: relanzar). Cada endpoint de escritura termina devolviendo el pedido completo (`consultaPedidos.pedido(id)` → `PedidoRespuesta`). El mapeo de errores:
```java
        HttpStatus estado = switch (e) {
            case NO_ENCONTRADO, MESA_NO_ENCONTRADA, PERSONA_NO_ENCONTRADA, LINEA_NO_ENCONTRADA,
                 PRODUCTO_NO_ENCONTRADO, PENSIONISTA_NO_ENCONTRADO -> HttpStatus.NOT_FOUND;
            case MESA_OCUPADA, PENSIONISTA_YA_EN_EL_PEDIDO, PERSONA_CON_LINEAS, LINEA_YA_COMANDADA,
                 PRODUCTO_AGOTADO -> HttpStatus.CONFLICT;
            default -> HttpStatus.UNPROCESSABLE_ENTITY;
        };
```
`ConfiguracionSeguridad` — insertar **antes** del matcher de `/mesas`:
```java
                // US-028: pedidos. Ver, venta.ver; tomar y corregir un pedido, venta.crear.
                .requestMatchers(org.springframework.http.HttpMethod.GET, "/pedidos", "/pedidos/**")
                    .hasAuthority("PERM_venta.ver")
                .requestMatchers("/pedidos", "/pedidos/**")
                    .hasAuthority("PERM_venta.crear")
```
(`GET /mesas/{id}/pedido` ya está cubierto por el matcher `GET /mesas/**` → `venta.ver`.)

`ListarAuditoria.humanizar` — en el `switch`:
```java
            case "precio_manual" -> actor + " puso el precio de un extra a mano: \""
                    + RegistradorAuditoria.extraerCampo(fila.getValorNuevo(), "descripcion") + "\" a "
                    + formatearSoles(RegistradorAuditoria.extraerCampo(fila.getValorNuevo(), "precio")) + " (pedido \""
                    + nombreDePedido(fila) + "\")";
```
con `nombreDePedido` leyendo el campo `nombre` (`"Pedido 0001"`) sin el prefijo. En `PantallaAuditoria.tsx`, `EVENTOS`: `{ valor: 'precio_manual', etiqueta: 'Precio de extra puesto a mano' }`.

- [ ] **Step 4: Run tests to verify they pass**

Run: mismo comando del Step 2, más `MesasAccesoPorRolTest,ArquitecturaDeModulosTest`. Expected: todos en verde; `ArquitecturaDeModulosTest` solo con `- Cycle detected: Slice comun ->`.

- [ ] **Step 5: Commit** (solo si el usuario lo pide)

---

### Task 10: Cierre de la entrega — suite completa y documentos

**Files:**
- Create: `rikos-backend/doc/historias/US-028-tomar-el-pedido-backend.md`
- Modify: `rikos-backend/doc/historias/README.md`, `docs/producto/plan/sprint-06-vender-en-mesa.md` (entrega 2A ✅, sección 3.11), `docs/producto/modelo-de-datos/06-venta-pos-comandas--ep05.md` (§ 8.2 a 8.5: totales derivados, `tipo_linea`, `producto_id` nulo, snapshot al cerrar), `docs/producto/plan/README.md`, `ESTADO.md`, `docs/pendientes/README.md`

- [ ] **Step 1: Run the full suite**

Run: `cd rikos-backend && ./mvnw -q -pl servicio-operacion -am test -Dtest='!ArquitecturaDeModulosTest' -DfailIfNoTests=false; grep -h "Tests run" $(ls servicio-operacion/target/surefire-reports/*.txt | grep -v Arquitectura) | awk -F'[:,]' '{t+=$2; f+=$4; e+=$6} END {print "total",t,"fallos",f,"errores",e}'` (sin `cd` a `target`).
Expected: `fallos 0 errores 0`; el total = 368 + las pruebas nuevas de esta entrega (contar `@Test` nuevos con grep antes de escribir la cifra).

- [ ] **Step 2: Verify in the API**

Con el backend reiniciado (aplica V32) y `npm run generar-tipos`: con `curl` o el navegador, abrir un pedido para llevar, agregar un producto de la carta y un menú, y comprobar `total`, `taper` y el estado "ocupada" de la mesa en el mapa del panel. Confirmar que el mapa del POS muestra la mesa ocupada.

- [ ] **Step 3: Write the historia and update the documents**

`US-028-tomar-el-pedido-backend.md` con las secciones habituales (*Qué se pidió*, *Qué se construyó*, *Qué se dejó fuera y por qué* — la pantalla del POS es la 2B, comandar y anular la entrega 3, reorganizar la 4 —, *Cómo se verificó* con el conteo real, *Bugs y tropiezos*, *Datos de desarrollo*). Marcar 2A ✅ en el plan, `ESTADO.md` y los índices; anotar en `docs/pendientes/README.md` los `Ruling:` que hayan salido.

- [ ] **Step 4: Check the documents**

Run: `grep -n "US-028" ESTADO.md rikos-backend/doc/historias/README.md docs/producto/plan/README.md | cut -c1-120`
Expected: las referencias a `US-028` aparecen en los tres.

- [ ] **Step 5: Commit** (solo si el usuario lo pide)

---

## Self-review

**Cobertura de la especificación (secciones 3.3, 3.4, 3.5, 3.11 y 4.1, entrega 2):**
- Pedido con canal mesa o para llevar, servicio del horario o elegido, número, personas → Tareas 2 y 3.
- Mesa mixta: cliente normal y pensionista buscado por nombre o DNI, inactivo rechazado, sin repetir → Tarea 4.
- Precio del motor, segundo consumo entre locales, táper por menú para llevar, sin precio no entra → Tareas 5 y 6.
- Menú del día con opciones (una sola se asigna, varias se eligen, fila guardada siempre, extra con su segundo), carta, agotados ocultos → Tarea 6 y 7 (oferta).
- Comida del plan de desayuno y cena, y extra libre con precio del mozo (decisión del usuario, 3.11) → Tarea 6.
- Observaciones y cantidades; modificar y quitar líneas pendientes → Tareas 6 y 7.
- La mesa ocupada de verdad, y una mesa con pedido no se bloquea ni desactiva → Tarea 8.
- API, permisos por rol, concurrencia, bitácora → Tarea 9.
- Corregir el modelo del bloque 6 (totales derivados, `tipo_linea`) → Tarea 10.

**Dejado para después, a propósito:** la pantalla del POS (2B, plan aparte, cuando esta API exista y su forma esté fija); comandar y anular (entrega 3); reorganizar y el recálculo al cambiar de persona (entrega 4; por eso `PERSONA_CON_LINEAS`); el descuento de inventario (sprint 9); el cargo a la cuenta del pensionista (sprint 7).

**Riesgos que el plan deja explícitos:** (1) las categorías de la carta (`producto_fijo`/`extra`) y los ids `S001`/`S005` de las pruebas dependen de la semilla; si no existen, se registra un `Ruling:` y se ajusta el ayudante `PedidosBase`; (2) el comportamiento del motor con "segundo consumo a precio normal" sobre una línea del plan (sin precio normal) se fija en una prueba con su aviso; (3) las tareas 7 y 8 traen los casos completos en el plan pero los cuerpos se escriben al ejecutar con los mismos ayudantes: los valores esperados están en el comentario de cada caso.

**Consistencia de tipos:** `SolicitudDeLinea` (10 campos) definida en la tarea 6 y usada en 4, 6, 7 y 9; `ErrorPedido` (31 constantes) definida en la 2 y usada en todas; `PrecioDeLinea` definida en la 5 y consumida en la 6; `AbrirPedido.ESTADOS_EN_CURSO` definida en la 3 y usada en la 8; los nombres de repositorio de la tarea 2 coinciden con los usados en 3 a 8.

**Review Focus → prueba:** otro local (`AbrirPedidoTest`, `PersonasDelPedidoTest`, `LineasDelPedidoTest`, `PedidosAccesoPorRolTest`); mesa no disponible (`AbrirPedidoTest`); pensionista inactivo/repetido/con líneas (`PersonasDelPedidoTest`); agotado y sin precio (`LineasDelPedidoTest`); segundo consumo entre locales (`LineasDelPedidoTest`); opciones del menú (`MenuDelDiaEnPedidoTest`); idempotencia y concurrencia (`AbrirPedidoTest`, `PersonasDelPedidoTest`, `LineasDelPedidoTest`, `PedidosConcurrenciaTest`); cantidad, observación, extra libre (`LineasDelPedidoTest`); línea comandada (`ConsultaDelPedidoTest`).
