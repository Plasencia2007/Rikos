# Auditoría integral de RIKOS — corte hasta sprint 7 parcial

Fecha: **11 de octubre de 2026**, America/Lima. Base Git de referencia: `9e6391b`; también se revisaron los cambios locales presentes al comenzar, incluidos cobro y V41. Responsable de implementación posterior: Claude y el equipo. Esta entrega modifica documentación Markdown.

## Dictamen

RIKOS tiene una base funcional valiosa para demostrar administración, recetas, menú, pensionistas y atención en mesa. **El circuito financiero completo todavía no reúne evidencia suficiente para entregarlo al cliente como terminado.** Sprint 6 conserva aceptación de hardware pendiente; sprint 7 tiene cajas/turnos y cobro en curso, con descuentos, conciliación y reapertura pendientes según su plan.

Las decisiones de modelado principales encajan con el negocio: cuenta corriente como libro, importes en céntimos, datos por local, versiones de tarifas y comanda como fotografía. Las prioridades son cerrar las brechas de autorización, consistencia concurrente y preservación histórica, y usar criterios de aceptación verificables para cerrar sprints.

## Alcance y forma de leer los resultados

| Tipo de evidencia | Significado |
|---|---|
| Confirmado por ejecución | Se ejecutó una comprobación y se conserva su resultado en [validaciones](06-validacion-y-aceptacion.md). |
| Confirmado por lectura | El recorrido de código o DDL demuestra la condición descrita; puede faltar reproducción HTTP. |
| Riesgo razonado | Intercalación, diseño o exposición plausible; necesita la prueba indicada antes de atribuir un incidente real. |
| Entrega prevista | Está en el backlog o en un sprint posterior; no se reporta como regresión de una entrega anterior. |
| No verificado | No se hizo prueba con navegador, hardware, datos reales o infraestructura de producción. |

La auditoría cubre los módulos presentes, contratos, gateway, esqueleto fiscal, frontend, modelo físico/lógico, documentación y método de sprints. Inventario, delivery, portal, kiosco, reportes y sincronización se revisan como límites y dependencias futuras; su implementación posterior queda fuera de esta sesión. No se ejecutaron transacciones contra la base habitual de desarrollo. No se hizo una auditoría de vulnerabilidades de terceros, una prueba de carga ni una certificación fiscal.

## Documentos entregados

| Documento | Qué resuelve |
|---|---|
| [01 — Producto, sprints y equipo](01-producto-sprints-y-equipo.md) | Cobertura por módulo, valor para el cliente, corte de sprints y propuesta de trabajo con Product Backlog. |
| [02 — Fundaciones, catálogo y pensionistas](02-fundaciones-catalogo-pensionistas.md) | Revisión de identidad, seguridad, auditoría, horarios, catálogo, recetas, menú y cuenta corriente. |
| [03 — Venta, POS y caja](03-venta-pos-caja.md) | Pedidos, mesas, comandas, reorganización, dinero, cierre y trabajo actual del sprint 7. |
| [04 — Frontend y experiencia del cliente](04-frontend-experiencia-cliente.md) | Superficies, permisos, errores, estados compartidos, accesibilidad y uso móvil. |
| [05 — Modelo de datos y arquitectura](05-modelo-datos-arquitectura.md) | Modelo lógico frente a migraciones, relaciones, contratos y arquitectura construida frente a prevista. |
| [06 — Validación y aceptación](06-validacion-y-aceptacion.md) | Checks ejecutados y escenarios que deben demostrar el equipo y el cliente. |
| [Registro central de pendientes](../../pendientes/02-auditoria-integral-sprints-0-7-2026-10-11.md) | Hallazgos con prioridades, solución propuesta, aceptación, responsables por función y dependencia. |
| [Guía de las carpetas de documentación](../../LEER-DOCUMENTACION.md) | Explica qué se guarda en cada carpeta y el orden de lectura. |

## Decisiones para el siguiente refinamiento

1. Llevar los hallazgos de prioridad P0/P1 al refinamiento inmediato; no declarar un sprint financiero aceptado mientras afecten su recorrido crítico.
2. Completar el recorrido de un cajero y un mozo con permisos reales, sin asumir que las pruebas del administrador representan a todos.
3. Conciliar la documentación de estado con el código actual; conservar los pendientes antiguos como historial y usar el registro nuevo para esta revisión.
4. Preparar una demostración del almuerzo con mesa mixta, pago dividido, cargo al pensionista, impresión, conciliación y recuperación de errores. El informe de aceptación lo firma el cliente cuando se ejecute.

P0 significa riesgo de privilegios o acceso crítico; P1 afecta dinero, historial, operación central o controles necesarios para aceptar la entrega; P2 cubre robustez y claridad; P3 es una mejora menor. Prioridades propuestas por la auditoría, sujetas al refinamiento del equipo. No equivalen a la clasificación MoSCoW del Product Backlog.
