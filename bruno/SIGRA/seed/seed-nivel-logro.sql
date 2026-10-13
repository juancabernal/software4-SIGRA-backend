-- =============================================================================
-- SIGRA - Datos de prueba para la coleccion Bruno de RF-14 / RF-15 (nivel de
-- logro y estadisticas por RA).
-- Archivo: bruno/SIGRA/seed/seed-nivel-logro.sql
-- Motor: PostgreSQL 15+
--
-- Es IDEMPOTENTE: borra los datos de prueba anteriores (prefijo BRUNIV en
-- programa, asignatura y semestre; RA-NIV-*; ids fijos en estudiante) y los
-- vuelve a crear. Ejecutalo ANTES de cada corrida de la carpeta "nivel-logro":
-- el recalculo crea filas en sigra.nivel_logro_estudiante y sigra.conclusion.
--
-- Crea su PROPIO programa, asignatura, semestre, estudiantes y matriculas
-- (prefijo BRUNIV) para ejecutarse de forma independiente de los demas seeds.
--
-- NOTA (calificaciones): sigra.registro_calificacion_prueba es un DATO DE PRUEBA
-- temporal. Los modulos de evaluaciones y calificaciones todavia no existen;
-- mientras tanto FuenteCalificacionesPruebaImpl lee esta tabla (nota 0.0-5.0 y
-- porcentaje de la evaluacion) para calcular el nivel de logro.
--
-- Solo ASCII para que psql en Windows no dane los caracteres. Referencias
-- explicitas (sigra.tabla): no depende del search_path.
-- =============================================================================

-- 0. Prerrequisitos.
DO $$
BEGIN
    IF to_regclass('sigra.registro_calificacion_prueba') IS NULL
       OR to_regclass('sigra.nivel_logro_estudiante') IS NULL THEN
        RAISE EXCEPTION 'Seed abortado: faltan las tablas de RF-14/RF-15. Ejecute docs/db/SIGRA_SCHEMA_CURRENT.sql primero.';
    END IF;
END $$;

-- 1. Limpieza en orden de claves foraneas.
DELETE FROM sigra.nivel_logro_estudiante
 WHERE resultado_aprendizaje_id IN (SELECT id FROM sigra.resultado_aprendizaje WHERE codigo LIKE 'RA-NIV-%');
DELETE FROM sigra.registro_calificacion_prueba
 WHERE resultado_aprendizaje_id IN (SELECT id FROM sigra.resultado_aprendizaje WHERE codigo LIKE 'RA-NIV-%');
DELETE FROM sigra.accion_mejora
 WHERE resultado_aprendizaje_id IN (SELECT id FROM sigra.resultado_aprendizaje WHERE codigo LIKE 'RA-NIV-%');
DELETE FROM sigra.conclusion
 WHERE resultado_aprendizaje_id IN (SELECT id FROM sigra.resultado_aprendizaje WHERE codigo LIKE 'RA-NIV-%');
DELETE FROM sigra.matricula WHERE asignatura_id = '00000000-0000-4000-e720-000000000001';
DELETE FROM sigra.resultado_aprendizaje WHERE codigo LIKE 'RA-NIV-%';
DELETE FROM sigra.asignatura WHERE codigo = 'BRUNIV1';
DELETE FROM sigra.programa_academico WHERE codigo = 'BRU-NIV';
DELETE FROM sigra.semestre WHERE codigo = 'BRUNIV-A1';
DELETE FROM sigra.estudiante WHERE id::text LIKE '00000000-0000-4000-e750-%';
DELETE FROM sigra.usuario WHERE id::text LIKE '00000000-0000-4000-e750-%';

-- 2. Programa, asignatura y semestre propios. Fechas relativas a hoy para que
-- el semestre siga ACTIVO sin importar cuando se ejecute.
INSERT INTO sigra.programa_academico (id, nombre, codigo, estado) VALUES
  ('00000000-0000-4000-e710-000000000001', 'Programa Bruno Nivel de Logro', 'BRU-NIV', 'ACTIVO');

INSERT INTO sigra.asignatura (id, codigo, nombre, programa_id, estado) VALUES
  ('00000000-0000-4000-e720-000000000001', 'BRUNIV1', 'Bruno Nivel de Logro Activa', '00000000-0000-4000-e710-000000000001', 'ACTIVA');

INSERT INTO sigra.semestre (id, codigo, fecha_inicio, fecha_fin, estado) VALUES
  ('00000000-0000-4000-e730-000000000001', 'BRUNIV-A1', (CURRENT_DATE - INTERVAL '1 year')::date, (CURRENT_DATE + INTERVAL '3 years')::date, 'ACTIVO');

-- 3. Resultados de aprendizaje:
--   RA-NIV-01: con calificaciones (todos los casos del calculo).
--   RA-NIV-02: sin ninguna calificacion (todos "Sin evaluar").
INSERT INTO sigra.resultado_aprendizaje (id, asignatura_id, codigo, descripcion, estado, inactivado_con_asignatura) VALUES
  ('00000000-0000-4000-e740-000000000001', '00000000-0000-4000-e720-000000000001', 'RA-NIV-01', 'RA de prueba con calificaciones', 'ACTIVO', false),
  ('00000000-0000-4000-e740-000000000002', '00000000-0000-4000-e720-000000000001', 'RA-NIV-02', 'RA de prueba sin calificaciones', 'ACTIVO', false);

-- 4. Seis estudiantes, todos con matricula ACTIVA en BRUNIV1 / BRUNIV-A1.
DO $$
DECLARE
    v_tipo_doc_id UUID;
    i INT;
BEGIN
    -- El id compartido del equipo (tipoDocCc en local.bru) o, si no existe en esta base, la cedula por nombre.
    SELECT id INTO v_tipo_doc_id FROM sigra.tipo_documento
     ORDER BY (id = '07219e8d-126d-44cb-9d22-0f57afebec90') DESC, (nombre LIKE 'C%dula%') DESC
     LIMIT 1;
    IF v_tipo_doc_id IS NULL THEN
        RAISE EXCEPTION 'Seed abortado: no hay ningun tipo de documento. Ejecute seed-auth.sql primero.';
    END IF;
    FOR i IN 1..6 LOOP
        INSERT INTO sigra.usuario (id, tipo_documento_id, nombre_completo, correo_institucional, estado) VALUES
          (('00000000-0000-4000-e750-00000000000' || i)::uuid, v_tipo_doc_id,
           'Bruno Nivel Estudiante ' || i, 'bruno.niv' || i || '@uco.net.co', 'ACTIVO');
        INSERT INTO sigra.estudiante (id, numero_documento) VALUES
          (('00000000-0000-4000-e750-00000000000' || i)::uuid, '210000000' || i);
        INSERT INTO sigra.matricula (estudiante_id, asignatura_id, semestre_id, fecha_matricula, estado) VALUES
          (('00000000-0000-4000-e750-00000000000' || i)::uuid, '00000000-0000-4000-e720-000000000001',
           '00000000-0000-4000-e730-000000000001', CURRENT_DATE, 'ACTIVA');
    END LOOP;
END $$;

-- 5. Calificaciones de prueba de RA-NIV-01 (nota, porcentaje de la evaluacion):
--   E1: 2.0 / 2.0 / 2.0 (30, 30, 40)            -> 2.00 BAJO
--   E2: 3.0 (10), 3.0 (10), 4.5 (20)           -> 3.75 MEDIO (porcentajes normalizados: suman 40)
--   E3: 2.0 (20), 4.0 (30), 5.0 (50)           -> 4.10 ALTO
--   E4: 4.8 / 4.8 / 4.8 (30, 30, 40)            -> 4.80 SOBRESALIENTE
--   E5: solo 2 calificaciones                   -> sin datos suficientes
--   E6: ninguna                                 -> sin evaluar
-- Evaluados 4, en Bajo o Medio 2 (50%) -> el recalculo genera la conclusion de RF-16.
INSERT INTO sigra.registro_calificacion_prueba (resultado_aprendizaje_id, semestre_id, estudiante_id, nota, porcentaje_evaluacion)
SELECT '00000000-0000-4000-e740-000000000001'::uuid, '00000000-0000-4000-e730-000000000001'::uuid,
       ('00000000-0000-4000-e750-00000000000' || e)::uuid, nota, pct
  FROM (VALUES
    (1, 2.0, 30), (1, 2.0, 30), (1, 2.0, 40),
    (2, 3.0, 10), (2, 3.0, 10), (2, 4.5, 20),
    (3, 2.0, 20), (3, 4.0, 30), (3, 5.0, 50),
    (4, 4.8, 30), (4, 4.8, 30), (4, 4.8, 40),
    (5, 5.0, 30), (5, 5.0, 30)
  ) AS c(e, nota, pct);

-- Resumen de verificacion.
SELECT u.correo_institucional, COUNT(c.id) AS calificaciones
  FROM sigra.usuario u
  LEFT JOIN sigra.registro_calificacion_prueba c ON c.estudiante_id = u.id
 WHERE u.id::text LIKE '00000000-0000-4000-e750-%'
 GROUP BY u.correo_institucional
 ORDER BY u.correo_institucional;
