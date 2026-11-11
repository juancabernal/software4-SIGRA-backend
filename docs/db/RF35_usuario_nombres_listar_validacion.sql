-- =============================================================================
-- SIGRA — Listado de validación 0000035 (solo lectura)
-- Correr en Supabase SQL Editor (o psql) ANTES de la migración.
-- Lista TODOS los usuarios y cómo quedarían las 4 partes.
-- =============================================================================

-- A) Listado completo: actual vs previsto
SELECT
    u.id,
    u.correo_institucional,
    u.nombre_completo AS nombre_actual,
    cardinality(s.parts) AS palabras,
    CASE WHEN cardinality(s.parts) >= 1 THEN s.parts[1] ELSE 'S/D' END AS primer_nombre,
    CASE WHEN cardinality(s.parts) >= 4 THEN s.parts[2] ELSE NULL END AS segundo_nombre,
    CASE
        WHEN cardinality(s.parts) >= 4 THEN s.parts[3]
        WHEN cardinality(s.parts) = 3 THEN s.parts[2]
        WHEN cardinality(s.parts) = 2 THEN s.parts[2]
        ELSE 'S/D'
    END AS primer_apellido,
    CASE
        WHEN cardinality(s.parts) >= 4 THEN s.parts[4]
        WHEN cardinality(s.parts) = 3 THEN s.parts[3]
        ELSE 'S/D'
    END AS segundo_apellido,
    CASE
        WHEN cardinality(s.parts) = 4 THEN 'OK: nombre + segundo + 2 apellidos'
        WHEN cardinality(s.parts) = 3 THEN 'OK: nombre + 2 apellidos (sin segundo nombre)'
        WHEN cardinality(s.parts) = 2 THEN 'REVISAR: solo 1 apellido → segundo_apellido = S/D'
        WHEN cardinality(s.parts) = 1 THEN 'REVISAR: una sola palabra → apellidos = S/D'
        WHEN cardinality(s.parts) > 4 THEN 'REVISAR: más de 4 palabras → se truncará'
        ELSE 'REVISAR'
    END AS diagnostico
FROM sigra.usuario u
CROSS JOIN LATERAL (
    SELECT regexp_split_to_array(btrim(u.nombre_completo), '\s+') AS parts
) s
ORDER BY
    CASE
        WHEN cardinality(s.parts) IN (3, 4) THEN 0
        ELSE 1
    END,
    u.nombre_completo;

-- B) Resumen
SELECT
    count(*) AS total_usuarios,
    count(*) FILTER (WHERE cardinality(regexp_split_to_array(btrim(nombre_completo), '\s+')) = 4)
        AS con_nombre_segundo_y_dos_apellidos,
    count(*) FILTER (WHERE cardinality(regexp_split_to_array(btrim(nombre_completo), '\s+')) = 3)
        AS con_nombre_y_dos_apellidos,
    count(*) FILTER (WHERE cardinality(regexp_split_to_array(btrim(nombre_completo), '\s+')) = 2)
        AS solo_nombre_y_un_apellido,
    count(*) FILTER (WHERE cardinality(regexp_split_to_array(btrim(nombre_completo), '\s+')) = 1)
        AS una_sola_palabra,
    count(*) FILTER (WHERE cardinality(regexp_split_to_array(btrim(nombre_completo), '\s+')) > 4)
        AS mas_de_cuatro_palabras
FROM sigra.usuario;

-- C) Solo los que habría que revisar a mano
SELECT
    correo_institucional,
    nombre_completo,
    cardinality(regexp_split_to_array(btrim(nombre_completo), '\s+')) AS palabras
FROM sigra.usuario
WHERE cardinality(regexp_split_to_array(btrim(nombre_completo), '\s+')) NOT IN (3, 4)
ORDER BY palabras, nombre_completo;
