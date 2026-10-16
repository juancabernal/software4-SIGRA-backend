# Integración definitiva de `main` y `develop` — 2026-10-16

## Estado y alcance

Esta rama prepara un merge real de `origin/main` sobre la línea de trabajo de `develop`. Se
conservan ambos historiales y no se modifica ni se fusiona `main` o `develop`. El PR de revisión es
[backend #60](https://github.com/juancabernal/software4-SIGRA-backend/pull/60), cuyo destino es
`develop`.

La decisión arquitectónica es única: Spring Security es la fuente de verdad para autenticación,
autorización y alcance. Se conservan `JwtAuthenticationFilter`, `UsuarioPrincipal`, `CurrentUser`,
`SecurityAuthorizationService` y `SecurityConfig`.

## Seguridad definitiva

El flujo de una petición privada es:

1. `JwtAuthenticationFilter` valida el Bearer JWT, carga el usuario activo y publica la
   autenticación en `SecurityContext`.
2. `SecurityConfig` mantiene una API stateless, CORS, límite de tamaño, correlación de petición,
   respuestas 401/403 y `anyRequest().authenticated()` como barrera mínima.
3. `@PreAuthorize` aplica el rol requerido en cada endpoint.
4. `CurrentUser.required()` obtiene el `UsuarioPrincipal` validado; nunca se acepta identidad desde
   headers arbitrarios, parámetros, cuerpos o atributos manipulables.
5. Los servicios verifican propiedad o asignación docente activa para recursos individuales.

Se retiraron los mecanismos temporales de `develop`: `IdentidadActual`, `ResolvedorIdentidad`,
`RolesPermitidos`, `ControlAccesoAsignaturasInterceptor`, `ControlAccesoMatriculaInterceptor`,
`RolesPermitidosMatricula`, `ConfiguracionWebMatricula` y `WebConfig` paralelo. La ausencia de
identidad ahora falla cerradamente; no existe un interruptor que desactive RBAC en producción.

## Matriz de conservación

| Área | Trabajo conservado de `main` | Trabajo conservado de `develop` | Adaptación final |
| --- | --- | --- | --- |
| Auth/RBAC | JWT, roles, filtros, 401/403 y alcance | Endpoints nuevos | `@PreAuthorize` + `CurrentUser` + verificaciones de servicio |
| Evaluaciones | Asignación docente y control IDOR | RF-10/RF-11, porcentajes, fechas, calificaciones y cascadas | `UsuarioPrincipal` en todos los métodos que operan recursos |
| Estadísticas | Alcance por asignatura | RF-16, filtros, umbrales y conclusiones | ADMIN ve todo; PROFESOR solo asignaturas autorizadas |
| Acciones de mejora | Autorización por recurso | RF-17/RF-18 y conversión transaccional | El principal se propaga hasta `EvaluacionService` |
| Matrículas | Rol, propiedad docente, historial | fecha, reloj, estados, confirmación, reactivación y alta atómica | `EstadoMatricula` para matrícula; `EstadoRegistro` para entidades que corresponda |
| Base de datos | `ddl-auto=validate` y esquema PostgreSQL | migración de fecha/estado | `RF08_matricula_fecha_y_estado_migration.sql`, sin tocar producción |

## Matrículas y datos

La matrícula usa `EstadoMatricula.ACTIVA` y `DESVINCULADA`; los estados de usuario, estudiante,
asignatura y asignación docente siguen usando `EstadoRegistro` donde corresponde. La terna sigue
siendo única, la rematrícula reactiva el registro existente y `fecha_matricula` la fija el reloj del
backend. La desvinculación requiere confirmación cuando el puerto de calificaciones informa notas y
conserva el historial: no borra calificaciones ni filas.

La migración `docs/db/RF08_matricula_fecha_y_estado_migration.sql` es transaccional e idempotente:
rellena fechas existentes, traduce `ACTIVO/INACTIVO` a `ACTIVA/DESVINCULADA`, actualiza el `CHECK`
y deja consultas de verificación y reversión documentada. Debe ejecutarse antes de arrancar esta
versión contra una copia respaldada; no se ejecutó sobre Neon ni otra base de producción.

## Validación ejecutada

| Verificación | Resultado observado |
| --- | --- |
| `compileJava` | PASS; solo permanece el warning de MapStruct ya existente sobre `tieneAsignacionesActivas` |
| `compileTestJava` | PASS |
| Suite unitaria `test` | PASS: 832 tests, 0 fallos |
| Evaluaciones, estadísticas, mejoras y guardia arquitectónica | PASS en ejecuciones focalizadas |
| `integrationTest` | BLOQUEADA por entorno: no existe `SIGRA/.env` ni URL JDBC disponible; 97 tests no pudieron crear `entityManagerFactory` |
| PostgreSQL/migración | No se ejecutó contra una base real en esta sesión; requiere credenciales y PostgreSQL local/CI |

El fallo de integración es verificable como externo al código: Hibernate reportó que no pudo
determinar el dialecto por ausencia de `jakarta.persistence.jdbc.url`. No se reporta como PASS ni se
crea un `.env` con credenciales inventadas.

## Operación y revisión

Antes de fusionar a `develop`, el equipo debe ejecutar `integrationTest` con PostgreSQL aislado,
`.env` local seguro, el esquema base y la migración RF08 aplicada; después debe revisar los checks
del SHA final del PR. Los controles de OWASP/Dependency Review que dependan de secretos NVD,
Dependency Graph o permisos administrativos deben permanecer visibles como bloqueos de CI si GitHub
no los puede ejecutar. Esta rama no realiza el merge definitivo.
