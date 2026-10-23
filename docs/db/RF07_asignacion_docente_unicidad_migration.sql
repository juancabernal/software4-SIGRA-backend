-- =============================================================================
-- SIGRA — Migración: unicidad de asignación docente
-- Archivo: docs/db/RF07_asignacion_docente_unicidad_migration.sql
-- Motor: PostgreSQL 15+
--
-- Protege RF-07: un profesor solo puede tener una asignación —activa o inactiva—
-- para una misma asignatura. Las reasignaciones reactivan esa fila existente.
--
-- La migración es idempotente y no elimina ni corrige datos. Si detecta pares
-- duplicados, se detiene antes de modificar el esquema para que se revisen de
-- forma explícita; nunca decide cuál fila conservar automáticamente.
-- =============================================================================

BEGIN;

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM sigra.asignacion_docente
        GROUP BY profesor_id, asignatura_id
        HAVING count(*) > 1
    ) THEN
        RAISE EXCEPTION
            'No se puede agregar la unicidad de asignación docente: existen pares duplicados de profesor_id y asignatura_id. Revísalos antes de reintentar.';
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint c
        JOIN pg_class t ON t.oid = c.conrelid
        JOIN pg_namespace n ON n.oid = t.relnamespace
        WHERE n.nspname = 'sigra'
          AND t.relname = 'asignacion_docente'
          AND c.conname = 'uk_asignacion_docente_profesor_asignatura'
    ) THEN
        ALTER TABLE sigra.asignacion_docente
            ADD CONSTRAINT uk_asignacion_docente_profesor_asignatura
            UNIQUE (profesor_id, asignatura_id);
    END IF;
END
$$;

COMMIT;

-- Verificación posterior: ambas consultas deben devolver 0.
-- SELECT count(*)
-- FROM (
--     SELECT profesor_id, asignatura_id
--     FROM sigra.asignacion_docente
--     GROUP BY profesor_id, asignatura_id
--     HAVING count(*) > 1
-- ) duplicados;
--
-- SELECT count(*)
-- FROM pg_constraint c
-- JOIN pg_class t ON t.oid = c.conrelid
-- JOIN pg_namespace n ON n.oid = t.relnamespace
-- WHERE n.nspname = 'sigra'
--   AND t.relname = 'asignacion_docente'
--   AND c.conname = 'uk_asignacion_docente_profesor_asignatura';
