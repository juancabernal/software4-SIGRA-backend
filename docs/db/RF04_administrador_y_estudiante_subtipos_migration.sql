-- =============================================================================
-- SIGRA — Migración: Administrador y Estudiante como subtipos de Usuario (RF-04)
-- Archivo: docs/db/RF04_administrador_y_estudiante_subtipos_migration.sql
-- Motor: PostgreSQL 15+
--
-- Por qué: RF-04 exige autenticar a Administrador, Profesor y Estudiante. Usuario es una
-- clase base abstracta (herencia JOINED): una persona solo puede iniciar sesión si existe
-- como subtipo. Hasta ahora solo existía Profesor. Esta migración:
--   1. Crea sigra.administrador (subtipo sin atributos propios).
--   2. Convierte sigra.estudiante en subtipo: sus datos comunes (tipo de documento, nombre,
--      correo y estado) pasan a sigra.usuario con el MISMO id y en estudiante queda solo
--      numero_documento. Los estudiantes migrados quedan SIN contraseña (password_hash NULL)
--      hasta que se les asigne una.
--
-- Para quién: bases que ya tienen sigra.estudiante con el diseño independiente (creada por
-- SIGRA_SCHEMA_CURRENT.sql anterior o por RF09_estudiante_y_asignacion_docente_migration.sql).
-- Una base creada desde cero con SIGRA_SCHEMA_CURRENT.sql actualizado NO la necesita.
--
-- Es IDEMPOTENTE: si estudiante ya es subtipo, no hace nada. Corre en una sola transacción:
-- si algo falla (por ejemplo un correo de estudiante que ya existe como usuario), no se
-- cambia nada. Nunca borra datos: los mueve.
--
-- Uso (puerto y usuario según tu máquina):
--   psql -h localhost -p <PUERTO> -U <USUARIO> -d postgres -v ON_ERROR_STOP=1 -f docs/db/RF04_administrador_y_estudiante_subtipos_migration.sql
-- =============================================================================

BEGIN;

-- 1) Administrador
CREATE TABLE IF NOT EXISTS sigra.administrador (
    id UUID NOT NULL,
    CONSTRAINT administrador_pkey PRIMARY KEY (id),
    CONSTRAINT fk_administrador_usuario FOREIGN KEY (id) REFERENCES sigra.usuario (id)
);

-- 2) Estudiante pasa a ser subtipo de usuario
DO $$
DECLARE
    v_repetidos TEXT;
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns
                WHERE table_schema = 'sigra' AND table_name = 'estudiante'
                  AND column_name = 'correo_institucional') THEN

        -- 2.1 Un correo no puede repetirse entre un estudiante y otro usuario.
        SELECT string_agg(e.correo_institucional, ', ') INTO v_repetidos
          FROM sigra.estudiante e
          JOIN sigra.usuario u
            ON lower(btrim(u.correo_institucional)) = lower(btrim(e.correo_institucional));
        IF v_repetidos IS NOT NULL THEN
            RAISE EXCEPTION 'Migración cancelada (no se cambió nada): estos correos de estudiantes ya existen como usuarios: %. Corrígelos y vuelve a ejecutar.', v_repetidos;
        END IF;

        -- 2.2 Los estudiantes existentes pasan a usuario (mismo id, sin contraseña todavía).
        INSERT INTO sigra.usuario (id, tipo_documento_id, nombre_completo, correo_institucional,
                                   password_hash, estado)
        SELECT id, tipo_documento_id, nombre_completo, lower(btrim(correo_institucional)),
               NULL, estado
          FROM sigra.estudiante;

        -- 2.3 estudiante conserva solo lo propio. Al quitar las columnas, PostgreSQL también
        --     elimina sus restricciones e índices (FK a tipo_documento, UNIQUEs y CHECK).
        ALTER TABLE sigra.estudiante DROP COLUMN tipo_documento_id;
        ALTER TABLE sigra.estudiante DROP COLUMN nombre_completo;
        ALTER TABLE sigra.estudiante DROP COLUMN correo_institucional;
        ALTER TABLE sigra.estudiante DROP COLUMN estado;
        ALTER TABLE sigra.estudiante
            ADD CONSTRAINT fk_estudiante_usuario FOREIGN KEY (id) REFERENCES sigra.usuario (id);
    END IF;
END $$;

COMMIT;
