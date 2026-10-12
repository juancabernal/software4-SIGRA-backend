-- =============================================================================
-- SIGRA — Datos de prueba para la coleccion Bruno de RF-16 (conclusiones
-- automaticas), RF-17 (acciones de mejora) y RF-18 (conversion en evaluacion).
-- Archivo: bruno/SIGRA/seed/seed-conclusiones.sql
-- Motor: PostgreSQL 15+
--
-- Es IDEMPOTENTE: borra los datos de prueba anteriores (prefijo BRUCONC en
-- programa, asignatura, semestre y resultados de aprendizaje; ids fijos en
-- estudiante) y los vuelve a crear. Ejecutalo ANTES de cada corrida completa
-- de la coleccion: las pruebas de "conclusiones" y "acciones-mejora" generan
-- filas nuevas en sigra.conclusion, sigra.accion_mejora y
-- sigra.registro_evaluacion_prueba que referencian estos resultados de
-- aprendizaje, y hay que limpiarlas antes de volver a crear el mismo RA.
--
-- Crea su PROPIO programa, asignatura, semestre y estudiantes (prefijo
-- BRUCONC) en lugar de reutilizar los de otros seeds, para poder ejecutarse
-- de forma independiente.
--
-- NOTA IMPORTANTE (nivel de logro): sigra.registro_nivel_logro_prueba es un
-- DATO DE PRUEBA temporal (ver su comentario en docs/db/SIGRA_SCHEMA_CURRENT.sql):
-- el modulo real de niveles de logro, a cargo de otro companero de equipo,
-- todavia no existe. Mientras tanto, ProveedorNivelesLogroPruebaImpl lee esta
-- tabla para decidir si una conclusion se genera. Este seed carga filas ahi
-- para poder probar RF-16 sin ese modulo.
--
-- Solo ASCII (con escapes Unicode donde se necesitan tildes) para que psql en
-- Windows no dane los caracteres.
--
-- Todas las referencias son explicitas (sigra.tabla): no depende del
-- search_path de la sesion y nunca escribe en public.
-- =============================================================================

-- 0. Prerrequisitos: mensaje claro en vez de un error de tabla inexistente.
DO $$
BEGIN
    IF to_regclass('sigra.registro_nivel_logro_prueba') IS NULL THEN
        RAISE EXCEPTION 'Seed abortado: no existe sigra.registro_nivel_logro_prueba. Ejecute docs/db/SIGRA_SCHEMA_CURRENT.sql primero.';
    END IF;
END $$;

-- 1. Limpieza de datos de prueba anteriores, en orden que respeta las FKs:
-- registro_evaluacion_prueba / accion_mejora / conclusion / registro_nivel_logro_prueba
-- -> resultado_aprendizaje -> asignatura -> programa_academico / semestre ->
-- estudiante -> usuario.
DELETE FROM sigra.registro_evaluacion_prueba
 WHERE resultado_aprendizaje_id IN (
     SELECT id FROM sigra.resultado_aprendizaje WHERE codigo LIKE 'RA-CONC-%'
 );
DELETE FROM sigra.accion_mejora
 WHERE resultado_aprendizaje_id IN (
     SELECT id FROM sigra.resultado_aprendizaje WHERE codigo LIKE 'RA-CONC-%'
 );
DELETE FROM sigra.conclusion
 WHERE resultado_aprendizaje_id IN (
     SELECT id FROM sigra.resultado_aprendizaje WHERE codigo LIKE 'RA-CONC-%'
 );
DELETE FROM sigra.registro_nivel_logro_prueba
 WHERE resultado_aprendizaje_id IN (
     SELECT id FROM sigra.resultado_aprendizaje WHERE codigo LIKE 'RA-CONC-%'
 );
DELETE FROM sigra.resultado_aprendizaje WHERE codigo LIKE 'RA-CONC-%';
DELETE FROM sigra.asignatura WHERE codigo = 'BRUCONC1';
DELETE FROM sigra.programa_academico WHERE codigo = 'BRU-CONC';
DELETE FROM sigra.semestre WHERE codigo = 'BRUCONC-A1';
DELETE FROM sigra.estudiante WHERE id IN (
    '00000000-0000-4000-e500-000000000001',
    '00000000-0000-4000-e500-000000000002',
    '00000000-0000-4000-e500-000000000003',
    '00000000-0000-4000-e500-000000000004',
    '00000000-0000-4000-e500-000000000005'
);
DELETE FROM sigra.usuario WHERE id IN (
    '00000000-0000-4000-e500-000000000001',
    '00000000-0000-4000-e500-000000000002',
    '00000000-0000-4000-e500-000000000003',
    '00000000-0000-4000-e500-000000000004',
    '00000000-0000-4000-e500-000000000005'
);

-- 2. Programa, asignatura y semestre propios (BRUCONC). El semestre usa
-- fechas RELATIVAS a hoy, igual que seed-estudiantes.sql, para que siga
-- ACTIVO sin importar cuando se ejecute este seed.
INSERT INTO sigra.programa_academico (id, nombre, codigo, estado) VALUES
  ('00000000-0000-4000-e100-000000000001', 'Programa Bruno Conclusiones', 'BRU-CONC', 'ACTIVO');

INSERT INTO sigra.asignatura (id, codigo, nombre, programa_id, estado) VALUES
  ('00000000-0000-4000-e200-000000000001', 'BRUCONC1', 'Bruno Conclusiones Activa', '00000000-0000-4000-e100-000000000001', 'ACTIVA');

INSERT INTO sigra.semestre (id, codigo, fecha_inicio, fecha_fin, estado) VALUES
  ('00000000-0000-4000-e300-000000000001', 'BRUCONC-A1', (CURRENT_DATE - INTERVAL '1 year')::date, (CURRENT_DATE + INTERVAL '3 years')::date, 'ACTIVO');

-- 3. Resultados de aprendizaje de prueba, uno por escenario de RF-16:
--   RA-CONC-01: va a tener 60% de niveles Bajo/Medio (mas del 40%)        -> genera conclusion.
--   RA-CONC-02: va a tener exactamente 40% de niveles Bajo/Medio         -> NO genera (umbral estricto).
--   RA-CONC-03: solo va a tener 2 registros de nivel de logro            -> NO genera (menos de 3).
INSERT INTO sigra.resultado_aprendizaje (id, asignatura_id, codigo, descripcion, estado, inactivado_con_asignatura) VALUES
  ('00000000-0000-4000-e400-000000000001', '00000000-0000-4000-e200-000000000001', 'RA-CONC-01', 'RA de prueba con mas del 40% en Bajo o Medio', 'ACTIVO', false),
  ('00000000-0000-4000-e400-000000000002', '00000000-0000-4000-e200-000000000001', 'RA-CONC-02', 'RA de prueba con exactamente 40% en Bajo o Medio', 'ACTIVO', false),
  ('00000000-0000-4000-e400-000000000003', '00000000-0000-4000-e200-000000000001', 'RA-CONC-03', 'RA de prueba con menos de 3 calificaciones', 'ACTIVO', false);

-- 4. Estudiantes de prueba (mismo tipo de documento compartido que usan
-- estudiantes y profesores; se valida antes que el id exista).
DO $$
DECLARE
    v_tipo_doc_id UUID := '07219e8d-126d-44cb-9d22-0f57afebec90';
BEGIN
    IF NOT EXISTS (SELECT 1 FROM sigra.tipo_documento WHERE id = v_tipo_doc_id) THEN
        RAISE EXCEPTION 'Seed abortado: no existe el tipo de documento % (el mismo id que environments/local.bru usa en tipoDocCc). Ajuste v_tipo_doc_id en este script al id real de "Cedula de Ciudadania" en su base.', v_tipo_doc_id;
    END IF;

    INSERT INTO sigra.usuario (id, tipo_documento_id, nombre_completo, correo_institucional, estado) VALUES
      ('00000000-0000-4000-e500-000000000001', v_tipo_doc_id, 'Bruno Conclusiones Estudiante 1', 'bruno.conc1@uco.net.co', 'ACTIVO'),
      ('00000000-0000-4000-e500-000000000002', v_tipo_doc_id, 'Bruno Conclusiones Estudiante 2', 'bruno.conc2@uco.net.co', 'ACTIVO'),
      ('00000000-0000-4000-e500-000000000003', v_tipo_doc_id, 'Bruno Conclusiones Estudiante 3', 'bruno.conc3@uco.net.co', 'ACTIVO'),
      ('00000000-0000-4000-e500-000000000004', v_tipo_doc_id, 'Bruno Conclusiones Estudiante 4', 'bruno.conc4@uco.net.co', 'ACTIVO'),
      ('00000000-0000-4000-e500-000000000005', v_tipo_doc_id, 'Bruno Conclusiones Estudiante 5', 'bruno.conc5@uco.net.co', 'ACTIVO');

    INSERT INTO sigra.estudiante (id, numero_documento) VALUES
      ('00000000-0000-4000-e500-000000000001', '2000000001'),
      ('00000000-0000-4000-e500-000000000002', '2000000002'),
      ('00000000-0000-4000-e500-000000000003', '2000000003'),
      ('00000000-0000-4000-e500-000000000004', '2000000004'),
      ('00000000-0000-4000-e500-000000000005', '2000000005');
END $$;

-- 5. Niveles de logro de prueba (dato puente, ver nota de arriba):
--   RA-CONC-01 (5 evaluados): 3 Bajo/Medio, 2 Alto -> 60% -> SI genera.
--   RA-CONC-02 (5 evaluados): 2 Bajo/Medio, 3 Alto  -> 40% -> NO genera.
--   RA-CONC-03 (2 evaluados): 1 Bajo, 1 Alto        -> menos de 3 -> NO genera.
INSERT INTO sigra.registro_nivel_logro_prueba (resultado_aprendizaje_id, semestre_id, estudiante_id, nivel_logro) VALUES
  ('00000000-0000-4000-e400-000000000001', '00000000-0000-4000-e300-000000000001', '00000000-0000-4000-e500-000000000001', 'BAJO'),
  ('00000000-0000-4000-e400-000000000001', '00000000-0000-4000-e300-000000000001', '00000000-0000-4000-e500-000000000002', 'BAJO'),
  ('00000000-0000-4000-e400-000000000001', '00000000-0000-4000-e300-000000000001', '00000000-0000-4000-e500-000000000003', 'MEDIO'),
  ('00000000-0000-4000-e400-000000000001', '00000000-0000-4000-e300-000000000001', '00000000-0000-4000-e500-000000000004', 'ALTO'),
  ('00000000-0000-4000-e400-000000000001', '00000000-0000-4000-e300-000000000001', '00000000-0000-4000-e500-000000000005', 'ALTO'),

  ('00000000-0000-4000-e400-000000000002', '00000000-0000-4000-e300-000000000001', '00000000-0000-4000-e500-000000000001', 'BAJO'),
  ('00000000-0000-4000-e400-000000000002', '00000000-0000-4000-e300-000000000001', '00000000-0000-4000-e500-000000000002', 'MEDIO'),
  ('00000000-0000-4000-e400-000000000002', '00000000-0000-4000-e300-000000000001', '00000000-0000-4000-e500-000000000003', 'ALTO'),
  ('00000000-0000-4000-e400-000000000002', '00000000-0000-4000-e300-000000000001', '00000000-0000-4000-e500-000000000004', 'ALTO'),
  ('00000000-0000-4000-e400-000000000002', '00000000-0000-4000-e300-000000000001', '00000000-0000-4000-e500-000000000005', 'ALTO'),

  ('00000000-0000-4000-e400-000000000003', '00000000-0000-4000-e300-000000000001', '00000000-0000-4000-e500-000000000001', 'BAJO'),
  ('00000000-0000-4000-e400-000000000003', '00000000-0000-4000-e300-000000000001', '00000000-0000-4000-e500-000000000002', 'ALTO');

-- Resumen de verificacion.
SELECT ra.codigo,
       COUNT(*) AS evaluados,
       COUNT(*) FILTER (WHERE n.nivel_logro IN ('BAJO', 'MEDIO')) AS en_bajo_o_medio,
       ROUND(100.0 * COUNT(*) FILTER (WHERE n.nivel_logro IN ('BAJO', 'MEDIO')) / COUNT(*), 2) AS porcentaje
  FROM sigra.registro_nivel_logro_prueba n
  JOIN sigra.resultado_aprendizaje ra ON ra.id = n.resultado_aprendizaje_id
 WHERE ra.codigo LIKE 'RA-CONC-%'
 GROUP BY ra.codigo
 ORDER BY ra.codigo;
