-- =============================================================================
-- RF-12 / RF-13 — Calificaciones y su historial (trazabilidad)
-- Archivo: docs/db/RF12_calificacion_migration.sql
--
-- Para una base que ya tiene el esquema actual. Es idempotente: se puede correr más de una vez.
-- Crea también la tabla mínima de evaluaciones que necesitan las calificaciones; si el módulo de
-- evaluaciones (RF-10 / RF-11) ya la creó, el CREATE TABLE IF NOT EXISTS no hace nada.
--
--   psql -h localhost -p <PUERTO> -U <USUARIO> -d postgres -v ON_ERROR_STOP=1 -f docs/db/RF12_calificacion_migration.sql
-- =============================================================================

BEGIN;

-- Evaluación de un RA en un semestre (mínimo que usa RF-12).
CREATE TABLE IF NOT EXISTS sigra.evaluacion (
                                                id                       UUID          NOT NULL DEFAULT gen_random_uuid(),
    resultado_aprendizaje_id UUID          NOT NULL,
    semestre_id              UUID          NOT NULL,
    nombre                   VARCHAR(150)  NOT NULL,
    fecha_tentativa          DATE          NOT NULL,
    porcentaje               NUMERIC(5,2)  NOT NULL,
    descripcion              VARCHAR(500)  NOT NULL,
    estado                   VARCHAR(10)   NOT NULL DEFAULT 'ACTIVA',
    CONSTRAINT evaluacion_pkey PRIMARY KEY (id),
    CONSTRAINT fk_evaluacion_ra FOREIGN KEY (resultado_aprendizaje_id)
    REFERENCES sigra.resultado_aprendizaje (id),
    CONSTRAINT fk_evaluacion_semestre FOREIGN KEY (semestre_id) REFERENCES sigra.semestre (id),
    CONSTRAINT chk_evaluacion_porcentaje CHECK (porcentaje > 0 AND porcentaje <= 100),
    CONSTRAINT chk_evaluacion_estado CHECK (estado IN ('ACTIVA', 'INACTIVA'))
    );

CREATE INDEX IF NOT EXISTS idx_evaluacion_ra_semestre
    ON sigra.evaluacion (resultado_aprendizaje_id, semestre_id);

-- Calificación: nota de un estudiante (vía su matrícula) en una evaluación.
-- Una sola por (evaluación, matrícula), que equivale a (evaluación, estudiante).
CREATE TABLE IF NOT EXISTS sigra.calificacion (
                                                  id                 UUID         NOT NULL DEFAULT gen_random_uuid(),
    evaluacion_id      UUID         NOT NULL,
    matricula_id       UUID         NOT NULL,
    nota               NUMERIC(2,1) NOT NULL,
    observacion        VARCHAR(500) NOT NULL,
    fecha_registro     TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    fecha_modificacion TIMESTAMP WITHOUT TIME ZONE,
    fuera_de_fecha     BOOLEAN      NOT NULL DEFAULT FALSE,
    CONSTRAINT calificacion_pkey PRIMARY KEY (id),
    CONSTRAINT uk_calificacion_evaluacion_matricula UNIQUE (evaluacion_id, matricula_id),
    CONSTRAINT fk_calificacion_evaluacion FOREIGN KEY (evaluacion_id) REFERENCES sigra.evaluacion (id),
    CONSTRAINT fk_calificacion_matricula FOREIGN KEY (matricula_id) REFERENCES sigra.matricula (id),
    CONSTRAINT chk_calificacion_nota CHECK (nota >= 0.0 AND nota <= 5.0),
    CONSTRAINT chk_calificacion_observacion CHECK (char_length(btrim(observacion)) >= 10)
    );

CREATE INDEX IF NOT EXISTS idx_calificacion_matricula ON sigra.calificacion (matricula_id);

-- Historial de la calificación: solo inserción. Una fila por registro y por modificación.
CREATE TABLE IF NOT EXISTS sigra.historial_calificacion (
                                                            id                   UUID         NOT NULL DEFAULT gen_random_uuid(),
    calificacion_id      UUID         NOT NULL,
    tipo_evento          VARCHAR(20)  NOT NULL,
    nota_anterior        NUMERIC(2,1),
    nota_nueva           NUMERIC(2,1) NOT NULL,
    observacion_anterior VARCHAR(500),
    observacion_nueva    VARCHAR(500) NOT NULL,
    fuera_de_fecha       BOOLEAN      NOT NULL,
    usuario_id           UUID         NOT NULL,
    rol_usuario          VARCHAR(20)  NOT NULL,
    fecha_evento         TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    CONSTRAINT historial_calificacion_pkey PRIMARY KEY (id),
    CONSTRAINT fk_historial_calificacion FOREIGN KEY (calificacion_id) REFERENCES sigra.calificacion (id),
    CONSTRAINT chk_historial_calificacion_tipo CHECK (tipo_evento IN ('REGISTRO', 'MODIFICACION')),
    CONSTRAINT chk_historial_calificacion_rol CHECK (rol_usuario IN ('ADMINISTRADOR', 'PROFESOR', 'ESTUDIANTE'))
    );

CREATE INDEX IF NOT EXISTS idx_historial_calificacion ON sigra.historial_calificacion (calificacion_id, fecha_evento);

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
    inactivado_con_asignatura BOOLEAN NOT NULL DEFAULT FALSE,
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
-- Administrador (RF-04): subtipo de usuario, sin atributos propios
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS sigra.administrador (
                                                   id UUID NOT NULL,
                                                   CONSTRAINT administrador_pkey PRIMARY KEY (id),
    CONSTRAINT fk_administrador_usuario FOREIGN KEY (id) REFERENCES sigra.usuario (id)
    );

-- -----------------------------------------------------------------------------
-- Estudiante (RF-09a): subtipo de usuario. Tipo de documento, nombre, correo y estado
-- viven en sigra.usuario; aquí solo lo propio del estudiante.
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS sigra.estudiante (
                                                id               UUID        NOT NULL,
                                                numero_documento VARCHAR(10) NOT NULL,
    CONSTRAINT estudiante_pkey PRIMARY KEY (id),
    CONSTRAINT fk_estudiante_usuario FOREIGN KEY (id) REFERENCES sigra.usuario (id)
    );

-- -----------------------------------------------------------------------------
-- Matrícula (RF-08, RF-09): vínculo estudiante - asignatura - semestre.
-- La terna es única sin importar el estado: desvincular pasa el registro a INACTIVO.
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS sigra.matricula (
                                               id            UUID         NOT NULL DEFAULT gen_random_uuid(),
    estudiante_id UUID         NOT NULL,
    asignatura_id UUID         NOT NULL,
    semestre_id   UUID         NOT NULL,
    estado        VARCHAR(255) NOT NULL DEFAULT 'ACTIVO',
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