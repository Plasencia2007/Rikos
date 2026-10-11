# Salones y mesas (2026-09-30)

## Qué se pidió
Copiar la idea de otro sistema: organizar las mesas por **salones** (zonas del local), con lista de salones,
detalle con sus mesas, formulario de salón, "Generar mesa" y un **editor de plano visual** (arrastrar mesas;
clic abre propiedades; "Mover mesas" desbloquea el arrastre; Guardar plano / Descartar).
"Administrar mesas" vive solo en Configuración (tarjeta "Salones y mesas"), no en la barra lateral.
Diseño aprobado antes en el canvas (tableros Salones, MesaFormulario, PlanoEditor, PlanoMesaPropiedades).

## Qué se construyó
- **V34** `salon` (por local, nombre único por local, icono, color, medidas y color de piso del plano) y
  columnas nuevas en `mesa`: `salon_id` (nullable), `forma`, `vip`, `zona_exterior`, `zona_fumadores`,
  `pos_x`, `pos_y`, `ancho`, `alto`. Backfill: un salón "Principal" por local con mesas, posiciones en cuadrícula.
- Casos de uso (`venta/aplicacion`): CrearSalon, ActualizarSalon, CambiarEstadoSalon, EliminarSalon (solo vacío),
  ConsultaSalones, GuardarPlano (todo o nada), SalonPorDefecto. CrearMesa/ActualizarMesa aceptan salón, forma,
  atributos y medidas.
- API: `/salones` (GET venta.ver; resto venta.administrar), `PUT /salones/{id}/plano`; `MesaRequest`/`MesaRespuesta` ampliados.
- Frontend `features/salones/`: lista, detalle, formulario de salón, formulario de mesa, plano; `/admin/configuracion/mesas`
  redirige a salones.
- Auditoría: los cambios de salón reutilizan el evento `cambio_mesa` con entidad `salon` (el CHECK no admite evento nuevo).

## Decisiones
- `salon_id` nullable por seguridad con inserts antiguos; CrearMesa siempre asigna salón (por defecto "Principal").
- Las mesas no se borran (tienen historial de pedidos): se **desactivan/reactivan**; en el diseño había papelera, se reemplazó.
- `MesaRequest` usa `Boolean` (no `boolean`): un `null` explícito en el JSON tumbaba con 500 los tests HTTP existentes.
- Nota: `PUT /mesas/{id}` sin vip/zonas los deja en falso.

## Verificación
- `GestionDeSalonesTest` (14), `GestionDeMesasTest`, `MapaDeMesasTest`, `BloqueoDeMesasTest`, `MesasAccesoPorRolTest`,
  `MesasConcurrenciaTest` en verde. `npx tsc -b --force` limpio.
- En el navegador: crear salón, generar mesa, VIP, arrastrar y guardar plano (persistió posiciones).
- Bug hallado: el 500 por booleanos primitivos en `MesaRequest` (corregido).

## Fuera de alcance / pendiente
- El mapa de mesas del POS todavía no agrupa por salón.
- Datos de prueba: salón "Terraza" con mesas T1/T2 en San Isidro (se pueden desactivar, no borrar).
- Coordinación: la otra sesión (sprint 6) usa `Mesa` y `DatosMesa`; V34 podría chocar si ella agrega una V34.

## Mapa operativo por salón (2026-09-30)
El Mapa de mesas del sidebar y del POS (`features/venta/MapaDeMesas.tsx`, estilos en `estilos/mapa.css`) pasó de la rejilla plana
al diseño aprobado (tableros MapaMesas y MapaMesasAbrir): pestañas por salón, plano de solo lectura a escala con las posiciones
del editor, color por estado, leyenda y cifras (ocupadas con porcentaje, libres).
- Estados: libre (verde), ocupada (rojo), "Por cobrar" (`pendiente_cierre`, ámbar), bloqueada, unida; VIP como etiqueta.
- Mesas sin posición se acomodan en cuadrícula; mesas sin salón caen en la pestaña "Sin salón".
- En el POS, tocar una mesa libre abre el diálogo "Abrir mesa" (comensales 1–30 y cliente opcional, enviados como `comensales`
  y `nombreCliente` a `POST /pedidos`); "Bloquear mesa" sigue disponible con permiso. Las ocupadas abren su pedido.
- El "42 min" usa `abiertaEn` de `GET /mesas` (lo agrega la sesión de pedidos; el front lo trata como opcional).
- Dejado fuera: "personas" en las cifras (el mapa no trae comensales) y "Fusionar mesas" (US-029).
- Verificado en navegador (admin y POS, sin abrir pedidos); `npx tsc -b --force` limpio. Sin pruebas automáticas de front.
