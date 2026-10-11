# US-010 — Recetas y subrecetas valorizadas por porción

Sprint 4, entrega 1 de 4 (2026-09-28). Especificación y decisiones en
`docs/producto/plan/sprint-04-costos-y-menu.md`; la regla de cálculo en
DT-054.

## Qué se pidió

Registrar la receta de un plato vendible o de una subreceta (insumos y/o
subrecetas, cantidades, unidades, rendimiento, porciones), calcular su
costo por porción con el último costo de cada insumo, incorporar el costo
de las subrecetas, recalcular cuando cambia un costo, y ocultar los
costos a quien no tiene permiso. Todo cambio va a la bitácora.

## Qué se construyó

**Base de datos — `V20__recetas.sql`.** `receta` (producto, rendimiento
en la unidad base del producto, porciones, `costo_total` y
`costo_por_porcion` derivados, `version`) con índice único parcial "una
receta activa por producto". `ingrediente_receta` (producto, cantidad,
unidad, orden) con único `(receta, producto)` e índice por producto para
el recálculo en cascada. Distinto del modelo original: sin `nombre` ni
`unidad_rendimiento_id` (razones en DT-054).

**Backend (`catalogo`).**

- `CalculadoraCostoReceta`: conversión a unidad base (en los dos sentidos
  de `conversion_unidad`), costo unitario (receta activa → `costo_total /
  rendimiento`; si no, `costo_ultima_compra`), cálculo de la receta,
  recálculo en cascada hacia arriba y detección de ciclos.
- `ComposicionReceta`: validación común de alta y edición; cada error de
  ingrediente sale con el `productoId` culpable (`FalloReceta`).
- `CrearReceta` / `ActualizarReceta`: auditan `cambio_receta` con la foto
  antes/después (nombre, rendimiento, porciones, "Cebolla: 1 kg"...).
- `RegistrarCostoManual` ahora dispara el recálculo en cascada.
- `EliminarProducto` rechaza con `TIENE_HISTORIAL` un producto que tiene
  receta o que es ingrediente de una (antes el FK reventaba como error SQL).
- `RecetaController` (`GET /recetas`, `GET /recetas/{id}`, `POST`,
  `PUT /recetas/{id}`): sin `recetas.ver_costos`, todos los costos salen
  nulos y `costosVisibles=false`. Rutas protegidas con `recetas.ver`,
  `recetas.crear` y `recetas.modificar` en `ConfiguracionSeguridad`.
- `ListarAuditoria`: "admin creó / modificó la receta de "Seco de res"".

**Frontend.**

- `PantallaRecetas` (Catálogo y recetas → Recetas): lista con tipo, rinde,
  porciones, ingredientes y costo por porción (solo si llegó), con los
  cuatro estados.
- `PantallaRecetaFormulario` (`/admin/catalogo/recetas/nueva` y `/:id`):
  producto (solo vendibles o subrecetas sin receta), rendimiento con la
  unidad base como sufijo, porciones, filas de ingredientes agrupados en
  insumos / subrecetas. El selector de unidad ofrece solo la base del
  ingrediente y las unidades con conversión a ella. Tarjeta de costo con
  por porción, total, margen (plato) o costo por unidad base
  (subreceta), aviso de qué ingredientes no tienen costo, y nota cuando
  hay cambios sin guardar. Solo lectura para quien no puede escribir.
- Bitácora: filtro "Cambio de receta".

## Qué se dejó fuera a propósito

- **Desactivar recetas** (P-32): ningún criterio lo pide y desactivar una
  subreceta deja sin costo a las recetas que la usan.
- **Versiones históricas (RN-045, CA5):** se cumple cuando la venta copie
  su consumo (sprint 6). Hoy la receta se edita en su lugar y sube
  `version`; la bitácora guarda el antes y el después.
- **CA4 con compra real:** las compras son del sprint 9. El recálculo se
  demuestra con el costo manual; la compra tendrá que llamar a
  `recalcularEnCascada` igual que `RegistrarCostoManual`.
- **Presentaciones de compra como unidad de receta:** nadie escribe una
  receta en sacos.

## Cómo se verificó

**Pruebas automatizadas: 162 en verde** (sin contar `ArquitecturaDeModulosTest`, excluida de la corrida).

- `RecetaTest` (15): el escenario de la demo (seco con 300 g de un aderezo
  que rinde en kg → 2.60), división por porciones, cascada al editar el
  costo de un insumo hasta el plato, cascada al cambiar la subreceta,
  edición que conserva ingredientes sin chocar con la restricción única,
  insumo sin costo → costo nulo, conversión en los dos sentidos, unidad
  sin conversión (con el ingrediente culpable), ciclo directo e indirecto,
  producto que no admite receta, ingrediente que no es insumo ni
  subreceta, segunda receta activa, bitácora con antes y después, borrado
  bloqueado de un insumo usado.
- `RecetaAccesoPorRolTest` (4), con los roles reales del sembrador:
  administrador central ve costos y escribe; administrador de local ve
  costos pero recibe 403 al crear y editar; **cocina ve la receta con
  todos los costos en null** (cabecera, ingredientes y lista) y 403 al
  crear; mozo 403 hasta para leer.
- `ListarAuditoriaTest`: "creó" y "modificó" la receta se distinguen.
- `ArquitecturaDeModulosTest`: solo el ciclo `comun ↔ identidad` ya
  conocido, nada nuevo.

**En vivo** (backend + gateway + Vite, usuario `admin`), con datos
cargados para la demo: cebolla roja S/ 4.00/kg, aceite S/ 10.00/l, carne
de res S/ 32.00/kg, subreceta "Aderezo base" (kg, conversión 1 kg = 1000
g) y plato "Seco de res" (S/ 15.00).

- Aderezo base: 1 kg cebolla + 0.5 l aceite, rinde 1.5 kg, 10 porciones →
  **S/ 9.00 total, S/ 0.90 por porción, S/ 6.00 por kg**; líneas S/ 4.00
  y S/ 5.00.
- Seco de res: 300 g aderezo (el selector ofreció kg y g) + 0.2 kg carne
  + 0.1 kg cebolla → **S/ 1.80 + S/ 6.40 + S/ 0.40 = S/ 8.60, margen 43
  %**.
- Costo de la cebolla a S/ 5.00 → aderezo **S/ 10.00**, seco **S/ 8.90**
  sin tocar las recetas.
- Al crear, el aderezo desapareció de la lista de productos sin receta; el
  aderezo no aparece como ingrediente de sí mismo.

No se verificó en el navegador la vista de solo lectura con un usuario de
cocina (sí en la prueba de API de arriba).

## Ajustes después de la primera revisión del usuario

- **Desplegable de producto vacío:** todos los vendibles y subrecetas ya
  tenían receta, y la pantalla no lo decía. Ahora muestra un aviso con
  "Crear producto". También se alineó "Rendimiento" con "Porciones" (el
  campo sin texto de ayuda se estiraba).
- **"Un plato que rinde en litros":** el rendimiento solo se pide en
  subrecetas; en un plato vendible solo hay porciones y se envía
  `rendimiento = porciones` (la lista muestra "—" en Rinde). Al crear un
  producto que solo se vende, la unidad base sugerida pasa a `und`.
  Verificado en el navegador con "Pollito" (solo Porciones) y "Aderezo
  base" (Rendimiento 1.5 kg + Porciones).

## Encontrado en el camino

- **Docker Desktop no arrancaba**: dos sockets Unix huérfanos
  (`AppData\Local\Docker\run\dockerInference` y
  `AppData\Local\docker-secrets-engine\engine.sock`) que Windows no deja
  borrar. El diálogo ofrecía "Reset to factory defaults", que borraría el
  volumen de Postgres. Arreglo: cerrar Docker, **renombrar** las dos
  carpetas (se recrean solas) y volver a abrirlo. Ya había pasado antes
  (`run_corrupto_old`, `run_old2`).
- **P-30**: el catálogo muestra el costo de los insumos a cualquiera con
  `catalogo.ver`, lo que deja reconstruir a mano el costo que CA6 oculta.
- **P-31**: cambiar la unidad base de un producto que ya está en recetas
  deja cantidades sin conversión.
