-- Sprint 6 (docs/producto/plan/sprint-06-vender-en-mesa.md, 3.10): el mozo reorganiza cuentas
-- (trasladar, dividir, unir mesas), asi que gana venta.modificar. SembradorCatalogoPermisos solo agrega
-- a un rol que YA tiene permisos los permisos nuevos del catalogo (para no deshacer lo que un
-- administrador quito a proposito); venta.modificar ya existia, por eso una base ya sembrada no lo
-- recibe sola. Esta migracion se lo otorga una vez. En una base nueva el sembrador ya lo asigna y el
-- INSERT no hace nada (ON CONFLICT).
INSERT INTO rol_permiso (rol_id, permiso_id)
SELECT r.id, p.id
FROM rol r
JOIN permiso p ON p.codigo = 'venta.modificar'
WHERE r.nombre = 'mozo'
ON CONFLICT (rol_id, permiso_id) DO NOTHING;
