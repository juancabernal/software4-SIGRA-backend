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
| Cambios sobre una base existente | Scripts SQL revisados y ejecutados a mano (por ahora, `RF04_usuario_migration.sql`, ya aplicada) |
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

### Subtipos futuros

`Estudiante` y `Administrador` podrán heredar de `Usuario` de la misma forma: una tabla propia
con `id` PK/FK a `usuario.id` y solo sus atributos específicos. **Hoy no existen en la base ni
en el código**; ver «Deudas».

## Correo institucional

- `correo_institucional` es **global a `Usuario`**: la unicidad se garantiza sobre la tabla base,
  así que un correo no puede repetirse entre profesores, estudiantes o administradores.
- Se **normaliza** (sin espacios laterales, en minúsculas con `Locale.ROOT`) antes de persistir,
  en la entidad (`@PrePersist`/`@PreUpdate`) y en la autenticación.
- Como defensa adicional, la base tiene un índice único sobre `lower(trim(correo_institucional))`.

## Identidad documental

`(tipoDocumento, numeroDocumento)` debe ser única. Como `tipo_documento_id` vive en `usuario` y
`numero_documento` en `profesor`, **el UNIQUE compuesto no se puede declarar en una sola tabla**.
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
| `bruno/SIGRA/seed/seed-auth.sql` | Datos de prueba | Usuarios de Bruno para RF-04, idempotente |

## Deudas conocidas (no bloquean RF-04 backend)

| Código | Estado | Descripción |
| --- | --- | --- |
| `FAILED_ATTEMPTS_CONCURRENCY` | DEFERRED | Dos intentos fallidos simultáneos pueden perder un incremento del contador |
| `PASSWORD_PROVISIONING_FLOW` | PENDING PRODUCT/TEAM DECISION | `POST /api/v1/profesores` no recibe contraseña; un profesor creado así no puede iniciar sesión |
| `AUTH_RUNTIME_ROLES` | solo subtipos implementados | Solo `Profesor` extiende `Usuario`; `Estudiante` y `Administrador` no pueden autenticarse aún |
