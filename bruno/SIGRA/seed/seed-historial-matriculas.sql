-- =============================================================================
-- SIGRA — Datos de prueba para validar el HISTORIAL de matriculas de semestres
-- anteriores (incidencia: "Consultar el historial de matriculas para localizar
-- registros de semestres anteriores").
-- Archivo: bruno/SIGRA/seed/seed-historial-matriculas.sql
-- Motor: PostgreSQL 15+
--
-- Causa raiz de la incidencia: existian semestres ya finalizados (p. ej. el
-- semestre real 2026-1, que termino el 2026-06-15), pero NINGUNA matricula
-- estaba asociada a un semestre con fecha_fin en el pasado. Por eso el
-- historial de un estudiante (GET /api/v1/estudiantes/{id}/asignaturas, que
-- usa MatriculaRepository.buscarPorEstudiante sin filtrar por semestre) no
-- tenia ningun registro de un semestre anterior contra el cual validar el
-- comportamiento.
--
-- Esta es una fila AISLADA (prefijo HIST en programa/asignatura, y un
-- estudiante propio con correo historial.qa@uco.net.co) que NO modifica ningun
-- dato existente: solo LEE el id del semestre real 2026-1 (ya finalizado) para
-- usarlo como FK de una matricula nueva. No se borra ni se actualiza esa fila
-- de semestre.
--
-- Es IDEMPOTENTE: borra su propia fila de prueba anterior (por id fijo) y la
-- vuelve a crear.
--
-- Todas las referencias son explicitas (sigra.tabla): no depende del
-- search_path de la sesion y nunca escribe en public.
-- =============================================================================
BEGIN;

-- 0. Prerrequisitos: mensaje claro en vez de un error generico.
DO $$
BEGIN
    IF to_regclass('sigra.matricula') IS NULL THEN
        RAISE EXCEPTION 'Seed abortado: la tabla sigra.matricula no existe.';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM sigra.semestre WHERE codigo = '2026-1') THEN
        RAISE EXCEPTION 'Seed abortado: no existe el semestre 2026-1 esperado como semestre anterior de referencia.';
    END IF;
END $$;

-- 1. Limpieza de la fila de prueba anterior (ids fijos, prefijo HIST), en orden
--    que respeta las FKs: matricula -> estudiante/asignatura -> programa.
DELETE FROM sigra.matricula WHERE id = '10000000-0000-4000-9000-000000000004';
DELETE FROM sigra.estudiante WHERE id = '10000000-0000-4000-9000-000000000003';
DELETE FROM sigra.usuario WHERE id = '10000000-0000-4000-9000-000000000003';
DELETE FROM sigra.asignatura WHERE id = '10000000-0000-4000-9000-000000000002';
DELETE FROM sigra.programa_academico WHERE id = '10000000-0000-4000-9000-000000000001';

-- 2. Programa y asignatura propios de esta incidencia (prefijo HIST), para no
--    acoplarse a datos de otros seeds.
INSERT INTO sigra.programa_academico (id, nombre, codigo, estado) VALUES
  ('10000000-0000-4000-9000-000000000001', 'Programa Historial QA', 'HIST-QA', 'ACTIVO');

INSERT INTO sigra.asignatura (id, codigo, nombre, programa_id, estado) VALUES
  ('10000000-0000-4000-9000-000000000002', 'HISTQA01', 'Asignatura Historial QA', '10000000-0000-4000-9000-000000000001', 'ACTIVA');

-- 3. Estudiante de prueba. Reutiliza el tipo de documento real del catalogo
--    compartido (Cedula de Ciudadania), igual que seed-estudiantes.sql.
DO $$
DECLARE
    v_tipo_doc_id UUID;
BEGIN
    SELECT id INTO v_tipo_doc_id
      FROM sigra.tipo_documento
     WHERE lower(nombre) = lower('Cédula de Ciudadanía')
     ORDER BY id
     LIMIT 1;
    IF NOT EXISTS (SELECT 1 FROM sigra.tipo_documento WHERE id = v_tipo_doc_id) THEN
        RAISE EXCEPTION 'Seed abortado: no existe el tipo de documento Cedula de Ciudadania';
    END IF;

    INSERT INTO sigra.usuario (id, tipo_documento_id, nombre_completo, correo_institucional, estado) VALUES
      ('10000000-0000-4000-9000-000000000003', v_tipo_doc_id, 'Estudiante Historial QA', 'historial.qa@uco.net.co', 'ACTIVO');

    INSERT INTO sigra.estudiante (id, numero_documento) VALUES
      ('10000000-0000-4000-9000-000000000003', '9000000001');
END $$;

-- 4. Matricula ligada al semestre REAL 2026-1 (ya finalizado: fecha_fin
--    2026-06-15), referenciado solo por su codigo para no depender de que su
--    id no cambie entre entornos. fecha_matricula cae dentro del rango de ese
--    semestre (fecha_inicio 2026-02-01 .. fecha_fin 2026-06-15).
INSERT INTO sigra.matricula (id, estudiante_id, asignatura_id, semestre_id, fecha_matricula, estado)
SELECT '10000000-0000-4000-9000-000000000004',
       '10000000-0000-4000-9000-000000000003',
       '10000000-0000-4000-9000-000000000002',
       s.id,
       '2026-02-10'::date,
       'ACTIVA'
  FROM sigra.semestre s
 WHERE s.codigo = '2026-1';

COMMIT;

-- Resumen: historial del estudiante de prueba, debe mostrar el semestre 2026-1
-- (ya finalizado) junto con su fecha_fin para evidenciar que es un semestre
-- anterior.
SELECT u.nombre_completo, a.codigo AS asignatura, s.codigo AS semestre,
       s.fecha_fin AS semestre_fecha_fin, m.fecha_matricula, m.estado
  FROM sigra.matricula m
  JOIN sigra.estudiante e ON e.id = m.estudiante_id
  JOIN sigra.usuario u ON u.id = e.id
  JOIN sigra.asignatura a ON a.id = m.asignatura_id
  JOIN sigra.semestre s ON s.id = m.semestre_id
 WHERE e.id = '10000000-0000-4000-9000-000000000003';
