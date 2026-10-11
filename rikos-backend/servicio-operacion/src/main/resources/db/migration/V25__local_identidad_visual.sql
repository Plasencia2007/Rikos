-- Paso 4 del asistente de locales ("Aspecto"): color principal y portada del
-- menu digital, junto al logo que ya existia. Ambos son opcionales.
ALTER TABLE local
    ADD COLUMN color_principal VARCHAR(7),
    ADD COLUMN portada_url     VARCHAR(500);
