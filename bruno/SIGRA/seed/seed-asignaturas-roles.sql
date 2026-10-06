-- =============================================================================
-- Datos de prueba de ROLES para la coleccion Bruno de RF-03 (asignaturas):
--   - asignacion docente ACTIVA de profesor.bruno@uco.net.co a BRU10 y a BRU07;
--   - matricula ACTIVA de estudiante.bruno@uco.net.co en BRU10, en un semestre propio BRU-ROL.
-- Sirven para las carpetas 07-roles (alcance del profesor) y 08-mis-asignaturas.
--
-- Prerrequisitos, en este orden: seed-auth.sql (profesor.bruno), seed-roles.sql
-- (estudiante.bruno) y seed-asignaturas.sql (BRU10, BRU07). Si falta alguno, el script se
-- detiene con un mensaje claro y no cambia nada.
--
-- IDEMPOTENTE: borra y recrea SOLO sus propias filas (ids fijos y el semestre BRU-ROL).
-- Todo lleva el esquema sigra. Solo ASCII.
--
-- Uso (puerto y base segun tu .env):
--   psql -U postgres -h localhost -p <PUERTO> -d <BASE> -v ON_ERROR_STOP=1 -f bruno/SIGRA/seed/seed-asignaturas-roles.sql
-- =============================================================================
BEGIN;

DELETE FROM sigra.matricula         WHERE id = '00000000-0000-4000-c0a0-0000000000b1';
DELETE FROM sigra.asignacion_docente WHERE id IN ('00000000-0000-4000-c0a0-0000000000a1',
                                                  '00000000-0000-4000-c0a0-0000000000a2');
DELETE FROM sigra.matricula         WHERE semestre_id IN (SELECT id FROM sigra.semestre WHERE codigo = 'BRU-ROL');
DELETE FROM sigra.semestre          WHERE codigo = 'BRU-ROL';

DO $$
DECLARE
    v_profesor   UUID;
    v_estudiante UUID;
    v_bru10      UUID;
    v_bru07      UUID;
BEGIN
    SELECT u.id INTO v_profesor
      FROM sigra.usuario u JOIN sigra.profesor p ON p.id = u.id
     WHERE lower(u.correo_institucional) = 'profesor.bruno@uco.net.co';
    IF v_profesor IS NULL THEN
        RAISE EXCEPTION 'Falta profesor.bruno@uco.net.co: ejecuta antes bruno/SIGRA/seed/seed-auth.sql';
    END IF;

    SELECT u.id INTO v_estudiante
      FROM sigra.usuario u JOIN sigra.estudiante e ON e.id = u.id
     WHERE lower(u.correo_institucional) = 'estudiante.bruno@uco.net.co';
    IF v_estudiante IS NULL THEN
        RAISE EXCEPTION 'Falta estudiante.bruno@uco.net.co: ejecuta antes bruno/SIGRA/seed/seed-roles.sql';
    END IF;

    SELECT id INTO v_bru10 FROM sigra.asignatura WHERE codigo = 'BRU10';
    SELECT id INTO v_bru07 FROM sigra.asignatura WHERE codigo = 'BRU07';
    IF v_bru10 IS NULL OR v_bru07 IS NULL THEN
        RAISE EXCEPTION 'Faltan BRU10 o BRU07: ejecuta antes bruno/SIGRA/seed/seed-asignaturas.sql';
    END IF;

    -- Semestre propio, con fechas lejanas para no cruzarse con otros semestres de prueba
    -- (seed-semestres usa 1990/2090/2091/2098 y seed-estudiantes 1980/2080/2081).
    INSERT INTO sigra.semestre (id, codigo, fecha_inicio, fecha_fin, estado)
    VALUES ('00000000-0000-4000-c0a0-0000000000c1', 'BRU-ROL', '2075-01-20', '2075-06-10', 'INACTIVO');

    INSERT INTO sigra.asignacion_docente (id, profesor_id, asignatura_id, estado) VALUES
      ('00000000-0000-4000-c0a0-0000000000a1', v_profesor, v_bru10, 'ACTIVO'),
      ('00000000-0000-4000-c0a0-0000000000a2', v_profesor, v_bru07, 'ACTIVO');

    INSERT INTO sigra.matricula (id, estudiante_id, asignatura_id, semestre_id, estado)
    VALUES ('00000000-0000-4000-c0a0-0000000000b1', v_estudiante, v_bru10,
            '00000000-0000-4000-c0a0-0000000000c1', 'ACTIVO');
END $$;

COMMIT;

-- Resumen
SELECT 'asignacion' AS tipo, a.codigo, u.correo_institucional AS usuario, ad.estado
  FROM sigra.asignacion_docente ad
  JOIN sigra.asignatura a ON a.id = ad.asignatura_id
  JOIN sigra.usuario u ON u.id = ad.profesor_id
 WHERE ad.id::text LIKE '00000000-0000-4000-c0a0-%'
UNION ALL
SELECT 'matricula', a.codigo, u.correo_institucional, m.estado
  FROM sigra.matricula m
  JOIN sigra.asignatura a ON a.id = m.asignatura_id
  JOIN sigra.usuario u ON u.id = m.estudiante_id
 WHERE m.id::text LIKE '00000000-0000-4000-c0a0-%'
 ORDER BY 1, 2;
