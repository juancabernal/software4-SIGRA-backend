-- =============================================================================
-- SIGRA — Migración 0000035: nombre partido en sigra.usuario
-- Archivo: docs/db/RF35_usuario_nombres_migration.sql
-- Motor: PostgreSQL 15+
--
-- PROPÓSITO:
--   Reemplaza nombre_completo por:
--     primer_nombre (NOT NULL), segundo_nombre (nullable),
--     primer_apellido (NOT NULL), segundo_apellido (NOT NULL).
--   Backfill heurístico desde nombre_completo. Filas ambiguas usan 'S/D'.
--
-- ORDEN:
--   1. Backup / snapshot Supabase.
--   2. Ejecutar RF35_usuario_nombres_preflight.sql y revisar.
--   3. psql ... -v ON_ERROR_STOP=1 -f docs/db/RF35_usuario_nombres_migration.sql
--   4. Arrancar backend con código 0000035 (ddl-auto=validate).
--
-- IDEMPOTENTE: si ya no existe nombre_completo y existen las 4 columnas, no hace nada.
-- =============================================================================

BEGIN;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.tables
        WHERE table_schema = 'sigra' AND table_name = 'usuario'
    ) THEN
        RAISE EXCEPTION 'No existe sigra.usuario';
    END IF;
END $$;

-- Ya migrado: salir limpio.
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'sigra' AND table_name = 'usuario' AND column_name = 'nombre_completo'
    )
    AND EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'sigra' AND table_name = 'usuario' AND column_name = 'primer_nombre'
    ) THEN
        RAISE NOTICE '0000035 ya aplicado: se omite la migración.';
        RETURN;
    END IF;
END $$;

ALTER TABLE sigra.usuario ADD COLUMN IF NOT EXISTS primer_nombre VARCHAR(50);
ALTER TABLE sigra.usuario ADD COLUMN IF NOT EXISTS segundo_nombre VARCHAR(50);
ALTER TABLE sigra.usuario ADD COLUMN IF NOT EXISTS primer_apellido VARCHAR(50);
ALTER TABLE sigra.usuario ADD COLUMN IF NOT EXISTS segundo_apellido VARCHAR(50);

-- Backfill solo filas aún sin primer_nombre, mientras exista nombre_completo.
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'sigra' AND table_name = 'usuario' AND column_name = 'nombre_completo'
    ) THEN
        UPDATE sigra.usuario u
        SET
            primer_nombre = CASE
                WHEN cardinality(s.parts) >= 1 THEN s.parts[1]
                ELSE 'S/D'
            END,
            segundo_nombre = CASE
                WHEN cardinality(s.parts) >= 4 THEN s.parts[2]
                ELSE NULL
            END,
            primer_apellido = CASE
                WHEN cardinality(s.parts) >= 4 THEN s.parts[3]
                WHEN cardinality(s.parts) = 3 THEN s.parts[2]
                WHEN cardinality(s.parts) = 2 THEN s.parts[2]
                ELSE 'S/D'
            END,
            segundo_apellido = CASE
                WHEN cardinality(s.parts) >= 4 THEN s.parts[4]
                WHEN cardinality(s.parts) = 3 THEN s.parts[3]
                ELSE 'S/D'
            END
        FROM (
            SELECT
                id,
                regexp_split_to_array(btrim(nombre_completo), '\s+') AS parts
            FROM sigra.usuario
        ) s
        WHERE u.id = s.id
          AND (u.primer_nombre IS NULL OR btrim(u.primer_nombre) = '');
    END IF;
END $$;

UPDATE sigra.usuario SET primer_nombre = 'S/D' WHERE primer_nombre IS NULL OR btrim(primer_nombre) = '';
UPDATE sigra.usuario SET primer_apellido = 'S/D' WHERE primer_apellido IS NULL OR btrim(primer_apellido) = '';
-- Si falta segundo apellido, repetir el primero (salvo S/D).
UPDATE sigra.usuario
SET segundo_apellido = primer_apellido
WHERE (segundo_apellido IS NULL OR btrim(segundo_apellido) = '' OR segundo_apellido = 'S/D')
  AND primer_apellido IS NOT NULL AND btrim(primer_apellido) <> '' AND primer_apellido <> 'S/D';
UPDATE sigra.usuario SET segundo_apellido = 'S/D' WHERE segundo_apellido IS NULL OR btrim(segundo_apellido) = '';
UPDATE sigra.usuario SET segundo_nombre = NULL WHERE segundo_nombre IS NOT NULL AND btrim(segundo_nombre) = '';

ALTER TABLE sigra.usuario ALTER COLUMN primer_nombre SET NOT NULL;
ALTER TABLE sigra.usuario ALTER COLUMN primer_apellido SET NOT NULL;
ALTER TABLE sigra.usuario ALTER COLUMN segundo_apellido SET NOT NULL;

ALTER TABLE sigra.usuario DROP COLUMN IF EXISTS nombre_completo;

-- Verificación dura
DO $$
DECLARE
    total bigint;
    incompletos bigint;
BEGIN
    SELECT count(*) INTO total FROM sigra.usuario;
    SELECT count(*) INTO incompletos
    FROM sigra.usuario
    WHERE primer_nombre IS NULL OR btrim(primer_nombre) = ''
       OR primer_apellido IS NULL OR btrim(primer_apellido) = ''
       OR segundo_apellido IS NULL OR btrim(segundo_apellido) = '';

    IF incompletos > 0 THEN
        RAISE EXCEPTION 'Backfill incompleto: % filas sin nombres obligatorios', incompletos;
    END IF;

    RAISE NOTICE '0000035 OK: % usuarios migrados', total;
END $$;

-- Muestra post-migración: cómo quedaron los datos (revisar en el cliente SQL)
SELECT
    id,
    primer_nombre,
    segundo_nombre,
    primer_apellido,
    segundo_apellido,
    concat_ws(' ', primer_nombre, segundo_nombre, primer_apellido, segundo_apellido) AS nombre_derivado,
    (segundo_apellido = 'S/D' OR primer_apellido = 'S/D' OR primer_nombre = 'S/D') AS usa_sd
FROM sigra.usuario
ORDER BY usa_sd DESC, primer_apellido, primer_nombre;

SELECT
    count(*) AS total,
    count(*) FILTER (WHERE segundo_apellido = 'S/D' OR primer_apellido = 'S/D' OR primer_nombre = 'S/D') AS con_sd,
    count(*) FILTER (WHERE segundo_nombre IS NOT NULL) AS con_segundo_nombre
FROM sigra.usuario;

COMMIT;
