-- =============================================================================
-- SIGRA — Migración: retira la tabla de prueba de calificaciones del nivel de logro
-- Archivo: docs/db/RF14_drop_registro_calificacion_prueba.sql
-- Motor: PostgreSQL 15+
--
-- Qué hace: elimina sigra.registro_calificacion_prueba. Era el puente temporal que leía
-- FuenteCalificacionesPruebaImpl mientras no existían evaluaciones (RF-10) y calificaciones (RF-12).
-- Ahora el nivel de logro (RF-14 / RF-15) lee las calificaciones reales con FuenteCalificacionesImpl,
-- y la entidad RegistroCalificacionPrueba ya no existe.
--
-- Es opcional para arrancar: con ddl-auto=validate, una tabla sobrante no rompe el arranque. Es
-- idempotente. Mismo patrón que docs/db/RF16_drop_registro_nivel_logro_prueba.sql.
--
-- Uso:
--   psql -h localhost -p <PUERTO> -U <USUARIO> -d postgres -v ON_ERROR_STOP=1 -f docs/db/RF14_drop_registro_calificacion_prueba.sql
-- =============================================================================
BEGIN;

DROP TABLE IF EXISTS sigra.registro_calificacion_prueba;

COMMIT;
