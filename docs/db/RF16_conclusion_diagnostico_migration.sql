-- =============================================================================
-- SIGRA — Migración: conclusion gana el diagnóstico (RF-16)
-- Archivo: docs/db/RF16_conclusion_diagnostico_migration.sql
-- Motor: PostgreSQL 15+
--
-- Para quién: toda base que ya tenga sigra.conclusion sin estas columnas
-- (docs/db/SIGRA_SCHEMA_CURRENT.sql ya las trae para bases nuevas).
-- Corresponde a RF-16 (conclusiones automáticas).
--
-- Qué hace, y por qué es OBLIGATORIA y no opcional:
--   Añade seis columnas a sigra.conclusion: total_evaluados, en_nivel_bajo,
--   en_nivel_medio, umbral_porcentaje, porcentaje_semestre_anterior y
--   semestre_anterior_codigo. La entidad Conclusion ya las mapea; con
--   ddl-auto=validate el backend no arranca porque esas columnas mapeadas
--   no existirían en la tabla.
--
-- Todas quedan NULLABLE y sin valor por defecto: las conclusiones ya
-- generadas no tienen esta foto del diagnóstico y se quedan en null, igual
-- que el mensaje fijo de RF-16 no cambia. Este avance no modifica ninguna
-- regla de cuándo se genera una conclusión, solo agrega más información al
-- generarla.
--
-- Es IDEMPOTENTE: se puede ejecutar varias veces (los ADD COLUMN IF NOT
-- EXISTS son no-op si la columna ya existe). Va entera en una transacción:
-- si algo falla, no queda a medias.
--
-- Uso:
--   psql "<conexión>" -v ON_ERROR_STOP=1 -f docs/db/RF16_conclusion_diagnostico_migration.sql
-- =============================================================================

BEGIN;

ALTER TABLE sigra.conclusion
    ADD COLUMN IF NOT EXISTS total_evaluados INTEGER,
    ADD COLUMN IF NOT EXISTS en_nivel_bajo INTEGER,
    ADD COLUMN IF NOT EXISTS en_nivel_medio INTEGER,
    ADD COLUMN IF NOT EXISTS umbral_porcentaje DOUBLE PRECISION,
    ADD COLUMN IF NOT EXISTS porcentaje_semestre_anterior DOUBLE PRECISION,
    ADD COLUMN IF NOT EXISTS semestre_anterior_codigo VARCHAR(30);

COMMIT;
