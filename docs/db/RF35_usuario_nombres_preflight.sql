-- =============================================================================
-- SIGRA — Preflight 0000035 (solo lectura)
-- Muestra columnas, preview del backfill (si aún hay nombre_completo) y resumen.
-- Termina en ROLLBACK: no modifica datos permanentes.
-- =============================================================================

BEGIN;

SELECT
    EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'sigra' AND table_name = 'usuario' AND column_name = 'nombre_completo'
    ) AS tiene_nombre_completo,
    EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'sigra' AND table_name = 'usuario' AND column_name = 'primer_nombre'
    ) AS tiene_primer_nombre,
    (SELECT count(*) FROM sigra.usuario) AS total_usuarios;

CREATE TEMP TABLE rf35_preview (
    id UUID,
    nombre_actual TEXT,
    tokens INT,
    primer_nombre_previsto TEXT,
    segundo_nombre_previsto TEXT,
    primer_apellido_previsto TEXT,
    segundo_apellido_previsto TEXT,
    posible_revision_manual BOOLEAN
) ON COMMIT DROP;

DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'sigra' AND table_name = 'usuario' AND column_name = 'nombre_completo'
    ) THEN
        INSERT INTO rf35_preview
        SELECT
            u.id,
            u.nombre_completo,
            cardinality(s.parts),
            CASE WHEN cardinality(s.parts) >= 1 THEN s.parts[1] ELSE 'S/D' END,
            CASE WHEN cardinality(s.parts) >= 4 THEN s.parts[2] ELSE NULL END,
            CASE
                WHEN cardinality(s.parts) >= 4 THEN s.parts[3]
                WHEN cardinality(s.parts) = 3 THEN s.parts[2]
                WHEN cardinality(s.parts) = 2 THEN s.parts[2]
                ELSE 'S/D'
            END,
            CASE
                WHEN cardinality(s.parts) >= 4 THEN s.parts[4]
                WHEN cardinality(s.parts) = 3 THEN s.parts[3]
                ELSE 'S/D'
            END,
            CASE
                WHEN cardinality(s.parts) < 3 THEN true
                WHEN cardinality(s.parts) > 4 THEN true
                ELSE false
            END
        FROM sigra.usuario u
        CROSS JOIN LATERAL (
            SELECT regexp_split_to_array(btrim(u.nombre_completo), '\s+') AS parts
        ) s;
        RAISE NOTICE 'Preview cargado: revisar SELECT de rf35_preview';
    ELSE
        RAISE NOTICE 'Sin nombre_completo: no hay preview de split (¿ya migrado?).';
    END IF;
END $$;

-- Cómo quedarían los datos si se corre la migración (misma regla)
SELECT *
FROM rf35_preview
ORDER BY posible_revision_manual DESC, nombre_actual;

-- Resumen por cantidad de palabras
SELECT tokens, count(*) AS usuarios
FROM rf35_preview
GROUP BY tokens
ORDER BY tokens;

-- Cuántos quedarían con S/D (1 o 2 palabras en el nombre actual)
SELECT count(*) AS filas_con_sd_previsto
FROM rf35_preview
WHERE segundo_apellido_previsto = 'S/D'
   OR primer_apellido_previsto = 'S/D'
   OR primer_nombre_previsto = 'S/D';

ROLLBACK;
