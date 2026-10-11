# Auditorías

Hallazgos de revisiones externas al código -- no auditoría de datos
(eso es `auditoria` en el modelo de datos, tabla real del sistema), sino
revisiones del propio trabajo: qué se encontró, qué era real y qué era
falsa alarma, qué se corrigió.

Ejemplo real ya ocurrido (sin archivo propio todavía, documentado dentro
de `rikos-backend/doc/historias/2026-09-27-sprint3-control-y-trazabilidad.md`):
la revisión posterior al cierre de US-006 que encontró 4 problemas reales
(JSON armado a mano, local desactualizado en el token, intentos denegados
sin auditar, falta de pruebas por rol) y 1 falsa alarma.

## Revisiones disponibles

| Fecha | Auditoría | Alcance |
|---|---|---|
| 2026-10-11 | [Auditoría integral de RIKOS](2026-10-11/README.md) | Revisión por módulo hasta sprint 7 parcial, modelo de datos, arquitectura, frontend, sprints y aceptación; tablas y Mermaid |

Las revisiones transversales viven aquí con su fecha y evidencia. La historia de
cada entrega sigue registrando lo que se construyó y cómo se verificó. Las soluciones
pendientes de esta revisión están en [el registro central](../pendientes/02-auditoria-integral-sprints-0-7-2026-10-11.md).
