-- =============================================================================
-- SIGRA — Datos de prueba para la coleccion Bruno de RF-08/RF-09 (estudiantes y
-- matricula).
-- Archivo: bruno/SIGRA/seed/seed-estudiantes.sql
-- Motor: PostgreSQL 15+
--
-- Es IDEMPOTENTE: borra los datos de prueba anteriores (prefijo BRU en programa,
-- asignatura y semestre; ids fijos en estudiante y matricula) y los vuelve a
-- crear. Ejecutalo ANTES de cada corrida completa de la coleccion, porque las
-- pruebas de 03-modificar-inactivar, 04-matricular y 05-desvincular-consultar
-- cambian el estado de los datos de prueba (un estudiante queda INACTIVO, varias
-- matriculas quedan ACTIVAS/INACTIVAS).
--
-- Crea su PROPIO programa, asignaturas y semestres (prefijo BRUEST) en lugar de
-- reutilizar los de seed-asignaturas.sql o seed-semestres.sql, para que este seed
-- se pueda ejecutar de forma independiente.
--
-- El tipo de documento SI se reutiliza del catalogo compartido (Cedula de
-- Ciudadania), igual que ya hace el modulo de profesores: no se inserta uno
-- propio.
--
-- Solo ASCII (con escapes Unicode donde se necesitan tildes) para que psql en
-- Windows no dane los caracteres.
--
-- Todas las referencias son explicitas (sigra.tabla): no depende del search_path
-- de la sesion y nunca escribe en public.
-- =============================================================================
BEGIN;

-- 0. Prerrequisitos: mensaje claro en vez de un error de tabla inexistente.
DO $$
BEGIN
    IF to_regclass('sigra.estudiante') IS NULL THEN
        RAISE EXCEPTION 'Seed abortado: la tabla sigra.estudiante no existe. Aplique docs/db/RF09_estudiante_y_asignacion_docente_migration.sql o SIGRA_SCHEMA_CURRENT.sql.';
    END IF;
    IF to_regclass('sigra.matricula') IS NULL THEN
        RAISE EXCEPTION 'Seed abortado: la tabla sigra.matricula no existe. Falta su migracion en docs/db (la entidad Matricula del codigo no tiene script de creacion de tabla todavia).';
    END IF;
    IF to_regclass('sigra.tipo_documento') IS NULL THEN
        RAISE EXCEPTION 'Seed abortado: la tabla sigra.tipo_documento no existe. Aplique SIGRA_SCHEMA_CURRENT.sql.';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM sigra.tipo_documento) THEN
        RAISE EXCEPTION 'Seed abortado: sigra.tipo_documento esta vacia. Este seed reutiliza un tipo de documento real del catalogo (p. ej. Cedula de Ciudadania); cargue el catalogo primero.';
    END IF;
END $$;

-- 1. Limpieza de datos de prueba anteriores, en orden que respeta las FKs:
--    matricula -> estudiante / asignatura -> programa_academico / semestre.
DELETE FROM sigra.matricula WHERE id IN (
    '00000000-0000-4000-f400-000000000001'
) OR estudiante_id IN (
    '00000000-0000-4000-f300-000000000001',
    '00000000-0000-4000-f300-000000000002',
    '00000000-0000-4000-f300-000000000003',
    '00000000-0000-4000-f300-000000000004',
    '00000000-0000-4000-f300-000000000005',
    '00000000-0000-4000-f300-000000000006',
    '00000000-0000-4000-f300-000000000007'
) OR asignatura_id IN (
    '00000000-0000-4000-f100-000000000001',
    '00000000-0000-4000-f100-000000000002'
);

DELETE FROM sigra.estudiante WHERE id IN (
    '00000000-0000-4000-f300-000000000001',
    '00000000-0000-4000-f300-000000000002',
    '00000000-0000-4000-f300-000000000003',
    '00000000-0000-4000-f300-000000000004',
    '00000000-0000-4000-f300-000000000005',
    '00000000-0000-4000-f300-000000000006',
    '00000000-0000-4000-f300-000000000007'
);

DELETE FROM sigra.asignatura WHERE codigo LIKE 'BRUEST%';
DELETE FROM sigra.programa_academico WHERE codigo = 'BRU-EST';
DELETE FROM sigra.semestre WHERE codigo LIKE 'BRUEST-%';

-- 2. Programa y asignaturas propias (BRUEST), para no acoplarse al seed de asignaturas.
INSERT INTO sigra.programa_academico (id, nombre, codigo, estado) VALUES
  ('00000000-0000-4000-f000-000000000001', 'Programa Bruno Estudiantes', 'BRU-EST', 'ACTIVO');

INSERT INTO sigra.asignatura (id, codigo, nombre, programa_id, estado) VALUES
  ('00000000-0000-4000-f100-000000000001', 'BRUEST10', 'Bruno Estudiantes Activa',   '00000000-0000-4000-f000-000000000001', 'ACTIVA'),
  ('00000000-0000-4000-f100-000000000002', 'BRUEST11', 'Bruno Estudiantes Borrador', '00000000-0000-4000-f000-000000000001', 'BORRADOR');

-- 3. Semestres propios (BRUEST-), con anios lejanos para no cruzarse con semestres
-- reales ni con los de seed-semestres.sql (que usa 1990/2090/2091/2098).
INSERT INTO sigra.semestre (id, codigo, fecha_inicio, fecha_fin, estado) VALUES
  ('00000000-0000-4000-f200-000000000001', 'BRUEST-A1', '2080-01-20', '2080-06-10', 'ACTIVO'),
  ('00000000-0000-4000-f200-000000000002', 'BRUEST-A2', '2081-01-20', '2081-06-10', 'ACTIVO'),
  ('00000000-0000-4000-f200-000000000003', 'BRUEST-IN', '1980-01-20', '1980-06-10', 'INACTIVO');

-- 4. Estudiantes de prueba. Todos usan el mismo tipo de documento real del
-- catalogo compartido (el mismo que usa environments/local.bru en tipoDocCc, y
-- que ya reutiliza el modulo de profesores); documentos y correos son unicos
-- entre si. Se valida antes que el id exista, para fallar con un mensaje claro
-- en vez de un error de FK si el catalogo de este entorno es distinto.
DO $$
DECLARE
    v_tipo_doc_id UUID := '07219e8d-126d-44cb-9d22-0f57afebec90';
BEGIN
    IF NOT EXISTS (SELECT 1 FROM sigra.tipo_documento WHERE id = v_tipo_doc_id) THEN
        RAISE EXCEPTION 'Seed abortado: no existe el tipo de documento % (el mismo id que environments/local.bru usa en tipoDocCc). Ajuste v_tipo_doc_id en este script al id real de "Cedula de Ciudadania" en su base.', v_tipo_doc_id;
    END IF;

    INSERT INTO sigra.estudiante (id, tipo_documento_id, numero_documento, nombre_completo, correo_institucional, estado) VALUES
      ('00000000-0000-4000-f300-000000000001', v_tipo_doc_id, '1000000001', 'Bruno Registrado Original',          'bruno.registrado@uco.net.co',       'ACTIVO'),
      ('00000000-0000-4000-f300-000000000002', v_tipo_doc_id, '1000000002', 'Bruno Esperanza Quintero Consulta',  'bruno.consulta@uco.net.co',         'ACTIVO'),
      ('00000000-0000-4000-f300-000000000003', v_tipo_doc_id, '1000000003', 'Bruno Modificar Original',           'bruno.modificar@uco.net.co',        'ACTIVO'),
      ('00000000-0000-4000-f300-000000000004', v_tipo_doc_id, '1000000004', 'Bruno Otro Correo',                  'bruno.otrocorreo@uco.net.co',       'ACTIVO'),
      ('00000000-0000-4000-f300-000000000005', v_tipo_doc_id, '1000000005', 'Bruno Matricula Valida',             'bruno.matricula@uco.net.co',        'ACTIVO'),
      ('00000000-0000-4000-f300-000000000006', v_tipo_doc_id, '1000000006', 'Bruno Matricula Inactivo',           'bruno.inactivo@uco.net.co',         'INACTIVO'),
      ('00000000-0000-4000-f300-000000000007', v_tipo_doc_id, '1000000007', 'Bruno Listado Matriculados',         'bruno.listado@uco.net.co',          'ACTIVO');
END $$;

-- 5. Matricula pre-sembrada e INACTIVA, para que el listado de matriculados de
-- BRUEST10/BRUEST-A1 tenga una fila inactiva ademas de la que crea/retira/
-- reactiva la coleccion (04-matricular y 05-desvincular-consultar).
INSERT INTO sigra.matricula (id, estudiante_id, asignatura_id, semestre_id, estado) VALUES
  ('00000000-0000-4000-f400-000000000001',
   '00000000-0000-4000-f300-000000000007',
   '00000000-0000-4000-f100-000000000001',
   '00000000-0000-4000-f200-000000000001',
   'INACTIVO');

COMMIT;

-- Resumen
SELECT e.numero_documento, e.nombre_completo, e.correo_institucional, e.estado
  FROM sigra.estudiante e
 WHERE e.id::text LIKE '00000000-0000-4000-f300-%'
 ORDER BY e.numero_documento;
