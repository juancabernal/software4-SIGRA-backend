# SIGRA — estado de seguridad y evidencia de release a main (2026-10-12)

## Alcance

Este documento pertenece al release aislado de autenticación JWT/RBAC y contratos de API del PR backend [#54](https://github.com/juancabernal/software4-SIGRA-backend/pull/54), destino `main`. No integra el trabajo posterior de `develop`. Su pareja frontend es [#32](https://github.com/juancabernal/software4-SIGRA-frontend/pull/32).

Se añadieron y conservaron DOS controles de análisis de vulnerabilidades de dependencias:
1. `SCA de dependencias` en `.github/workflows/ci.yml`: `actions/dependency-review-action@v4`, umbral `moderate` para las dependencias introducidas en el PR.
2. `SCA Gradle (OWASP Dependency-Check)`: plugin `org.owasp.dependencycheck` versión `13.0.0` en `SIGRA/build.gradle`, tarea `dependencyCheckAnalyze`, `failBuildOnCVSS = 4.0`, `failOnError = true`, formatos HTML/SARIF/JUNIT y reporte como artefacto del workflow.

**Estos controles están configurados, pero ninguno terminó un análisis exitoso en el último CI. No hay resultado SCA que permita afirmar que las dependencias no tienen vulnerabilidades. No confundir error de infraestructura/configuración con vulnerabilidad confirmada ni con evaluación satisfactoria.**

## Evidencia CI (commit de9de7d6b481321fd31ab7a4a191537557eff0cb)

GitHub Actions: https://github.com/juancabernal/software4-SIGRA-backend/actions/runs/38308853304

| Job | Resultado | Hallazgo |
| --- | --- | --- |
| Pruebas unitarias | PASS | Pruebas Java completadas |
| Pruebas de integración (PostgreSQL 16 efímero) | PASS | El esquema `sigra` usado por CI se valida con Hibernate/JPA |
| SCA de dependencias | FAIL / no ejecutable | `Dependency review is not supported on this repository`: requiere Dependency graph y soporte de la API de GitHub |
| SCA Gradle (OWASP Dependency-Check) | FAIL / análisis incompleto | NVD no pudo actualizarse: `Invalid API Key, length of 0 too short`, la variable `NVD_API_KEY` del workflow no estaba configurada |

Los dos fallos de SCA **no** son una lista de vulnerabilidades; representan *ausencia de escaneo válido*. La integración PostgreSQL del CI usa una instancia efímera, no el servidor Neon desplegado.

## Acción manual y riesgo residual

Permisos de administrador necesarios:
- En el repositorio backend: **Settings → Security & analysis**. Habilitar `Dependency graph` y, cuando aplique, la revisión de dependencias.
- Solicitar a NVD una API key y guardarla como secreto de GitHub Actions con nombre exacto `NVD_API_KEY`. No subir la clave ni registrarla en logs/docs.
- Ejecutar de nuevo **ambos** trabajos SCA. Si detectan dependencias vulnerables CVSS >= 4.0, corregir versiones y volver a correr; NO desactivar tests, NO añadir `continue-on-error` ni fingir `PASS`.

**Excepción solicitada por el responsable del cambio (2026-10-12):** autoriza evaluar temporalmente la publicación de SIGRA para *pruebas de demostración*, aunque ambos escáneres SCA no hayan podido ejecutarse por no disponer de permisos de administrador/clave NVD. La excepción reconoce riesgo residual de vulnerabilidades desconocidas. No constituye aprobación institucional ni sustituye una revisión de seguridad. Mantener los controles y revalidarlos antes de un uso real por usuarios externos.

## Datos QA en Neon

Evidencia del SQL Editor de Neon, base `neondb`, esquema `sigra`, rama `production` (2026-10-12): 17 comprobaciones de contenido/datos de prueba registraron `PASS`: 14 cuentas (11 activas, 3 inactivas), cada una con exactamente un subtipo de rol y hash no vacío; 2 administradores, 4 profesores, 8 estudiantes, 7 asignaturas `QA26-*`, 18 resultados de aprendizaje, 5 asignaciones docentes (4 activas), 8 matrículas (7 activas), semestre `2026-2` vigente. Esto verifica consistencia de los *seeds*; **NO** prueba que el hash coincida con la contraseña distribuida, que Render lea esa base, que JPA valide Neon, ni que JWT/RBAC funcione end-to-end.

Nota de seguridad: las 14 cuentas QA usan **una contraseña compartida** y una cuenta administrativa activa. Son exclusivamente para pruebas controladas. Retirar estas cuentas o rotar a contraseñas distintas y cerrar acceso público antes de habilitar usuarios reales; nunca guardar hashes ni contraseñas de QA en el repositorio.

## Trazabilidad de despliegue

- Backend `main` antes del merge: `51f45f0ab527a9ca8398325da30aadda7e5a286f`. La URL del backend que está compilada en el frontend `main` es `https://software4-sigra-backend.onrender.com/api`. **No se pudo confirmar mediante consulta de Render qué commit está desplegado actualmente ni un resultado live del health check**.
- Frontend `main` antes del merge: `89139504a74d00631c1803455b23a665deadd425`, con estado GitHub `Vercel: Deployment has completed` (2026-10-12). Frontend PR #32, commit `9a5fa537708fb711318a9b353c481289299c7240`, también recibió estado Vercel exitoso y CI `PASS`. El despliegue del PR puede ser una *preview*, no necesariamente el dominio principal.
- Frontend conserva URL Render en `src/environments/environment.ts`; no se deben sustituir valores ni editar secretos de Render/Vercel para este release sin causa verificada.

## Checklist post-merge obligatorio (NO realizado)

1. Confirmar en Render **Deploys** que el deployment de `main` tomó el SHA de merge del PR #54, estado `Live`, y revisar logs de inicio: Hibernate `validate`, conexión Neon, sin excepciones.
2. Consultar `GET https://software4-sigra-backend.onrender.com/actuator/health`, esperar `200` y `UP` si el actuator está expuesto.
3. Confirmar en Vercel **Deployments** que el deployment de producción de `main` tomó el SHA de merge del PR #32 y estado `Ready`.
4. En la UI: inicio de sesión con cuentas QA de ADMINISTRADOR, PROFESOR y ESTUDIANTE, navegación correcta por rol, listados de asignaturas, matrículas. Rechazo de usuario inactivo, `401` sin token y `403` con rol no autorizado. Probar profesor sin asignación y estudiante sin matrícula.
5. Confirmar que ningún rol tenga acceso a datos de otro usuario. No almacenar JWT ni contraseñas en capturas ni documentación.
6. Si cualquier comprobación falla, detener pruebas, analizar logs, preparar fix/rollback; no afirmar despliegue correcto hasta verificaciones reales.
7. Cerrar la excepción SCA tan pronto se obtengan permisos y clave NVD.

## Estado

- Datos Neon: **17/17 comprobaciones de contenido PASS**.
- CI backend: **unitarias PASS / integración PostgreSQL PASS / SCA GitHub BLOQUEADO / SCA OWASP BLOQUEADO**.
- CI frontend: **PASS** para PR #32.
- Pruebas end-to-end de la **aplicación desplegada**: **PENDIENTES**.
