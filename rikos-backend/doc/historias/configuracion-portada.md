# Portada de Configuración (2026-09-29)

**Qué se pidió.** Una portada de "Configuración del sistema" con un ajuste por tarjeta (diseño
en el canvas: "Configuración del sistema (hub)"), con Locales dentro y los ajustes que todavía
no existen marcados "Próximamente".

**Qué se construyó (solo frontend).**
- `features/configuracion/PantallaConfiguracion.tsx` + `estilos/configuracion.css`; ruta
  `/admin/configuracion`.
- Activas: **Locales** (`/admin/locales`), **Parámetros de cobro**
  (`/admin/configuracion/parametros-de-cobro`) y **Operación**, que abre Motivos
  (`/admin/configuracion/motivos`). Las demás (Mi restaurante, Medios de pago, Salones y mesas,
  Producción, Impresoras, Delivery, Cajas registradoras) son "Próximamente" y no se pueden abrir.
- Locales y Operación muestran un dato en vivo (locales activos / motivos activos); si la
  consulta falla, la tarjeta queda sin él.
- **Sidebar:** "Configuración" pasó de acordeón con 4 entradas (Motivos, Parámetros de cobro,
  Medios de pago y Datos de la empresa, estas dos sin pantalla) a un solo enlace a la portada.
  "Locales" **se quitó del sidebar**: se abre desde su tarjeta en la portada, y mientras se está en
  `/admin/locales` el ítem "Configuración" queda marcado como activo (`rutasRelacionadas` en
  `BarraLateral.tsx`). Equivalencias: Motivos → tarjeta *Operación*; Datos de
  la empresa → tarjeta *Mi restaurante*.

**Verificado en vivo** (admin): las 10 tarjetas, enlaces correctos, datos en vivo ("3 locales ·
3 activos", "3 motivos activos"), ítem del sidebar activo (también estando en Locales; el sidebar ya no lista Locales). `npx tsc -b --force` sin errores.
