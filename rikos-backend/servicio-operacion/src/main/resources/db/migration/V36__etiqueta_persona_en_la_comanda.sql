-- Sprint 6, entrega 4 (US-029): una comanda es una FOTO de lo que se mando a cocina. Si despues una linea pasa a otra
-- persona (trasladar o dividir la cuenta), la comanda que ya salio no debe cambiar: guarda la etiqueta de la persona
-- tal como era al comandar. Lo que ya existia se rellena con la etiqueta actual de la persona de la linea.
ALTER TABLE detalle_comanda ADD COLUMN etiqueta_persona VARCHAR(120);

UPDATE detalle_comanda d
   SET etiqueta_persona = c.etiqueta
  FROM detalle_pedido l
  JOIN cuenta_persona c ON c.id = l.cuenta_persona_id
 WHERE l.id = d.detalle_pedido_id;

ALTER TABLE detalle_comanda ALTER COLUMN etiqueta_persona SET NOT NULL;
