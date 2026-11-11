-- =============================================================================
-- SIGRA — Migración: facultades y programa_academico.facultad_id
-- Archivo: docs/db/RF20_facultad_migration.sql
-- Motor: PostgreSQL 15+
--
-- Para quién: toda base que ya tenga sigra.programa_academico sin la tabla
-- sigra.facultad ni la columna facultad_id (docs/db/SIGRA_SCHEMA_CURRENT.sql ya
-- las trae para bases nuevas). Corresponde a la incidencia #0000036 (facultades).
--
-- Qué hace, y por qué es OBLIGATORIA:
--   1. Crea sigra.facultad (entidad Facultad).
--   2. Crea una facultad por defecto, 'Facultad sin asignar', para no dejar huérfanos
--      los programas que ya existen.
--   3. Añade programa_academico.facultad_id, la rellena con esa facultad por defecto,
--      la vuelve NOT NULL y le pone la clave foránea.
--   La entidad ProgramaAcademico mapea la facultad como @ManyToOne(optional = false);
--   con ddl-auto=validate el backend NO arranca si la tabla o la columna no existen.
--
-- Después de ejecutarla, el administrador debe reasignar cada programa a su facultad
-- real (o renombrar 'Facultad sin asignar' si todos pertenecen a la misma).
--
-- Es IDEMPOTENTE: se puede ejecutar varias veces. No borra filas y no sobreescribe
-- un facultad_id ya poblado (ver el guardado IS NULL). Va entera en una transacción:
-- si algo falla, no queda a medias.
--
-- Uso:
--   psql "<conexión>" -v ON_ERROR_STOP=1 -f docs/db/RF20_facultad_migration.sql
-- =============================================================================

BEGIN;

-- Paso 1 — Tabla facultad.
CREATE TABLE IF NOT EXISTS sigra.facultad (
    id     UUID         NOT NULL DEFAULT gen_random_uuid(),
    nombre VARCHAR(150) NOT NULL,
    estado VARCHAR(20)  NOT NULL DEFAULT 'ACTIVO',
    CONSTRAINT facultad_pkey PRIMARY KEY (id),
    CONSTRAINT uk_facultad_nombre UNIQUE (nombre),
    CONSTRAINT chk_facultad_estado CHECK (estado IN ('ACTIVO', 'INACTIVO'))
);

-- Paso 2 — Facultad por defecto para los programas que ya existen.
INSERT INTO sigra.facultad (id, nombre, estado)
VALUES ('00000000-0000-4000-fa00-000000000001', 'Facultad sin asignar', 'ACTIVO')
ON CONFLICT (id) DO NOTHING;

-- Paso 3 — Columna, todavía aceptando nulos.
ALTER TABLE sigra.programa_academico
    ADD COLUMN IF NOT EXISTS facultad_id UUID;

-- Paso 4 — Rellenar las filas existentes. Idempotente por el IS NULL.
UPDATE sigra.programa_academico
   SET facultad_id = '00000000-0000-4000-fa00-000000000001'
 WHERE facultad_id IS NULL;

-- Paso 5 — Ahora sí, no nula. Repetirlo sobre una columna ya NOT NULL es un no-op.
ALTER TABLE sigra.programa_academico
    ALTER COLUMN facultad_id SET NOT NULL;

-- Paso 6 — Clave foránea (solo si no existe todavía).
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
         WHERE conname = 'fk_programa_facultad'
           AND conrelid = 'sigra.programa_academico'::regclass
    ) THEN
        ALTER TABLE sigra.programa_academico
            ADD CONSTRAINT fk_programa_facultad FOREIGN KEY (facultad_id)
                REFERENCES sigra.facultad (id);
    END IF;
END $$;

-- Paso 7 — Índice para la FK (lo usa la validación "facultad con programas activos").
CREATE INDEX IF NOT EXISTS idx_programa_facultad_id ON sigra.programa_academico (facultad_id);

COMMIT;

-- =============================================================================
-- VERIFICACIÓN — correr después del COMMIT y antes de arrancar la aplicación.
-- Debe devolver 0.
-- =============================================================================
-- SELECT count(*) AS programas_sin_facultad
--   FROM sigra.programa_academico
--  WHERE facultad_id IS NULL;
