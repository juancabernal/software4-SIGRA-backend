-- =============================================================================
-- SIGRA — Migración LEGACY RF-04: herencia JOINED Usuario -> Profesor
-- Archivo: docs/db/RF04_usuario_migration.sql
-- Motor: PostgreSQL 15+
--
-- ESTADO:
--   LEGACY. Pasa una base con el modelo antiguo (atributos comunes dentro de 'profesor')
--   al modelo actual (usuario + profesor). Ya está aplicada en la base local de
--   referencia; la tabla sigra.profesor de esa base NO tiene columnas antiguas.
--   Para una base vacía NO se usa esta migración: se usa docs/db/SIGRA_SCHEMA_CURRENT.sql.
--
-- PROPÓSITO:
--   - usuario  (id PK, tipo_documento_id, nombre_completo, correo_institucional,
--               password_hash, estado, intentos_fallidos, fecha_bloqueo)
--   - profesor (id PK y FK a usuario(id), numero_documento)
--
-- ALCANCE:
--   Todas las tablas están calificadas con el schema sigra (sigra.usuario, sigra.profesor,
--   sigra.tipo_documento). "sigra" es un SCHEMA dentro de la base "postgres", no una base.
--   NO modifica el schema public ni ninguna otra base.
--
-- ORDEN OBLIGATORIO (manual y supervisado):
--   1. Backup completo (pg_dump). OBLIGATORIO.
--   2. Ejecutar docs/db/RF04_schema_preflight.sql y revisar el resultado.
--   3. Ejecutar esta migración con ON_ERROR_STOP (puerto según tu máquina):
--        psql -h localhost -p <PUERTO> -U <USUARIO> -d postgres -v ON_ERROR_STOP=1 -f docs/db/RF04_usuario_migration.sql
--   4. Después, arrancar el backend (ddl-auto=validate por defecto).
--   5. Después, cargar el seed de Bruno (bruno/SIGRA/seed/seed-auth.sql).
--
-- GARANTÍAS Y LÍMITES:
--   - Todo ocurre dentro de BEGIN ... COMMIT: ante cualquier error no queda nada aplicado.
--   - Aborta (RAISE EXCEPTION) si: hay profesores sin usuario, correos duplicados ignorando
--     mayúsculas y espacios, un correo de profesor pertenece a otro usuario, o los datos
--     migrados no coinciden. Antes de borrar columnas antiguas se comprueba todo lo anterior.
--   - NO elimina duplicados automáticamente.
--   - NO usa ON CONFLICT DO NOTHING: ocultaría un usuario preexistente con datos distintos.
--   - NO hace DROP DATABASE ni DROP TABLE. Solo elimina columnas antiguas de sigra.profesor,
--     y únicamente después de las comprobaciones.
--   - Los profesores migrados quedan con password_hash NULL: no pueden iniciar sesión hasta
--     que se les asigne credencial (ver PASSWORD_PROVISIONING_FLOW en el README).
-- =============================================================================

BEGIN;

-- -----------------------------------------------------------------------------
-- PASO 1: Crear la tabla base sigra.usuario si no existe
-- -----------------------------------------------------------------------------
CREATE SCHEMA IF NOT EXISTS sigra;

CREATE TABLE IF NOT EXISTS sigra.usuario (
    id UUID NOT NULL,
    tipo_documento_id UUID NOT NULL,
    nombre_completo VARCHAR(255) NOT NULL,
    correo_institucional VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255),
    estado VARCHAR(50) NOT NULL DEFAULT 'ACTIVO',
    intentos_fallidos INTEGER NOT NULL DEFAULT 0,
    fecha_bloqueo TIMESTAMP WITHOUT TIME ZONE,
    CONSTRAINT pk_usuario PRIMARY KEY (id),
    CONSTRAINT uk_usuario_correo UNIQUE (correo_institucional),
    CONSTRAINT fk_usuario_tipo_documento FOREIGN KEY (tipo_documento_id)
        REFERENCES sigra.tipo_documento (id)
);

-- -----------------------------------------------------------------------------
-- PASO 2: Comprobaciones previas al backfill (solo si sigra.profesor aún tiene el esquema antiguo)
-- -----------------------------------------------------------------------------
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'sigra' AND table_name = 'profesor' AND column_name = 'correo_institucional'
    ) THEN
        RAISE NOTICE 'sigra.profesor ya no tiene columnas antiguas: se omiten las comprobaciones y el backfill.';
        RETURN;
    END IF;

    IF EXISTS (
        SELECT 1 FROM sigra.profesor
        GROUP BY LOWER(TRIM(correo_institucional))
        HAVING COUNT(*) > 1
    ) THEN
        RAISE EXCEPTION 'Migración RF04 abortada: existen correos de profesor duplicados ignorando mayúsculas y espacios. Corríjalos manualmente; esta migración no los elimina.';
    END IF;

    IF EXISTS (
        SELECT 1
        FROM sigra.profesor p
        JOIN sigra.usuario u ON LOWER(TRIM(u.correo_institucional)) = LOWER(TRIM(p.correo_institucional))
        WHERE u.id <> p.id
    ) THEN
        RAISE EXCEPTION 'Migración RF04 abortada: el correo de un profesor ya pertenece a otro usuario.';
    END IF;
END $$;

-- -----------------------------------------------------------------------------
-- PASO 3: Backfill de profesores hacia sigra.usuario.
--         Conserva el mismo UUID como PK. Solo inserta profesores que aún no tienen usuario;
--         los existentes se verifican en el PASO 4 (no se usa ON CONFLICT DO NOTHING).
-- -----------------------------------------------------------------------------
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'sigra' AND table_name = 'profesor' AND column_name = 'correo_institucional'
    ) THEN
        RETURN;
    END IF;

    INSERT INTO sigra.usuario (
        id, tipo_documento_id, nombre_completo, correo_institucional,
        password_hash, estado, intentos_fallidos, fecha_bloqueo
    )
    SELECT
        p.id,
        p.tipo_documento_id,
        p.nombre_completo,
        LOWER(TRIM(p.correo_institucional)),
        NULL, -- transición segura: no se inventan contraseñas
        COALESCE(p.estado, 'ACTIVO'),
        COALESCE(p.intentos_fallidos, 0),
        p.fecha_bloqueo
    FROM sigra.profesor p
    WHERE NOT EXISTS (SELECT 1 FROM sigra.usuario u WHERE u.id = p.id);

    RAISE NOTICE 'Backfill de profesores a sigra.usuario completado.';
END $$;

-- -----------------------------------------------------------------------------
-- PASO 4: Verificaciones POSTERIORES al backfill. Si alguna falla, se aborta todo.
-- -----------------------------------------------------------------------------
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'sigra' AND table_name = 'profesor' AND column_name = 'correo_institucional'
    ) THEN
        RETURN;
    END IF;

    -- 4.1 Todo profesor debe tener su usuario
    IF EXISTS (
        SELECT 1
        FROM sigra.profesor p
        LEFT JOIN sigra.usuario u ON u.id = p.id
        WHERE u.id IS NULL
    ) THEN
        RAISE EXCEPTION 'Migración RF04 abortada: existen profesores sin usuario';
    END IF;

    -- 4.2 Los campos migrados deben coincidir con las columnas antiguas
    IF EXISTS (
        SELECT 1
        FROM sigra.profesor p
        JOIN sigra.usuario u ON u.id = p.id
        WHERE u.tipo_documento_id IS DISTINCT FROM p.tipo_documento_id
           OR u.nombre_completo IS DISTINCT FROM p.nombre_completo
           OR u.correo_institucional IS DISTINCT FROM LOWER(TRIM(p.correo_institucional))
           OR u.estado IS DISTINCT FROM COALESCE(p.estado, 'ACTIVO')
           OR u.intentos_fallidos IS DISTINCT FROM COALESCE(p.intentos_fallidos, 0)
           OR u.fecha_bloqueo IS DISTINCT FROM p.fecha_bloqueo
    ) THEN
        RAISE EXCEPTION 'Migración RF04 abortada: los datos migrados de profesor no coinciden con usuario. No se eliminan columnas.';
    END IF;

    -- 4.3 La identidad documental (tipoDocumento, numeroDocumento) debe ser única.
    --     Se verifica aquí y en la aplicación, no como restricción de base de datos.
    IF EXISTS (
        SELECT 1
        FROM sigra.profesor p
        JOIN sigra.usuario u ON u.id = p.id
        GROUP BY u.tipo_documento_id, p.numero_documento
        HAVING COUNT(*) > 1
    ) THEN
        RAISE EXCEPTION 'Migración RF04 abortada: existen documentos (tipo, número) duplicados entre profesores.';
    END IF;
END $$;

-- -----------------------------------------------------------------------------
-- PASO 5: Asegurar la FK profesor.id -> usuario.id
-- -----------------------------------------------------------------------------
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE constraint_schema = 'sigra' AND constraint_name = 'fk_profesor_usuario' AND table_name = 'profesor'
    ) THEN
        ALTER TABLE sigra.profesor
            ADD CONSTRAINT fk_profesor_usuario FOREIGN KEY (id)
            REFERENCES sigra.usuario (id);
    END IF;
END $$;

-- -----------------------------------------------------------------------------
-- PASO 6: Eliminar de sigra.profesor las columnas comunes que ahora residen en sigra.usuario.
--         Solo se llega aquí si los PASOS 2 a 5 no lanzaron ninguna excepción.
-- -----------------------------------------------------------------------------
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE constraint_schema = 'sigra' AND constraint_name = 'profesor_correo_institucional_key' AND table_name = 'profesor'
    ) THEN
        ALTER TABLE sigra.profesor DROP CONSTRAINT profesor_correo_institucional_key;
    END IF;

    IF EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE constraint_schema = 'sigra' AND constraint_name = 'fk_profesor_tipo_documento' AND table_name = 'profesor'
    ) THEN
        ALTER TABLE sigra.profesor DROP CONSTRAINT fk_profesor_tipo_documento;
    END IF;

    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = 'sigra' AND table_name = 'profesor' AND column_name = 'correo_institucional') THEN
        ALTER TABLE sigra.profesor DROP COLUMN correo_institucional;
    END IF;

    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = 'sigra' AND table_name = 'profesor' AND column_name = 'nombre_completo') THEN
        ALTER TABLE sigra.profesor DROP COLUMN nombre_completo;
    END IF;

    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = 'sigra' AND table_name = 'profesor' AND column_name = 'estado') THEN
        ALTER TABLE sigra.profesor DROP COLUMN estado;
    END IF;

    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = 'sigra' AND table_name = 'profesor' AND column_name = 'intentos_fallidos') THEN
        ALTER TABLE sigra.profesor DROP COLUMN intentos_fallidos;
    END IF;

    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = 'sigra' AND table_name = 'profesor' AND column_name = 'fecha_bloqueo') THEN
        ALTER TABLE sigra.profesor DROP COLUMN fecha_bloqueo;
    END IF;

    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = 'sigra' AND table_name = 'profesor' AND column_name = 'tipo_documento_id') THEN
        ALTER TABLE sigra.profesor DROP COLUMN tipo_documento_id;
    END IF;

    RAISE NOTICE 'Limpieza de columnas redundantes en sigra.profesor completada.';
END $$;

COMMIT;
