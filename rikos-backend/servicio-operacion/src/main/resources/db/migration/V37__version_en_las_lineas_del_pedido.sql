-- Sprint 6, revision de las entregas 3 y 4: dos escrituras sobre la misma linea (anular contra trasladar, comandar
-- contra modificar) se pisaban en silencio porque la fila se reescribe entera. Con version, la segunda en confirmar
-- choca y el controlador contesta 409 en vez de dejar la linea en un estado que contradice la bitacora.
ALTER TABLE detalle_pedido ADD COLUMN version INTEGER NOT NULL DEFAULT 0;
