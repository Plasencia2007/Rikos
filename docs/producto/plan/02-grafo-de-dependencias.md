<!-- Extraído del Product Backlog v2.0 (RIKOS-product-backlog.md, 2026-09-11), líneas 3063-3078. Distribuido por paso del método el 2026-09-29, ver docs/METODO-spec-driven.md. -->

# 8. Nodos críticos del grafo de dependencias


| Historia | Historias que dependen de ella | Consecuencia si se retrasa |
|---|---|---|
| US-004 Roles y permisos | US-002, US-005, US-010, US-031, US-034, US-035, US-045, US-051, US-052, US-053 | Ninguna restricción de acceso es verificable; el caso crítico 19 de `07` no se puede probar |
| US-033 Cierre en caja | US-018, US-021, US-036, US-037, US-039, US-042, US-048, US-053, US-058 | Se bloquea el cargo al pensionista, el descuento por receta, el comprobante y toda la rentabilidad |
| US-010 Recetas y subrecetas | US-014, US-031, US-048, US-050, US-053, US-056 | Sin costo por porción no hay utilidad, margen ni descuento correcto de inventario |
| US-043/US-044 Inventario y Kardex | US-031, US-045, US-046, US-047, US-048, US-049, US-050, US-051, US-056 | Toda la trazabilidad de existencias queda en el aire |
| US-058 Operación offline | US-059, US-060, y la viabilidad práctica de EP-05 y EP-06 | Cualquier caída de Internet detiene el local, que es exactamente lo que la especificación prohíbe |

---


---

