<!-- Extraído del Product Backlog v2.0 (RIKOS-product-backlog.md, 2026-09-11), líneas 96-116. Distribuido por paso del método el 2026-09-29, ver docs/METODO-spec-driven.md. -->

# 3. Épicas identificadas

| ID épica | Nombre | Objetivo | Módulos o procesos cubiertos | Prioridad |
|---|---|---|---|---|
| EP-01 | Fundaciones: organización, usuarios, permisos y auditoría | Habilitar la operación multilocal con acceso controlado, permisos granulares y trazabilidad de acciones sensibles | Empresa y locales, autenticación, PIN de mozo, roles y permisos, asignación multilocal, bitácora de auditoría, horarios de servicio | Must |
| EP-02 | Catálogos, datos maestros y costeo | Disponer de productos, unidades, proveedores, recetas valorizadas y motivos configurables antes de operar | Catálogo y clasificación, presentaciones y conversiones, recetas y subrecetas, proveedores, listas de motivos | Must |
| EP-03 | Menú, programación semanal y disponibilidad | Planificar el almuerzo semanal y controlar qué se puede vender en cada momento | Menú normal y extra, SemanaMenu/DiaMenu/OpcionMenu, productos fijos de desayuno y cena, marcado de agotado | Must |
| EP-04 | Pensionistas: cuenta corriente, autoservicio, portal y abonos | Controlar el consumo a crédito de estudiantes con trazabilidad completa y autoservicio presencial | Ficha y responsable, planes tarifarios, cuenta corriente, pre-ticket, portal compartido, abonos, cierre mensual, desactivación | Must |
| EP-05 | POS, mesas y cocina | Registrar la venta en mesa y para llevar con asignación por persona y comunicación con cocina | Mapa de mesas, toma de pedido, gestión de cuentas, comandas, anulaciones | Must |
| EP-06 | Caja, cobro y facturación electrónica | Cobrar, conciliar por turno y emitir comprobantes electrónicos válidos incluso sin Internet | Turno de caja, cobro multimedio, descuentos y anulaciones, reapertura, cierre y conciliación, integración fiscal | Must |
| EP-07 | Delivery propio y recojo programado | Atender los canales no presenciales con control administrativo y liquidación | Recojo programado, no recogido, delivery, vista del motorizado, liquidación | Should |
| EP-08 | Inventario, producción y compras | Mantener existencias reales por local con origen rastreable y costo actualizado | Existencias y stock mínimo, Kardex, ajustes, transferencias, conteos físicos, producción y sobrantes, compras, consumo interno | Must |
| EP-09 | Panel de administración y reportes | Dar visibilidad de ventas, rentabilidad, caja, pensionistas, inventario y operación, exportable a Excel y PDF | Panel con filtros, 12 reportes mínimos, exportación, PDF fijo de estados mensuales | Should |
| EP-10 | Continuidad: offline, sincronización, respaldo y seguridad | Garantizar operación sin Internet, sincronización sin duplicados, respaldo y protección de credenciales y adjuntos | Operación local completa, sincronización idempotente, resolución de conflictos, respaldo en tiempo real, seguridad de accesos | Must |
| EP-11 | Habilitantes técnicas | Construir la base sobre la que se apoyan todas las demás épicas: entorno, migraciones, identificadores, eventos, fronteras verificadas y despliegue | Esqueleto, Flyway, ULID, outbox, generación de tipos, Modulith, componentes táctiles, observabilidad, perfiles | Must |

---


---

