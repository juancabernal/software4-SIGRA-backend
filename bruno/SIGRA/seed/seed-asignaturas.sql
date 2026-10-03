-- =============================================================================
-- Datos de prueba para la coleccion Bruno de RF-03 (asignaturas).
-- Es IDEMPOTENTE: borra los datos de prueba anteriores (los que usan el prefijo
-- BRU) y los vuelve a crear. Ejecutalo ANTES de cada corrida completa de la
-- coleccion, porque las pruebas cambian el estado de algunas asignaturas.
--
-- Solo ASCII (con escapes Unicode donde se necesitan tildes) para que psql en
-- Windows no dane los caracteres.
--
-- Si mas adelante otras tablas referencian asignatura (asignacion docente,
-- matricula...), agrega aqui el DELETE correspondiente ANTES de borrar asignatura.
-- =============================================================================
BEGIN;

DELETE FROM resultado_aprendizaje
 WHERE asignatura_id IN (SELECT id FROM asignatura WHERE codigo LIKE 'BRU%');
DELETE FROM asignatura WHERE codigo LIKE 'BRU%';
DELETE FROM programa_academico WHERE codigo IN ('BRU-ACT', 'BRU-INA');

-- Programas: uno ACTIVO y uno INACTIVO
INSERT INTO programa_academico (id, nombre, codigo, estado) VALUES
  ('00000000-0000-4000-a000-0000000000a1', 'Programa Bruno Activo',   'BRU-ACT', 'ACTIVO'),
  ('00000000-0000-4000-a000-0000000000a2', 'Programa Bruno Inactivo', 'BRU-INA', 'INACTIVO');

-- Asignaturas (todas en el programa ACTIVO)
INSERT INTO asignatura (id, codigo, nombre, programa_id, estado) VALUES
  ('00000000-0000-4000-b000-000000000001', 'BRU00', 'Bruno Cero RA',                    '00000000-0000-4000-a000-0000000000a1', 'BORRADOR'),
  ('00000000-0000-4000-b000-000000000002', 'BRU04', 'Bruno Cuatro RA',                  '00000000-0000-4000-a000-0000000000a1', 'BORRADOR'),
  ('00000000-0000-4000-b000-000000000003', 'BRU4I', 'Bruno Cuatro activos y tres inactivos', '00000000-0000-4000-a000-0000000000a1', 'BORRADOR'),
  ('00000000-0000-4000-b000-000000000004', 'BRU05', 'Bruno Cinco RA',                   '00000000-0000-4000-a000-0000000000a1', 'BORRADOR'),
  ('00000000-0000-4000-b000-000000000005', 'BRU07', 'Bruno Siete RA',                   '00000000-0000-4000-a000-0000000000a1', 'BORRADOR'),
  ('00000000-0000-4000-b000-000000000006', 'BRU08', 'Bruno Ocho RA',                    '00000000-0000-4000-a000-0000000000a1', 'BORRADOR'),
  ('00000000-0000-4000-b000-000000000007', 'BRU10', 'Bruno Activa',                     '00000000-0000-4000-a000-0000000000a1', 'ACTIVA'),
  ('00000000-0000-4000-b000-000000000008', 'BRU11', 'Bruno Inactiva',                   '00000000-0000-4000-a000-0000000000a1', 'INACTIVA'),
  ('00000000-0000-4000-b000-000000000009', 'BRU12', 'Bruno Renombrar',                  '00000000-0000-4000-a000-0000000000a1', 'BORRADOR'),
  -- Nombre con tilde (U&'...\00F3' = o con tilde) para probar la busqueda sin tildes
  ('00000000-0000-4000-b000-00000000000a', 'BRU13', U&'Programaci\00F3n Bruno',        '00000000-0000-4000-a000-0000000000a1', 'BORRADOR');

-- Resultados de aprendizaje de prueba
-- Cantidad de RA ACTIVOS por asignatura: BRU00=0, BRU04=4, BRU4I=4 (+3 inactivos),
-- BRU05=5, BRU07=7, BRU08=8, BRU10=5 (ACTIVA), BRU11=0 (INACTIVA, con 5 RA ya inactivos).
INSERT INTO resultado_aprendizaje (id, asignatura_id, codigo, descripcion, estado)
SELECT gen_random_uuid(), a.id, 'RA-0' || g, 'RA de prueba ' || g, 'ACTIVO'
  FROM (VALUES ('BRU04', 4), ('BRU4I', 4), ('BRU05', 5), ('BRU07', 7), ('BRU08', 8), ('BRU10', 5)) AS cfg(cod, n)
  JOIN asignatura a ON a.codigo = cfg.cod
  CROSS JOIN LATERAL generate_series(1, cfg.n) AS g;

-- BRU4I: 3 RA INACTIVOS adicionales (suman 7 en total, pero solo 4 cuentan)
INSERT INTO resultado_aprendizaje (id, asignatura_id, codigo, descripcion, estado)
SELECT gen_random_uuid(), a.id, 'RA-0' || g, 'RA de prueba ' || g, 'INACTIVO'
  FROM asignatura a CROSS JOIN generate_series(5, 7) AS g
 WHERE a.codigo = 'BRU4I';

-- BRU11 (INACTIVA): sus 5 RA quedaron inactivos por la cascada
INSERT INTO resultado_aprendizaje (id, asignatura_id, codigo, descripcion, estado)
SELECT gen_random_uuid(), a.id, 'RA-0' || g, 'RA de prueba ' || g, 'INACTIVO'
  FROM asignatura a CROSS JOIN generate_series(1, 5) AS g
 WHERE a.codigo = 'BRU11';

COMMIT;

-- Resumen (RA activos por asignatura de prueba)
SELECT a.codigo, a.estado,
       COUNT(r.id) FILTER (WHERE r.estado = 'ACTIVO')   AS ra_activos,
       COUNT(r.id) FILTER (WHERE r.estado = 'INACTIVO') AS ra_inactivos
  FROM asignatura a LEFT JOIN resultado_aprendizaje r ON r.asignatura_id = a.id
 WHERE a.codigo LIKE 'BRU%'
 GROUP BY a.codigo, a.estado
 ORDER BY a.codigo;
