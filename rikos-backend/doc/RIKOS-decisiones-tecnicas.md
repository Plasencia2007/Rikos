# Decisiones técnicas y hallazgos de integración

Documento complementario al `claude/08-product-backlog-inicial.md`
Estado: vivo — se actualiza conforme se cierran pendientes
Última actualización: 25 de septiembre de 2026 (tercera pasada)
Origen: sesiones de trabajo sobre arquitectura, facturación electrónica y ejecución del Sprint 0

---

## 0. Cómo leer este documento

La especificación funcional (los 8 documentos `.md` originales) es agnóstica de tecnología por decisión explícita (RN-093). Este documento registra las decisiones técnicas que se han ido tomando **fuera** de esa especificación, para que no se pierdan entre sesiones y para que quien retome el proyecto no vuelva a discutirlas desde cero.

Cada decisión lleva un estado:

- **Decidido** — acordado, se puede construir sobre esto.
- **Recomendado** — propuesta del análisis, todavía sin confirmación del equipo o del propietario.
- **Pendiente** — falta información de un tercero para poder decidir.

Los hallazgos sobre la API de Factivo llevan además **Confirmado** o **No confirmado**, según hayan sido verificados contra código real o no. Esta distinción importa: varias decisiones de arquitectura dependen de datos que todavía no están verificados.

---

## 1. Decisiones tomadas

### DT-001 — Stack: TypeScript full-stack
**Estado:** Superada parcialmente por DT-012 — el backend pasa a Java con Spring Boot. El frontend sigue en TypeScript.

El equipo construirá con JavaScript/TypeScript en toda la pila. La orientación de referencia es React + Vite como PWA para el POS, NestJS + PostgreSQL en el servidor, monorepo con un paquete de dominio compartido donde vivan las reglas que no pueden duplicarse: cálculo de la cuenta corriente del pensionista, aplicación del saldo a favor, costeo de recetas con subrecetas y resolución de permisos efectivos.

**Motivo:** es el stack con el que el equipo tiene experiencia real, y permite que la misma lógica de dominio corra en el nodo del local y en la nube sin reimplementarla.

---

### DT-002 — Despliegue: nodo por local más nube
**Estado:** Recomendado — no decidido

Cada local con un equipo pequeño en su red (base de datos, API y agente de impresión), sincronizando contra un servidor central en la nube. Los dispositivos del POS se conectan al nodo local por la red del local.

**Motivo:** RN-086 exige que el *local* opere sin Internet, no que cada dispositivo lo haga. Con offline solo en el navegador, dos mozos sin conexión no se ven entre sí y se rompe el caso crítico 7 (mesa mixta con varios pensionistas). Además RN-062 obliga a tener un proceso en la red del local para la impresora de cocina: si ya hay una máquina ahí, poner la base de datos encima cuesta poco y resuelve el offline de verdad.

**Alternativas evaluadas:** solo nube con IndexedDB (más barato, no cumple RN-086 en su espíritu); todo on-premise (cumple offline pero encarece el respaldo en tiempo real y la consolidación multilocal).

**Qué falta:** decisión formal del equipo. Conviene resolverla antes de escribir código, porque cambia la forma del proyecto. Depende también de RF-010 (cantidad de locales, mesas, usuarios concurrentes y volumen pico), que sigue sin definirse.

---

### DT-003 — Proveedor de facturación electrónica: Factivo
**Estado:** Decidido

Se usará Factivo como proveedor. Entrega XML, PDF y CDR ya resueltos; se accede por URL propia de la cuenta más un token de API.

**Consecuencia:** salen del alcance la firma digital, el manejo del certificado, la construcción del UBL, la comunicación con SUNAT y el almacenamiento del CDR.

**Cierra parcialmente RF-004.** Siguen pendientes los datos fiscales de la empresa: RUC, razón social y domicilio fiscal por local.

---

### DT-004 — La emisión del comprobante es siempre asíncrona
**Estado:** Decidido

La venta cierra en caja y encola el comprobante. **No hay dos caminos**: no existe "si hay Internet emito directo, si no encolo". Siempre encola. Con conexión el worker lo procesa en segundos y el usuario no nota diferencia.

**Motivo:** un solo camino es la mitad del código y la mitad de los casos de prueba, y elimina la clase de errores donde el reintento duplica documentos. Además es exactamente lo que ya pedía RN-075 y lo que ya estaba redactado en US-037 CA3.

**Consecuencia:** el cierre de la venta, el cargo al pensionista (RN-015), la impresión de la comanda (RN-062) y el descuento de inventario nunca dependen de que el proveedor esté disponible.

---

### DT-005 — Durante un corte se entrega ticket o nota de venta
**Estado:** Decidido

De los cinco tipos que ofrece Factivo, solo boleta, factura y nota de crédito se transmiten a SUNAT. Nota de venta y ticket son documentos internos que no pasan por la API.

El flujo de mostrador es:

| Cliente | Documento | Datos que se piden en caja |
|---|---|---|
| Cliente regular que no pide nada | Ticket o nota de venta | Ninguno |
| Cliente regular que pide boleta | Boleta | DNI |
| Cliente empresa | Factura | RUC, razón social, dirección |
| Pensionista | Boleta con su DNI | Ninguno: salen de su ficha |
| Corrección tras cierre de turno | Nota de crédito | Motivo |

**Consecuencia importante y contraintuitiva:** como Factivo asigna el correlativo (ver H-01), **durante un corte de Internet no es posible imprimir una boleta con su número**. El cliente que la pida recibe el ticket interno y la boleta le llega después. Esto conviene dejarlo advertido, porque cualquiera que lea la especificación asumiría lo contrario.

**Pendiente de validación tributaria:** si el restaurante puede entregar ticket o nota de venta como documento por defecto y emitir boleta solo a pedido. Es parte de RF-014, para el contador.

---

### DT-006 — El correo del comprobante lo envía nuestro sistema
**Estado:** Decidido

Cuando el comprobante pasa a Aceptado, nuestro sistema descarga el PDF y el XML desde Factivo y envía el correo al cliente desde nuestro propio dominio.

**Condiciones:**

- El disparador es **Aceptado**, no Registrado: recién ahí hay CDR y el documento es válido. Enviar en Registrado expone a mandar algo que SUNAT después rechaza.
- El envío debe ocurrir **exactamente una vez**. Con reconsulta periódica activa, el duplicado clásico es que el mismo comprobante se procese dos veces: se controla con una marca `correo_enviado_at` y una restricción única, verificada en la misma transacción que dispara el envío.
- Hay que usar un proveedor de correo transaccional con dominio verificado y SPF, DKIM y DMARC configurados. Correo con adjuntos desde un dominio nuevo cae en spam con facilidad.
- Para el cliente regular el correo es **opcional y a pedido**: no se pide en cada venta, solo cuando el cliente dice que quiere el comprobante.

---

### DT-007 — Los pensionistas no reciben correo por consumo
**Estado:** Recomendado — requiere confirmación del propietario

Los comprobantes del pensionista viven en el portal, que ya es requisito (RN-028 le permite consultar comprobantes), y en su estado de cuenta mensual (US-025). No se le envía correo por cada consumo.

**Motivo:** RN-029 dice explícitamente que las notificaciones internas son solo por abono aprobado o rechazado y por estado mensual generado, y que no hay notificación por cada consumo. Un correo con la boleta en cada consumo es, en la práctica, una notificación por consumo. Además el volumen lo desaconseja: con 100 pensionistas y tres servicios diarios serían hasta 9,000 correos mensuales no solicitados, que es el patrón que hace que un dominio termine marcado como spam.

**Si el propietario quiere lo contrario**, debe ser una preferencia en la ficha del pensionista, apagada por defecto, y hay que modificar RN-029.

---

### DT-008 — Las anulaciones se hacen con nota de crédito
**Estado:** Decidido — impuesto por la API

Factivo no expone comunicación de baja por API (H-08). Toda corrección de un comprobante ya emitido se hace emitiendo una nota de crédito.

**No cuesta nada:** RN-074 ya exigía que después del cierre de turno las correcciones se hicieran mediante operación compensatoria o documento relacionado, sin alterar el cierre original.

**Matiz útil:** si una venta se reabre mientras su comprobante sigue en estado Pendiente en la cola (todavía no despachado), no hace falta nota de crédito — basta cancelar el elemento encolado y volver a encolar el corregido. La nota de crédito solo es necesaria si el documento ya se envió.

---

### DT-009 — Emisión serializada por serie con reconciliación
**Estado:** Decidido — es la respuesta a la ausencia de idempotencia

Factivo no acepta clave de idempotencia ni referencia externa nuestra, y su endpoint de consulta solo filtra por rango de fechas (H-02, H-03). Eso deja abierto el escenario: enviamos, Factivo crea el documento, la respuesta se pierde por timeout, y no sabemos si se emitió.

El mecanismo acordado:

1. Un único worker despacha comprobantes de una serie a la vez. Nunca dos en vuelo simultáneos de la misma serie.
2. Antes del envío se registra el intento en nuestra base con estado `en_vuelo` y la huella del documento: fecha, hora, cliente, total e items.
3. Si la respuesta se pierde, **no se reintenta a ciegas**. Se consulta `GET /api/documents/lists/{hoy}/{hoy}` y se compara contra lo registrado. Como solo había un documento en vuelo para esa serie, el que aparezca de más es el nuestro: se adopta su identificador y su número.
4. Recién entonces continúa el siguiente elemento de la cola.

La serialización no cuesta rendimiento porque la emisión ya es asíncrona: nadie espera en el mostrador.

**Si la prueba P-02 sale bien** (que Factivo acepte y devuelva un campo propio), este mecanismo se simplifica mucho: nuestro identificador viajaría dentro del documento y la reconciliación dejaría de depender de la huella.

---

### DT-010 — Guardamos nuestra propia copia de PDF, XML y CDR
**Estado:** Decidido

Al pasar a Aceptado se descargan los tres archivos y se almacenan en nuestro propio almacenamiento de objetos. El portal y los reenvíos se sirven desde ahí, no desde los enlaces de Factivo.

**Motivo:** RN-089 exige respaldo, RN-085 exige conservar, y no conviene que la disponibilidad de comprobantes de años anteriores dependa de que esa cuenta siga activa. Además los enlaces de descarga de Factivo no requieren token (H-06): la URL misma es la credencial, así que exponerlos en el portal equivaldría a publicar el comprobante. Descargar y servir desde nuestro lado, con permisos, es lo coherente con RN-092.

---

## 2. Hallazgos sobre la API de Factivo

Fuente: análisis del código de un sistema en producción que ya integra Factivo, realizado en septiembre de 2026. Lo marcado como **No confirmado** no pudo verificarse contra código o documentación real y debe validarse antes de construir sobre ello.

### H-01 — Numeración: la asigna Factivo · Confirmado

El emisor manda la serie pero no el número: se envía `"numero_documento": "#"` literal y Factivo devuelve el correlativo asignado en `data.number`, con formato `"BB01-1"`.

**Consecuencia doble.** Por un lado, no es posible imprimir una boleta numerada sin conexión (ver DT-005). Por otro, y esto es una buena noticia, **desaparece el riesgo de que dos locales sin conexión emitan el mismo correlativo**: el número se asigna cuando el documento llega al proveedor, no cuando ocurre la venta. Series separadas por local pasan a ser una conveniencia, no un requisito de corrección.

### H-02 — No hay clave de idempotencia · Confirmado

El payload no admite `external_reference`, `idempotency_key` ni equivalente. Existe un código de respuesta `2800 "El documento ya existe"`, pero **no se sabe con qué criterio Factivo decide que algo ya existe** — es el riesgo descrito en la sección 3.

### H-03 — Consulta solo por rango de fechas · Confirmado

`GET /api/documents/lists/{desde}/{hasta}` devuelve un arreglo donde cada elemento trae el identificador asignado por Factivo, el número, el estado y los enlaces de descarga. No hay filtro por una referencia propia del emisor: el filtrado se hace del lado del cliente.

### H-04 — Estados observados · Confirmado (catálogo posiblemente incompleto)

- `01` Registrado
- `05` Aceptado
- `09` Rechazado

No se confirmó la existencia de estados "observado" o "anulado" en este mismo campo.

### H-05 — No hay webhooks · Confirmado

La integración de referencia usa reconsulta periódica cada quince minutos sobre el listado por rango de fechas.

**Dato operativo relevante:** con el interruptor de "Envío automático" desactivado, Factivo procesa mediante un resumen nocturno entre las 22:20 y las 23:10. Con el interruptor activado, SUNAT responde en el mismo request y el documento llega a Aceptado en segundos. **Para este proyecto hay que activarlo**: de lo contrario el cliente que compró al mediodía recibiría su boleta pasadas las diez de la noche, lo que hace inviable el envío por correo de DT-006.

### H-06 — Descarga de archivos · Confirmado

La respuesta de creación y cada elemento del listado traen enlaces directos a XML, PDF y CDR. Se descargan sin token adicional: la URL misma actúa como credencial. El enlace de CDR entrega un ZIP de SUNAT, no un PDF. **No confirmado** si esos enlaces caducan.

### H-07 — Tipos de documento que pasan por la API · Confirmado

Solo boleta (`03`), factura (`01`) y nota de crédito (`07`). Nota de venta y ticket no llegan al proveedor: son documentos internos y no generan XML ni CDR.

La nota de crédito requiere el bloque `documento_afectado` con serie, número y tipo del comprobante original, más `codigo_tipo_nota` del catálogo 09 de SUNAT (`01` anulación de la operación, `02` error en el RUC, y demás).

### H-08 — No hay comunicación de baja · Confirmado

No existe endpoint confirmado para comunicaciones de baja ante SUNAT. Las anulaciones se resuelven con nota de crédito (DT-008).

### H-09 — Token · Confirmado parcialmente

Un token por empresa. No hay concepto de sucursal en ese campo. **No confirmado:** si caduca, si hay que rotarlo, y si se pueden tener varios tokens bajo la misma cuenta.

### H-10 — Errores observados · Parcialmente confirmado

- `401` — token inválido (confirmado).
- `422` — payload inválido, con mensaje descriptivo (confirmado).
- `500` — error del servidor. Se observó al menos un caso por omitir `fecha_de_vencimiento`, así que ese campo debe enviarse siempre.
- `2800` — "El documento ya existe" (visto en pruebas, criterio desconocido).
- Los códigos de respuesta de SUNAT siguen la convención habitual (0–1999 aceptado, 2000–3999 rechazado, 4000+ aceptado con observaciones), pero eso es inferencia de la convención estándar, no documentación de Factivo.

**No confirmado:** límite de peticiones por minuto, catálogo completo de errores, plazo máximo de envío desde la fecha de emisión, envío automático de correo al cliente y cómo desactivarlo, personalización de la plantilla del PDF, y creación de series por API.

---

## 3. Diseño resultante del módulo de facturación

### 3.1 Dos estados separados

Conviene modelar dos ciclos de vida independientes en lugar de uno solo:

**Estado fiscal:** `Pendiente → Enviado → Registrado → Aceptado | Rechazado`

**Estado de entrega:** `No aplica | Pendiente → Enviado → Rebotado`

Así un comprobante rechazado no entra al circuito de entrega, y un reenvío de correo no altera el estado fiscal.

### 3.2 Clasificación de errores en la cola

Este es el punto donde conviene ser más cuidadoso que la integración de referencia, que trata todo 4xx y 5xx por igual:

| Respuesta | Categoría | Comportamiento de la cola |
|---|---|---|
| Timeout o error de conexión | Reintentable con reconciliación previa | Consulta el listado del día; solo reintenta si el documento no aparece |
| HTTP 500 | Igual que el anterior | Un 500 puede llegar después de que el documento ya fue creado |
| HTTP 422 | No reintentable | A la bandeja de revisión: reintentar no arregla un payload inválido |
| HTTP 401 | No reintentable, con alerta | Pausa la cola y avisa al administrador: el token está mal |
| Código 2800 | Nunca reintentar | Dispara reconciliación por listado |
| Estado 09 Rechazado | Terminal | A la bandeja. La venta ya está cobrada: se resuelve administrativamente |

### 3.3 Riesgo específico de este negocio

El criterio con el que Factivo determina que un documento "ya existe" es desconocido, y ahí hay un riesgo que no aparece en otros rubros.

Si el proveedor deduplica por contenido, en un restaurante eso se convierte en un problema real: dos menús de S/12.00, boleta a "CLIENTE VARIOS", mismo minuto, mismos items, son dos ventas legítimas y distintas que ocurren constantemente. Si la segunda es rechazada como duplicado, se queda una venta real sin comprobante.

Por eso la prueba P-01 no debe limitarse a "¿duplica?", sino a "¿qué considera duplicado?".

### 3.4 Bandeja de comprobantes

Requisito operativo, no técnico: debe existir una pantalla con los comprobantes pendientes, rechazados y con correo rebotado, con reintento automático y opción de reintento manual. El riesgo de este diseño no es técnico sino humano — que el reenvío dependa de que alguien se acuerde.

---

## 4. Pendientes

| ID | Pendiente | Responsable | Bloquea |
|---|---|---|---|
| P-01 | Probar en la cuenta demo qué considera Factivo un duplicado: enviar dos veces el mismo payload, y además dos boletas legítimamente idénticas separadas por segundos, confirmando que ambas se crean | Equipo técnico | DT-009 y el diseño de la cola |
| P-02 | Probar si Factivo acepta y devuelve un campo propio en el payload (por ejemplo `observaciones` con un identificador nuestro). Si lo acepta, hay idempotencia real y DT-009 se simplifica | Equipo técnico | Optimización de DT-009 |
| P-03 | Confirmar si se pueden crear varias series bajo la misma cuenta, para tener una por local | Equipo técnico / soporte Factivo | Conveniencia, no corrección |
| P-04 | Datos fiscales de la empresa: RUC, razón social y domicilio fiscal por local | Propietario | Cierre de RF-004 y de US-037 |
| P-05 | Consulta al contador (RF-014): tratamiento de ventas a crédito al pensionista, abonos, notas de crédito, cierres mensuales, entrega electrónica del comprobante, y si se puede usar ticket o nota de venta como documento por defecto | Propietario y contador | US-018, US-021, US-025, US-035, US-037 |
| P-06 | Confirmar con el propietario que los pensionistas no reciben correo por consumo (DT-007) | Propietario | Redacción final de RN-029 |
| P-07 | Decidir formalmente el modelo de despliegue (DT-002). Toda la sección 7 lo asume ya adoptado, pero falta la confirmación explícita | Equipo técnico | Arranque del desarrollo |
| P-08 | Activar el interruptor de envío automático en la cuenta de Factivo | Equipo técnico | Viabilidad de DT-006 |
| P-09 | Verificar la versión vigente de Spring Boot y de Java LTS al arrancar el proyecto | Equipo técnico | DT-012 |
| P-10 | Confirmar la memoria del equipo del local antes de comprarlo: con servicios en JVM hacen falta 8 GB, no 4 | Equipo técnico | DT-012, DT-020 |
| P-11 | Confirmar con el propietario si acepta el nodo por local, o si prefiere la alternativa sin nodo con las limitaciones de DT-019 | Propietario | DT-002, DT-011 |
| P-12 | Confirmar con el propietario la compra del equipo del local y su UPS, y si acepta que la boleta se emita al restablecerse la conexión durante un corte | Propietario | DT-005, DT-031 |
| P-13 | Probar la pantalla de toma de pedido con asignación por persona con un mozo real, antes de programarla | Equipo técnico | US-028 |
| ~~P-14~~ | **Cerrado el 17/09/2026.** Verificado: el ULID lleva marca de tiempo en los primeros 48 bits y ordena cronológicamente como texto. Los índices de recálculo incremental del modelo son válidos | — | — |
| P-15 | Definir la política de redondeo de `Dinero`: qué ocurre con las fracciones de céntimo en el costo por porción de recetas con subrecetas, en el prorrateo de una cuenta dividida y en el cálculo del IGV. Decidir si el costo necesita más precisión que el precio | Equipo técnico, con validación del contador en la parte del IGV | DT-035, US-010, US-034, US-037 |
| ~~P-16~~ | **Cerrado el 17/09/2026.** El diagnóstico se movió a `apps/diagnostico/` y `PantallaPin` quedó como la pantalla de bloqueo real | — | — |
| P-18 | Confirmar con el propietario el umbral y la duración del bloqueo, tanto de contraseña como de PIN. Valores provisionales adoptados: 5 intentos, 15 minutos | Propietario | RF-006, US-002, US-003 |
| P-19 | Revisar el bloqueo del PIN si cambia el diseño de esa pantalla. Si algún día el mozo eligiera su nombre antes de teclear el PIN, el contador podría volver al usuario y `bloqueo_pin` sobraría | Equipo técnico | DT-039 |
| P-20 | Confirmar con el propietario los campos agregados en V12: enlaces de redes sociales, logo del local y tiempos mínimo y máximo de entrega. Los tres estaban descartados por la especificación y se agregaron durante el sprint 1 ampliado | Propietario | DT-046 |
| P-21 | Definir dónde se mostrarán los enlaces de redes y el logo. Hoy se guardan y no los consume ninguna pantalla | Propietario | DT-046 |
| P-22 | Reemplazar `ResponseEntity<?>` por el tipo concreto en los 47 métodos del backend donde aparece. El comodín hace invisible el DTO para la especificación OpenAPI, y con él la generación de tipos del frontend pierde valor a medida que crecen los endpoints | Equipo técnico | HT-05 |
| P-23 | Decidir cómo se carga el catálogo real de productos. Hoy hay **un** producto de prueba, y la importación masiva desde Excel está excluida del alcance (`README`). Cargar a mano el catálogo completo de un restaurante es una jornada de trabajo | Propietario y equipo | US-008, sprint 4 |
| ~~P-25~~ | **Cerrado el 25/09/2026.** `esquema.d.ts` regenerado contra el backend real reiniciado; los cuatro campos parchados a mano ahora vienen del esquema | — | — |
| P-26 | **Comprobar si `/error` está permitido en la configuración de seguridad.** La investigación del 25/09 no encontró ningún código que convierta una excepción en 403, y tiene razón. Pero el 403 se observó dos veces y está documentado. La explicación más probable es otra: cuando algo revienta, Spring **reenvía la petición a `/error`**, y si esa ruta no está en `permitAll`, la cadena de seguridad bloquea ese reenvío y el cliente ve un 403 en lugar del 500 real. El manejador global lo tapa para lo que ocurre dentro de un controlador, pero no para lo que falle en un filtro | Equipo técnico | DT-049, DT-052 |
| P-30 | **`GET /productos` devuelve el costo a cualquiera con `catalogo.ver`** (cocina, cajero, mozo). US-010 CA6 oculta los costos de las recetas a quien no tiene `recetas.ver_costos`, pero el costo de cada insumo sigue a la vista en el catálogo, y con eso el costo de la receta se reconstruye a mano. Filtrarlo con `inventario.ver_costos` en `ProductoController` | Equipo técnico | US-010, RN-012 |
| P-31 | **Cambiar la unidad base de un producto que ya está en recetas.** `ActualizarProducto` lo permite, y desde ese momento las cantidades guardadas en las recetas quedan en una unidad que puede no tener conversión: el costo se calcularía mal (sin conversión el ingrediente no suma). Bloquearlo o exigir conversión antes de aceptar el cambio | Equipo técnico | DT-054 |
| P-32 | **Desactivar recetas.** El permiso `recetas.desactivar` existe pero no hay caso de uso: desactivar una subreceta deja sin costo a todas las recetas que la usan, y hay que decidir si se bloquea o se avisa | Equipo técnico | US-010 |
| P-29 | **Revisar que un despliegue nuevo al perfil `nube` pueda arrancar.** `V9` crea el usuario semilla sin condición y `GuardaSemillaDesarrollo` detiene el arranque si lo encuentra: en un entorno nuevo las dos cosas ocurren en el mismo arranque, así que el servicio no levantaría nunca | Equipo técnico | DT-053 |
| P-27 | Confirmar con el propietario los umbrales de margen que colorean la lista de productos (hoy 55 % y 35 %). Son una decisión de diseño, no una regla del negocio, y un margen marcado en rojo que el restaurante considera normal genera desconfianza en el sistema | Propietario | US-008 |
| P-28 | Agregar la comprobación de historial en `EliminarProveedor` cuando exista la tabla de compras. Hoy no hay nada que lo referencie, así que el borrado siempre procede | Equipo técnico | DT-051, sprint 9 |
| P-24 | Implementar o retirar los endpoints de desactivar y activar categoría. `ConfiguracionSeguridad` declara los matchers pero el controlador no expone los endpoints: es configuración muerta | Equipo técnico | US-008 |
| P-17 | Comprobar que el puerto de Postgres se lee de una variable de entorno y no está escrito en el código, para que el remapeo al 5433 no se arrastre al despliegue | Equipo técnico | DT-036 |

---

## 5. Cambios pendientes en el backlog

Las decisiones de este documento todavía no están reflejadas en `claude/08-product-backlog-inicial.md`. Mientras no se apliquen, el backlog y este documento discrepan en los puntos siguientes, y quien lea solo el backlog construiría cosas distintas de las acordadas.

| Elemento del backlog | Cambio necesario | Origen |
|---|---|---|
| US-037 Emitir comprobantes electrónicos | Reemplazar los criterios genéricos por los estados reales (01, 05, 09), la emisión serializada por serie, la reconciliación por listado de fecha, la clasificación de errores y el almacenamiento propio de PDF, XML y CDR | DT-009, DT-010, H-01 a H-10 |
| US-037 | Añadir que la anulación se hace por nota de crédito y que no existe comunicación de baja | DT-008, H-08 |
| US-033 Cobrar la cuenta | Añadir criterio: selector de tipo de documento al cerrar, con valor por defecto según el tipo de cliente | DT-005 |
| Historia nueva en EP-06 | Crear una historia de entrega del comprobante por correo, con disparo al aceptar, adjuntos, reintento, rebote y reenvío manual desde caja | DT-006 |
| RF-004 | Marcar como cerrado en cuanto al proveedor; dejar abierto solo por los datos fiscales | DT-003, P-04 |
| RF-015 | Cerrar en su mayor parte con los hallazgos de la sección 2; dejar abierto solo lo listado como no confirmado | Sección 2 |
| RF-017 Ficha del cliente regular | Cerrar parcialmente: DNI para boleta, RUC más razón social y dirección para factura, correo opcional | DT-005, DT-006 |
| RN-029 | Registrar el conflicto: el envío del comprobante por correo al pensionista sería una notificación por consumo, que la regla prohíbe. Pendiente de decisión del propietario | DT-007, P-06 |
| Nota general en US-037 | Dejar advertido que durante un corte de Internet no puede imprimirse una boleta numerada, porque es lo contrario de lo que asumiría cualquier lector de la especificación | DT-005, H-01 |

Ninguno de estos cambios es urgente mientras el módulo no se retome. Cuando se retome, conviene aplicarlos antes de empezar a construir.

---

## 6. Qué se puede postergar y qué no

Pregunta frecuente al leer DT-002: si el despliegue con nodo por local se puede dejar para el final. La respuesta corta es que **la infraestructura se posterga, el modelo de datos no**.

### 6.1 Lo que se deja para el final sin ningún riesgo

- Comprar el equipo del local, el UPS y la impresora de red.
- Instalar el sistema operativo, Docker y los contenedores.
- Certificados, direcciones IP fijas y configuración del router.
- Construir el sincronizador entre el nodo y la nube.
- El despliegue físico completo en cada local.

Nada de esto hace falta para empezar a programar. Elegir el modelo de despliegue **no obliga a comprar hardware ahora**.

### 6.2 Las cinco decisiones que hay que tomar desde el primer día

Todas se implementan en poco tiempo al inicio y todas son caras de agregar después. Son la diferencia entre que el modo sin conexión sea un detalle de despliegue o una reescritura.

| # | Decisión | Por qué no se puede postergar |
|---|---|---|
| 1 | **Los identificadores se generan en el origen** (ULID o UUIDv7), nunca autoincrementales de la base de datos | Cambiarlo después obliga a tocar todas las tablas y todas las llaves foráneas del sistema. Es la más costosa de las cinco si se omite |
| 2 | **Cada registro transaccional lleva su local** (`local_id` en ventas, pedidos, movimientos de caja e inventario) | Agregar la columna después es trivial; rellenarla correctamente hacia atrás, no |
| 3 | **La cuenta del pensionista es un libro de movimientos**, no un campo de saldo mutable. El saldo es una proyección | Con un campo `saldo` que se actualiza, cambiar después implica reescribir toda la lógica contable y ya se perdió el historial que exige el estado de cuenta mensual (US-025) |
| 4 | **El inventario también es un libro de movimientos** con origen rastreable, y el stock es la suma | Ya lo exige RN-057, así que no es una decisión nueva, pero conviene implementarlo desde el inicio y no como añadido |
| 5 | **El snapshot histórico se escribe al cerrar la venta**: precio, costo, receta aplicada, utilidad y margen | Si no se guarda en ese momento, es imposible reconstruirlo después, porque precios y recetas ya cambiaron. Lo exigen RN-005 y RN-046 |

Las decisiones 3 y 4 tienen además un beneficio que se explica en DT-009: con libros append-only, dos locales sin conexión que registran movimientos sobre la misma cuenta no generan conflicto de escritura — los movimientos simplemente se ordenan al sincronizar.

### 6.3 Por qué esto no complica el desarrollo

El equipo del local corre exactamente la misma aplicación que la nube, con distinta configuración. Mientras se desarrolla en una laptop, esa laptop **es** un nodo: no se está simulando nada. El día del despliegue se copia la configuración y se arranca; no hay reescritura.

El sincronizador es la última pieza en construirse y puede quedar para los sprints finales.

### 6.4 Señal de alarma

Si a los dos meses de desarrollo el sistema tiene identificadores autoincrementales, un campo de saldo que se actualiza a mano, y todo el código asume una sola base de datos siempre disponible, entonces el modo sin conexión dejó de ser algo que se agrega al final y pasó a ser una reescritura. Es el modo habitual en que este tipo de proyecto se atasca.

---

## 7. Arquitectura técnica: estilo, stack y despliegue

Bloque de decisiones tomadas después de la sesión inicial. Todas son **Decididas** salvo donde se indique.

### DT-011 — Estilo arquitectónico: monolito modular más un microservicio

El backend se divide en dos servicios desplegables de forma independiente:

- **`servicio-operacion`** — monolito modular con toda la operación: identidad, catálogo y recetas, menú, pensionistas, venta y POS, caja, inventario y producción, delivery.
- **`servicio-facturacion`** — el microservicio: emisión a Factivo, cola con reintentos, reconciliación, descarga de PDF, XML y CDR, y envío de correos. Con su propia base de datos.

**El criterio de corte**, que es lo que hay que saber defender: *solo se separa lo que no necesita correr en el local sin Internet*. Todo lo que debe funcionar durante un corte se queda junto, porque además comparte transacción; lo que vive solo en la nube puede salir sin costo.

**Por qué facturación y no otro módulo:** es asíncrono por naturaleza, depende de un tercero, no participa de la transacción del cierre, y RN-088 autoriza explícitamente que la API fiscal quede indisponible sin afectar la operación del local.

**Qué no separar:** pensionistas e inventario participan de la misma transacción que el cobro, y separarlos exigiría sagas y compensaciones. Delivery tampoco, porque RN-086 obliga a que funcione sin Internet y tendría que bajar al nodo del local.

**Segundo candidato**, si hiciera falta un tercer servicio: reportes y exportaciones, por el mismo criterio — vive solo en la nube y no participa de ninguna transacción.

---

### DT-012 — Backend en Java 21 con Spring Boot 3.x

**Sustituye a DT-001 en la parte del backend.** El frontend sigue en TypeScript.

**Motivo declarado por el equipo:** el gateway. **Advertencia registrada:** ese argumento por sí solo es débil, porque un gateway se resuelve con configuración en cualquier stack. Las razones que sí sostienen la elección al sustentar son:

- **Spring Modulith** verifica automáticamente que un módulo no invada a otro, lo que convierte la arquitectura limpia en una regla comprobada por el build y no en una intención.
- **`@Transactional`** maduro para el cierre de venta, que debe ser atómico.
- **JPA con `@Version`** para bloqueo optimista en los datos maestros sincronizados.
- Alineación con lo que el curso enseña y evalúa.

**Costo asumido:** cada servicio de Spring Boot consume entre 300 y 500 MB en reposo, frente a 100-150 MB de un servicio en Node. El equipo del local necesita 8 GB de memoria cómodos, no 4. Y los tipos entre frontend y backend ya no se comparten: hay que generarlos desde la especificación OpenAPI.

**Pendiente:** verificar la versión vigente de Spring Boot y de Java LTS al momento de arrancar.

---

### DT-013 — Estructura: Maven multi-módulo

```
rikos-backend/                    pom padre
├── pom.xml
├── docker-compose.yml
├── contratos/                    módulo compartido: eventos y DTOs
├── gateway/                      Spring Cloud Gateway
├── servicio-operacion/           Spring Boot + Modulith
└── servicio-facturacion/         Spring Boot
```

Paquetes: `pe.rikos.operacion`, `pe.rikos.facturacion`, `pe.rikos.contratos`.

Los módulos de Modulith dentro de `servicio-operacion` son los contextos del negocio, y corresponden con las épicas del backlog: `identidad`, `catalogo`, `menu`, `pensionistas`, `venta`, `caja`, `inventario`, `delivery`.

El módulo `contratos` resuelve el problema de que los esquemas de eventos se dupliquen entre servicios: ambos lo declaran como dependencia y no hay nada que publicar.

El frontend queda fuera de este árbol, en su propio proyecto.

---

### DT-014 — Gateway como única puerta de entrada

Spring Cloud Gateway en el 8080, enrutando por prefijo de ruta:

```
/api/**            →  servicio-operacion   (:8081)
/facturacion/**    →  servicio-facturacion (:8082)
```

Los puertos de los servicios quedan hacia adentro; nadie de afuera los conoce.

**El beneficio concreto para este proyecto:** el frontend tiene una sola variable de configuración, la dirección base. Apuntando al nodo del local es el POS de ese local; apuntando a la nube es el portal. El código del frontend es idéntico en ambos casos.

El gateway enruta y termina HTTPS. No lleva lógica de negocio ni decisiones de permisos: eso se queda en `servicio-operacion`.

---

### DT-015 — Perfiles `local` y `nube` — crítico para el modo sin conexión

Esta es la decisión que no se puede olvidar al desplegar.

| | Perfil `nube` | Perfil `local` (el mini PC del restaurante) |
|---|---|---|
| Gateway | Sí | Sí |
| `servicio-operacion` | Sí, en modo central | Sí |
| `servicio-facturacion` | Sí | **No** |
| RabbitMQ | Sí | No |
| Eureka | Opcional | **No** |
| Config Server | Opcional | **No** |
| Configuración | Centralizada | Variables de entorno |

**El motivo, y es un riesgo real:** un servicio de Spring Boot configurado contra un Config Server va a buscar su configuración al arrancar. Si el Config Server está en la nube y el restaurante está sin Internet, el servicio **no levanta** — no funciona a medias, no arranca. Eso viola RN-086 de forma directa.

Eureka tiene el mismo problema por otro lado, y además resuelve un problema que en el nodo no existe: allí hay una sola instancia de cada cosa, en una dirección fija de Docker.

Si aun así se quiere usar Config Server, debe ir con `fail-fast` desactivado y una copia local de respaldo.

---

### DT-016 — Mensajería: RabbitMQ en la nube, outbox en el local

**RabbitMQ**, no Kafka. Kafka es desproporcionado para dos servicios y tres locales, y en el equipo del local sería inviable. RabbitMQ además corre **solo en la nube**: el nodo del restaurante no lo necesita.

En el nodo, el encargo se guarda en la tabla `outbox` de Postgres, escrita en la misma transacción que la venta. El sincronizador la vacía cuando hay Internet. Esa misma pieza sirve para los dos propósitos: comunicar servicios y sincronizar con el central.

Reintentos con Spring Retry y cola de fallidos de RabbitMQ. Reconciliación periódica de comprobantes con `@Scheduled`.

---

### DT-017 — Comunicación entre servicios: dos colas, por eventos

```
Cierre de venta en el local
   └─► outbox (misma transacción)
         └─► sincronizador, cuando hay Internet
               └─► cola  facturacion.solicitudes
                     └─► servicio-facturacion
                           · emite a Factivo (serializado por serie)
                           · consulta estado, descarga PDF/XML/CDR
                           · envía el correo
                           └─► cola  facturacion.resultados
                                 └─► servicio-operacion actualiza la venta
```

Dos colas, una en cada sentido. Nada de llamadas HTTP directas entre servicios para este flujo: el nodo puede estar sin Internet y necesita dejar el encargo escrito.

`servicio-facturacion` expone además una API pequeña, detrás del gateway, para la bandeja de comprobantes del administrador: consultar estado, reintentar y reenviar el correo.

---

### DT-018 — Tiempo real dentro del local con WebSockets

El nodo local notifica por WebSocket a los dispositivos del local cuando cambia el estado compartido: mesas, pedidos y comandas.

Cuando un mozo agrega un plato, la tablet del otro mozo y la pantalla de caja se actualizan solas. Funciona aunque no haya Internet, porque la red del local sigue viva y el nodo está adentro.

---

### DT-019 — Dónde vive el modo sin conexión

Registrado porque la duda va a volver a aparecer.

La recomendación habitual para aplicaciones móviles es que el modo sin conexión viva **en el cliente**: base local en el dispositivo, cola de operaciones, sincronización al reconectar. Esa recomendación es correcta cuando **cada cliente trabaja sobre sus propios datos**.

**En este sistema no aplica como única solución**, por dos motivos:

1. **RN-062** exige una única impresora central de cocina. Un navegador no imprime en una comandera ESC/POS: tiene que haber un proceso en la red del local. Es decir, ya hay backend dentro del restaurante.
2. **El estado es compartido entre dispositivos.** Mesas, comandas y turno de caja son de todos a la vez. Con el modo sin conexión solo en el cliente, el mozo A toma el pedido en su tablet, el mozo B agrega platos desde su celular sin ver lo del primero, y el cajero no ve nada de los dos. Eso no es un conflicto que se resuelva con *last write wins* ni con CRDTs: la información nunca llegó al dispositivo que la necesitaba. Es el caso crítico 7 del documento de aceptación.

**La solución es en dos capas:** el nodo del local resuelve que el *local* opere sin Internet; un buffer pequeño en el cliente resuelve que el *dispositivo* aguante microcortes de WiFi o un reinicio del nodo.

**Simplificación que se gana:** como la cuenta del pensionista y el inventario son libros de movimientos que solo agregan, dos locales que registran a la vez no producen conflicto — los movimientos se ordenan al sincronizar. Y los datos maestros tienen un único escritor, el central, así que *last write wins* alcanza. No hacen falta CRDTs ni vector clocks.

**Alternativa honesta:** si el propietario acepta que durante un corte cada dispositivo trabaje aislado, y que las mesas compartidas y el cierre en caja no funcionen hasta que vuelva la conexión, se puede prescindir del nodo local. Es más barato y más simple, pero contradice RN-086 tal como está escrito. Sería una decisión de negocio, no técnica, y debe quedar registrada como tal.

---

### DT-020 — Todo en contenedores

Docker y Docker Compose para desarrollo y para el despliegue en el nodo del local.

**Cinco tropiezos conocidos**, anotados para no perder tiempo la primera vez:

1. `localhost` dentro de un contenedor es el contenedor mismo. Se usa el nombre del servicio: `postgres:5432`, `rabbitmq:5672`, `servicio-facturacion:8082`.
2. `depends_on` no espera a que la base esté lista, solo a que el contenedor arranque. Hace falta `healthcheck` más `condition: service_healthy`.
3. No copiar dependencias compiladas desde la máquina de desarrollo al contenedor.
4. Para desarrollo hacen falta volúmenes montados, si no cada cambio obliga a reconstruir la imagen.
5. Si los servicios comparten el módulo `contratos`, el contexto de construcción de cada imagen debe ser la raíz del proyecto, no la subcarpeta del servicio.

---

### 7.1 Qué corre dónde

```
        NODO LOCAL (mini PC)                      NUBE
   ┌────────────────────────────┐        ┌──────────────────────────────┐
   │  gateway                   │        │  gateway                     │
   │  servicio-operacion        │◄──────►│  servicio-operacion          │
   │  postgres                  │  sync  │  servicio-facturacion        │
   │  agente de impresión       │        │  postgres · rabbitmq         │
   │  agente de sincronización  │        │  almacenamiento de archivos  │
   └────────────────────────────┘        └──────────────────────────────┘
```

El microservicio de facturación **no baja al local**. El equipo del restaurante corre lo mínimo para vender.

---

### 7.2 Primer paso recomendado

No construir los módulos vacíos. Levantar primero el esqueleto que camina: Docker Compose con Postgres y RabbitMQ, el gateway enrutando, los dos servicios respondiendo un endpoint de salud, y un evento real viajando de uno al otro por la cola.

Eso resuelve de una vez la red, el enrutamiento, la base, la cola y la comunicación entre servicios. Lo demás es rellenar con lógica de negocio.

Después, una rebanada vertical completa —autenticación más catálogo de productos, del navegador a la base— antes de abrir el resto de los módulos.

---

## 8. Frontend, accesos y red del local

### DT-021 — Un solo proyecto de frontend con las cinco superficies como rutas

**Corrige una versión anterior de esta decisión**, que separaba el frontend en dos proyectos. Esa separación existía solo porque se preveían dos frameworks distintos; al unificar en uno (DT-022), pierde sentido.

El proyecto es `rikos-web` y contiene las cinco superficies como rutas: POS, kiosco, portal, vista del motorizado y panel del administrador.

**Siguen siendo dos despliegues**, pero eso depende de dónde corren, no de cómo se organiza el código: se compila una vez y la misma carpeta de archivos estáticos se copia al equipo del local (que sirve POS y kiosco) y al servidor en la nube (que sirve portal, motorizado y administración).

**Condición obligatoria: carga perezosa por ruta.** Sin ella, la tablet del mozo descarga también el panel del administrador y el portal, en un dispositivo que suele ser modesto. Se configura desde el inicio.

**Costo aceptado:** el código del panel administrativo también queda en el servidor del local, así que escribir `/admin` en una tablet muestra su interfaz. No es un hueco de seguridad —los permisos los valida el backend y no se vería ningún dato—, pero es visible. Si más adelante molesta, se resuelve compilando dos versiones desde el mismo proyecto con distinto conjunto de rutas; es configuración, no división del código.

---

### DT-022 — React con TypeScript sobre Vite, compilado a estático

Un solo framework para todo el frontend: React y TypeScript, empaquetados con Vite. El resultado son archivos estáticos servidos por nginx, tanto en el equipo del local como en la nube.

**Se descarta el renderizado en servidor dentro del restaurante.** No por consumo de memoria —un proceso de Node consume poco—, sino por cuatro razones operativas:

- Es un proceso más que puede morirse, y si muere el POS no carga. Los archivos estáticos servidos por nginx no se caen ni hay que reiniciarlos.
- Tras un corte de luz, nginx sirve el POS apenas prende la máquina; un servidor de Node tarda en levantar.
- En el local no hay nadie de sistemas: cada proceso extra es algo que alguien tendrá que diagnosticar por teléfono un viernes de noche.
- No aporta nada aquí. El renderizado en servidor sirve para buscadores y para la primera carga de páginas públicas; el POS es una aplicación con login, usada por pocas personas, en los mismos dispositivos, con los archivos ya cacheados.

**Aclaración frecuente:** Node se necesita para *compilar*, no para *servir*. En las máquinas de desarrollo se ejecuta la compilación; lo que se despliega son archivos planos.

Next.js seguiría siendo viable si el equipo lo prefiere, siempre que se use en modo de exportación estática. Se eligió Vite por ser más directo para una aplicación con login que no necesita renderizado en servidor.

El POS se instala como aplicación en la pantalla de inicio de las tablets: abre sin barra de navegador, arranca rápido y los archivos quedan cacheados.

---

### DT-023 — Los tipos del frontend se generan desde OpenAPI

Como el backend pasó a Java, frontend y backend ya no comparten código. Los tipos y el cliente se generan automáticamente desde la especificación OpenAPI que expone Spring con springdoc.

**Configurarlo desde el primer día.** Si se posterga, se acumulan interfaces escritas a mano que dejan de coincidir con el backend y nadie se entera hasta que algo falla en producción.

La carpeta con el código generado no se edita nunca a mano.

---

### DT-024 — Estado, tiempo real y buffer local

- **TanStack Query** para todo lo que viene del backend: caché, reintentos y revalidación.
- **WebSocket con STOMP** contra el equipo del local para el estado compartido — mesas, pedidos y comandas. Funciona sin Internet porque la red del local sigue viva (ver DT-018).
- **Buffer local con Dexie**, acotado a microcortes: que si el WiFi parpadea o el equipo se reinicia, el mozo siga tecleando y se reenvíe solo.

El buffer **no** es una réplica del sistema. El modo sin conexión del *local* lo da el equipo del local; el del *dispositivo*, este buffer. Confundirlos lleva a construir dos veces el mismo sistema.

---

### DT-025 — Organización por funcionalidad, con frontera explícita

Las carpetas del frontend se organizan por funcionalidad, no por tipo de archivo, y coinciden con los módulos del backend: `autenticacion`, `mesas`, `pedido`, `comanda`, `caja`, `pensionistas`, `kiosco`, `inventario`, `delivery`.

Cada una expone un `index.ts` y **ninguna importa desde las tripas de otra**. Es la misma disciplina que Spring Modulith aplica en el backend, ahora en la capa de presentación.

Tres carpetas transversales concentran lo que no debe dispersarse: `comun/sesion` (usuario activo, permisos, cambio por PIN), `comun/offline` (todo el buffer, en un solo lugar) y `comun/tiempo-real`.

---

### DT-026 — Navegación del panel administrativo

Adaptada del sistema de referencia que revisó el equipo, ajustada al alcance real de RIKO'S:

```
OPERACIÓN
  Inicio
  Caja y turnos
  Delivery y recojo

NEGOCIO
  Pensionistas          ← cuentas, abonos, planes, estados mensuales
  Menú semanal
  Catálogo y recetas
  Inventario
  Producción
  Compras y proveedores
  Facturación           ← bandeja de comprobantes

ADMINISTRACIÓN
  Reportes
  Usuarios y permisos
  Locales
  Configuración
```

**Se quitaron** Marketing, Reclutamiento y Personal: `README` excluye fidelización, y RN-014 excluye asistencia, horarios, planilla y remuneraciones.

**Se reemplazó** "Clientes" por "Pensionistas": el cliente regular casi no tiene ficha, el pensionista es el centro del sistema.

**Se agregaron** Menú semanal, Producción, Caja y turnos, y Reportes, que no existían en la referencia y sí en el backlog.

El encabezado muestra siempre el local activo, con selector para cambiar.

---

### DT-027 — El POS no vive dentro del panel administrativo

En el sistema de referencia, el POS es una opción del menú lateral. Acá no puede serlo: el panel se sirve desde la nube y el POS desde el equipo del local, en direcciones distintas.

Además, por diseño: el POS va a pantalla completa, sin barra lateral. El mozo no navega menús, opera sobre mesas.

---

### DT-028 — La pantalla de PIN es una pantalla de bloqueo, no un inicio de sesión

RN-011 pide PIN individual para cambio rápido en dispositivos compartidos. Eso implica que esa pantalla aparece **cada vez que un mozo suelta la tablet y otro la toma**, no solo al empezar el turno.

Por lo tanto:

- Botón de "cambiar de mozo" siempre visible en el POS.
- Retorno automático a la pantalla de PIN por inactividad.
- **Sin perder nada**: el pedido pertenece a la mesa, no al dispositivo ni al mozo. Si el mozo A tenía abierta la mesa 5 y entra el mozo B, la mesa 5 sigue igual; solo cambia a quién se atribuyen las acciones siguientes.

Si esto no se diseña así, los mozos van a compartir un mismo PIN para no perder el pedido, y se cae toda la trazabilidad que exige RN-004.

Decisiones de la pantalla: PIN de cuatro dígitos (la especificación solo dice "PIN corto"), ingreso automático al cuarto dígito sin botón de confirmar, estado visible de bloqueo por intentos fallidos (RN-091), y teclado en la mitad inferior en pantallas de celular, alcanzable con el pulgar.

El kiosco reutiliza el mismo componente de teclado numérico, con flujo propio: DNI y luego PIN.

---

### DT-029 — Esquema de direcciones y aislamiento de orígenes

```
sanisidro.rikos.pe            →  POS            ← equipo del local, IP privada
sanisidro.rikos.pe/kiosco     →  kiosco

app.rikos.pe/admin            →  administrador  ┐ autenticación interna,
app.rikos.pe/motorizado       →  motorizado     ┘ misma sesión

portal.rikos.pe               →  pensionista y responsable  ← autenticación aparte
```

Un subdominio por local, con el nombre del local y no un número: el mozo ve en la barra del navegador dónde está trabajando. El POS va en la raíz de ese subdominio.

**El portal se separa en otro dominio a propósito.** Hay dos sistemas de autenticación distintos: usuarios internos con usuario y contraseña (RN-011, RN-012), y pensionista o responsable con DNI y contraseña de portal, acceso compartido y solo lectura (RN-028). Al estar en orígenes distintos, las sesiones quedan aisladas por el navegador y una falla en el portal —la parte más expuesta, con decenas de estudiantes entrando desde sus celulares— no puede alcanzar al panel administrativo. Va en la línea de RN-092.

Sigue siendo **un solo proyecto** de frontend desplegado una vez, respondiendo a los dos dominios. Un certificado comodín para `*.rikos.pe` cubre todos los subdominios, incluidos los de cada local.

---

### DT-030 — El POS no es accesible desde fuera del local

La dirección del POS resuelve a una IP privada dentro del restaurante. Desde fuera no se llega, ni con datos móviles ni desde otra red.

**Es una decisión, no una limitación.** Nadie puede entrar al POS desde internet: ni un mozo desde su casa ni un desconocido desde cualquier parte.

Quien necesita acceso remoto lo tiene por la nube: el administrador ve ventas, saldos y reportes desde donde sea; el motorizado usa su vista en la calle con datos móviles; el pensionista y su responsable entran al portal.

La regla: **lo que se hace dentro del local se hace desde el local; lo que se consulta desde afuera vive en la nube.**

Única excepción prevista, para soporte técnico: una VPN hacia la red del local. **Nunca** exponer el equipo del local directamente a internet.

---

### DT-031 — Requisitos de red y energía del local

Lista de instalación, porque cada punto es una causa real de fallo:

**Energía.** El UPS alimenta el router, el equipo del local y el switch. Si se corta la luz y el router se apaga, las tablets se quedan sin red aunque el equipo siga encendido con batería. La impresora también, si alcanza la capacidad.

**Resolución de nombres sin Internet.** El router debe resolver localmente el nombre del local hacia la IP del equipo. Si el dispositivo tuviera que preguntarle a un servidor de Internet dónde queda ese nombre, durante un corte no podría — aunque el equipo esté a veinte metros. Respaldo: acceder por la IP directa.

**Direcciones fijas.** Reserva de IP en el router para el equipo del local y para la impresora de cocina.

**Datos móviles desactivados en los dispositivos.** Android e iOS traen activada la opción de cambiar a datos móviles cuando el WiFi "no tiene Internet". Durante un corte, el dispositivo se pasa solo a la red móvil, pierde el acceso a la red del local y el POS deja de funcionar justo cuando más se necesita. Hay que desactivarlo en cada dispositivo; idealmente las tablets no llevan chip.

**Cobertura WiFi en todo el salón.** Una zona muerta al fondo genera quejas que parecen del sistema y son de la red. En locales grandes, un punto de acceso adicional.

**Configuración de las tablets.** Orientación fija, suspensión desactivada, POS instalado en la pantalla de inicio, kiosco en modo pantalla completa sin barra de navegador.

---

### 8.1 Cómo explicarlo a alguien no técnico

Registrado porque la pregunta se repite y conviene responderla siempre igual.

**La confusión habitual** es creer que el sistema tiene un "modo de emergencia" que se activa cuando se cae Internet. Técnicamente conviene que no lo sea: un camino que solo se usa durante los cortes es el menos probado del sistema y falla justo el viernes de noche. Si el sistema siempre funciona igual —contra el equipo del local—, el corte de Internet no es un caso especial, y se está probando solo todos los días.

**La distinción clave: WiFi no es lo mismo que Internet.** El router hace dos trabajos: crear la red interna del local y conectarla a Internet. Cuando "se cae Internet" falla el segundo; el primero sigue funcionando, y los aparatos del local se siguen viendo entre ellos.

**Por qué el navegador funciona sin Internet:** la dirección del POS no apunta a un servidor de Internet, apunta al equipo del restaurante. Es el mismo mecanismo de escribir `192.168.1.1` en el navegador y entrar a la configuración del router: una página web, sin Internet, servida por un aparato que está a tres metros.

**Explicación breve para enviar por escrito:**

> Funciona siempre igual, con o sin Internet. El sistema vive dentro del local, en un equipo conectado al mismo router, así que vender no depende de Internet. Las tablets se conectan a ese equipo por el WiFi del local, no salen a Internet para tomar un pedido. Internet solo sirve para respaldar en la nube, enviar las boletas a SUNAT y que los pensionistas vean su cuenta desde su celular. Si se cae, todo eso queda en cola y sale solo apenas vuelve la conexión.

**La única excepción que hay que mencionar siempre:** durante un corte no se puede emitir la boleta electrónica en el momento, porque el número lo asigna el proveedor de facturación. Se entrega el ticket y la boleta sale al restablecerse la conexión.

**Comparación que suele funcionar:** un cajero automático sigue operando aunque se caiga el enlace con el banco, y después se pone al día solo.

---

## 9. Convenciones establecidas en el Sprint 0

Sprint 0 ejecutado el 16 de septiembre de 2026. Los tres habilitantes (HT-01, HT-02, HT-03) quedaron completos: el reactor compila, el gateway enruta, los tres servicios responden su endpoint de salud, Flyway aplica y valida el esquema, y el frontend muestra en pantalla la respuesta del backend. No se tocó ninguna funcionalidad de negocio.

Las decisiones que siguen se tomaron durante esa ejecución y se registran aquí porque el resto del proyecto se apoya en ellas.

---

### DT-032 — Wrapper de Maven en la raíz del backend
**Estado:** Decidido

El wrapper existía en los tres servicios pero incompleto (faltaba `.mvn/wrapper/maven-wrapper.properties`), y la raíz de `rikos-backend` no tenía wrapper propio pese a que el `README.md` ya asumía `../mvnw`. Se agregaron `mvnw`, `mvnw.cmd` y las propiedades, fijando **Maven 3.9.9**.

**Regla:** el reactor se compila desde la raíz con el wrapper, no con el Maven instalado en la máquina de cada quien. Es lo que evita que el build dependa de la versión que cada integrante tenga puesta.

---

### DT-033 — Convención de nombres de las migraciones
**Estado:** Decidido

`V<n>__<descripcion_en_snake_case>.sql`, documentado en `ESTRUCTURA.md`.

**Regla que no admite excepción:** una migración ya aplicada **nunca se edita**. Si hay que corregir algo, se agrega una migración nueva. Editar una aplicada rompe la suma de verificación de Flyway y deja la base de un compañero en un estado que ya no se puede reproducir.

Se confirmó que `ddl-auto: validate` y Flyway ya estaban correctamente configurados en ambos servicios con base de datos propia.

---

### DT-034 — La tabla `event_publication` de Spring Modulith se versiona con Flyway
**Estado:** Decidido

`spring-modulith-starter-jpa` junto con `spring-modulith-events-amqp` exige su propia tabla `event_publication` para el registro de eventos publicados, y esa tabla no estaba migrada. Se agregó como `V2__event_publication.sql` en ambos servicios.

**Cómo se obtuvo el DDL, y por qué importa:** con el volcado offline de Hibernate (`schema-generation.scripts.action=create` más `database.action=none`), **nunca** con `ddl-auto: update` contra la base real. Es el procedimiento a repetir cada vez que haga falta el DDL de una tabla que genera un framework: se obtiene el script, se revisa, se guarda como migración.

Esta tabla es la que sostiene el patrón outbox de DT-016, así que no es un detalle de infraestructura: es el cimiento del modo sin conexión.

---

### DT-035 — Tipos base del dominio en `contratos`
**Estado:** Decidido

Tres tipos, en Java puro y sin dependencias externas, construidos con pruebas primero. 22 pruebas en verde.

| Tipo | Forma | Decisión que fija |
|---|---|---|
| `Identificador` | ULID de 26 caracteres en Crockford Base32, generado con `SecureRandom`, ordenable como texto | Los identificadores se generan en el origen, nunca son autoincrementales de la base. Es la decisión número 1 de la sección 6.2 y la más cara de revertir del proyecto |
| `Dinero` | `record` sobre céntimos en `long` | No hay punto flotante en ningún importe del sistema |
| `Resultado<T, E>` | `sealed interface` con `Exito` y `Fracaso`, pensado para `switch` de patrones de Java 21 | Los errores esperados de la capa de aplicación son valores de retorno, no excepciones |

El módulo `contratos` no tenía ni JUnit; se agregaron `junit-bom` y `maven-surefire-plugin` con versión fijada.

---

### DT-036 — Postgres en el puerto 5433 en las máquinas de desarrollo
**Estado:** Decidido — solo para desarrollo

Un Postgres nativo de otro proyecto ya escuchaba en el 5432 de Windows, y las conexiones externas (la aplicación Java y `psql` del host) caían ahí en lugar de en el contenedor `rikos-postgres`. **El síntoma era engañoso: `password authentication failed`**, que hace pensar en credenciales cuando el problema es de puerto.

Se remapeó el contenedor a `5433:5432` en vez de tocar la instalación existente. Documentado en `docker-compose.yml` y en `ESTRUCTURA.md`.

**Alcance de la decisión:** es una conveniencia de las máquinas de desarrollo. En el nodo del local y en la nube, donde no hay otro Postgres compitiendo, el puerto vuelve a ser el estándar. Conviene que el puerto salga de una variable de entorno y no esté escrito en el código, para que ese cambio no requiera tocar nada.

---

### DT-037 — El endpoint de salud devuelve contenido verificable
**Estado:** Decidido

Los tres servicios responden en `/salud`. No basta con un 200 vacío: `servicio-operacion` y `servicio-facturacion` informan además `perfilActivo` y `baseDeDatos` (conectada o desconectada, probando la conexión real, no asumiéndola). El gateway, que no tenía ninguno, ahora expone el suyo.

**Por qué se registra:** un endpoint de salud que solo devuelve 200 responde que sí incluso cuando la base está caída, que es justo el caso en que hace falta que responda que no. En el nodo del local, donde no hay nadie de sistemas, esta pantalla es la primera herramienta de diagnóstico.

---

### 9.1 Deuda declarada del Sprint 0

Tres cosas quedaron pendientes de verificar o de deshacer. Se anotan para que no se vuelvan permanentes por olvido.

**La pantalla de PIN se usó como pantalla de diagnóstico.** Para mostrar la respuesta del backend se extendió `PantallaPin.tsx` y se conectó `apps/pos/index.tsx` a ella. Es andamiaje del Sprint 0: DT-028 define esa pantalla como pantalla de bloqueo, no como tablero de estado. Cuando se implemente el ingreso por PIN real (US-003, sprint 1), el diagnóstico debe salir de ahí y vivir en una ruta propia.

**Falta confirmar la estructura interna del ULID.** Un ULID correcto son 48 bits de marca de tiempo seguidos de 80 bits aleatorios; es esa primera mitad la que lo hace ordenable cronológicamente como texto. Si la implementación fuera aleatoria de punta a punta, el orden alfabético no coincidiría con el orden temporal, y la sincronización del outbox y el orden de los movimientos de los libros de cuenta e inventario dependen de esa propiedad. Ver P-14.

**Falta la política de redondeo de `Dinero`.** Guardar céntimos en `long` resuelve el punto flotante, pero no dice qué hacer cuando una operación produce fracciones de céntimo, y en este sistema eso ocurre a diario: el costo por porción de una receta con subrecetas, el prorrateo de una cuenta dividida entre varias personas, y el cálculo del IGV. Ver P-15.

---

## 10. Convenciones establecidas en el Sprint 1

Sprint 1 ejecutado el 17 de septiembre de 2026. Objetivo cumplido: login real, cambio rápido de mozo por PIN y local activo visible. 34 pruebas en verde. Ocho migraciones aplicadas, Hibernate validando sin tocar el esquema.

---

### DT-038 — Token JWT como portador de la sesión
**Estado:** Decidido

El login devuelve un token con el usuario, sus roles y sus locales asignados. Se agregó `jjwt` como única dependencia nueva del sprint.

**Consecuencia para el sprint 3:** cuando entre US-004, el token debe llevar los **permisos efectivos** resueltos —rol más excepciones— y no los roles. Resolverlos en cada petición sería recalcular lo mismo miles de veces al día; llevar solo el rol obligaría a consultar la base para cada decisión de autorización.

**Consecuencia para el modo sin conexión:** el token se firma y valida en el nodo del local con su propia clave. No puede depender de un servicio de identidad en la nube, o el local no podría autenticar durante un corte (RN-086).

---

### DT-039 — El bloqueo del PIN se cuenta por dispositivo y se persiste
**Estado:** Decidido — **implementado en V17** (25/09/2026)

El modelo ligaba `usuario.intentos_fallidos` y `usuario.bloqueado_hasta` tanto al ingreso por contraseña como al del PIN. **No funciona para el PIN**, y el motivo es de diseño de pantalla: el PIN se teclea a ciegas, cuatro dígitos sin elegir usuario antes (DT-028). Un intento fallido no dice qué usuario falló, así que no hay fila de `usuario` a la cual sumarle el intento.

El sprint 1 lo resolvió con un contador en memoria por local. Se corrige en dos puntos:

**Por dispositivo, no por local.** Con bloqueo por local, un mozo que olvidó su PIN deja inservible la terminal de al lado durante quince minutos, en plena hora de almuerzo. El ataque que RN-091 previene —probar PIN de cuatro dígitos a mano— no se vuelve viable porque haya tres tablets en el salón.

**Persistido, no en memoria.** Un bloqueo que se borra reiniciando el proceso no es un bloqueo. Y el nodo del local se reinicia solo cada vez que vuelve la luz después de un corte, que en este negocio no es un caso raro.

Se agregó la tabla `bloqueo_pin` al modelo de datos (sección 3.7 ter).

---

### DT-040 — El POS se identifica por el código del local, no por su ULID
**Estado:** Decidido

`AutenticarConPin` recibe `sanisidro`, no el identificador del local.

**Motivo:** el POS ya conoce su código de despliegue, porque está en su propio subdominio (DT-029). Pedirle el ULID primero sería una dependencia circular: necesitaría consultar para saber quién es antes de poder autenticar.

**Consecuencia:** `local.codigo` deja de ser una conveniencia de nombres y pasa a ser parte del contrato de la API. Cambiarlo rompe el despliegue de ese local.

---

### DT-041 — Las claves primarias compuestas se mapean con identificadores planos
**Estado:** Decidido — restricción de Hibernate, no del diseño

Hibernate no propaga `@JdbcTypeCode(CHAR)` a una asociación `@ManyToOne` que forma parte de una clave compuesta con `@IdClass`, y como los identificadores son `char(26)`, la asociación se rompe.

**Solución adoptada:** las columnas de la clave se declaran como `@Id` de tipo `String` plano, y las asociaciones se añaden aparte como solo lectura (`insertable = false, updatable = false`).

**No cambia el esquema físico.** Vuelve a aplicar en `rol_permiso`, `usuario_rol`, `usuario_local`, `usuario_permiso_excepcion`, `producto_servicio` y `existencia`: conviene resolverlo igual en todas para que el código se lea de una sola manera.

---

### DT-042 — `sesion.dispositivo` se trunca al guardar
**Estado:** Decidido

La columna pasó de 120 a 255 caracteres, pero el valor real es el User-Agent del navegador y **puede superar también ese límite**. La aplicación trunca; no se confía en que quepa.

**Lo que hace este caso instructivo:** el `INSERT` fallaba solo desde un navegador real. Con `curl` o PowerShell no se notaba, porque esos clientes mandan una cadena corta. Es el patrón de error que sobrevive a toda la batería de pruebas automatizadas y aparece el día de la demostración.

---

### 10.1 Deuda declarada del Sprint 1

**El `DROP` + `CREATE` de `local` fue la última vez.** La migración `V6` reemplazó la tabla `local` que `V1` había creado como marcador de posición en el sprint 0. Fue correcto no editar `V1` y correcto hacerlo ahora, porque no había datos reales en ninguna parte. **A partir de aquí, ningún `DROP TABLE` en una migración**: en cuanto un entorno tenga datos, esa operación los destruye sin aviso y sin vuelta atrás. Las correcciones se hacen con `ALTER`.

**Falta la prueba de la desactivación de local.** Las cuatro pruebas del sprint cubren login válido, usuario desactivado, bloqueo por intentos y resolución de locales asignados. US-001 CA3 —desactivar un local impide nuevas operaciones y conserva su historial— no tiene prueba. Conviene agregarla antes del sprint 2, porque a partir de ahí todo cuelga del local.

**El usuario semilla tiene PIN `1234` y contraseña `cambiar123`.** Está bien para desarrollo. Debe quedar imposible que esas credenciales lleguen al perfil `nube`: conviene que la migración de semilla no se ejecute en ese perfil, o que el arranque en `nube` falle si detecta ese usuario.

**`GET /roles` se agregó sin estar en el enunciado**, para que el formulario de alta de usuario pueda listarlos. Es correcto y conviene registrarlo: cuando llegue US-004, ese mismo endpoint tendrá que devolver también los permisos de cada rol.

---

### DT-043 — El local admite borrado físico mientras no tenga operaciones
**Estado:** Decidido por el equipo — contradice `01` § Reglas de historial, y queda registrado como decisión consciente

La pantalla de locales ofrece borrar, no solo desactivar.

**El motivo, que es legítimo:** `local.codigo` no se puede cambiar una vez creado, porque forma parte de la dirección del POS y del contrato de la API (DT-040). Un código mal escrito dejaría un local permanentemente equivocado si la única salida fuera desactivarlo.

**El choque, que conviene tener presente:** `01` § Reglas de historial dice que desactivar registros no elimina su historial, y US-001 CA3 pide conservarlo íntegro. El borrado físico va en contra de esa regla.

**Lo que acota el riesgo sin que nadie tenga que acordarse:** todas las claves foráneas que apuntan a `local` son `ON DELETE RESTRICT`. La base de datos **rechaza** el borrado en cuanto exista un pedido, un turno de caja, un movimiento de inventario o un usuario asignado. En la práctica solo se puede borrar un local recién creado y sin usar, que es exactamente el caso del código mal escrito.

**Lo que hay que hacer bien:** el mensaje de error no puede ser un fallo genérico. Debe decir qué impide el borrado —«este local ya tiene ventas registradas»— y ofrecer desactivar en su lugar.

---

### DT-044 — Los horarios de servicio son por día de la semana, con varios turnos
**Estado:** Decidido — corrige el modelo de datos

La versión anterior guardaba un rango por servicio y por local: almuerzo de 12:00 a 15:00, sin distinguir el día. No alcanza.

**Por qué:** casi ningún restaurante abre igual el domingo que el martes, y un mismo día suele tener dos turnos. Y de esos horarios depende que el kiosco determine solo el servicio (US-019 CA2): con un horario único, el domingo cargaría el consumo al servicio equivocado.

Se agregaron `dia_semana` a `horario_servicio` y las tablas `dia_especial` y `horario_dia_especial` para feriados. Dos reglas que hay que implementar explícitamente:

- **Si `hora_fin` es menor o igual que `hora_inicio`, la franja cruza la medianoche.** Un turno de 19:00 a 00:30 es normal en este negocio.
- **El día especial manda sobre el horario semanal.** Si hay fila para esa fecha, se ignora el día de la semana.

Origen: revisión de la pantalla equivalente de un sistema en producción que el equipo tomó como referencia.

---

### DT-045 — Coordenadas del local: se guardan, no se usan todavía
**Estado:** Decidido

`latitud` y `longitud` son opcionales en `local`.

**Hoy no alimentan ningún cálculo**, porque RN-081 fija el delivery como gratuito y sin zonas: caja decide si acepta la dirección, no hay reparto por distancia.

**Se guardan igual** porque son baratas de capturar mientras alguien está dando de alta el local, y caras de recolectar después local por local. Si el propietario decide algún día cobrar envío por zona, el dato ya está.

---

### 10.2 Lo que se descartó de la pantalla de referencia

El equipo revisó la pantalla de locales de un sistema en producción (RESPRO) para subir el nivel de la propia. Se tomó su estructura en pestañas, las tarjetas de resumen, el correo de contacto, el gerente del local y los horarios por día. **Se descartaron tres cosas**, y conviene que quede el motivo:

| Descartado | Motivo | Estado |
|---|---|---|
| Redes sociales y logo del local | `README` excluye la publicación pública del menú del día; el menú solo se publica dentro del POS. Sin superficie pública, esos campos no alimentan nada | **Revertido en V12** — ver DT-046 |
| Tiempos mínimo y máximo de entrega | Comprometer un plazo de entrega es una decisión de negocio que el propietario no ha tomado, y no está en la especificación | **Revertido en V12** — ver DT-046 |
| RUC y razón social por sucursal | `06` § Organización define Empresa 1—N Local: un solo RUC y una sola razón social, con los locales como establecimientos anexos. Si el propietario tuviera dos razones sociales, el modelo cambiaría de verdad — va junto con P-04 | Sigue descartado |
| Zonas de reparto | RN-081: el delivery es gratuito y sin zonas | Sigue descartado |

---

### DT-046 — Redes sociales, logo y tiempos de entrega: agregados fuera de la especificación
**Estado:** Implementado en V12 — **pendiente de confirmación del propietario (P-20)**

Durante la ampliación de la pantalla de locales se agregaron ocho columnas que la sección 10.2 de este documento había descartado con motivo explícito: `facebook_url`, `instagram_url`, `tiktok_url`, `logo_url`, `delivery_habilitado`, `recojo_habilitado`, `tiempo_min_entrega_min` y `tiempo_max_entrega_min`.

**Por qué se registra aparte y no como un cambio más.** Los ocho campos existen ahora en la base de datos, pero **ninguna regla de la especificación los pide**, y dos de ellos chocan con reglas que sí están escritas:

- `README` excluye la publicación pública del menú del día. **Hoy no hay ninguna pantalla que muestre los enlaces de redes ni el logo**: se capturan y no se leen en ningún lado.
- Comprometer un tiempo de entrega mínimo y máximo es una promesa al cliente. El delivery de RN-081 es gratuito, sin zonas, y es caja quien decide si acepta la dirección: no hay nada en la especificación que comprometa un plazo.

**Lo que hay que resolver, y es de trazabilidad, no técnico.** Estos campos entran al sistema como decisión del equipo, no como requisito documentado. Ante una revisión académica o ante el propietario, la pregunta va a ser de dónde sale el requisito. Hay dos salidas limpias:

1. **El propietario los confirma**, y entonces se agregan a la especificación funcional como requisitos nuevos, con su fecha.
2. **No los confirma**, y se retiran o se dejan ocultos hasta que exista la superficie que los use.

Lo que no conviene es dejarlos a mitad de camino: columnas pobladas sin regla que las respalde y sin pantalla que las consuma.

**Lo que sí se mantuvo descartado**, y está bien: RUC y razón social por local, y zonas de reparto.

---

### DT-047 — El permiso efectivo se resuelve al iniciar sesión y viaja en el token
**Estado:** Decidido — implementado en V13

El token del login lleva, además de los roles, la lista de **códigos de permiso efectivos** ya resueltos: la unión de los permisos de todos los roles del usuario más sus excepciones individuales, con la excepción ganando sobre el rol.

**Por qué al iniciar sesión y no en cada petición:** resolverlo por petición significa repetir la misma consulta miles de veces al día, y en el nodo del local eso se nota.

**La consecuencia que hay que conocer:** un cambio de permisos **no afecta a una sesión ya abierta** hasta que el usuario vuelva a entrar. Con 49 permisos el token sigue siendo pequeño; si el catálogo creciera mucho habría que pasar a una representación compacta.

**Y la parte que importa de verdad:** el backend valida el permiso en cada endpoint sensible, no solo se oculta el botón. Comprobado con un token real de un cajero: `PUT /api/roles/{id}/permisos` devuelve 403 aunque la pantalla no muestre el botón. Es el caso crítico 19 de la especificación.

---

### 10.3 Un hallazgo del sprint 1 ampliado que conviene no olvidar

Al construir la administración de roles apareció un defecto que llevaba semanas en el sistema sin que nadie lo notara: el rol **«administrador de local»** nunca coincidía con el nombre de autoridad que esperaba la configuración de seguridad, porque uno conservaba el «de» y el otro no. **Cualquier usuario con ese rol recibía 403 en todo el panel desde que el rol existe.**

No se había detectado porque hasta entonces nadie había iniciado sesión con ese rol: todas las pruebas usaban el administrador central.

**La lección, que vale para los sprints que vienen:** hace falta al menos una prueba de acceso **por cada rol del sistema**, no solo del administrador. Un rol que nadie usa en las pruebas es un rol que nadie sabe si funciona.

---

## 11. Convenciones establecidas en el Sprint 2

Sprint 2 ejecutado el 24 de septiembre de 2026: catálogo de productos, unidades y presentaciones, proveedores, generación de tipos desde OpenAPI y teclado numérico compartido. 58 pruebas en verde, 7 tablas nuevas, tres migraciones.

---

### DT-048 — Costo inicial manual, reemplazado por la primera compra
**Estado:** Decidido — temporal por diseño

RN-047 valoriza los insumos con el **costo de la última compra**, pero las compras llegan en el sprint 9. Sin una salida, el catálogo nace con todos los costos en cero y el costeo de recetas del sprint 4 no se puede probar con números reales.

**La solución:** `producto.origen_costo`, con dos valores. Un costo cargado a mano nace como `manual` y se muestra marcado como provisional. Cuando una compra confirmada lo reemplaza, el origen pasa a `compra` y **el campo deja de ser editable**: un intento posterior se rechaza con `COSTO_YA_CONFIRMADO_POR_COMPRA`.

**Por qué el bloqueo importa:** sin él, alguien podría pisar con una estimación un costo que ya viene de una factura, y el reporte de margen mentiría sin que nadie lo note.

Es un requisito fuera del backlog original, añadido por necesidad del arranque. Queda registrado como tal.

---

### DT-049 — Los controladores llevan `@Transactional` porque `open-in-view` está desactivado
**Estado:** Decidido — y conviene conocer el síntoma

Con `open-in-view: false`, la sesión de Hibernate se cierra al salir del caso de uso. Un controlador que después lee una relación `@ManyToOne` perezosa para armar su respuesta lanza `LazyInitializationException`.

**Lo que hace este caso digno de registrarse es el síntoma: Spring la traducía a un 403, no a un 500.** Parecía un problema de permisos, y se perdió tiempo revisando roles antes de encontrar la causa real. Se confirmó con un token que sí tenía el permiso correcto y seguía fallando.

**La regla:** todo controlador que arme una respuesta leyendo relaciones perezosas lleva `@Transactional(readOnly = true)` a nivel de método.

**Y la señal de alarma para el futuro:** un 403 que no cuadra con los permisos del usuario **no siempre es un problema de permisos**. Antes de revisar roles, mirar el log del servidor.

---

### DT-050 — El chequeo de tipos del frontend es `tsc -b`, no `tsc --noEmit`
**Estado:** Decidido — corrige un método de verificación que no verificaba nada

El `tsconfig.json` raíz está en modo *solution*: `"files": []` con referencias a `tsconfig.app.json` y `tsconfig.node.json`. **`tsc --noEmit` contra ese archivo no revisa nada**, y devuelve limpio siempre.

Durante el sprint 1, el bloque de diseño y buena parte del sprint 2 se usó ese comando como punto de control. Al correr por primera vez `tsc -b`, el build real, aparecieron de inmediato dos problemas que llevaban semanas ahí: un import sin usar y la falta de `@types/node`, que bloqueaba la compilación real desde antes.

**La regla:** el punto de control del frontend es `npx tsc -b` o `npm run build`. El otro comando no debe volver a usarse como evidencia de nada.

**La lección más general:** un checkpoint que siempre pasa no está confirmando que todo esté bien; lo más probable es que no esté mirando.

---

### 11.1 El hallazgo del sprint: funcionalidad completa salvo el endpoint

En la revisión final apareció que **el botón «Activar» no funcionaba en ninguna parte**. Existía la función en el cliente de la API, existía el botón con su modal de confirmación en las pantallas de productos y proveedores, existían hasta los matchers de seguridad que protegían la ruta — **y no existía el endpoint**. Cualquier administrador que desactivara un producto y quisiera reactivarlo habría recibido un 404.

**Lo que más importa de este caso:** ni las 54 pruebas del backend ni la verificación en el navegador lo detectaron. Las pruebas cubrían desactivar; la verificación en vivo también. **Ninguna recorría el ciclo completo desactivar → reactivar.**

**La regla que queda:** cuando una operación tiene su inversa —desactivar y activar, abrir y cerrar, vincular y desvincular, emitir y anular—, la prueba cubre **el ciclo completo**, no cada mitad por separado. Media función probada da la misma sensación de seguridad que una función entera, y es lo que hace peligroso este tipo de hueco.

Aplica directamente a lo que viene: abrir y cerrar turno de caja, vincular y anular un pre-ticket, emitir y anular un comprobante, transferir entre locales.

---

### 11.2 Lo que la generación de tipos ya está evitando

HT-05 se probó de verdad, y conviene que quede el resultado: se renombró un campo en el backend, se regeneraron los tipos, y el compilador del frontend señaló **los cuatro usos reales** de ese campo, con archivo y línea. Después se revirtió.

Sin eso, ese renombre habría llegado a la pantalla como un valor vacío, sin error, sin aviso — que es exactamente lo que pasó con el factor de las presentaciones (bug 6 del sprint): el campo no existía en el DTO, `undefined` se renderizó en silencio, y se descubrió comparando una captura contra el texto esperado.

**El límite que hay que levantar:** 47 métodos del backend devuelven `ResponseEntity<?>`. Para la especificación OpenAPI, ese comodín hace **invisible** el DTO, y esos endpoints quedan fuera de la red de seguridad. Está anotado como P-22.

---

### DT-051 — Borrado real con bloqueo previo, extendido a producto, proveedor, categoría, rol y usuario
**Estado:** Decidido por el equipo — misma naturaleza que DT-043

Tras el cierre del sprint 2 se extendió el borrado físico que DT-043 introdujo para el local. Ahora también se pueden borrar productos, proveedores, categorías, roles y usuarios.

**El patrón adoptado, y es el correcto:** antes de borrar se **consulta** si existe algo que referencie al registro, y se devuelve un error de negocio claro —«este producto ya tiene presentaciones de compra registradas»— en lugar de dejar que reviente la clave foránea. El usuario ve una explicación, no un error del servidor, y se le sugiere desactivar en su lugar.

**La excepción es `usuario`**, y la solución merece quedar registrada. Casi todas las tablas del sistema apuntan a `usuario` por `creado_por`, `actualizado_por` o `registrado_por`. Consultar tabla por tabla obligaría a recordar actualizar esa lista cada vez que se agregue una tabla nueva — y nadie se acuerda. Se optó por intentar el borrado y capturar la violación.

**Eso expuso un comportamiento de PostgreSQL que conviene conocer:** una vez que una violación de clave foránea aborta la transacción, **toda la transacción queda inutilizable**, incluidas las lecturas posteriores, aunque la excepción ya se haya capturado en Java. La solución fue envolver el borrado en una transacción genuinamente nueva (`REQUIRES_NEW`), para que su aborto no contamine la del llamador. Está documentado en la propia clase.

**Lo que sigue en tensión con la especificación:** `01` § Reglas de historial dice que desactivar no elimina el historial, y US-005 CA2 pide conservar la atribución de las operaciones pasadas. El bloqueo previo lo protege en la práctica —un usuario con actividad registrada no se puede borrar—, pero la regla escrita dice otra cosa. Queda como decisión consciente del equipo, igual que DT-043.

---

### 11.3 Dos cosas que conviene arreglar antes del sprint 3

**El archivo de tipos generado se editó a mano.** `esquema.d.ts` está en `.gitignore` y se le agregaron cuatro campos manualmente en lugar de regenerarlo. Eso anula justamente lo que HT-05 existe para dar: la garantía de que un cambio en el backend rompe la compilación del frontend en vez de llegar como un valor vacío. Esos cuatro campos hoy no están verificados contra nada.

Y tiene un efecto secundario para el equipo: al estar en `.gitignore`, quien clone el repositorio **no puede compilar el frontend** hasta levantar el backend y regenerar los tipos. Hay que decirlo en la guía de arranque.

**Las excepciones inesperadas siguen apareciendo como 403.** Ya van dos: una `LazyInitializationException` en el sprint 2 y una `NullPointerException` al crear subcategorías. Las dos costaron tiempo revisando permisos que estaban bien.

Un fallo interno que se presenta como «no tienes permiso» es de los que más caro salen, porque manda a buscar en el lugar equivocado. Vale la pena encontrar qué lo está traduciendo —muy probablemente la configuración de seguridad o un manejador global— y hacer que un error inesperado devuelva 500 con su traza. Es una tarde de trabajo que se paga sola en el primer bug del sprint 3.

---

### DT-052 — Manejador global de errores con referencia de correlación
**Estado:** Decidido — implementado el 25/09/2026

Hasta ahora no existía ningún manejador global: una excepción no prevista caía en la página de error por defecto de Spring Boot, un 500 sin cuerpo útil y sin forma de relacionarlo con el log.

`ManejadorGlobalDeErrores` hace dos cosas:

- **Re-lanza** `AccessDeniedException` y `AuthenticationException` tal cual, para que Spring Security siga devolviendo sus 401 y 403 reales. Un manejador que se las come convierte un problema de permisos en un 500 genérico, que es peor que el problema original.
- Para cualquier otra excepción: registra la traza completa del lado del servidor con una **referencia corta**, y responde 500 con esa referencia y nada más. El cliente nunca ve el tipo ni el mensaje de la excepción —que puede filtrar estructura interna— pero quien atiende el problema puede ir al log y encontrar exactamente qué pasó.

Esa referencia es lo que convierte «me salió un error» en algo investigable. Sin ella, un fallo en el local a las nueve de la noche es una llamada telefónica sin datos.

---

### DT-053 — Guarda de la semilla de desarrollo en el perfil de nube
**Estado:** Implementado — **con un efecto que hay que revisar (P-29)**

`GuardaSemillaDesarrollo` se ejecuta al arrancar solo bajo el perfil `nube`: si encuentra el usuario semilla —buscado por su identificador fijo, no por nombre, para no confundirlo con un administrador real que también se llame «admin»— detiene el arranque con un mensaje explícito.

**La decisión de fondo es correcta:** fallar cerrado. Un sistema que arranca en producción con `admin` / `cambiar123` es peor que uno que no arranca.

**El efecto que hay que revisar:** en un despliegue **nuevo** al perfil `nube`, la migración `V9` crea el usuario semilla y el guarda lo encuentra en ese mismo arranque. El servicio no levantaría nunca, y alguien tendría que borrar la fila a mano contra la base de producción antes del primer arranque exitoso.

Dos salidas limpias, cualquiera sirve:

1. **Que el guarda desactive el usuario** en lugar de detener el arranque, con una advertencia bien visible en el log. Sigue siendo seguro —la cuenta no sirve para entrar— y el despliegue funciona.
2. **Que la semilla no se ejecute en `nube`**, con los datos de desarrollo en una ubicación de migración aparte que solo se active en los perfiles locales.

La primera es más simple y no toca el historial de Flyway.

---

## 12. Convenciones establecidas en el Sprint 4

### DT-054 — El costo de una receta se calcula por unidad base, también el de las subrecetas
**Estado:** Implementado (US-010) — corrige la fórmula del modelo de datos §5.3

El modelo multiplicaba la cantidad de una subreceta usada como ingrediente por su **costo por porción**. Solo es correcto si la cantidad está en porciones, y en la cocina no se piensa así: el seco lleva "300 g de aderezo", no "0,4 porciones de aderezo".

**La regla:** el rendimiento de una receta va siempre en la **unidad base de su producto**, y el costo por unidad base de una subreceta es `costo_total / rendimiento` — el mismo papel que `costo_ultima_compra` en un insumo. Así un ingrediente se costea de una sola manera:

```
costo del ingrediente = cantidad convertida a la unidad base × costo por unidad base
```

**Consecuencias en el código:**

- `CalculadoraCostoReceta` es el único lugar que hace la cuenta. La conversión usa `conversion_unidad` del producto del ingrediente, en los dos sentidos (`1 origen = factor destino`). Sin conversión, la receta **no se guarda** (`SIN_CONVERSION`, con el ingrediente culpable): un costo mal convertido es peor que no tener costo.
- Si un ingrediente no tiene costo todavía, la receta queda con costo **nulo** — "incompleta" en pantalla —, nunca cero. Cero mentiría un margen.
- Costos con 6 decimales y redondeo mitad arriba (la escala de las columnas); se redondean a céntimos solo al mostrarlos. P-15 sigue abierto para `Dinero`.
- **Recálculo en cascada:** al cambiar el costo de un insumo (hoy, el costo manual de DT-048; en el sprint 9, la compra) o al guardar una receta, se recalculan las recetas que usan ese producto y, hacia arriba, las que usan a esas. Termina porque los ciclos se rechazan al guardar (`CICLO`).
- **Quién ve el costo lo decide el backend** (`RecetaController`): sin `recetas.ver_costos` los campos de costo salen nulos. Ver P-30 para el hueco que queda en el catálogo.
- Se quitaron `receta.nombre` (la receta se llama como su producto) y `unidad_rendimiento_id` (siempre es la unidad base).

---

## 13. Convenciones establecidas en el Sprint 6

Decisiones tomadas al construir la venta en mesa (US-027 a US-031, sprint 6b). Detalle y pruebas en `historias/`.

### DT-055 — El estado de una mesa se deriva de sus pedidos, no se guarda
**Estado:** Implementado (US-027, regla ajustada el 2026-10-01 en sprint 6b)

`CalculadorDeEstadoDeMesa` calcula libre, ocupada, unida o bloqueada desde los pedidos y la tabla de uniones; no hay columna de estado que pueda quedar desincronizada.
**Ocupada significa pedido comandado.** Un pedido abierto sin comandar deja la mesa `libre` con la marca `tomandoPedido`, así una mesa no queda "ocupada" por alguien que solo abrió la pantalla. Consecuencias: abrir un pedido sobre una mesa con pedido abierto devuelve ese mismo (200 en vez de 201); una mesa con pedido en curso no se une como secundaria (`TIENE_PEDIDO_ABIERTO`). Enmienda al criterio US-07 CA2 del backlog v3 en `docs/producto/prd/05-enmiendas-al-backlog-v3.md`.

### DT-056 — Bloqueo optimista en `pedido` y `detalle_pedido`
**Estado:** Implementado (V37)

Dos mozos tocando el mismo pedido o la misma línea no se pisan: `@Version` en ambos. Los controladores de venta reintentan hasta 3 veces ante una violación de unicidad o de versión (`protegido()`) y, si persiste, devuelven 409. Cualquier escritura nueva sobre pedidos o líneas debe pasar por ese mismo envoltorio.

### DT-057 — La comanda es una foto, y el contador de impresiones es atómico
**Estado:** Implementado (V33, V36)

`detalle_comanda` guarda `etiqueta_persona` y las cantidades al momento de comandar: si después se traslada una línea o se cambia el nombre, la comanda impresa no cambia. Reimprimir se marca como copia (RN-063). `sumarImpresion` es un `UPDATE … RETURNING` nativo, para que dos reimpresiones simultáneas no pierdan una cuenta; el caso de uso refresca la entidad ya cargada.

### DT-058 — El precio lo decide el servidor; al trasladar solo se recalcula si interviene un pensionista
**Estado:** Implementado (US-028, US-029)

El mozo solo escribe el precio de un "extra libre" (queda marcado `manual` y auditado). Al trasladar líneas entre personas, el precio del menú normal se recalcula únicamente si el origen o el destino es pensionista (cambia entre público y plan); el resto conserva su precio. La anulación exige `venta.anular` y un motivo de tipo `anulacion` (RN-072); reorganizar exige `venta.modificar`.

### DT-059 — "Terminar pedido" es un cierre sin cobro, provisional hasta el sprint 7
**Estado:** Decidido, provisional (V38, 2026-10-01; confirmado por el dueño el 2026-10-05)

Deja el pedido `cerrado` y la mesa libre, sin cobro, sin motivo y con solo `venta.modificar`. Es una brecha conocida contra US-09 CA3 / RN-072 / RN-065 / RN-005 (ver `docs/producto/plan/04-comparacion-backlog-v3.md`). **No se endurece**: el sprint 7 lo reemplaza por el cobro real. Mientras tanto, esos pedidos no cuentan como ventas ni se usan en reportes.

### DT-060 — El grupo de un producto de la carta sale de su categoría raíz, nunca de su nombre
**Estado:** Implementado (sprint 6b, 2026-10-01)

`ProductoOfrecido(id, nombre, precio, grupo, categoria)`: `grupo` es `entrada`, `segundo` o `extra` según la categoría raíz (naturaleza `extra`, o `componente_menu` con componente entrada/segundo) y `null` para bebidas, postres y guarniciones. La carta incluye las categorías `componente_menu`, que antes quedaban fuera, y sale ordenada por grupo y nombre. El frontend filtra por `grupo`, nunca por texto.

### DT-061 — El User-Agent se recorta antes de guardarse
**Estado:** Implementado (2026-10-05)

El User-Agent de un navegador real supera las columnas: `bloqueo_pin.dispositivo` mide 120 y `sesion.dispositivo` 255. `LimitadorIntentosPin` recorta la huella a 120 y `Sesion` a 255. Antes, un PIN incorrecto desde un navegador real daba 500 y el fallo no se contaba, o sea que el bloqueo por intentos no funcionaba (US-54 CA3).


## 14. Convenciones establecidas en el Sprint 7

### DT-062 — El cobro es por líneas, y el snapshot vive en `cobro_linea`
**Estado:** Implementado (V41, 2026-10-11)

Un cobro (`cobro`) es un acto de cobrar líneas de un pedido en el turno de un cajero. Cada línea cobrada guarda su snapshot económico en `cobro_linea` (nombre, cantidad, precio, taper, descuento, importe, costo unitario de la receta, utilidad y composición), único por línea (`cobro_linea_una_vez`), y pasa a estado `cobrado`. El pedido se cierra solo cuando no le queda ninguna línea pendiente ni comandada; la mesa queda libre porque su estado se deriva de los pedidos (DT-055). El modelo original enlazaba `pago` a `cuenta_persona` (cobro por persona): al cobrar por líneas, `pago` cuelga de `cobro` y no existe la tabla `medio_pago` (el medio es un texto con CHECK). Costo y utilidad se guardan **en soles** y los precios e importes en **céntimos**; si falta el costo de una receta, el costo queda **nulo**, nunca cero.

### DT-063 — Un turno abierto por caja y por cajero, garantizado por la base de datos
**Estado:** Implementado (V40)

Dos índices únicos parciales (`WHERE estado = 'abierto'`) sostienen RN-073 y que el dinero de cajas distintas no se cruce; un choque de dos aperturas simultáneas se traduce a 409, no a 500. "Caja 1" se crea sola la primera vez que un local pide sus cajas.

### DT-064 — Las lineas de un pensionista se cargan a su cuenta dentro de la misma transacción del cobro
**Estado:** Implementado (V41)

`CobrarPedido` llama a `RegistrarConsumo` (pensionistas) en la transacción del cobro: si algo falla, se deshace todo. El cargo aplica primero el saldo a favor porque el saldo es la suma del libro. Superar el tope solo marca `cobro_cargo.supera_tope`, nunca bloquea (RN-017). Un pensionista desactivado no consume y no se cobra ninguna línea del cobro.
