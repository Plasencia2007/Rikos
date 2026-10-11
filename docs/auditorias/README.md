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

Pendiente: decidir si las auditorías futuras se documentan acá como
archivo propio, o siguen viviendo dentro del historia doc del sprint que
revisan.
