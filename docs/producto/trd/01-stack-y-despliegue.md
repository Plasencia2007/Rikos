# Stack y despliegue

## Stack

| Capa | Tecnología |
|---|---|
| Backend | Java 21 · Spring Boot 4.1 · Spring Modulith · Maven multimódulo |
| Puerta de entrada | Spring Cloud Gateway (variante WebMVC) |
| Base de datos | PostgreSQL 17 · Flyway (`ddl-auto: validate`, siempre) |
| Mensajería | RabbitMQ en la nube · patrón outbox en el nodo del local |
| Frontend | React 19 · TypeScript · Vite · carga perezosa por ruta |
| Infraestructura | Docker Compose |

## Cómo levantarlo

**Requisitos:** Java 21, Node 20 o superior, Docker Desktop abierto.

```bash
# 1. Infraestructura
cd rikos-backend
copy .env.example .env          # y completa los valores
docker compose up -d            # Postgres, RabbitMQ y Mailpit

# 2. Backend -- OJO: cada mvnw se corre desde el directorio del propio
# modulo, nunca desde la raiz del reactor con -pl/-am (falla con "No
# plugin found for prefix 'spring-boot'").
./mvnw clean install
cd gateway && ../mvnw spring-boot:run
cd ../servicio-operacion && ../mvnw spring-boot:run
cd ../servicio-facturacion && ../mvnw spring-boot:run

# 3. Frontend
cd ../../rikos-web
npm install
npm run dev
```

### Direcciones

| Qué | Dónde |
|---|---|
| Panel de administración | http://localhost:5173/admin |
| POS | http://localhost:5173/pos |
| Kiosco del pensionista | http://localhost:5173/kiosco |
| Portal del pensionista | http://localhost:5173/portal |
| Vista del motorizado | http://localhost:5173/motorizado |
| Salud vía gateway | http://localhost:8080/api/salud |
| RabbitMQ | http://localhost:15672 |
| Correos de prueba | http://localhost:8025 |
| PostgreSQL | localhost:**5433** |

**El puerto 5433 no es un error.** Está remapeado a propósito, porque el
5432 suele estar ocupado por una instalación previa de Postgres. El
síntoma cuando se olvida es engañoso: «password authentication failed»,
que hace pensar en credenciales cuando el problema es de puerto.

### Credenciales de desarrollo

`admin` / `cambiar123`, con PIN `1234`. **Solo para desarrollo** -- si el
sistema arranca con perfil `nube` y detecta ese usuario semilla,
`GuardaSemillaDesarrollo` lo desactiva automáticamente al vuelo (ver
DT-039 y siguientes en la bitácora de decisiones).

## Cuando Docker/Postgres se cae (WSL2, frecuente en desarrollo)

Si algo falla con "Connection refused" en :5433/:8081/:8080:

1. `docker ps` -- si aparece vacío, los contenedores no están corriendo.
2. `docker compose up -d` desde `rikos-backend/`.
3. Esperar `docker inspect --format '{{.State.Health.Status}}' rikos-postgres`
   en `healthy` antes de asumir que hay un bug real.
4. Si `docker ps` da error de conexión al daemon (no solo lista vacía),
   Docker Desktop mismo no está corriendo -- hay que relanzarlo.
