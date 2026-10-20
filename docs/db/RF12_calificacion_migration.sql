-- =============================================================================
-- RF-12 / RF-13 — Calificaciones y su historial (trazabilidad)
-- Archivo: docs/db/RF12_calificacion_migration.sql
--
-- Para una base que ya tiene el esquema actual (con sigra.evaluacion de RF-10 / RF-11 y
-- sigra.matricula). Es idempotente: se puede correr más de una vez. En una base nueva basta con
-- SIGRA_SCHEMA_CURRENT.sql, que ya incluye estas tablas.
--
--   psql -h localhost -p <PUERTO> -U <USUARIO> -d postgres -v ON_ERROR_STOP=1 -f docs/db/RF12_calificacion_migration.sql
-- =============================================================================

BEGIN;

-- Calificación: nota de un estudiante (vía su matrícula) en una evaluación.
-- Una sola por (evaluación, matrícula), que equivale a (evaluación, estudiante).
CREATE TABLE IF NOT EXISTS sigra.calificacion (
                                                  id                 UUID         NOT NULL DEFAULT gen_random_uuid(),
    evaluacion_id      UUID         NOT NULL,
    matricula_id       UUID         NOT NULL,
    nota               NUMERIC(2,1) NOT NULL,
    observacion        VARCHAR(500) NOT NULL,
    fecha_registro     TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    fecha_modificacion TIMESTAMP WITHOUT TIME ZONE,
    fuera_de_fecha     BOOLEAN      NOT NULL DEFAULT FALSE,
    CONSTRAINT calificacion_pkey PRIMARY KEY (id),
    CONSTRAINT uk_calificacion_evaluacion_matricula UNIQUE (evaluacion_id, matricula_id),
    CONSTRAINT fk_calificacion_evaluacion FOREIGN KEY (evaluacion_id) REFERENCES sigra.evaluacion (id),
    CONSTRAINT fk_calificacion_matricula FOREIGN KEY (matricula_id) REFERENCES sigra.matricula (id),
    CONSTRAINT chk_calificacion_nota CHECK (nota >= 0.0 AND nota <= 5.0),
    CONSTRAINT chk_calificacion_observacion CHECK (char_length(btrim(observacion)) >= 10)
    );

CREATE INDEX IF NOT EXISTS idx_calificacion_matricula ON sigra.calificacion (matricula_id);

-- Historial de la calificación: solo inserción. Una fila por registro y por modificación.
CREATE TABLE IF NOT EXISTS sigra.historial_calificacion (
                                                            id                   UUID         NOT NULL DEFAULT gen_random_uuid(),
    calificacion_id      UUID         NOT NULL,
    tipo_evento          VARCHAR(20)  NOT NULL,
    nota_anterior        NUMERIC(2,1),
    nota_nueva           NUMERIC(2,1) NOT NULL,
    observacion_anterior VARCHAR(500),
    observacion_nueva    VARCHAR(500) NOT NULL,
    fuera_de_fecha       BOOLEAN      NOT NULL,
    usuario_id           UUID         NOT NULL,
    rol_usuario          VARCHAR(20)  NOT NULL,
    fecha_evento         TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    CONSTRAINT historial_calificacion_pkey PRIMARY KEY (id),
    CONSTRAINT fk_historial_calificacion FOREIGN KEY (calificacion_id) REFERENCES sigra.calificacion (id),
    CONSTRAINT chk_historial_calificacion_tipo CHECK (tipo_evento IN ('REGISTRO', 'MODIFICACION')),
    CONSTRAINT chk_historial_calificacion_rol CHECK (rol_usuario IN ('ADMINISTRADOR', 'PROFESOR', 'ESTUDIANTE'))
    );

CREATE INDEX IF NOT EXISTS idx_historial_calificacion ON sigra.historial_calificacion (calificacion_id, fecha_evento);

COMMIT;