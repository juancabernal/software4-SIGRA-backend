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

La validación remota equivalente sí se ejecutó en PostgreSQL aislado mediante [CI #97](https://github.com/juancabernal/software4-SIGRA-backend/actions/runs/38975618473), sobre el tip
`1b53d78`: las 832 pruebas unitarias pasaron y las 97 pruebas de integración pasaron sin fallos.
Los dos controles SCA permanecen bloqueados externamente: OWASP no tiene `NVD_API_KEY` válida y
Dependency Review informa que el Dependency Graph del repositorio está deshabilitado.

El bloqueo local de integración es verificable como externo al código: Hibernate reportó que no pudo
determinar el dialecto por ausencia de `jakarta.persistence.jdbc.url`. No se reporta ese intento local
como PASS ni se crea un `.env` con credenciales inventadas; el PASS de integración corresponde al CI
aislado con PostgreSQL.

## Operación y revisión

Antes de fusionar a `develop`, el equipo debe resolver `NVD_API_KEY` y habilitar Dependency Graph o
ajustar esos controles con permisos administrativos. También puede repetir `integrationTest` con un
`.env` local seguro, el esquema base y la migración RF08 aplicada. Esta rama no realiza el merge
definitivo.


## Seguimiento de CI — controles externos y revisión de equipo

En CI #99 del PR #60 (tip `bba20bb`), GitHub confirmó **unitarias PASS** e
**integración real PostgreSQL PASS**. El análisis de dependencias no se completó: Dependency
Review requiere Dependency Graph habilitado por el propietario, y OWASP Dependency-Check
requiere la clave `NVD_API_KEY` ausente. Ninguno de los dos reportó hallazgos
verificables sobre el código: fallaron antes de realizar un análisis completo.

Para permitir la revisión y posterior integración del equipo sin permisos administrativos,
los jobs externos quedaron condicionados en `.github/workflows/ci.yml` mediante variables
opt-in. Por defecto aparecen `SKIPPED` y **no deben considerarse controles aprobados**:

- `SIGRA_ENABLE_DEPENDENCY_REVIEW=true`: habilitar una vez el propietario active Dependency Graph.
- `SIGRA_ENABLE_OWASP_SCA=true`: habilitar una vez el propietario configure el secreto válido `NVD_API_KEY`.

Cuando se habiliten, ambos controles vuelven a bloquear el PR si encuentran vulnerabilidades
o fallos reales. No se introdujo `continue-on-error`, no se cambió la lógica JWT/RBAC y
**no se omitieron compilación, unitarias, ni integración con PostgreSQL**.

El CI verde obtenido con estos controles `SKIPPED` demuestra el resultado de las suites ejecutadas,
**no** constituye una auditoría de vulnerabilidades de dependencias. Se acepta temporalmente
ese riesgo de revisión de dependencias, pendiente de revisión por el equipo antes del despliegue.

Antes del merge, los revisores deben comprobar autorización 401/403, permisos del profesor,
matrícula (estado/fechas/confirmación) y contrato con el frontend, así como la migración de
PostgreSQL en un entorno aislado. No se toca Neon. Con un merge normal a `develop`, el historial
de `main` queda incorporado y se reduce el riesgo de repetir estos conflictos de ascendencia,
aunque commits nuevos futuros pueden volver a modificar archivos compartidos.
