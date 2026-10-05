-- =============================================================================
-- SIGRA — Migración: tabla matricula (RF-08, RF-09)
-- Archivo: docs/db/RF08_matricula_migration.sql
-- Motor: PostgreSQL 15+
--
-- Para quién: bases creadas antes de que existiera la entidad Matricula. Con
-- ddl-auto=validate, sin esta tabla el backend NO arranca.
-- Requiere que ya existan sigra.estudiante, sigra.asignatura y sigra.semestre.
-- Es IDEMPOTENTE y no modifica ni borra datos. Una base creada desde cero con
-- SIGRA_SCHEMA_CURRENT.sql actualizado NO la necesita.
--
-- Uso (puerto y usuario según tu máquina):
--   psql -h localhost -p <PUERTO> -U <USUARIO> -d postgres -v ON_ERROR_STOP=1 -f docs/db/RF08_matricula_migration.sql
-- =============================================================================

BEGIN;

CREATE TABLE IF NOT EXISTS sigra.matricula (
    id            UUID         NOT NULL DEFAULT gen_random_uuid(),
    estudiante_id UUID         NOT NULL,
    asignatura_id UUID         NOT NULL,
    semestre_id   UUID         NOT NULL,
    estado        VARCHAR(255) NOT NULL DEFAULT 'ACTIVO',
    CONSTRAINT matricula_pkey PRIMARY KEY (id),
    CONSTRAINT uk_matricula_terna UNIQUE (estudiante_id, asignatura_id, semestre_id),
    CONSTRAINT fk_matricula_estudiante FOREIGN KEY (estudiante_id) REFERENCES sigra.estudiante (id),
    CONSTRAINT fk_matricula_asignatura FOREIGN KEY (asignatura_id) REFERENCES sigra.asignatura (id),
    CONSTRAINT fk_matricula_semestre FOREIGN KEY (semestre_id) REFERENCES sigra.semestre (id),
    CONSTRAINT chk_matricula_estado CHECK (estado IN ('ACTIVO', 'INACTIVO'))
);
CREATE INDEX IF NOT EXISTS idx_matricula_asignatura ON sigra.matricula (asignatura_id);
CREATE INDEX IF NOT EXISTS idx_matricula_semestre ON sigra.matricula (semestre_id);

COMMIT;
