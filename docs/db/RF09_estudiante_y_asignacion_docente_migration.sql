-- =============================================================================
-- SIGRA — Migración: tabla estudiante y asignacion_docente.asignatura_id
-- Archivo: docs/db/RF09_estudiante_y_asignacion_docente_migration.sql
-- Motor: PostgreSQL 15+
--
-- Para quién: bases que ya aplicaron docs/db/SIGRA_SCHEMA_CURRENT.sql (o
-- RF04_usuario_migration.sql) ANTES de que existieran estos dos cambios del modelo:
--   1. La entidad Estudiante (RF-09a) necesita la tabla sigra.estudiante.
--   2. AsignacionDocente (RF-07) quedó ligada a Asignatura: necesita la columna
--      sigra.asignacion_docente.asignatura_id.
-- Con ddl-auto=validate, sin estos cambios el backend NO arranca.
--
-- Es IDEMPOTENTE: se puede ejecutar varias veces. No borra ni modifica datos.
-- Una base creada desde cero con SIGRA_SCHEMA_CURRENT.sql actualizado NO la necesita.
--
-- Uso (puerto y usuario según tu máquina):
--   psql -h localhost -p <PUERTO> -U <USUARIO> -d postgres -v ON_ERROR_STOP=1 -f docs/db/RF09_estudiante_y_asignacion_docente_migration.sql
-- =============================================================================

BEGIN;

-- 1) Estudiante
CREATE TABLE IF NOT EXISTS sigra.estudiante (
    id                   UUID         NOT NULL DEFAULT gen_random_uuid(),
    tipo_documento_id    UUID         NOT NULL,
    numero_documento     VARCHAR(10)  NOT NULL,
    nombre_completo      VARCHAR(255) NOT NULL,
    correo_institucional VARCHAR(255) NOT NULL,
    estado               VARCHAR(255) NOT NULL DEFAULT 'ACTIVO',
    CONSTRAINT estudiante_pkey PRIMARY KEY (id),
    CONSTRAINT uk_estudiante_correo UNIQUE (correo_institucional),
    CONSTRAINT uk_estudiante_documento UNIQUE (tipo_documento_id, numero_documento),
    CONSTRAINT fk_estudiante_tipo_documento FOREIGN KEY (tipo_documento_id)
        REFERENCES sigra.tipo_documento (id),
    CONSTRAINT chk_estudiante_estado CHECK (estado IN ('ACTIVO', 'INACTIVO'))
);
CREATE INDEX IF NOT EXISTS idx_estudiante_tipo_documento ON sigra.estudiante (tipo_documento_id);

-- 2) AsignacionDocente -> Asignatura
ALTER TABLE sigra.asignacion_docente ADD COLUMN IF NOT EXISTS asignatura_id UUID;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint
                    WHERE conname = 'fk_asignacion_docente_asignatura'
                      AND conrelid = 'sigra.asignacion_docente'::regclass) THEN
        ALTER TABLE sigra.asignacion_docente
            ADD CONSTRAINT fk_asignacion_docente_asignatura FOREIGN KEY (asignatura_id)
            REFERENCES sigra.asignatura (id);
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_asignacion_docente_asignatura
    ON sigra.asignacion_docente (asignatura_id);

-- NOT NULL solo si no hay filas antiguas sin asignatura. Nunca se borran datos.
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM sigra.asignacion_docente WHERE asignatura_id IS NULL) THEN
        RAISE NOTICE 'Hay asignaciones docentes antiguas sin asignatura. La columna asignatura_id queda SIN NOT NULL (el backend arranca igual). Corrige o elimina esas filas manualmente y luego ejecuta: ALTER TABLE sigra.asignacion_docente ALTER COLUMN asignatura_id SET NOT NULL;';
    ELSE
        ALTER TABLE sigra.asignacion_docente ALTER COLUMN asignatura_id SET NOT NULL;
    END IF;
END $$;

COMMIT;
