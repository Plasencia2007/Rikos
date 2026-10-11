<!-- Extraído del Product Backlog v2.0 (RIKOS-product-backlog.md, 2026-09-11), líneas 3095-3156. Distribuido por paso del método el 2026-09-29, ver docs/METODO-spec-driven.md. -->

# 10. Preguntas de refinamiento y vacíos detectados

## 10.1 Clasificación de impacto

- **Bloqueante para desarrollo:** la historia no puede construirse ni probarse sin la decisión.
- **Importante antes de implementar:** la historia puede diseñarse, pero necesita la decisión antes de codificarse o certificarse.
- **Puede definirse durante el desarrollo:** admite un valor configurable o un default provisional.
- **Mejora futura:** no afecta la primera versión.

## 10.2 Tabla de refinamiento

| ID | Tema | Hallazgo o ambigüedad | Impacto | Pregunta concreta para negocio | Historias afectadas |
|---|---|---|---|---|---|
| RF-001 | Horarios de servicio | `07` declara pendientes los horarios exactos de desayuno, almuerzo y cena, pero `02` los usa para determinar el servicio automáticamente y para vencer el pre-ticket | Bloqueante para desarrollo | ¿Cuál es el rango horario exacto de desayuno, almuerzo y cena, y puede diferir entre locales? | US-007, US-019, US-020 |
| RF-002 | Precios y planes | `README` aclara que los importes son ejemplos y `07` deja pendientes precios definitivos, planes y vigencias | Importante antes de implementar | ¿Cuáles son los precios del menú normal y extra, los planes tarifarios iniciales, sus precios por producto fijo y desde cuándo rigen? | US-014, US-017, US-010, US-033 |
| RF-003 | Cargo de táper | `01` obliga a cobrar táper por cada menú en para llevar, recojo y delivery; `07` deja pendiente su precio | Bloqueante para desarrollo | ¿Cuál es el precio del táper por menú y se cobra también en los menús extra? | US-028, US-038, US-040, US-033 |
| RF-004 | Facturación electrónica | `07` deja pendientes los datos fiscales y el proveedor de facturación API | Bloqueante para desarrollo | ¿Qué proveedor de comprobantes se usará y cuáles son los datos fiscales de la empresa y de cada local? | US-037 |
| RF-005 | Motivos configurables | `04` y `03` exigen motivos en ajustes, anulaciones y rechazos; `07` deja pendiente la lista inicial | Puede definirse durante el desarrollo | ¿Qué motivos iniciales deben cargarse para ajuste de inventario, anulación de productos o ventas y rechazo de abonos? | US-012, US-031, US-034, US-045, US-024 |
| RF-006 | Bloqueo y recuperación de acceso | `05` exige bloqueo temporal por intentos fallidos; `07` deja pendientes el plazo y el método de recuperación | Importante antes de implementar | ¿Cuántos intentos fallidos activan el bloqueo, cuánto dura y cómo se recupera el acceso de un usuario o pensionista? | US-003, US-019, US-061, US-002 |
| RF-007 | Conteos físicos | `04` exige conteos totales periódicos; `07` deja pendiente la frecuencia | Puede definirse durante el desarrollo | ¿Con qué frecuencia se harán los conteos físicos totales y quién los autoriza? | US-047 |
| RF-008 | Cortesías y productos compartidos | `07` deja pendientes las reglas de cortesía y de productos compartidos, que afectan la asignación por persona y el cobro | Importante antes de implementar | ¿Cómo se registra una cortesía y cómo se reparte el costo de un producto compartido entre varias personas de la mesa? | US-028, US-029, US-034 |
| RF-009 | Campos adicionales | `07` deja pendientes los campos opcionales adicionales de clientes y proveedores | Puede definirse durante el desarrollo | ¿Qué campos adicionales deben registrarse para clientes y proveedores? | US-011, US-016 |
| RF-010 | Dimensionamiento | `07` deja pendientes la cantidad de locales iniciales, mesas, usuarios concurrentes y volumen pico | Importante antes de implementar | ¿Cuántos locales, mesas, usuarios concurrentes y transacciones pico debe soportar la primera versión? | US-001, US-005, US-027, y el diseño de EP-10 |
| RF-011 | Retención de datos | `07` deja pendiente la política de retención de respaldos y adjuntos; la auditoría y los PDF mensuales también se conservan indefinidamente según `01` y `05` | Importante antes de implementar | ¿Cuánto tiempo deben conservarse los respaldos, los adjuntos de pagos y compras, y los registros de auditoría? | US-060, US-050, US-023, US-006, US-025 |
| RF-012 | Hardware de impresión y kiosco | `07` deja pendientes el dispositivo e impresora del kiosco y el modelo de impresora central | Bloqueante para desarrollo | ¿Qué dispositivo e impresora se usarán en el kiosco de autoservicio y qué modelo de impresora central tendrá la cocina? | US-019, US-030 |
| RF-013 | Conflictos offline | `05` exige resolver conflictos sin perder transacciones; `07` reconoce que las reglas exactas entre operaciones offline de varios locales están pendientes | Bloqueante para desarrollo | ¿Qué criterio prevalece cuando dos locales registran offline movimientos incompatibles sobre la misma cuenta de pensionista o el mismo comprobante? | US-059, US-058, US-037, US-018 |
| RF-014 | Tratamiento tributario | `07` exige contrastar con el contador el tratamiento de ventas a crédito, abonos, notas y cierres mensuales | Bloqueante para desarrollo | ¿Cómo debe tratarse tributariamente la venta a crédito al pensionista, el abono, las notas y el cierre mensual? | US-018, US-021, US-025, US-035, US-037 |
| RF-015 | Contrato con el proveedor de comprobantes | `07` exige validar endpoints, estados, contingencia e idempotencia con el proveedor | Bloqueante para desarrollo | ¿Cuáles son los endpoints, estados, mecanismo de contingencia y clave de idempotencia del proveedor elegido? | US-037, US-059 |
| RF-016 | Pruebas con hardware real | `07` exige probar impresión y operación offline con el hardware real | Importante antes de implementar | ¿Cuándo y en qué local se harán las pruebas de impresión y de operación offline con el equipamiento definitivo? | US-030, US-058 |
| RF-017 | Ficha del cliente regular | "Cliente regular" es un actor en `01` y aparece en `06` asociado al Pedido, pero ningún documento define qué datos se registran de él | Importante antes de implementar | ¿Qué datos del cliente regular deben registrarse en un pedido de mesa, recojo y delivery? | US-028, US-038, US-040 |
| RF-018 | Pre-tickets simultáneos | `02` permite múltiples consumos del mismo servicio en un día, pero no aclara si pueden coexistir varios pre-tickets pendientes del mismo servicio para el mismo pensionista | Bloqueante para desarrollo | ¿Puede un pensionista emitir un segundo pre-ticket del mismo servicio mientras el primero sigue pendiente, o debe vincularse el anterior primero? | US-019, US-020, US-018 |
| RF-019 | Precio de productos fuera del plan | `02` define precios de plan solo para el almuerzo normal y los productos fijos de desayuno y cena; no define qué precio paga el pensionista por extras, postres sueltos u otros productos | Importante antes de implementar | ¿Qué precio se aplica al pensionista cuando consume un producto que su plan tarifario no contempla? | US-017, US-018, US-028, US-033 |
| RF-020 | Momento de pago en delivery | `03` exige pago anticipado del cliente regular solo en recojo programado; para delivery indica que caja cierra la venta tras la liquidación, sin definir cuándo paga el cliente | Importante antes de implementar | ¿El cliente regular de delivery paga por anticipado o contra entrega, y qué ocurre si un pedido resulta fallido? | US-040, US-042, US-033 |
| RF-021 | Turnos y cajas | `03` define caja por turnos pero no documenta cuántas cajas hay por local ni cómo se relaciona el turno con los horarios de servicio | Puede definirse durante el desarrollo | ¿Cuántas cajas opera cada local y cómo se delimitan los turnos respecto de los servicios? | US-032, US-036, US-054 |
| RF-022 | Autenticación local con credenciales nuevas | `05` exige autenticación disponible localmente y a la vez que las credenciales nunca se guarden legibles; no define qué ocurre con un usuario creado o con contraseña cambiada durante una caída | Importante antes de implementar | ¿Cómo debe habilitarse el acceso local de un usuario dado de alta o con credenciales cambiadas mientras el local está sin conexión? | US-002, US-058, US-061 |
| RF-023 | Alcance del consumo interno | `04` define una comida sin cobro por trabajador y turno, pero no qué platos incluye ni si aplica a motorizados u otro personal | Puede definirse durante el desarrollo | ¿Qué platos cubre el consumo interno y a qué personal alcanza? | US-051 |
| RF-024 | Confirmación de transferencias | `01` indica que la transferencia genera ajustes enlazados "al confirmarse", sin definir quién confirma ni si el local receptor debe aceptar | Importante antes de implementar | ¿Quién confirma una transferencia entre locales y el local receptor debe aceptarla antes de que impacte su stock? | US-046 |
| RF-025 | Estado "en servicio" del menú | `04` define tres estados de la programación semanal pero no la transición a "en servicio" | Puede definirse durante el desarrollo | ¿El paso de confirmado a en servicio es manual o automático según el horario del servicio? | US-013 |
| RF-026 | Origen de pedidos por WhatsApp | `03` indica que recojo y delivery se reciben por WhatsApp y se registran manualmente, mientras `README` excluye la integración automática | Puede definirse durante el desarrollo | ¿Se confirma que el registro de pedidos de WhatsApp será totalmente manual en la primera versión? | US-038, US-040 |
| RF-027 | Credenciales del portal | `02` exige contraseña del portal en la ficha del pensionista pero no documenta cómo se asigna, comunica ni recupera | Importante antes de implementar | ¿Quién asigna la contraseña inicial del portal, cómo se entrega al pensionista y su responsable, y cómo se recupera? | US-016, US-022, US-061 |
| RF-028 | Estado "bloqueada" de mesa | `03` lista el estado bloqueada entre los estados de mesa, sin regla que indique quién lo aplica ni por qué | Puede definirse durante el desarrollo | ¿En qué situaciones se bloquea una mesa y qué rol puede hacerlo y liberarla? | US-027 |

## 10.3 Contradicciones y duplicidades detectadas

| Hallazgo | Documentos implicados | Tratamiento aplicado en este backlog |
|---|---|---|
| El límite de crédito se describe como "preventivo, alerta pero permite", y a la vez la ficha lo llama "límite de crédito" | `02` § Cuenta corriente vs. § Ficha | No es contradicción real: se implementa como alerta no bloqueante (US-018 CA4), coherente con el caso crítico 4 de `07` |
| La inmutabilidad de ventas y cierres aparece en tres documentos con redacciones distintas | `01` § Reglas de historial, `06` § Reglas de integridad, `07` § Casos críticos 17 | Consolidado en RN-005, RN-006 y RN-073, referenciados desde US-033, US-035 y US-036 sin duplicar historias |
| El descuento de inventario por venta aparece en `03` (anulaciones), `04` (producción) y `07` (caso 11) | `03`, `04`, `07` | Consolidado en US-033 CA5 y US-048, con la anulación como caso propio en US-031 |
| El cargo de táper aparece en `01` (regla general) y en `03` (recojo) pero no se repite en delivery salvo por el canal | `01` § Canales, `03` § Recojo programado | Aplicado como regla transversal RN-009 a los tres canales no presenciales (US-028, US-038, US-040), tal como lo enuncia `01` |
| La exportación a Excel y PDF se enuncia una vez para todos los reportes | `05` § Reportes mínimos | No se creó una historia de exportación aislada; se incorporó como criterio de aceptación en cada historia de reporte |

---


## 10.4 Preguntas que bloquean el diseño de pantallas concretas

| Pregunta | Qué bloquea | Pantallas afectadas |
|

---

