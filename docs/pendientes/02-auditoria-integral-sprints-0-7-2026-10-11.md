# Pendientes de auditoría integral — sprints 0–7

**Corte:** 11/10/2026, America/Lima. **Estado:** asuntos abiertos para refinamiento o implementación, salvo DOC-02 corregido documentalmente en esta sesión; ninguna solución de código se programó. **Base:** commit `9e6391b` más los cambios locales existentes al comenzar, incluidos cobro/V41 en curso.

Este registro reúne **hallazgos, riesgos, validaciones y entregas previstas**, no 65 errores reproducidos. Cada ID conserva su evidencia y detalle en los informes enlazados. Los IDs P-NN y RF-NNN existentes se conservan; cuando coinciden, se indican como antecedentes y no crean un segundo trabajo.

## Cómo usarlo en el equipo

| Campo | Regla |
|---|---|
| Prioridad | P0 acceso crítico; P1 dinero/historial/recorrido central o validación para aceptar entrega; P2 robustez y consistencia; P3 mejora menor. Propuesta, no compromiso de fecha. |
| Tipo | C = confirmado por lectura; E = confirmado por ejecución; R = riesgo razonado; V = validación pendiente; F = entrega funcional prevista; D = divergencia documental; M = mejora a acordar. Puede tener más de un tipo. |
| Estado | Abierto → refinado → seleccionado → en curso → verificado → aceptado/cerrado. Un cambio de código no basta para cerrar sin el criterio de aceptación. |
| Responsable | Se indica función sugerida. El equipo asigna personas durante refinamiento; Claude implementa, QA verifica y Product Owner valida decisiones. |
| Ubicación | Evidencia detallada en [fundaciones](../auditorias/2026-10-11/02-fundaciones-catalogo-pensionistas.md), [venta/caja](../auditorias/2026-10-11/03-venta-pos-caja.md), [frontend](../auditorias/2026-10-11/04-frontend-experiencia-cliente.md), [modelo](../auditorias/2026-10-11/05-modelo-datos-arquitectura.md). |
| Solución | Es propuesta documentada. Si el equipo elige otra, registrar decisión, razón y criterio equivalente. No editar migraciones ya aplicadas. |

No asignar todo a «sprint 7» automáticamente. Los defectos de entregas anteriores van a consolidación; la integración de caja sigue su plan; offline/fiscal/reportes mantienen sus sprints previstos. Las pruebas de carreras requieren transacciones/conexiones reales, no únicamente excepciones simuladas con Mockito.

## Orden sugerido de trabajo

| Bloque | IDs principales | Resultado y dependencia | Responsable por función |
|---|---|---|---|
| 1. Acceso y actor correcto | FND-01..05, CAT-01, WEB-01/06, ARC-04 | Impedir elevación, revocar acceso y atribuir operaciones a la persona correcta; UI consume permisos propios | Claude backend/frontend; QA por rol/local; PO acuerda política |
| 2. Contratos e integridad económica | MOD-01, CAT-02..05, FND-07, POS-04/05, CAJ-01/02/06 | Identificadores correctos, costos/precios coherentes, snapshots completos y errores previsibles | Claude backend; QA con escenarios económicos |
| 3. Operación concurrente y recuperación | POS-01/02/03, CAJ-03/04/05, PEN-01/02/03, WEB-02..05/07/09 | Dos mozos/cajeros no pierden datos ni duplican deuda; reintento recupera resultado | Claude; QA con dos dispositivos/conexiones |
| 4. Aceptación de sprint 6 y 7 | POS-06..08, CAJ-07, WEB-08/10/11/14/15/16, FND-06 | Comanda, turno, cobro, móvil y flujo completos según entregas seleccionadas | Claude; mozo/cocina/cajero; QA; PO acepta |
| 5. Evidencia y documentación | FND-08..10, CAT-06/07, WEB-12/13/17/18, ARC-01/03, CAL-01, DOC-01..04 | Historial reconstruible, build repetible y documentos que representan la entrega | Claude; revisor independiente; documentación |

Este orden admite trabajo paralelo después de acordar los contratos compartidos. POS-01 y CAJ-03 deben usar el mismo protocolo de agregado; CAJ-02 depende de una interfaz de snapshot del catálogo; FND-02 y WEB-06 deben compartir la misma matriz de acciones.

## Fundaciones — sprints 1 y 3

Evidencia y justificación: [informe 02](../auditorias/2026-10-11/02-fundaciones-catalogo-pensionistas.md).

| ID | Prioridad / tipo | Pendiente | Solución propuesta | Criterio mínimo para cerrar | Responsable sugerido |
|---|---|---|---|---|---|
| FND-01 | P0 / C | Admin local puede concederse rol central mediante roles/locales o alta | Política de concesión y ámbito dentro de casos de uso; central concede privilegio central; auditar cambios | Autoelevación, edición de central y usuario ajeno rechazadas 403 sin escrituras; central realiza operaciones permitidas | Backend + QA seguridad |
| FND-02 | P1 / C | Identidad usa roles amplios en lugar de permisos de cada acción | Matriz por método/acción y permisos efectivos, conservando restricciones jerárquicas FND-01 | Retirar `identidad.crear` impide crear; conceder a rol permitido habilita; pruebas por excepción | Backend + PO |
| FND-03 | P1 / C | Token previo conserva permisos/local tras desactivación o retirada | Revocación o versión de autorización local persistida; verificar actor/local/ámbito | Token realmente emitido deja de operar tras cambio; reactivar no reactiva tokens revocados | Backend + QA |
| FND-04 | P1 / C | Dos PIN iguales de trabajadores del mismo local identifican al primero | Evitar colisiones también al asignar local; mantener hash; si necesario, revisar selección de identidad | PIN no ambiguo en locales compartidos; colisión responde negocio; bitácora nunca contiene PIN/hash | Backend + PO |
| FND-05 | P1 / C/R | Bloqueo depende de User-Agent alterable y compartido | Identidad de terminal y límites adicionales, contador concurrente y limpieza de vencidos | Cambiar encabezado no elimina límite; dos terminales iguales independientes; reinicio conserva bloqueo | Backend + QA; antecedente DT-039/RF-006 |
| FND-06 | P1 / E/C | Franja nocturna se aplica a día incorrecto | Intervalo con fecha de origen y arrastre anterior; prioridad de día especial | Lunes 19:00–martes 00:30 activo martes 00:15 y no lunes 00:15; límites/feriado probados | Backend + QA horarios |
| FND-07 | P1 / E/C/V | Entradas y colisiones previsibles carecen de contrato uniforme de error; validación/JSON/tipo sí reproducidos | Validación antes de transformación; mapear 400/422/409 conocidos y preservar correlación del 500 | JSON/formato/nulo/largo/duplicado reciben 4xx acordado y no cambian datos; fallo inesperado sigue trazable | Backend + QA HTTP |
| FND-08 | P2 / C/V | Bitácora sin protección SQL versionada | Migración nueva con inmutabilidad y usuario de aplicación limitado; mantenimiento controlado | UPDATE/DELETE rechazados en PostgreSQL con conexión de aplicación; INSERT/lectura funcionan | Backend/infra + QA; brecha ya conocida |
| FND-09 | P2 / C | Repetir estado registra anterior inventado | Leer anterior real y acordar acción idempotente/evento de intento | Dos desactivaciones no producen falso verdadero→falso; antes/después fiel | Backend + QA |
| FND-10 | P2 / C | Roles/locales y PIN trabajador sin auditoría transversal | Registrar conjunto previo/nuevo y actor; evento de credencial sin secreto | Reconstruir quién cambió acceso o reinició PIN y cuándo, sin exponer credenciales | Backend + QA |

## Catálogo, recetas y menú — sprints 2 y 4

Evidencia: [informe 02](../auditorias/2026-10-11/02-fundaciones-catalogo-pensionistas.md).

| ID | Prioridad / tipo | Pendiente | Solución propuesta | Criterio mínimo para cerrar | Responsable sugerido |
|---|---|---|---|---|---|
| CAT-01 | P1 / C | DTO de productos revela costos sin permiso | Política económica común para lectura y respuesta de escritura | Actor sin permiso no obtiene costos desde catálogo ni receta; autorizado sí | Backend + QA; **P-30** |
| CAT-02 | P1 / C | Cambio de unidad base deja dependencias en otra escala | Bloquear con dependencias o conversión explícita completa; conservar historia | kg→g no conserva cifras incompatibles en receta/costo/presentación; error informa dependencias si bloqueado | Backend + PO; **P-31** |
| CAT-03 | P1 / C/V | Grafo de costos con convergencia deja ancestro desactualizado | Orden topológico del subgrafo o cálculo que espera todos los hijos | X→A/B→C→D termina C=D=4 cuando X pasa 1→2, independiente del orden; integración Java/DB | Backend + QA; simulación de traza ejecutada |
| CAT-04 | P1 / C | Precio/costo negativos y límites no validados | Validación de signo/precisión/rango en dominio y CHECK nuevo; acordar cero | Negativos/extremos se rechazan antes de guardar/propagar; ausencia sigue distinta de cero | Backend + PO + QA |
| CAT-05 | P2 / C | Conversiones inversas incompatibles y auto conversión | Par canónico o reciprocidad exacta; política de cadena | kg→g=1000 incompatible con g→kg=.002 se rechaza; 1000 g→1 kg sin depender del orden | Backend + QA |
| CAT-06 | P2 / C/V | Borrado ignora referencias nuevas de pedido/caja | Desactivar usados; revisar todas las FK y devolver `TIENE_HISTORIAL` donde corresponda | Producto vendido, opción usada y local con operaciones no se borran ni dan 500; retirar oferta conserva historia | Backend + QA; DT-051 |
| CAT-07 | P2 / C/M | Renombrar motivo altera lectura del movimiento antiguo | Snapshot/versionado del motivo aplicado; auditar mantenimiento | Estado de cuenta conserva motivo original y cambio posterior es reconstruible | Backend + PO |

## Pensionistas — sprint 5 e integración con 7

Evidencia: [informe 02](../auditorias/2026-10-11/02-fundaciones-catalogo-pensionistas.md). PEN-01 debe resolverse antes de sincronización; no se atribuye explotación externa al servicio interno.

| ID | Prioridad / tipo | Pendiente | Solución propuesta | Criterio mínimo para cerrar | Responsable sugerido |
|---|---|---|---|---|---|
| PEN-01 | P1 antes de s10 / C/R | Libro acepta ID existente con contenido distinto y carrera de reenvío | Inserción atómica idempotente + comparación normalizada de contenido | Mismo ID/payload una fila; otro importe/cuenta conflicto; dos reenvíos concurrentes coherentes | Backend + QA |
| PEN-02 | P2 / C | Repetir POST ajuste tras perder respuesta duplica deuda | ID de acción estable y recuperación; no deduplicar solo por monto/tiempo | Mismo comando perdido no duplica; dos acciones intencionales idénticas con IDs distintos sí | Backend/frontend + QA |
| PEN-03 | P2 / R | Dos cambios de tarifa pierden actualización o chocan sin recuperación | Bloqueo por plan/versión optimista y vigencias sin solapamientos | Una actualización y un conflicto recuperable; fecha límite consulta versión correcta | Backend + QA dos conexiones |
| PEN-04 | P2 / M | Mozo recibe ficha extensa que no requiere identificación | DTO operativo mínimo y matriz de visibilidad acordada | Búsqueda con datos necesarios; contacto/responsable financiero según permiso aprobado | PO + backend/frontend |

## Venta, mesas y comandas — sprint 6/6b

Evidencia y escenarios detallados AC-POS: [informe 03](../auditorias/2026-10-11/03-venta-pos-caja.md).

| ID | Prioridad / tipo | Pendiente | Solución propuesta | Criterio mínimo para cerrar | Responsable sugerido |
|---|---|---|---|---|---|
| POS-01 | P1 / R | Cierre concurrente con alta deja cerrado con pendientes | Protocolo único de bloqueo/versionado del agregado pedido en toda mutación | Dos conexiones cerrar↔agregar: serialización o 409; nunca cerrado con pendiente/comandada | Backend + QA; coordinar CAJ-03 |
| POS-02 | P1 / R | Uniones A→B/B→A y disponibilidad concurrente | Bloquear mesas en orden estable y revalidar abrir/unir/mover/bloquear | Nunca ciclo ni mesa bloqueada/unida con pedido propio; conflicto explícito | Backend + QA |
| POS-03 | P2 / C | PUT mismo pensionista/misma persona no idempotente | Reconocer asignación existente y excluir propia persona al buscar duplicado | Repetición retorna mismo resultado; otra persona sigue rechazada | Backend + QA |
| POS-04 | P2 / C | Segundo consumo usa hoy en vez de fecha operativa | Fecha de pedido y definición de consumo efectivo; excluir borrador descartado | Cena cruza fecha y borrador terminado no generan tarifa incorrecta | Backend + PO + QA |
| POS-05 | P2 / C/R/M | Trasladar/anular primero no recompone tarifa del segundo; altas paralelas | Acordar momento de fijación de tarifa; si dinámica, recálculo atómico por pensionista/fecha/servicio | Regla aprobada se cumple al trasladar/anular; no toca cobradas; dos primeras altas probadas | PO + backend + QA |
| POS-06 | P2 / C | Reimpresión depende de nombre/mesa actual y agrupa etiquetas iguales | Foto con nombres/componentes, ID persona, ubicación y emisor históricos | Editar plato/mover mesa no cambia reimpresión; dos personas homónimas separadas; emisor correcto | Backend/frontend + cocina |
| POS-07 | P2 / C/M | Terminar admite borrador con líneas pendientes contra plan estricto | Descartar explícitamente con estado/auditoría o restringir a vacío/todas anuladas | No queda obligación pendiente bajo cerrado ni cuenta como consumo efectivo | PO + backend/frontend |
| POS-08 | P2 / C | Oferta/escritura de menú no revisa siempre activo del producto | Política uniforme de activo/vendible para opción programada y venta | Producto desactivado no se agrega/ofrece nuevo según política; historia intacta | Backend + QA |
| POS-09 | P3 / C | Aumentar cantidad de extra libre sin evento económico nuevo | Auditar cantidad/importe antes y después | Importe manual modificado se reconstruye; evento no confunde cantidad con precio unitario | Backend + QA |

## Caja y cobro — sprint 7 en curso

CAJ-01..04/06/07 se refieren al **trabajo local V41**, no a una entrega ya aceptada. Evidencia y escenarios AC-CAJ: [informe 03](../auditorias/2026-10-11/03-venta-pos-caja.md).

| ID | Prioridad / tipo | Pendiente | Solución propuesta | Criterio mínimo para cerrar | Responsable sugerido |
|---|---|---|---|---|---|
| CAJ-01 | P1 / C | Efectivo repetido valida suma pero persiste primer recibido | Un pago por medio con 422 duplicado, o consolidación/distribución consistente | S/10 con S/5+S/5 no genera 500; S/20+S/10 conserva recibido/neto/vuelto coherentes | Backend + QA financiero |
| CAJ-02 | P1 / C | Snapshot no conserva ingredientes/subrecetas completos | Snapshot de árbol/versiones, cantidades/unidades/factores/rendimiento/costos y formato | Cobrar, editar receta e insumos y reconstruir exactamente composición/costo históricos | Backend catálogo/caja + QA |
| CAJ-03 | P1 / R | Dos parciales disjuntos dejan todo cobrado sin cerrar | Mismo protocolo del pedido de POS-01; cierre en transacción consistente | Dos últimas líneas cobradas por dos cajeros cierran una vez y liberan mesas, o conflicto recuperable | Backend + QA |
| CAJ-04 | P2 / C | Reintento de cobro no recupera resultado tras respuesta perdida | ID operación/hash y consulta autorizada de cobro | Reenvío mismo payload retorna mismo cobro/cargo; distinto payload 409 | Backend/frontend + QA |
| CAJ-05 | P2 / R | Dos primeros GET crean caja predeterminada y uno puede fallar | Crear al alta de local o inserción segura y recuperación en transacción apropiada | Ambos consultan misma Caja 1 sin 500; creación trazable | Backend + QA |
| CAJ-06 | P2 / C | `pagos:[null]` y límites estructurales/monetarios | Validar antes de mapear; tamaño/rango y aritmética exacta | Nulos/excesos/overflow 4xx sin escrituras; rollback financiero probado | Backend + QA HTTP |
| CAJ-07 | P2 / C | Total no distingue cobrado/pendiente y persona sigue abierta | DTO consumido/cobrado/por cobrar y estado derivado o transaccional | Persona A saldada y B pendiente visibles; total histórico se conserva y mesa sigue ocupada | Backend/frontend + QA |

## Frontend y experiencia operativa

Evidencia completa, soluciones y matriz de dispositivos: [informe 04](../auditorias/2026-10-11/04-frontend-experiencia-cliente.md).

| ID | Prioridad / tipo | Pendiente | Solución propuesta | Criterio mínimo para cerrar | Responsable sugerido |
|---|---|---|---|---|---|
| WEB-01 | P1 / C | Permisos globales se cargan desde endpoint de admin | `/auth/mis-permisos`, estados de carga/error y respuesta de sesión vigente | Cocina autorizada marca agotado; usuario sin permiso no; error se puede reintentar | Frontend + QA roles |
| WEB-02 | P2 / C | Sin política común de recuperación 401; falta demostrar bloqueo operativo | Reautenticar por superficie, conservar destino y validar sesión almacenada | Token vencido lleva login/PIN y retoma pedido; 403 distingue permiso; JSON inválido no rompe | Frontend + QA |
| WEB-03 | P1 / C | Error se muestra vacío o datos viejos sin aviso | Separar vacío/error/anterior; última actualización y alcance API local | Cortar API tras carga no indica ausencia de pedidos; recuperar refresca | Frontend + QA red |
| WEB-04 | P2 / C/R | Cambio de local falla silencioso y respuestas pueden invertirse | Estado en curso, feedback, selección controlada y datos dependientes recargados | Error muestra local anterior; selección rápida no aplica token/local ambiguo | Frontend + QA |
| WEB-05 | P1 / C | Guardado de varias peticiones queda parcial y reintento confuso | Caso de uso transaccional o conservar ID/etapa guardada y borrador | Segunda petición falla: una entidad, etapas recuperables; plano no pierde borrador | Backend/frontend + QA |
| WEB-06 | P2 / C | Navegación/acciones ofrecen funciones sin permiso | Matriz ruta/acción/permiso y acceso denegado claro | Rol sin acción no recibe botón imposible; URL directa clara; servidor sigue controlando | Frontend + QA; depende WEB-01/FND-02 |
| WEB-07 | P1 / C | Pedido abierto no refleja cambios de otra tablet | Refresco visible o suscripción, invalidación oferta y feedback 409 | Tablet A ve cambio/agotado de B en intervalo acordado; no duplicación ni pérdida de borrador | Frontend + QA dos dispositivos |
| WEB-08 | P2 / V/D | 56 px y teclado compartido siguen sin aceptación | Criterio único/token táctil o enmienda formal; entrada de cantidad común | Medidas reales y una mano en 375/768/1280; CA1/CA3 cumplidos o cambio aceptado | UX/frontend + PO + mozo |
| WEB-09 | P2 / R | Búsqueda lenta pisa consulta nueva de pensionista | Cancelar/invalidate petición y limpiar resultado vigente | A lenta no pisa B rápida; texto vacío no recupera datos anteriores | Frontend + QA |
| WEB-10 | P2 / C/R | Local ficticio por fallback y efecto PIN dentro de updater | Configuración local obligatoria, error visible y un efecto por PIN | Otro local/sin config no aparece San Isidro; un intento de login por PIN medido | Frontend/infra + QA |
| WEB-11 | P2 / C | Retomar pierde nombre del pensionista e identificación ambigua | Resumen autorizado en DTO y etiqueta de modo real de identificación | Otro mozo reconoce pensionista; distingue búsqueda de ticket; sin datos sobrantes | Backend/frontend + QA |
| WEB-12 | P2 / R | Órdenes limita primera página a 100 | Paginación/carga incremental y totales por filtro | 101 pedidos accesibles y contadores coherentes | Frontend/backend + QA |
| WEB-13 | P2 / C | Eventos de bitácora/filtros incompletos | Catálogo vigente y consultas mínimas autorizadas; feedback de error | Filtrar cada evento del corte sin conceder admin completo | Backend/frontend + QA |
| WEB-14 | P1 para s7 / F/C | UI caja y estado cobrado no integrados | Apertura, líneas/medios/cargo/vuelto/pendiente; limitar acciones de cobradas | Mesa mixta parcial, turno correcto, vuelto neto, líneas protegidas, último cobro libera | Frontend/backend + cajero + QA |
| WEB-15 | P1 aceptación / V/C | Se registra impresión antes de papel; hardware pendiente | Distinguir intento/confirmación, probar cancelación/sin papel/copia/emisor | Acta HT-10 con impresora real y semántica explícita; cocina sin preparación duplicada | Frontend/infra + cocina + QA |
| WEB-16 | P2 / C/V | Modal/cajón no administra ciclo de foco | Patrón con foco inicial, Tab contenido, fondo inactivo y retorno | Recorrido solo teclado y lector conserva foco y errores; una confirmación | Frontend/UX + QA |
| WEB-17 | P2 / C/D | Tipos manuales/OpenAPI y 204 incongruentes | Contrato reproducible, DTO explícitos, generar en bootstrap/CI y retorno 204 honesto | Clone limpio, cambio DTO detectado, nullable seguro y 204 definido | Backend/frontend + QA contratos |
| WEB-18 | P3 / D | Pantallas/diseño/pendientes conservan estado anterior | Matriz vigente de rutas/permiso/entrega; enlazar enmiendas y design system real | Nuevo integrante ubica agotados y distingue implementado/futuro; no repite arreglos | Documentación + PO |

## Modelo, arquitectura, calidad y documentación

Evidencia: [informe 05](../auditorias/2026-10-11/05-modelo-datos-arquitectura.md) y [validaciones](../auditorias/2026-10-11/06-validacion-y-aceptacion.md).

| ID | Prioridad / tipo | Pendiente | Solución propuesta | Criterio mínimo para cerrar | Responsable sugerido |
|---|---|---|---|---|---|
| MOD-01 | P1 / E/C | ULID sobrescribe timestamp en posiciones 8/9 | Distribución 48+80 correcta, validación canónica y análisis de compatibilidad; conservar históricos | Decodificar fecha de IDs nuevos da instante de generación; alfabeto/overflow/case probados; FKs previas intactas | Backend contratos + QA |
| ARC-01 | P1 / C/V | Test de fronteras falla por ciclos de módulos | Puertos pequeños y seguridad transversal sin ciclos; no omitir test | `ArquitecturaDeModulosTest` y suite con código final pasan íntegros | Backend arquitectura + revisor |
| ARC-02 | P2 / F/D | Diseño local/offline no tiene perfiles/circuito completo | Mantener s10/s11; implementar perfiles/red, outbox/inbox, deduplicación y reconexión | Nodo arranca sin Internet y conserva operación; reconectar no duplica; frontera local/fiscal verificada | Backend/infra + QA, futuro |
| ARC-03 | P1 calidad / C | Integración usa DB habitual por defecto | Base efímera y perfil test; protección contra URL productiva | Suite limpia migrada desde cero no deja filas en desarrollo; resultado repetible | Backend/infra + QA |
| ARC-04 | P1 antes de piloto / C | Perfil local puede conservar semilla/secreto de desarrollo | Separar desarrollo de producción local; bootstrap seguro y guarda en entorno real | Arranque productivo local no acepta credenciales conocidas ni secreto fallback; admin real disponible | Infra/backend + QA |
| CAL-01 | P2 / C | Sin pipeline ni suite frontend observables | Pipeline con DB efímera, build/lint/contratos/arquitectura y recorridos críticos | Una instalación limpia reproduce controles y conserva evidencia por entrega | Infra + Claude + QA |
| DOC-01 | P2 / D | Resúmenes/modelo/IDs RN y estados contradictorios | Conciliar físico/lógico por fase y entrega con evidencia; incluye DOC-POS/CAJ del informe03 | Plan/estado/historia/modelo coinciden; V41 sigue en curso hasta aceptación; RN enlazadas correctas | Documentación + PO + backend |
| DOC-02 | P2 / D, corregido | Figuras del modelo inexistentes | **Hecho:** Mermaid en modelo/91 y nota objetivo/actual; referencias a este corte | Figuras `dg/d6.png`/`dg/d7.png` sustituidas; relaciones/diseño revisados, bloques presentes y enlaces comprobados; no se ejecutó renderer Mermaid | Documentación |
| DOC-03 | P2 / D | Guías/scripts contradicen comandos de arranque | Wrapper y terminal por módulo; diferenciar compilar/arrancar/probar | Compañero instala desde cero siguiendo una sola guía, sin errores ocultos ni comandos contradictorios | Documentación/infra |
| DOC-04 | P2 / D | Node mínimo documentado no satisface Vite instalado | Versiones exactas por package lock/engines y verificación previa | Versión admitida ejecuta build; una incompatible muestra explicación al iniciar guía/check | Documentación/infra |

Se añadió guía de lectura y conciliación, y se corrigió DOC-02 en esta sesión. DOC-01 sigue abierto donde los documentos originales aún necesitan actualizarse; añadir un informe no convierte todos sus textos anteriores en correctos. Los sub-ID `DOC-POS-01..04` y `DOC-CAJ-01..03` del informe de venta son evidencias particulares de DOC-01/02, no otras siete tareas obligatorias.

## Entregas futuras y preguntas que deben seguir visibles

| Asunto | Dónde pertenece | Condición / decisión pendiente |
|---|---|---|
| Descuentos/anulación autorizada, cierre de turno y reapertura | Sprint 7, entregas 3–5 | Seguir [plan aprobado](../producto/plan/sprint-07-cobrar-y-cerrar.md); no declarar completado por tener API de cobro |
| Pre-ticket, vinculación y abonos | Sprint 8 | Crédito/abono idempotentes; digital pendiente no cambia saldo |
| Compras, receta aplicada, producción y Kardex | Sprint 9 | Resolver snapshot y cascada antes; el proveedor sin compras no es una compra entregada |
| Sincronización y respaldo/restauración | Sprint 10 | Resolver IDs, reintentos, orden de FKs y conflictos; ensayo offline real |
| Fiscal y correo | Sprint 11 | P-01..05: datos del emisor, demo/proveedor y contador; esta sesión no certifica integración fiscal |
| Delivery/recojo, portal/cierre mensual y reportes | Sprints 12–14 | Conservar backlog; el siguiente compañero abordará implementación cuando corresponda |
| Precio real de menú/plan/táper, saldos iniciales, local/mesas/hardware | Cliente antes del piloto | Confirmar datos; los valores de desarrollo no son compromiso comercial |
| Desactivación de receta | P-32 | Acordar efecto en consumidores/oferta y luego implementar; no borrarla con historial |
| Tipo de entrega de la comanda y diseño táctil | Refinamiento s6/s7 | Cliente valida valores y criterio de 56 px/teclado, o enmienda registrada |

## Pendientes antiguos que ya no describen el código actual

| Asunto del README antiguo | Evidencia al corte | Acción documental |
|---|---|---|
| 500 por User-Agent largo | Normalización DT-061 y prueba existente | Marcar resuelto; FND-05 trata identidad/límites, un problema distinto |
| Bloquear mesa principal con uniones | `BloquearMesa` rechaza principal con unidas | Marcar resuelto; POS-02 trata carreras, no esa validación secuencial |
| No comprobar pedido cerrado | Casos revisados rechazan cerrado | Marcar resuelto; POS-01 sigue por carrera entre lectura y cierre |
| Falta grupo/pestañas de oferta | Grupo y pestañas presentes | Marcar resuelto; probar UX vigente |
| Pedido rápido no existe | Ruta y pantalla actuales presentes | Marcar resuelto; integración financiera sigue s7 |
| No probar comanda intacta al agotar | `ComandasIntactasAlAgotarTest` existe | Marcar cubierto por prueba al corte según resultado suite; POS-06 es mutabilidad de nombres/ubicación |

## Plantilla de cierre de un asunto

| Campo | Completar al implementar |
|---|---|
| ID y vínculo de historia v3/antigua | Mantener trazabilidad |
| Cambio y razón | Resumen entendible para revisor |
| Migración / contrato afectado | Nueva migración si corresponde; coordinar frontend |
| Verificación | Comando/caso, fecha, rol/local, resultado y evidencia |
| Limitación o decisión de cliente | No ocultar validaciones diferidas |
| Aceptación y estado final | QA verifica; PO/cliente acepta donde corresponda |

Un asunto que se difiere conserva prioridad, razón y dependencia. Una incidencia observada después del corte requiere nueva evidencia; esta lista representa el código disponible el 11/10/2026.
