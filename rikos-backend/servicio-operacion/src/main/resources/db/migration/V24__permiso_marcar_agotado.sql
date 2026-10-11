-- US-015: la accion "marcar_agotado" (permiso menu.marcar_agotado, lo siembra
-- SembradorCatalogoPermisos). permiso.accion tiene un CHECK con la lista
-- cerrada de acciones y V23 no la amplio -- el sembrador fallaba al arrancar.
-- Va en una migracion nueva porque V23 ya esta aplicada (Flyway manda).
ALTER TABLE permiso DROP CONSTRAINT permiso_accion_check;
ALTER TABLE permiso ADD CONSTRAINT permiso_accion_check
    CHECK (accion IN ('ver', 'crear', 'modificar', 'desactivar', 'aprobar', 'anular', 'descontar', 'reabrir',
                      'ver_costos', 'exportar', 'administrar_permisos', 'marcar_agotado'));
