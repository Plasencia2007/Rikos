-- V49: Motivos iniciales para rechazo de abonos digitales (RF-005, US-024)
-- Tipo 'rechazo_abono' ya estaba permitido en la tabla motivo (V13).

INSERT INTO motivo (id, tipo, descripcion, requiere_observacion, activo) VALUES
    ('01M49A0MT0000000000000R001', 'rechazo_abono', 'Operación no encontrada en cuenta bancaria', FALSE, TRUE),
    ('01M49A0MT0000000000000R002', 'rechazo_abono', 'Monto no coincide con el extracto bancario', TRUE, TRUE),
    ('01M49A0MT0000000000000R003', 'rechazo_abono', 'Comprobante ilegible, incompleto o adulterado', FALSE, TRUE),
    ('01M49A0MT0000000000000R004', 'rechazo_abono', 'Número de operación duplicado o ya utilizado', FALSE, TRUE),
    ('01M49A0MT0000000000000R005', 'rechazo_abono', 'Otro motivo', TRUE, TRUE)
ON CONFLICT DO NOTHING;
