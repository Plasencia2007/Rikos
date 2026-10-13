# Plan de Implementación: US-048 — Producir subrecetas y descontar receta al vender

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implementar el módulo de producción en cocina (registro de producción de subrecetas con rendimiento, salida de insumos y entrada de subreceta al Kardex, actualización de costo real, seguimiento de porciones planificadas/preparadas/vendidas) y el descuento automático de recetas en venta al enviar comanda (restando subrecetas como productos sin duplicar consumo de insumos base per Caso Crítico 11).

**Architecture:** 
- Backend Spring Boot (`servicio-operacion`):
  - Migración Flyway `V53__produccion.sql` (tabla `produccion`, índices y permisos `produccion.registrar`, `produccion.ver`).
  - Entidad JPA `Produccion`, repositorio `ProduccionRepositorio`.
  - Caso de uso `RegistrarProduccion`: descuenta insumos escalados por `rendimiento_obtenido / receta.rendimiento`, ingresa la subreceta producida al inventario (`origen = 'produccion'`), calcula y actualiza costo unitario real en `producto`.
  - Caso de uso `ConsultarProducciones`: consulta de producciones por local, fecha y servicio con trazabilidad de porciones.
  - Caso de uso `DescontarInventarioPorComanda`: descuenta ingredientes de receta al emitir comanda (`origen = 'venta'`), descontando subrecetas de su stock sin re-descontar sus insumos (CA2 / Caso Crítico 11), y reversión en anulación de comanda (`origen = 'anulacion_post_comanda'`).
  - Controlador `ProduccionController` protegido con Spring Security (`PERM_produccion.ver`, `PERM_produccion.registrar`).
- Frontend React (`rikos-web`):
  - Tipos y llamadas API en `cliente.ts`.
  - Pantalla `PantallaProduccion.tsx` (ADM-19 en `/admin/produccion`), activada en `BarraLateral.tsx` y `apps/admin/index.tsx`.
  - Modal de registro con previsualización reactiva de insumos a consumir, stock actual, saldo proyectado y alertas de saldo negativo/mínimo.

**Tech Stack:** Java 21, Spring Boot 3.3, Spring Data JPA / Hibernate, PostgreSQL 16, Spring Security, TypeScript, React 18, Vanilla CSS con tokens de Riko's, Lucide Icons, Playwright.

**Spec & Decisiones Acordadas:**
- `docs/producto/flujos/08-ep08-inventario-produccion-compras.md` (§ US-048, RN-047, RN-048, RN-057, RN-043).
- **D1 (Descuento de venta):** Al emitir comanda a cocina (`origen = 'venta'`), con reversión en anulación (`origen = 'anulacion_post_comanda'`).
- **D2 (Porciones planificadas):** Precarga automática desde la programación semanal del menú para ese día/servicio.
- **D3 (Mando de insumos):** El `rendimiento_obtenido` manda el factor de escala proporcional de insumos consumidos (`factor = rendimiento_obtenido / receta.rendimiento`).
- **D4 (Stock negativo):** Permitido con alerta per RN-057, sin bloquear operaciones de cocina ni comandas.
- **D5 (Costeo real):** Costo unitario real de la subreceta (`costo_total_insumos / rendimiento_obtenido`) registrado en Kardex y actualizado como costo de referencia en `producto`.
- **D6 (UI Producción ADM-19):** Historial con seguimiento de porciones (planificadas, preparadas, vendidas per CA3) y modal con simulación en tiempo real de consumos.

---

## Global Constraints
- Cada movimiento en el Kardex originado por producción lleva `origen = 'produccion'`, `referencia_tipo = 'produccion'` y `referencia_id = produccion.id`.
- Para venta, lleva `origen = 'venta'`, `referencia_tipo = 'pedido'` y `referencia_id = pedido.id` (o comanda).
- La venta descuenta directamente la subreceta de su inventario si la receta la incluye; nunca re-explota insumos consumidos previamente.
- No se calcula automáticamente lista de compras/insumos desde la planificación (RN-048, CA4).
- Respetar aislamiento estricto por local (`local_id`) per RN-002.

---

### Task 1: Migración Flyway `V53__produccion.sql` y Permisos

**Files:**
- Create: `rikos-backend/servicio-operacion/src/main/resources/db/migration/V53__produccion.sql`

- [ ] **Step 1:** Crear tabla `produccion` con columnas: `id`, `local_id`, `producto_id`, `receta_id`, `fecha`, `servicio`, `porciones_planificadas`, `porciones_preparadas`, `porciones_vendidas`, `rendimiento_obtenido`, `sobrante`, `destino_sobrante`, `motivo_id`, `estado`, `registrado_por`, `creado_en`.
- [ ] **Step 2:** Crear índices para optimizar búsquedas por `local_id`, `fecha`, `producto_id`.
- [ ] **Step 3:** Insertar permisos `produccion.registrar` y `produccion.ver` y asignarlos a roles `admin` y `cocina`.
- [ ] **Step 4:** Validar migración ejecutando `mvn test -pl servicio-operacion -Dtest=KardexTest`.

---

### Task 2: Entidad JPA `Produccion` y Repositorios

**Files:**
- Create: `rikos-backend/servicio-operacion/src/main/java/pe/rikos/operacion/produccion/dominio/Produccion.java`
- Create: `rikos-backend/servicio-operacion/src/main/java/pe/rikos/operacion/produccion/infraestructura/ProduccionRepositorio.java`
- Create: `rikos-backend/servicio-operacion/src/main/java/pe/rikos/operacion/produccion/aplicacion/dto/RegistroProduccionComando.java`
- Create: `rikos-backend/servicio-operacion/src/main/java/pe/rikos/operacion/produccion/aplicacion/dto/DetalleProduccionDTO.java`

- [ ] **Step 1:** Implementar entidad `Produccion` con validaciones de negocio e invariantes.
- [ ] **Step 2:** Implementar repositorio `ProduccionRepositorio` con métodos de consulta por local, fecha y servicio.
- [ ] **Step 3:** Implementar DTOs `RegistroProduccionComando` y `DetalleProduccionDTO`.
- [ ] **Step 4:** Verificar compilación del backend.

---

### Task 3: Caso de Uso `RegistrarProduccion` y Pruebas Unitarias (TDD)

**Files:**
- Create: `rikos-backend/servicio-operacion/src/main/java/pe/rikos/operacion/produccion/aplicacion/RegistrarProduccion.java`
- Create: `rikos-backend/servicio-operacion/src/test/java/pe/rikos/operacion/produccion/aplicacion/RegistrarProduccionTest.java`

- [ ] **Step 1:** Escribir pruebas unitarias en `RegistrarProduccionTest.java` verificando:
  - Salida de insumos escalados según `rendimiento_obtenido / receta.rendimiento`.
  - Entrada de la subreceta producida con el rendimiento real.
  - Actualización de stock en `Existencia` de cada insumo y de la subreceta.
  - Generación de movimientos en Kardex con `origen = 'produccion'` y referencia cruzada.
  - Cálculo de costo unitario real y actualización en `producto`.
  - Comportamiento ante insumo con stock insuficiente (pasa a saldo negativo per RN-057 sin error).
- [ ] **Step 2:** Ejecutar test y verificar que falle antes de la implementación.
- [ ] **Step 3:** Implementar `RegistrarProduccion.java` con lógica transaccional completa.
- [ ] **Step 4:** Ejecutar pruebas y confirmar BUILD SUCCESS.

---

### Task 4: Caso de Uso `DescontarInventarioPorComanda` (CA2 / Caso Crítico 11)

**Files:**
- Create: `rikos-backend/servicio-operacion/src/main/java/pe/rikos/operacion/inventario/aplicacion/DescontarInventarioPorComanda.java`
- Create: `rikos-backend/servicio-operacion/src/test/java/pe/rikos/operacion/inventario/aplicacion/DescontarInventarioPorComandaTest.java`
- Modify: `rikos-backend/servicio-operacion/src/main/java/pe/rikos/operacion/venta/aplicacion/EnviarComanda.java` (o handler de evento de comanda emitida)

- [ ] **Step 1:** Escribir pruebas unitarias en `DescontarInventarioPorComandaTest.java` verificando:
  - Descuento de ingredientes directos de la receta al vender un plato.
  - Si un ingrediente es subreceta (`es_subreceta = true`), descuenta la subreceta de su existencia y NO re-descuenta los insumos base (CA2).
  - Si no tiene receta (ej. refresco comprado o producto sin receta), descuenta directo si es insumo/producto con control de stock.
  - Reversión por `anulacion_post_comanda` reponiendo las cantidades exactas al Kardex.
- [ ] **Step 2:** Ejecutar test y verificar que falle.
- [ ] **Step 3:** Implementar `DescontarInventarioPorComanda.java` e integrarlo al flujo de comanda/venta.
- [ ] **Step 4:** Ejecutar pruebas y verificar BUILD SUCCESS.

---

### Task 5: Consultas de Producción y Controlador REST

**Files:**
- Create: `rikos-backend/servicio-operacion/src/main/java/pe/rikos/operacion/produccion/aplicacion/ConsultarProducciones.java`
- Create: `rikos-backend/servicio-operacion/src/main/java/pe/rikos/operacion/produccion/api/ProduccionController.java`
- Modify: `rikos-backend/servicio-operacion/src/main/java/pe/rikos/operacion/comun/seguridad/ConfiguracionSeguridad.java`
- Create: `rikos-backend/servicio-operacion/src/test/java/pe/rikos/operacion/produccion/api/ProduccionControllerAccesoTest.java`

- [ ] **Step 1:** Implementar `ConsultarProducciones` con cálculo de porciones planificadas (leyendo programación semanal si aplica), preparadas y vendidas.
- [ ] **Step 2:** Implementar `ProduccionController` (`POST /locales/{localId}/produccion`, `GET /locales/{localId}/produccion`, `GET /locales/{localId}/produccion/precarga`).
- [ ] **Step 3:** Configurar seguridad en `ConfiguracionSeguridad.java` con `PERM_produccion.ver` y `PERM_produccion.registrar`.
- [ ] **Step 4:** Escribir y pasar pruebas de acceso en `ProduccionControllerAccesoTest.java`.

---

### Task 6: Cliente Frontend y Pantalla de Producción ADM-19

**Files:**
- Modify: `rikos-web/src/api/cliente.ts`
- Create: `rikos-web/src/features/produccion/PantallaProduccion.tsx`
- Create: `rikos-web/src/features/produccion/ModalRegistrarProduccion.tsx`
- Create: `rikos-web/src/features/produccion/index.ts`
- Modify: `rikos-web/src/apps/admin/BarraLateral.tsx`
- Modify: `rikos-web/src/apps/admin/index.tsx`

- [ ] **Step 1:** Agregar tipos y métodos API en `cliente.ts` (`listarProducciones`, `registrarProduccion`, `obtenerPrecargaProduccion`).
- [ ] **Step 2:** Implementar `ModalRegistrarProduccion.tsx` con selección de producto/subreceta, rendimiento, porciones y tabla reactiva de previsualización de insumos a descontar con stock resultante y alertas de saldo negativo/mínimo.
- [ ] **Step 3:** Implementar `PantallaProduccion.tsx` con filtros por fecha y servicio, tabla con porciones planificadas/preparadas/vendidas y botón de nueva producción.
- [ ] **Step 4:** Enlazar en `BarraLateral.tsx` (`/admin/produccion`) y `apps/admin/index.tsx`.
- [ ] **Step 5:** Validar compilación con `npm run build`.

---

### Task 7: Verificación End-to-End con Playwright y Reporte

**Files:**
- Create: `docs/superpowers/plans/2026-10-13-us-048-produccion-subrecetas.md` (seguimiento de tareas)
- Create: Artefacto `reporte_us048.md`

- [ ] **Step 1:** Navegar con Playwright a `/admin/produccion`.
- [ ] **Step 2:** Registrar una producción de subreceta y confirmar la creación.
- [ ] **Step 3:** Verificar en `/admin/inventario/kardex` que figuren las salidas de insumos y la entrada de la subreceta con origen `produccion`.
- [ ] **Step 4:** Capturar pantallas de evidencia y generar `reporte_us048.md`.
