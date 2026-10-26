# Matriz definitiva RBAC y alcance RF-02/RF-03

La tabla documenta los mappings de negocio actuales, incluido el login público. `SecurityConfig` aplica `anyRequest().authenticated()` y la autorización fina se ejecuta con `@PreAuthorize` y controles de servicio. `GET /actuator/health` y las solicitudes CORS `OPTIONS` son excepciones operativas fuera de esta matriz.

`✓` significa permitido con el alcance indicado; `—` significa denegado. `ADMINISTRADOR` opera globalmente salvo donde se indique. El profesor nunca obtiene alcance por el UUID recibido: se comprueba una asignación docente ACTIVA y, para estudiantes, una matrícula ACTIVA en una asignatura asignada. El estudiante solo puede consultar su propio UUID.

| # | Método y ruta | ADMINISTRADOR | PROFESOR | ESTUDIANTE | Control de recurso |
|---:|---|---|---|---|---|
| 1 | `POST /api/v1/asignaturas` | ✓ | — | — | Gestión |
| 2 | `GET /api/v1/asignaturas` | ✓ | ✓ | — | Profesor: asignaturas con asignación docente ACTIVA |
| 3 | `GET /api/v1/asignaturas/mias` | — | ✓ | ✓ | Profesor: asignación ACTIVA; estudiante: matrícula ACTIVA |
| 4 | `GET /api/v1/asignaturas/{id}` | ✓ | ✓ | — | Profesor solo si la asignatura está asignada ACTIVA |
| 5 | `PUT /api/v1/asignaturas/{id}` | ✓ | — | — | Gestión |
| 6 | `PATCH /api/v1/asignaturas/{id}/activar` | ✓ | — | — | Gestión |
| 7 | `PATCH /api/v1/asignaturas/{id}/inactivar` | ✓ | — | — | Gestión |
| 8 | `POST /api/v1/auth/login` | Público | Público | Público | Credenciales institucionales; no requiere token |
| 9 | `GET /api/v1/tipos-documento` | ✓ | ✓ | — | Catálogo; profesor lo necesita para registrar estudiantes (fila 10) |
| 10 | `POST /api/v1/estudiantes` | ✓ | ✓ | — | Alta; el profesor puede registrar cuando la regla de negocio lo permite |
| 11 | `GET /api/v1/estudiantes` | ✓ | ✓ | — | Admin global; profesor con `filtro` busca entre todos los estudiantes ACTIVOS (RF-09a, D18, para poder matricular a quien aún no tiene relación con él); sin `filtro` ve su propio roster (matrícula y asignación ACTIVA) |
| 12 | `GET /api/v1/estudiantes/{id}` | ✓ | ✓ | ✓ | Admin global; profesor relación ACTIVA; estudiante solo sí mismo |
| 13 | `PUT /api/v1/estudiantes/{id}` | ✓ | — | — | Gestión |
| 14 | `DELETE /api/v1/estudiantes/{id}` | ✓ | — | — | Gestión; inactivación lógica |
| 15 | `GET /api/v1/estudiantes/{id}/asignaturas` | ✓ | ✓ | ✓ | Admin historial; profesor solo matrículas ACTIVA de sus asignaturas; estudiante propio |
| 16 | `POST /api/v1/matriculas` | ✓ | ✓ | — | Profesor solo en asignatura con asignación docente ACTIVA |
| 17 | `DELETE /api/v1/matriculas/{id}` | ✓ | ✓ | — | Profesor solo si la matrícula pertenece a una asignatura asignada ACTIVA |
| 18 | `GET /api/v1/matriculas` | ✓ | ✓ | — | Mismo alcance por asignatura; `incluirInactivas` no amplía el alcance |
| 19 | `POST /api/v1/profesores` | ✓ | — | — | Gestión |
| 20 | `GET /api/v1/profesores` | ✓ | — | — | Catálogo administrativo |
| 21 | `GET /api/v1/profesores/{id}/asignaturas` | ✓ | — | — | Catálogo administrativo |
| 22 | `PUT /api/v1/profesores/{id}` | ✓ | — | — | Gestión |
| 23 | `DELETE /api/v1/profesores/{id}` | ✓ | — | — | Gestión; inactivación lógica |
| 24 | `POST /api/v1/programas` | ✓ | — | — | Gestión |
| 25 | `GET /api/v1/programas` | ✓ | — | — | Catálogo administrativo |
| 26 | `GET /api/v1/programas/{id}` | ✓ | — | — | Catálogo administrativo |
| 27 | `PUT /api/v1/programas/{id}` | ✓ | — | — | Gestión |
| 28 | `PATCH /api/v1/programas/{id}/inactivar` | ✓ | — | — | Gestión |
| 29 | `POST /api/v1/asignaturas/{asignaturaId}/resultados-aprendizaje` | ✓ | — | — | Gestión |
| 30 | `GET /api/v1/asignaturas/{asignaturaId}/resultados-aprendizaje` | ✓ | ✓ | — | Profesor solo con asignación docente ACTIVA |
| 31 | `GET /api/v1/resultados-aprendizaje` | ✓ | — | — | Catálogo global administrativo |
| 32 | `GET /api/v1/resultados-aprendizaje/{id}` | ✓ | ✓ | — | Profesor solo si el RA pertenece a asignatura asignada ACTIVA |
| 33 | `PUT /api/v1/resultados-aprendizaje/{id}` | ✓ | — | — | Gestión |
| 34 | `PATCH /api/v1/resultados-aprendizaje/{id}/inactivar` | ✓ | — | — | Gestión |
| 35 | `PATCH /api/v1/resultados-aprendizaje/{id}/reactivar` | ✓ | — | — | Gestión |
| 36 | `POST /api/v1/semestres` | ✓ | — | — | Gestión |
| 37 | `GET /api/v1/semestres` | ✓ | ✓ | ✓ | Lectura |
| 38 | `GET /api/v1/semestres/{codigo}` | ✓ | ✓ | ✓ | Lectura |
| 39 | `PATCH /api/v1/semestres/{codigo}` | ✓ | — | — | Gestión |
| 40 | `POST /api/v1/resultados-aprendizaje/{raId}/evaluaciones` | — | ✓ | — | Profesor solo con asignación docente ACTIVA sobre la asignatura del RA (verificado en el servicio) |
| 41 | `GET /api/v1/resultados-aprendizaje/{raId}/evaluaciones` | — | ✓ | — | Mismo alcance docente; sin `semestreId` usa el semestre activo |
| 42 | `GET /api/v1/evaluaciones/{id}` | — | ✓ | — | Profesor solo si la evaluación pertenece a una asignatura asignada ACTIVA |
| 43 | `PUT /api/v1/evaluaciones/{id}` | — | ✓ | — | Mismo alcance docente; solo semestre en curso y evaluación ACTIVA |
| 44 | `PATCH /api/v1/evaluaciones/{id}/inactivar` | — | ✓ | — | Mismo alcance docente |
| 45 | `PATCH /api/v1/evaluaciones/{id}/reactivar` | — | ✓ | — | Mismo alcance docente |
| 46 | `GET /api/v1/asignaturas/{asignaturaId}/resultados-aprendizaje/rendimiento` | ✓ | ✓ | — | Profesor solo con asignación docente ACTIVA sobre la asignatura (`@securityAuthorization.hasActiveAssignment`); calcula sobre el semestre activo |

## Identidad y datos

- `UsuarioPrincipal` implementa `java.security.Principal`; `Authentication.getName()` devuelve el UUID persistido.
- El UUID de ruta y los IDs del cuerpo son datos de selección, nunca una identidad autenticada.
- El filtro valida firma, algoritmo HS256, claims mínimos, UUID, rol, correo, usuario existente y estado ACTIVO en cada solicitud.
- Los overloads de consultas protegidas que no reciben principal resuelven `CurrentUser.required()` y fallan cerrado; no convierten ausencia de identidad en administrador.

## Certificación estructural

`PrivateEndpointAuthorizationArchitectureTest` inspecciona el `RequestMappingHandlerMapping` real de Spring MVC: el único endpoint público permitido en la lista explícita es `POST /api/v1/auth/login`; todos los demás mappings de `/api/` deben declarar `@PreAuthorize`. Esta prueba no sustituye los controles de alcance en servicio, que siguen siendo obligatorios para profesor y estudiante.
