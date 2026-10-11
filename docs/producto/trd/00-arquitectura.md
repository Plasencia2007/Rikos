# Arquitectura

## Estructura del repositorio

```
rikos/
├── docs/                    Especificación (este método) y planes de trabajo
├── rikos-backend/           Backend Java
│   ├── contratos/           Tipos compartidos: Identificador (ULID), Dinero, Resultado
│   ├── gateway/              Spring Cloud Gateway, puerta única de entrada
│   ├── servicio-operacion/   Monolito modular: toda la operación del restaurante
│   ├── servicio-facturacion/ Microservicio: emisión de comprobantes electrónicos
│   └── doc/                  Bitácora de decisiones técnicas + historias
└── rikos-web/                Frontend React + TypeScript + Vite
```

**Un repositorio, tres servicios desplegables.** Que el código viva junto
no lo hace monolítico: cada servicio tiene su propia base de datos, su
puerto y su ciclo de despliegue. Están juntos porque los cambios cruzan
la frontera -- una columna nueva toca la migración, el DTO y el tipo del
frontend -- y separarlos convertiría cada cambio en dos pull requests
que deben entrar coordinados.

## Dos servicios, no doce

El criterio de corte es uno solo: **se separa lo que no necesita correr
en el local sin Internet**. Todo lo que debe funcionar durante un corte
se queda junto, porque además comparte transacción.

Por eso la facturación electrónica es el único microservicio: es
asíncrona por naturaleza, depende de un tercero (SUNAT) y no participa
de la transacción del cierre de venta.

## El local opera sin Internet

Cada local tiene un equipo pequeño en su red con la base de datos, la
API y el agente de impresión. Los dispositivos del POS se conectan a ese
equipo por la red del local, no salen a Internet para tomar un pedido.
Internet sirve para respaldar en la nube, enviar los comprobantes a
SUNAT y que los pensionistas consulten su cuenta.

## Cinco decisiones de modelado que no se pueden revertir barato

Por eso están desde el primer día, antes de que el código las necesitara:

1. Los identificadores se generan en el origen (ULID), nunca
   autoincrementales -- necesario para que un nodo local pueda crear
   registros sin depender de la nube para asignar un id.
2. Cada registro transaccional lleva su local (`local_id`).
3. La cuenta del pensionista es un libro de movimientos; el saldo es una
   suma, no una columna -- se puede reconstruir, auditar y nunca queda
   "desincronizado" de su propio historial.
4. El inventario también es un libro; el stock es una suma, mismo
   motivo.
5. El snapshot económico -- precio, costo, receta aplicada, margen -- se
   escribe al cerrar la venta y no se toca nunca más, aunque después
   cambie el precio del plato o el costo del insumo.

## Módulos del backend

`servicio-operacion` es un monolito modular (Spring Modulith), no un
conjunto de microservicios internos. Los módulos hoy son `identidad`,
`catalogo` y `comun` (transversal: seguridad, web, auditoría). Un módulo
no consulta las tablas de otro directamente -- pide el dato por su
interfaz pública (`@NamedInterface` en el `package-info.java` del
paquete que se expone). Esto se verifica automáticamente
(`ArquitecturaDeModulosTest`, Spring Modulith) -- ver
`docs/pendientes/README.md` por el único ciclo conocido sin resolver
todavía (`comun ↔ identidad`).

Igual regla del lado del frontend: código dentro de `features/x/` solo
importa de otra `features/y/` a través de su `index.ts` público, nunca
de un archivo interno -- verificado con ESLint
(`eslint-plugin-boundaries`).

## Concurrencia en la venta

El POS lo usan varios mozos a la vez sobre las mismas mesas. `pedido` y `detalle_pedido` llevan control de versión optimista (`@Version`, V37): una escritura sobre datos que otro cambió primero falla en vez de pisarlo, y los controladores de venta reintentan hasta 3 veces antes de responder 409. El estado de una mesa no se guarda: se deriva de sus pedidos. Ver DT-055 y DT-056 en `rikos-backend/doc/RIKOS-decisiones-tecnicas.md`.
