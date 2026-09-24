# rikos-backend

Backend del Sistema de Gestion Integral RIKO'S.

Java 21 · Spring Boot 4.1.1 · Maven multi-modulo.

## Estructura

| Modulo | Puerto | Que es |
|---|---|---|
| `gateway` | 8080 | Unica puerta de entrada. Enruta por prefijo de ruta. |
| `servicio-operacion` | 8081 | Monolito modular: identidad, catalogo, menu, pensionistas, venta, caja, inventario, delivery. |
| `servicio-facturacion` | 8082 | Microservicio: emision a Factivo, cola, reconciliacion y correos. Vive solo en la nube. |
| `contratos` | — | Eventos y DTOs compartidos. Java puro, sin Spring. |

El gateway enruta asi:

```
http://localhost:8080/api/**           ->  servicio-operacion
http://localhost:8080/facturacion/**   ->  servicio-facturacion
```

## Levantar el entorno

```bash
docker compose up -d          # postgres, rabbitmq y mailpit
mvn clean install -DskipTests # compila los cuatro modulos
```

Despues, cada servicio se arranca por separado desde el IDE o con:

```bash
cd servicio-operacion && ../mvnw spring-boot:run
```

## Comprobar que todo responde

```
http://localhost:8081/api/salud                  servicio-operacion directo
http://localhost:8082/facturacion/salud          servicio-facturacion directo
http://localhost:8080/api/salud                  a traves del gateway
http://localhost:8080/facturacion/salud          a traves del gateway
```

Si las cuatro responden, el esqueleto esta completo: red, enrutamiento y
base de datos resueltos.

## Perfiles

| Perfil | Donde corre | Que incluye |
|---|---|---|
| `local` | Equipo dentro del restaurante | gateway y servicio-operacion. Sin Config Server ni Eureka: un servicio que busca su configuracion al arrancar no levanta sin Internet. |
| `nube` | Servidor central | Todo, incluido servicio-facturacion. |

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

## Reglas que no se negocian

1. Los identificadores se generan en el origen (ULID), nunca autoincrementales.
2. Todo registro transaccional lleva su `local_id`.
3. La cuenta del pensionista es un libro de movimientos, no un campo de saldo.
4. El inventario tambien es un libro de movimientos; el stock es la suma.
5. La venta guarda snapshot inmutable de precio, costo y receta al cerrarse.
6. Las tablas las crea Flyway. Hibernate esta en `validate`, nunca en `update`.
7. Dentro de cada modulo, el paquete `dominio` no lleva anotaciones de Spring ni de JPA.

El detalle esta en los documentos del proyecto: backlog y decisiones tecnicas.

## Pendiente antes de nada serio

Desde el Sprint 1, `ConfiguracionSeguridad` ya exige login (US-002) o PIN
(US-003) para todo lo que no sea `/salud` o `/auth/**`, y la administracion
de locales/usuarios (`/locales/**`, `/usuarios/**`, `/roles/**`) exige rol
`administrador central` o `administrador de local`.

**ADM-29 parcial:** se puede asignar rol y locales a un usuario, pero las
excepciones de permiso por usuario individual todavia no existen. Eso, junto
con el resto de US-004 (permisos granulares por modulo y accion), llega en
el sprint 3.
