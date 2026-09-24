-- Amplia "local" con redes sociales, logo y canales de venta (delivery /
-- recojo en tienda, con tiempos estimados de entrega). Revierte a proposito
-- dos exclusiones documentadas en la ampliacion anterior de US-007 ("sin
-- superficie publica que use redes/logo", "el tiempo de entrega es una
-- decision de negocio que el propietario no habia tomado"): el propietario
-- ya tomo esa decision, asi que deja de aplicar la exclusion.
--
-- delivery_habilitado / recojo_habilitado nacen en TRUE: los locales
-- existentes (San Isidro) ya operan ambos canales hoy, activarlos por
-- defecto no es una regresion.

ALTER TABLE local
    ADD COLUMN facebook_url            VARCHAR(255),
    ADD COLUMN instagram_url           VARCHAR(255),
    ADD COLUMN tiktok_url              VARCHAR(255),
    ADD COLUMN logo_url                VARCHAR(500),
    ADD COLUMN delivery_habilitado     BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN recojo_habilitado       BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN tiempo_min_entrega_min  INTEGER,
    ADD COLUMN tiempo_max_entrega_min  INTEGER;
