-- =============================================================================
-- SIGRA — Migración: RF-18 crea evaluaciones reales y se retira la tabla de prueba
-- Archivo: docs/db/RF18_conversion_a_evaluacion_migration.sql
-- Motor: PostgreSQL 15+
--
-- Contexto:
--   La conversión de una acción de mejora en evaluación (RF-18) guardaba la evaluación en la tabla
--   temporal sigra.registro_evaluacion_prueba porque el módulo de evaluaciones no existía. Ahora la
--   conversión crea la evaluación en sigra.evaluacion con el servicio de evaluaciones (RF-10/RF-11),
--   así que la tabla de prueba sobra y el backend ya no tiene entidad para ella.
--
-- Qué hace:
--   1. Desmarca las acciones de mejora cuyo evaluacion_generada_id no apunta a ninguna fila de
--      sigra.evaluacion (las convertidas contra la tabla de prueba). Quedan como no convertidas y se
--      pueden volver a convertir, esta vez creando una evaluación real.
--   2. Elimina sigra.registro_evaluacion_prueba (y su índice, que cae con ella).
--
-- Requisito: sigra.evaluacion debe existir (docs/db/RF10_evaluacion_migration.sql).
-- Es IDEMPOTENTE: se puede ejecutar varias veces. Va en una sola transacción.
--
-- Uso (puerto, usuario y base según tu máquina):
--   psql -h localhost -p <PUERTO> -U <USUARIO> -d postgres -v ON_ERROR_STOP=1 -f docs/db/RF18_conversion_a_evaluacion_migration.sql
-- =============================================================================

BEGIN;

UPDATE sigra.accion_mejora a
   SET evaluacion_generada_id = NULL
 WHERE a.evaluacion_generada_id IS NOT NULL
   AND NOT EXISTS (SELECT 1 FROM sigra.evaluacion e WHERE e.id = a.evaluacion_generada_id);

DROP TABLE IF EXISTS sigra.registro_evaluacion_prueba;

COMMIT;

-- VERIFICACIÓN (debe devolver NULL en la primera y 0 en la segunda):
-- SELECT to_regclass('sigra.registro_evaluacion_prueba');
-- SELECT count(*) FROM sigra.accion_mejora a
--  WHERE a.evaluacion_generada_id IS NOT NULL
--    AND NOT EXISTS (SELECT 1 FROM sigra.evaluacion e WHERE e.id = a.evaluacion_generada_id);
