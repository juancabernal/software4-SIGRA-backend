# Plan y cobertura de pruebas RF-02/RF-03

## Capas

1. **Unitarias:** JWT, filtro, principal, autorización por recurso, bloqueo, servicios de estudiante/matrícula/asignatura y reglas de error.
2. **Web Spring Security:** `MockMvc` con `SecurityFilterChain` real para 401, 403, CORS/preflight, login público y errores JSON.
3. **Integración PostgreSQL:** `@SpringBootTest`, `MockMvc` real y JWT emitido por `JwtService`; el esquema versionado se aplica con `ddl-auto=validate`.
4. **Colecciones Bruno:** inventario estático de 246 requests. La ejecución CLI queda pendiente hasta
   disponer de PostgreSQL desechable y una instancia levantada desde el JAR candidato; no se cuentan
   resultados históricos como evidencia de esta entrega.

## Casos de seguridad verificados

- Anónimo, Bearer vacío/manipulado, JWT expirado y firmado con otra clave.
- Usuario inactivo, rol/correo persistido diferentes a los claims y roles no autorizados.
- ADMINISTRADOR global; PROFESOR en su asignación activa y rechazo cruzado; ESTUDIANTE propio y rechazo ajeno.
- Manipulación de UUID en detalle, asignaturas del estudiante, listado de matrículas y alta de matrícula.
- Asignaciones/matrículas inactivas y fallo cerrado cuando no existe principal.
- Quinto fallo 401, bloqueo posterior 423 y 12 intentos simultáneos en PostgreSQL.
- CORS permitido/rechazado y preflight sin abrir operaciones de negocio.
- Límite de cuerpo real de 16 KiB solo para login, incluso con longitud desconocida/chunked; payload JSON con tipos inesperados y `X-Request-Id` UUID.
- Correo de login de 254 caracteres, contraseña de 72 bytes UTF-8 y contraseña multibyte sin truncamiento.
- Falla de repositorio durante autenticación: 503 JSON genérico, sin continuación al controlador ni fuga técnica.
- Arquitectura: `RequestMappingHandlerMapping` real, login público explícito y todo otro mapping `/api/` con `@PreAuthorize`, sin conteo fijo.
- Logging: fallos de login y respuestas 401/403 sin correo, contraseña, JWT, Authorization ni cuerpo.

## Resultado de esta pasada

`clean test` reporta 618 tests, 0 fallos, 0 errores y 0 omitidos; las tres pruebas de H-02 quedaron
activas. `bootJar` compila y su JAR se identificó por SHA-256 en `VALIDATION.md`. La integración
PostgreSQL y Bruno quedan `BLOCKED` por falta de un destino aislado verificable. CI/SCA remoto tampoco
se ejecutó porque todavía no existe un commit candidato publicado.
