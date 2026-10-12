# SIGRA — contratos de integración frontend/backend

## Alcance

La integración de las vistas «Mis asignaturas» y «Mis materias» consume únicamente datos entregados
por la API autenticada. La identidad y el alcance se resuelven en el backend a partir del JWT; el
frontend no envía un UUID de usuario para construir esas listas.

| Funcionalidad | Endpoint | Roles autorizados | Componente Angular | Estado |
|---|---|---|---|---|
| Mis asignaturas del profesor | `GET /api/v1/asignaturas/mias` | `PROFESOR` | `/profesor/asignaturas` | Integrada |
| Mis materias del estudiante | `GET /api/v1/asignaturas/mias` | `ESTUDIANTE` | `/estudiante/materias` | Integrada |
| Resultados de una asignatura propia | `GET /api/v1/asignaturas/{id}/resultados-aprendizaje` | `ADMINISTRADOR` o profesor con asignación activa | Selector de la vista del profesor | Integrada |
| Selector de asignaturas para matrícula | `GET /api/v1/asignaturas` | `ADMINISTRADOR`, `PROFESOR` con alcance propio | `/profesor/matricula`, `/admin/matricula` | Existente y auditado |
| Selector de semestres | `GET /api/v1/semestres` | `ADMINISTRADOR`, `PROFESOR`, `ESTUDIANTE` | Pantallas de matrícula | Existente y auditado |
| Matriculados por asignatura y semestre | `GET /api/v1/matriculas?asignaturaId=&semestreId=&incluirInactivas=` | `ADMINISTRADOR`, `PROFESOR` con asignación activa | `/profesor/matricula` | Existente y auditado |
| Materias de un estudiante | `GET /api/v1/estudiantes/{id}/asignaturas` | Administrador, estudiante propio o profesor relacionado | Ficha administrativa | No se usa para «Mis materias» |

## Contrato usado por Angular

`MiAsignaturaDTO` entrega `id`, `codigo`, `nombre`, `programaId`, `programaNombre`, `estado`,
`cantidadRa`, `cantidadEstudiantes` y `profesores`. La respuesta de `/mias` no incluye el código del
semestre de cada matrícula; por eso la vista de estudiante no inventa ni deduce ese dato. Si el
producto requiere semestre en esta pantalla, el backend debe exponerlo en un contrato explícito en
un trabajo posterior.

La respuesta de matrículas entrega `matriculaId`, documento, nombre completo y estado. No se añadió
una columna de fecha ni de calificaciones: esos campos no existen en el contrato actual.

## Autorización y errores esperados

- JWT ausente, inválido o expirado: `401`.
- Rol válido sin permiso o recurso fuera del alcance del profesor: `403`.
- UUID o código bien formado pero inexistente: `404` según el recurso.
- Parámetros inválidos o incompletos: `400`.
- Fallo no controlado del servidor: `500`, sin exponer detalles técnicos al usuario.
- Cuenta inactiva: no inicia sesión; no se crea una sesión frontend utilizable.

El endpoint de matrículas comprueba nuevamente la propiedad de la asignatura en el servicio. La
selección o un query parameter enviado desde el navegador no amplía el alcance del profesor.

## QA y limitaciones

La validación QA usa las asignaturas de demostración `QA-VEC3B41597D` y `QA-V1A16EFA4DE`, el semestre
`BRUEST-A1` y seis estudiantes matriculados por asignatura. Los módulos de evaluaciones,
calificaciones, estadísticas e historial continúan pendientes: no hay soporte backend completo para
simularlos de forma segura y permanecen en `ModuloPendiente`.

No se modificaron contraseñas, roles, usuarios, esquema PostgreSQL ni seeds destructivos. En este
cambio no fue necesario modificar código backend; se documenta el contrato que ya utiliza Angular.
