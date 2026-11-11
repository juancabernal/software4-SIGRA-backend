-- =============================================================================
-- RF-19 / RF-21c — Historial de auditoría de solo inserción
-- Archivo: docs/db/RF19_historial_auditoria_migration.sql
--
-- Para una base que ya tiene el esquema actual. Es idempotente: se puede correr más de una vez.
-- En una base nueva basta con SIGRA_SCHEMA_CURRENT.sql, que ya incluye la tabla y sus triggers.
--
--   psql -h localhost -p <PUERTO> -U <USUARIO> -d postgres -v ON_ERROR_STOP=1 -f docs/db/RF19_historial_auditoria_migration.sql
--
-- Retención (RNF-16): ningún registro se puede borrar, ni por la aplicación ni a mano, así que todos
-- se conservan como mínimo 5 años. Si algún día se define una depuración de registros con más de 5
-- años, deberá hacerse con una migración revisada que reemplace el trigger; nunca desde la aplicación.
-- =============================================================================

BEGIN;

CREATE TABLE IF NOT EXISTS sigra.historial_auditoria (
                                                         id           UUID         NOT NULL DEFAULT gen_random_uuid(),
    fecha_hora   TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    usuario_id   UUID,
    rol_usuario  VARCHAR(20),
    accion       VARCHAR(40)  NOT NULL,
    tipo_entidad VARCHAR(40)  NOT NULL,
    id_entidad   UUID,
    detalle      VARCHAR(500) NOT NULL,
    CONSTRAINT historial_auditoria_pkey PRIMARY KEY (id),
    CONSTRAINT chk_historial_auditoria_rol CHECK (rol_usuario IS NULL
                                                  OR rol_usuario IN ('ADMINISTRADOR', 'PROFESOR', 'ESTUDIANTE'))
    );

CREATE INDEX IF NOT EXISTS idx_historial_auditoria_fecha ON sigra.historial_auditoria (fecha_hora DESC);
CREATE INDEX IF NOT EXISTS idx_historial_auditoria_tipo_fecha
    ON sigra.historial_auditoria (tipo_entidad, fecha_hora DESC);

-- Solo inserción: cualquier UPDATE, DELETE o TRUNCATE se rechaza para todo usuario de la base,
-- incluido el dueño del esquema y el administrador del sistema.
CREATE OR REPLACE FUNCTION sigra.historial_auditoria_solo_insercion() RETURNS trigger
    LANGUAGE plpgsql AS
$$
BEGIN
    RAISE EXCEPTION 'El historial de auditoría es de solo inserción: % no está permitido', TG_OP
        USING ERRCODE = 'insufficient_privilege';
END;
$$;

DROP TRIGGER IF EXISTS trg_historial_auditoria_sin_cambios ON sigra.historial_auditoria;
CREATE TRIGGER trg_historial_auditoria_sin_cambios
    BEFORE UPDATE OR DELETE ON sigra.historial_auditoria
    FOR EACH ROW EXECUTE FUNCTION sigra.historial_auditoria_solo_insercion();

DROP TRIGGER IF EXISTS trg_historial_auditoria_sin_truncate ON sigra.historial_auditoria;
CREATE TRIGGER trg_historial_auditoria_sin_truncate
    BEFORE TRUNCATE ON sigra.historial_auditoria
    FOR EACH STATEMENT EXECUTE FUNCTION sigra.historial_auditoria_solo_insercion();

COMMIT;