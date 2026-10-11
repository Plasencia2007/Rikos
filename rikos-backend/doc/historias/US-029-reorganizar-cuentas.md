# US-029 — Reorganizar cuentas

Sprint 6, entrega 4 de 4 (2026-09-30). Plan en `docs/superpowers/plans/2026-09-30-sprint6-entrega4-reorganizar.md`;
especificación en `docs/producto/plan/sprint-06-vender-en-mesa.md` 3.8.

## Qué se pidió

Que el mozo reorganice una mesa sin perder nada: pasar productos de una persona a otra (o a una persona nueva, que es
dividir la cuenta) con el precio recalculado si cambia el tipo de cliente; mover el pedido a otra mesa libre; y unir
mesas para un grupo grande y separarlas después.

## Qué se construyó

**Base de datos — `V36__etiqueta_persona_en_la_comanda.sql`.** `detalle_comanda.etiqueta_persona` (relleno con la etiqueta
actual de lo ya existente): la comanda es una foto, así que si una línea comandada cambia de persona, la comanda que ya
salió no cambia. `ComandarPedido` guarda la etiqueta al comandar y `ConsultaComandas` agrupa por ella.

**Backend (módulo `venta`).**

- `TrasladarLineas`: pasa líneas a otra persona del pedido o a una persona nueva ("Persona N+1"). **Todo o nada**: se
  valida y se recalcula todo antes de mover la primera. Acepta líneas pendientes y comandadas; rechaza las anuladas
  (`LINEA_ANULADA`), las de otro pedido o local (`LINEA_NO_ENCONTRADA`, sin decir cuál), el mismo destino y un pedido ya
  no en curso (`PEDIDO_NO_REORGANIZABLE`).
  - **Recálculo (CA5) con `CalculadorDePrecioDeLinea`, solo donde el precio depende de la persona:** el menú normal
    (público ↔ plan del pensionista, con "segundo del día" y táper) y la comida del plan (entre pensionistas con otro
    plan; a una persona normal da `SOLO_PARA_PENSIONISTA`). Carta, menú extra y extra libre conservan su precio. A un
    pensionista solo llegan menús y comidas del plan de una unidad (`CANTIDAD_INVALIDA`). Si el motor no puede cobrar
    (plan sin precio), no se mueve nada.
- `MoverPedidoDeMesa`: pedido de mesa en curso → mesa libre (activa, sin bloqueo, sin pedido, no unida); la vieja vuelve a
  libre sola porque el estado se deriva. No mueve un pedido para llevar ni deja atrás mesas unidas.
- `UnirMesas` / `SepararMesa`: una mesa **libre** (secundaria) se une a una **principal** activa, sin bloqueo y no unida; la
  principal puede o no tener pedido. `GET /mesas/{id}/pedido` de una unida devuelve el pedido de su principal. Una principal
  con mesas unidas no se bloquea ni se desactiva (`TIENE_MESAS_UNIDAS`).
- API (todo `venta.modificar`, que el mozo ya tiene): `POST /pedidos/{id}/lineas/trasladar {lineaIds, personaDestinoId?}`,
  `PUT /pedidos/{id}/mesa {mesaId}`, `POST /mesas/{id}/unir {principalId}`, `POST /mesas/{id}/separar`. Los dos primeros
  devuelven el pedido completo; los de mesa, la mesa del mapa.
- Bitácora (evento `transferencia` con un campo `tipo`): "pasó 1 productos de "Persona 1" a "Persona 2" en el pedido "0001"",
  "movió el pedido "0001" de la mesa 3 a la mesa 5", "unió "Mesa 4" a la mesa 3", "separó "Mesa 4" de la mesa 3".

**Frontend (`features/venta`, solo desde la pantalla del pedido; el mapa de mesas es del otro chat de diseño).** Casilla para
marcar cada producto (con `venta.modificar`), barra "N marcados · Pasar a… · Quitar marcas", `DialogoPasarA` (personas del
pedido y "Persona nueva · dividir la cuenta", con el aviso de recálculo), botones "Mover a otra mesa" (`DialogoMoverMesa`:
lista de mesas libres, título actualizado) y "Unir mesa" (`DialogoUnirMesa`: libres para unir y "Unidas a esta mesa" con
Separar). Cada diálogo con sus estados (cargando, vacío "No hay mesas libres", error) y errores en español.

## Qué se dejó fuera y por qué

- Cobrar por persona y cerrar (sprint 7); al cerrar el pedido de una principal habrá que separar las mesas unidas
  (anotado para ese sprint).
- Unir, separar y mover **desde el mapa** (lo decide el rediseño del otro chat).
- El botón "Dividir la cuenta" como atajo aparte: "Pasar a… → Persona nueva" ya lo cubre.
- Recalcular el "segundo del día" de las líneas que se quedan: si un pensionista tenía dos menús y se pasa el primero a otra
  persona, el segundo conserva su marca (anotado en pendientes).

## Cómo se verificó

**Backend:** 41 pruebas nuevas en verde: `TrasladarLineasTest` (11), `RecalculoAlTrasladarTest` (8), `MoverPedidoDeMesaTest` (8),
`UnirMesasTest` (11), `ReorganizarAccesoPorRolTest` (4: mozo, cajero y administradores; cocina y motorizado 403; 404 entre
locales; códigos), una en `ListarAuditoriaTest` y una en `ConsultaDeComandasTest`. Suite completa: 549 pruebas, solo falla el
ciclo conocido `comun ↔ identidad` (se ajustaron las pruebas de concurrencia de pedidos a la firma nueva de `AbrirPedido`).

**Navegador (POS como `mozotest`, pedido de prueba con "Prueba E4", persona 2 pensionista):**

- Menú normal de la persona 1 → "Pasar a… → Luis Alberto Quispe Rojas": baja de S/ 12.00 a **S/ 9.00 "precio de su plan"**
  (total 19 → 16, el recálculo legítimo) y la pantalla salta al destino; devuelto a la persona 1 vuelve a S/ 12.00.
- Gaseosa ×2 → "Persona nueva": aparece "Persona 3" con S/ 7.00 y el total no cambia (19.00).
- Comandar, pasar la gaseosa (ya "En cocina") a la persona 1 y reabrir la comanda: **sigue diciendo "Persona 3"**.
- "Mover a otra mesa": Mesa 1 → Mesa 3 (título actualizado, la 1 queda libre). "Unir mesa": Mesa 1 unida (el mapa la marca
  `unida`); "Separar" la deja libre.
- Móvil (375): sin desborde, casillas y botones de 44 px.

## Decisiones tomadas al ejecutar (rulings)

- Un solo caso de uso para trasladar y dividir; sin botón "Dividir la cuenta" aparte.
- La persona que queda sin líneas se conserva (el mozo la quita con "Quitar esta persona").
- Los códigos nuevos se sumaron a `ErrorPedido` y `ErrorMesa` (un solo controlador por recurso).
- Las pruebas que tocan la base por JDBC hacen `flush` antes y `flush + clear` después (la caché de JPA no ve un `UPDATE`).
- Al empezar esta entrega los datos de prueba del POS se siguen creando con pedidos propios y se borran solo esos.

## Pase de arreglos tras la revisión independiente (2026-09-30)

Un revisor independiente (solo lectura, sobre las entregas 3 y 4) encontró **3 Important y 6 Minor**. Cada arreglo llevó su
prueba escrita primero:

1. **Important — actualizaciones perdidas en `detalle_pedido`.** La línea no tenía versión y se reescribía entera: anular
   contra trasladar, o comandar contra modificar, se pisaban en silencio (una línea anulada volvía a sumar; una comandada
   volvía a "pendiente" y trababa todos los comandar). Migración **V37** (`detalle_pedido.version`) y `@Version` en la
   entidad: la segunda escritura choca y el controlador contesta 409 `PEDIDO_MODIFICADO`. `VersionDeLineasTest` (2).
2. **Important — dos menús pasados juntos a un pensionista no marcaban el "segundo del día".** Cada menú se calculaba contra
   la base antes de mover ninguno. `CalculadorDePrecioDeLinea` acepta ahora `consumosPrevios` y `TrasladarLineas` los
   cuenta dentro de la misma operación.
3. **Important — el menú se recalculaba aunque el tipo de cliente no cambiara** (incumplía "dividir conserva detalle y
   precio": si el dueño subía el menú, una línea vieja pasaba al precio nuevo, y sin precio de menú un simple dividir se
   rechazaba). Ahora solo se recalcula si el origen o el destino es un pensionista.
4. **Minor arreglados:** no se puede desactivar una mesa que está unida a otra; un `null` en la lista de líneas da
   `LINEA_NO_ENCONTRADA` y no un 500; la bitácora del traslado guarda cuánto cambió el total por el recalculo ("…
   (el total bajó S/ 4.00)").
5. **Minor que quedan anotados** (`docs/pendientes/README.md`): carreras entre mover, unir y bloquear sobre la misma mesa (el
   estado de la mesa no se serializa), y dos unir cruzados A→B y B→A podrían formar un ciclo; `AnularLinea` no revisa el
   estado del pedido (se decide en el sprint 7 junto con el cierre); la hoja de cocina agrupa por el texto de la etiqueta
   (dos personas con el mismo nombre salen juntas).

Áreas que la revisión no halló problemas: matchers y permisos de `ConfiguracionSeguridad`, aislamiento entre locales,
reutilización de ids, contador atómico de impresiones y unicidad de comandas, migraciones V33, V35 y V36.
