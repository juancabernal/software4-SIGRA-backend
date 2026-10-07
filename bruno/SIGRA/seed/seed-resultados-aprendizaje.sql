-- =============================================================================
-- SIGRA - Datos de prueba para RF-06 (resultados de aprendizaje)
-- Archivo: bruno/SIGRA/seed/seed-resultados-aprendizaje.sql
-- Motor: PostgreSQL 15+
--
-- Opera SOLO sobre el schema "sigra" (dentro de la base "postgres"). No toca "public".
-- Prerequisito: el schema sigra ya existe (docs/db/SIGRA_SCHEMA_CURRENT.sql).
--
-- ES IDEMPOTENTE: borra los datos con prefijo RAP y los vuelve a crear.
-- Ejecutalo antes de cada corrida completa de pruebas, porque las pruebas cambian estados.
--
-- Ids legibles:
--   asignaturas: 00000000-0000-4000-c000-00000000000K   (K = 1..5)
--   RA:          00000000-0000-4000-d000-000000000KGG   (K = asignatura, GG = numero de RA)
--
-- Escenarios:
--   RAP-BOR0  BORRADOR  0 activos, 0 inactivos  -> registrar desde cero
--   RAP-BOR7  BORRADOR  7 activos, 1 inactivo   -> tope de 7; en borrador se puede inactivar
--   RAP-ACT5  ACTIVA    5 activos               -> minimo de 5
--   RAP-ACT6  ACTIVA    6 activos, 1 inactivo   -> inactivar de 6 a 5; cascada al inactivar la asignatura
--   RAP-INA   INACTIVA  0 activos, 5 inactivos  -> asignatura inactiva; sus 5 RA quedaron
--                                                  inactivos por la cascada (inactivado_con_asignatura = true)
--
-- Solo ASCII para que psql en Windows no dane caracteres.
-- =============================================================================
BEGIN;

DELETE FROM sigra.resultado_aprendizaje
 WHERE asignatura_id IN (SELECT id FROM sigra.asignatura WHERE codigo LIKE 'RAP%');
DELETE FROM sigra.asignatura WHERE codigo LIKE 'RAP%';
DELETE FROM sigra.programa_academico WHERE codigo = 'RAP-PRG';

INSERT INTO sigra.programa_academico (id, nombre, codigo, estado) VALUES
  ('00000000-0000-4000-c000-0000000000a1', 'Programa Pruebas RA', 'RAP-PRG', 'ACTIVO');

INSERT INTO sigra.asignatura (id, codigo, nombre, programa_id, estado) VALUES
  ('00000000-0000-4000-c000-000000000001', 'RAP-BOR0', 'RA Borrador sin RA',            '00000000-0000-4000-c000-0000000000a1', 'BORRADOR'),
  ('00000000-0000-4000-c000-000000000002', 'RAP-BOR7', 'RA Borrador 7 activos 1 inact', '00000000-0000-4000-c000-0000000000a1', 'BORRADOR'),
  ('00000000-0000-4000-c000-000000000003', 'RAP-ACT5', 'RA Activa 5 activos',           '00000000-0000-4000-c000-0000000000a1', 'ACTIVA'),
  ('00000000-0000-4000-c000-000000000004', 'RAP-ACT6', 'RA Activa 6 activos 1 inact',   '00000000-0000-4000-c000-0000000000a1', 'ACTIVA'),
  ('00000000-0000-4000-c000-000000000005', 'RAP-INA',  'RA Inactiva 5 inactivos',       '00000000-0000-4000-c000-0000000000a1', 'INACTIVA');

-- (K, desde, hasta, estado, inactivado_con_asignatura)
INSERT INTO sigra.resultado_aprendizaje (id, asignatura_id, codigo, descripcion, estado, inactivado_con_asignatura)
SELECT ('00000000-0000-4000-d000-' || lpad((cfg.k * 100 + g)::text, 12, '0'))::uuid,
       ('00000000-0000-4000-c000-' || lpad(cfg.k::text, 12, '0'))::uuid,
       'RA-' || lpad(g::text, 2, '0'),
       'Resultado de aprendizaje de prueba ' || g,
       cfg.estado,
       cfg.por_cascada
  FROM (VALUES (2, 1, 7, 'ACTIVO',   FALSE), (2, 8, 8, 'INACTIVO', FALSE),
               (3, 1, 5, 'ACTIVO',   FALSE),
               (4, 1, 6, 'ACTIVO',   FALSE), (4, 7, 7, 'INACTIVO', FALSE),
               (5, 1, 5, 'INACTIVO', TRUE)) AS cfg(k, desde, hasta, estado, por_cascada)
 CROSS JOIN LATERAL generate_series(cfg.desde, cfg.hasta) AS g;

COMMIT;

-- Resumen
SELECT a.codigo, a.estado,
       COUNT(r.id) FILTER (WHERE r.estado = 'ACTIVO')   AS ra_activos,
       COUNT(r.id) FILTER (WHERE r.estado = 'INACTIVO') AS ra_inactivos,
       COUNT(r.id) FILTER (WHERE r.inactivado_con_asignatura) AS ra_por_cascada
  FROM sigra.asignatura a
  LEFT JOIN sigra.resultado_aprendizaje r ON r.asignatura_id = a.id
 WHERE a.codigo LIKE 'RAP%'
 GROUP BY a.codigo, a.estado
 ORDER BY a.codigo;
