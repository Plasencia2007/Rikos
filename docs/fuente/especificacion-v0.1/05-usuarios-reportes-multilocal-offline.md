# Usuarios, reportes y requisitos no funcionales

## Usuarios y permisos

- Acceso general mediante usuario/contraseña.
- Mozos mediante PIN individual para cambio rápido en dispositivos compartidos.
- Roles y permisos granulares por módulo y acción; no depender solo del nombre del cargo.
- Asignar usuarios a uno o varios locales.
- Desactivar sin borrar historial.
- No incluir asistencia, horarios, planilla ni remuneraciones.

Permisos mínimos configurables: ver, crear, modificar, desactivar, aprobar, anular, descontar, reabrir, ver costos, exportar y administrar permisos.

## Panel del administrador

Priorizar:

- Ventas y rentabilidad.
- Caja y diferencias.
- Pensionistas y saldos.
- Inventario y costos.

Filtros: día, semana, mes, turno, local, canal, producto, plan y tipo de cliente.

## Reportes mínimos

- Ventas por local/canal/servicio/producto/usuario.
- Precio, costo histórico, utilidad y margen.
- Cierres y diferencias de caja.
- Pensionistas: deuda, saldo a favor, límite y crédito disponible.
- Estados de cuenta mensuales.
- Abonos pendientes/aprobados/rechazados.
- Inventario, Kardex, stock negativo, mínimo y ajustes.
- Producción: planificado/preparado/vendido/sobrante.
- Productos anulados después de comanda.
- Delivery, entregas, incidencias y liquidación de motorizados.
- Consumos internos.

Exportar reportes y estados de cuenta a Excel y PDF. Conservar PDF fijo de cada estado mensual cerrado.

## Offline-first

- Operación local completa sin Internet: autenticación disponible localmente, POS, pensionistas, impresora, caja, inventario, producción y delivery local.
- Conservar una copia local del saldo necesario para operar pensionistas.
- Sincronizar al restablecer Internet.
- Prevenir duplicados mediante identificadores únicos e idempotencia.
- Resolver conflictos sin perder transacciones y dejar registro de la resolución.
- El portal externo, API fiscal y ubicación remota pueden quedar temporalmente indisponibles.

## Respaldo y seguridad

- Respaldo central en tiempo real más copias recuperables.
- Contraseñas y PIN nunca se guardan ni muestran en texto legible.
- Bloqueo temporal tras intentos fallidos de PIN.
- Separar PIN presencial y contraseña del portal.
- Proteger adjuntos de pagos y compras según permisos.
- Registrar sesiones y acciones sensibles.

## Restricción tecnológica

La documentación no prescribe lenguaje, framework, base de datos, nube ni arquitectura de despliegue. El equipo técnico debe proponer una solución que cumpla PWA, multilocal, operación local completa, sincronización y respaldo.

