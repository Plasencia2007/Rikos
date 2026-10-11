# Validación ejecutada y aceptación por preparar

Fecha: 11/10/2026, America/Lima. Se verifica el árbol de trabajo disponible, no solo el commit: incluye los archivos de cobro/V41 sin confirmar existentes al iniciar. No se modificó código de producto ni se confirmó ningún commit.

## Entorno y protección de datos

| Elemento | Entorno utilizado |
|---|---|
| JDK | Eclipse Adoptium 21.0.11; `JAVA_HOME` fijado solo para el proceso Maven. El `java` del PATH habitual devolvía Java 8, una diferencia operativa a incluir en la guía. |
| Node | v24.16.0; dependencias ya instaladas del workspace |
| Frontend | TypeScript por `tsc -b` dentro de `npm run build`, Vite 8.3.0 y ESLint del proyecto |
| PostgreSQL | Imagen local `postgres:17-alpine`, contenedor temporal `rikos-auditoria-20261011`, puerto solo loopback 55433 |
| Base de tests | `rikos_auditoria`, creada vacía; `DB_URL`, `DB_USER` y `DB_PASS` se sustituyeron para el proceso de test |
| Base habitual | Los checks no hicieron transacciones contra `rikos_operacion` habitual. No se limpiaron pedidos, pensionistas ni filas del usuario. |
| Evidencia | Resultado de comandos y consultas, fuentes citadas, reports Surefire; se registran resúmenes aquí. Los probes Java se ejecutaron fuera del repositorio. |

## Resultados

| Comprobación | Resultado del corte | Qué demuestra / límite |
|---|---|---|
| `npm run build` | **PASS**, código 0; 2031 módulos transformados; compilación Vite finalizada | TypeScript y bundle disponibles con las dependencias/tipos presentes. No demuestra instalación limpia, UI en navegador ni funciones fiscales/offline. |
| `npm run lint` | **PASS con 5 warnings**, 0 errores, código 0 | Reglas ESLint del árbol actual. No corrige advertencias ni prueba accesibilidad. |
| Contratos, dentro del reactor Maven | **22 tests, 0 fallos, 0 errores, 0 omitidos** | Pruebas existentes de Dinero/Resultado/Identificador; no detectan MOD-01. |
| Flyway V1–V41 desde cero | **41 migraciones aplicadas**, última V41 `success=true`; **52 tablas** sin contar `flyway_schema_history` | Esquema actual, incluido WIP V41, puede crearse desde vacío. No prueba upgrade de una copia con datos reales ni migración histórica de todo el producto. |
| Suite operación/arquitectura | **Resultado final pendiente al redactar; se actualiza al terminar la ejecución** | No contabilizar reports antiguos como si fueran de esta corrida. |
| MOD-01, decodificación de ULID | **Defecto reproducido con clase Java compilada** | Timestamp de diez caracteres fuera del intervalo real de generación; detalle y propuesta en informe05. |
| FND-06, resolver de servicio | **Defecto reproducido con clase real y repositorios dobles** | Cena lunes 19:00–martes 00:30: resolver devuelve cena lunes 00:15 y vacío martes 00:15. No se ejecutó una venta real a medianoche. |
| FND-07, errores HTTP | **Tres casos reproducidos con MockMvc standalone y manejador real** | `@Valid` blank, JSON roto y tipo int inválido responden 500; control válido responde 200. No reproduce todos los endpoints ni cada colisión SQL. |
| CAT-03, cascada | **Simulación del recorrido reproducida** | Ambas ramas producen C=4/D=3 frente a D=4 esperado. Falta prueba integrada del algoritmo Java con entidades reales. |
| Carreras de pedido/mesas/cobro | **No ejecutadas con hilos/conexiones independientes** | POS-01/02, CAJ-03 y similares siguen como riesgo razonado; los tests secuenciales/dobles no los certifican. |
| Navegador/dispositivos/impresora | **No verificado en esta sesión** | Las historias contienen verificaciones históricas. HT-10 y recorridos por rol deben comprobarse con hardware/instancia. |
| Producción/offline/fiscal/restauración/carga | **No verificado** | Dominios posteriores o infraestructura externa; no se afirma disponibilidad, certificación fiscal ni rendimiento medido. |

### Advertencias de lint

| Archivo | Línea | Advertencia |
|---|---:|---|
| `rikos-web/src/features/autenticacion/PantallaPin.tsx` | 69 | Falta dependencia `intentar` del efecto; relacionar con WEB-10 |
| `rikos-web/src/features/locales/PestanaUbicacion.tsx` | 97 | Falta dependencia `onCambiar` |
| `rikos-web/src/features/pensionistas/PantallaParametrosDeCobro.tsx` | 79 | Falta dependencia `cargarUltimoCambio` |
| `rikos-web/src/features/salones/PantallaSalones.tsx` | 43 | Expresión compleja en dependencias |
| `rikos-web/src/features/usuarios/PantallaUsuarioFormulario.tsx` | 111 | Directiva eslint-disable innecesaria |

El pendiente antiguo «lint roto por `_clave`» no se reprodujo. No volver a implementarlo sin nueva evidencia; registrar advertencias vigentes en consolidación del frontend.

### Reproducir los checks generales

Usar una PostgreSQL aislada, verificar que acepta conexiones y exportar sus datos de conexión **en esa terminal**. No correr el comando siguiente con el `DB_URL` habitual de desarrollo.

```powershell
# En rikos-backend, con JAVA_HOME apuntando al JDK 21 y DB_* a la base efímera:
.\mvnw.cmd -pl contratos,servicio-operacion -am test

# En rikos-web, con dependencias y tipos disponibles:
npm run lint
npm run build
```

Los probes FND-06/FND-07 tienen clases mínimas, classpath y comandos en [informe02](02-fundaciones-catalogo-pensionistas.md). La clase compilada de contratos se inspeccionó con JShell/JDK21 y decodificación Crockford Base32 de los primeros diez caracteres. Resultado concreto: rango real `1791751051459..1791751051623`, timestamp decodificado `1791751051316`; el problema se sigue además directamente en los bucles de `Identificador.generar`.

## Guion de aceptación del recorrido hasta sprint 7

Este guion se ejecutará cuando Claude termine las entregas seleccionadas. No representa pruebas realizadas ni aprobación del cliente.

| Caso | Escenario y actores | Resultado esperado | Evidencia para cliente/socios |
|---|---|---|---|
| A-01 | Central configura un local real, horarios y trabajadores; admin local intenta elevarse | Ámbito y permisos respetados, PIN identifica a una persona, desactivación corta acceso | Matriz por rol y registro de autorización/rechazo |
| A-02 | Cocina marca plato agotado; mozo con otro dispositivo consulta | Oferta actualizada sin modificar comanda previa; reactivar con motivo | Pantallas antes/después y bitácora del local |
| A-03 | Administrador cambia costo de X usado en dos subrecetas que convergen | Todas las consumidoras muestran costo correcto; sin permiso no se ve costo | Tabla aritmética esperada frente a resultado |
| A-04 | Mozo abre mesa con regular y pensionista, agrega comida y táper según canal | Persona/tarifa correctas, precio servidor, saldo a favor/deuda entendibles | Pedido con líneas y reglas tarifarias aplicadas |
| A-05 | Dos mozos editan/comandan/reorganizan mismo pedido | Estado coherente, conflicto visible, sin línea/comanda duplicada | Prueba con dos conexiones y dos dispositivos |
| A-06 | Comanda, cancelación de impresión, sin papel y reimpresión | Semántica intento/éxito clara; copia identificada; emisor/personas/componentes correctos | Acta con impresora real y cocina |
| A-07 | Mover/unir/separar/trasladar; dos uniones cruzadas | No ciclo, solo mesas permitidas, precio según regla y foto histórica estable | Antes/después y registro de movimiento |
| A-08 | Cajero abre turno con fondo S/100; otro intenta misma caja | Solo un turno abierto por caja/cajero; error explicable | Turno, actor, caja/local y prueba concurrente |
| A-09 | Línea S/12; efectivo recibido S/20 | Neto caja S/12, vuelto S/8, esperado S/112 con fondo; no restar vuelto dos veces | Resumen de cobro/pago/movimiento reconciliados |
| A-10 | Mesa mixta, pago efectivo/Yape y cargo al pensionista; cobro parcial | Medios+cargos=líneas; cargo único; persona saldada visible; mesa ocupada hasta final | Libro cuenta y caja con mismo origen |
| A-11 | Últimas dos líneas cobradas simultáneamente; respuesta de una se pierde | Cierre correcto y reintento devuelve mismo resultado, sin duplicar | Resultado de dos transacciones y recuperación del cliente |
| A-12 | Fallo tardío tras primer pago/cargo | Rollback completo de cobro, líneas, pagos, snapshots y libro | Consulta independiente de todas las tablas implicadas |
| A-13 | Editar receta/precio después de cobrar | Precio, ingredientes, unidades, costo y utilidad históricos estables | Snapshot antes/después, sin consultar maestro mutable |
| A-14 | Cerrar turno con diferencia S/0,01, cuando entrega 4 esté implementada | Conciliación y alerta, cierre permitido conforme al plan; motivo/datos auditados | Cierre y cifras esperadas/contadas |
| A-15 | Reabrir dentro del turno y corregir después, cuando entrega 5 esté implementada | Historial preservado y operaciones compensatorias según plan | Libro de hechos y autorización/motivo |
| A-16 | API del local inaccesible, 401, 403 y guardado parcial | No confundir vacío con fallo; autenticación/contexto/borrador recuperables | Recorrido de recuperación con estado visible |
| A-17 | Móvil 375/768 px, escritorio 1280 px y solo teclado | Acciones críticas utilizables, criterio táctil vigente y foco de diálogos | Medidas/capturas y prueba con mozo de referencia |

Usar precios y pensionistas de prueba identificados, sin presentarlos como datos comerciales reales. Para el piloto se acuerdan precios, saldos y hardware con el propietario. No fijar métricas inventadas: medir tiempo de atención/cobro y errores en una primera prueba, acordar objetivo y repetir con el equipo.

## Plantilla de acta de demostración

| Campo | Registrar |
|---|---|
| Entrega / sprint / historia | ID vigente y antiguo, versión/commit realmente mostrado |
| Fecha / local / configuración | Fecha Lima, datos de prueba o reales, impresora/dispositivo |
| Participantes | Cliente, cajero/mozo/cocina, PO y QA; socios si participan |
| Casos ejecutados | A-NN y hallazgos asociados |
| Resultado | Pasó/falló/no ejecutado, cifras y evidencia |
| Observaciones | Problema entendido por actor, impacto y pendiente vinculado |
| Aceptación | Aceptado, aceptado con condiciones o pendiente; persona que decidió |

La cantidad de tests y el cierre documental de un sprint no sustituyen esta aceptación. Los hallazgos permanecen en [pendientes](../../pendientes/02-auditoria-integral-sprints-0-7-2026-10-11.md) hasta verificar su criterio específico.
