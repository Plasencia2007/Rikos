# TRD

Stack, arquitectura interna, modelo de despliegue, autenticación,
integraciones, infraestructura, backups y qué se prueba. Per
`docs/METODO-spec-driven.md` §4.

## Archivos

| Archivo | Qué contiene |
|---|---|
| `00-arquitectura.md` | Por qué dos servicios y no doce, por qué el local opera sin Internet, las cinco decisiones de modelado que no se pueden revertir barato |
| `01-stack-y-despliegue.md` | Stack con versiones, cómo se levanta el entorno, direcciones locales, credenciales de desarrollo |

## Decisiones técnicas -- no duplicadas acá

`rikos-backend/doc/RIKOS-decisiones-tecnicas.md` (DT-001 en adelante) es
la bitácora **cronológica** de decisiones técnicas, con su razón --
sigue siendo la fuente de verdad, citada por su número (`DT-039`, etc.)
en código y en los historia docs. No se migró ni se duplicó acá a
propósito: es un documento vivo que crece con cada sprint, y tener dos
copias garantiza que una de las dos quede desactualizada.

Este TRD es el resumen organizado por tema (para alguien que llega
nuevo); la bitácora de decisiones es el detalle cronológico con su
razón completa (para entender por qué algo quedó como quedó).
