-- =============================================================================
-- SIGRA — Preflight de esquema para RF-04 (SOLO LECTURA)
-- Archivo: docs/db/RF04_schema_preflight.sql
-- Motor: PostgreSQL 15+
--
-- PROPÓSITO:
--   Describir el estado real del schema "sigra" ANTES de tocar nada.
--   No modifica datos ni esquema: toda la sesión es READ ONLY y termina con ROLLBACK.
--   "sigra" es un SCHEMA dentro de la base "postgres", no una base de datos.
--
-- EJECUCIÓN (PostgreSQL local; cambia el puerto según tu máquina):
--   psql -h localhost -p <PUERTO> -U <USUARIO> -d postgres -v ON_ERROR_STOP=1 -f docs/db/RF04_schema_preflight.sql
--
-- LECTURA DEL RESULTADO:
--   - Las filas "*_TABLE_EXISTS" indican si la tabla existe en el schema sigra.
--   - Los avisos NOTICE "PREFLIGHT:" muestran conteos y duplicados.
--   - PROFESOR_TIENE_COLUMNAS_ANTIGUAS = true significa que aún hay que ejecutar
--     docs/db/RF04_usuario_migration.sql (con backup previo).
--   - ALINEACION_ASIGNACIONES_REQUERIDA = true: NO migres ni arranques el backend
--     hasta resolverlo con el responsable del módulo de asignaciones.
-- =============================================================================

BEGIN;
SET TRANSACTION READ ONLY;

-- 1. Identidad de la conexión y del servidor
SELECT current_database()          AS base_datos,
       current_user                AS usuario_conectado,
       current_schema()            AS schema_actual,
       current_schemas(true)       AS search_path_efectivo;
SHOW server_version;
SHOW port;

-- 2. Existencia de las tablas del backend en el schema sigra
SELECT 'SIGRA_SCHEMA_EXISTS' AS verificacion,
       EXISTS (SELECT 1 FROM information_schema.schemata WHERE schema_name = 'sigra') AS resultado
UNION ALL SELECT 'USUARIO_TABLE_EXISTS',              to_regclass('sigra.usuario') IS NOT NULL
UNION ALL SELECT 'PROFESOR_TABLE_EXISTS',             to_regclass('sigra.profesor') IS NOT NULL
UNION ALL SELECT 'TIPO_DOCUMENTO_TABLE_EXISTS',       to_regclass('sigra.tipo_documento') IS NOT NULL
UNION ALL SELECT 'PROGRAMA_ACADEMICO_TABLE_EXISTS',   to_regclass('sigra.programa_academico') IS NOT NULL
UNION ALL SELECT 'ASIGNATURA_TABLE_EXISTS',           to_regclass('sigra.asignatura') IS NOT NULL
UNION ALL SELECT 'SEMESTRE_TABLE_EXISTS',             to_regclass('sigra.semestre') IS NOT NULL
UNION ALL SELECT 'RESULTADO_APRENDIZAJE_TABLE_EXISTS', to_regclass('sigra.resultado_aprendizaje') IS NOT NULL
UNION ALL SELECT 'ASIGNACION_DOCENTE_TABLE_EXISTS',   to_regclass('sigra.asignacion_docente') IS NOT NULL
-- Tabla heredada de un modelo anterior de asignaciones. Si existe sin asignacion_docente, hay desalineación.
UNION ALL SELECT 'PROFESOR_ASIGNATURA_TABLE_EXISTS',  to_regclass('sigra.profesor_asignatura') IS NOT NULL
UNION ALL SELECT 'ALINEACION_ASIGNACIONES_REQUERIDA',
       to_regclass('sigra.asignacion_docente') IS NULL
       AND to_regclass('sigra.profesor_asignatura') IS NOT NULL;

-- 3. Tablas relevantes del schema sigra y su número de columnas
SELECT table_name, COUNT(*) AS columnas
FROM information_schema.columns
WHERE table_schema = 'sigra'
GROUP BY table_name
ORDER BY table_name;

-- 4. Columnas de usuario y profesor (detalle para revisar el tipo y la nulabilidad)
SELECT table_name, column_name, data_type, is_nullable
FROM information_schema.columns
WHERE table_schema = 'sigra'
  AND table_name IN ('usuario', 'profesor')
ORDER BY table_name, ordinal_position;

-- 5. Restricciones (PK, FK, UNIQUE, CHECK) de usuario y profesor
SELECT c.conrelid::regclass AS tabla,
       c.conname            AS restriccion,
       c.contype            AS tipo,
       pg_get_constraintdef(c.oid) AS definicion
FROM pg_constraint c
WHERE c.conrelid IN (to_regclass('sigra.usuario'), to_regclass('sigra.profesor'))
ORDER BY tabla, restriccion;

-- 6. Conteos e integridad (solo NOTICE, sin modificar nada)
DO $$
DECLARE
    v_total_profesores          BIGINT := 0;
    v_profesores_sin_usuario    BIGINT := 0;
    v_correos_dup_usuario       BIGINT := 0;
    v_docs_dup                  BIGINT := 0;
    v_tiene_columnas_antiguas   BOOLEAN;
BEGIN
    IF to_regclass('sigra.profesor') IS NULL THEN
        RAISE NOTICE 'PREFLIGHT: la tabla sigra.profesor no existe; no hay nada que migrar.';
        RETURN;
    END IF;

    v_total_profesores := (SELECT COUNT(*) FROM sigra.profesor);
    RAISE NOTICE 'PREFLIGHT: PROFESOR_COUNT = %', v_total_profesores;

    -- Columnas del modelo antiguo de profesor (ya migradas a usuario)
    v_tiene_columnas_antiguas := EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'sigra' AND table_name = 'profesor'
          AND column_name IN ('correo_institucional', 'tipo_documento_id', 'nombre_completo',
                              'estado', 'intentos_fallidos', 'fecha_bloqueo'));
    RAISE NOTICE 'PREFLIGHT: PROFESOR_TIENE_COLUMNAS_ANTIGUAS = %', v_tiene_columnas_antiguas;

    IF to_regclass('sigra.usuario') IS NULL THEN
        v_profesores_sin_usuario := v_total_profesores;
    ELSE
        v_profesores_sin_usuario := (
            SELECT COUNT(*) FROM sigra.profesor p
            LEFT JOIN sigra.usuario u ON u.id = p.id
            WHERE u.id IS NULL);

        -- Correos duplicados ignorando mayúsculas y espacios
        v_correos_dup_usuario := (
            SELECT COUNT(*) FROM (
                SELECT 1 FROM sigra.usuario
                GROUP BY LOWER(TRIM(correo_institucional))
                HAVING COUNT(*) > 1) d);

        -- Duplicados de identidad documental (tipo de documento, número)
        v_docs_dup := (
            SELECT COUNT(*) FROM (
                SELECT 1 FROM sigra.profesor p
                JOIN sigra.usuario u ON u.id = p.id
                GROUP BY u.tipo_documento_id, p.numero_documento
                HAVING COUNT(*) > 1) d);
    END IF;

    RAISE NOTICE 'PREFLIGHT: PROFESORES_SIN_USUARIO = %', v_profesores_sin_usuario;
    RAISE NOTICE 'PREFLIGHT: CORREOS_DUPLICADOS_USUARIO_IGNORE_CASE = %', v_correos_dup_usuario;
    RAISE NOTICE 'PREFLIGHT: DOCUMENTOS_DUPLICADOS_TIPO_NUMERO = %', v_docs_dup;
END $$;

-- 7. Catálogo de tipos de documento
DO $$
BEGIN
    IF to_regclass('sigra.tipo_documento') IS NOT NULL THEN
        RAISE NOTICE 'PREFLIGHT: TIPO_DOCUMENTO_COUNT = %', (SELECT COUNT(*) FROM sigra.tipo_documento);
    END IF;
END $$;

ROLLBACK;
