-- =============================================================================
-- SIGRA — Migración: tabla matricula
-- Archivo: docs/db/RF08_matricula_migration.sql
-- Motor: PostgreSQL 15+
--
-- Para quién: bases que ya aplicaron docs/db/SIGRA_SCHEMA_CURRENT.sql (o
-- RF09_estudiante_y_asignacion_docente_migration.sql) ANTES de que existiera la
-- entidad Matricula (RF-08, RF-09): el vínculo entre un estudiante, una asignatura
-- y un semestre.
-- Con ddl-auto=validate, sin esta tabla el backend NO arranca.
--
-- Es IDEMPOTENTE: se puede ejecutar varias veces. No borra ni modifica datos.
-- Una base creada desde cero con SIGRA_SCHEMA_CURRENT.sql actualizado NO la necesita.
--
-- Uso (puerto y usuario según tu máquina):
--   psql -h localhost -p <PUERTO> -U <USUARIO> -d postgres -v ON_ERROR_STOP=1 -f docs/db/RF08_matricula_migration.sql
-- =============================================================================

BEGIN;

-- Matricula (RF-08, RF-09)
-- La terna (estudiante, asignatura, semestre) es única SIEMPRE, sin importar el
-- estado: desvincular no borra el registro, lo pasa a INACTIVO.
CREATE TABLE IF NOT EXISTS sigra.matricula (
    id             UUID         NOT NULL DEFAULT gen_random_uuid(),
    estudiante_id  UUID         NOT NULL,
    asignatura_id  UUID         NOT NULL,
    semestre_id    UUID         NOT NULL,
    estado         VARCHAR(255) NOT NULL DEFAULT 'ACTIVO',
    CONSTRAINT matricula_pkey PRIMARY KEY (id),
    CONSTRAINT uk_matricula_terna UNIQUE (estudiante_id, asignatura_id, semestre_id),
    CONSTRAINT fk_matricula_estudiante FOREIGN KEY (estudiante_id) REFERENCES sigra.estudiante (id),
    CONSTRAINT fk_matricula_asignatura FOREIGN KEY (asignatura_id) REFERENCES sigra.asignatura (id),
    CONSTRAINT fk_matricula_semestre FOREIGN KEY (semestre_id) REFERENCES sigra.semestre (id),
    CONSTRAINT chk_matricula_estado CHECK (estado IN ('ACTIVO', 'INACTIVO'))
);

CREATE INDEX IF NOT EXISTS idx_matricula_estudiante ON sigra.matricula (estudiante_id);
CREATE INDEX IF NOT EXISTS idx_matricula_asignatura_semestre ON sigra.matricula (asignatura_id, semestre_id);

COMMIT;
