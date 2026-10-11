# Fuente: la especificación del dueño

`especificacion-v0.1/` es la especificación funcional que entregó el dueño
de Riko's: los ocho archivos a partir de los cuales se armó el Product
Backlog y, desde ahí, todo lo que está en `docs/producto/`.

| Archivo | Tema |
|---|---|
| `README.md` | Objetivo, alcance confirmado de la primera versión, convenciones, exclusiones |
| `01-alcance-y-reglas.md` | Contexto, actores, canales, multilocal, auditoría, reglas de historial |
| `02-pensionistas.md` | Ficha, cuenta corriente, planes, autoservicio, portal, abonos |
| `03-pos-caja-cocina-delivery.md` | POS y mesas, comandas, disponibilidad, caja, facturación, recojo, delivery |
| `04-menu-recetas-inventario-compras.md` | Catálogo, almuerzo, programación semanal, recetas, producción, inventario, compras |
| `05-usuarios-reportes-multilocal-offline.md` | Permisos, panel, reportes, offline, respaldo y seguridad |
| `06-modelo-de-datos.md` | Modelo conceptual (entidades y relaciones, sin tablas concretas) |
| `07-criterios-de-aceptacion-y-pendientes.md` | Los 20 casos críticos de aceptación y los pendientes funcionales |

## Reglas

- **No se edita.** Es lo que dijo el dueño en la versión 0.1, y sirve
  justamente para comparar contra eso. Si el dueño manda una versión
  nueva, va en otra carpeta (`especificacion-v0.2/`) y se anota qué
  cambió.
- **Se conservaron los nombres originales** porque el backlog y el modelo
  de datos los citan así: cuando un documento dice "`04` § Almuerzo" o
  "caso crítico 11", se refiere a estos archivos.
- **Manda sobre lo que derivamos.** Si algo en `docs/producto/`
  contradice esta fuente, o es una decisión nuestra que tiene que estar
  anotada con su razón, o es un error nuestro. No hay tercera opción.

## Relación con el resto

```
fuente/especificacion-v0.1/   lo que pidió el dueño (fijo)
        ↓
producto/                     lo que vamos a construir y por qué (vivo)
        ↓
rikos-backend/doc/historias/  lo que se construyó y cómo se verificó
```
