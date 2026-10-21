-- =============================================================================
-- SIGRA - Datos de prueba para la coleccion Bruno de RF-14 / RF-15 (nivel de
-- logro y estadisticas por RA).
-- Archivo: bruno/SIGRA/seed/seed-nivel-logro.sql
-- Motor: PostgreSQL 15+
--
-- Es IDEMPOTENTE: borra los datos de prueba anteriores (prefijo BRUNIV en
-- programa, asignatura y semestre; RA-NIV-*; ids fijos en estudiante, matricula
-- y evaluacion) y los vuelve a crear. Ejecutalo ANTES de cada corrida de la
-- carpeta "nivel-logro": el recalculo crea filas en sigra.nivel_logro_estudiante
-- y sigra.conclusion.
--
-- Usa datos REALES de evaluaciones (RF-10), matriculas (RF-08) y calificaciones
-- (RF-12), que es lo que lee FuenteCalificacionesImpl. Incluye a proposito una
-- evaluacion INACTIVA con nota y un estudiante DESVINCULADO con notas: ninguno
-- de los dos debe contar para el nivel de logro.
--
-- Solo ASCII para que psql en Windows no dane los caracteres. Referencias
-- explicitas (sigra.tabla): no depende del search_path.
-- =============================================================================

-- 0. Prerrequisitos.
DO $$
BEGIN
    IF to_regclass('sigra.calificacion') IS NULL
       OR to_regclass('sigra.evaluacion') IS NULL
       OR to_regclass('sigra.nivel_logro_estudiante') IS NULL THEN
        RAISE EXCEPTION 'Seed abortado: faltan tablas de RF-10/RF-12/RF-14. Ejecute docs/db/SIGRA_SCHEMA_CURRENT.sql primero.';
    END IF;
END $$;

-- 1. Limpieza en orden de claves foraneas.
DELETE FROM sigra.nivel_logro_estudiante
 WHERE resultado_aprendizaje_id IN (SELECT id FROM sigra.resultado_aprendizaje WHERE codigo LIKE 'RA-NIV-%');
DELETE FROM sigra.historial_calificacion
 WHERE calificacion_id IN (SELECT c.id FROM sigra.calificacion c JOIN sigra.evaluacion e ON e.id = c.evaluacion_id
                            JOIN sigra.resultado_aprendizaje r ON r.id = e.resultado_aprendizaje_id
                           WHERE r.codigo LIKE 'RA-NIV-%');
DELETE FROM sigra.calificacion
 WHERE evaluacion_id IN (SELECT e.id FROM sigra.evaluacion e JOIN sigra.resultado_aprendizaje r
                           ON r.id = e.resultado_aprendizaje_id WHERE r.codigo LIKE 'RA-NIV-%');
DELETE FROM sigra.accion_mejora
 WHERE resultado_aprendizaje_id IN (SELECT id FROM sigra.resultado_aprendizaje WHERE codigo LIKE 'RA-NIV-%');
DELETE FROM sigra.conclusion
 WHERE resultado_aprendizaje_id IN (SELECT id FROM sigra.resultado_aprendizaje WHERE codigo LIKE 'RA-NIV-%');
DELETE FROM sigra.evaluacion
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

-- 4. Siete estudiantes con matricula en BRUNIV1 / BRUNIV-A1: E1..E6 ACTIVA y E7 DESVINCULADA.
--    Matricula de Ei: 00000000-0000-4000-e760-00000000000i.
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
    FOR i IN 1..7 LOOP
        INSERT INTO sigra.usuario (id, tipo_documento_id, nombre_completo, correo_institucional, estado) VALUES
          (('00000000-0000-4000-e750-00000000000' || i)::uuid, v_tipo_doc_id,
           'Bruno Nivel Estudiante ' || i, 'bruno.niv' || i || '@uco.net.co', 'ACTIVO');
        INSERT INTO sigra.estudiante (id, numero_documento) VALUES
          (('00000000-0000-4000-e750-00000000000' || i)::uuid, '210000000' || i);
        INSERT INTO sigra.matricula (id, estudiante_id, asignatura_id, semestre_id, fecha_matricula, estado) VALUES
          (('00000000-0000-4000-e760-00000000000' || i)::uuid, ('00000000-0000-4000-e750-00000000000' || i)::uuid,
           '00000000-0000-4000-e720-000000000001', '00000000-0000-4000-e730-000000000001', CURRENT_DATE,
           CASE WHEN i = 7 THEN 'DESVINCULADA' ELSE 'ACTIVA' END);
    END LOOP;
END $$;

-- 5. Evaluaciones de RA-NIV-01: tres ACTIVA (30 + 30 + 40 = 100 %) y una INACTIVA.
INSERT INTO sigra.evaluacion (id, resultado_aprendizaje_id, semestre_id, nombre, fecha_tentativa,
                              porcentaje, descripcion, estado, inactivada_con_ra) VALUES
  ('00000000-0000-4000-e770-000000000001', '00000000-0000-4000-e740-000000000001', '00000000-0000-4000-e730-000000000001',
   'BRUNIV Parcial 1', CURRENT_DATE, 30.00, 'Evaluacion activa de prueba', 'ACTIVA', FALSE),
  ('00000000-0000-4000-e770-000000000002', '00000000-0000-4000-e740-000000000001', '00000000-0000-4000-e730-000000000001',
   'BRUNIV Parcial 2', CURRENT_DATE, 30.00, 'Evaluacion activa de prueba', 'ACTIVA', FALSE),
  ('00000000-0000-4000-e770-000000000003', '00000000-0000-4000-e740-000000000001', '00000000-0000-4000-e730-000000000001',
   'BRUNIV Final', CURRENT_DATE, 40.00, 'Evaluacion activa de prueba', 'ACTIVA', FALSE),
  ('00000000-0000-4000-e770-000000000004', '00000000-0000-4000-e740-000000000001', '00000000-0000-4000-e730-000000000001',
   'BRUNIV Inactiva', CURRENT_DATE, 20.00, 'Evaluacion inactiva: sus notas no cuentan', 'INACTIVA', FALSE);

-- 6. Calificaciones (evaluacion 1 = 30 %, 2 = 30 %, 3 = 40 %, 4 = INACTIVA):
--   E1: 2.0 / 2.0 / 2.0 (+ 5.0 en la INACTIVA, no cuenta) -> 2.00 BAJO
--   E2: 3.0 / 3.0 / 4.5                                   -> 3.60 MEDIO
--   E3: 4.0 / 4.0 / 4.5                                   -> 4.20 ALTO
--   E4: 4.8 / 4.8 / 4.8                                   -> 4.80 SOBRESALIENTE
--   E5: 5.0 / 5.0 (solo 2 calificaciones)                 -> sin datos suficientes
--   E6: ninguna                                           -> sin evaluar
--   E7: 1.0 / 1.0 / 1.0 pero DESVINCULADA                 -> no cuenta
-- Evaluados 4, en Bajo o Medio 2 (50 %) -> el recalculo genera la conclusion de RF-16.
-- Promedio de los evaluados: (2.00 + 3.60 + 4.20 + 4.80) / 4 = 3.65 (73 %).
INSERT INTO sigra.calificacion (evaluacion_id, matricula_id, nota, observacion, fecha_registro, fuera_de_fecha)
SELECT ('00000000-0000-4000-e770-00000000000' || ev)::uuid, ('00000000-0000-4000-e760-00000000000' || est)::uuid,
       nota, 'Calificacion de prueba Bruno', NOW(), FALSE
  FROM (VALUES
    (1, 1, 2.0), (1, 2, 2.0), (1, 3, 2.0), (1, 4, 5.0),
    (2, 1, 3.0), (2, 2, 3.0), (2, 3, 4.5),
    (3, 1, 4.0), (3, 2, 4.0), (3, 3, 4.5),
    (4, 1, 4.8), (4, 2, 4.8), (4, 3, 4.8),
    (5, 1, 5.0), (5, 2, 5.0),
    (7, 1, 1.0), (7, 2, 1.0), (7, 3, 1.0)
  ) AS c(est, ev, nota);

-- Resumen de verificacion.
SELECT u.correo_institucional, m.estado AS matricula,
       COUNT(c.id) FILTER (WHERE e.estado = 'ACTIVA') AS calificaciones_activas,
       COUNT(c.id) FILTER (WHERE e.estado = 'INACTIVA') AS calificaciones_inactivas
  FROM sigra.usuario u
  JOIN sigra.matricula m ON m.estudiante_id = u.id
  LEFT JOIN sigra.calificacion c ON c.matricula_id = m.id
  LEFT JOIN sigra.evaluacion e ON e.id = c.evaluacion_id
 WHERE u.id::text LIKE '00000000-0000-4000-e750-%'
 GROUP BY u.correo_institucional, m.estado
 ORDER BY u.correo_institucional;
