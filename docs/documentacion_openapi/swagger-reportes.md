# Guía de documentación Swagger (OpenAPI) — Reportes

Documentación del reporte **R1 – Rendimiento académico por RA y asignatura**, siguiendo el patrón de [`swagger-profesores.md`](swagger-profesores.md).

## Ver la documentación

1. Arranca el backend (`./gradlew bootRun` dentro de `SIGRA/`).
2. Abre `http://localhost:8080/swagger-ui.html` y busca la sección **Reportes**.
3. Haz login con `POST /api/v1/auth/login`, copia el `token` y pulsa **Authorize** (solo el token, sin `Bearer`).

## Endpoints documentados

| Método y ruta | Rol | Códigos documentados |
| --- | --- | --- |
| `GET /api/v1/reportes/rendimiento-ra` | ADMINISTRADOR (todo), PROFESOR (solo sus asignaturas con asignación ACTIVA) | 200, 400, 401, 403, 404 |

Parámetros opcionales: `semestreId` (sin valor usa el semestre activo), `programaId` y `asignaturaId`, todos UUID y combinables.

## Reglas del reporte

- Reutiliza las estadísticas existentes (nivel, promedio y distribución); no redefine los rangos de `CatalogoNivelesLogro`.
- Estado por RA: `SIN_EVALUAR` (sin calificaciones), `DATOS_INSUFICIENTES` (nadie llega a 3 calificaciones) o `CON_NIVEL`.
- Sin asignaturas que mostrar, el campo `mensaje` es «Sin datos disponibles».
- Un PROFESOR que pide una `asignaturaId` que no tiene asignada recibe 403.
- Los 404 no incluyen identificadores en el mensaje (RNF-17).
