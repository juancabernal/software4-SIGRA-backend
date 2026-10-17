-- =============================================================================
-- RF-16 — Retiro de sigra.registro_nivel_logro_prueba
-- Archivo: docs/db/RF16_drop_registro_nivel_logro_prueba.sql
--
-- El equipo de Estadísticas ya entregó RF-14 / RF-15: ProveedorNivelesLogroImpl
-- lee sigra.nivel_logro_estudiante (dato real), y la tabla puente de prueba
-- registro_nivel_logro_prueba quedó muerta. Para una base que ya tiene el
-- esquema actual. Es idempotente: se puede correr más de una vez.
--
--   psql -h localhost -p <PUERTO> -U <USUARIO> -d postgres -v ON_ERROR_STOP=1 -f docs/db/RF16_drop_registro_nivel_logro_prueba.sql
-- =============================================================================

BEGIN;

DROP TABLE IF EXISTS sigra.registro_nivel_logro_prueba;

COMMIT;
