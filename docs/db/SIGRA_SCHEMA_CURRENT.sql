-- =============================================================================
-- SIGRA — Esquema ACTUAL (baseline) que funciona con el backend actual
-- Archivo: docs/db/SIGRA_SCHEMA_CURRENT.sql
-- Motor: PostgreSQL 15+
--
-- Qué es:
--   El DDL del schema "sigra" tal como existe en la base que usa el backend hoy.
--   Coincide con las entidades JPA actuales. Hibernate NO lo modifica: con
--   ddl-auto=validate solo comprueba que las entidades y las tablas coincidan.
--
-- Qué NO es:
--   - No es una migración. Para bases existentes con el esquema antiguo de
--     'profesor' usa docs/db/RF04_usuario_migration.sql (legacy, ya aplicada en la
--     base local de referencia).
--   - No contiene tablas futuras (administrador, etc.).
--   - "sigra" es un SCHEMA dentro de la base "postgres", no una base de datos.
--
-- Uso (base vacía, PostgreSQL local, puerto según tu máquina):
--   psql -h localhost -p <PUERTO> -U <USUARIO> -d postgres -v ON_ERROR_STOP=1 -f docs/db/SIGRA_SCHEMA_CURRENT.sql
--
-- Todas las referencias son explícitas (sigra.tabla). No depende del search_path.
-- Se ejecuta en una sola transacción: si algo falla, no queda nada creado a medias.
-- =============================================================================

BEGIN;

CREATE SCHEMA IF NOT EXISTS sigra;

-- -----------------------------------------------------------------------------
-- Catálogo de tipos de documento (lo reutilizan usuario y sus subtipos)
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS sigra.tipo_documento (
    id     UUID NOT NULL DEFAULT gen_random_uuid(),
    nombre VARCHAR(50) NOT NULL,
    CONSTRAINT tipo_documento_pkey PRIMARY KEY (id),
    CONSTRAINT uk_tipo_documento_nombre UNIQUE (nombre)
);

-- -----------------------------------------------------------------------------
-- Usuario (clase base abstracta en JPA, @Inheritance JOINED)
-- Guarda los atributos comunes de cualquier usuario del sistema.
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS sigra.usuario (
    id                   UUID         NOT NULL DEFAULT gen_random_uuid(),
    tipo_documento_id    UUID         NOT NULL,
    nombre_completo      VARCHAR(255) NOT NULL,
    correo_institucional VARCHAR(255) NOT NULL,
    password_hash        VARCHAR(255),
    estado               VARCHAR(255) NOT NULL DEFAULT 'ACTIVO',
    intentos_fallidos    INTEGER      NOT NULL DEFAULT 0,
    fecha_bloqueo        TIMESTAMP WITHOUT TIME ZONE,
    CONSTRAINT usuario_pkey PRIMARY KEY (id),
    CONSTRAINT uk_usuario_correo UNIQUE (correo_institucional),
    CONSTRAINT fk_usuario_tipo_documento FOREIGN KEY (tipo_documento_id)
        REFERENCES sigra.tipo_documento (id),
    CONSTRAINT chk_usuario_estado CHECK (estado IN ('ACTIVO', 'INACTIVO')),
    CONSTRAINT chk_usuario_intentos CHECK (intentos_fallidos >= 0)
);

CREATE INDEX IF NOT EXISTS idx_usuario_tipo_documento ON sigra.usuario (tipo_documento_id);

-- Unicidad del correo sin distinguir mayúsculas ni espacios laterales (la entidad ya normaliza
-- a minúsculas y sin espacios; este índice blinda la base aunque alguien inserte a mano).
CREATE UNIQUE INDEX IF NOT EXISTS uk_usuario_correo_lower
    ON sigra.usuario (lower(trim(both FROM correo_institucional)));

-- -----------------------------------------------------------------------------
-- Profesor (subtipo de Usuario: JOINED). Su PK es también FK a usuario.id.
-- Los atributos comunes NO se repiten aquí.
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS sigra.profesor (
    id                UUID        NOT NULL,
    numero_documento  VARCHAR(10) NOT NULL,
    CONSTRAINT profesor_pkey PRIMARY KEY (id),
    CONSTRAINT fk_profesor_usuario FOREIGN KEY (id) REFERENCES sigra.usuario (id)
);

CREATE INDEX IF NOT EXISTS idx_profesor_numero_documento ON sigra.profesor (numero_documento);

-- -----------------------------------------------------------------------------
-- Programa académico (RF-03)
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS sigra.programa_academico (
    id     UUID         NOT NULL DEFAULT gen_random_uuid(),
    nombre VARCHAR(100) NOT NULL,
    codigo VARCHAR(10)  NOT NULL,
    estado VARCHAR(255) NOT NULL DEFAULT 'ACTIVO',
    CONSTRAINT programa_academico_pkey PRIMARY KEY (id),
    CONSTRAINT uk_programa_codigo UNIQUE (codigo),
    CONSTRAINT uk_programa_nombre UNIQUE (nombre),
    CONSTRAINT chk_programa_estado CHECK (estado IN ('ACTIVO', 'INACTIVO'))
);

-- -----------------------------------------------------------------------------
-- Asignatura (RF-03)
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS sigra.asignatura (
    id          UUID         NOT NULL DEFAULT gen_random_uuid(),
    codigo      VARCHAR(20)  NOT NULL,
    nombre      VARCHAR(100) NOT NULL,
    programa_id UUID         NOT NULL,
    estado      VARCHAR(20)  NOT NULL DEFAULT 'BORRADOR',
    CONSTRAINT asignatura_pkey PRIMARY KEY (id),
    CONSTRAINT uk_asignatura_codigo UNIQUE (codigo),
    CONSTRAINT fk_asignatura_programa FOREIGN KEY (programa_id)
        REFERENCES sigra.programa_academico (id),
    CONSTRAINT chk_asignatura_estado CHECK (estado IN ('BORRADOR', 'ACTIVA', 'INACTIVA'))
);

CREATE INDEX IF NOT EXISTS idx_asignatura_programa ON sigra.asignatura (programa_id);

-- -----------------------------------------------------------------------------
-- Resultado de aprendizaje (RF-03)
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS sigra.resultado_aprendizaje (
    id            UUID          NOT NULL DEFAULT gen_random_uuid(),
    asignatura_id UUID          NOT NULL,
    codigo        VARCHAR(20)   NOT NULL,
    descripcion   VARCHAR(2000) NOT NULL,
    estado        VARCHAR(255)  NOT NULL DEFAULT 'ACTIVO',
    CONSTRAINT resultado_aprendizaje_pkey PRIMARY KEY (id),
    CONSTRAINT uk_ra_asignatura_codigo UNIQUE (asignatura_id, codigo),
    CONSTRAINT fk_ra_asignatura FOREIGN KEY (asignatura_id) REFERENCES sigra.asignatura (id),
    CONSTRAINT chk_ra_estado CHECK (estado IN ('ACTIVO', 'INACTIVO'))
);

CREATE INDEX IF NOT EXISTS idx_ra_asignatura ON sigra.resultado_aprendizaje (asignatura_id);

-- -----------------------------------------------------------------------------
-- Semestre (RF-02)
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS sigra.semestre (
    id           UUID        NOT NULL DEFAULT gen_random_uuid(),
    codigo       VARCHAR(30) NOT NULL,
    fecha_inicio DATE        NOT NULL,
    fecha_fin    DATE        NOT NULL,
    estado       VARCHAR(30) NOT NULL DEFAULT 'ACTIVO',
    CONSTRAINT semestre_pkey PRIMARY KEY (id),
    CONSTRAINT uk_semestre_codigo UNIQUE (codigo),
    CONSTRAINT chk_semestre_estado CHECK (estado IN ('ACTIVO', 'INACTIVO')),
    CONSTRAINT chk_semestre_fechas CHECK (fecha_fin > fecha_inicio)
);

-- -----------------------------------------------------------------------------
-- Asignación docente (módulo de asignaciones). Se referencia desde profesor.
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS sigra.asignacion_docente (
    id            UUID         NOT NULL DEFAULT gen_random_uuid(),
    profesor_id   UUID         NOT NULL,
    asignatura_id UUID         NOT NULL,
    estado        VARCHAR(255) NOT NULL,
    CONSTRAINT asignacion_docente_pkey PRIMARY KEY (id),
    CONSTRAINT fk_asignacion_docente_profesor FOREIGN KEY (profesor_id)
        REFERENCES sigra.profesor (id),
    CONSTRAINT fk_asignacion_docente_asignatura FOREIGN KEY (asignatura_id)
        REFERENCES sigra.asignatura (id),
    CONSTRAINT chk_asignacion_docente_estado CHECK (estado IN ('ACTIVO', 'INACTIVO'))
);

CREATE INDEX IF NOT EXISTS idx_asignacion_docente_profesor ON sigra.asignacion_docente (profesor_id);
CREATE INDEX IF NOT EXISTS idx_asignacion_docente_asignatura ON sigra.asignacion_docente (asignatura_id);

-- -----------------------------------------------------------------------------
-- Estudiante (RF-09a)
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS sigra.estudiante (
    id                   UUID         NOT NULL DEFAULT gen_random_uuid(),
    tipo_documento_id    UUID         NOT NULL,
    numero_documento     VARCHAR(10)  NOT NULL,
    nombre_completo      VARCHAR(255) NOT NULL,
    correo_institucional VARCHAR(255) NOT NULL,
    estado               VARCHAR(255) NOT NULL DEFAULT 'ACTIVO',
    CONSTRAINT estudiante_pkey PRIMARY KEY (id),
    CONSTRAINT uk_estudiante_correo UNIQUE (correo_institucional),
    CONSTRAINT uk_estudiante_documento UNIQUE (tipo_documento_id, numero_documento),
    CONSTRAINT fk_estudiante_tipo_documento FOREIGN KEY (tipo_documento_id)
        REFERENCES sigra.tipo_documento (id),
    CONSTRAINT chk_estudiante_estado CHECK (estado IN ('ACTIVO', 'INACTIVO'))
);
CREATE INDEX IF NOT EXISTS idx_estudiante_tipo_documento ON sigra.estudiante (tipo_documento_id);

-- -----------------------------------------------------------------------------
-- Matricula (RF-08, RF-09)
-- La terna (estudiante, asignatura, semestre) es única SIEMPRE, sin importar el
-- estado: desvincular no borra el registro, lo pasa a INACTIVO.
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS sigra.matricula (
    id             UUID         NOT NULL DEFAULT gen_random_uuid(),
    estudiante_id  UUID         NOT NULL,
    asignatura_id  UUID         NOT NULL,
    semestre_id    UUID         NOT NULL,
    estado         VARCHAR(255) NOT NULL DEFAULT 'ACTIVO',
    CONSTRAINT matricula_pkey PRIMARY KEY (id),
    CONSTRAINT uk_matricula_terna UNIQUE (estudiante_id, asignatura_id, semestre_id),
    CONSTRAINT fk_matricula_estudiante FOREIGN KEY (estudiante_id) REFERENCES sigra.estudiante (id),
    CONSTRAINT fk_matricula_asignatura FOREIGN KEY (asignatura_id) REFERENCES sigra.asignatura (id),
    CONSTRAINT fk_matricula_semestre FOREIGN KEY (semestre_id) REFERENCES sigra.semestre (id),
    CONSTRAINT chk_matricula_estado CHECK (estado IN ('ACTIVO', 'INACTIVO'))
);

CREATE INDEX IF NOT EXISTS idx_matricula_estudiante ON sigra.matricula (estudiante_id);
CREATE INDEX IF NOT EXISTS idx_matricula_asignatura_semestre ON sigra.matricula (asignatura_id, semestre_id);

COMMIT;
