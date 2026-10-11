# RIKOS

Sistema multilocal de gestión para el Restaurante Riko's (restaurante criollo peruano,
con pensionistas). Ver `README.md` para el pitch completo y la arquitectura de los tres
servicios (gateway, servicio-operacion, servicio-facturacion) + frontend.

@ESTADO.md

## Dónde está cada documento

- `docs/fuente/especificacion-v0.1/` -- la especificación que entregó el dueño
  (8 archivos). **No se edita.** Cuando un documento cita "`04` § Almuerzo" o
  "caso crítico 11", se refiere a estos archivos.
- `docs/METODO-spec-driven.md` -- cómo especificamos antes de programar.
- `docs/producto/{prd,flujos,ui-ux,trd,modelo-de-datos,plan}/` -- los 7 pasos del
  método. Mayormente vacío todavía (ver `ESTADO.md`); mientras se completa, la fuente
  de verdad real sigue siendo:
  - `rikos-backend/doc/RIKOS-modelo-de-datos.md` -- modelo de datos completo, real.
  - `rikos-backend/doc/RIKOS-decisiones-tecnicas.md` -- bitácora de decisiones (DT-NNN).
- `rikos-backend/doc/historias/` -- un archivo por historia de usuario o sprint: qué se
  pidió, qué se construyó, qué se dejó fuera y por qué, cómo se verificó, bugs
  encontrados. **Se escribe siempre**, no solo en el chat -- así una sesión nueva
  entiende el estado real sin releer la conversación completa.
- `docs/pendientes/`, `docs/aprendizajes/`, `docs/auditorias/`, `docs/designs/` --
  ver el `README.md` de cada una.

## Convenciones que se rompen seguido

- **`ULID` de 26 caracteres** para todo identificador (`Identificador.nuevo().valor()`
  en `contratos`). No usar autoincrement ni UUID random.
- **`Resultado<T, E>` (sealed interface) para todo flujo que puede fallar de forma
  esperada.** No lanzar excepciones para errores de negocio (código duplicado, no
  encontrado, etc.) -- esas van en un `enum` de error propio del caso de uso.
- **Flyway manda.** Las migraciones ya aplicadas nunca se editan -- una corrección va
  en una migración nueva, aunque sea sobre una tabla de la migración de ayer. Nunca
  `DROP TABLE`.
- **`tsc -b`, no `tsc --noEmit`.** El `tsconfig.json` raíz del frontend es de tipo
  "solution" (`"files": []`, solo `references`) -- `tsc --noEmit` en la raíz no
  verifica nada. El comando real, igual al de `package.json`, es `npx tsc -b --force`.
- **`./mvnw spring-boot:run` se corre desde el propio directorio del módulo**
  (`cd servicio-operacion && ../mvnw spring-boot:run`), nunca desde la raíz del
  reactor con `-pl`/`-am` -- eso falla con "No plugin found for prefix 'spring-boot'".
- **TDD real, siempre.** Un caso de uso nuevo lleva su test antes o junto con el
  código, no después "si da tiempo". Ver cualquier archivo en
  `rikos-backend/servicio-operacion/src/test/java` como referencia de estilo.
- **`RegistradorAuditoria` (`comun.auditoria`) resuelve el usuario y el local activo
  solos**, leyendo el `SecurityContext` -- ningún caso de uso le pasa el actor a mano.
  Un test que ejercite un caso de uso auditado necesita
  `ContextoDeSeguridadDePrueba.autenticarComo(...)` en `@BeforeEach`, o la escritura
  falla por la columna `NOT NULL` de `auditoria.usuario_id`.
- **Spring Modulith verifica los límites entre módulos**
  (`ArquitecturaDeModulosTest`). Un paquete nuevo que otro módulo necesita importar se
  expone con `@NamedInterface(...)` en su `package-info.java` -- no se asume acceso
  libre entre `identidad`, `catalogo`, `comun`.
- **Docker/Postgres se cae seguido en desarrollo** (WSL2). Si algo falla con
  "Connection refused" en :5433/:8081/:8080, primero `docker ps` -- si está vacío,
  `docker compose up -d` desde `rikos-backend/` y esperar `docker inspect --format
  '{{.State.Health.Status}}' rikos-postgres` en `healthy` antes de asumir un bug real.

## Reglas de trabajo

- Nunca commitear a menos que el usuario lo pida explícito.
- Documentar en `rikos-backend/doc/historias/` al cerrar cualquier historia o sprint,
  siempre -- ver `docs/aprendizajes/README.md` para el porqué.
- Para trabajo nuevo (feature o proyecto desde cero dentro de RIKOS), especificar
  primero con el método spec-driven antes de escribir código -- ver
  `docs/METODO-spec-driven.md`.
