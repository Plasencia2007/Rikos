-- Sprint 6, entrega 1 (US-027): el permiso venta.bloquear_mesa (SembradorCatalogoPermisos).
-- permiso.accion tiene un CHECK con la lista cerrada de acciones y Postgres no
-- altera un CHECK en su lugar (mismo patron que V18, V19, V24 y V26). El permiso
-- venta.administrar ya cabe en 'administrar' (V26). V29 ya estaba aplicada
-- cuando se noto, por eso va en una migracion nueva.
ALTER TABLE permiso DROP CONSTRAINT permiso_accion_check;
ALTER TABLE permiso ADD CONSTRAINT permiso_accion_check
    CHECK (accion IN ('ver', 'crear', 'modificar', 'desactivar', 'aprobar', 'anular', 'descontar', 'reabrir',
                      'ver_costos', 'exportar', 'administrar_permisos', 'marcar_agotado', 'administrar',
                      'bloquear_mesa'));
