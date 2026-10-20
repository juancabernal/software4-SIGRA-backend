-- =============================================================================
-- SIGRA - Datos de prueba para RF-10 / RF-11 (gestion de evaluaciones)
-- Archivo: bruno/SIGRA/seed/seed-evaluaciones.sql
-- Motor: PostgreSQL 15+
--
-- Opera SOLO sobre el schema "sigra" (dentro de la base "postgres"). No toca "public".
-- Prerequisitos: schema sigra con la tabla evaluacion (docs/db/RF10_evaluacion_migration.sql)
-- y, para el control por rol, seed-auth.sql (profesor.bruno@uco.net.co).
--
-- ES IDEMPOTENTE: borra los datos con prefijo EVA (y sus evaluaciones) y los vuelve a crear.
-- Ejecutalo antes de cada corrida completa de la carpeta evaluaciones de Bruno.
--
-- Ids legibles:
--   programa:     00000000-0000-4000-e100-0000000000a1
--   asignaturas:  00000000-0000-4000-e100-00000000000K   (K = 1..3)
--   RA:           00000000-0000-4000-e200-000000000KGG   (K = asignatura, GG = numero de RA)
--   semestres:    00000000-0000-4000-e300-00000000000N
--   evaluacion:   00000000-0000-4000-e400-000000000001   (historica, semestre pasado)
--
-- Escenarios:
--   EVA-ACT  ACTIVA    RA-01..RA-06 activos, RA-07 inactivo -> crear, modificar, 100%, cascada de RA
--   EVA-BOR  BORRADOR  RA-01..RA-05 activos                -> no se crean evaluaciones (asignatura no activa)
--   EVA-CAS  ACTIVA    RA-01..RA-05 activos                -> cascada al inactivar/reactivar la asignatura
--   EVA-PASADO  semestre 2001 (solo lectura) con una evaluacion historica en EVA-ACT RA-01
--   EVA-ACTUAL  semestre de ayer a manana, SOLO si ningun semestre contiene la fecha de hoy
--               (las pruebas usan la fecha de hoy como fecha tentativa)
--
-- Solo ASCII para que psql en Windows no dane caracteres.
-- =============================================================================
BEGIN;

DELETE FROM sigra.evaluacion
 WHERE resultado_aprendizaje_id IN (SELECT r.id FROM sigra.resultado_aprendizaje r
                                      JOIN sigra.asignatura a ON a.id = r.asignatura_id
                                     WHERE a.codigo LIKE 'EVA-%');
DELETE FROM sigra.asignacion_docente WHERE id::text LIKE '00000000-0000-4000-e500-%';
DELETE FROM sigra.resultado_aprendizaje
 WHERE asignatura_id IN (SELECT id FROM sigra.asignatura WHERE codigo LIKE 'EVA-%');
DELETE FROM sigra.asignatura WHERE codigo LIKE 'EVA-%';
DELETE FROM sigra.programa_academico WHERE codigo = 'EVA-PRG';
DELETE FROM sigra.semestre WHERE codigo = 'EVA-PASADO';

INSERT INTO sigra.programa_academico (id, nombre, codigo, estado) VALUES
  ('00000000-0000-4000-e100-0000000000a1', 'Programa Pruebas Evaluaciones', 'EVA-PRG', 'ACTIVO');

INSERT INTO sigra.asignatura (id, codigo, nombre, programa_id, estado) VALUES
  ('00000000-0000-4000-e100-000000000001', 'EVA-ACT', 'Evaluaciones activa',    '00000000-0000-4000-e100-0000000000a1', 'ACTIVA'),
  ('00000000-0000-4000-e100-000000000002', 'EVA-BOR', 'Evaluaciones borrador',  '00000000-0000-4000-e100-0000000000a1', 'BORRADOR'),
  ('00000000-0000-4000-e100-000000000003', 'EVA-CAS', 'Evaluaciones cascada',   '00000000-0000-4000-e100-0000000000a1', 'ACTIVA');

-- (K, desde, hasta, estado)
INSERT INTO sigra.resultado_aprendizaje (id, asignatura_id, codigo, descripcion, estado, inactivado_con_asignatura)
SELECT ('00000000-0000-4000-e200-' || lpad((cfg.k * 100 + g)::text, 12, '0'))::uuid,
       ('00000000-0000-4000-e100-' || lpad(cfg.k::text, 12, '0'))::uuid,
       'RA-' || lpad(g::text, 2, '0'),
       'Resultado de aprendizaje de prueba ' || g,
       cfg.estado,
       FALSE
  FROM (VALUES (1, 1, 6, 'ACTIVO'), (1, 7, 7, 'INACTIVO'),
               (2, 1, 5, 'ACTIVO'),
               (3, 1, 5, 'ACTIVO')) AS cfg(k, desde, hasta, estado)
 CROSS JOIN LATERAL generate_series(cfg.desde, cfg.hasta) AS g;

-- Semestre pasado (solo lectura) con una evaluacion historica.
INSERT INTO sigra.semestre (id, codigo, fecha_inicio, fecha_fin, estado) VALUES
  ('00000000-0000-4000-e300-0000000000e1', 'EVA-PASADO', '2001-01-15', '2001-06-15', 'INACTIVO');

INSERT INTO sigra.evaluacion (id, resultado_aprendizaje_id, semestre_id, nombre, fecha_tentativa,
                              porcentaje, descripcion, estado, inactivada_con_ra) VALUES
  ('00000000-0000-4000-e400-000000000001', '00000000-0000-4000-e200-000000000101',
   '00000000-0000-4000-e300-0000000000e1', 'Evaluacion historica', '2001-03-01', 100.00,
   'Evaluacion de un semestre anterior', 'ACTIVA', FALSE);

-- Semestre activo: solo si ningun semestre contiene hoy. Ventana corta (ayer a manana) para no
-- solaparse con semestres reales; si ya existe EVA-ACTUAL se le actualizan las fechas.
INSERT INTO sigra.semestre (id, codigo, fecha_inicio, fecha_fin, estado)
SELECT '00000000-0000-4000-e300-000000000002', 'EVA-ACTUAL', current_date - 1, current_date + 1, 'ACTIVO'
 WHERE NOT EXISTS (SELECT 1 FROM sigra.semestre
                    WHERE current_date BETWEEN fecha_inicio AND fecha_fin)
ON CONFLICT (codigo) DO UPDATE
   SET fecha_inicio = EXCLUDED.fecha_inicio, fecha_fin = EXCLUDED.fecha_fin, estado = 'ACTIVO';

-- Asignacion docente del profesor de Bruno (solo cuenta con el control por rol encendido).
INSERT INTO sigra.asignacion_docente (id, profesor_id, asignatura_id, estado)
SELECT v.id::uuid, '00000000-0000-4000-e000-000000000001', v.asignatura::uuid, 'ACTIVO'
  FROM (VALUES ('00000000-0000-4000-e500-000000000001', '00000000-0000-4000-e100-000000000001'),
               ('00000000-0000-4000-e500-000000000002', '00000000-0000-4000-e100-000000000002'),
               ('00000000-0000-4000-e500-000000000003', '00000000-0000-4000-e100-000000000003')) AS v(id, asignatura)
 WHERE EXISTS (SELECT 1 FROM sigra.profesor WHERE id = '00000000-0000-4000-e000-000000000001');

COMMIT;

-- Resumen
SELECT s.codigo AS semestre_activo, s.fecha_inicio, s.fecha_fin
  FROM sigra.semestre s
 WHERE current_date BETWEEN s.fecha_inicio AND s.fecha_fin;

SELECT a.codigo, a.estado,
       COUNT(r.id) FILTER (WHERE r.estado = 'ACTIVO')   AS ra_activos,
       COUNT(r.id) FILTER (WHERE r.estado = 'INACTIVO') AS ra_inactivos
  FROM sigra.asignatura a
  LEFT JOIN sigra.resultado_aprendizaje r ON r.asignatura_id = a.id
 WHERE a.codigo LIKE 'EVA-%'
 GROUP BY a.codigo, a.estado
 ORDER BY a.codigo;
