# Sprint 6, entrega 2B — La pantalla del pedido en el POS (US-028) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Que el mozo, desde el POS, abra un pedido en una mesa libre (o para llevar), lo reparta entre personas, asigne pensionistas y agregue líneas (menú del día, carta, comida del plan, extra) viendo el precio y el total, usando la API de la entrega 2A.

**Architecture:** Tres piezas en `features/venta`: la **API** tipada a mano en `api/cliente.ts` (mismo patrón del resto), la pantalla `PantallaPedido` con sus diálogos, y dos cambios en el mapa de mesas (abrir o ver el pedido) y en `apps/pos` (qué vista se muestra). Toda escritura de la API devuelve el **pedido completo**, así que la pantalla no calcula nada: pinta lo que llega. El mozo nunca escribe un precio salvo el "extra".

**Tech Stack:** React 19 + TypeScript + Vite; componentes de `comun/componentes`; sin librería de estado (`useState`). Verificación: `npx tsc -b --force`, `npm run lint` y el navegador (el proyecto no tiene pruebas de componentes; ver "Sobre las pruebas").

**Spec:** `docs/producto/plan/sprint-06-vender-en-mesa.md` (3.3, 3.4, 3.5, 3.11 y 5). API: `rikos-backend/doc/historias/US-028-tomar-el-pedido-backend.md`.

## Sobre las pruebas

El frontend no tiene Vitest ni pruebas de componentes (ver `package.json`). El estándar de las entregas anteriores es: **el compilador falla primero** (se escribe el uso antes que la pieza), luego `tsc` y `lint` limpios, y luego se **verifica en el navegador** con capturas y comprobaciones por JavaScript. Este plan sigue ese estándar. No se agrega un framework de pruebas aquí (fuera de alcance).

## Global Constraints

- **Texto en español**, con los cuatro estados de cada pantalla (cargando, vacío, error, con datos).
- **Dinero en céntimos** en la API; se muestra con `enSoles` y se lee con `aCentimos`.
- **Ninguna pantalla calcula un precio ni un total:** pinta lo que devuelve la API.
- **Táctil primero:** botones grandes (mínimo 44 px de alto), sin depender de hover, operable en celular, tablet y escritorio.
- **`npx tsc -b --force`, no `tsc --noEmit`.** `npm run lint` no debe marcar nada de lo nuevo (el error ajeno de `features/locales/HorarioSemanal.tsx` no cuenta).
- **Fronteras entre features** (`eslint-plugin-boundaries`): una feature solo importa de otra por su `index.ts`. `features/venta` **no** importa de `features/pensionistas`: el dinero se mueve a `comun` (tarea 2).
- **Nunca commitear** salvo que el usuario lo pida (los pasos "Commit" **no se ejecutan** sin ese pedido).
- **El backend (8081), el gateway (8080) y Vite (5173) deben estar arriba**; si Docker se cayó, abrirlo y esperar `healthy` antes de asumir un bug.
- **Datos de prueba:** el PIN del mozo de prueba se asigna con un valor aleatorio desde el navegador y **no se escribe en la conversación**. No se toca el PIN de `jhon`.

## Review Focus

- **Un pedido de otra mesa o de otro mozo no se mezcla:** al abrir una mesa ocupada se ve *su* pedido; al volver al mapa y abrir otra, no queda nada de la anterior.
- **Doble toque:** tocar dos veces "Agregar" no duplica la línea (el botón se deshabilita mientras la petición está en curso).
- **Errores de la API en español**, sin códigos crudos: producto agotado, sin precio, menú no programado, mesa ocupada por otro mozo, pensionista inactivo o ya en el pedido.
- **El pedido se actualiza solo desde la respuesta**; si otra persona lo cambia a la vez (409), se avisa y se vuelve a cargar.
- **Menú con varias opciones:** no se puede agregar sin elegir; con una sola se muestra "incluida".
- **Pensionista:** al asignarlo, la persona cambia de etiqueta y el precio del menú pasa a ser el de su plan; una persona con líneas no se convierte en pensionista.
- **Sin menú confirmado o sin carta:** la pantalla lo dice ("hoy no hay menú programado"), no queda en blanco.
- **Celular y tablet:** nada se sale de la pantalla y todo se puede tocar.

---

## File Structure

**Frontend (`rikos-web/src`)**
- `comun/formato/dinero.ts` — `enSoles`, `aCentimos`, `desdeCentimos` (movidos desde `features/pensionistas/dinero.ts`, que los reexporta).
- `api/cliente.ts` — tipos y funciones de pedidos y la búsqueda de pensionistas.
- `features/venta/mensajes.ts` — códigos de error de pedidos en español.
- `features/venta/PantallaPedido.tsx` — la pantalla.
- `features/venta/DialogoMenu.tsx`, `DialogoCarta.tsx`, `DialogoExtra.tsx`, `DialogoPensionista.tsx`, `DialogoNota.tsx` — los diálogos.
- `features/venta/MapaDeMesas.tsx` — abrir o ver el pedido, y "Pedido para llevar".
- `features/venta/index.ts` — exporta `PantallaPedido`.
- `apps/pos/index.tsx` — la vista actual (mapa o pedido).
- `estilos/global.css` — clases `ped-*`.

---

### Task 1: Datos de ejemplo para probar

**Files:** ninguno del repositorio (script de un solo uso en el scratchpad de la sesión). Se documenta en la historia de la entrega.

**Interfaces:**
- Produces: para **hoy** (miércoles 2026-09-30, semana `2026-09-28` ya confirmada): un menú con 2 entradas, 3 segundos normales, 1 segundo extra, 1 refresco y 1 postre; el precio del menú extra; y 5 productos de carta con precio.

- [ ] **Step 1: Check the current state**

Run (con el gateway en el 8080; token de `admin`/`cambiar123`):
```bash
H='Content-Type: application/json'; B=localhost:8080/api
TOK=$(curl -s -X POST $B/auth/login -H "$H" -d '{"nombreUsuario":"admin","contrasena":"cambiar123"}' | sed 's/.*"token":"\([^"]*\)".*/\1/'); A="Authorization: Bearer $TOK"
curl -s "$B/menu/dias/2026-09-30" -H "$A" | head -c 400; echo
curl -s $B/menu/semanas -H "$A" | head -c 400; echo
```
Expected: la semana `2026-09-28` en estado `confirmado` y el día 30 sin opciones (hoy la oferta dice `hayMenu: false`). Anotar el `id` de la semana.

- [ ] **Step 2: Program today's menu**

Con los ids de producto que devuelve `docker exec rikos-postgres psql -U rikos -d rikos_operacion -c "select id, nombre from producto where es_vendible and activo and tipo_menu is null"`:
```bash
SEM=<id de la semana 2026-09-28>
op() { curl -s -X POST $B/menu/semanas/$SEM/opciones -H "$A" -H "$H" -d "{\"fecha\":\"2026-09-30\",\"productoId\":\"$1\",\"tipoMenu\":\"$2\",\"porcionesEstimadas\":null}" -o /dev/null -w "%{http_code} $3\n"; }
op <id Sopa criolla>         normal "entrada"
op <id Papa a la huancaína>  normal "entrada"
op <id Seco de res>          normal "segundo"
op <id Lomo saltado>         normal "segundo"
op <id Ají de gallina>       normal "segundo"
op <id Arroz con pollo>      extra  "segundo extra"
op <id Chicha morada>        normal "refresco"
op <id Mazamorra morada>     normal "postre"
curl -s -X PUT $B/menu/extra/precio -H "$A" -H "$H" -d '{"precioVenta":1800}'
```
Expected: `200` (o `201`) en cada una. **Si el servidor rechaza agregar opciones a una semana ya confirmada** (regla de la programación semanal), anotarlo como `Ruling:` y usar la pantalla de Programación semanal del panel para reabrir el borrador, o crear la semana siguiente y probar con la fecha que corresponda.

- [ ] **Step 3: Load the carta**

```bash
UND=01M39A0KG00000000000000005
prod() { curl -s -X POST $B/productos -H "$A" -H "$H" -d "{\"codigo\":\"$1\",\"nombre\":\"$2\",\"categoriaId\":\"$3\",\"unidadBaseId\":\"$UND\",\"esVendible\":true,\"esInsumo\":false,\"esSubreceta\":false,\"precioVenta\":$4,\"controlaStock\":false}" -o /dev/null -w "%{http_code} $2\n"; }
prod CARTA-01 "Gaseosa personal"    01M39A0RZ0000000000000S018 350
prod CARTA-02 "Jugo de naranja"     01M39A0RZ0000000000000S018 500
prod CARTA-03 "Café pasado"         01M39A0RZ0000000000000S017 300
prod CARTA-04 "Sándwich de pollo"   01M39A0RZ0000000000000S016 900
prod CARTA-05 "Huevo frito adicional" 01M39A0RZ0000000000000S013 300
```
Expected: `201` en cada una (si un código ya existe, usar otro).

- [ ] **Step 3b: Verify**

Run: `curl -s "$B/menu/dias/2026-09-30" -H "$A" | head -c 300` y luego abrir un pedido para llevar de prueba y pedir su oferta:
```bash
P=$(curl -s -X POST $B/pedidos -H "$A" -H "$H" -d '{"canal":"para_llevar","servicio":"almuerzo"}'); PID=$(echo "$P" | grep -o '"id":"[^"]*"' | head -1 | sed 's/"id":"\([^"]*\)"/\1/')
curl -s $B/pedidos/$PID/oferta -H "$A" | head -c 600
```
Expected: `"hayMenu":true`, `precioMenuNormal":1200`, `"precioMenuExtra":1800`, componentes `entrada`(2 opciones), `segundo`(3), `refresco`(1), `postre`(1), y la carta con los 5 productos. **Borrar el pedido de prueba** con las sentencias de limpieza de la historia US-028 (o dejarlo si se va a usar).

- [ ] **Step 4: Commit** (no aplica: no hay archivos del repositorio; se documentan los datos en la historia)

---

### Task 2: El dinero en `comun`, la API tipada y los mensajes

**Files:**
- Create: `rikos-web/src/comun/formato/dinero.ts`, `rikos-web/src/features/venta/mensajes.ts`
- Modify: `rikos-web/src/features/pensionistas/dinero.ts` (reexporta), `rikos-web/src/api/cliente.ts`

**Interfaces:**
- Produces: `comun/formato/dinero.ts` con `enSoles(centimos)`, `aCentimos(texto)`, `desdeCentimos(centimos)` (mismo contenido de hoy).
- Produces (`api/cliente.ts`): los tipos `TipoDeLinea`, `LineaDelPedido`, `PersonaDelPedido`, `Pedido`, `OfertaDelDia`, `ComponenteOfrecido`, `OpcionOfrecida`, `ProductoOfrecido`, `DatosDeLinea`, y las funciones `abrirPedido`, `obtenerPedido`, `obtenerPedidoDeMesa`, `ofertaDelPedido`, `agregarPersonaAlPedido`, `asignarPensionistaAPersona`, `quitarPersonaDelPedido`, `agregarLineaAlPedido`, `modificarLineaDelPedido`, `quitarLineaDelPedido`, `buscarPensionistas`.
- Produces: `mensajeDePedido(error: unknown, porDefecto: string): string`.

- [ ] **Step 1: Write the failing check**

En `features/venta/index.ts` no cambia nada todavía. El chequeo que falla: en `features/venta/MapaDeMesas.tsx` (más adelante) se importan `abrirPedido` y `obtenerPedidoDeMesa`; **aquí** el chequeo es que `features/pensionistas/dinero.ts` reexporte desde `comun`. Escribir primero, en `features/venta/mensajes.ts`, la línea `import { ErrorApi } from '../../api/cliente'` y `export { enSoles } from '../../comun/formato/dinero'` y correr el compilador.

- [ ] **Step 2: Run check to verify it fails**

Run: `cd rikos-web && npx tsc -b --force`
Expected: FAIL, `Cannot find module '../../comun/formato/dinero'`.

- [ ] **Step 3: Write minimal implementation**

`comun/formato/dinero.ts` — **mover** el contenido de `features/pensionistas/dinero.ts` (las tres funciones) tal cual. Y dejar `features/pensionistas/dinero.ts` así:
```ts
/** Los precios viajan en centimos; ahora viven en comun/formato para que otras features los usen sin importar de pensionistas. */
export { enSoles, aCentimos, desdeCentimos } from '../../comun/formato/dinero'
```

`features/venta/mensajes.ts`:
```ts
import { ErrorApi } from '../../api/cliente'

/** Los codigos de error de pedidos (US-028) en el idioma del mozo. */
const MENSAJES: Record<string, string> = {
  NO_ENCONTRADO: 'Ese pedido ya no existe en este local.',
  SIN_LOCAL_ACTIVO: 'Elige primero el local en el que estás trabajando.',
  CANAL_INVALIDO: 'Ese tipo de pedido no existe.',
  MESA_REQUERIDA: 'Elige una mesa.',
  MESA_NO_ENCONTRADA: 'Esa mesa ya no existe en este local.',
  MESA_NO_DISPONIBLE: 'Esa mesa está bloqueada, unida a otra o desactivada.',
  MESA_OCUPADA: 'Esa mesa ya tiene un pedido abierto.',
  SERVICIO_REQUERIDO: 'Estamos fuera de horario: elige el servicio (desayuno, almuerzo o cena).',
  SERVICIO_INVALIDO: 'Ese servicio no existe.',
  PERSONA_NO_ENCONTRADA: 'Esa persona ya no está en el pedido.',
  PENSIONISTA_NO_ENCONTRADO: 'No se encontró a ese pensionista.',
  PENSIONISTA_INACTIVO: 'Ese pensionista está desactivado: no puede consumir.',
  PENSIONISTA_YA_EN_EL_PEDIDO: 'Ese pensionista ya está en este pedido.',
  PERSONA_CON_LINEAS: 'Esa persona ya tiene productos: no se puede cambiar ni quitar.',
  PERSONA_NO_ES_PENSIONISTA: 'Solo un pensionista puede pedir la comida de su plan.',
  PRODUCTO_NO_ENCONTRADO: 'Ese producto ya no existe.',
  PRODUCTO_NO_VENDIBLE: 'Ese producto no se vende.',
  PRODUCTO_AGOTADO: 'Ese producto está agotado en este local.',
  SIN_PRECIO: 'Ese producto todavía no tiene precio: avísale al administrador.',
  PLAN_SIN_PRECIO: 'El plan de este pensionista no tiene precio para esa comida: avísale al administrador.',
  MENU_NO_PROGRAMADO: 'Hoy no hay menú programado.',
  MENU_FUERA_DE_SERVICIO: 'El menú del día solo se pide en el almuerzo.',
  COMPONENTE_OBLIGATORIO: 'Elige una opción para cada parte del menú.',
  OPCION_INVALIDA: 'Esa opción no es del menú de hoy.',
  PLAN_FUERA_DE_SERVICIO: 'Esa comida del plan no corresponde a este servicio.',
  CANTIDAD_INVALIDA: 'La cantidad no es válida (un pensionista pide una comida del plan por línea).',
  DESCRIPCION_OBLIGATORIA: 'Escribe qué es el extra.',
  PRECIO_INVALIDO: 'Escribe el precio del extra (mayor que cero).',
  OBSERVACION_LARGA: 'La nota es muy larga (máximo 250 caracteres).',
  LINEA_NO_ENCONTRADA: 'Ese producto ya no está en el pedido.',
  LINEA_YA_COMANDADA: 'Ese producto ya salió a cocina: no se puede cambiar aquí.',
  TIPO_INVALIDO: 'Ese tipo de producto no existe.',
  PEDIDO_MODIFICADO: 'Otra persona cambió este pedido al mismo tiempo. Se actualizó: revisa y vuelve a intentarlo.',
  NUMERO_DE_PEDIDO_REPETIDO: 'Hubo un choque al numerar el pedido: vuelve a intentarlo.',
}

export function mensajeDePedido(error: unknown, porDefecto: string): string {
  if (error instanceof ErrorApi) {
    const codigo = error.cuerpo?.error
    if (codigo && MENSAJES[codigo]) return MENSAJES[codigo]
    if (error.estado === 403) return 'No tienes permiso para hacer esto.'
  }
  return porDefecto
}
```

`api/cliente.ts` — agregar después de la sección de mesas (antes de `// ---- Disponibilidad por local (US-015)`), con el mismo estilo del archivo:
```ts
// ---- Pedidos (US-028) ----------------------------------------------------------

export type TipoDeLinea = 'menu_normal' | 'menu_extra' | 'plan_desayuno' | 'plan_cena' | 'producto' | 'extra_libre'
export type ServicioDelPedido = 'desayuno' | 'almuerzo' | 'cena'

export interface ComponenteDelPedido {
  componente: string
  productoNombre: string | null
}

/** Todo en centimos. Una linea anulada no suma. */
export interface LineaDelPedido {
  id: string
  tipoLinea: TipoDeLinea
  productoId: string | null
  productoNombre: string | null
  descripcion: string | null
  cantidad: number
  observacion: string | null
  estado: 'pendiente' | 'comandado' | 'anulado'
  precioUnitario: number
  taperUnitario: number
  tarifaOrigen: 'publico' | 'plan' | 'extra' | 'manual'
  segundoDelDia: boolean
  componentes: ComponenteDelPedido[]
}

export interface PersonaDelPedido {
  id: string
  etiqueta: string
  tipoCliente: 'regular' | 'pensionista'
  pensionistaId: string | null
  lineas: LineaDelPedido[]
  subtotal: number
  taper: number
  total: number
}

/** avisos: por ahora TAPER_SIN_MONTO (es para llevar y el duenio no ha puesto el monto del taper). */
export interface Pedido {
  id: string
  numero: string
  canal: 'mesa' | 'para_llevar'
  servicio: ServicioDelPedido
  mesaId: string | null
  estado: 'abierto' | 'comandado' | 'pendiente_cierre'
  personas: PersonaDelPedido[]
  subtotal: number
  taper: number
  total: number
  avisos: string[]
}

export interface OpcionOfrecida {
  opcionMenuId: string
  productoNombre: string
}

export interface ComponenteOfrecido {
  componente: string
  opciones: OpcionOfrecida[]
}

export interface ProductoOfrecido {
  id: string
  nombre: string
  precio: number
}

export interface OfertaDelDia {
  fecha: string
  servicio: ServicioDelPedido
  hayMenu: boolean
  precioMenuNormal: number | null
  precioMenuExtra: number | null
  normal: ComponenteOfrecido[]
  extra: ComponenteOfrecido[]
  carta: ProductoOfrecido[]
}

export interface DatosDeLinea {
  personaId: string
  tipo: TipoDeLinea
  cantidad: number
  productoId?: string
  observacion?: string | null
  componentes?: { componente: string; opcionMenuId: string }[]
  /** Solo en extra_libre. */
  descripcion?: string
  /** Solo en extra_libre, en centimos: el unico precio que escribe el mozo. */
  precio?: number
}

export const abrirPedido = (
  token: string,
  datos: { canal: 'mesa' | 'para_llevar'; mesaId?: string; servicio?: ServicioDelPedido },
) => pedirAutenticado<Pedido>('/api/pedidos', token, { method: 'POST', body: JSON.stringify(datos) })

export const obtenerPedido = (token: string, id: string) => pedirAutenticado<Pedido>(`/api/pedidos/${id}`, token)

/** 404 si la mesa no tiene un pedido en curso. */
export const obtenerPedidoDeMesa = (token: string, mesaId: string) =>
  pedirAutenticado<Pedido>(`/api/mesas/${mesaId}/pedido`, token)

export const ofertaDelPedido = (token: string, id: string) =>
  pedirAutenticado<OfertaDelDia>(`/api/pedidos/${id}/oferta`, token)

export const agregarPersonaAlPedido = (token: string, pedidoId: string, etiqueta?: string) =>
  pedirAutenticado<Pedido>(`/api/pedidos/${pedidoId}/personas`, token, {
    method: 'POST',
    body: JSON.stringify({ etiqueta: etiqueta ?? null }),
  })

export const asignarPensionistaAPersona = (token: string, pedidoId: string, personaId: string, pensionistaId: string) =>
  pedirAutenticado<Pedido>(`/api/pedidos/${pedidoId}/personas/${personaId}/pensionista`, token, {
    method: 'PUT',
    body: JSON.stringify({ pensionistaId }),
  })

export const quitarPersonaDelPedido = (token: string, pedidoId: string, personaId: string) =>
  pedirAutenticado<Pedido>(`/api/pedidos/${pedidoId}/personas/${personaId}`, token, { method: 'DELETE' })

export const agregarLineaAlPedido = (token: string, pedidoId: string, datos: DatosDeLinea) =>
  pedirAutenticado<Pedido>(`/api/pedidos/${pedidoId}/lineas`, token, { method: 'POST', body: JSON.stringify(datos) })

export const modificarLineaDelPedido = (
  token: string,
  pedidoId: string,
  lineaId: string,
  datos: { cantidad: number; observacion: string | null },
) =>
  pedirAutenticado<Pedido>(`/api/pedidos/${pedidoId}/lineas/${lineaId}`, token, {
    method: 'PUT',
    body: JSON.stringify(datos),
  })

export const quitarLineaDelPedido = (token: string, pedidoId: string, lineaId: string) =>
  pedirAutenticado<Pedido>(`/api/pedidos/${pedidoId}/lineas/${lineaId}`, token, { method: 'DELETE' })

/** Por nombre o DNI (el mozo lo necesita para asignar un pensionista; requiere pensionistas.ver, que el mozo tiene). */
export const buscarPensionistas = (token: string, q: string) =>
  pedirAutenticado<ResumenPensionista[]>(`/api/pensionistas?q=${encodeURIComponent(q)}`, token)
```

- [ ] **Step 4: Run check to verify it passes**

Run: `cd rikos-web && npx tsc -b --force && npm run lint 2>&1 | grep -iE "venta|dinero|cliente.ts"`
Expected: `tsc` sin salida y el lint sin líneas de lo nuevo. Comprobar que **no se rompió** lo de pensionistas: `npx tsc -b --force` cubre todos los imports de `./dinero`.

- [ ] **Step 5: Commit** (solo si el usuario lo pide)

---

### Task 3: Los diálogos

**Files:**
- Create: `rikos-web/src/features/venta/DialogoMenu.tsx`, `DialogoCarta.tsx`, `DialogoExtra.tsx`, `DialogoPensionista.tsx`, `DialogoNota.tsx`

**Interfaces:**
- Consumes: `Modal`, `Boton`, `Campo`, `Aviso`, `EstadoVacio` de `comun/componentes`; `enSoles`, `aCentimos` de `comun/formato/dinero`; los tipos de `api/cliente.ts`; `buscarPensionistas`.
- Produces (todos reciben `abierto`-implícito: se montan solo cuando se necesitan, como los diálogos de la cuenta):
  - `DialogoMenu({ tipo, oferta, esPensionista, trabajando, onAgregar, onCerrar })` con `onAgregar(componentes: {componente: string; opcionMenuId: string}[], observacion: string | null)`.
  - `DialogoCarta({ carta, trabajando, onElegir, onCerrar })` con `onElegir(producto: ProductoOfrecido)`.
  - `DialogoExtra({ trabajando, onAgregar, onCerrar })` con `onAgregar(descripcion: string, precio: number, cantidad: number)`.
  - `DialogoPensionista({ token, trabajando, onElegir, onCerrar })` con `onElegir(pensionista: ResumenPensionista)`.
  - `DialogoNota({ inicial, trabajando, onGuardar, onCerrar })` con `onGuardar(nota: string | null)`.

- [ ] **Step 1: Write the failing check**

Importar los cinco diálogos en `features/venta/PantallaPedido.tsx` (tarea 4) **antes** de crearlos, o, para ver el RED ahora, crear `features/venta/prueba-dialogos.ts` con `import './DialogoMenu'` … y correr `tsc`, borrándolo después.

- [ ] **Step 2: Run check to verify it fails**

Run: `cd rikos-web && npx tsc -b --force`
Expected: FAIL, los cinco módulos no existen.

- [ ] **Step 3: Write minimal implementation**

`DialogoMenu.tsx`:
```tsx
import { useState } from 'react'
import { Aviso, Boton, Campo, Modal } from '../../comun/componentes'
import { enSoles } from '../../comun/formato/dinero'
import type { OfertaDelDia } from '../../api/cliente'

const TITULO: Record<string, string> = {
  entrada: 'Entrada',
  segundo: 'Segundo',
  guarnicion: 'Guarnición',
  refresco: 'Refresco',
  postre: 'Postre',
}

/**
 * El menu del dia (o el extra): una parte por componente. Con una sola opcion se muestra "incluida"; con varias, el
 * mozo tiene que elegir una antes de agregar. El precio no se calcula aqui: se muestra el normal y, si es un
 * pensionista, se avisa que se cobra el de su plan (lo decide el servidor).
 */
export function DialogoMenu({
  tipo,
  oferta,
  esPensionista,
  trabajando,
  onAgregar,
  onCerrar,
}: {
  tipo: 'menu_normal' | 'menu_extra'
  oferta: OfertaDelDia
  esPensionista: boolean
  trabajando: boolean
  onAgregar: (componentes: { componente: string; opcionMenuId: string }[], observacion: string | null) => void
  onCerrar: () => void
}) {
  const componentes = tipo === 'menu_extra' ? oferta.extra : oferta.normal
  const precio = tipo === 'menu_extra' ? oferta.precioMenuExtra : oferta.precioMenuNormal
  const [elegidas, setElegidas] = useState<Record<string, string>>({})
  const [nota, setNota] = useState('')
  const [error, setError] = useState('')

  function agregar() {
    const faltan = componentes.filter((c) => c.opciones.length > 1 && !elegidas[c.componente])
    if (faltan.length > 0) {
      setError(`Elige una opción de: ${faltan.map((c) => TITULO[c.componente] ?? c.componente).join(', ')}.`)
      return
    }
    // Solo se mandan las elecciones que el mozo hizo; las de una sola opcion las asigna el servidor.
    onAgregar(
      Object.entries(elegidas).map(([componente, opcionMenuId]) => ({ componente, opcionMenuId })),
      nota.trim() || null,
    )
  }

  return (
    <Modal
      abierto
      titulo={tipo === 'menu_extra' ? 'Menú extra' : 'Menú del día'}
      onCerrar={onCerrar}
      pie={
        <>
          <Boton variante="secundario" onClick={onCerrar}>
            Cancelar
          </Boton>
          <Boton cargando={trabajando} onClick={agregar}>
            Agregar al pedido
          </Boton>
        </>
      }
    >
      <div className="ped-dialogo">
        <p className="ped-precio-menu">
          {precio == null ? 'Sin precio' : enSoles(precio)}
          {esPensionista && <span className="cta-nota"> · a este pensionista se le cobra el precio de su plan</span>}
        </p>
        {error && <Aviso tono="error">{error}</Aviso>}
        {componentes.map((c) => (
          <fieldset key={c.componente} className="ped-parte">
            <legend>{TITULO[c.componente] ?? c.componente}</legend>
            {c.opciones.length === 1 ? (
              <p className="ped-incluida">
                {c.opciones[0].productoNombre} <span className="cta-nota">(incluida)</span>
              </p>
            ) : (
              c.opciones.map((o) => (
                <label key={o.opcionMenuId} className={`ped-opcion ${elegidas[c.componente] === o.opcionMenuId ? 'ped-opcion-elegida' : ''}`}>
                  <input
                    type="radio"
                    name={c.componente}
                    checked={elegidas[c.componente] === o.opcionMenuId}
                    onChange={() => {
                      setError('')
                      setElegidas({ ...elegidas, [c.componente]: o.opcionMenuId })
                    }}
                  />
                  {o.productoNombre}
                </label>
              ))
            )}
          </fieldset>
        ))}
        <Campo etiqueta="Nota para cocina" maxLength={250} value={nota} onChange={(e) => setNota(e.target.value)} />
      </div>
    </Modal>
  )
}
```

`DialogoCarta.tsx`:
```tsx
import { useMemo, useState } from 'react'
import { Boton, Campo, EstadoVacio, Modal } from '../../comun/componentes'
import { enSoles } from '../../comun/formato/dinero'
import type { ProductoOfrecido } from '../../api/cliente'

/** La carta del local (sin los agotados): tocar un producto lo agrega con cantidad 1; la cantidad se cambia en la lista. */
export function DialogoCarta({
  carta,
  trabajando,
  onElegir,
  onCerrar,
}: {
  carta: ProductoOfrecido[]
  trabajando: boolean
  onElegir: (producto: ProductoOfrecido) => void
  onCerrar: () => void
}) {
  const [busqueda, setBusqueda] = useState('')
  const visibles = useMemo(() => {
    const q = busqueda.trim().toLowerCase()
    return q ? carta.filter((p) => p.nombre.toLowerCase().includes(q)) : carta
  }, [carta, busqueda])

  return (
    <Modal abierto titulo="Carta" onCerrar={onCerrar} pie={<Boton variante="secundario" onClick={onCerrar}>Cerrar</Boton>}>
      <div className="ped-dialogo">
        {carta.length === 0 ? (
          <EstadoVacio
            titulo="La carta está vacía"
            descripcion="No hay productos con precio disponibles en este local (o están todos agotados)."
          />
        ) : (
          <>
            <Campo etiqueta="Buscar" value={busqueda} onChange={(e) => setBusqueda(e.target.value)} />
            <ul className="ped-lista-carta">
              {visibles.map((p) => (
                <li key={p.id}>
                  <button type="button" className="ped-producto" disabled={trabajando} onClick={() => onElegir(p)}>
                    <span>{p.nombre}</span>
                    <strong>{enSoles(p.precio)}</strong>
                  </button>
                </li>
              ))}
              {visibles.length === 0 && <li className="prod-vacio">Nada coincide con "{busqueda}".</li>}
            </ul>
          </>
        )}
      </div>
    </Modal>
  )
}
```

`DialogoExtra.tsx`:
```tsx
import { useState } from 'react'
import { Aviso, Boton, Campo, Modal } from '../../comun/componentes'
import { aCentimos } from '../../comun/formato/dinero'

/**
 * El "extra": algo que no esta en la carta (un huevo de mas). Es el UNICO caso en que el mozo escribe un precio;
 * queda marcado como manual y en la bitacora.
 */
export function DialogoExtra({
  trabajando,
  onAgregar,
  onCerrar,
}: {
  trabajando: boolean
  onAgregar: (descripcion: string, precio: number, cantidad: number) => void
  onCerrar: () => void
}) {
  const [descripcion, setDescripcion] = useState('')
  const [precio, setPrecio] = useState('')
  const [cantidad, setCantidad] = useState('1')
  const [error, setError] = useState('')

  function agregar() {
    const centimos = aCentimos(precio)
    const unidades = Number(cantidad)
    if (descripcion.trim() === '') return setError('Escribe qué es el extra.')
    if (centimos == null || Number.isNaN(centimos)) return setError('Escribe el precio, por ejemplo 3 o 3.50.')
    if (!Number.isInteger(unidades) || unidades < 1) return setError('La cantidad tiene que ser un número entero desde 1.')
    onAgregar(descripcion.trim(), centimos, unidades)
  }

  return (
    <Modal
      abierto
      titulo="Extra"
      onCerrar={onCerrar}
      pie={
        <>
          <Boton variante="secundario" onClick={onCerrar}>
            Cancelar
          </Boton>
          <Boton cargando={trabajando} onClick={agregar}>
            Agregar al pedido
          </Boton>
        </>
      }
    >
      <div className="ped-dialogo">
        <p className="cta-nota">Para algo que no está en la carta. El precio que escribas queda registrado como puesto a mano.</p>
        {error && <Aviso tono="error">{error}</Aviso>}
        <Campo etiqueta="Qué es" maxLength={120} value={descripcion} onChange={(e) => setDescripcion(e.target.value)} />
        <Campo etiqueta="Precio (S/)" inputMode="decimal" value={precio} onChange={(e) => setPrecio(e.target.value)} />
        <Campo etiqueta="Cantidad" inputMode="numeric" value={cantidad} onChange={(e) => setCantidad(e.target.value)} />
      </div>
    </Modal>
  )
}
```

`DialogoPensionista.tsx`:
```tsx
import { useEffect, useState } from 'react'
import { Aviso, Boton, Campo, EstadoVacio, Modal } from '../../comun/componentes'
import { buscarPensionistas, type ResumenPensionista } from '../../api/cliente'

/**
 * El mozo busca al pensionista por nombre o DNI y lo asigna a la persona. Paso provisional hasta el ticket del
 * kiosco (sprint 8). Un pensionista desactivado se ve pero no se puede elegir.
 */
export function DialogoPensionista({
  token,
  trabajando,
  onElegir,
  onCerrar,
}: {
  token: string
  trabajando: boolean
  onElegir: (pensionista: ResumenPensionista) => void
  onCerrar: () => void
}) {
  const [q, setQ] = useState('')
  const [resultados, setResultados] = useState<ResumenPensionista[] | null>(null)
  const [error, setError] = useState('')

  useEffect(() => {
    const texto = q.trim()
    if (texto.length < 2) {
      setResultados(null)
      return
    }
    const espera = window.setTimeout(() => {
      setError('')
      buscarPensionistas(token, texto)
        .then(setResultados)
        .catch(() => setError('No se pudo buscar. Revisa la conexión e inténtalo otra vez.'))
    }, 300)
    return () => window.clearTimeout(espera)
  }, [q, token])

  return (
    <Modal abierto titulo="Asignar un pensionista" onCerrar={onCerrar} pie={<Boton variante="secundario" onClick={onCerrar}>Cerrar</Boton>}>
      <div className="ped-dialogo">
        <Campo etiqueta="Nombre o DNI" ayuda="Escribe al menos 2 letras o números." value={q} autoFocus onChange={(e) => setQ(e.target.value)} />
        {error && <Aviso tono="error">{error}</Aviso>}
        {resultados && resultados.length === 0 && (
          <EstadoVacio titulo="No se encontró a nadie" descripcion="Prueba con otro nombre o con el DNI." />
        )}
        <ul className="ped-lista-carta">
          {(resultados ?? []).map((p) => (
            <li key={p.id}>
              <button type="button" className="ped-producto" disabled={!p.activo || trabajando} onClick={() => onElegir(p)}>
                <span>
                  {p.apellidos}, {p.nombres} <span className="cta-nota">DNI {p.dni}</span>
                </span>
                <span className="cta-nota">{p.activo ? (p.planNombre ?? '') : 'Desactivado'}</span>
              </button>
            </li>
          ))}
        </ul>
      </div>
    </Modal>
  )
}
```

`DialogoNota.tsx`:
```tsx
import { useState } from 'react'
import { Boton, Campo, Modal } from '../../comun/componentes'

/** La nota para cocina de una linea pendiente ("sin cebolla"). Vacia = sin nota. */
export function DialogoNota({
  inicial,
  trabajando,
  onGuardar,
  onCerrar,
}: {
  inicial: string | null
  trabajando: boolean
  onGuardar: (nota: string | null) => void
  onCerrar: () => void
}) {
  const [nota, setNota] = useState(inicial ?? '')
  return (
    <Modal
      abierto
      titulo="Nota para cocina"
      onCerrar={onCerrar}
      pie={
        <>
          <Boton variante="secundario" onClick={onCerrar}>
            Cancelar
          </Boton>
          <Boton cargando={trabajando} onClick={() => onGuardar(nota.trim() || null)}>
            Guardar
          </Boton>
        </>
      }
    >
      <div className="ped-dialogo">
        <Campo etiqueta="Nota" maxLength={250} value={nota} autoFocus onChange={(e) => setNota(e.target.value)} />
      </div>
    </Modal>
  )
}
```

- [ ] **Step 4: Run check to verify it passes**

Run: `cd rikos-web && npx tsc -b --force && npm run lint 2>&1 | grep -i "venta"`
Expected: `tsc` limpio y sin líneas de lint de `features/venta`. Si `Campo` no acepta `autoFocus` (es un `input`, hereda `InputHTMLAttributes`), debe compilar.

- [ ] **Step 5: Commit** (solo si el usuario lo pide)

---

### Task 4: La pantalla del pedido

**Files:**
- Create: `rikos-web/src/features/venta/PantallaPedido.tsx`
- Modify: `rikos-web/src/features/venta/index.ts`, `rikos-web/src/estilos/global.css`

**Interfaces:**
- Consumes: la API de la tarea 2, `mensajeDePedido`, los diálogos de la tarea 3, `enSoles`.
- Produces: `PantallaPedido({ token, pedidoId, titulo, onVolver }: { token: string; pedidoId: string; titulo: string; onVolver: () => void })`.

- [ ] **Step 1: Write the failing check**

En `features/venta/index.ts` agregar `export { PantallaPedido } from './PantallaPedido'` **antes** de crear el archivo.

- [ ] **Step 2: Run check to verify it fails**

Run: `cd rikos-web && npx tsc -b --force`
Expected: FAIL, `Cannot find module './PantallaPedido'`.

- [ ] **Step 3: Write minimal implementation**

`features/venta/PantallaPedido.tsx`:
```tsx
import { useCallback, useEffect, useState } from 'react'
import { Aviso, Boton, EstadoCargando, EstadoError, Insignia } from '../../comun/componentes'
import { enSoles } from '../../comun/formato/dinero'
import {
  agregarLineaAlPedido,
  agregarPersonaAlPedido,
  asignarPensionistaAPersona,
  modificarLineaDelPedido,
  obtenerPedido,
  ofertaDelPedido,
  quitarLineaDelPedido,
  quitarPersonaDelPedido,
  type DatosDeLinea,
  type LineaDelPedido,
  type OfertaDelDia,
  type Pedido,
  type PersonaDelPedido,
  type ProductoOfrecido,
  type ResumenPensionista,
} from '../../api/cliente'
import { mensajeDePedido } from './mensajes'
import { DialogoCarta } from './DialogoCarta'
import { DialogoExtra } from './DialogoExtra'
import { DialogoMenu } from './DialogoMenu'
import { DialogoNota } from './DialogoNota'
import { DialogoPensionista } from './DialogoPensionista'

/**
 * US-028: el pedido de una mesa (o para llevar). Cada escritura de la API devuelve el pedido completo con sus
 * totales, asi que esta pantalla NO calcula nada: pinta lo que llega. El mozo agrega productos a una persona;
 * el precio lo decide el servidor (el mozo solo escribe el precio de un "extra"). Comandar llega en la entrega 3.
 */

const SERVICIO: Record<string, string> = { desayuno: 'Desayuno', almuerzo: 'Almuerzo', cena: 'Cena' }

type Dialogo =
  | { tipo: 'menu'; cual: 'menu_normal' | 'menu_extra' }
  | { tipo: 'carta' }
  | { tipo: 'extra' }
  | { tipo: 'pensionista' }
  | { tipo: 'nota'; linea: LineaDelPedido }
  | null

function nombreDeLinea(l: LineaDelPedido): string {
  return l.productoNombre ?? l.descripcion ?? 'Producto'
}

/** Una comida del plan o un menu de un pensionista se pide de a una por linea. */
function cantidadFija(l: LineaDelPedido, persona: PersonaDelPedido): boolean {
  return persona.tipoCliente === 'pensionista' && ['menu_normal', 'plan_desayuno', 'plan_cena'].includes(l.tipoLinea)
}

export function PantallaPedido({
  token,
  pedidoId,
  titulo,
  onVolver,
}: {
  token: string
  pedidoId: string
  titulo: string
  onVolver: () => void
}) {
  const [pedido, setPedido] = useState<Pedido | null>(null)
  const [oferta, setOferta] = useState<OfertaDelDia | null>(null)
  const [personaId, setPersonaId] = useState<string | null>(null)
  const [dialogo, setDialogo] = useState<Dialogo>(null)
  const [errorDeCarga, setErrorDeCarga] = useState('')
  const [aviso, setAviso] = useState('')
  const [trabajando, setTrabajando] = useState(false)

  const cargar = useCallback(() => {
    setErrorDeCarga('')
    Promise.all([obtenerPedido(token, pedidoId), ofertaDelPedido(token, pedidoId)])
      .then(([p, o]) => {
        setPedido(p)
        setOferta(o)
      })
      .catch((e) => setErrorDeCarga(mensajeDePedido(e, 'No se pudo cargar el pedido.')))
  }, [token, pedidoId])

  useEffect(() => {
    setPedido(null)
    setOferta(null)
    setPersonaId(null)
    cargar()
  }, [cargar])

  // La persona activa: la elegida si sigue existiendo; si no, la primera.
  const persona = pedido?.personas.find((p) => p.id === personaId) ?? pedido?.personas[0] ?? null

  /** Ejecuta una escritura; el pedido que devuelve reemplaza al actual. Un 409 de concurrencia recarga. */
  async function ejecutar(accion: Promise<Pedido>, alExito?: () => void) {
    if (trabajando) return
    setTrabajando(true)
    setAviso('')
    try {
      setPedido(await accion)
      alExito?.()
    } catch (e) {
      setAviso(mensajeDePedido(e, 'No se pudo hacer el cambio. Inténtalo de nuevo.'))
      cargar()
    } finally {
      setTrabajando(false)
    }
  }

  function agregarLinea(datos: Omit<DatosDeLinea, 'personaId'>) {
    if (!persona) return
    ejecutar(agregarLineaAlPedido(token, pedidoId, { ...datos, personaId: persona.id }), () => setDialogo(null))
  }

  if (errorDeCarga) {
    return <EstadoError mensaje={errorDeCarga} accion={<Boton onClick={cargar}>Reintentar</Boton>} />
  }
  if (!pedido || !oferta || !persona) return <EstadoCargando />

  const esPensionista = persona.tipoCliente === 'pensionista'
  const sinLineas = persona.lineas.length === 0
  const planDelServicio =
    esPensionista && pedido.servicio === 'desayuno'
      ? ('plan_desayuno' as const)
      : esPensionista && pedido.servicio === 'cena'
        ? ('plan_cena' as const)
        : null

  return (
    <section className="ped">
      <header className="ped-encabezado">
        <Boton variante="secundario" onClick={onVolver}>
          ← Mesas
        </Boton>
        <div className="ped-titulo">
          <strong>{titulo}</strong>
          <span className="cta-nota">
            Pedido {pedido.numero} · {SERVICIO[pedido.servicio]}
          </span>
        </div>
        <div className="ped-total-grande" aria-label="Total del pedido">
          {enSoles(pedido.total)}
        </div>
      </header>

      {aviso && <Aviso tono="error">{aviso}</Aviso>}
      {pedido.avisos.includes('TAPER_SIN_MONTO') && (
        <Aviso>Este pedido es para llevar pero todavía no se definió el precio del táper: no se está cobrando.</Aviso>
      )}

      <div className="ped-personas" role="tablist" aria-label="Personas de la mesa">
        {pedido.personas.map((p) => (
          <button
            key={p.id}
            type="button"
            role="tab"
            aria-selected={p.id === persona.id}
            className={`ped-persona ${p.id === persona.id ? 'ped-persona-activa' : ''}`}
            onClick={() => setPersonaId(p.id)}
          >
            <span>{p.etiqueta}</span>
            <strong>{enSoles(p.total)}</strong>
          </button>
        ))}
        <button
          type="button"
          className="ped-persona ped-persona-nueva"
          disabled={trabajando}
          onClick={() => ejecutar(agregarPersonaAlPedido(token, pedidoId), undefined)}
        >
          + Persona
        </button>
      </div>

      <div className="ped-cuerpo">
        <div className="ped-acciones">
          <p className="ped-persona-titulo">
            {persona.etiqueta} {esPensionista && <Insignia tono="activo">Pensionista</Insignia>}
          </p>
          {oferta.hayMenu ? (
            <>
              <Boton onClick={() => setDialogo({ tipo: 'menu', cual: 'menu_normal' })}>Menú del día</Boton>
              {oferta.extra.length > 0 && (
                <Boton variante="secundario" onClick={() => setDialogo({ tipo: 'menu', cual: 'menu_extra' })}>
                  Menú extra
                </Boton>
              )}
            </>
          ) : (
            pedido.servicio === 'almuerzo' && <p className="cta-nota">Hoy no hay menú programado.</p>
          )}
          {planDelServicio && (
            <Boton
              cargando={trabajando}
              onClick={() => agregarLinea({ tipo: planDelServicio, cantidad: 1 })}
            >
              {planDelServicio === 'plan_desayuno' ? 'Desayuno del plan' : 'Cena del plan'}
            </Boton>
          )}
          <Boton variante="secundario" onClick={() => setDialogo({ tipo: 'carta' })}>
            Carta
          </Boton>
          <Boton variante="secundario" onClick={() => setDialogo({ tipo: 'extra' })}>
            Extra
          </Boton>
          {!esPensionista && sinLineas && (
            <Boton variante="texto" onClick={() => setDialogo({ tipo: 'pensionista' })}>
              Es pensionista
            </Boton>
          )}
          {sinLineas && pedido.personas.length > 1 && (
            <Boton
              variante="texto"
              cargando={trabajando}
              onClick={() => ejecutar(quitarPersonaDelPedido(token, pedidoId, persona.id), () => setPersonaId(null))}
            >
              Quitar esta persona
            </Boton>
          )}
        </div>

        <ul className="ped-lineas" aria-label={`Productos de ${persona.etiqueta}`}>
          {sinLineas && <li className="ped-vacio">Todavía no hay nada para {persona.etiqueta}. Agrega un producto.</li>}
          {persona.lineas.map((l) => (
            <li key={l.id} className="ped-linea">
              <div className="ped-linea-info">
                <strong>{nombreDeLinea(l)}</strong>
                {l.componentes.length > 0 && (
                  <span className="cta-nota">{l.componentes.map((c) => c.productoNombre).join(' · ')}</span>
                )}
                {l.observacion && <span className="cta-nota">Nota: {l.observacion}</span>}
                <span className="cta-nota">
                  {enSoles(l.precioUnitario)}
                  {l.tarifaOrigen === 'plan' && ' · precio de su plan'}
                  {l.segundoDelDia && ' · segundo del día'}
                  {l.tarifaOrigen === 'manual' && ' · precio puesto a mano'}
                  {l.taperUnitario > 0 && ` · táper ${enSoles(l.taperUnitario)}`}
                </span>
              </div>
              <div className="ped-linea-cantidad">
                {l.estado === 'pendiente' && !cantidadFija(l, persona) ? (
                  <>
                    <button
                      type="button"
                      className="ped-paso"
                      aria-label="Uno menos"
                      disabled={trabajando || l.cantidad <= 1}
                      onClick={() =>
                        ejecutar(modificarLineaDelPedido(token, pedidoId, l.id, { cantidad: l.cantidad - 1, observacion: l.observacion }))
                      }
                    >
                      −
                    </button>
                    <span aria-label="Cantidad">{l.cantidad}</span>
                    <button
                      type="button"
                      className="ped-paso"
                      aria-label="Uno más"
                      disabled={trabajando}
                      onClick={() =>
                        ejecutar(modificarLineaDelPedido(token, pedidoId, l.id, { cantidad: l.cantidad + 1, observacion: l.observacion }))
                      }
                    >
                      +
                    </button>
                  </>
                ) : (
                  <span aria-label="Cantidad">× {l.cantidad}</span>
                )}
              </div>
              <strong className="ped-linea-total">{enSoles(l.precioUnitario * l.cantidad)}</strong>
              {l.estado === 'pendiente' && (
                <div className="ped-linea-botones">
                  <button type="button" className="plan-historial-boton" onClick={() => setDialogo({ tipo: 'nota', linea: l })}>
                    Nota
                  </button>
                  <button
                    type="button"
                    className="plan-historial-boton"
                    disabled={trabajando}
                    onClick={() => ejecutar(quitarLineaDelPedido(token, pedidoId, l.id))}
                  >
                    Quitar
                  </button>
                </div>
              )}
            </li>
          ))}
        </ul>
      </div>

      <footer className="ped-pie">
        <dl className="ped-totales">
          <div>
            <dt>Subtotal</dt>
            <dd>{enSoles(pedido.subtotal)}</dd>
          </div>
          {pedido.taper > 0 && (
            <div>
              <dt>Táper</dt>
              <dd>{enSoles(pedido.taper)}</dd>
            </div>
          )}
          <div className="ped-total">
            <dt>Total</dt>
            <dd>{enSoles(pedido.total)}</dd>
          </div>
        </dl>
        <Boton disabled title="Comandar llega en la siguiente entrega">
          Comandar (próximamente)
        </Boton>
      </footer>

      {dialogo?.tipo === 'menu' && (
        <DialogoMenu
          tipo={dialogo.cual}
          oferta={oferta}
          esPensionista={esPensionista}
          trabajando={trabajando}
          onCerrar={() => setDialogo(null)}
          onAgregar={(componentes, observacion) => agregarLinea({ tipo: dialogo.cual, cantidad: 1, componentes, observacion })}
        />
      )}
      {dialogo?.tipo === 'carta' && (
        <DialogoCarta
          carta={oferta.carta}
          trabajando={trabajando}
          onCerrar={() => setDialogo(null)}
          onElegir={(p: ProductoOfrecido) => agregarLinea({ tipo: 'producto', productoId: p.id, cantidad: 1 })}
        />
      )}
      {dialogo?.tipo === 'extra' && (
        <DialogoExtra
          trabajando={trabajando}
          onCerrar={() => setDialogo(null)}
          onAgregar={(descripcion, precio, cantidad) => agregarLinea({ tipo: 'extra_libre', descripcion, precio, cantidad })}
        />
      )}
      {dialogo?.tipo === 'pensionista' && (
        <DialogoPensionista
          token={token}
          trabajando={trabajando}
          onCerrar={() => setDialogo(null)}
          onElegir={(p: ResumenPensionista) =>
            ejecutar(asignarPensionistaAPersona(token, pedidoId, persona.id, p.id), () => setDialogo(null))
          }
        />
      )}
      {dialogo?.tipo === 'nota' && (
        <DialogoNota
          inicial={dialogo.linea.observacion}
          trabajando={trabajando}
          onCerrar={() => setDialogo(null)}
          onGuardar={(nota) =>
            ejecutar(
              modificarLineaDelPedido(token, pedidoId, dialogo.linea.id, { cantidad: dialogo.linea.cantidad, observacion: nota }),
              () => setDialogo(null),
            )
          }
        />
      )}
    </section>
  )
}
```

`estilos/global.css` — agregar al final:
```css
/* US-028: la pantalla del pedido del POS */
.ped { padding: var(--espacio-4); display: grid; gap: var(--espacio-4); }
.ped-encabezado { display: flex; align-items: center; gap: var(--espacio-4); flex-wrap: wrap; }
.ped-titulo { display: grid; flex: 1; min-width: 160px; }
.ped-titulo strong { font-size: 20px; }
.ped-total-grande { font-size: 28px; font-weight: 800; font-variant-numeric: tabular-nums; }
.ped-personas { display: flex; gap: var(--espacio-2); overflow-x: auto; padding-bottom: 4px; }
.ped-persona { min-height: 56px; min-width: 120px; padding: 0 var(--espacio-4); border: 2px solid var(--color-separador); border-radius: 12px; background: var(--color-superficie, #fff); font: inherit; display: grid; align-content: center; text-align: left; cursor: pointer; }
.ped-persona span { font-size: 13px; color: var(--color-texto-secundario); }
.ped-persona-activa { border-color: var(--color-marca-oscura, #2d6a4f); background: #eef7f1; }
.ped-persona-nueva { border-style: dashed; justify-items: center; font-weight: 700; }
.ped-cuerpo { display: grid; grid-template-columns: minmax(180px, 240px) 1fr; gap: var(--espacio-4); align-items: start; }
.ped-acciones { display: grid; gap: var(--espacio-2); }
.ped-acciones .boton { min-height: 52px; }
.ped-persona-titulo { margin: 0; font-weight: 700; display: flex; gap: var(--espacio-2); align-items: center; flex-wrap: wrap; }
.ped-lineas { list-style: none; margin: 0; padding: 0; display: grid; gap: var(--espacio-2); }
.ped-vacio { padding: var(--espacio-5); text-align: center; color: var(--color-texto-secundario); border: 2px dashed var(--color-separador); border-radius: 12px; }
.ped-linea { display: grid; grid-template-columns: 1fr auto auto; gap: var(--espacio-3); align-items: center; padding: var(--espacio-3) var(--espacio-4); border: 1px solid var(--color-separador); border-radius: 12px; background: var(--color-superficie, #fff); }
.ped-linea-info { display: grid; gap: 2px; min-width: 0; }
.ped-linea-cantidad { display: flex; align-items: center; gap: var(--espacio-2); font-weight: 700; font-variant-numeric: tabular-nums; }
.ped-paso { width: 44px; height: 44px; border-radius: 10px; border: 1px solid var(--color-separador); background: #fff; font: inherit; font-size: 20px; font-weight: 800; cursor: pointer; }
.ped-paso:disabled { opacity: .4; cursor: default; }
.ped-linea-total { font-variant-numeric: tabular-nums; }
.ped-linea-botones { grid-column: 1 / -1; display: flex; gap: var(--espacio-4); }
.ped-pie { display: flex; justify-content: space-between; align-items: center; gap: var(--espacio-4); flex-wrap: wrap; border-top: 1px solid var(--color-separador); padding-top: var(--espacio-4); }
.ped-totales { display: flex; gap: var(--espacio-5); margin: 0; }
.ped-totales dt { font-size: 12px; color: var(--color-texto-secundario); }
.ped-totales dd { margin: 0; font-weight: 700; font-variant-numeric: tabular-nums; }
.ped-total dd { font-size: 24px; font-weight: 800; }
.ped-dialogo { display: grid; gap: var(--espacio-3); }
.ped-parte { border: 1px solid var(--color-separador); border-radius: 10px; padding: var(--espacio-3); display: grid; gap: var(--espacio-2); }
.ped-parte legend { font-weight: 700; padding: 0 var(--espacio-2); }
.ped-opcion { display: flex; align-items: center; gap: var(--espacio-3); min-height: 48px; padding: 0 var(--espacio-3); border: 2px solid var(--color-separador); border-radius: 10px; cursor: pointer; }
.ped-opcion-elegida { border-color: var(--color-marca-oscura, #2d6a4f); background: #eef7f1; }
.ped-incluida { margin: 0; }
.ped-precio-menu { margin: 0; font-size: 22px; font-weight: 800; }
.ped-lista-carta { list-style: none; margin: 0; padding: 0; display: grid; gap: var(--espacio-2); max-height: 50vh; overflow-y: auto; }
.ped-producto { width: 100%; min-height: 52px; display: flex; justify-content: space-between; align-items: center; gap: var(--espacio-3); padding: 0 var(--espacio-4); border: 1px solid var(--color-separador); border-radius: 10px; background: #fff; font: inherit; text-align: left; cursor: pointer; }
.ped-producto:disabled { opacity: .5; cursor: default; }
@media (max-width: 640px) {
  .ped-cuerpo { grid-template-columns: 1fr; }
  .ped-acciones { grid-template-columns: repeat(2, minmax(0, 1fr)); }
  .ped-persona-titulo { grid-column: 1 / -1; }
}
```

- [ ] **Step 4: Run check to verify it passes**

Run: `cd rikos-web && npx tsc -b --force && npm run lint 2>&1 | grep -iE "venta"`
Expected: `tsc` limpio y sin líneas de lint de `features/venta`.

- [ ] **Step 5: Commit** (solo si el usuario lo pide)

---

### Task 5: Conectar el mapa y el POS

**Files:**
- Modify: `rikos-web/src/features/venta/MapaDeMesas.tsx`, `rikos-web/src/apps/pos/index.tsx`

**Interfaces:**
- Consumes: `abrirPedido`, `obtenerPedidoDeMesa`, `Pedido` de `api/cliente.ts`; `PantallaPedido`.
- Produces: `MapaDeMesas({ token, onAbrirPedido }: { token: string; onAbrirPedido?: (pedidoId: string, titulo: string) => void })`. Sin `onAbrirPedido` (el panel del administrador) se ve como hoy: solo el estado y el bloqueo.

- [ ] **Step 1: Write the failing check**

En `apps/pos/index.tsx`, pasar `onAbrirPedido={...}` a `<MapaDeMesas>` y renderizar `<PantallaPedido>` cuando haya un pedido abierto, **antes** de cambiar `MapaDeMesas`.

- [ ] **Step 2: Run check to verify it fails**

Run: `cd rikos-web && npx tsc -b --force`
Expected: FAIL, `Property 'onAbrirPedido' does not exist on type 'IntrinsicAttributes & { token: string }'`.

- [ ] **Step 3: Write minimal implementation**

`apps/pos/index.tsx` — agregar el estado y la vista (el resto del archivo no cambia):
```tsx
import { MapaDeMesas, PantallaPedido } from '../../features/venta'
// ...
const [pedidoAbierto, setPedidoAbierto] = useState<{ id: string; titulo: string } | null>(null)
// ...y al cambiar de mozo (setMozoActivo(null)) tambien: setPedidoAbierto(null)
// ...
      {pedidoAbierto ? (
        <PantallaPedido
          token={mozoActivo.token}
          pedidoId={pedidoAbierto.id}
          titulo={pedidoAbierto.titulo}
          onVolver={() => setPedidoAbierto(null)}
        />
      ) : (
        <MapaDeMesas token={mozoActivo.token} onAbrirPedido={(id, titulo) => setPedidoAbierto({ id, titulo })} />
      )}
```
Y en el efecto de inactividad, cuando expira el tiempo (`setMozoActivo(null)`) limpiar también `setPedidoAbierto(null)`: **el pedido no se pierde** (vive en el servidor), solo se vuelve a la pantalla de PIN.

`MapaDeMesas.tsx`:
1. Firma: `export function MapaDeMesas({ token, onAbrirPedido }: { token: string; onAbrirPedido?: (pedidoId: string, titulo: string) => void })` y pasar `onAbrirPedido` al `DialogoDeMesa`.
2. Importar `abrirPedido`, `obtenerPedidoDeMesa`, `type ServicioDelPedido` y `Select`.
3. Encima de la rejilla, si `onAbrirPedido`: un botón **"Pedido para llevar"** que llama `abrirPedido(token, { canal: 'para_llevar' })` y luego `onAbrirPedido(pedido.id, 'Para llevar')`; si la API responde `SERVICIO_REQUERIDO`, mostrar un `Select` de servicio (desayuno, almuerzo, cena) y reintentar con ese valor. Los errores se muestran con `mensajeDePedido`.
4. En `DialogoDeMesa`:
   - mesa **libre** y `onAbrirPedido`: botón principal **"Abrir pedido"** (arriba de "Bloquear mesa"): `abrirPedido(token, { canal: 'mesa', mesaId: mesa.id, servicio })` → `onAbrirPedido(pedido.id, `Mesa ${mesa.numero}`)`; con el mismo manejo de `SERVICIO_REQUERIDO`.
   - mesa **ocupada** o **pendiente de cierre** y `onAbrirPedido`: botón **"Ver pedido"**: `obtenerPedidoDeMesa(token, mesa.id)` → `onAbrirPedido(pedido.id, `Mesa ${mesa.numero}`)`. El texto "Las mesas ocupadas… se atienden desde su pedido" se reemplaza por el botón.
   - Un error de la API se muestra con `mensajeDePedido(e, 'No se pudo abrir el pedido.')` (p. ej. otro mozo abrió la mesa a la vez: "Esa mesa ya tiene un pedido abierto." y el mapa se recarga).

- [ ] **Step 4: Run check to verify it passes**

Run: `cd rikos-web && npx tsc -b --force && npm run lint 2>&1 | grep -iE "venta|apps/pos"`
Expected: `tsc` limpio y sin líneas de lint de lo nuevo.

- [ ] **Step 5: Commit** (solo si el usuario lo pide)

---

### Task 6: Verificación en el navegador y cierre

**Files:**
- Create: `rikos-backend/doc/historias/US-028-tomar-el-pedido-pantalla.md`
- Modify: `rikos-backend/doc/historias/README.md`, `docs/producto/plan/sprint-06-vender-en-mesa.md` (2B ✅), `docs/producto/plan/README.md`, `ESTADO.md`, `docs/pendientes/README.md`

- [ ] **Step 1: Entrar al POS como mozo**

Con el backend, el gateway y Vite arriba: asignar un PIN aleatorio de prueba a `mozotest` (desde el navegador, con el token de `admin`, guardándolo en `sessionStorage` y tecleándolo con el teclado numérico; **no escribirlo en la conversación**) y entrar en `http://localhost:5173/pos`. Confirmar que se ve el mapa con el botón "Pedido para llevar".

- [ ] **Step 2: El recorrido completo (escritorio)**

Comprobar cada paso con `read_page` / JavaScript y capturas:
1. Tocar la mesa 1 (libre) → **Abrir pedido** → aparece la pantalla del pedido con "Mesa 1 · Pedido 000N · Almuerzo", "Persona 1" y el total S/ 0.00.
2. **Menú del día** → sin elegir el segundo: error "Elige una opción de: Segundo."; elegir Seco de res → se agrega a S/ 12.00 y el total sube.
3. **Carta** → tocar "Gaseosa personal" → se agrega a S/ 3.50; subir la cantidad a 2 con "+" → S/ 7.00.
4. **Extra** → "Huevo frito", S/ 3, cantidad 2 → aparece "precio puesto a mano" y el total suma S/ 6.00.
5. **+ Persona** → "Persona 2" → **Es pensionista** → buscar "Quispe" → asignarlo → la etiqueta pasa al nombre y aparece la insignia **Pensionista**.
6. Con esa persona: **Menú del día** → el precio del menú es el de su plan (S/ 8.00) y la línea dice "precio de su plan"; agregar otro menú del día → la segunda línea dice "segundo del día".
7. **Nota** en una línea y **Quitar** otra; comprobar que el total baja.
8. Volver a **← Mesas**: la mesa 1 aparece **Ocupada**; tocarla → **Ver pedido** vuelve al mismo pedido con todo lo cargado.
9. Bloquear la mesa 1 → error "solo una mesa libre" (el servidor lo rechaza).
Expected: todos los pasos como se describen, sin códigos de error crudos en pantalla.

- [ ] **Step 3: Para llevar y táper**

**Pedido para llevar** → agregar un menú → la línea muestra el táper y el pie tiene la fila "Táper". Repetir con el parámetro del táper en blanco (Parámetros de cobro) → aparece el aviso "todavía no se definió el precio del táper".

- [ ] **Step 4: Errores y bordes**

- Marcar "Chicha morada" (de la carta de ejemplo, **otro producto**) como agotada desde la disponibilidad del panel y comprobar que ya no sale en la carta del pedido.
- Abrir la misma mesa desde dos pestañas: la segunda ve "Esa mesa ya tiene un pedido abierto.".
- Un local sin menú programado (cambiar la fecha del reloj no aplica: usar un día sin menú, p. ej. quitando temporalmente las opciones del día en un local de prueba) muestra "Hoy no hay menú programado.".

- [ ] **Step 5: Celular y tablet**

`resize_window` a `mobile` (375) y `tablet` (768): sin desborde horizontal de página (`scrollWidth === clientWidth`), los botones se pueden tocar (alto ≥ 44 px), los diálogos caben, y el reloj de inactividad no descarta el pedido (al volver de la pantalla de PIN, la mesa sigue ocupada y el pedido está intacto). Volver a `desktop` al terminar.

- [ ] **Step 6: Limpiar los datos de prueba y documentar**

Borrar los pedidos de prueba de la base (sentencias de la historia US-028); dejar las mesas, el menú de hoy y la carta de ejemplo (son datos de desarrollo, inventados). Escribir `US-028-tomar-el-pedido-pantalla.md` con las secciones habituales, incluido el detalle de los datos de ejemplo cargados; actualizar `ESTADO.md`, los índices y el plan (2B ✅), y anotar en `docs/pendientes/README.md` cualquier decisión (`Ruling:`) que haya salido.

- [ ] **Step 7: Commit** (solo si el usuario lo pide)

---

## Self-review

**Cobertura de la especificación (3.3, 3.4, 3.5, 3.11):**
- Abrir pedido en mesa o para llevar, servicio del horario o elegido → Tarea 5 (mapa) y tarea 2 (API).
- Personas, pensionista por búsqueda de nombre o DNI → Tarea 3 (`DialogoPensionista`) y 4.
- Menú del día con opciones (una incluida, varias se eligen), menú extra, carta sin agotados, comida del plan de desayuno y cena, extra con precio del mozo → Tareas 3 y 4.
- Cantidad, nota, quitar → Tarea 4.
- Totales por persona y del pedido, táper y su aviso → Tarea 4 (pinta lo que devuelve la API).
- Ver el pedido de una mesa ocupada → Tarea 5.
- Celular, tablet, estados de la pantalla → Tarea 6.

**Fuera de esta entrega, a propósito:** comandar y su vista imprimible (entrega 3, por eso el botón está deshabilitado con su explicación); anular (entrega 3); trasladar y dividir (entrega 4); pedir desde el panel del administrador (el mapa del panel sigue siendo solo de estado y bloqueo; el mozo trabaja en el POS); el ticket del kiosco (sprint 8).

**Riesgos que el plan deja explícitos:** (1) la programación semanal puede rechazar agregar opciones a una semana ya confirmada: la tarea 1 lo anticipa con un `Ruling:`; (2) `Campo` debe aceptar `autoFocus` (hereda de `input`); (3) el proyecto no tiene pruebas de componentes: la verificación es `tsc` + lint + navegador, como en las entregas anteriores.

**Consistencia de tipos:** `Pedido`, `PersonaDelPedido`, `LineaDelPedido`, `OfertaDelDia` (tarea 2) coinciden campo a campo con `PedidoRespuesta` y `OfertaDelDia` de la API de la 2A (incluido `avisos` y `taperUnitario`); las funciones `abrirPedido` … `quitarLineaDelPedido` de la tarea 2 son las que consumen las tareas 4 y 5; los diálogos de la tarea 3 exponen exactamente las props que usa la tarea 4.

**Review Focus → dónde se comprueba:** aislamiento entre mesas y volver al mapa (Tarea 6, pasos 2.8 y 4); doble toque (`trabajando` deshabilita y `ejecutar` ignora si ya hay una petición); errores en español (`mensajes.ts`, comprobado en 2.2 y 4); 409 de concurrencia (`ejecutar` recarga); menú con varias opciones (2.2); pensionista y precio de su plan (2.5 y 2.6); sin menú o sin carta (2.x y 4); celular y tablet (5).
