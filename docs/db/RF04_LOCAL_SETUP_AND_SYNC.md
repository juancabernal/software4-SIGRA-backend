# RF-04 — Configuración local y sincronización de base de datos

Esta guía deja un entorno local listo para ejecutar el backend y probar el login de RF-04 sin tocar otras bases, schemas o proyectos del equipo.

## 1. Cómo quedó SIGRA

- **Database PostgreSQL:** `postgres`
- **Schema de SIGRA:** `sigra`
- **Backend:** `http://localhost:8080`
- **Frontend local esperado:** `http://localhost:4200`
- **Hibernate:** `ddl-auto=validate`
- **El puerto de PostgreSQL NO está fijado:** cada desarrollador usa el suyo (`5432`, `5433`, etc.).

> `sigra` es un **schema**, no una base de datos. No se debe crear una database llamada `sigra` para este baseline.

## 2. Antes de tocar la base

Primero identifica tu instalación de PostgreSQL:

```sql
SELECT current_database() AS database,
       current_user AS usuario,
       current_schema() AS schema_actual,
       current_schemas(true) AS search_path;

SHOW port;
SHOW server_version;
```

Luego revisa si ya existe el schema de SIGRA:

```sql
SELECT schema_name
FROM information_schema.schemata
WHERE schema_name = 'sigra';
```

Y revisa qué tablas existen allí:

```sql
SELECT table_name
FROM information_schema.tables
WHERE table_schema = 'sigra'
ORDER BY table_name;
```

## 3. Elige SOLO uno de estos escenarios

### Escenario A — No tienes schema `sigra` o estás montando SIGRA desde cero

Ejecuta el baseline actual:

```text
docs/db/SIGRA_SCHEMA_CURRENT.sql
```

Con `psql`:

```powershell
& "C:\Program Files\PostgreSQL\18\bin\psql.exe" `
  -U postgres `
  -h localhost `
  -p <TU_PUERTO> `
  -d postgres `
  -v ON_ERROR_STOP=1 `
  -f docs/db/SIGRA_SCHEMA_CURRENT.sql
```

Ese archivo crea el schema `sigra` y la estructura que espera el backend actual.

### Escenario B — Ya tienes `sigra` y contiene la estructura antigua de `profesor`

No ejecutes el baseline encima ni borres tablas.

Primero ejecuta el preflight de solo lectura:

```text
docs/db/RF04_schema_preflight.sql
```

```powershell
& "C:\Program Files\PostgreSQL\18\bin\psql.exe" `
  -U postgres `
  -h localhost `
  -p <TU_PUERTO> `
  -d postgres `
  -v ON_ERROR_STOP=1 `
  -f docs/db/RF04_schema_preflight.sql
```

Si el resultado indica:

```text
PROFESOR_TIENE_COLUMNAS_ANTIGUAS = true
```

haz primero un backup y después ejecuta:

```text
docs/db/RF04_usuario_migration.sql
```

```powershell
& "C:\Program Files\PostgreSQL\18\bin\psql.exe" `
  -U postgres `
  -h localhost `
  -p <TU_PUERTO> `
  -d postgres `
  -v ON_ERROR_STOP=1 `
  -f docs/db/RF04_usuario_migration.sql
```

La migración mueve los datos comunes de `profesor` a `usuario`, conserva el mismo UUID y deja `profesor.id` como PK/FK de `usuario.id`.

### Escenario C — Ya tienes `sigra` actualizado

No corras migraciones. Verifica con:

```text
docs/db/RF04_schema_preflight.sql
```

Debe existir, como mínimo, la estructura actual usada por el backend:

```text
sigra.tipo_documento
sigra.usuario
sigra.profesor
sigra.programa_academico
sigra.asignatura
sigra.resultado_aprendizaje
sigra.semestre
sigra.asignacion_docente
```

`profesor` ya no debe repetir `correo_institucional`, `nombre_completo`, `tipo_documento_id`, `estado`, `intentos_fallidos` ni `fecha_bloqueo`.

Si tu base ya aplicó el esquema antes de RF-07/RF-09, ejecuta `docs/db/RF09_estudiante_y_asignacion_docente_migration.sql` (es idempotente y no borra datos).

Si tu base tiene `estudiante` con el diseño anterior (columnas propias), ejecuta `docs/db/RF04_administrador_y_estudiante_subtipos_migration.sql`; luego, para tener un administrador y un estudiante de prueba, ejecuta `bruno/SIGRA/seed/seed-roles.sql`.

Si tu base no tiene la tabla `matricula`, ejecuta `docs/db/RF08_matricula_migration.sql`.

Si tu base no tiene la columna `resultado_aprendizaje.inactivado_con_asignatura` (necesaria para reactivar asignaturas), ejecuta `docs/db/RF03_ra_inactivado_con_asignatura_migration.sql`.

Orden recomendado de migraciones sobre una base existente:

```text
1. RF09_estudiante_y_asignacion_docente_migration.sql   (si aplica)
2. RF04_administrador_y_estudiante_subtipos_migration.sql
3. RF08_matricula_migration.sql
4. RF03_ra_inactivado_con_asignatura_migration.sql
```

## 4. Si tus tablas antiguas están en `public`

No muevas ni elimines `public` automáticamente.

Los scripts de RF-04 están preparados para trabajar sobre `sigra`. Si tu instalación antigua de SIGRA vive en `public`, primero identifica exactamente qué tablas pertenecen a SIGRA y cuáles pertenecen a otros proyectos:

```sql
SELECT table_schema, table_name
FROM information_schema.tables
WHERE table_schema IN ('public', 'sigra')
ORDER BY table_schema, table_name;
```

Si SIGRA está mezclado con otras tablas en `public`, **no ejecutes una migración destructiva ni cambies el `search_path` a ciegas**. Haz backup y migra únicamente las tablas de SIGRA hacia un schema `sigra` separado antes de aplicar RF-04.

La configuración oficial del backend después de RF-04 siempre apunta a:

```text
Database: postgres
Schema:   sigra
```

## 5. Configura `SIGRA/.env`

Copia:

```text
SIGRA/.env.example
```

como:

```text
SIGRA/.env
```

Ejemplo con PostgreSQL en `5432`:

```env
DB_URL=jdbc:postgresql://localhost:5432/postgres?currentSchema=sigra
DB_USERNAME=postgres
DB_PASSWORD=TU_PASSWORD_LOCAL

SERVER_PORT=8080
JWT_SECRET=UN_SECRETO_LOCAL_DE_MINIMO_32_BYTES
JWT_EXPIRATION_MS=3600000
CORS_ALLOWED_ORIGINS=http://localhost:4200
JPA_DDL_AUTO=validate
JPA_SHOW_SQL=false
```

Si PostgreSQL está en `5433`, cambia únicamente el puerto:

```env
DB_URL=jdbc:postgresql://localhost:5433/postgres?currentSchema=sigra
```

No subas `.env` a Git.

## 6. Por qué usamos `validate` y no `update`

`validate` permite que JPA/Hibernate use normalmente la base para consultar y guardar datos, pero evita que cambie tablas automáticamente al arrancar.

Si una Entity y la base dejan de coincidir, el backend falla al iniciar y muestra la diferencia. El cambio estructural se hace con un SQL revisado y versionado.

No cambies `JPA_DDL_AUTO=validate` por `update` para ocultar un error de esquema.

## 7. Verificación final de la estructura

Después del baseline o de la migración, ejecuta de nuevo:

```text
docs/db/RF04_schema_preflight.sql
```

Después arranca el backend desde `SIGRA/`:

```powershell
.\gradlew.bat clean test
.\gradlew.bat integrationTest
.\gradlew.bat bootRun
```

Comprueba:

```text
http://localhost:8080/actuator/health
```

Debe responder:

```json
{"status":"UP"}
```

Swagger:

```text
http://localhost:8080/swagger-ui.html
```

OpenAPI:

```text
http://localhost:8080/v3/api-docs
```

## 8. Preparar los usuarios de prueba del login

Con la base ya sincronizada, ejecuta:

```text
bruno/SIGRA/seed/seed-auth.sql
```

```powershell
& "C:\Program Files\PostgreSQL\18\bin\psql.exe" `
  -U postgres `
  -h localhost `
  -p <TU_PUERTO> `
  -d postgres `
  -v ON_ERROR_STOP=1 `
  -f bruno/SIGRA/seed/seed-auth.sql
```

El seed es idempotente y trabaja sobre `sigra`.

## 9. Probar RF-04 con Bruno

Abre la colección:

```text
bruno/SIGRA
```

Selecciona el environment:

```text
local
```

Ejecuta primero `auth/01-login` y luego `auth/02-intentos-fallidos` en orden.

Resultados esperados:

| Caso | Resultado |
| --- | --- |
| Login correcto | `200` |
| Request inválido | `400` |
| Correo/contraseña incorrectos | `401` |
| Cinco intentos fallidos | cada uno `401` |
| Siguiente intento durante bloqueo | `423` |

Endpoint:

```text
POST http://localhost:8080/api/v1/auth/login
```

La colección completa y los pasos detallados están en `bruno/README.md`.

## 10. Qué NO hacer

- No crear una database `sigra` para este baseline.
- No borrar el schema `public`.
- No tocar tablas de otros proyectos.
- No ejecutar la migración legacy si el preflight indica que ya estás actualizado.
- No ejecutar `SIGRA_SCHEMA_CURRENT.sql` encima de un entorno legacy para intentar corregirlo.
- No usar `ddl-auto=update` como mecanismo de migración.
- No subir `.env`, contraseñas ni `JWT_SECRET` al repositorio.

## 11. Resumen rápido para un compañero

```text
1. Revisar puerto de PostgreSQL.
2. Conectarse a database postgres.
3. Revisar si existe schema sigra.
4. Si no existe -> SIGRA_SCHEMA_CURRENT.sql.
5. Si existe pero es antiguo -> RF04_schema_preflight.sql -> backup -> RF04_usuario_migration.sql.
6. Si ya está actualizado -> no migrar.
7. Crear SIGRA/.env con su puerto, usuario y contraseña.
8. Mantener currentSchema=sigra y JPA_DDL_AUTO=validate.
9. Arrancar backend y comprobar /actuator/health.
10. Ejecutar seed-auth.sql y probar auth en Bruno.
```

Con estos pasos el entorno local queda alineado con RF-04 sin depender del puerto o las credenciales específicas de otro desarrollador.
