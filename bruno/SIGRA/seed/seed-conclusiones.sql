-- =============================================================================
-- SIGRA — Datos de prueba para la coleccion Bruno de RF-16 (conclusiones
-- automaticas), RF-17 (acciones de mejora) y RF-18 (conversion en evaluacion).
-- Archivo: bruno/SIGRA/seed/seed-conclusiones.sql
-- Motor: PostgreSQL 15+
--
-- Es IDEMPOTENTE: lo ESTABLE (programa, asignatura, semestre, resultados de
-- aprendizaje y estudiantes de prueba, todos con ids fijos) se inserta solo
-- si no existe (ON CONFLICT DO NOTHING); lo VOLATIL (nivel_logro_estudiante,
-- conclusion, accion_mejora, y las evaluaciones que una conversion real haya
-- generado sobre estos RA) se borra y se recrea en cada corrida. Ejecutalo
-- ANTES de cada corrida completa de la coleccion.
--
-- Ya NO borra asignatura/programa/semestre/RA/estudiantes: desde que
-- "convertir una accion de mejora" (RF-18) crea una sigra.evaluacion REAL con
-- fk_evaluacion_ra apuntando a estos RA, borrar el RA aqui y volverlo a crear
-- con el mismo id rompia esa FK en la siguiente corrida. Mantener los mismos
-- ids fijos para lo estable es lo que permite seguir siendo idempotente.
--
-- Crea su PROPIO programa, asignatura, semestre y estudiantes (prefijo
-- BRUCONC) en lugar de reutilizar los de otros seeds, para poder ejecutarse
-- de forma independiente.
--
-- NOTA (nivel de logro): sigra.nivel_logro_estudiante es el dato real que
-- calcula el recalculo de estadisticas (RF-14 / RF-15). ProveedorNivelesLogroImpl
-- lee esta tabla para decidir si una conclusion se genera. Este seed siembra
-- filas ahi directamente (sin pasar por el recalculo ni por calificaciones)
-- para poder probar RF-16 de forma aislada.
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
    IF to_regclass('sigra.nivel_logro_estudiante') IS NULL THEN
        RAISE EXCEPTION 'Seed abortado: no existe sigra.nivel_logro_estudiante. Ejecute docs/db/SIGRA_SCHEMA_CURRENT.sql primero.';
    END IF;
END $$;

-- 1. Limpieza de lo VOLATIL de corridas anteriores, en orden que respeta las
-- FKs: lo que cuelga de evaluacion va primero (historial_calificacion ->
-- calificacion -> evaluacion), despues registro_evaluacion_prueba /
-- accion_mejora / conclusion / nivel_logro_estudiante. NO se borra
-- resultado_aprendizaje ni lo que esta encima (asignatura, programa,
-- semestre, estudiantes): eso es estable y se conserva (paso 2).
DELETE FROM sigra.historial_calificacion
 WHERE calificacion_id IN (
     SELECT c.id
       FROM sigra.calificacion c
       JOIN sigra.evaluacion e ON e.id = c.evaluacion_id
       JOIN sigra.resultado_aprendizaje ra ON ra.id = e.resultado_aprendizaje_id
      WHERE ra.codigo LIKE 'RA-CONC-%'
 );
DELETE FROM sigra.calificacion
 WHERE evaluacion_id IN (
     SELECT e.id
       FROM sigra.evaluacion e
       JOIN sigra.resultado_aprendizaje ra ON ra.id = e.resultado_aprendizaje_id
      WHERE ra.codigo LIKE 'RA-CONC-%'
 );
-- Evaluaciones generadas por conversiones de corridas anteriores sobre estos
-- RA: hay que borrarlas o el porcentaje acumulado del RA hace fallar las
-- pruebas de conversion (limite del 100%) en la siguiente corrida.
DELETE FROM sigra.evaluacion
 WHERE resultado_aprendizaje_id IN (
     SELECT id FROM sigra.resultado_aprendizaje WHERE codigo LIKE 'RA-CONC-%'
 );
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
DELETE FROM sigra.nivel_logro_estudiante
 WHERE resultado_aprendizaje_id IN (
     SELECT id FROM sigra.resultado_aprendizaje WHERE codigo LIKE 'RA-CONC-%'
 );

-- 2. Programa, asignatura, semestre, RA y estudiantes ESTABLES (BRUCONC), con
-- ids fijos: se insertan solo si no existen, para no violar fk_evaluacion_ra
-- cuando una corrida anterior ya convirtio una accion de mejora sobre estos
-- RA en una evaluacion real. El semestre usa fechas RELATIVAS a hoy (igual
-- que seed-estudiantes.sql) solo la primera vez que se crea.
INSERT INTO sigra.programa_academico (id, nombre, codigo, estado) VALUES
  ('00000000-0000-4000-e100-000000000001', 'Programa Bruno Conclusiones', 'BRU-CONC', 'ACTIVO')
  ON CONFLICT (id) DO NOTHING;

INSERT INTO sigra.asignatura (id, codigo, nombre, programa_id, estado) VALUES
  ('00000000-0000-4000-e200-000000000001', 'BRUCONC1', 'Bruno Conclusiones Activa', '00000000-0000-4000-e100-000000000001', 'ACTIVA')
  ON CONFLICT (id) DO NOTHING;

INSERT INTO sigra.semestre (id, codigo, fecha_inicio, fecha_fin, estado) VALUES
  ('00000000-0000-4000-e300-000000000001', 'BRUCONC-A1', (CURRENT_DATE - INTERVAL '1 year')::date, (CURRENT_DATE + INTERVAL '3 years')::date, 'ACTIVO')
  ON CONFLICT (id) DO NOTHING;

-- 3. Resultados de aprendizaje de prueba, uno por escenario de RF-16:
--   RA-CONC-01: va a tener 60% de niveles Bajo/Medio (mas del 40%)        -> genera conclusion.
--   RA-CONC-02: va a tener exactamente 40% de niveles Bajo/Medio         -> NO genera (umbral estricto).
--   RA-CONC-03: solo va a tener 2 registros de nivel de logro            -> NO genera (menos de 3).
INSERT INTO sigra.resultado_aprendizaje (id, asignatura_id, codigo, descripcion, estado, inactivado_con_asignatura) VALUES
  ('00000000-0000-4000-e400-000000000001', '00000000-0000-4000-e200-000000000001', 'RA-CONC-01', 'RA de prueba con mas del 40% en Bajo o Medio', 'ACTIVO', false),
  ('00000000-0000-4000-e400-000000000002', '00000000-0000-4000-e200-000000000001', 'RA-CONC-02', 'RA de prueba con exactamente 40% en Bajo o Medio', 'ACTIVO', false),
  ('00000000-0000-4000-e400-000000000003', '00000000-0000-4000-e200-000000000001', 'RA-CONC-03', 'RA de prueba con menos de 3 calificaciones', 'ACTIVO', false)
  ON CONFLICT (id) DO NOTHING;

-- 4. Estudiantes de prueba (mismo tipo de documento compartido que usan
-- estudiantes y profesores). Al seed le da igual QUE tipo de documento usen:
-- toma cualquiera que ya exista, o crea uno si la tabla esta vacia. Sin id
-- fijo ni comparacion por nombre, para no depender de datos preexistentes.
DO $$
DECLARE
    v_tipo_doc_id UUID;
BEGIN
    SELECT id INTO v_tipo_doc_id FROM sigra.tipo_documento LIMIT 1;

    IF v_tipo_doc_id IS NULL THEN
        INSERT INTO sigra.tipo_documento (nombre)
        VALUES ('Cedula de Ciudadania')
        RETURNING id INTO v_tipo_doc_id;
    END IF;

    INSERT INTO sigra.usuario (id, tipo_documento_id, nombre_completo, correo_institucional, estado) VALUES
      ('00000000-0000-4000-e500-000000000001', v_tipo_doc_id, 'Bruno Conclusiones Estudiante 1', 'bruno.conc1@uco.net.co', 'ACTIVO'),
      ('00000000-0000-4000-e500-000000000002', v_tipo_doc_id, 'Bruno Conclusiones Estudiante 2', 'bruno.conc2@uco.net.co', 'ACTIVO'),
      ('00000000-0000-4000-e500-000000000003', v_tipo_doc_id, 'Bruno Conclusiones Estudiante 3', 'bruno.conc3@uco.net.co', 'ACTIVO'),
      ('00000000-0000-4000-e500-000000000004', v_tipo_doc_id, 'Bruno Conclusiones Estudiante 4', 'bruno.conc4@uco.net.co', 'ACTIVO'),
      ('00000000-0000-4000-e500-000000000005', v_tipo_doc_id, 'Bruno Conclusiones Estudiante 5', 'bruno.conc5@uco.net.co', 'ACTIVO')
      ON CONFLICT (id) DO NOTHING;

    INSERT INTO sigra.estudiante (id, numero_documento) VALUES
      ('00000000-0000-4000-e500-000000000001', '2000000001'),
      ('00000000-0000-4000-e500-000000000002', '2000000002'),
      ('00000000-0000-4000-e500-000000000003', '2000000003'),
      ('00000000-0000-4000-e500-000000000004', '2000000004'),
      ('00000000-0000-4000-e500-000000000005', '2000000005')
      ON CONFLICT (id) DO NOTHING;
END $$;

-- 5. Niveles de logro de prueba (3 calificaciones cada uno, para que
-- cantidad_calificaciones sea coherente con "evaluado" = nivel_logro no nulo):
--   RA-CONC-01 (5 evaluados): 3 Bajo/Medio, 2 Alto -> 60% -> SI genera.
--   RA-CONC-02 (5 evaluados): 2 Bajo/Medio, 3 Alto  -> 40% -> NO genera.
--   RA-CONC-03 (2 evaluados): 1 Bajo, 1 Alto        -> menos de 3 -> NO genera.
INSERT INTO sigra.nivel_logro_estudiante
    (resultado_aprendizaje_id, semestre_id, estudiante_id, cantidad_calificaciones, promedio_ponderado, nivel_logro, fecha_calculo) VALUES
  ('00000000-0000-4000-e400-000000000001', '00000000-0000-4000-e300-000000000001', '00000000-0000-4000-e500-000000000001', 3, 2.0, 'BAJO', CURRENT_TIMESTAMP),
  ('00000000-0000-4000-e400-000000000001', '00000000-0000-4000-e300-000000000001', '00000000-0000-4000-e500-000000000002', 3, 2.0, 'BAJO', CURRENT_TIMESTAMP),
  ('00000000-0000-4000-e400-000000000001', '00000000-0000-4000-e300-000000000001', '00000000-0000-4000-e500-000000000003', 3, 2.5, 'MEDIO', CURRENT_TIMESTAMP),
  ('00000000-0000-4000-e400-000000000001', '00000000-0000-4000-e300-000000000001', '00000000-0000-4000-e500-000000000004', 3, 4.5, 'ALTO', CURRENT_TIMESTAMP),
  ('00000000-0000-4000-e400-000000000001', '00000000-0000-4000-e300-000000000001', '00000000-0000-4000-e500-000000000005', 3, 4.5, 'ALTO', CURRENT_TIMESTAMP),

  ('00000000-0000-4000-e400-000000000002', '00000000-0000-4000-e300-000000000001', '00000000-0000-4000-e500-000000000001', 3, 2.0, 'BAJO', CURRENT_TIMESTAMP),
  ('00000000-0000-4000-e400-000000000002', '00000000-0000-4000-e300-000000000001', '00000000-0000-4000-e500-000000000002', 3, 2.5, 'MEDIO', CURRENT_TIMESTAMP),
  ('00000000-0000-4000-e400-000000000002', '00000000-0000-4000-e300-000000000001', '00000000-0000-4000-e500-000000000003', 3, 4.5, 'ALTO', CURRENT_TIMESTAMP),
  ('00000000-0000-4000-e400-000000000002', '00000000-0000-4000-e300-000000000001', '00000000-0000-4000-e500-000000000004', 3, 4.5, 'ALTO', CURRENT_TIMESTAMP),
  ('00000000-0000-4000-e400-000000000002', '00000000-0000-4000-e300-000000000001', '00000000-0000-4000-e500-000000000005', 3, 4.5, 'ALTO', CURRENT_TIMESTAMP),

  ('00000000-0000-4000-e400-000000000003', '00000000-0000-4000-e300-000000000001', '00000000-0000-4000-e500-000000000001', 3, 2.0, 'BAJO', CURRENT_TIMESTAMP),
  ('00000000-0000-4000-e400-000000000003', '00000000-0000-4000-e300-000000000001', '00000000-0000-4000-e500-000000000002', 3, 4.5, 'ALTO', CURRENT_TIMESTAMP);

-- Resumen de verificacion.
SELECT ra.codigo,
       COUNT(*) AS evaluados,
       COUNT(*) FILTER (WHERE n.nivel_logro IN ('BAJO', 'MEDIO')) AS en_bajo_o_medio,
       ROUND(100.0 * COUNT(*) FILTER (WHERE n.nivel_logro IN ('BAJO', 'MEDIO')) / COUNT(*), 2) AS porcentaje
  FROM sigra.nivel_logro_estudiante n
  JOIN sigra.resultado_aprendizaje ra ON ra.id = n.resultado_aprendizaje_id
 WHERE ra.codigo LIKE 'RA-CONC-%'
 GROUP BY ra.codigo
 ORDER BY ra.codigo;
