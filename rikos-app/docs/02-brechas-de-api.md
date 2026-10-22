# 02 · Brechas de API para Rikos-App

> **Solo documentación.** No es un pedido al equipo de backend ni cambia prioridades: la web aún se está terminando y ya está en producción. Sirve para que, cuando llegue el momento de conectar la app, se sepa qué existe y qué falta.
>
> Fuente: colección Postman `docs/RIKOS-api.postman_collection.json` (182 operaciones, generada del OpenAPI vivo el 2026-10-14) y las decisiones técnicas DT-NNN. Lo marcado «no está» significa que **no lo encontré en esa colección**; puede existir bajo otro nombre o estar planeado.

## 1. Lo que ya existe y la app puede usar hoy

| Necesidad de la app | Endpoint | Observación |
|---|---|---|
| PIN del mozo | `POST /api/auth/pin` `{localCodigo, pin}` | 200 `SesionIniciada` (token, usuarioId, nombreUsuario, roles, locales[], localActivoId, permisos[]) · 401 · 423 (bloqueo, DT-039) |
| Login admin | `POST /api/auth/login` | |
| Cambiar de local | `POST /api/auth/cambiar-local` | |
| Permisos efectivos | `GET /api/auth/mis-permisos` | |
| Locales para autoservicio | `GET /api/locales/publico` | Solo `codigo` y `nombre` |
| Salud del servicio | `GET /api/salud` | Útil para el estado «sin conexión» |
| Mesas y salones | `GET /api/mesas`, `GET /api/salones`, `GET /api/salones/:id` | Unir/separar/liberar/bloquear mesas |
| Pedido de una mesa | `GET /api/mesas/:mesaId/pedido` | |
| Pedidos | `POST /api/pedidos`, líneas, comandas, anular, trasladar, mover mesa | `409 PEDIDO_MODIFICADO` (bloqueo optimista) cubre M29 |
| Resumen de pedidos | `GET /api/pedidos/resumen` | Base para «Mis pedidos» |
| Oferta del día | `GET /api/pedidos/:id/oferta` | |
| Cobro | `POST /api/pedidos/:id/cobros`, `POST /api/cobros/:id/reapertura` | Medios divididos y descuentos |
| Caja | `/api/cajas/:id/turnos`, `/api/turnos/actual/*`, abonos | |
| Auditoría por usuario | `GET /api/usuarios/:id/actividad` | Base para A08 |
| Imagen de producto | `PUT /api/productos/:id/imagen` | Hoy el canvas usa ilustraciones, no fotos |

## 2. Brechas (lo que el diseño asume y la colección no muestra)

| # | Brecha | Pantallas que la usan | Qué haría falta (idea, no pedido) | Alternativa mientras tanto |
|---|---|---|---|---|
| 1 | **Tiempo real (WebSocket/STOMP)**: plato listo, piden la cuenta, pedido tocado por otro equipo | M03d, M12b, M29 | Contrato de canal y de mensajes (DT-018/024) | Sondeo corto sobre `/api/pedidos/resumen` y `/api/mesas` |
| 2 | **Recojo y delivery** en `POST /api/pedidos` (hora, dirección, teléfono, costo de envío) | M24, M24a, M25 | Campos del canal en la creación | Solo salón y mostrador en la primera versión |
| 3 | **Estados de delivery y motorizado** | M25, M25b | Asignar motorizado, cambiar estado, liquidar (US-12 a US-14) | Posponer pantallas de delivery |
| 4 | **Idempotencia** en cobros y abonos para reintento offline | M27 | Id generado por el cliente aceptado como clave | Reintento solo manual con confirmación |
| 5 | **Aprobación de supervisor con PIN** (descuento, anulación) | M11c, M19 | Validar PIN de otro usuario en la misma operación | Pedir que el supervisor inicie sesión en el equipo |
| 6 | **Pasar líneas/pedido a otro mozo** | M13d | Operación explícita (hoy solo trasladar entre personas) | Ocultar la acción |
| 7 | **Resumen de turno por mozo** | M22b | Totales por mozo en el turno actual | Calcular en el cliente con `/api/pedidos/resumen` |
| 8 | **Desbloqueo de PIN desde admin** | M28, A09b | Endpoint de desbloqueo (DT-039 es por dispositivo) | Esperar el vencimiento del bloqueo |
| 9 | **Lista de mozos del local** para el selector de PIN | M02 | Lista pública mínima (nombre) por `localCodigo` | Teclado de PIN sin selector de nombre |
| 10 | **Consulta RUC/DNI** para factura | M16b | Proxy a un servicio de padrón | Ingreso manual |
| 11 | **Edición del menú desde admin** | A02c | Endpoints de menú del día desde el móvil | Solo lectura en la app |
| 12 | **Precuenta (impresión)** | M15c | Generar/imprimir la precuenta | Mostrar en pantalla sin imprimir |

## 3. Cosas a tener presentes (no son brechas)

- El POS vive en el nodo local y no se alcanza desde fuera (DT-029/030): la app solo opera dentro de la red del local; el modo remoto no aplica al mozo.
- Dinero en céntimos (enteros); IDs ULID de 26 caracteres; errores `{"error":"CODIGO"}` con 400/404/409/422.
- Los IDs de líneas, pedidos y comandas los **genera el cliente** (ya están en los cuerpos de `POST`), lo que facilita la idempotencia cuando se decida.

## 4. Cuándo revisar este documento

Cuando la web llegue a su siguiente corte, o antes de empezar el primer vertical de la app que cruce con un ítem de la tabla 2 (el primero será el #9 y el #1 al llegar a Mesas).
