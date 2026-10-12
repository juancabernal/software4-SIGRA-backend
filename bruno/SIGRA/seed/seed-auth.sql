-- =============================================================================
-- SIGRA — Datos de prueba para la colección Bruno de RF-04 (Autenticación / Login)
-- Archivo: bruno/SIGRA/seed/seed-auth.sql
-- Motor: PostgreSQL 15+
--
-- Opera SOLO sobre el schema "sigra" (dentro de la base "postgres"). No toca "public".
--
-- ES IDEMPOTENTE:
--   Borra los registros de prueba previos y los vuelve a crear con estado inicial
--   (intentos_fallidos = 0, fecha_bloqueo = null).
--   Ejecútalo antes de cada corrida de la colección Bruno de auth.
--
-- PREREQUISITOS (si no se cumplen, el script aborta sin cambiar nada):
--   1. El schema sigra existe y tiene las tablas del backend
--      (docs/db/SIGRA_SCHEMA_CURRENT.sql en una base vacía, o la base ya migrada).
--   2. La tabla sigra.usuario existe y sigra.profesor ya no tiene columnas antiguas.
--
-- CREDENCIALES DE PRUEBA (SOLO PARA TESTING LOCAL DE BRUNO; no son reales):
--   1. Usuario normal:
--      Correo: profesor.bruno@uco.net.co
--      Contraseña plana: PasswordSeguro123*
--      Hash BCrypt (factor 12): $2a$12$0xeKtqgxyBKM4Ks0ZWgxJu0ooSpm0OFeZ7hILuTE4henvC4dtDmjW
--      Rol: PROFESOR
--
--   2. Usuario para pruebas de bloqueo:
--      Correo: profesor.bloqueo@uco.net.co
--      Contraseña plana: PasswordSeguro123*
--      Hash BCrypt (factor 12): $2a$12$0xeKtqgxyBKM4Ks0ZWgxJu0ooSpm0OFeZ7hILuTE4henvC4dtDmjW
--      Rol: PROFESOR
--
-- EJECUCIÓN (puerto según tu máquina; sustituye <USUARIO> y <PUERTO>):
--   psql -h localhost -p <PUERTO> -U <USUARIO> -d postgres -v ON_ERROR_STOP=1 -f bruno/SIGRA/seed/seed-auth.sql
-- =============================================================================

BEGIN;

-- 0. Comprobaciones de prerrequisitos: mensaje claro en vez de un error de columna
DO $$
BEGIN
    IF to_regclass('sigra.usuario') IS NULL THEN
        RAISE EXCEPTION 'Seed abortado: la tabla sigra.usuario no existe. Cree el esquema con docs/db/SIGRA_SCHEMA_CURRENT.sql o aplique docs/db/RF04_usuario_migration.sql.';
    END IF;
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'sigra' AND table_name = 'profesor' AND column_name = 'correo_institucional'
    ) THEN
        RAISE EXCEPTION 'Seed abortado: sigra.profesor aún tiene columnas antiguas. Aplique primero docs/db/RF04_usuario_migration.sql.';
    END IF;
END $$;

-- 1. Limpieza de datos previos de prueba de autenticación
-- sigra.asignacion_docente puede no existir en bases antiguas; se borra solo si la tabla está presente.
DO $$
BEGIN
    IF to_regclass('sigra.asignacion_docente') IS NOT NULL THEN
        DELETE FROM sigra.asignacion_docente WHERE profesor_id IN (
            '00000000-0000-4000-e000-000000000001',
            '00000000-0000-4000-e000-000000000002'
        );
    END IF;
END $$;

DELETE FROM sigra.profesor WHERE id IN (
    '00000000-0000-4000-e000-000000000001',
    '00000000-0000-4000-e000-000000000002'
);

DELETE FROM sigra.usuario WHERE id IN (
    '00000000-0000-4000-e000-000000000001',
    '00000000-0000-4000-e000-000000000002'
);

-- 2. Asegurar TipoDocumento base
INSERT INTO sigra.tipo_documento (id, nombre)
VALUES ('00000000-0000-4000-d000-000000000001', 'Cédula de Ciudadanía')
ON CONFLICT (nombre) DO NOTHING;

-- Obtener el id de TipoDocumento (sea el insertado o preexistente) y crear los usuarios de prueba
DO $$
DECLARE
    v_tipo_doc_id UUID;
    v_hash VARCHAR := '$2a$12$0xeKtqgxyBKM4Ks0ZWgxJu0ooSpm0OFeZ7hILuTE4henvC4dtDmjW';
BEGIN
    SELECT id INTO v_tipo_doc_id FROM sigra.tipo_documento WHERE nombre = 'Cédula de Ciudadanía' LIMIT 1;

    -- 3. Usuario 1 (Profesor normal para login exitoso)
    INSERT INTO sigra.usuario (
        id, tipo_documento_id, nombre_completo, correo_institucional,
        password_hash, estado, intentos_fallidos, fecha_bloqueo
    ) VALUES (
        '00000000-0000-4000-e000-000000000001',
        v_tipo_doc_id,
        'Profesor Bruno',
        'profesor.bruno@uco.net.co',
        v_hash,
        'ACTIVO',
        0,
        NULL
    );

    INSERT INTO sigra.profesor (id, numero_documento)
    VALUES ('00000000-0000-4000-e000-000000000001', '1234567890');

    -- 4. Usuario 2 (Profesor para pruebas de intentos fallidos y bloqueo)
    INSERT INTO sigra.usuario (
        id, tipo_documento_id, nombre_completo, correo_institucional,
        password_hash, estado, intentos_fallidos, fecha_bloqueo
    ) VALUES (
        '00000000-0000-4000-e000-000000000002',
        v_tipo_doc_id,
        'Profesor Pruebas Bloqueo',
        'profesor.bloqueo@uco.net.co',
        v_hash,
        'ACTIVO',
        0,
        NULL
    );

    INSERT INTO sigra.profesor (id, numero_documento)
    VALUES ('00000000-0000-4000-e000-000000000002', '9876543210');
END $$;

COMMIT;
