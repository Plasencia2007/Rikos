# UI/UX

Design system y pantallas críticas. Per `docs/METODO-spec-driven.md` §3.

| Archivo | Qué contiene |
|---|---|
| `01-mapa-de-pantallas.md` | Las ~70 pantallas del sistema, agrupadas por superficie: POS, kiosco, portal, motorizado, panel del administrador |
| `03-pos-rediseno.md` | Rediseño del POS (2026-09-30): barra lateral, cabezal, avisos, Inicio, Mesas por salón, Pedido rápido, Órdenes activas en tarjetas e Historial; decisiones con su razón y la contradicción resuelta con "sin barra lateral" |
| `02-reglas-de-diseno-transversales.md` | El POS se usa de pie, toda acción sensible pide motivo, los permisos ocultan pero el backend decide, alertar sin bloquear, cuatro estados por pantalla, aviso sin conexión, estado compartido que se actualiza solo |

Origen: secciones 6 y 7 del Product Backlog v2.0.

**Ojo con la numeración interna**: en el backlog original, las
subsecciones del mapa de pantallas quedaron numeradas `7.1`-`7.5` en vez
de `6.1`-`6.5`, y chocan con las de reglas de diseño (que también son
`7.1`-`7.7`). Se conservó tal cual para no alterar la fuente -- al citar,
nombrar el archivo, no solo el número.

## Lo que falta

- **Design system como documento**: los tokens reales ya existen en
  código (`rikos-web/src/estilos/tokens.css`) pero no hay un documento
  que explique la paleta, la tipografía, los tamaños táctiles y la voz
  de la interfaz con su porqué.
- **Wireframes de las 3-5 pantallas críticas**. El backlog ya señala la
  más difícil: US-028, tomar el pedido asignando por persona en mesa
  mixta, y recomienda probar bocetos con un mozo real antes del sprint 6.
