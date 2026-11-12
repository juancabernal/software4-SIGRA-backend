# Guía de documentación Swagger (OpenAPI) — Reportes

Documentación de los reportes **R1** y **R6** (incidencia 0000041), siguiendo el patrón de [`swagger-profesores.md`](swagger-profesores.md).

## Ver la documentación

1. Arranca el backend (`./gradlew bootRun` dentro de `SIGRA/`).
2. Abre `http://localhost:8080/swagger-ui.html` y busca la sección **Reportes**.
3. Haz login con `POST /api/v1/auth/login`, copia el `token` y pulsa **Authorize** (solo el token, sin `Bearer`).

## Endpoints documentados

| Método y ruta | Rol | Códigos documentados |
| --- | --- | --- |
| `GET /api/v1/reportes/rendimiento-ra` | ADMINISTRADOR (todo), PROFESOR (solo sus asignaturas con asignación ACTIVA) | 200, 400, 401, 403, 404 |
| `GET /api/v1/reportes/riesgo-academico` | ADMINISTRADOR (todo), PROFESOR (solo sus asignaturas con asignación ACTIVA) | 200, 400, 401, 403, 404 |

Parámetros opcionales comunes: `semestreId` (sin valor usa el semestre activo), `programaId`, `facultadId` y `asignaturaId`, todos UUID y combinables con AND. R6 añade `minRaNoAlcanzados` (entero ≥ 1; por defecto 1).

**Facultad (incidencia 0000036).** `facultadId` filtra por la facultad del programa de cada asignatura: una facultad sin datos, inexistente o combinada con un `programaId` de otra facultad devuelve la lista vacía con «Sin datos disponibles» (no 404); un valor que no es UUID responde 400. Respeta el alcance del profesor: una `asignaturaId` propia de otra facultad queda fuera del resultado, y una ajena sigue siendo 403. R1 agrega `facultadId` y `facultadNombre` a cada asignatura; R6 agrega `programaId`, `facultadId` y `facultadNombre` a cada fila de estudiante.

## Reglas R1 — Rendimiento por RA

- Reutiliza las estadísticas existentes (nivel, promedio y distribución); no redefine los rangos de `CatalogoNivelesLogro`.
- Estado por RA: `SIN_EVALUAR` (sin calificaciones), `DATOS_INSUFICIENTES` (nadie llega a 3 calificaciones) o `CON_NIVEL`.
- Sin asignaturas que mostrar, el campo `mensaje` es «Sin datos disponibles».
- Un PROFESOR que pide una `asignaturaId` que no tiene asignada recibe 403.
- Los 404 no incluyen identificadores en el mensaje (RNF-17).

## Reglas R6 — Riesgo académico

- Lee `nivel_logro_estudiante` con nivel `BAJO` o `MEDIO` (mismo criterio de insuficiente que RF-16); no recalcula.
- Agrupa por estudiante × asignatura; severidad: `ATENCION` (1), `RIESGO` (2), `ALTO_RIESGO` (≥ 3).
- Lista vacía → `mensaje` «Sin datos disponibles».
- Misma política de 403/404 que R1.
