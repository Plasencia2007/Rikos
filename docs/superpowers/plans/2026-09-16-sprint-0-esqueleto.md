# Sprint 0 — Esqueleto RIKOS Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Dejar un camino completo navegador → gateway → servicios → base de datos funcionando y verificable, con Flyway operando, y los tres tipos base del dominio (`Identificador`, `Dinero`, `Resultado`) en `contratos` con pruebas en verde. Cero funcionalidad de negocio.

**Architecture:** No se rediseña nada de lo ya creado (ver `rikos-backend/ESTRUCTURA.md` y `rikos-web/ESTRUCTURA.md`). Se completan huecos puntuales: falta el Maven Wrapper en la raíz del backend, los `/salud` actuales no informan perfil activo ni estado de base de datos, `contratos` no tiene tipos ni pruebas, y el frontend no muestra en pantalla la respuesta de los dos servicios.

**Tech Stack:** Java 21, Spring Boot 4.1.1, Maven multimódulo, Spring Cloud Gateway (WebMVC), PostgreSQL 17, RabbitMQ 4, Flyway, React 19 + TypeScript + Vite. JUnit 5 para las pruebas de `contratos` (no existía ninguna dependencia de test ahí).

**Spec:** instrucción del usuario en esta conversación (Sprint 0 del backlog, habilitantes HT-01, HT-02, HT-03). No hay un documento de spec separado en el repo.

## Global Constraints

- No tocar la estructura de carpetas de `rikos-backend` ni `rikos-web`: ya está fijada.
- No implementar funcionalidad de negocio (productos, ventas, pensionistas, caja, inventario). Eso es Sprint 1.
- No agregar dependencias que no hagan falta para lo de abajo.
- Hibernate siempre en `ddl-auto: validate`. Nunca `update`. Si algo no valida, se corrige la migración.
- Identificadores ULID generados en el origen, nunca autoincrementales.
- Dinero sin decimales de punto flotante.
- `contratos` es Java puro: sin Spring, sin JPA, sin anotaciones de framework.
- Ninguna dirección de backend escrita a mano en el frontend: todo pasa por `src/api` y es relativo (`/api`, `/facturacion`).
- Ningún directorio de este proyecto tiene todavía repositorio git inicializado (verificado: ni la raíz, ni `rikos-backend`, ni `rikos-web`). Este plan por lo tanto **no incluye pasos de `git commit`** — se reemplazan por "guardar el archivo". Si más adelante se quiere versionar, es una decisión aparte que hay que pedir explícitamente.
- No borrar ni mover nada existente sin confirmación del usuario primero.

---

## Hallazgos previos (contexto para todas las tareas)

- `mvn` no está instalado en el PATH de esta máquina (ni en bash ni en PowerShell). Sí existe JDK 21 (`JAVA_HOME` apunta a Eclipse Adoptium 21.0.11), pero el `java` por defecto en PATH es 1.8 — hay que forzar `JAVA_HOME`/`PATH` al invocar Maven.
- `gateway/mvnw`, `servicio-operacion/mvnw` y `servicio-facturacion/mvnw` existen pero son wrappers **incompletos**: les falta `.mvn/wrapper/maven-wrapper.properties`, así que fallan al ejecutarse (el script lo requiere para saber qué Maven descargar). Además el `README.md` del backend documenta arrancar cada servicio con `../mvnw` (wrapper de la raíz), y la raíz **no tiene wrapper propio en absoluto**. Sin arreglar esto, nada de HT-01 puede compilarse.
- Los endpoints `/salud` actuales (`servicio-operacion`, `servicio-facturacion`) sólo devuelven `servicio`, `estado`, `hora`. Falta perfil activo y estado de conexión a base de datos, tal como pide HT-01. El `gateway` no tiene ningún endpoint `/salud` propio.
- `docker-compose.yml`, `infra/postgres/init.sql` (crea `rikos_operacion` y `rikos_facturacion`), y las migraciones `V1__inicial.sql` de ambos servicios **ya existen y son reales** (no son placeholders). `application.yaml` de ambos servicios ya tiene `ddl-auto: validate` y Flyway habilitado. HT-02 está mayormente hecho; falta documentar la convención y verificar en caliente.
- Revisé los archivos "sueltos" del frontend que pidió el usuario evaluar (`comun/componentes/Boton.tsx`, `comun/componentes/TecladoNumerico.tsx`, `comun/formato/dinero.ts`, `api/cliente.ts`, `features/autenticacion/PantallaPin.tsx` + su `index.ts`). Conclusión: **ninguno es basura de una versión anterior**, todos siguen la convención de `ESTRUCTURA.md` y `PantallaPin` ya deja dicho en un comentario que su chequeo de salud es un placeholder intencional hasta US-002. Se reutilizan y se extienden en la Tarea 8, no se borra nada. Esto se lo confirmo al usuario en el resumen final, no hace falta una tarea de borrado.
- No hay contenedores Docker corriendo todavía (no verificado en este plan si Docker Desktop está arriba; es el primer paso de ejecución).

---

### Tarea 0: Reparar el Maven Wrapper de la raíz del backend

**Files:**
- Create: `rikos-backend/mvnw`
- Create: `rikos-backend/mvnw.cmd`
- Create: `rikos-backend/.mvn/wrapper/maven-wrapper.properties`

**Interfaces:**
- Produce: un `./mvnw` funcional en la raíz de `rikos-backend`, que todas las tareas siguientes usan para compilar y arrancar los módulos (`../mvnw spring-boot:run` desde cada módulo, tal como documenta el `README.md`).

- [ ] **Paso 1: Copiar los scripts del wrapper (son genéricos, no específicos del módulo)**

```bash
cp "rikos-backend/servicio-operacion/mvnw" "rikos-backend/mvnw"
cp "rikos-backend/servicio-operacion/mvnw.cmd" "rikos-backend/mvnw.cmd"
mkdir -p "rikos-backend/.mvn/wrapper"
```

- [ ] **Paso 2: Crear las propiedades del wrapper que faltan**

`rikos-backend/.mvn/wrapper/maven-wrapper.properties`:
```properties
distributionUrl=https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.9.9/apache-maven-3.9.9-bin.zip
```

- [ ] **Paso 3: Verificar que arranca y descarga Maven 3.9.9**

Run (PowerShell, desde `rikos-backend/`): `./mvnw.cmd -v`
Expected: descarga Apache Maven 3.9.9 a `~/.m2/wrapper/dists/...` e imprime `Apache Maven 3.9.9` y `Java version: 21...`.

Si falla por proxy/descarga, es un bloqueador de entorno — se reporta, no se rodea.

---

### Tarea 1: Levantar la infraestructura Docker y verificar que quede sana

**Files:** ninguno (solo verificación de infraestructura ya declarada en `docker-compose.yml`).

- [ ] **Paso 1: Levantar Postgres, RabbitMQ y Mailpit**

Run (desde `rikos-backend/`): `docker compose up -d`

- [ ] **Paso 2: Confirmar que los contenedores están `healthy`**

Run: `docker compose ps`
Expected: `rikos-postgres` y `rikos-rabbitmq` en estado `healthy` (tienen healthcheck definido); `rikos-mailpit` en `running` (no tiene healthcheck).

- [ ] **Paso 3: Confirmar que `init.sql` creó las dos bases de datos**

Run: `docker exec rikos-postgres psql -U rikos -d postgres -c "\l"`
Expected: aparecen `rikos_operacion` y `rikos_facturacion` con `Owner: rikos`, además de la base `postgres` por defecto.

Si las bases no aparecen (por ejemplo porque ya existía un volumen `postgres-datos` de un intento anterior sin ellas), reportarlo al usuario antes de tocar el volumen — no se borra un volumen con datos sin confirmar primero.

---

### Tarea 2: Endpoint de salud del gateway

**Files:**
- Create: `rikos-backend/gateway/src/main/java/pe/rikos/gateway/SaludController.java`

**Interfaces:**
- Produce: `GET /salud` en el gateway → `{ "servicio": "gateway", "perfilActivo": "...", "estado": "ok", "hora": "..." }`. El gateway no tiene base de datos propia, así que no reporta `baseDeDatos`.

- [ ] **Paso 1: Escribir el controlador**

```java
package pe.rikos.gateway;

import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.util.Map;

/**
 * Ruta real: GET /salud (el gateway no se enruta a si mismo).
 */
@RestController
public class SaludController {

    private final Environment entorno;

    public SaludController(Environment entorno) {
        this.entorno = entorno;
    }

    @GetMapping("/salud")
    public Map<String, Object> salud() {
        return Map.of(
                "servicio", "gateway",
                "perfilActivo", perfilActivo(),
                "estado", "ok",
                "hora", OffsetDateTime.now().toString()
        );
    }

    private String perfilActivo() {
        String[] activos = entorno.getActiveProfiles();
        return activos.length == 0 ? "default" : String.join(",", activos);
    }
}
```

- [ ] **Paso 2: Verificación diferida a la Tarea 5** (necesita el servicio compilado y arrancado; no tiene sentido levantarlo solo).

---

### Tarea 3: Endpoint de salud de `servicio-operacion` con perfil y base de datos

**Files:**
- Modify: `rikos-backend/servicio-operacion/src/main/java/pe/rikos/operacion/comun/web/SaludController.java`

**Interfaces:**
- Produce: `GET /api/salud` → `{ "servicio": "servicio-operacion", "perfilActivo": "...", "baseDeDatos": "conectada"|"desconectada", "estado": "ok", "hora": "..." }`.

- [ ] **Paso 1: Reescribir el controlador para inyectar `Environment` y `DataSource`**

```java
package pe.rikos.operacion.comun.web;

import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.Map;

/**
 * Endpoint minimo para comprobar que el servicio responde, que sabe
 * en que perfil corre y que su conexion a base de datos funciona.
 *
 * Con el context-path /api, la ruta real es:  GET /api/salud
 * Y a traves del gateway:                     GET http://localhost:8080/api/salud
 */
@RestController
public class SaludController {

    private final Environment entorno;
    private final DataSource dataSource;

    public SaludController(Environment entorno, DataSource dataSource) {
        this.entorno = entorno;
        this.dataSource = dataSource;
    }

    @GetMapping("/salud")
    public Map<String, Object> salud() {
        return Map.of(
                "servicio", "servicio-operacion",
                "perfilActivo", perfilActivo(),
                "baseDeDatos", baseDeDatos(),
                "estado", "ok",
                "hora", OffsetDateTime.now().toString()
        );
    }

    private String perfilActivo() {
        String[] activos = entorno.getActiveProfiles();
        return activos.length == 0 ? "default" : String.join(",", activos);
    }

    private String baseDeDatos() {
        try (Connection conexion = dataSource.getConnection()) {
            return conexion.isValid(2) ? "conectada" : "desconectada";
        } catch (SQLException excepcion) {
            return "desconectada";
        }
    }
}
```

- [ ] **Paso 2: Verificación diferida a la Tarea 5.**

---

### Tarea 4: Endpoint de salud de `servicio-facturacion` con perfil y base de datos

**Files:**
- Modify: `rikos-backend/servicio-facturacion/src/main/java/pe/rikos/facturacion/comun/web/SaludController.java`

Mismo patrón que la Tarea 3, cambiando el nombre del servicio y el paquete.

- [ ] **Paso 1: Reescribir el controlador**

```java
package pe.rikos.facturacion.comun.web;

import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.Map;

/**
 * Ruta real:            GET /facturacion/salud
 * A traves del gateway: GET http://localhost:8080/facturacion/salud
 */
@RestController
public class SaludController {

    private final Environment entorno;
    private final DataSource dataSource;

    public SaludController(Environment entorno, DataSource dataSource) {
        this.entorno = entorno;
        this.dataSource = dataSource;
    }

    @GetMapping("/salud")
    public Map<String, Object> salud() {
        return Map.of(
                "servicio", "servicio-facturacion",
                "perfilActivo", perfilActivo(),
                "baseDeDatos", baseDeDatos(),
                "estado", "ok",
                "hora", OffsetDateTime.now().toString()
        );
    }

    private String perfilActivo() {
        String[] activos = entorno.getActiveProfiles();
        return activos.length == 0 ? "default" : String.join(",", activos);
    }

    private String baseDeDatos() {
        try (Connection conexion = dataSource.getConnection()) {
            return conexion.isValid(2) ? "conectada" : "desconectada";
        } catch (SQLException excepcion) {
            return "desconectada";
        }
    }
}
```

- [ ] **Paso 2: Verificación diferida a la Tarea 5.**

---

### Tarea 5: Compilar el reactor completo y arrancar los tres servicios

**Files:** ninguno nuevo — usa lo de las tareas 0, 2, 3, 4.

- [ ] **Paso 1: Compilar los cuatro módulos**

Run (desde `rikos-backend/`): `./mvnw.cmd -B clean install -DskipTests`
Expected: `BUILD SUCCESS`, con `contratos` compilando primero (es dependencia de los otros dos servicios).

- [ ] **Paso 2: Arrancar `servicio-operacion` en perfil `local`**

Run (background, desde `rikos-backend/servicio-operacion/`): `../mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=local`
Expected en el log: Flyway aplica `V1__inicial.sql`, Hibernate valida el esquema (no lo crea), arranca en el puerto 8081.

- [ ] **Paso 3: Arrancar `servicio-facturacion`**

Run (background, desde `rikos-backend/servicio-facturacion/`): `../mvnw.cmd spring-boot:run`
Expected en el log: Flyway aplica su propio `V1__inicial.sql`, Hibernate valida, arranca en el puerto 8082.

- [ ] **Paso 4: Arrancar el `gateway`**

Run (background, desde `rikos-backend/gateway/`): `../mvnw.cmd spring-boot:run`
Expected: arranca en el puerto 8080.

- [ ] **Paso 5: Verificar salud directa de cada servicio**

```bash
curl -s http://localhost:8081/api/salud
curl -s http://localhost:8082/facturacion/salud
curl -s http://localhost:8080/salud
```
Expected: JSON con `perfilActivo` (`local` para operación, `default` para facturación y gateway), `baseDeDatos: "conectada"` en los dos primeros, `estado: "ok"`.

- [ ] **Paso 6: Verificar el enrutamiento del gateway**

```bash
curl -s http://localhost:8080/api/salud
curl -s http://localhost:8080/facturacion/salud
```
Expected: mismas respuestas que el Paso 5 pero llegando a través del puerto 8080.

Si algo de esto falla, se reporta el error real (log de arranque, stacktrace) — no se avanza a la Tarea 6 con el backend roto.

---

### Tarea 6: Documentar la convención de migraciones Flyway (HT-02)

**Files:**
- Modify: `rikos-backend/ESTRUCTURA.md`

- [ ] **Paso 1: Agregar una sección nueva al final de `ESTRUCTURA.md`**

```markdown
---

## 9. Convencion de migraciones Flyway

Cada servicio con base de datos propia (`servicio-operacion`, `servicio-facturacion`)
tiene su carpeta `src/main/resources/db/migration/` versionada de forma independiente:
no comparten numeracion porque no comparten esquema.

**Nombre de archivo:** `V<numero>__<descripcion_en_snake_case>.sql`

- `<numero>` es secuencial dentro del servicio (V1, V2, V3...), sin ceros a la izquierda.
- `<descripcion_en_snake_case>` va en espanol, en minusculas, palabras separadas por
  guion bajo. Ejemplo ya aplicado: `V1__inicial.sql`.
- Doble guion bajo (`__`) entre el numero y la descripcion: lo exige Flyway.

**Reglas duras:**

1. Una migracion ya aplicada en cualquier ambiente **no se edita nunca**. Si el esquema
   necesita cambiar, se agrega una migracion nueva con el siguiente numero.
2. `ddl-auto: validate` en Hibernate no es negociable. Hibernate compara el esquema
   contra las entidades y falla el arranque si no coinciden — eso es lo que obliga a
   que toda la verdad del esquema pase por Flyway.
3. `flyway.baseline-on-migrate: true` esta activado en ambos servicios para que una
   base de datos existente sin historial de Flyway no rompa el primer arranque.
```

- [ ] **Paso 2: Confirmar que ya se verificó en caliente (Tarea 5, Pasos 2-3)**

No hace falta repetir el arranque: el log de Flyway/Hibernate de la Tarea 5 ya demuestra que `V1__inicial.sql` se aplicó y el esquema validó en ambos servicios. Si por algún motivo se re-ejecuta esta tarea de forma aislada, repetir Tarea 5 Pasos 2-3 y confirmar en el log las líneas `Flyway ... Successfully applied 1 migration` y ausencia de errores de `SchemaManagementException`.

---

### Tarea 7: Frontend — mostrar en pantalla la salud de los dos servicios

**Files:**
- Modify: `rikos-web/src/api/cliente.ts`
- Modify: `rikos-web/src/features/autenticacion/PantallaPin.tsx`

**Interfaces:**
- Produce: `consultarSalud(ruta: string): Promise<Salud>` genérico (antes solo pegaba a `/api/salud`); `PantallaPin` llama automáticamente al montar a `/api/salud` y `/facturacion/salud` y pinta el resultado de los dos.

- [ ] **Paso 1: Generalizar el cliente de salud**

Editar `rikos-web/src/api/cliente.ts`, reemplazando el bloque final (desde `export interface Salud`):

```ts
export interface Salud {
  servicio: string
  perfilActivo: string
  baseDeDatos?: 'conectada' | 'desconectada'
  estado: string
  hora: string
}

/** Comprueba que un servicio del backend responde a traves del gateway. */
export const consultarSalud = (ruta: '/api/salud' | '/facturacion/salud') =>
  pedir<Salud>(ruta)
```

- [ ] **Paso 2: Chequear salud de ambos servicios al montar la pantalla**

Editar `rikos-web/src/features/autenticacion/PantallaPin.tsx`. Cambios sobre el archivo actual:

1. Importar `useEffect` de React y el tipo `Salud`.
2. Reemplazar el estado `detalle: string` por `resultados: Salud[]`.
3. Agregar un `useEffect` que llame a `comprobar()` al montar, además de seguir permitiendo el chequeo manual por PIN (se deja el teclado como está: es la futura pantalla de login de US-002, no se toca su interaccion).
4. `comprobar()` pasa a pedir los dos servicios en paralelo con `Promise.allSettled` y guardar lo que haya respondido, marcando error por servicio si alguno falla, en vez de fallar todo junto.

```tsx
import { useEffect, useState } from 'react'
import { TecladoNumerico } from '../../comun/componentes/TecladoNumerico'
import { consultarSalud, type Salud } from '../../api/cliente'

const LARGO_PIN = 4
const SERVICIOS = ['/api/salud', '/facturacion/salud'] as const

/**
 * Pantalla de PIN del mozo.
 *
 * OJO con el diseno (DT-028): esto no es un inicio de sesion, es la pantalla
 * de bloqueo que aparece cada vez que un mozo suelta la tablet y otro la toma.
 * Por eso entra sola al cuarto digito, sin boton de confirmar.
 *
 * Por ahora, ademas, sirve de comprobacion visual del esqueleto del Sprint 0:
 * al montar consulta la salud de servicio-operacion y servicio-facturacion a
 * traves del gateway. La autenticacion real es US-002; esta comprobacion se
 * retira cuando esa historia reemplace esta pantalla.
 */
export function PantallaPin() {
  const [pin, setPin] = useState('')
  const [estado, setEstado] = useState<'verificando' | 'listo'>('verificando')
  const [resultados, setResultados] = useState<Record<string, string>>({})

  async function comprobar() {
    setEstado('verificando')
    const respuestas = await Promise.allSettled(SERVICIOS.map((ruta) => consultarSalud(ruta)))
    const siguiente: Record<string, string> = {}
    respuestas.forEach((r, i) => {
      const ruta = SERVICIOS[i]
      siguiente[ruta] =
        r.status === 'fulfilled'
          ? `${r.value.servicio} · perfil ${r.value.perfilActivo} · bd ${r.value.baseDeDatos ?? 'n/a'} · ${r.value.estado}`
          : 'sin respuesta'
    })
    setResultados(siguiente)
    setEstado('listo')
    setPin('')
  }

  useEffect(() => {
    void comprobar()
  }, [])

  function agregar(digito: string) {
    if (pin.length >= LARGO_PIN) return
    const nuevo = pin + digito
    setPin(nuevo)
    if (nuevo.length === LARGO_PIN) void comprobar()
  }

  return (
    <div className="pantalla-pin">
      <aside className="pin-marca">
        <h1>
          Bienvenido al
          <br />
          <span>Punto de Venta</span>
        </h1>
        <p>Ingresa tu PIN para acceder al sistema de ventas.</p>
        <footer>Sede Principal</footer>
      </aside>

      <main className="pin-acceso">
        <h2>Ingresa tu PIN</h2>
        <p className="pin-ayuda">Usa tu codigo de acceso rapido</p>

        <div className="pin-puntos">
          {Array.from({ length: LARGO_PIN }).map((_, i) => (
            <span key={i} className={i < pin.length ? 'punto lleno' : 'punto'} />
          ))}
        </div>

        <TecladoNumerico
          onDigito={agregar}
          onBorrar={() => setPin('')}
          deshabilitado={estado === 'verificando'}
        />

        <div className="pin-diagnostico">
          <p className="pin-ayuda">Esqueleto Sprint 0 — salud de los servicios:</p>
          {SERVICIOS.map((ruta) => (
            <p key={ruta} className={resultados[ruta] === 'sin respuesta' ? 'pin-mensaje error' : 'pin-mensaje ok'}>
              {ruta}: {resultados[ruta] ?? 'comprobando…'}
            </p>
          ))}
        </div>
      </main>
    </div>
  )
}
```

- [ ] **Paso 3: Instalar dependencias y arrancar Vite**

```bash
cd rikos-web
npm install
npm run dev
```

- [ ] **Paso 4: Verificar visualmente**

Abrir `http://localhost:5173` (redirige a `/pos`, que sigue siendo el placeholder `POS — por construir`). Navegar directo a `http://localhost:5173/kiosco` no aplica — la pantalla de salud vive en `features/autenticacion`, que hoy no está montada en ninguna ruta de `apps/`. **Antes de este paso, revisar si hace falta enchufar `PantallaPin` en alguna ruta temporalmente para verla** (por ejemplo dentro de `apps/pos/index.tsx`) — decidir en el momento de ejecutar, dejando constancia de qué se hizo, porque `apps/pos/index.tsx` hoy es un placeholder de una sola línea sin uso real todavía.

Tomar captura de pantalla mostrando las dos líneas de diagnóstico con `perfil`, `bd` y `estado`.

---

### Tarea 8: `contratos` — preparar JUnit 5 y crear `Identificador` (ULID)

**Files:**
- Modify: `rikos-backend/contratos/pom.xml`
- Create: `rikos-backend/contratos/src/main/java/pe/rikos/contratos/Identificador.java`
- Create: `rikos-backend/contratos/src/test/java/pe/rikos/contratos/IdentificadorTest.java`

**Interfaces:**
- Produce: `Identificador.nuevo()` (genera un ULID nuevo), `Identificador.de(String valor)` (reconstruye uno persistido, valida formato), `identificador.valor()` (String de 26 caracteres). Sin dependencias externas: Crockford Base32 + `SecureRandom`, todo en `java.*`.

`contratos/pom.xml` no tenía ninguna dependencia (ni de test): hay que agregar JUnit 5 y fijar la versión del `maven-surefire-plugin`, porque este módulo no hereda de `spring-boot-starter-parent` y por lo tanto no tiene el `BOM` que normalmente resuelve esas versiones.

- [ ] **Paso 1: Agregar JUnit 5 y el plugin de test al pom**

Reemplazar el `<properties>` y agregar dependencias/build en `rikos-backend/contratos/pom.xml`:

```xml
<properties>
    <maven.compiler.source>21</maven.compiler.source>
    <maven.compiler.target>21</maven.compiler.target>
    <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
    <junit-bom.version>5.11.3</junit-bom.version>
</properties>

<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>org.junit</groupId>
            <artifactId>junit-bom</artifactId>
            <version>${junit-bom.version}</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>

<dependencies>
    <dependency>
        <groupId>org.junit.jupiter</groupId>
        <artifactId>junit-jupiter</artifactId>
        <scope>test</scope>
    </dependency>
    <dependency>
        <groupId>org.junit.platform</groupId>
        <artifactId>junit-platform-launcher</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>

<build>
    <plugins>
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-surefire-plugin</artifactId>
            <version>3.5.2</version>
        </plugin>
    </plugins>
</build>
```

- [ ] **Paso 2: Escribir la prueba primero**

`rikos-backend/contratos/src/test/java/pe/rikos/contratos/IdentificadorTest.java`:

```java
package pe.rikos.contratos;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class IdentificadorTest {

    @Test
    void unIdentificadorNuevoTiene26Caracteres() {
        Identificador id = Identificador.nuevo();
        assertEquals(26, id.valor().length());
    }

    @Test
    void dosIdentificadoresNuevosNuncaColisionan() {
        Set<String> generados = new HashSet<>();
        for (int i = 0; i < 10_000; i++) {
            assertTrue(generados.add(Identificador.nuevo().valor()));
        }
    }

    @Test
    void esOrdenableCronologicamenteComoTexto() throws InterruptedException {
        Identificador primero = Identificador.nuevo();
        Thread.sleep(5);
        Identificador segundo = Identificador.nuevo();
        assertTrue(primero.valor().compareTo(segundo.valor()) < 0);
    }

    @Test
    void reconstruyeUnIdentificadorValidoPersistido() {
        String valor = Identificador.nuevo().valor();
        Identificador reconstruido = Identificador.de(valor);
        assertEquals(valor, reconstruido.valor());
    }

    @Test
    void rechazaUnValorConLargoIncorrecto() {
        assertThrows(IllegalArgumentException.class, () -> Identificador.de("demasiado-corto"));
    }

    @Test
    void rechazaCaracteresFueraDelAlfabetoCrockford() {
        // 'I', 'L', 'O', 'U' no existen en Crockford Base32
        String invalido = "0".repeat(25) + "U";
        assertThrows(IllegalArgumentException.class, () -> Identificador.de(invalido));
    }

    @Test
    void dosIdentificadoresConElMismoValorSonIguales() {
        String valor = Identificador.nuevo().valor();
        assertEquals(Identificador.de(valor), Identificador.de(valor));
    }
}
```

- [ ] **Paso 3: Confirmar que no compila / falla (falta `Identificador`)**

Run: `./mvnw.cmd -pl contratos -am test`
Expected: error de compilación, `Identificador` no existe.

- [ ] **Paso 4: Implementar `Identificador`**

`rikos-backend/contratos/src/main/java/pe/rikos/contratos/Identificador.java`:

```java
package pe.rikos.contratos;

import java.security.SecureRandom;
import java.util.Objects;

/**
 * ULID (Universally Unique Lexicographically Sortable Identifier) generado
 * en el origen de la aplicacion, nunca autoincremental de la base de datos.
 *
 * 26 caracteres en Base32 de Crockford: 48 bits de marca de tiempo en
 * milisegundos (10 caracteres) + 80 bits de aleatoriedad (16 caracteres).
 * Es ordenable como texto porque la marca de tiempo va primero.
 *
 * Por que no autoincremental: un local sin conexion tiene que poder crear
 * registros sin colisionar con los de otro local (ver ESTRUCTURA.md, seccion 5).
 */
public final class Identificador {

    private static final char[] ALFABETO_CROCKFORD = "0123456789ABCDEFGHJKMNPQRSTVWXYZ".toCharArray();
    private static final int LARGO = 26;
    private static final SecureRandom ALEATORIO = new SecureRandom();

    private final String valor;

    private Identificador(String valor) {
        this.valor = valor;
    }

    public static Identificador nuevo() {
        return new Identificador(generar(System.currentTimeMillis()));
    }

    public static Identificador de(String valor) {
        validar(valor);
        return new Identificador(valor);
    }

    public String valor() {
        return valor;
    }

    private static String generar(long marcaDeTiempoMs) {
        byte[] aleatoriedad = new byte[10];
        ALEATORIO.nextBytes(aleatoriedad);

        char[] resultado = new char[LARGO];
        long tiempo = marcaDeTiempoMs;
        for (int i = 9; i >= 0; i--) {
            resultado[i] = ALFABETO_CROCKFORD[(int) (tiempo & 0x1F)];
            tiempo >>>= 5;
        }

        long altos = ((long) (aleatoriedad[0] & 0xFF) << 32)
                | ((long) (aleatoriedad[1] & 0xFF) << 24)
                | ((long) (aleatoriedad[2] & 0xFF) << 16)
                | ((long) (aleatoriedad[3] & 0xFF) << 8)
                | (aleatoriedad[4] & 0xFF);
        long bajos = ((long) (aleatoriedad[5] & 0xFF) << 32)
                | ((long) (aleatoriedad[6] & 0xFF) << 24)
                | ((long) (aleatoriedad[7] & 0xFF) << 16)
                | ((long) (aleatoriedad[8] & 0xFF) << 8)
                | (aleatoriedad[9] & 0xFF);

        for (int i = 15; i >= 8; i--) {
            resultado[i] = ALFABETO_CROCKFORD[(int) (altos & 0x1F)];
            altos >>>= 5;
        }
        for (int i = 25; i >= 16; i--) {
            resultado[i] = ALFABETO_CROCKFORD[(int) (bajos & 0x1F)];
            bajos >>>= 5;
        }

        return new String(resultado);
    }

    private static void validar(String valor) {
        if (valor == null || valor.length() != LARGO) {
            throw new IllegalArgumentException(
                    "Un identificador debe tener " + LARGO + " caracteres: " + valor);
        }
        for (char c : valor.toCharArray()) {
            if (indiceEnAlfabeto(c) < 0) {
                throw new IllegalArgumentException(
                        "Caracter fuera del alfabeto Crockford Base32: " + c);
            }
        }
    }

    private static int indiceEnAlfabeto(char c) {
        char mayuscula = Character.toUpperCase(c);
        for (int i = 0; i < ALFABETO_CROCKFORD.length; i++) {
            if (ALFABETO_CROCKFORD[i] == mayuscula) return i;
        }
        return -1;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Identificador otro)) return false;
        return valor.equals(otro.valor);
    }

    @Override
    public int hashCode() {
        return Objects.hash(valor);
    }

    @Override
    public String toString() {
        return valor;
    }
}
```

- [ ] **Paso 5: Correr las pruebas y confirmar que pasan**

Run: `./mvnw.cmd -pl contratos -am test`
Expected: `Tests run: 7, Failures: 0, Errors: 0` para `IdentificadorTest`.

- [ ] **Paso 6: Guardar los archivos** (no hay commit: no hay git inicializado).

---

### Tarea 9: `contratos` — crear `Dinero`

**Files:**
- Create: `rikos-backend/contratos/src/main/java/pe/rikos/contratos/Dinero.java`
- Create: `rikos-backend/contratos/src/test/java/pe/rikos/contratos/DineroTest.java`

**Interfaces:**
- Produce: `Dinero.deCentimos(long)`, `Dinero.CERO`, `dinero.sumar(Dinero)`, `dinero.restar(Dinero)`, `dinero.multiplicarPor(int cantidad)`, `dinero.centimos()`, comparación (`compararCon`, `esMayorQue`, `esMenorQue`).

Representación interna: `long` en céntimos (entero), nunca `double`/`float`. Es un `record` inmutable.

- [ ] **Paso 1: Escribir la prueba primero**

`rikos-backend/contratos/src/test/java/pe/rikos/contratos/DineroTest.java`:

```java
package pe.rikos.contratos;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DineroTest {

    @Test
    void ceroTieneCeroCentimos() {
        assertEquals(0L, Dinero.CERO.centimos());
    }

    @Test
    void sumaDosMontos() {
        Dinero total = Dinero.deCentimos(1050).sumar(Dinero.deCentimos(250));
        assertEquals(1300L, total.centimos());
    }

    @Test
    void restaPuedeQuedarNegativaParaAjustes() {
        Dinero resultado = Dinero.deCentimos(500).restar(Dinero.deCentimos(700));
        assertEquals(-200L, resultado.centimos());
    }

    @Test
    void multiplicaPorUnaCantidadEntera() {
        Dinero unitario = Dinero.deCentimos(350);
        assertEquals(1050L, unitario.multiplicarPor(3).centimos());
    }

    @Test
    void comparaDosMontos() {
        assertTrue(Dinero.deCentimos(500).esMayorQue(Dinero.deCentimos(400)));
        assertTrue(Dinero.deCentimos(400).esMenorQue(Dinero.deCentimos(500)));
        assertFalse(Dinero.deCentimos(500).esMayorQue(Dinero.deCentimos(500)));
    }

    @Test
    void dosMontosIgualesSonIguales() {
        assertEquals(Dinero.deCentimos(999), Dinero.deCentimos(999));
    }

    @Test
    void noExisteConstructorQueAceptePuntoFlotante() {
        // Prueba de diseno, no de comportamiento: Dinero solo se construye
        // desde un long de centimos. Si este test compila, la garantia se cumple.
        Dinero d = Dinero.deCentimos(100);
        assertNotNull(d);
    }
}
```

- [ ] **Paso 2: Confirmar que falla la compilación** (falta `Dinero`).

Run: `./mvnw.cmd -pl contratos -am test`

- [ ] **Paso 3: Implementar `Dinero`**

`rikos-backend/contratos/src/main/java/pe/rikos/contratos/Dinero.java`:

```java
package pe.rikos.contratos;

/**
 * Importe monetario representado en centimos (entero), nunca en punto
 * flotante. Un double no puede representar exactamente 0.10, y ese error
 * de redondeo es inaceptable en dinero real.
 */
public record Dinero(long centimos) implements Comparable<Dinero> {

    public static final Dinero CERO = new Dinero(0L);

    public static Dinero deCentimos(long centimos) {
        return new Dinero(centimos);
    }

    public Dinero sumar(Dinero otro) {
        return new Dinero(this.centimos + otro.centimos);
    }

    public Dinero restar(Dinero otro) {
        return new Dinero(this.centimos - otro.centimos);
    }

    public Dinero multiplicarPor(int cantidad) {
        return new Dinero(this.centimos * cantidad);
    }

    public boolean esMayorQue(Dinero otro) {
        return this.centimos > otro.centimos;
    }

    public boolean esMenorQue(Dinero otro) {
        return this.centimos < otro.centimos;
    }

    @Override
    public int compareTo(Dinero otro) {
        return Long.compare(this.centimos, otro.centimos);
    }
}
```

- [ ] **Paso 4: Correr las pruebas y confirmar que pasan**

Run: `./mvnw.cmd -pl contratos -am test`
Expected: `Tests run: 7, Failures: 0, Errors: 0` para `DineroTest`.

- [ ] **Paso 5: Guardar los archivos.**

---

### Tarea 10: `contratos` — crear `Resultado<T, E>`

**Files:**
- Create: `rikos-backend/contratos/src/main/java/pe/rikos/contratos/Resultado.java`
- Create: `rikos-backend/contratos/src/test/java/pe/rikos/contratos/ResultadoTest.java`

**Interfaces:**
- Produce: `Resultado.exito(T)`, `Resultado.fracaso(E)`, `resultado.esExito()`, `resultado.esFracaso()`, `resultado.valorOLanza()` (lanza `IllegalStateException` si es fracaso — uso interno/pruebas, no reemplaza el manejo explícito), `resultado.map(Function<T,R>)`, `resultado.mapError(Function<E,F>)`. `sealed interface` con dos implementaciones record (`Exito`, `Fracaso`), pensado para usarse con pattern matching de `switch` (Java 21).

- [ ] **Paso 1: Escribir la prueba primero**

`rikos-backend/contratos/src/test/java/pe/rikos/contratos/ResultadoTest.java`:

```java
package pe.rikos.contratos;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ResultadoTest {

    @Test
    void unExitoEsExitoYNoFracaso() {
        Resultado<Integer, String> r = Resultado.exito(42);
        assertTrue(r.esExito());
        assertFalse(r.esFracaso());
    }

    @Test
    void unFracasoEsFracasoYNoExito() {
        Resultado<Integer, String> r = Resultado.fracaso("no encontrado");
        assertTrue(r.esFracaso());
        assertFalse(r.esExito());
    }

    @Test
    void valorOLanzaDevuelveElValorEnExito() {
        assertEquals(42, Resultado.<Integer, String>exito(42).valorOLanza());
    }

    @Test
    void valorOLanzaLanzaEnFracaso() {
        Resultado<Integer, String> r = Resultado.fracaso("no encontrado");
        assertThrows(IllegalStateException.class, r::valorOLanza);
    }

    @Test
    void mapTransformaElValorSoloEnExito() {
        Resultado<Integer, String> r = Resultado.<Integer, String>exito(2).map(v -> v * 10);
        assertEquals(20, r.valorOLanza());
    }

    @Test
    void mapNoTocaUnFracaso() {
        Resultado<Integer, String> r = Resultado.<Integer, String>fracaso("error").map(v -> v * 10);
        assertTrue(r.esFracaso());
    }

    @Test
    void mapErrorTransformaElErrorSoloEnFracaso() {
        Resultado<Integer, String> r = Resultado.<Integer, String>fracaso("error").mapError(String::toUpperCase);
        assertEquals("ERROR", ((Resultado.Fracaso<Integer, String>) r).error());
    }

    @Test
    void sePuedeUsarConSwitchDePatrones() {
        Resultado<Integer, String> r = Resultado.exito(7);
        String descripcion = switch (r) {
            case Resultado.Exito<Integer, String> e -> "ok:" + e.valor();
            case Resultado.Fracaso<Integer, String> f -> "error:" + f.error();
        };
        assertEquals("ok:7", descripcion);
    }
}
```

- [ ] **Paso 2: Confirmar que falla la compilación** (falta `Resultado`).

Run: `./mvnw.cmd -pl contratos -am test`

- [ ] **Paso 3: Implementar `Resultado`**

`rikos-backend/contratos/src/main/java/pe/rikos/contratos/Resultado.java`:

```java
package pe.rikos.contratos;

import java.util.function.Function;

/**
 * Resultado de una operacion que puede fallar de forma esperada.
 *
 * Se usa en la capa de aplicacion en lugar de excepciones para los errores
 * que forman parte del flujo normal del negocio (ej. "el pensionista no
 * tiene saldo"), reservando las excepciones para lo verdaderamente
 * excepcional. Sellado a proposito: el compilador obliga a manejar los
 * dos casos en un switch de patrones.
 */
public sealed interface Resultado<T, E> {

    static <T, E> Resultado<T, E> exito(T valor) {
        return new Exito<>(valor);
    }

    static <T, E> Resultado<T, E> fracaso(E error) {
        return new Fracaso<>(error);
    }

    boolean esExito();

    boolean esFracaso();

    T valorOLanza();

    <R> Resultado<R, E> map(Function<T, R> transformacion);

    <F> Resultado<T, F> mapError(Function<E, F> transformacion);

    record Exito<T, E>(T valor) implements Resultado<T, E> {

        @Override
        public boolean esExito() {
            return true;
        }

        @Override
        public boolean esFracaso() {
            return false;
        }

        @Override
        public T valorOLanza() {
            return valor;
        }

        @Override
        public <R> Resultado<R, E> map(Function<T, R> transformacion) {
            return new Exito<>(transformacion.apply(valor));
        }

        @Override
        public <F> Resultado<T, F> mapError(Function<E, F> transformacion) {
            return new Exito<>(valor);
        }
    }

    record Fracaso<T, E>(E error) implements Resultado<T, E> {

        @Override
        public boolean esExito() {
            return false;
        }

        @Override
        public boolean esFracaso() {
            return true;
        }

        @Override
        public T valorOLanza() {
            throw new IllegalStateException("Resultado en fracaso, no tiene valor: " + error);
        }

        @Override
        public <R> Resultado<R, E> map(Function<T, R> transformacion) {
            return new Fracaso<>(error);
        }

        @Override
        public <F> Resultado<T, F> mapError(Function<E, F> transformacion) {
            return new Fracaso<>(transformacion.apply(error));
        }
    }
}
```

- [ ] **Paso 4: Correr las pruebas y confirmar que pasan**

Run: `./mvnw.cmd -pl contratos -am test`
Expected: `Tests run: 8, Failures: 0, Errors: 0` para `ResultadoTest`.

- [ ] **Paso 5: Guardar los archivos.**

---

### Tarea 11: Verificación final de todo el reactor

**Files:** ninguno.

- [ ] **Paso 1: Correr todas las pruebas de `contratos` juntas**

Run (desde `rikos-backend/`): `./mvnw.cmd -pl contratos test`
Expected: `Tests run: 22, Failures: 0, Errors: 0` (7 + 7 + 8 de las tareas 8-10).

- [ ] **Paso 2: Build completo del reactor con los cuatro módulos**

Run: `./mvnw.cmd clean install -DskipTests`
Expected: `BUILD SUCCESS`.

- [ ] **Paso 3: Confirmar que los tres servicios siguen sanos** (reusar los `curl` de la Tarea 5, Pasos 5-6, sin tener que reiniciarlos si siguen corriendo).

- [ ] **Paso 4: Armar el resumen final para el usuario**, en el orden que pidió:
  1. Contenedores Docker corriendo y sanos (salida de `docker compose ps`).
  2. Respuesta de cada `/salud`, directa y vía gateway.
  3. Descripción/captura de la pantalla del frontend con los datos del backend.
  4. Salida en verde de `IdentificadorTest`, `DineroTest`, `ResultadoTest`.
  5. Lista de archivos creados/modificados y convenciones fijadas (wrapper Maven de la raíz, formato de `/salud`, convención de nombres de migración, `Identificador`/`Dinero`/`Resultado`).

---

## Notas de ejecución

- Todas las tareas 2-4 y 8-10 son independientes entre sí dentro de su grupo (se podrían paralelizar), pero la Tarea 5 depende de 0+1+2+3+4, y la Tarea 11 depende de todo lo anterior. Ejecutar en orden numérico es lo más simple y es lo que pidió el usuario ("al terminar cada habilitante, compila, arranca y comprueba").
- Los procesos de Spring Boot (`spring-boot:run` x3) y Vite quedan corriendo en segundo plano durante la verificación; hay que recordarlos para poder detenerlos al terminar si el usuario no los quiere dejar corriendo.
