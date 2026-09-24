# Estructura del backend

Como se organiza el codigo y por que. Si vas a escribir aqui —persona o
asistente— lee esto primero.

---

## 1. El nivel de arriba son modulos, no capas

```
pe.rikos.operacion/
├── identidad/
├── catalogo/
├── menu/
├── pensionistas/
├── venta/
├── caja/
├── inventario/
├── delivery/
├── configuracion/
└── comun/
```

No existe un paquete `controller/` ni `service/` global. La razon es concreta:
**Spring Modulith detecta como modulo cada paquete directamente debajo del
paquete de la aplicacion**. Con una estructura por capas veria un solo modulo
gigante y no podria verificar nada.

Los modulos corresponden con las epicas del backlog y con las carpetas de
`features/` en el frontend. Un requisito se trabaja siempre en los mismos
dos lugares.

---

## 2. Dentro de cada modulo, cuatro capas

```
pensionistas/
├── api/                 controladores REST. Solo traducen HTTP.
├── aplicacion/          servicios. Orquestan pasos, no deciden reglas.
├── dominio/             entidades del negocio + interfaces de repositorio.
└── infraestructura/     @Entity de JPA + implementacion de repositorios.
```

Equivalencia con la nomenclatura clasica de Spring:

| Nombre clasico | Aqui va en |
|---|---|
| `Controller` | `api/` |
| `Service` | `aplicacion/` |
| `Repository` (la interfaz) | `dominio/` |
| `Repository` (la implementacion JPA) | `infraestructura/` |
| `Entity` | ver el punto 3 |

### Que va exactamente en cada una

**`api/`** — recibe la peticion, valida el formato, llama a un caso de uso de
`aplicacion/` y traduce el resultado a una respuesta. **Cero logica de
negocio.** Si un controlador tiene un `if` que decide algo del negocio, esa
decision esta en el lugar equivocado.

**`aplicacion/`** — un servicio por caso de uso. Coordina: busca por el
repositorio, le pide a la entidad que decida, guarda, publica el evento.
Las transacciones se abren aqui.

**`dominio/`** — las entidades con sus reglas y las interfaces de repositorio
que el modulo necesita. **Sin anotaciones de Spring ni de JPA.** Java puro.

**`infraestructura/`** — todo lo que toca el mundo exterior: las clases con
`@Entity`, los repositorios de Spring Data, los adaptadores.

---

## 3. Por que la entidad se parte en dos

La `@Entity` de JPA es una fila de una tabla. La entidad del dominio es el
objeto del negocio con sus reglas. En los modulos con reglas de verdad no
son la misma cosa.

Ejemplo: `CuentaPensionista` sabe que el saldo a favor se aplica antes que la
deuda, que el limite alerta pero no bloquea, y que el saldo es la suma de sus
movimientos. Eso son metodos que deciden, no getters y setters.

Si esa clase lleva `@Entity` encima, las reglas quedan atadas a Hibernate y
para probarlas hace falta levantar una base de datos.

**Donde aplicar esto:** en `pensionistas`, `venta`, `caja`, `inventario` y
`catalogo`, que es donde estan las reglas.

**Donde no:** en `configuracion` y en cualquier modulo que sea guardar y
listar, la `@Entity` se usa directo y `dominio/` queda vacio. Cuatro capas de
mapeo para guardar un nombre y un RUC es ceremonia sin valor. En `menu` y
`delivery` el dominio sera delgado, y esta bien.

**No fuerces clases en `dominio/` solo porque la carpeta existe.**

---

## 4. La regla dura entre modulos

> Un servicio nunca inyecta el repositorio de otro modulo.

Si `venta` necesita algo de `inventario`, tiene dos caminos:

1. Llamar al **API publico** de `inventario` — lo que ese modulo expone a
   proposito.
2. Publicar un **evento** y que `inventario` reaccione.

Nunca alcanzar sus tablas ni sus repositorios por dentro. Esto es lo que
Spring Modulith verifica en cada compilacion, y es lo que hace que dentro de
un año el sistema siga siendo entendible.

Cuando el acoplamiento es inevitable y sincronico, va por el API publico.
Cuando puede ser diferido, va por evento — y casi siempre puede.

---

## 5. Reglas de datos que no se negocian

Estan aqui porque agregarlas despues es carisimo:

1. **Los identificadores se generan en el origen** (ULID o UUIDv7), nunca
   autoincrementales de la base de datos. Un local sin conexion tiene que
   poder crear registros sin colisionar con otro local.
2. **Todo registro transaccional lleva su `local_id`.**
3. **La cuenta del pensionista es un libro de movimientos**, no un campo de
   saldo que se actualiza. El saldo es una proyeccion. Asi dos locales sin
   conexion nunca producen un conflicto de escritura.
4. **El inventario tambien es un libro de movimientos** con origen rastreable;
   el stock es la suma.
5. **La venta guarda snapshot inmutable** de precio, costo, receta aplicada,
   utilidad y margen al cerrarse. Si no se guarda en ese momento, despues es
   imposible reconstruirlo.
6. **Las tablas las crea Flyway.** Hibernate esta en `validate` y ahi se queda.
7. **Todo evento que sale del servicio pasa por el outbox**, escrito en la
   misma transaccion que el cambio de datos.

---

## 6. Convenciones

- Paquetes y carpetas en minuscula, sin guiones, sin tildes.
- Clases en español, como el negocio: `CuentaPensionista`, `PreTicket`,
  `TurnoCaja`. El dominio se lee igual en el codigo y en el backlog.
- Un caso de uso por clase en `aplicacion/`, con nombre de accion:
  `VincularPreTicket`, `CerrarCuentaEnCaja`, `RegistrarCompra`.
- Los casos de uso corresponden casi uno a uno con las historias del backlog.
  Vale la pena citar la historia en el javadoc de la clase.

---

## 7. Errores tipicos que conviene no cometer

- Poner reglas de negocio en el controlador.
- Inyectar el repositorio de otro modulo "solo por esta vez".
- Crear un paquete `dominio/` lleno de clases con getters y setters: eso no
  es dominio, es un DTO con otro nombre.
- Poner `@Entity` en la entidad del dominio de un modulo con reglas.
- Usar `ddl-auto: update` para salir del paso.
- Un campo `saldo` que se actualiza a mano.
- Inventar tipos de evento en un solo servicio: los eventos compartidos viven
  en el modulo `contratos`.

---

## 8. El microservicio de facturacion

`servicio-facturacion` es un solo contexto, asi que no tiene modulos por
dentro, solo las capas:

```
pe.rikos.facturacion/
├── api/
├── aplicacion/
├── dominio/
└── infraestructura/
    ├── factivo/       el adaptador del proveedor
    ├── correo/        envio del comprobante al cliente
    └── mensajeria/    colas de entrada y salida
```

El dominio declara un puerto `ProveedorFacturacion` y **no sabe que Factivo
existe**. Si mañana se cambia de proveedor, se escribe otro adaptador en
`infraestructura/` y el dominio no se toca. Dado lo que quedo sin confirmar
de esa API, ese aislamiento es un seguro real.

Dos cosas que este servicio debe respetar siempre:

- **La emision se serializa por serie.** El proveedor no acepta clave de
  idempotencia, asi que no puede haber dos comprobantes en vuelo de la misma
  serie.
- **Ante una respuesta perdida no se reintenta a ciegas.** Se consulta el
  listado del dia y se reconcilia antes de volver a enviar.

El detalle esta en el documento de decisiones tecnicas del proyecto.

---

## 9. Convencion de migraciones Flyway

Cada servicio con base de datos propia (`servicio-operacion`, `servicio-facturacion`)
tiene su carpeta `src/main/resources/db/migration/` versionada de forma independiente:
no comparten numeracion porque no comparten esquema.

**Nombre de archivo:** `V<numero>__<descripcion_en_snake_case>.sql`

- `<numero>` es secuencial dentro del servicio (V1, V2, V3...), sin ceros a la izquierda.
- `<descripcion_en_snake_case>` va en espanol, en minusculas, palabras separadas por
  guion bajo. Ejemplos ya aplicados: `V1__inicial.sql`, `V2__event_publication.sql`.
- Doble guion bajo (`__`) entre el numero y la descripcion: lo exige Flyway.

**Reglas duras:**

1. Una migracion ya aplicada en cualquier ambiente **no se edita nunca**. Si el esquema
   necesita cambiar, se agrega una migracion nueva con el siguiente numero. Asi se agrego
   `V2__event_publication.sql`: en vez de tocar `V1__inicial.sql`.
2. `ddl-auto: validate` en Hibernate no es negociable. Hibernate compara el esquema
   contra las entidades y falla el arranque si no coinciden — eso es lo que obliga a
   que toda la verdad del esquema pase por Flyway. Cuando hace falta saber que DDL
   exacto espera una entidad (por ejemplo una de un starter externo, no escrita por
   nosotros), se puede volcar sin tocar la base real con las propiedades JPA
   `jakarta.persistence.schema-generation.scripts.action=create` +
   `...database.action=none`, y de ahi pasar el resultado a una migracion de Flyway.
   Eso no es lo mismo que `ddl-auto: update`: no se le pide nunca a Hibernate que
   toque el esquema real, solo que imprima que esperaria encontrar.
3. `flyway.baseline-on-migrate: true` esta activado en ambos servicios para que una
   base de datos existente sin historial de Flyway no rompa el primer arranque.
4. Si el servicio usa `spring-modulith-starter-jpa` junto con JPA (como los dos de
   este proyecto), Spring Modulith registra su propio libro de eventos publicados en
   una entidad JPA (`event_publication`, mas `event_publication_archive` si se activa
   el modo de archivado). Esa tabla no la genera Flyway sola: hay que migrarla a mano
   como cualquier otra, y es facil olvidarla porque no aparece en ningun diagrama de
   dominio nuestro. `V2__event_publication.sql` es esa migracion en ambos servicios.

## 10. Puerto de Postgres en desarrollo

En `docker-compose.yml` el contenedor de Postgres mapea `5433:5432` (puerta del host
distinta a la puerta de siempre) en vez de `5432:5432`. No es el estandar de Postgres:
es porque en la maquina donde se armo este esqueleto ya habia un Postgres nativo,
fuera de Docker, escuchando en el 5432 del host, y las conexiones desde afuera del
contenedor (la app Java, un cliente psql del host) caian en ese Postgres nativo en vez
de en `rikos-postgres`, con el sintoma enganoso de "password authentication failed"
porque en realidad era un servidor distinto con otras credenciales.

Si tu maquina no tiene otro Postgres compitiendo por el 5432, podes volver el mapeo a
`5432:5432` y quitar el `DB_URL` explicito de los `application.yaml`; si lo dejas en
5433 no hace falta tocar nada mas, ya esta enchufado de punta a punta.
