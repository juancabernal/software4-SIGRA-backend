# Auditoría técnica y de seguridad RF-02/RF-03

Fecha de revisión: 2026-10-11. Rama: `jose-valencia/rf-05-security-rbac`. La etiqueta RF-05 de la rama es un nombre interno del incremento; la trazabilidad normativa está aclarada en `TRACEABILITY.md`. Esta pasada no implica merge, push, PR ni certificación CI remota.

## Hallazgos de la auditoría recibida

| ID | Evidencia y cambio aplicado | Estado |
|---|---|---|
| A1 | El inventario Bruno cuenta 246 requests `.bru`, pero no hubo instancia backend/JAR y PostgreSQL aislado disponibles para ejecutar la colección en esta pasada. Los resultados históricos no se vinculan al SHA actual. | BLOCKED |
| A2 | `TRACEABILITY.md` identifica el PDF oficial, las páginas consultadas y la diferencia entre la etiqueta interna RF-05 y RF-02/RF-03 normativos. | CORREGIDO |
| A3 | `git diff --check` no reporta errores; quedan advertencias informativas de conversión LF/CRLF en archivos preexistentes. | VERIFICADO |
| A4 | `JwtAuthenticationFilter` separa parseo/claims, consulta de usuario y continuación de cadena. Una falla de repositorio responde 503 JSON genérico y nunca 401; se registra clase de excepción y `requestId`, sin token. | CORREGIDO Y TESTEADO |
| A5 | `LoginRequestDTO` limita correo a 254 caracteres y contraseña a 72 bytes UTF-8 mediante `Utf8ByteSize`; no trunca contraseñas ni registra payloads. `RequestSizeLimitFilter` aplica 16 KiB reales únicamente a `POST /api/v1/auth/login`, incluso con longitud desconocida/chunked. | CORREGIDO Y TESTEADO |
| A6 | `ErrorResponseBuilder`, `AuthExceptionHandler` y los handlers globales convergen en timestamp con zona, `status`, `error` derivado, `mensaje` y `detalles` opcional. La precedencia de autenticación es explícita. | CORREGIDO Y TESTEADO |
| A7 | `RequestCorrelationFilter` corre antes de CORS, añade `X-Request-Id` y MDC, y CORS expone ese header al origen permitido. Se verifican 401, 403, 413, 500, 503 y rechazo CORS; nunca se registra contraseña, JWT, Authorization ni cuerpo. | CORREGIDO Y TESTEADO |
| A8 | `PrivateEndpointAuthorizationArchitectureTest` usa el `RequestMappingHandlerMapping` real del contexto MVC: el login es la única ruta pública de aplicación y todo otro mapping `/api/` exige `@PreAuthorize`; el alcance de recurso permanece en servicios/repositorios. | CORREGIDO Y TESTEADO |
| A9 | El detalle de estudiante para profesor usa `existsVisibleToProfessor` en vez de cargar la lista completa; conserva filtros ACTIVE y falla cerrado. | CORREGIDO Y TESTEADO |
| A10 | Los riesgos residuales están separados de defectos cerrados: HS256/TTL, revocación, rate limit distribuido, integración/Bruno bloqueados, CI/SCA remoto pendiente y revisión humana del diff. | DOCUMENTADO |
| A11 | `SIGRA/.env` no se rastrea (`.gitignore`) ni se incluyó en un artefacto; se documentó excluir `.env*`, `.git` y salidas de build de futuros paquetes. Solo `.env.example` es compartible. | VERIFICADO |
| A12 | 401, 403, 404 y política de usuario/matrícula/asignación inactiva tienen pruebas web, unitarias e integración. | CORREGIDO Y TESTEADO |
| A13 | Las tres pruebas de H-02 fueron habilitadas y la suite `clean test` terminó con 618 tests, 0 fallos, 0 errores y 0 omitidos. | CERRADO Y TESTEADO |

## Archivos y símbolos relevantes

- Autenticación y límites: `JwtAuthenticationFilter`, `RequestSizeLimitFilter`, `RequestCorrelationFilter`, `LoginRequestDTO`, `AuthServiceImpl` y `SecurityConfig`.
- Errores: `JsonSecurityResponse`, `JsonAuthenticationEntryPoint`, `JsonAccessDeniedHandler`, `ErrorResponseBuilder`, `AuthExceptionHandler`, `GlobalExceptionHandler` y `ManejadorErroresTransversal`.
- Autorización: `PrivateEndpointAuthorizationArchitectureTest`, controladores con `@PreAuthorize`, `EstudianteRepository.existsVisibleToProfessor` y `EstudianteServiceImpl`.
- Evidencia: `VALIDATION.md`, `BRUNO_AUDIT.md`, `TEST_PLAN.md` y `FRONTEND_AUTH_CONTRACT.md`.

## Ubicaciones verificables

| Archivo | Líneas/símbolos relevantes |
|---|---|
| `SIGRA/src/main/java/co/edu/uco/sigra/auth/security/JwtAuthenticationFilter.java` | 30 `JwtAuthenticationFilter`; 51 `doFilterInternal`; 105 evento 503 de repositorio |
| `SIGRA/src/main/java/co/edu/uco/sigra/auth/security/RequestCorrelationFilter.java` | 14 clase; 20 `doFilterInternal` y MDC/header |
| `SIGRA/src/main/java/co/edu/uco/sigra/auth/security/RequestSizeLimitFilter.java` | 14 clase; 25 `doFilterInternal` y respuesta 413 |
| `SIGRA/src/main/java/co/edu/uco/sigra/auth/dto/LoginRequestDTO.java` | 10 record; 17–21 límites de correo y contraseña |
| `SIGRA/src/main/java/co/edu/uco/sigra/common/exception/ErrorResponseBuilder.java` | 10 clase; 14–25 cuerpo estándar |
| `SIGRA/src/main/java/co/edu/uco/sigra/config/SecurityConfig.java` | 34 clase y configuración de filtros/cadena |
| `SIGRA/src/main/java/co/edu/uco/sigra/auth/service/impl/AuthServiceImpl.java` | 65, 73, 92 y 98 eventos sanitizados de login |
| `SIGRA/src/main/java/co/edu/uco/sigra/estudiantes/repository/EstudianteRepository.java` | 70 `existsVisibleToProfessor` |
| `SIGRA/src/main/java/co/edu/uco/sigra/estudiantes/service/impl/EstudianteServiceImpl.java` | 142 uso de existencia visible |
| `SIGRA/src/test/java/co/edu/uco/sigra/auth/security/PrivateEndpointAuthorizationArchitectureTest.java` | prueba estructural basada en `RequestMappingHandlerMapping`, sin conteo fijo de mappings |

## Vulnerabilidades cerradas

- Acceso anónimo a rutas privadas: `anyRequest().authenticated()` más 401 JSON.
- Confusión de rol/correo/estado en JWT: se contrasta contra el usuario persistido en cada request.
- Doble ejecución del filtro y conversión incorrecta de errores posteriores a 401.
- BOLA/IDOR de profesores sobre estudiantes, matrículas y asignaturas fuera de su alcance.
- Bypass por principal nulo en servicios de estudiante, matrícula y asignatura.
- Pérdida de incrementos del bloqueo bajo concurrencia.
- Exposición de detalles técnicos en respuestas de error y registros de seguridad.

## Riesgos residuales

- El logout elimina el token en el cliente; no existe revocación inmediata en servidor.
- El bloqueo es por usuario, no un rate limit distribuido por IP/cuenta.
- La integración PostgreSQL y Bruno están bloqueados hasta disponer de un destino desechable y una instancia backend identificada por el JAR candidato.
- CI remoto y SCA no fueron ejecutados desde esta sesión; SCA quedó configurado para pull requests en `.github/workflows/ci.yml` y solo cubre dependencias cambiadas.
- La rotación de secretos solo sería necesaria si el `.env` local hubiera sido compartido o publicado; la verificación actual confirma que no está trackeado ni empaquetado.
