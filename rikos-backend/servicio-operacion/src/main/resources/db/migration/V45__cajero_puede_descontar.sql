-- Sprint 7: el cajero puede aplicar descuentos al cobrar (decision del dueño, 2026-10-11), siempre y cuando haya un descuento
-- habilitado (un motivo de tipo "descuento" activo). SembradorCatalogoPermisos solo agrega a un rol que YA tiene permisos los
-- permisos nuevos del catalogo; caja.descontar se creo con la entrega 3, asi que una base ya sembrada no lo recibe sola. Esta
-- migracion se lo otorga una vez. En una base nueva el sembrador ya lo asigna y el INSERT no hace nada (ON CONFLICT, o el
-- permiso aun no existe y el sembrador lo crea y lo asigna despues).
INSERT INTO rol_permiso (rol_id, permiso_id)
SELECT r.id, p.id
FROM rol r
JOIN permiso p ON p.codigo = 'caja.descontar'
WHERE r.nombre = 'cajero'
ON CONFLICT (rol_id, permiso_id) DO NOTHING;
