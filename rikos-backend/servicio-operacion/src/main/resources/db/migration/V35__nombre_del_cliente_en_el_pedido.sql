-- Sprint 6: al abrir una mesa el mozo puede anotar a nombre de quien es el pedido ("Familia Garcia"). Opcional,
-- solo para reconocer la mesa; no es una ficha de cliente (RF-017 sigue sin definir).
ALTER TABLE pedido ADD COLUMN nombre_cliente VARCHAR(120);
