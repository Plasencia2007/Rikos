# US-019, US-020, US-021, US-024 — Circuito integral del pensionista (sprint 8)

**Especificado en:** `docs/producto/plan/sprint-08-circuito-del-pensionista.md`, `docs/producto/ui-ux/04-kiosco-pensionista.md` y `docs/pendientes/04-kiosco-pensionista-2026-10-12.md`. Épica EP-04 (Pensionistas). Implementado el 2026-10-12 (commits `5b06fa3` y `6b4663d`), backend y frontend.

## Qué se pidió
Cerrar el circuito presencial y digital del pensionista en el restaurante:
1. **US-019 (Pre-ticket de autoservicio en tótem/kiosco):** El estudiante llega al terminal `/kiosco`, ingresa su PIN numérico único de 6 dígitos, el sistema resuelve su servicio actual automáticamente según la hora del local (desayuno, almuerzo, cena) y genera un pre-ticket con código numérico (ej. `381920`) **sin generar consumo contable**.
2. **US-020 (Vincular pre-ticket al pedido):** El mozo en el salón ingresa el código del pre-ticket en la pantalla del pedido del POS, asociando al pensionista con su plan activo y tarifa aplicable.
3. **US-021 (Abonos en caja presencial):** El cajero registra ingresos de saldo a la cuenta corriente del pensionista con recibo y método de pago (efectivo, tarjeta, transferencia).
4. **US-024 (Aprobación y rechazo de abonos digitales):** Bandeja administrativa central (ADM-07) para validar comprobantes enviados por estudiantes (Yape/Plin/Transferencia), cotejar contra cuenta bancaria, aprobar (acreditación automática en cuenta corriente) o rechazar con motivo formal obligatorio.

## Qué se construyó
- **Migraciones Flyway V47 a V50:**
  - V47: Tabla `pre_ticket` (id, código `\d{6}`, `pensionista_id`, `local_id`, `servicio`, `estado`, `emitido_en`, `vence_en`).
  - V48: Auditoría y trazabilidad de vinculación a pedidos.
  - V49: Motivos formales de rechazo de abonos digitales (`abono_digital`, `motivo_rechazo_abono`).
  - V50: `huella_pin` con hash criptográfico único e índice para búsqueda por PIN único sin requerir DNI, más tabla de tasa de solicitudes para protección de fuerza bruta.
- **Backend Operación (`pensionistas` y `venta`):**
  - Casos de uso: `EmitirPreTicket`, `ConsultarContextoKiosco`, `LimitarSolicitudesKiosco`, `GeneradorDePin` (PIN único), `VincularPreTicket`, `RegistrarAbonoCaja`, `AprobarAbonoDigital`, `RechazarAbonoDigital`.
  - Seguridad y Rate Limiting: Endpoint `/kiosco/**` público con protección de 5 intentos fallidos consecutivos por pensionista (bloqueo temporal 15 min) y limitador por IP/solicitud.
  - Resolución horaria automática: `ResolverServicioVigente` determina si el local está en horario de atención y qué servicio corresponde sin intervención manual.
- **Frontend Kiosco (`rikos-web/src/apps/kiosco`):**
  - `PantallaKiosco.tsx` rediseñada por completo (fondos orgánicos de marca, reloj de servicio, tarjeta de ticket térmico emergente, temporizador de autocierre a los 15 s).
  - Solo pide PIN (6 dígitos) en teclado numérico accesible de 56 px; no expone DNI ni requiere selector de comida.
  - Selector de local persistente en cabecera cargado desde catálogo de locales autorizados (`/locales/publicos`).
- **Frontend Administración (`PantallaAbonosPendientes.tsx`, `DialogoCotejoAbono.tsx`):**
  - Bandeja ADM-07 con estados de abono (pendiente, aprobado, rechazado), visor de imagen del voucher bancario y confirmación con un clic.

## Respuestas al feedback del Kiosco (Sprint 8 review)
1. **DNI ya no se solicita:** El pensionista solo digita su PIN de 6 dígitos. La unicidad atómica se garantizó mediante `huella_pin` en base de datos.
2. **Diseño de pantalla:** Se reemplazó el fondo plano por una composición moderna con diseño de marca, fotografía de plato decorativo y jerarquía clara.
3. **Local configurable/seleccionable:** Se eliminó el campo de texto libre. Ahora el local se selecciona desde el catálogo activo y se fija en el terminal.
4. **Comida automática por horario:** Se eliminó cualquier selector manual en modo público; el servicio y su vigencia provienen estrictamente de la hora real del local configurada en el sistema.

## Cómo se verificó
- Tests unitarios en `EmitirPreTicketTest`, `VincularPreTicketTest` y `AbonoDigitalTest`.
- Verificación en navegador y Playwright para el flujo completo: kiosco emite pre-ticket -> mozo lo vincula en el pedido -> caja cobra con cargo al pensionista -> administración valida abonos digitales.
