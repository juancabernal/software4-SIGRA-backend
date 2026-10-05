# SIGRA — Base de datos: línea base actual (local)

Este documento describe la base de datos que usa el backend **hoy**. Es la referencia para
configurar un entorno local y para revisar cambios de esquema.

```
================================================
SIGRA DATABASE — CURRENT LOCAL BASELINE
================================================

Motor:                 PostgreSQL 18 (validado con 18.0; el proyecto pide 15+)
Instancia local validada: localhost:5433
Database:              postgres
Schema:                sigra
JDBC local del desarrollador:
                       jdbc:postgresql://localhost:5433/postgres?currentSchema=sigra

Hibernate:             ddl-auto=validate
Estrategia:            el esquema lo controla SQL/migraciones.
                       Hibernate solamente lo valida.
```

## Puntos clave

- **`sigra` es un SCHEMA, no una base de datos.** Todas las tablas del backend viven en
  `postgres.sigra`. La URL JDBC incluye `?currentSchema=sigra` para que Hibernate y las
  consultas nativas resuelvan los nombres dentro de ese schema.
- **El puerto no está fijado en el código.** Cada desarrollador configura el suyo en
  `SIGRA/.env` (`DB_URL`). En la máquina de referencia de este documento el servidor escucha en
  `5433`; el `.env.example` compartido muestra `5432`, que es el valor por defecto de PostgreSQL.
- **No se versionan credenciales.** `DB_USERNAME`, `DB_PASSWORD` y `JWT_SECRET` van solo en
  `SIGRA/.env` (ignorado por Git) o en variables del sistema.
- **`public` no se usa.** La base `postgres` también contiene tablas de otros proyectos en
  `public` y un schema de copia de seguridad (`sigra_backup_*`). Ninguno pertenece a SIGRA ni
  debe modificarse desde los scripts de este repositorio.

## Estrategia de esquema

| Elemento | Decisión |
| --- | --- |
| Creación del esquema | `docs/db/SIGRA_SCHEMA_CURRENT.sql` (base vacía) |
| Cambios sobre una base existente | Scripts SQL revisados y ejecutados a mano (`RF04_usuario_migration.sql`, ya aplicada; `RF09_estudiante_y_asignacion_docente_migration.sql`; y `RF04_administrador_y_estudiante_subtipos_migration.sql`) |
| Validación en el arranque | Hibernate `ddl-auto=validate`: si una entidad no coincide con la tabla, el backend no arranca |
| Hibernate modifica tablas | **No.** Ni `update`, ni `create`, ni `create-drop` |

Por eso el backend **no arranca** si la base no tiene el esquema esperado: es el comportamiento
deseado. Un cambio de entidad sin su script SQL se detecta al arrancar, no en producción.

## Modelo de usuarios (herencia JOINED)

`Usuario` es una clase base **abstracta** con `@Inheritance(strategy = JOINED)`. Cada subtipo
tiene su propia tabla, enlazada por la misma clave primaria.

### Tabla `sigra.usuario` (atributos comunes)

| Columna | Tipo | Nulo | Notas |
| --- | --- | --- | --- |
| `id` | UUID | no | PK |
| `tipo_documento_id` | UUID | no | FK a `sigra.tipo_documento(id)` (catálogo reutilizable) |
| `nombre_completo` | VARCHAR(255) | no | |
| `correo_institucional` | VARCHAR(255) | no | UNIQUE. Ver «Correo institucional» |
| `password_hash` | VARCHAR(255) | sí | BCrypt factor 12. Puede ser nulo (ver deuda `PASSWORD_PROVISIONING_FLOW`) |
| `estado` | VARCHAR(255) | no | `ACTIVO` / `INACTIVO` (CHECK) |
| `intentos_fallidos` | INTEGER | no | Empieza en 0, no negativo (CHECK) |
| `fecha_bloqueo` | TIMESTAMP | sí | Nulo si no hay bloqueo activo |

### Tabla `sigra.profesor` (subtipo)

| Columna | Tipo | Nulo | Notas |
| --- | --- | --- | --- |
| `id` | UUID | no | PK **y** FK a `sigra.usuario(id)` |
| `numero_documento` | VARCHAR(10) | no | Parte de la identidad documental |

`Profesor extends Usuario`. Los atributos comunes **no** se repiten en `profesor`.

### Tabla `sigra.administrador` (subtipo, RF-04)

| Columna | Tipo | Nulo | Notas |
| --- | --- | --- | --- |
| `id` | UUID | no | PK **y** FK a `sigra.usuario(id)` |

`Administrador extends Usuario` y no tiene atributos propios: todo lo suyo vive en `usuario`.

### Tabla `sigra.estudiante` (subtipo, RF-09a)

| Columna | Tipo | Nulo | Notas |
| --- | --- | --- | --- |
| `id` | UUID | no | PK **y** FK a `sigra.usuario(id)` |
| `numero_documento` | VARCHAR(10) | no | Parte de la identidad documental |

`Estudiante extends Usuario`. Tipo de documento, nombre, correo y estado viven en `usuario`.
En bases que tenían `estudiante` con el diseño anterior (columnas propias),
`RF04_administrador_y_estudiante_subtipos_migration.sql` mueve esos datos a `usuario` con el
mismo `id`. **Los estudiantes migrados quedan sin contraseña** (`password_hash` nulo) y no pueden
iniciar sesión hasta que se les asigne una.

## Tabla `sigra.asignacion_docente` (RF-07)

| Columna | Tipo | Nulo | Notas |
| --- | --- | --- | --- |
| `id` | UUID | no | PK |
| `profesor_id` | UUID | no | FK a `sigra.profesor(id)` |
| `asignatura_id` | UUID | no | FK a `sigra.asignatura(id)` |
| `estado` | VARCHAR(255) | no | `ACTIVO` / `INACTIVO` (CHECK) |

En bases anteriores a este cambio, `asignatura_id` se agrega con
`RF09_estudiante_y_asignacion_docente_migration.sql`; si había asignaciones antiguas sin
asignatura, la columna queda sin `NOT NULL` hasta que se corrijan esas filas.

## Tabla `sigra.matricula` (RF-08, RF-09)

Vínculo entre un estudiante, una asignatura y un semestre.

| Columna | Tipo | Nulo | Notas |
| --- | --- | --- | --- |
| `id` | UUID | no | PK |
| `estudiante_id` | UUID | no | FK a `sigra.estudiante(id)` |
| `asignatura_id` | UUID | no | FK a `sigra.asignatura(id)` |
| `semestre_id` | UUID | no | FK a `sigra.semestre(id)` |
| `estado` | VARCHAR(255) | no | `ACTIVO` / `INACTIVO` (CHECK), por defecto `ACTIVO` |

La terna `(estudiante_id, asignatura_id, semestre_id)` es **única sin importar el estado**
(`uk_matricula_terna`). Desvincular no borra el registro: lo deja en `INACTIVO`, y volver a
matricular la misma terna reactiva ese registro en lugar de crear otro.

## Correo institucional

- `correo_institucional` es **global a `Usuario`**: la unicidad se garantiza sobre la tabla base,
  así que un correo no puede repetirse entre **todos los roles** (administradores, profesores y
  estudiantes).
- Se **normaliza** (sin espacios laterales, en minúsculas con `Locale.ROOT`) antes de persistir,
  en la entidad (`@PrePersist`/`@PreUpdate`) y en la autenticación.
- Como defensa adicional, la base tiene un índice único sobre `lower(trim(correo_institucional))`.

## Identidad documental

`(tipoDocumento, numeroDocumento)` debe ser única. Como `tipo_documento_id` vive en `usuario` y
`numero_documento` en cada subtipo (`profesor`, `estudiante`), **el UNIQUE compuesto no se puede
declarar en una sola tabla**.
Se verifica en el servicio (`ProfesorServiceImpl`) y en la migración, pero no es una restricción
de base de datos.

## Estado de la base de referencia

Resultado del preflight (solo lectura) ejecutado sobre la base local en la fecha de este cambio:

| Verificación | Resultado |
| --- | --- |
| Schema `sigra` existe | sí |
| Tablas `usuario`, `profesor`, `tipo_documento`, `programa_academico`, `asignatura`, `semestre`, `resultado_aprendizaje`, `asignacion_docente` | presentes |
| `profesor_asignatura` (tabla antigua de asignaciones) | ausente: no hay desalineación |
| Profesores sin usuario | 0 |
| Correos duplicados (ignorando mayúsculas y espacios) | 0 |
| Documentos duplicados (tipo, número) | 0 |
| Columnas antiguas en `profesor` | no (migración aplicada) |

## Scripts relacionados

| Archivo | Tipo | Para qué |
| --- | --- | --- |
| `docs/db/SIGRA_SCHEMA_CURRENT.sql` | Baseline | Crea el schema actual en una base vacía |
| `docs/db/RF04_schema_preflight.sql` | Solo lectura | Describe el estado antes de cualquier cambio |
| `docs/db/RF04_usuario_migration.sql` | Legacy | Pasa `profesor` del modelo antiguo a `usuario` + `profesor` (ya aplicada) |
| `docs/db/RF09_estudiante_y_asignacion_docente_migration.sql` | Migración idempotente | Crea `sigra.estudiante` (diseño anterior, independiente) y agrega `asignacion_docente.asignatura_id` en bases creadas antes de RF-07/RF-09 |
| `docs/db/RF04_administrador_y_estudiante_subtipos_migration.sql` | Migración idempotente | Crea `sigra.administrador` y convierte `sigra.estudiante` en subtipo de `usuario` (mueve los datos, no los borra) |
| `docs/db/RF08_matricula_migration.sql` | Migración idempotente | Crea `sigra.matricula` en bases creadas antes de RF-08/RF-09 |
| `bruno/SIGRA/seed/seed-roles.sql` | Datos de prueba | Un administrador y un estudiante de Bruno para RF-04, idempotente |
| `bruno/SIGRA/seed/seed-auth.sql` | Datos de prueba | Usuarios de Bruno para RF-04, idempotente |

## Deudas conocidas (no bloquean RF-04 backend)

| Código | Estado | Descripción |
| --- | --- | --- |
| `FAILED_ATTEMPTS_CONCURRENCY` | DEFERRED | Dos intentos fallidos simultáneos pueden perder un incremento del contador |
| `PASSWORD_PROVISIONING_FLOW` | PENDING PRODUCT/TEAM DECISION | `POST /api/v1/profesores` no recibe contraseña; un profesor creado así no puede iniciar sesión. Lo mismo aplica a los estudiantes migrados desde el diseño anterior |
| `AUTH_RUNTIME_ROLES` | RESUELTA | `Administrador`, `Profesor` y `Estudiante` extienden `Usuario` y pueden autenticarse si tienen contraseña |
