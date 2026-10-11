<!-- Extraído del Product Backlog v2.0 (RIKOS-product-backlog.md, 2026-09-11), líneas 2738-2865. Distribuido por paso del método el 2026-09-29, ver docs/METODO-spec-driven.md. -->

## 5.3 Detalle de historias técnicas habilitantes

Trabajo real, necesario, y ausente del backlog original. No proviene de la especificación funcional —que es agnóstica de tecnología por decisión (RN-093)— sino de las decisiones técnicas registradas por el equipo. Se identifican con el prefijo **HT** para distinguirlas de las historias de negocio.

---

### [HT-01] Esqueleto que camina

- **Épica/Módulo:** EP-11 — Habilitantes técnicas
- **Prioridad:** Must · **Sprint:** 0 · **Estimación inicial:** M
- **Actor:** Equipo de desarrollo
- **Objetivo:** que exista un camino completo desde el navegador hasta la base de datos, antes de escribir lógica de negocio.
- **Alcance:** Docker Compose con Postgres y RabbitMQ levantando; el gateway enrutando `/api/**` y `/facturacion/**`; los dos servicios respondiendo su endpoint de salud; el frontend llamando a través del proxy y mostrando la respuesta.
- **Por qué va primero:** resuelve de una vez la red, el enrutamiento, la base de datos y la comunicación entre servicios. Todo lo demás se apoya en esto, y los problemas de entorno que no se resuelven aquí reaparecen disfrazados de "la historia tardó más de lo estimado" durante los tres primeros sprints.
- **Termina cuando:** las cuatro direcciones de salud responden y el frontend muestra el resultado en pantalla.
- **Dependencias:** ninguna.

---

### [HT-02] Base de datos y convenciones de migración

- **Épica/Módulo:** EP-11 — Habilitantes técnicas
- **Prioridad:** Must · **Sprint:** 0 · **Estimación inicial:** S
- **Actor:** Equipo de desarrollo
- **Objetivo:** que el esquema de base de datos tenga un único dueño y una historia reproducible.
- **Alcance:** Flyway operando en ambos servicios, convención de nombres de migración acordada, Hibernate fijado en `validate`, primera migración real aplicada.
- **Regla que establece:** las tablas las crea Flyway. Nunca `ddl-auto: update`, ni siquiera para salir del paso.
- **Dependencias:** HT-01.

---

### [HT-03] Tipos base del dominio

- **Épica/Módulo:** EP-11 — Habilitantes técnicas
- **Prioridad:** Must · **Sprint:** 0 · **Estimación inicial:** S
- **Actor:** Equipo de desarrollo
- **Objetivo:** fijar desde el primer día las decisiones de modelado que son caras de revertir.
- **Alcance:** `Identificador` (ULID generado en el origen), `Dinero` (montos sin decimales flotantes) y el manejo de errores como valor. En el módulo compartido, lo que viaja entre servicios.
- **Por qué tan temprano:** cambiar de identificadores autoincrementales a generados en el origen obliga después a tocar todas las tablas y todas las llaves foráneas del sistema. Es la decisión de mayor costo de reversión de todo el proyecto.
- **Dependencias:** HT-02.

---

### [HT-04] Patrón outbox

- **Épica/Módulo:** EP-11 — Habilitantes técnicas
- **Prioridad:** Must · **Sprint:** 10 · **Estimación inicial:** M
- **Actor:** Equipo de desarrollo
- **Objetivo:** que ningún evento se pierda aunque el bus o la conexión fallen.
- **Alcance:** tabla de outbox, escritura dentro de la misma transacción que el cambio de datos, y el proceso que la vacía hacia la cola.
- **Doble propósito:** es el mecanismo que comunica los dos servicios y, a la vez, el que sincroniza el equipo del local con la nube. Una sola pieza para las dos cosas.
- **Dependencias:** HT-01, HT-03.

---

### [HT-05] Generación de tipos desde OpenAPI

- **Épica/Módulo:** EP-11 — Habilitantes técnicas
- **Prioridad:** Must · **Sprint:** 2 · **Estimación inicial:** S
- **Actor:** Equipo de desarrollo
- **Objetivo:** que frontend y backend no puedan desincronizarse en silencio.
- **Alcance:** `springdoc-openapi` en el backend, generador en el frontend, y el comando que regenera la carpeta de tipos.
- **Por qué no se posterga:** si se deja para después, se acumulan decenas de interfaces escritas a mano que dejan de coincidir con el backend sin que nadie se entere hasta que algo falla en producción.
- **Dependencias:** HT-01, y el primer módulo con endpoints reales.

---

### [HT-06] Verificación automática de fronteras

- **Épica/Módulo:** EP-11 — Habilitantes técnicas
- **Prioridad:** Must · **Sprint:** 3 · **Estimación inicial:** S
- **Actor:** Equipo de desarrollo
- **Objetivo:** que la arquitectura se defienda sola en cada compilación.
- **Alcance:** la prueba de Spring Modulith que falla el build si un módulo invade a otro; en el frontend, la regla que impide importar desde adentro de otra funcionalidad.
- **Por qué importa:** sin esto, la arquitectura existe solo en el diagrama. A los tres meses alguien inyecta el repositorio de otro módulo "solo por esta vez" y ya nadie sabe quién depende de quién.
- **Dependencias:** al menos dos módulos construidos.

---

### [HT-07] Componentes base y diseño táctil

- **Épica/Módulo:** EP-11 — Habilitantes técnicas
- **Prioridad:** Must · **Sprint:** 2 · **Estimación inicial:** M
- **Actor:** Equipo de desarrollo
- **Objetivo:** que las pantallas del POS nazcan usables en tablet.
- **Alcance:** componentes compartidos —teclado numérico, botón, modal, formato de moneda— y las variables de diseño táctil: objetivo mínimo de 56 px, contraste, comportamiento en pantalla de celular.
- **Por qué antes de las pantallas:** el teclado numérico lo usan cuatro pantallas distintas —PIN del mozo, DNI y PIN del kiosco, cantidades del pedido y conteo de efectivo—. Hacerlo bien una vez es más barato que corregirlo en cuatro.
- **Dependencias:** HT-01.

---

### [HT-08] Observabilidad mínima

- **Épica/Módulo:** EP-11 — Habilitantes técnicas
- **Prioridad:** Should · **Sprint:** 3 · **Estimación inicial:** S
- **Actor:** Equipo de desarrollo
- **Objetivo:** poder responder "¿por qué esta venta no llegó a facturación?" sin adivinar.
- **Alcance:** identificador de correlación propagado en los logs y entre servicios, y formato de log consistente.
- **Por qué:** con dos servicios y colas de por medio, rastrear un problema sin correlación es impracticable.
- **Dependencias:** HT-01.

---

### [HT-09] Imágenes y perfiles de despliegue

- **Épica/Módulo:** EP-11 — Habilitantes técnicas
- **Prioridad:** Must · **Sprint:** 10 · **Estimación inicial:** M
- **Actor:** Equipo de desarrollo
- **Objetivo:** que el equipo del local arranque sin Internet, comprobado.
- **Alcance:** Dockerfile de cada servicio y del frontend, y los perfiles `local` y `nube` probados de verdad. En particular, que el perfil `local` levante sin Config Server ni descubrimiento de servicios.
- **Riesgo que previene:** un servicio configurado contra un servidor central de configuración no arranca cuando el restaurante está sin conexión. No funciona a medias: no arranca. Eso incumple RN-086 de forma directa.
- **Dependencias:** HT-01.

---

### [HT-10] Prueba con hardware real

- **Épica/Módulo:** EP-11 — Habilitantes técnicas
- **Prioridad:** Must · **Sprint:** 6 · **Estimación inicial:** S
- **Actor:** Equipo de desarrollo
- **Objetivo:** confirmar que la comanda se imprime en la comandera real, desde el equipo del local, con Internet desconectado.
- **Por qué es una historia y no una tarea:** puede invalidar decisiones. Si la impresión no funciona como se supone, cambia el diseño del agente de impresión y el modelo de hardware a comprar.
- **Riesgos y pendientes:** el modelo de impresora central sigue sin definirse (RF-012), y la prueba de impresión con hardware real está listada como validación externa pendiente (RF-016).
- **Dependencias:** US-030, y hardware disponible.


---

