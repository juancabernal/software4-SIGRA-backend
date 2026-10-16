-- =============================================================================
-- SIGRA — Migración: asignacion_docente gana fecha_asignacion
-- Archivo: docs/db/RF07_asignacion_docente_fecha_migration.sql
-- Motor: PostgreSQL 15+
--
-- Para quién: toda base que ya tenga sigra.asignacion_docente sin la columna
-- fecha_asignacion (docs/db/SIGRA_SCHEMA_CURRENT.sql ya la trae para bases nuevas).
-- Corresponde a RF-07 (asignación docente).
--
-- Qué hace, y por qué es OBLIGATORIA y no opcional:
--   Añade asignacion_docente.fecha_asignacion (DATE, NOT NULL, DEFAULT CURRENT_DATE).
--   La entidad AsignacionDocente la mapea `nullable = false`. Con ddl-auto=update
--   Hibernate crea la columna pero NO rellena las filas que ya existen, así que
--   las dejaría nulas contra un campo no nulo, y con ddl-auto=validate el backend
--   no arranca porque la columna ya mapeada no existe en la tabla.
--
-- No hay una fecha real de cuándo se asignó cada profesor existente (no se guardaba
-- antes de este cambio), así que las filas ya creadas se rellenan con CURRENT_DATE:
-- es la mejor aproximación disponible y no afecta ninguna regla de negocio, porque
-- la fecha de asignación no participa en validaciones, solo es informativa.
--
-- Es IDEMPOTENTE: se puede ejecutar varias veces. No borra filas y no sobreescribe
-- una fecha_asignacion ya poblada (ver el guardado IS NULL). Va entera en una
-- transacción: si algo falla, no queda a medias.
--
-- Uso:
--   psql "<conexión>" -v ON_ERROR_STOP=1 -f docs/db/RF07_asignacion_docente_fecha_migration.sql
-- =============================================================================

BEGIN;

-- Paso 1 — Añadir la columna, todavía aceptando nulos.
ALTER TABLE sigra.asignacion_docente
    ADD COLUMN IF NOT EXISTS fecha_asignacion DATE;

-- Paso 2 — Rellenar las filas existentes. Idempotente por el IS NULL.
UPDATE sigra.asignacion_docente
   SET fecha_asignacion = CURRENT_DATE
 WHERE fecha_asignacion IS NULL;

-- Paso 3 — Ahora sí, no nula. Repetirlo sobre una columna ya NOT NULL es un no-op.
ALTER TABLE sigra.asignacion_docente
    ALTER COLUMN fecha_asignacion SET NOT NULL;

-- Paso 4 — Default para que los INSERT escritos a mano (seeds, scripts) sigan
-- funcionando sin tener que indicar la columna explícitamente.
ALTER TABLE sigra.asignacion_docente
    ALTER COLUMN fecha_asignacion SET DEFAULT CURRENT_DATE;

COMMIT;

-- =============================================================================
-- VERIFICACIÓN — correr después del COMMIT y antes de arrancar la aplicación.
-- Debe devolver 0.
-- =============================================================================
-- SELECT count(*) AS filas_sin_fecha_asignacion
--   FROM sigra.asignacion_docente
--  WHERE fecha_asignacion IS NULL;
