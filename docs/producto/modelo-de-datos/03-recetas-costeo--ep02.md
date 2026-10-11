<!-- Extraído de rikos-backend/doc/RIKOS-modelo-de-datos.md (fuente completa, con la numeración de sección original intacta -- ver esa numeración si algún comentario en el código cita p. ej. "13.1"). Partido por bloque el 2026-09-29 per docs/METODO-spec-driven.md §5. -->

# 5. Bloque 3 — Recetas y costeo

Corresponde a EP-02, historia US-010. También central, sin `local_id`.

## 5.1 `receta`

| Atributo | Tipo | Nulo | Descripción |
|---|---|---|---|
| `id` | identificador | No | PK |
| `producto_id` | identificador | No | FK → `producto`. El plato o la subreceta que produce |
| `rendimiento` | cantidad | No | Cuánto produce una ejecución, **siempre en la unidad base del producto** |
| `porciones` | entero | No | En cuántas porciones se divide el rendimiento |
| `costo_por_porcion` | costo | Sí | **Derivado**, recalculado al cambiar un costo de insumo |
| `costo_total` | costo | Sí | Derivado |
| `activo` | booleano | No | |
| `version` | entero | No | |

**Reglas**

- Una receta activa por producto.
- **RN-045.** Modificar una receta actualiza la receta maestra y aplica a las ventas **nuevas**. Las anteriores conservan su copia histórica.
- `costo_por_porcion` es un valor derivado que se guarda por rendimiento, no una fuente de verdad. Se recalcula cuando cambia el costo de cualquier insumo de su árbol, en cascada hacia las recetas que la usan como subreceta.
- **RN-012.** Ver los costos requiere permiso `ver_costos`. Es un filtro de la capa de aplicación, no una columna.

## 5.2 `ingrediente_receta`

| Atributo | Tipo | Nulo | Descripción |
|---|---|---|---|
| `id` | identificador | No | PK |
| `receta_id` | identificador | No | FK → `receta` |
| `producto_id` | identificador | No | FK → `producto`. Insumo o subreceta |
| `cantidad` | cantidad | No | |
| `unidad_id` | identificador | No | FK → `unidad_medida` |
| `orden` | entero | No | |

**Reglas**

- **RN-043.** Un ingrediente puede referenciar un insumo **o una subreceta**, y ahí está la recursividad del modelo: `producto` → `receta` → `ingrediente_receta` → `producto`.
- **Restricción que hay que validar en la aplicación, no en la base:** una receta no puede contenerse a sí misma ni directa ni indirectamente. Un ciclo en el árbol de recetas produce un cálculo de costo infinito. Debe comprobarse al guardar, recorriendo el árbol.
- **Supuesto de modelado:** la profundidad de anidamiento no se limita. La especificación no fija un máximo. Conviene, sin embargo, registrar la profundidad alcanzada, porque un árbol de cinco niveles hace lento el recálculo de costos.

## 5.3 Cómo se calcula el costo

Registrado aquí porque es la regla que más consecuencias tiene y no está escrita en un solo lugar de la especificación.

```
costo_total(receta) = Σ para cada ingrediente:
     cantidad_convertida_a_unidad_base × costo_unitario(producto)

costo_unitario(producto) =
     si tiene receta activa (subreceta) → costo_total(su receta) / rendimiento
     si no                              → costo_ultima_compra

costo_por_porcion(receta) = costo_total(receta) / porciones
```

La conversión usa `conversion_unidad` del producto del ingrediente, en cualquiera de los dos sentidos, y se resuelve **siempre a la unidad base del producto** antes de multiplicar. Sin conversión, la receta no se guarda. Si algún ingrediente no tiene costo, `costo_total` queda nulo (receta incompleta), nunca cero.

**Corregido en el sprint 4 (2026-09-28):** la versión anterior multiplicaba la cantidad de una subreceta por su `costo_por_porcion`, correcto solo si la cantidad estaba en porciones. Se quitaron `nombre` y `unidad_rendimiento_id`. Razones en `plan/sprint-04-costos-y-menu.md`.

**Al vender el plato** se descuenta la receta completa, incluidas las cantidades de las subrecetas (`04` § Producción), pero **sin duplicar el consumo**: si la subreceta ya se produjo y su producción descontó los insumos, la venta descuenta la subreceta como producto, no sus insumos otra vez. Es el caso crítico 11 y la fuente más probable de un error de inventario.

---

