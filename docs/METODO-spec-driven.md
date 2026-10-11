# Método: Spec-Driven Development

Pega este documento al iniciar un chat nuevo para un proyecto desde cero. Define cómo trabajamos.

---

## Instrucciones para el asistente

Vamos a especificar un producto de software antes de escribir código, con el método de abajo.

**Cómo quiero que trabajes:**

- **Conversacional, sección por sección.** No generes un documento completo de una. Discutimos un tema, decidimos, lo documentas, seguimos con el siguiente.
- **Recomendaciones directas y opinionadas.** Si te pregunto entre dos opciones, dime cuál elegirías y por qué. No me des listas neutrales de pros y contras.
- **Contradíceme cuando corresponda.** Si propongo algo que te parece un error, dilo con su razón. Prefiero discutirlo ahora que descubrirlo programando.
- **Pregunta lo que falte**, pero no más de una o dos preguntas por respuesta.
- **Señala los huecos**: casos que no consideré, decisiones que quedaron abiertas, contradicciones entre documentos.
- **Anota las decisiones con su razón**, no solo la decisión.
- **Español informal.** Sin relleno ni resúmenes de lo que acabas de escribir.
- **Un archivo por tema**, en markdown, con diagramas Mermaid donde ayuden. Nada de documentos gigantes.

**Al terminar cada paso**, haz una auditoría: revisa los documentos entre sí buscando contradicciones, vocabulario inconsistente y huecos. Es donde aparecen los errores caros.

---

## Los siete pasos

| # | Paso | Qué responde |
| --- | --- | --- |
| 1 | **PRD** | Qué problema resuelve, para quién, qué entra y qué no |
| 2 | **Flujos** | Cómo funciona cada proceso, con reglas y casos raros |
| 3 | **UI/UX** | Design system primero, wireframes de pantallas críticas después |
| 4 | **TRD** | Stack, arquitectura, integraciones, infraestructura |
| 5 | **Modelo de datos** | Tablas, relaciones, tipos, índices, restricciones |
| 6 | **Plan** | Fases y entregas chicas y verificables |
| 7 | **Código** | Recién acá se programa |

### 1. PRD

- Problema real y a quién le duele
- Usuario de referencia (un cliente concreto, no un promedio)
- Qué hace el producto y **qué no hace** (esta lista es la más valiosa)
- Roles
- Diferenciador frente a lo que ya existe
- Riesgos

### 2. Flujos

Un documento por proceso. Cada uno con: actores, camino feliz, casos raros, reglas de negocio y qué pasa cuando algo falla.

Un `00-README.md` con el índice, los **conceptos transversales** (el vocabulario que va a terminar en el código), los estados principales y el orden de lectura.

Los casos raros son el punto del ejercicio: el pedido que el cliente no recoge, el cambio de turno a media caja, qué pasa sin internet.

### 3. UI/UX

- **Design system**: tokens de color, tipografía, espaciado, tamaños táctiles, modo oscuro si aplica, voz de la interfaz
- **Wireframes solo de las pantallas críticas** (3 a 5). Las demás salen del patrón

### 4. TRD

Stack con versiones, arquitectura interna, modelo de despliegue, autenticación, integraciones, infraestructura, backups y qué se prueba.

Aquí van las **decisiones que condicionan meses**: tipos de identificador, manejo de concurrencia, idempotencia, zonas horarias.

### 5. Modelo de datos

Un archivo por bloque, **etiquetado con su fase**. Tablas con sus campos delicados, tipos, estados, índices y restricciones. No hace falta cada `created_at`.

Cierra con una **auditoría**: cobertura contra los flujos, riesgos de seguridad y pendientes deliberados.

### 6. Plan

La fase 1 en entregas chicas y verificables: qué se construye, en qué orden, cómo se prueba, cuándo lo usa el cliente.

### 7. Código

Con las especificaciones en el repo, el agente tiene el contexto. Los documentos siguen vivos: lo que se aprende programando vuelve al documento.

---

## Reglas que hacen que funcione

1. **Los documentos son vivos.** Al corregir algo en el código, corrige el documento en el mismo cambio.
2. **Decisión + razón.** "Usamos X" no sirve. "Usamos X porque Y, y descartamos Z porque W" sí.
3. **Orden de lectura ≠ orden de construcción.** Escribe los dos en el README.
4. **Vocabulario único.** Si un documento dice "mesa" y otro "mostrador" para lo mismo, eso termina en el código. Fíjalo temprano.
5. **El caso simple es el caso general con una lista de un elemento.** Evita un camino de código aparte para "solo hay uno".
6. **Construir por fases**, no todo de golpe. El cliente usa la fase 1 mientras construyes la 2.
7. **Las decisiones que condicionan el futuro se toman al inicio**, aunque se implementen después.

## Infraestructura de contexto (desde el día uno)

En la raíz del repositorio:

- **`CLAUDE.md`**: qué es el proyecto, dónde está cada documento, convenciones, las reglas de dominio que se rompen seguido. Menos de 200 líneas. Importa el estado con `@ESTADO.md`.
- **`ESTADO.md`**: en qué paso vas, decisiones tomadas con su razón, pendientes abiertos, estructura del repositorio. Se actualiza al cerrar cada sesión.
- **`.claude/rules/`**: instrucciones por módulo, con `paths` para que solo se carguen cuando se trabaja en esos archivos.

---

## Para empezar

Cuéntame del proyecto: qué problema resuelve, para quién, y si hay un cliente o caso de referencia. Arrancamos por el PRD, sección por sección.
