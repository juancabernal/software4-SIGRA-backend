# Trazabilidad RF-02/RF-03 y nomenclatura

La rama y varios documentos llaman `RF-05` al incremento interno de seguridad/RBAC. La numeración oficial se verificó en `docs/requisitos_funcionales/Entendimiento de Problema y Requisitos Funcionales.pdf`, páginas 1–3, extraído con `pdfplumber` y revisado visualmente. No se renombró la rama ni se reconstruyó RF-05.

## Mapa verificable usado

| Fuente | Uso en esta corrección |
|---|---|
| `SIGRA_RF02_RF03_REVISION_FINAL_2026-10-11.md` | Hallazgos P1–P3, criterios de auditoría y bloqueos de certificación |
| `SIGRA_RF02_RF03_INSTRUCCIONES_CIERRE_CODEX.md` | Acciones de cierre solicitadas para autenticación, autorización y transversal de seguridad |
| Código actual de `SIGRA` | Fuente de verdad para permisos efectivos y comportamiento ejecutable |
| `docs/requisitos_funcionales/Entendimiento de Problema y Requisitos Funcionales.pdf` | Fuente oficial revisada; páginas 1–3 |

## Mapa oficial

| Requisito oficial | Evidencia usada en esta pasada |
|---|---|
| RF-02 | Autenticación: login, JWT, usuario activo, bloqueo, 401/423 y contrato frontend |
| RF-03 | Autorización: roles, `@PreAuthorize`, alcance por recurso, 403 y matriz RBAC |
| RF-05 | En el PDF oficial corresponde a asignar profesores a asignaturas; no se reconstruyó en esta pasada |
| RF-06/RF-07 | Operaciones académicas de matrícula y registro; se usaron como contexto de regresión, no como alcance correctivo |

La matriz ejecutable está en `RBAC_MATRIX.md`. La etiqueta interna RF-05 no se presenta como equivalente al RF-05 oficial.
