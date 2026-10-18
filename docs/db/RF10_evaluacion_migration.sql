-- =============================================================================
-- SIGRA — Migración: tabla evaluacion (RF-10 / RF-11, gestión de evaluaciones por RA)
-- Archivo: docs/db/RF10_evaluacion_migration.sql
-- Motor: PostgreSQL 15+
--
-- Para quién: toda base que ya tenga el schema "sigra" y todavía no tenga sigra.evaluacion.
-- Una base creada desde cero con SIGRA_SCHEMA_CURRENT.sql actualizado NO la necesita.
-- Con ddl-auto=validate, sin esta tabla el backend NO arranca una vez exista la entidad Evaluacion.
--
-- Qué crea:
--   - sigra.evaluacion: una evaluación mide un RA en un semestre (nombre, fecha tentativa,
--     porcentaje, descripción y estado ACTIVA/INACTIVA, con eliminación lógica).
--   - porcentaje NUMERIC(5,2), mayor que 0 y hasta 100. La regla de que la suma de las
--     evaluaciones ACTIVAS de un (RA, semestre) no supere 100 la aplica el servicio.
--   - inactivada_con_ra: marca las evaluaciones que se inactivaron en cascada al inactivar su RA,
--     para que al reactivarlo vuelvan a ACTIVA solo esas (y no las que el profesor inactivó a mano).
--   - Índice único de nombre por (RA, semestre) sin distinguir mayúsculas.
--
-- Es IDEMPOTENTE y no borra datos: se puede ejecutar varias veces.
--
-- Uso (puerto, usuario y base según tu máquina):
--   psql -h localhost -p <PUERTO> -U <USUARIO> -d postgres -v ON_ERROR_STOP=1 -f docs/db/RF10_evaluacion_migration.sql
-- =============================================================================

BEGIN;

CREATE TABLE IF NOT EXISTS sigra.evaluacion (
    id                       UUID          NOT NULL DEFAULT gen_random_uuid(),
    resultado_aprendizaje_id UUID          NOT NULL,
    semestre_id              UUID          NOT NULL,
    nombre                   VARCHAR(150)  NOT NULL,
    fecha_tentativa          DATE          NOT NULL,
    porcentaje               NUMERIC(5,2)  NOT NULL,
    descripcion              VARCHAR(500)  NOT NULL,
    estado                   VARCHAR(10)   NOT NULL DEFAULT 'ACTIVA',
    inactivada_con_ra        BOOLEAN       NOT NULL DEFAULT FALSE,
    CONSTRAINT evaluacion_pkey PRIMARY KEY (id),
    CONSTRAINT fk_evaluacion_ra FOREIGN KEY (resultado_aprendizaje_id)
        REFERENCES sigra.resultado_aprendizaje (id),
    CONSTRAINT fk_evaluacion_semestre FOREIGN KEY (semestre_id) REFERENCES sigra.semestre (id),
    CONSTRAINT chk_evaluacion_porcentaje CHECK (porcentaje > 0 AND porcentaje <= 100),
    CONSTRAINT chk_evaluacion_estado CHECK (estado IN ('ACTIVA', 'INACTIVA'))
);

-- Nombre único por (RA, semestre) sin distinguir mayúsculas. Es un índice funcional (lower), por eso
-- no se declara en la entidad JPA; con ddl-auto=validate Hibernate no revisa índices.
CREATE UNIQUE INDEX IF NOT EXISTS uk_evaluacion_ra_semestre_nombre
    ON sigra.evaluacion (resultado_aprendizaje_id, semestre_id, lower(nombre));
CREATE INDEX IF NOT EXISTS idx_evaluacion_ra_semestre
    ON sigra.evaluacion (resultado_aprendizaje_id, semestre_id);

COMMIT;

-- -----------------------------------------------------------------------------
-- Reversión (solo si hace falta deshacerla; BORRA las evaluaciones registradas):
--   BEGIN;
--   DROP TABLE IF EXISTS sigra.evaluacion;
--   COMMIT;
-- -----------------------------------------------------------------------------
