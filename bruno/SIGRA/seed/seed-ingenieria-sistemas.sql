-- =============================================================================
-- SIGRA — Datos de prueba: 3 semestres y 10 asignaturas de Ingenieria de
-- Sistemas, para probar manualmente matricula (RF-08) y consultas de
-- asignaturas/semestres sin depender de seed-demo.sql ni de las colecciones
-- de Bruno.
-- Archivo: bruno/SIGRA/seed/seed-ingenieria-sistemas.sql
-- Motor: PostgreSQL 15+
--
-- Es IDEMPOTENTE: se puede ejecutar varias veces sin duplicar datos.
--   - El programa "Ingenieria de Sistemas" se reutiliza si ya existe (por
--     ejemplo porque seed-demo.sql ya lo creo); si no existe, se crea. Nunca
--     se borra, porque otras asignaturas podrian estar usandolo.
--   - Las asignaturas (prefijo de codigo ISIS-) y los semestres (codigos
--     fijos de abajo) SI se borran y se recrean en cada corrida.
--
-- Las 10 asignaturas se insertan directamente en estado ACTIVA (sin pasar por
-- el servicio ni por resultados de aprendizaje): la regla de 5 a 7 RA activos
-- solo la aplica Asignatura.activar() en el flujo normal de la API, no una
-- restriccion de base de datos. Aqui se omite a proposito porque el objetivo
-- es tener asignaturas listas para matricular, no probar esa regla.
--
-- Los 3 semestres usan codigos en el ano 2092/2093 (lejos de cualquier
-- semestre real) para que el script nunca choque con datos reales ni con los
-- de seed-semestres.sql (1990/2090/2091) o seed-estudiantes.sql (1980/2080/
-- 2081): mismo criterio que ya usan esos dos archivos.
--
-- Uso (puerto y base segun tu .env):
--   psql -U postgres -h localhost -p <PUERTO> -d <BASE> -v ON_ERROR_STOP=1 -f bruno/SIGRA/seed/seed-ingenieria-sistemas.sql
--
-- Todas las referencias son explicitas (sigra.tabla): no depende del
-- search_path de la sesion y nunca escribe en public.
-- =============================================================================
SET client_encoding TO 'UTF8';
BEGIN;

-- 1. Programa: se reutiliza por nombre si ya existe; si no, se crea con id fijo.
INSERT INTO sigra.programa_academico (id, nombre, codigo, estado)
SELECT '00000000-0000-4000-d000-000000000001', 'Ingenieria de Sistemas', 'ISIS', 'ACTIVO'
 WHERE NOT EXISTS (
     SELECT 1 FROM sigra.programa_academico WHERE nombre = 'Ingenieria de Sistemas'
 );

-- 2. Limpieza de corridas anteriores de este script, en orden que respeta las FKs.
DELETE FROM sigra.matricula
 WHERE asignatura_id IN (SELECT id FROM sigra.asignatura WHERE codigo LIKE 'ISIS-%')
    OR semestre_id IN (SELECT id FROM sigra.semestre WHERE codigo IN ('2092-1', '2092-2', '2093-1'));
DELETE FROM sigra.asignacion_docente
 WHERE asignatura_id IN (SELECT id FROM sigra.asignatura WHERE codigo LIKE 'ISIS-%');
DELETE FROM sigra.resultado_aprendizaje
 WHERE asignatura_id IN (SELECT id FROM sigra.asignatura WHERE codigo LIKE 'ISIS-%');
DELETE FROM sigra.asignatura WHERE codigo LIKE 'ISIS-%';
DELETE FROM sigra.semestre WHERE codigo IN ('2092-1', '2092-2', '2093-1');

-- 3. Semestres de prueba: dos ACTIVOS (para matricular) y uno INACTIVO (para
-- probar el rechazo "semestre no activo").
INSERT INTO sigra.semestre (id, codigo, fecha_inicio, fecha_fin, estado) VALUES
  ('00000000-0000-4000-d200-000000000001', '2092-1', '2092-01-20', '2092-06-10', 'ACTIVO'),
  ('00000000-0000-4000-d200-000000000002', '2092-2', '2092-07-15', '2092-12-10', 'ACTIVO'),
  ('00000000-0000-4000-d200-000000000003', '2093-1', '2093-01-20', '2093-06-10', 'INACTIVO');

-- 4. Diez asignaturas de Ingenieria de Sistemas, todas ACTIVA.
INSERT INTO sigra.asignatura (id, codigo, nombre, programa_id, estado)
SELECT v.id, v.codigo, v.nombre,
       (SELECT id FROM sigra.programa_academico WHERE nombre = 'Ingenieria de Sistemas'),
       'ACTIVA'
  FROM (VALUES
    ('00000000-0000-4000-d100-000000000001'::uuid, 'ISIS-101', 'Programacion I'),
    ('00000000-0000-4000-d100-000000000002'::uuid, 'ISIS-102', 'Programacion II'),
    ('00000000-0000-4000-d100-000000000003'::uuid, 'ISIS-103', 'Estructuras de Datos'),
    ('00000000-0000-4000-d100-000000000004'::uuid, 'ISIS-201', 'Bases de Datos I'),
    ('00000000-0000-4000-d100-000000000005'::uuid, 'ISIS-202', 'Arquitectura de Computadores'),
    ('00000000-0000-4000-d100-000000000006'::uuid, 'ISIS-203', 'Sistemas Operativos'),
    ('00000000-0000-4000-d100-000000000007'::uuid, 'ISIS-301', 'Redes de Computadores'),
    ('00000000-0000-4000-d100-000000000008'::uuid, 'ISIS-302', 'Ingenieria de Software'),
    ('00000000-0000-4000-d100-000000000009'::uuid, 'ISIS-303', 'Inteligencia Artificial'),
    ('00000000-0000-4000-d100-00000000000a'::uuid, 'ISIS-304', 'Seguridad Informatica')
  ) AS v(id, codigo, nombre);

COMMIT;

-- Resumen
SELECT a.codigo, a.nombre, a.estado, p.nombre AS programa
  FROM sigra.asignatura a
  JOIN sigra.programa_academico p ON p.id = a.programa_id
 WHERE a.codigo LIKE 'ISIS-%'
 ORDER BY a.codigo;

SELECT codigo, fecha_inicio, fecha_fin, estado
  FROM sigra.semestre
 WHERE codigo IN ('2092-1', '2092-2', '2093-1')
 ORDER BY fecha_inicio;
