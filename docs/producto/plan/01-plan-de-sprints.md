<!-- Extraído del Product Backlog v2.0 (RIKOS-product-backlog.md, 2026-09-11), líneas 117-288. Distribuido por paso del método el 2026-09-29, ver docs/METODO-spec-driven.md. -->

# 4. Plan de sprints

## 4.1 Un primer sprint sin nada que mostrar

El orden original arranca con las siete historias de la épica de fundaciones: locales, roles, permisos, usuarios, autenticación, auditoría y horarios.

Al terminar ese sprint no existe una sola pantalla que haga algo del negocio. Para un equipo que está aprendiendo la tecnología y para un propietario que espera ver avances, un primer sprint sin demostración es un riesgo real: de motivación, y de validación temprana de que se va por buen camino.

**Principio corregido:** cada sprint termina con algo que se puede enseñar.

## 4.2 Los permisos granulares, demasiado pronto

US-004 pide un sistema de permisos por módulo y acción, con once acciones configurables. La especificación lo exige (RN-012) y no está en discusión **qué** hay que construir.

El problema es **cuándo**. En el primer sprint no existe ningún módulo, así que no existen las acciones sobre las que esos permisos aplican. Construir el motor de permisos ahí significa diseñarlo sobre supuestos y descubrir después que no encaja con la superficie real.

**Corrección:** en el arranque, roles fijos y simples. El motor granular entra cuando ya hay dos o tres módulos y se conoce la superficie de acciones de verdad.

## 4.3 La auditoría transversal, por el mismo motivo

US-006 es una capacidad que consumen eventos de otros módulos: descuentos, anulaciones, ajustes, transferencias, cierres. Ninguno de esos eventos existe en el primer sprint, así que la auditoría se podría construir pero no probar.

**Corrección:** entra junto con el primer módulo que genera acciones auditables, no antes.

## 4.4 Un factor que no es del backlog

El equipo está aprendiendo al mismo tiempo Spring Boot 4, Spring Modulith, Docker, una arquitectura por módulos y un frontend nuevo. El primer sprint se va en pelear con las herramientas, no con el dominio.

Eso no se arregla reordenando historias: se arregla reconociendo que existe un **Sprint 0** cuyo objetivo es que las herramientas dejen de estorbar.

---


## 4.5 La secuencia

**Supuesto de análisis.** El backlog advierte que `README.md` declara todo el alcance como primera versión, así que cualquier secuencia es una recomendación, no un requisito. Lo que sigue ordena las 61 historias de negocio más las 10 habilitantes según tres criterios: que cada sprint termine con algo demostrable, que ninguna abstracción se construya antes de conocer su superficie real, y que las decisiones caras de revertir se tomen primero.

La duración de cada sprint no se fija aquí: depende del tamaño y la disponibilidad del equipo. Lo que importa es el orden y el corte.

| Sprint | Objetivo | Contenido | Qué se puede demostrar al final |
|---|---|---|---|
| **0** | Que las herramientas dejen de estorbar | HT-01, HT-02, HT-03 | El navegador llama al backend a través del gateway y muestra datos que vienen de la base |
| **1** | Entrar al sistema | US-001, US-005, US-002, US-003 · roles fijos, sin motor de permisos | Login real, cambio rápido de mozo por PIN, y el local activo visible |
| **2** | El primer módulo del negocio | US-008, US-009, US-011 · HT-05, HT-07 (en el backlog v3 es **US-01**, ver `04-comparacion-backlog-v3.md`) | Alta y edición de productos, unidades y proveedores desde el panel |
| **3** | Control y trazabilidad, ya con superficie real | US-004, US-006, US-012, US-007 · HT-06 | Un usuario sin permiso no ve el botón; la bitácora registra quién cambió qué |
| **4** | Costos y menú | US-010, US-014, US-013, US-015 | Una receta con subrecetas mostrando su costo por porción, y la semana de menú programada |
| **5** | La cuenta del pensionista | US-017, US-016, US-018 | Alta de pensionista con su plan y su responsable, y su cuenta abierta |
| **6** | Vender en mesa | US-027, US-028, US-030, US-029 · HT-10 | Tomar un pedido en mesa mixta e imprimir la comanda en la comandera real |
| **7** | Cobrar y cerrar caja | US-032, US-033, US-034, US-035, US-036 | Ciclo completo: pedido, cobro dividido, cierre parcial y cierre de turno con conciliación |
| **8** | El circuito del pensionista, completo | US-019, US-020, US-021, US-024 | El caso crítico 1 de `07`, punta a punta: kiosco, pre-ticket, mozo, caja, cargo |
| **9** | Existencias reales | US-043, US-050, US-044, US-048, US-045, US-049, US-051 | Una venta descuenta la receta con subrecetas y el Kardex lo explica |
| **10** | Que el local no dependa de Internet | HT-09, US-058, US-059, US-061, US-060 · HT-04 ya en uso | Se corta la conexión, el local sigue vendiendo, vuelve y todo sincroniza sin duplicar |
| **11** | Comprobantes | US-037 y la historia nueva de envío por correo | Boleta emitida, aceptada y enviada al cliente |
| **12** | Canales no presenciales | US-038, US-039, US-040, US-041, US-042 | Un delivery desde el registro hasta la liquidación del motorizado |
| **13** | Autoservicio del pensionista y cierre mensual | US-022, US-023, US-025, US-026 | El pensionista consulta su cuenta desde el celular y recibe su estado mensual |
| **14** | Visibilidad | US-052 a US-057, US-046, US-047 | El panel con sus filtros y los reportes exportables |

## 4.6 Los tres cambios de fondo respecto del orden original

**US-004 y US-006 se mueven del sprint 1 al sprint 3.** Cuando entran, ya existen productos, unidades y proveedores: hay acciones reales que permitir o negar, y hay cambios reales que auditar. El motor de permisos se diseña contra una superficie conocida en vez de contra una imaginada.

**Aparece un sprint 0 que no estaba.** No contiene ninguna historia de negocio y es, probablemente, el de mayor retorno: resuelve de una vez los problemas de entorno que de otro modo aparecerían disfrazados de "la historia tardó más de lo estimado" durante los primeros tres sprints.

**El modo sin conexión sube al sprint 10, antes de facturación y de los canales no presenciales.** El orden original lo dejaba más adelante. Conviene adelantarlo porque condiciona cómo se implementa todo lo que viene después, y porque HT-04 —el outbox, que ya se usa desde el sprint 0— es su cimiento.

## 4.7 Lo que sigue bloqueado

El sprint 11 depende de decisiones que no son del equipo técnico: los datos fiscales de la empresa, las tres pruebas en la cuenta demo del proveedor y la consulta al contador. Están listadas como P-01 a P-05 en el documento de decisiones.

**Recomendación:** moverlas ahora, no en el sprint 10. Son consultas a terceros y tardan en volver.

## 4.8 Una historia que merece tratamiento aparte

US-028, la toma de pedido con asignación por persona en mesa mixta, está en el sprint 6. Es la pantalla más difícil del sistema y la que decide si los mozos usan el sistema o vuelven al papel.

**Sugerencia:** antes de ese sprint, hacer bocetos y probarlos con un mozo real. Media jornada de trabajo que puede ahorrar una reescritura completa.

---


---

# 5. Product Backlog priorizado

> **Supuesto de análisis sobre la priorización.** `README.md` declara que *todo* el alcance listado pertenece a la primera versión. Por lo tanto, la clasificación MoSCoW y la separación MVP / Post-MVP que aparece a continuación **no proviene de los documentos**: es una recomendación de secuenciación técnica y de valor operativo elaborada por el analista, pensada para que RIKO'S pueda salir a producción por etapas. Debe validarse con el propietario antes de comprometer un plan de releases. Ninguna historia marcada como Post-MVP está fuera del alcance funcional acordado.


## 5.1 Tabla resumen, ordenada por sprint

Las 61 historias de negocio más las 10 habilitantes técnicas, en el orden en que conviene construirlas.


| Sprint | Orden | ID | Épica | Historia | Prioridad | Est. |
|---|---:|---|---|---|---|---|
| **0** | 1 | HT-01 | EP-11 | Esqueleto que camina | Must | M |
|  | 2 | HT-02 | EP-11 | Base de datos y convenciones de migración | Must | S |
|  | 3 | HT-03 | EP-11 | Tipos base del dominio | Must | S |
| **1** | 4 | US-001 | EP-01 | Administrar empresa y locales | Must | S |
|  | 5 | US-005 | EP-01 | Gestionar usuarios y su asignación multilocal | Must | M |
|  | 6 | US-002 | EP-01 | Autenticar usuarios con usuario y contraseña | Must | M |
|  | 7 | US-003 | EP-01 | Acceder como mozo mediante PIN individual | Must | S |
| **2** | 8 | US-008 | EP-02 | Administrar el catálogo central de productos | Must | M |
|  | 9 | US-009 | EP-02 | Definir unidades base, presentaciones y conversiones | Must | M |
|  | 10 | US-011 | EP-02 | Administrar el catálogo de proveedores | Must | S |
|  | 11 | HT-05 | EP-11 | Generación de tipos desde OpenAPI | Must | S |
|  | 12 | HT-07 | EP-11 | Componentes base y diseño táctil (v3: US-01 "Operar el POS con una sola mano", con 3 criterios; CA1 de 56 px y CA3 del teclado único **abiertos**) | Must | M |
| **3** | 13 | US-004 | EP-01 | Definir roles y permisos granulares | Must | M |
|  | 14 | US-006 | EP-01 | Registrar auditoría transversal de acciones sensibles | Must | M |
|  | 15 | US-012 | EP-02 | Configurar listas de motivos del sistema | Must | XS |
|  | 16 | US-007 | EP-01 | Configurar horarios de servicio por local | Must | S |
|  | 17 | HT-06 | EP-11 | Verificación automática de fronteras | Must | S |
|  | 18 | HT-08 | EP-11 | Observabilidad mínima | Should | S |
| **4** | 19 | US-010 | EP-02 | Crear recetas y subrecetas valorizadas por porción | Must | XL |
|  | 20 | US-014 | EP-03 | Configurar el menú normal y el menú extra | Must | M |
|  | 21 | US-013 | EP-03 | Programar el menú semanal del almuerzo | Must | L |
|  | 22 | US-015 | EP-03 | Marcar productos agotados y reactivarlos | Must | S |
| **5** | 23 | US-017 | EP-04 | Administrar planes tarifarios y precios especiales | Must | L |
|  | 24 | US-016 | EP-04 | Registrar la ficha del pensionista y su responsable | Must | M |
|  | 25 | US-018 | EP-04 | Mantener la cuenta corriente del pensionista | Must | L |
| **6** | 26 | US-027 | EP-05 | Visualizar y operar el mapa de mesas | Must | M |
|  | 27 | US-028 | EP-05 | Tomar el pedido asignando productos por persona | Must | XL |
|  | 28 | US-030 | EP-05 | Emitir la comanda a la impresora central de cocina | Must | L |
|  | 29 | US-029 | EP-05 | Unir, mover, dividir y trasladar cuentas y productos | Must | L |
|  | 30 | US-031 | EP-05 | Anular productos antes y después de comandar | Must | M |
|  | 31 | HT-10 | EP-11 | Prueba con hardware real | Must | S |
| **7** | 32 | US-032 | EP-06 | Abrir el turno de caja con fondo inicial | Must | S |
|  | 33 | US-033 | EP-06 | Cobrar la cuenta con medios divididos y cierres parciales | Must | XL |
|  | 34 | US-034 | EP-06 | Aplicar descuentos y anulaciones con autorización y motivo | Must | M |
|  | 35 | US-035 | EP-06 | Reabrir una venta dentro del turno o compensarla después | Must | M |
|  | 36 | US-036 | EP-06 | Cerrar el turno de caja con conciliación y alertas | Must | L |
| **8** | 37 | US-019 | EP-04 | Emitir el pre-ticket de autoservicio del pensionista | Must | L |
|  | 38 | US-020 | EP-04 | Vincular el pre-ticket al pedido del pensionista | Must | M |
|  | 39 | US-021 | EP-04 | Registrar abonos del pensionista en caja | Must | M |
|  | 40 | US-024 | EP-04 | Aprobar o rechazar abonos digitales | Must | M |
| **9** | 41 | US-043 | EP-08 | Mantener existencias por local con stock mínimo y alertas | Must | M |
|  | 42 | US-050 | EP-08 | Registrar compras y actualizar el costo de última compra | Must | L |
|  | 43 | US-044 | EP-08 | Consultar el Kardex con origen rastreable | Must | L |
|  | 44 | US-048 | EP-08 | Producir subrecetas y descontar receta al vender | Must | XL |
|  | 45 | US-045 | EP-08 | Registrar ajustes de inventario con motivo | Must | M |
|  | 46 | US-049 | EP-08 | Registrar el destino de los sobrantes de producción | Should | S |
|  | 47 | US-051 | EP-08 | Registrar el consumo interno del personal | Should | M |
| **10** | 48 | HT-04 | EP-11 | Patrón outbox | Must | M |
|  | 49 | HT-09 | EP-11 | Imágenes y perfiles de despliegue | Must | M |
|  | 50 | US-058 | EP-10 | Operar el local completo sin conexión a Internet | Must | XL |
|  | 51 | US-059 | EP-10 | Sincronizar sin duplicados y resolver conflictos | Must | XL |
|  | 52 | US-061 | EP-10 | Proteger credenciales, sesiones y adjuntos | Must | M |
|  | 53 | US-060 | EP-10 | Respaldar la información en tiempo real | Must | L |
| **11** | 54 | US-037 | EP-06 | Emitir comprobantes electrónicos por proveedor API | Must | XL |
|  | 55 | US-062 | EP-06 | Entregar el comprobante al cliente por correo | Should | M |
| **12** | 56 | US-038 | EP-07 | Registrar pedidos de recojo programado | Should | L |
|  | 57 | US-039 | EP-07 | Gestionar el recojo preparado y no retirado | Should | M |
|  | 58 | US-040 | EP-07 | Registrar el pedido de delivery y asignar motorizado | Should | L |
|  | 59 | US-041 | EP-07 | Operar la vista móvil del motorizado | Should | L |
|  | 60 | US-042 | EP-07 | Liquidar al motorizado y cerrar la venta de delivery | Should | M |
| **13** | 61 | US-022 | EP-04 | Consultar el portal compartido del pensionista | Should | L |
|  | 62 | US-023 | EP-04 | Reportar un abono digital desde el portal | Should | M |
|  | 63 | US-025 | EP-04 | Generar el cierre mensual y el estado de cuenta | Must | L |
|  | 64 | US-026 | EP-04 | Desactivar y reactivar a un pensionista | Should | S |
| **14** | 65 | US-052 | EP-09 | Consultar el panel del administrador con filtros | Should | L |
|  | 66 | US-053 | EP-09 | Reportar ventas, costo histórico, utilidad y margen | Should | L |
|  | 67 | US-054 | EP-09 | Reportar cierres y diferencias de caja | Should | M |
|  | 68 | US-055 | EP-09 | Reportar pensionistas, saldos, estados y abonos | Should | M |
|  | 69 | US-056 | EP-09 | Reportar inventario, Kardex y producción | Should | M |
|  | 70 | US-057 | EP-09 | Reportar delivery, anulaciones y consumo interno | Could | M |
|  | 71 | US-046 | EP-08 | Transferir productos entre locales | Should | M |
|  | 72 | US-047 | EP-08 | Ejecutar conteos físicos sin detener la operación | Should | XL |


US-031 aparece en el sprint 6 porque el mozo necesita poder anular un producto desde el primer día que toma pedidos. Su efecto sobre el inventario —descontar la receta como pérdida cuando ya se comandó— solo queda comprobable en el sprint 9, cuando existe el Kardex.

**Distribución:** 72 elementos — 61 historias de negocio de la versión 1.0, más US-062 (envío del comprobante por correo) y las 10 habilitantes técnicas.

