# Convencion: una historia de usuario, un archivo

A partir de ahora, cada historia de usuario (o sprint completo, si son
varias chicas relacionadas) se documenta aca, no solo en el mensaje de
chat. Un archivo por historia: `US-XXX-nombre-corto.md`.

## Que va en cada archivo

- **Que se pidio** (resumen corto, no el copy-paste completo del chat).
- **Que se construyo**: modelo/migraciones, backend, frontend -- lo
  suficiente para que alguien que no vio el chat entienda el cambio sin
  leer el diff completo.
- **Que se dejo fuera a proposito y por que** (si algo se excluyo por
  duda o por no estar pedido explicitamente).
- **Como se verifico**: pruebas automatizadas (cuantas, cuales) +
  evidencia en vivo (que se probo a mano, con que datos reales -- no
  "deberia funcionar", sino "se probo con el usuario X y paso esto").
- **Bugs encontrados en el camino** (aunque no sean del alcance de la
  historia) y como se resolvieron.

## Por que

Para que otra sesion de Claude (u otra persona) pueda entender el estado
real del sistema sin tener que releer la conversacion completa donde se
construyo. El chat se pierde o se resume; esto queda.

## Historias documentadas

| Archivo | Historia | Estado |
|---|---|---|
| `US-004-US-006-permisos-roles.md` | Permisos granulares + roles y auditoria | Hecho, verificado |
