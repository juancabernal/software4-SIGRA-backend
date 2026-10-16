-- =============================================================================
-- SIGRA — Datos de prueba para la coleccion Bruno de RF-08/RF-09 (estudiantes y
-- matricula).
-- Archivo: bruno/SIGRA/seed/seed-estudiantes.sql
-- Motor: PostgreSQL 15+
--
-- Es IDEMPOTENTE: borra los datos de prueba anteriores (prefijo BRU en programa,
-- asignatura y semestre; ids fijos en estudiante y matricula) y los vuelve a
-- crear. Ejecutalo ANTES de cada corrida completa de la coleccion, porque las
-- pruebas de 03-modificar-inactivar, 04-matricular, 05-desvincular-consultar,
-- 06-registrar-con-matricula y 07-confirmacion-desvincular cambian el estado de
-- los datos de prueba (un estudiante queda INACTIVO, varias matriculas quedan
-- ACTIVAS/DESVINCULADAS).
--
-- Requiere que docs/db/RF08_matricula_fecha_y_estado_migration.sql ya se haya
-- corrido: la columna fecha_matricula es NOT NULL y el CHECK de estado solo
-- admite 'ACTIVA'/'DESVINCULADA' desde esa migracion. Si no se corrio, este
-- seed debe fallar ruidosamente (columna inexistente o violacion del CHECK
-- viejo) en vez de insertar datos a medias: es la senal mas temprana de un
-- entorno sin migrar.
--
-- Crea su PROPIO programa, asignaturas y semestres (prefijo BRUEST) en lugar de
-- reutilizar los de seed-asignaturas.sql o seed-semestres.sql, para que este seed
-- se pueda ejecutar de forma independiente. Los dos semestres de prueba activos
-- usan fechas relativas a CURRENT_DATE para no caducar con el calendario.
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
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
         WHERE table_schema = 'sigra' AND table_name = 'matricula' AND column_name = 'fecha_matricula'
    ) THEN
        RAISE EXCEPTION 'Seed abortado: sigra.matricula no tiene la columna fecha_matricula. Aplique docs/db/RF08_matricula_fecha_y_estado_migration.sql antes de volver a correr este seed.';
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
    '00000000-0000-4000-f300-000000000007',
    '00000000-0000-4000-f300-000000000008'
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
    '00000000-0000-4000-f300-000000000007',
    '00000000-0000-4000-f300-000000000008'
);

DELETE FROM sigra.usuario WHERE id IN (
    '00000000-0000-4000-f300-000000000001',
    '00000000-0000-4000-f300-000000000002',
    '00000000-0000-4000-f300-000000000003',
    '00000000-0000-4000-f300-000000000004',
    '00000000-0000-4000-f300-000000000005',
    '00000000-0000-4000-f300-000000000006',
    '00000000-0000-4000-f300-000000000007',
    '00000000-0000-4000-f300-000000000008'
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

-- 3. Semestres propios (BRUEST-). BRUEST-A1/A2 usan fechas RELATIVAS a hoy
-- (CURRENT_DATE +/- un margen amplio) en lugar de anios fijos lejanos: desde el
-- cambio matricula-desvinculacion (design.md D3), la vigencia de un semestre se
-- juzga por sus FECHAS, no por su columna estado, asi que un semestre con
-- fecha_inicio en 2080 nunca es vigente hoy y el camino feliz de 04-matricular
-- fallaria con 400 "no esta vigente" en vez de 201. (Hallazgo confirmado
-- ejecutando la coleccion contra el backend real: con los anios fijos viejos,
-- SemestreServiceImpl.actualizarEstadoSiCambio ya habia recalculado estas filas
-- a INACTIVO apenas alguien las consultaba.) BRUEST-IN si puede seguir fija en
-- el pasado (1980): una fecha_fin ya pasada sigue siendo "no vigente" sin
-- importar cuando se ejecute este seed, que es exactamente lo que ese caso
-- necesita probar.
INSERT INTO sigra.semestre (id, codigo, fecha_inicio, fecha_fin, estado) VALUES
  ('00000000-0000-4000-f200-000000000001', 'BRUEST-A1', (CURRENT_DATE - INTERVAL '2 years')::date, (CURRENT_DATE + INTERVAL '3 years')::date, 'ACTIVO'),
  ('00000000-0000-4000-f200-000000000002', 'BRUEST-A2', (CURRENT_DATE - INTERVAL '1 year')::date,  (CURRENT_DATE + INTERVAL '4 years')::date, 'ACTIVO'),
  ('00000000-0000-4000-f200-000000000003', 'BRUEST-IN', '1980-01-20', '1980-06-10', 'INACTIVO');

-- 4. Estudiantes de prueba. Desde RF-04, Estudiante es subtipo de Usuario: tipo
-- de documento, nombre, correo y estado viven en sigra.usuario; sigra.estudiante
-- solo guarda su id (FK a usuario) y el numero de documento. Todos usan el mismo
-- tipo de documento real del catalogo compartido (el mismo que usa
-- environments/local.bru en tipoDocCc, y que ya reutiliza el modulo de
-- profesores); documentos y correos son unicos entre si. Se valida antes que el
-- id exista, para fallar con un mensaje claro en vez de un error de FK si el
-- catalogo de este entorno es distinto.
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
        RAISE EXCEPTION 'Seed abortado: no existe el tipo de documento Cédula de Ciudadanía';
    END IF;

    INSERT INTO sigra.usuario (id, tipo_documento_id, nombre_completo, correo_institucional, estado) VALUES
      ('00000000-0000-4000-f300-000000000001', v_tipo_doc_id, 'Bruno Registrado Original',          'bruno.registrado@uco.net.co',       'ACTIVO'),
      ('00000000-0000-4000-f300-000000000002', v_tipo_doc_id, 'Bruno Esperanza Quintero Consulta',  'bruno.consulta@uco.net.co',         'ACTIVO'),
      ('00000000-0000-4000-f300-000000000003', v_tipo_doc_id, 'Bruno Modificar Original',           'bruno.modificar@uco.net.co',        'ACTIVO'),
      ('00000000-0000-4000-f300-000000000004', v_tipo_doc_id, 'Bruno Otro Correo',                  'bruno.otrocorreo@uco.net.co',       'ACTIVO'),
      ('00000000-0000-4000-f300-000000000005', v_tipo_doc_id, 'Bruno Matricula Valida',             'bruno.matricula@uco.net.co',        'ACTIVO'),
      ('00000000-0000-4000-f300-000000000006', v_tipo_doc_id, 'Bruno Matricula Inactivo',           'bruno.inactivo@uco.net.co',         'INACTIVO'),
      ('00000000-0000-4000-f300-000000000007', v_tipo_doc_id, 'Bruno Listado Matriculados',         'bruno.listado@uco.net.co',          'ACTIVO'),
      ('00000000-0000-4000-f300-000000000008', v_tipo_doc_id, 'Bruno Confirmacion Desvincular',     'bruno.confirmacion@uco.net.co',     'ACTIVO');

    INSERT INTO sigra.estudiante (id, numero_documento) VALUES
      ('00000000-0000-4000-f300-000000000001', '1000000001'),
      ('00000000-0000-4000-f300-000000000002', '1000000002'),
      ('00000000-0000-4000-f300-000000000003', '1000000003'),
      ('00000000-0000-4000-f300-000000000004', '1000000004'),
      ('00000000-0000-4000-f300-000000000005', '1000000005'),
      ('00000000-0000-4000-f300-000000000006', '1000000006'),
      ('00000000-0000-4000-f300-000000000007', '1000000007'),
      ('00000000-0000-4000-f300-000000000008', '1000000008');
END $$;

-- 5. Matricula pre-sembrada y DESVINCULADA, para que el listado de matriculados
-- de BRUEST10/BRUEST-A1 tenga una fila desvinculada ademas de la que crea/
-- retira/reactiva la coleccion (04-matricular y 05-desvincular-consultar).
-- fecha_matricula es relativa a hoy (como el rango de BRUEST-A1 de la seccion 3)
-- para que siga siendo una fecha plausible dentro de ese semestre sin importar
-- cuando se ejecute este seed.
INSERT INTO sigra.matricula (id, estudiante_id, asignatura_id, semestre_id, fecha_matricula, estado) VALUES
  ('00000000-0000-4000-f400-000000000001',
   '00000000-0000-4000-f300-000000000007',
   '00000000-0000-4000-f100-000000000001',
   '00000000-0000-4000-f200-000000000001',
   (CURRENT_DATE - INTERVAL '6 months')::date,
   'DESVINCULADA');

COMMIT;

-- Resumen
SELECT e.numero_documento, u.nombre_completo, u.correo_institucional, u.estado
  FROM sigra.estudiante e
  JOIN sigra.usuario u ON u.id = e.id
 WHERE e.id::text LIKE '00000000-0000-4000-f300-%'
 ORDER BY e.numero_documento;
