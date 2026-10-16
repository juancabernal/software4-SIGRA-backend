-- =============================================================================
-- SIGRA — Diagnóstico de esquema para Neon/PostgreSQL (SOLO LECTURA)
--
-- Ejecutar con una conexión explícitamente apuntada al entorno que se quiere revisar:
--   psql "$DATABASE_URL" -v ON_ERROR_STOP=1 -f docs/db/SIGRA_NEON_READONLY_DIAGNOSTIC.sql
--
-- Este archivo NO crea schemas, NO crea tablas, NO inserta seeds y NO cambia el search_path.
-- Abre una transacción READ ONLY y termina con ROLLBACK. No ejecutarlo con la intención de
-- "arreglar" producción: primero se debe guardar el resultado y comparar con
-- docs/db/SIGRA_SCHEMA_CURRENT.sql.
-- =============================================================================

BEGIN;
SET TRANSACTION READ ONLY;

-- 1. Identidad del destino: confirma base, usuario, schema efectivo y versión.
SELECT current_database() AS base_datos,
       current_user AS usuario,
       current_schema() AS schema_actual,
       current_schemas(true) AS search_path;
SHOW server_version;
SHOW port;

-- 2. El baseline vive en sigra. public solo se enumera para detectar una captura antigua
--    o tablas de otro proyecto; el diagnóstico nunca mueve ni elimina nada.
SELECT schema_name,
       schema_name = 'sigra' AS es_schema_oficial,
       schema_name = 'public' AS es_schema_legacy_o_compartido
FROM information_schema.schemata
WHERE schema_name IN ('public', 'sigra')
ORDER BY schema_name;

-- 3. Tablas esperadas por SIGRA_SCHEMA_CURRENT.sql y presencia real.
WITH esperadas(table_name) AS (
    VALUES
        ('tipo_documento'), ('usuario'), ('profesor'), ('programa_academico'),
        ('asignatura'), ('resultado_aprendizaje'), ('semestre'), ('asignacion_docente'),
        ('administrador'), ('estudiante'), ('matricula'), ('conclusion'),
        ('registro_nivel_logro_prueba')
)
SELECT e.table_name,
       to_regclass(format('sigra.%I', e.table_name)) IS NOT NULL AS existe_en_sigra,
       to_regclass(format('public.%I', e.table_name)) IS NOT NULL AS existe_en_public,
       CASE
           WHEN to_regclass(format('sigra.%I', e.table_name)) IS NULL THEN 'FALTA_EN_SIGRA'
           WHEN to_regclass(format('public.%I', e.table_name)) IS NOT NULL THEN 'REVISAR_PUBLIC_COMPARTIDO'
           ELSE 'PRESENTE_EN_SIGRA'
       END AS diagnostico
FROM esperadas e
ORDER BY e.table_name;

-- 4. Tablas adicionales dentro de sigra/public. Sirve para saber si la base contiene
--    objetos de otras versiones o de otro proyecto antes de preparar una migración.
SELECT table_schema, table_name, table_type
FROM information_schema.tables
WHERE table_schema IN ('sigra', 'public')
ORDER BY table_schema, table_name;

-- 5. Todas las columnas, tipos, nulabilidad y defaults del schema oficial.
SELECT table_name, ordinal_position, column_name, data_type,
       udt_schema, udt_name, is_nullable, column_default,
       character_maximum_length, numeric_precision
FROM information_schema.columns
WHERE table_schema = 'sigra'
ORDER BY table_name, ordinal_position;

-- 6. PK, FK, UNIQUE y CHECK del schema oficial, incluyendo su definición PostgreSQL.
SELECT tc.table_name,
       tc.constraint_name,
       tc.constraint_type,
       kcu.column_name,
       ccu.table_schema AS tabla_referenciada_schema,
       ccu.table_name AS tabla_referenciada,
       ccu.column_name AS columna_referenciada,
       pg_get_constraintdef(pc.oid) AS definicion
FROM information_schema.table_constraints tc
LEFT JOIN information_schema.key_column_usage kcu
       ON kcu.constraint_schema = tc.constraint_schema
      AND kcu.constraint_name = tc.constraint_name
      AND kcu.table_name = tc.table_name
LEFT JOIN information_schema.constraint_column_usage ccu
       ON ccu.constraint_schema = tc.constraint_schema
      AND ccu.constraint_name = tc.constraint_name
LEFT JOIN pg_constraint pc
       ON pc.conname = tc.constraint_name
      AND pc.connamespace = to_regnamespace('sigra')
WHERE tc.table_schema = 'sigra'
ORDER BY tc.table_name, tc.constraint_type, tc.constraint_name, kcu.ordinal_position;

-- 7. Conteos exactos de filas por tabla. Solo se hace SELECT dinámico sobre objetos ya existentes.
DO $$
DECLARE
    t RECORD;
    cantidad BIGINT;
BEGIN
    FOR t IN
        SELECT table_name
        FROM information_schema.tables
        WHERE table_schema = 'sigra' AND table_type = 'BASE TABLE'
        ORDER BY table_name
    LOOP
        EXECUTE format('SELECT count(*) FROM sigra.%I', t.table_name) INTO cantidad;
        RAISE NOTICE 'SIGRA_ROW_COUNT table=%.% rows=%', 'sigra', t.table_name, cantidad;
    END LOOP;
END $$;

-- 8. Señales de migración: el backend actual espera JOINED Usuario -> subtipo y no espera
--    atributos comunes repetidos en profesor/estudiante.
SELECT 'PROFESOR_MODELO_ANTIGUO' AS verificacion,
       EXISTS (
           SELECT 1 FROM information_schema.columns
           WHERE table_schema = 'sigra' AND table_name = 'profesor'
             AND column_name IN ('tipo_documento_id', 'nombre_completo', 'correo_institucional',
                                 'estado', 'intentos_fallidos', 'fecha_bloqueo')
       ) AS resultado
UNION ALL
SELECT 'ESTUDIANTE_MODELO_ANTIGUO',
       EXISTS (
           SELECT 1 FROM information_schema.columns
           WHERE table_schema = 'sigra' AND table_name = 'estudiante'
             AND column_name IN ('tipo_documento_id', 'nombre_completo', 'correo_institucional', 'estado')
       )
UNION ALL
SELECT 'ASIGNACIONES_MODELO_ANTIGUO',
       to_regclass('sigra.profesor_asignatura') IS NOT NULL
       AND to_regclass('sigra.asignacion_docente') IS NULL;

ROLLBACK;
