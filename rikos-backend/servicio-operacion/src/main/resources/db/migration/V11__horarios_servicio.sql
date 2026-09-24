-- modelo-de-datos.md, secciones 3.2, 3.3, 3.3 bis y 3.3 ter. Adelanta
-- US-007 (horarios de servicio), planificada para el sprint 3. horario_
-- servicio todavia no existia como tabla real -- se crea directamente con
-- su forma final (por dia de la semana, varios turnos por dia), no como un
-- ALTER sobre una version anterior que nunca llego a tener filas.

ALTER TABLE local
    ADD COLUMN correo    VARCHAR(160),
    ADD COLUMN latitud   NUMERIC(10, 7),
    ADD COLUMN longitud  NUMERIC(10, 7);

CREATE TABLE horario_servicio (
    id           CHAR(26) PRIMARY KEY,
    local_id     CHAR(26) NOT NULL REFERENCES local(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    servicio     VARCHAR(20) NOT NULL CHECK (servicio IN ('desayuno', 'almuerzo', 'cena')),
    dia_semana   VARCHAR(20) NOT NULL CHECK (dia_semana IN
                     ('lunes', 'martes', 'miercoles', 'jueves', 'viernes', 'sabado', 'domingo')),
    hora_inicio  TIME NOT NULL,
    hora_fin     TIME NOT NULL,
    activo       BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE INDEX ix_horario_servicio_local_id ON horario_servicio(local_id);

-- 2.7 / seccion 1: unico por (local_id, servicio, dia_semana, hora_inicio)
-- entre los activos -- indice parcial, no una UNIQUE plana, porque un turno
-- desactivado no debe bloquear que se reuse la misma franja despues.
CREATE UNIQUE INDEX ux_horario_servicio_activo
    ON horario_servicio(local_id, servicio, dia_semana, hora_inicio)
    WHERE activo;

CREATE TABLE dia_especial (
    id           CHAR(26) PRIMARY KEY,
    local_id     CHAR(26) NOT NULL REFERENCES local(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    fecha        DATE NOT NULL,
    cerrado      BOOLEAN NOT NULL DEFAULT FALSE,
    descripcion  VARCHAR(120)
);

CREATE UNIQUE INDEX ux_dia_especial_local_fecha ON dia_especial(local_id, fecha);

CREATE TABLE horario_dia_especial (
    id              CHAR(26) PRIMARY KEY,
    dia_especial_id CHAR(26) NOT NULL REFERENCES dia_especial(id) ON DELETE CASCADE ON UPDATE RESTRICT,
    servicio        VARCHAR(20) NOT NULL CHECK (servicio IN ('desayuno', 'almuerzo', 'cena')),
    hora_inicio     TIME NOT NULL,
    hora_fin        TIME NOT NULL
);

CREATE INDEX ix_horario_dia_especial_dia_especial_id ON horario_dia_especial(dia_especial_id);
