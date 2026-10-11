# RIKO'S — Sistema de Gestión Integral

Sistema multilocal para el Restaurante Riko's: restaurante criollo peruano que atiende
público general y una proporción importante de estudiantes pensionistas con cuenta
corriente.

Proyecto del curso de Ingeniería de Software — Universidad Peruana Unión, Facultad de
Ingeniería, E.P. Ingeniería de Sistemas. **Cliente real.**

---

## Qué resuelve

Cinco problemas que hoy se manejan en cuaderno:

1. **La cuenta corriente del pensionista.** Consumos, abonos, límite de crédito,
   cierre mensual con arrastre, y un portal donde el estudiante y su apoderado
   consultan el saldo.
2. **La venta en sus cuatro canales**: mesa, para llevar, delivery propio y recojo
   programado, con mesas mixtas donde conviven clientes regulares y pensionistas.
3. **El costo real del plato.** Recetas con subrecetas, costeo por última compra,
   producción, Kardex y conteos físicos que no detienen la operación.
4. **La continuidad sin Internet.** El local debe poder vender, comandar, cobrar y
   cargar consumos aunque se caiga la conexión, y sincronizar después sin duplicar
   nada.
5. **El control administrativo**: auditoría de descuentos, anulaciones, ajustes y
   cierres, más reportes exportables.

---

## Estructura del repositorio

```
rikos/
├── docs/                    Capturas y planes de trabajo
├── rikos-backend/           Backend Java
│   ├── contratos/           Tipos compartidos: Identificador (ULID), Dinero, Resultado
│   ├── gateway/             Spring Cloud Gateway, puerta única de entrada
│   ├── servicio-operacion/  Monolito modular: toda la operación del restaurante
│   ├── servicio-facturacion/ Microservicio: emisión de comprobantes electrónicos
│   └── doc/                 Modelo de datos y decisiones técnicas
└── rikos-web/               Frontend React + TypeScript + Vite
```

**Un repositorio, tres servicios desplegables.** Que el código viva junto no lo hace
monolítico: cada servicio tiene su propia base de datos, su puerto y su ciclo de
despliegue. Están juntos porque los cambios cruzan la frontera —una columna nueva toca
la migración, el DTO y el tipo del frontend— y separarlos convertiría cada cambio en
dos pull requests que deben entrar coordinados.

---

## Arquitectura en una página

**Dos servicios, no doce.** El criterio de corte es uno solo: *se separa lo que no
necesita correr en el local sin Internet*. Todo lo que debe funcionar durante un corte
se queda junto, porque además comparte transacción.

Por eso la facturación electrónica es el único microservicio: es asíncrona por
naturaleza, depende de un tercero y no participa de la transacción del cierre de venta.

**El local opera sin Internet.** Cada local tiene un equipo pequeño en su red con la
base de datos, la API y el agente de impresión. Los dispositivos del POS se conectan a
ese equipo por la red del local, no salen a Internet para tomar un pedido. Internet
sirve para respaldar en la nube, enviar los comprobantes a SUNAT y que los pensionistas
consulten su cuenta.

**Cinco decisiones de modelado que no se pueden revertir barato**, y por eso están desde
el primer día:

- Los identificadores se generan en el origen (ULID), nunca autoincrementales.
- Cada registro transaccional lleva su local.
- La cuenta del pensionista es un libro de movimientos; el saldo es una suma, no una
  columna.
- El inventario también es un libro; el stock es una suma.
- El snapshot económico —precio, costo, receta aplicada, margen— se escribe al cerrar
  la venta y no se toca nunca más.

---

## Stack

| Capa | Tecnología |
|---|---|
| Backend | Java 21 · Spring Boot 4.1 · Spring Modulith · Maven multimódulo |
| Puerta de entrada | Spring Cloud Gateway (variante WebMVC) |
| Base de datos | PostgreSQL 17 · Flyway (`ddl-auto: validate`, siempre) |
| Mensajería | RabbitMQ en la nube · patrón outbox en el nodo del local |
| Frontend | React 19 · TypeScript · Vite · carga perezosa por ruta |
| Infraestructura | Docker Compose |

---

## Cómo levantarlo

**Requisitos:** Java 21, Node 20 o superior, Docker Desktop abierto.

```bash
# 1. Infraestructura
cd rikos-backend
copy .env.example .env          # y completa los valores
docker compose up -d            # Postgres, RabbitMQ y Mailpit

# 2. Backend
./mvnw clean install
./mvnw -pl gateway spring-boot:run
./mvnw -pl servicio-operacion spring-boot:run
./mvnw -pl servicio-facturacion spring-boot:run

# 3. Frontend
cd ../rikos-web
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

**El puerto 5433 no es un error.** Está remapeado a propósito, porque el 5432 suele
estar ocupado por una instalación previa de Postgres. El síntoma cuando se olvida es
engañoso: «password authentication failed», que hace pensar en credenciales cuando el
problema es de puerto.

### Credenciales de desarrollo

`admin` / `cambiar123`, con PIN `1234`. **Solo para desarrollo**: no deben existir en
el perfil de nube.

---

## Documentación

Lo que hay que leer antes de tocar código:

| Documento | Dónde | Qué contiene |
|---|---|---|
| Modelo de datos lógico | `rikos-backend/doc/RIKOS-modelo-de-datos.md` | 69 tablas, sus claves, índices, integridad referencial y trazabilidad hasta el requisito que las justifica |
| Decisiones técnicas | `rikos-backend/doc/RIKOS-decisiones-tecnicas.md` | DT-001 a DT-047 y los pendientes abiertos. Por qué el sistema es como es |
| Estructura del backend | `rikos-backend/ESTRUCTURA.md` | Módulos y capas |
| Estructura del frontend | `rikos-web/ESTRUCTURA.md` | Superficies y organización por funcionalidad |

El modelo de datos manda sobre cualquier suposición. Si el código lo contradice, el
error está en el código o el documento quedó desactualizado — en ambos casos hay que
resolverlo, no ignorarlo.

---

## Reglas de trabajo

**Sobre la base de datos**

- Flyway es el dueño del esquema. Hibernate queda en `validate`, nunca en `update`, ni
  temporalmente para salir del paso.
- Una migración ya aplicada **no se edita jamás**. Si hay que corregir algo, se agrega
  otra. Editar una aplicada rompe la suma de verificación y deja la base de un
  compañero en un estado que ya no se puede reproducir.
- Ningún `DROP TABLE` en una migración. En cuanto un entorno tenga datos, esa operación
  los destruye sin aviso.

**Sobre el código**

- Ningún color ni medida escrito dentro de un componente: todo sale de los tokens de
  diseño.
- Los permisos se validan **siempre en el backend**. Ocultar un botón es comodidad, no
  seguridad.
- Un módulo no consulta las tablas de otro: pide el dato por su interfaz pública.

**Sobre los secretos**

El `.env` no se sube nunca. Un token en el historial de git es un token comprometido,
aunque el repositorio sea privado.

---

## Estado

Ver `docs/producto/plan/README.md` para el detalle y las rutas para
probar cada cosa en vivo.

| Sprint | Contenido | Estado |
|---|---|---|
| 0 | Esqueleto que camina, migraciones, tipos base | Completo |
| 1 | Empresa y locales, usuarios, autenticación, PIN del mozo | Completo |
| 1+ | Sistema de diseño, dashboard, horarios por día, permisos granulares y roles | Completo |
| 2 | Catálogo de productos, unidades y presentaciones, proveedores | Completo |
| 3 | Permisos granulares, auditoría transversal, motivos, verificación de fronteras | Completo (HT-08 movido al sprint 10) |
| 4 | Costos y menú | Completo -- recetas, menú normal y extra, programación semanal, agotados por local |
| 5 a 14 | Pensionistas, venta en mesa, caja, inventario, offline, comprobantes, delivery, portal, reportes | Pendiente -- ver `docs/producto/plan/` |

Quince sprints en total. El plan completo está en el product backlog.

---

## Equipo

| Rol | Nombre |
|---|---|
| | |
| | |
| | |
| Asesor | |
