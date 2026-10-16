# Validación RF-02/RF-03

Fecha de esta pasada: 2026-10-11 (America/Bogota). Rama: `jose-valencia/rf-05-security-rbac`.
La fuente de verdad de los resultados es el código y las salidas conservadas en `SIGRA/build`;
los reportes previos no se reutilizan como evidencia de este SHA.

## Compuertas

| Compuerta | Estado | Evidencia actual |
|---|---|---|
| H-02: `ContratoErrorProfesoresTest` habilitado | PASS | Las tres pruebas dejaron de estar deshabilitadas; `test` focalizado terminó `BUILD SUCCESSFUL`. `ProfesorExceptionHandler` declara `@Order(Ordered.HIGHEST_PRECEDENCE)`. |
| Suite unitaria limpia | PASS | `clean test --offline --no-daemon --max-workers=1`: 618 tests, 0 fallos, 0 errores, 0 omitidos. XML/HTML completos preservados en `SIGRA/build/evidence/clean-test-2026-10-11`. |
| Integración PostgreSQL | BLOCKED | No había listener local en 5432; Docker no fue accesible. El task usa `.env` y contiene limpieza real de datos, por lo que no se ejecutó contra un destino cuya localidad y aislamiento no pudieron confirmarse. |
| `bootJar` | PASS | `bootJar --offline --no-daemon --max-workers=1`: `BUILD SUCCESSFUL`. JAR: `SIGRA/build/libs/SIGRA-0.0.1-SNAPSHOT.jar`; SHA-256: `C69FFA0B91A34F6339AA042FCD165608F72ADEA30B13F0829D4E87E36311AA37`. |
| Bruno seguridad y colección global | BLOCKED | No se ejecutó en esta pasada: no existe una instancia backend identificada sobre el JAR anterior ni una base de pruebas aislada disponible. El inventario estático sí cuenta 246 requests `.bru`; no se declara 8/8 ni 246/246 por inferencia. |
| `git diff --check` | PASS | Sin errores de whitespace con `core.whitespace=cr-at-eol`; permanecen solo advertencias informativas de conversión LF/CRLF. |
| Secretos y artefactos | PASS | `SIGRA/.env` está ignorado y no trackeado. El JAR no contiene `.env`, `.git`, `build`, `.gradle` ni logs. No se generó ZIP compartible. |
| CI remoto | BLOCKED | No hay commit candidato publicado en esta pasada; no se ejecutaron jobs remotos ni se abrió PR. |
| SCA | PENDIENTE DE EJECUCIÓN REMOTA | El workflow conserva `actions/dependency-review-action@v4` con umbral `moderate` y añade `dependencyCheckAnalyze` de OWASP para Gradle con umbral CVSS 4.0 y `failOnError=true`. El scanner bloquea ante vulnerabilidades moderadas o superiores y ante fallos de actualización NVD; falta observar el job en GitHub sobre este commit. |

## Criterio de cierre

Estado global: **BLOCKED** para certificación y merge. El código unitario y el empaquetado local están
verificados, pero faltan una integración sobre PostgreSQL aislado, Bruno contra este JAR identificado,
CI/SCA remoto en un SHA publicado y revisión humana del diff. No se hizo merge a `develop` o `main`.

## Evidencia reproducible

Las ejecuciones exitosas usaron Java 21 y Gradle 9.7.1 en modo offline. En esta estación el wrapper
no pudo crear el lock en la caché global por permisos; se utilizó la distribución Gradle ya instalada,
sin descargar dependencias.

El inventario runtime se conserva en `SIGRA/build/evidence/runtime-dependencies-2026-10-11.txt`.
El secreto `NVD_API_KEY` debe configurarse en GitHub Actions para que la versión actual del
cliente NVD pueda actualizar su base; si falta o es inválido, el job queda bloqueado por
`failOnError=true` y no se transforma en PASS.
Los reportes de la corrida limpia se conservan en `SIGRA/build/evidence/clean-test-2026-10-11`.
