# Sistema de Gestión Integral — Restaurante Riko’s

Versión: 0.1  
Estado: especificación funcional en elaboración  
Enfoque técnico: agnóstico de tecnología  

## Objetivo

Documentar el sistema integral, multilocal y offline-first de Riko’s para que un equipo de desarrollo pueda diseñar su arquitectura e implementarlo sin depender de explicaciones verbales.

## Alcance confirmado de la primera versión

- POS responsive para celular, tablet y terminal fija.
- Atención en mesa, para llevar, delivery propio y recojo programado.
- Gestión de pensionistas con cuenta corriente, planes tarifarios y portal web.
- Programación semanal del almuerzo y catálogo fijo para desayuno y cena.
- Recetas, subrecetas, costos, producción, inventario y Kardex.
- Proveedores, productos y registro básico de compras.
- Caja por turnos y facturación electrónica mediante proveedor API.
- Delivery propio con seguimiento administrativo y vista para motorizados.
- Usuarios, roles y permisos configurables.
- Reportes Excel/PDF, operación multilocal y respaldo en tiempo real.

## Documentos

1. `01-alcance-y-reglas.md`: alcance, actores y reglas transversales.
2. `02-pensionistas.md`: cuenta corriente, autoservicio, portal, tarifas y abonos.
3. `03-pos-caja-cocina-delivery.md`: flujos operativos de venta.
4. `04-menu-recetas-inventario-compras.md`: catálogo, costeo y existencias.
5. `05-usuarios-reportes-multilocal-offline.md`: permisos, reportes y requisitos no funcionales.
6. `06-modelo-de-datos.md`: entidades y relaciones conceptuales.
7. `07-criterios-de-aceptacion-y-pendientes.md`: pruebas funcionales y preguntas abiertas.

## Convenciones

- **Obligatorio:** requisito confirmado para la primera versión.
- **Pendiente:** decisión todavía no proporcionada por el propietario.
- **Excluido:** función expresamente fuera de la primera versión.
- Los importes usados como ejemplos no constituyen precios definitivos.

## Exclusiones confirmadas

- Reservas de mesas.
- Fidelización para clientes regulares.
- Asistencia, horarios laborales, planilla y remuneraciones.
- Órdenes de compra y cuentas por pagar.
- Importación inicial masiva desde Excel o sistemas anteriores.
- Integración automática con WhatsApp.
- Publicación pública del menú del día.
- Optimización automática de rutas.

