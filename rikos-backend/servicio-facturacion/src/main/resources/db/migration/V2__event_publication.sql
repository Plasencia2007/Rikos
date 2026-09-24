-- Tabla que exige Spring Modulith para el registro de publicacion de eventos
-- (spring-modulith-starter-jpa + spring-modulith-events-amqp). Guarda cada
-- evento de aplicacion publicado, para poder reintentar los que un listener
-- no llego a procesar. DDL tomado del volcado offline de Hibernate para la
-- entidad DefaultJpaEventPublication (Spring Modulith 2.1.1), no inventado.

CREATE TABLE IF NOT EXISTS event_publication (
    id                      UUID PRIMARY KEY,
    listener_id             VARCHAR(255) NOT NULL,
    event_type              VARCHAR(255) NOT NULL,
    serialized_event        VARCHAR(255) NOT NULL,
    publication_date        TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    completion_date         TIMESTAMP(6) WITH TIME ZONE,
    last_resubmission_date  TIMESTAMP(6) WITH TIME ZONE,
    completion_attempts     INTEGER NOT NULL,
    status                  VARCHAR(255)
        CHECK (status IN ('PUBLISHED', 'PROCESSING', 'COMPLETED', 'FAILED', 'RESUBMITTED'))
);
