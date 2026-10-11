# Aprendizajes

Lecciones que costaron tiempo o plata y conviene no repetir -- distinto
de una decisión de arquitectura (eso va en `producto/trd/`). Un
aprendizaje es del tipo "esto nos pasó, así lo evitamos la próxima".

Ejemplos reales de esta sesión que calificarían acá (hoy documentados
dentro de historias/decisiones técnicas, no en archivo propio):

- `tsc --noEmit` no verificaba nada útil en este proyecto (config
  "solution" de TypeScript) -- el comando real es `tsc -b`.
- Un 403 puede ser en realidad un 500 disfrazado (`LazyInitializationException`
  + `/error` sin `permitAll`) -- no asumir que un 403 es siempre un
  problema de permisos.
- `./mvnw spring-boot:run` desde la raíz del reactor con `-pl`/`-am`
  falla -- hay que correrlo desde el propio directorio del módulo.

Pendiente: decidir si se migran acá o quedan donde están (documentadas
inline en `RIKOS-decisiones-tecnicas.md` y los historia docs).
