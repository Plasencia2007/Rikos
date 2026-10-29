# Empezar — guía para el equipo

Todo lo que hay que hacer desde que clonas el repositorio hasta ver el sistema
funcionando en tu máquina. Si algo de acá falla, avisa en el grupo antes de
improvisar una solución: probablemente le pase a otro después.

---

## 1. Instalar (una sola vez)

| Qué | Versión | Cómo verificar |
|---|---|---|
| **JDK 21** | 21 exacto, no 17 ni 23 | `java -version` |
| **Node.js** | 20 o superior | `node -v` |
| **Docker Desktop** | La actual | `docker --version` |
| **Git** | La actual | `git --version` |

Opcionales pero recomendados: **IntelliJ IDEA** o **VS Code**, y **DBeaver** para
mirar la base de datos.

**Docker Desktop tiene que estar abierto** cada vez que trabajes, no solo instalado.
Es el error más común del primer día: si no está corriendo, nada levanta y el mensaje
de error no lo dice claramente.

---

## 2. Clonar y configurar git

```bash
git config --global core.autocrlf true
git clone https://github.com/Plasencia2007/Rikos.git
cd Rikos
```

Lo del `core.autocrlf` no es opcional. Todos trabajamos en Windows y el proyecto tiene
scripts que necesitan finales de línea de Linux; sin esa configuración, `mvnw` deja de
funcionar y el error es confuso.

---

## 3. Crear los archivos de configuración

Hay dos archivos `.env` que **no están en el repositorio y nunca lo estarán**, porque
contienen secretos. Cada uno se crea a partir de su ejemplo.

### Backend

```bash
cd rikos-backend
copy .env.example .env
```

Abre el `.env` y completa **`JWT_SECRET`**, que viene vacío. Genera uno con:

```bash
node -e "console.log(require('crypto').randomBytes(32).toString('base64'))"
```

Pega el resultado. Los demás valores sirven como están para desarrollo. Los cuatro
campos de `FACTIVO_` se quedan vacíos: la facturación electrónica llega en el
sprint 11.

### Frontend

```bash
cd ../rikos-web
copy .env.example .env
```

Este no necesita cambios para desarrollo.

---

## 4. Levantar la infraestructura

```bash
cd rikos-backend
docker compose up -d
docker compose ps
```

Tienen que aparecer tres contenedores en estado `healthy`: `rikos-postgres`,
`rikos-rabbitmq` y `rikos-mailpit`. Si alguno dice `unhealthy` o se reinicia solo,
espera treinta segundos y vuelve a mirar; si sigue mal, avisa en el grupo.

**Postgres está en el puerto 5433, no en el 5432.** Está remapeado a propósito porque
el 5432 suele estar ocupado por una instalación previa de Postgres en Windows. Si
alguna vez ves el error *«password authentication failed»*, casi siempre no es la
contraseña: es que te estás conectando al Postgres equivocado.

### Sin Docker (si Docker consume mucha RAM)

Se puede usar el PostgreSQL instalado en Windows (puerto 5432). RabbitMQ y Mailpit no
hacen falta todavía: el código solo tiene la dependencia, no envía ni escucha mensajes.

Una sola vez, con el usuario administrador de tu Postgres, crea el usuario y las bases:

```bash
"/c/Program Files/PostgreSQL/18/bin/psql.exe" -U postgres -h localhost -p 5432 -c "CREATE ROLE rikos LOGIN PASSWORD 'rikos';" -c "CREATE DATABASE rikos_operacion OWNER rikos;" -c "CREATE DATABASE rikos_facturacion OWNER rikos;"
```

Después, cada vez: `rikos-backend\levantar-sin-docker.bat` abre `servicio-operacion` y el
gateway en sus ventanas (con `operacion` como argumento, solo levanta ese servicio en la
misma ventana). El script apunta los servicios al 5432 con la variable `DB_URL`; el
código no cambia. Las bases empiezan vacías: Flyway crea las tablas al primer arranque.

El script activa el perfil `demo`, que carga **datos de prueba** inventados: mozos `mozo1` y
`mozo2` (PIN 1111 y 2222), `cajero1`, `cocina1` y `adminlocal` (contraseña `demo123`), mesas,
platos, recetas, stock, pensionistas (PIN de kiosco 111111, 222222 y 333333) y el menú de la
semana. Son distintos de los datos del sistema que crean las migraciones. Detalle en
`rikos-backend/doc/historias/datos-de-prueba.md`.

---

## 5. Levantar el backend

```bash
./mvnw clean install
```

La primera vez descarga medio Internet y tarda varios minutos. Las siguientes son
rápidas.

Después, **tres servicios, cada uno en su propia terminal**:

```bash
./mvnw -pl gateway spring-boot:run
./mvnw -pl servicio-operacion spring-boot:run
./mvnw -pl servicio-facturacion spring-boot:run
```

En Windows también existe `levantar.bat`, que los arranca juntos y deja la salida en
un archivo de texto.

Para comprobar que están vivos, abre estas dos direcciones en el navegador:

- http://localhost:8080/api/salud
- http://localhost:8080/facturacion/salud

Las dos deben responder `ok`, con el perfil activo y la conexión a la base en
`conectada`.

---

## 6. Levantar el frontend

```bash
cd ../rikos-web
npm install
npm run generar-tipos     # el backend tiene que estar corriendo
npm run dev
```

Vite te dice en qué puerto quedó, normalmente el **5173**.

**El paso `generar-tipos` no es opcional la primera vez.** Los tipos de la API se
generan a partir del backend en marcha y **no están en el repositorio**: si te lo
saltas, el frontend no compila y el error no dice claramente por qué. Repítelo cada vez
que alguien cambie un endpoint.

---

## 7. Comprobar que todo funciona

Entra a **http://localhost:5173/admin** e inicia sesión:

- Usuario: `admin`
- Contraseña: `cambiar123`

Deberías ver el panel con la barra lateral, el selector de local mostrando "San Isidro",
y las secciones de Locales y Usuarios activas. El resto del menú aparece atenuado: son
los módulos de los sprints que vienen.

Después prueba **http://localhost:5173/pos**, que pide un PIN. El del usuario admin es
`1234`.

**Esas credenciales son solo de desarrollo.** Vienen sembradas en una migración y no
deben existir nunca en el entorno de producción.

### Las cinco direcciones del sistema

| Superficie | URL |
|---|---|
| Panel de administración | http://localhost:5173/admin |
| POS | http://localhost:5173/pos |
| Kiosco del pensionista | http://localhost:5173/kiosco |
| Portal del pensionista | http://localhost:5173/portal |
| Vista del motorizado | http://localhost:5173/motorizado |

Las tres últimas todavía están vacías: son de los sprints 8, 12 y 13.

### Herramientas de apoyo

| Qué | Dónde |
|---|---|
| RabbitMQ | http://localhost:15672 — usuario y clave `rikos` |
| Correos de prueba | http://localhost:8025 |
| PostgreSQL | `localhost:5433`, base `rikos_operacion`, usuario y clave `rikos` |

---

## 8. Antes de escribir código, lee esto

No es opcional. Dos documentos, y con leerlos por encima alcanza para empezar:

| Documento | Para qué |
|---|---|
| `rikos-backend/doc/RIKOS-modelo-de-datos.md` | Las 69 tablas, sus claves e índices, y **por qué** cada una es como es |
| `rikos-backend/doc/RIKOS-decisiones-tecnicas.md` | DT-001 a DT-047: las decisiones tomadas y su motivo |

**El modelo de datos manda sobre cualquier suposición.** Si lo que vas a escribir lo
contradice, no lo escribas: avisa primero. O el documento está desactualizado —y hay que
corregirlo— o la idea está equivocada. Las dos cosas se resuelven conversando, ninguna
programando encima.

---

## 9. Cómo trabajamos

**Nadie empuja a `main`.** Una rama por historia de usuario:

```bash
git checkout -b sprint-2/US-008-catalogo-productos
```

Cuando termines: pull request, y que un compañero lo revise antes de fusionar.

**Los mensajes de commit en español, explicando el qué y el por qué.** «Agrega costo
inicial provisional al producto, porque las compras llegan en el sprint 9» vale mucho
más que «update Producto.java». Ese historial lo va a leer el profesor.

Antes de empezar a trabajar cada día:

```bash
git checkout main
git pull
git checkout -b sprint-N/US-XXX-descripcion
```

---

## 10. Cuatro reglas que no se negocian

**No se edita una migración de Flyway que ya se aplicó.** Nunca, ni para arreglar una
coma. Si hay que corregir algo, se agrega otra migración. Editar una aplicada rompe la
suma de verificación y deja la base de un compañero en un estado que ya no se puede
reproducir — y el error aparece en la máquina de él, no en la tuya.

**Ningún `DROP TABLE` en una migración.** En cuanto un entorno tenga datos reales, eso
los destruye sin aviso y sin vuelta atrás.

**El `.env` no se sube jamás.** Un token en el historial de git es un token comprometido,
aunque el repositorio sea privado.

**La carpeta de tipos generados no se edita a mano.** Se regenera con
`npm run generar-tipos` mientras el backend está corriendo. Si editas el archivo
generado, tu cambio desaparece en la siguiente regeneración y nadie va a entender por
qué algo dejó de funcionar.

---

## 11. Cuando algo falle

| Síntoma | Casi siempre es |
|---|---|
| Nada levanta, errores raros de conexión | Docker Desktop cerrado |
| «password authentication failed» en Postgres | Te conectaste al 5432 en vez del 5433 |
| `./mvnw` no ejecuta o da error de formato | Falta `git config --global core.autocrlf true` |
| El backend arranca y se cae al validar el esquema | Faltan migraciones: `git pull` y vuelve a levantar |
| El frontend compila pero la API responde 401 | Tu sesión expiró; vuelve a iniciar sesión |
| El frontend responde 403 en todo | Tu usuario no tiene el permiso; revisa su rol |
| Un tipo de TypeScript no coincide con la API | Corre `npm run generar-tipos` con el backend arriba |

Si el problema no está en esta tabla, escríbelo en el grupo con el mensaje de error
completo. No lo resuelvas por tu cuenta cambiando configuración al azar: lo más probable
es que el arreglo correcto sea para todos, no solo para ti.

---

## 12. Resumen para volver rápido

Ya configurado, el día a día es:

```bash
# Terminal 1
cd rikos-backend && docker compose up -d && ./mvnw -pl gateway spring-boot:run
# Terminal 2
cd rikos-backend && ./mvnw -pl servicio-operacion spring-boot:run
# Terminal 3
cd rikos-backend && ./mvnw -pl servicio-facturacion spring-boot:run
# Terminal 4
cd rikos-web && npm run dev
```

Y al terminar el día, `docker compose down` para que los contenedores no queden
consumiendo memoria.
