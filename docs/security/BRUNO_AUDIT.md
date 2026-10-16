# Auditoría de colecciones Bruno

Fecha de revisión: 2026-10-11 (America/Bogota). Esta pasada no ejecutó Bruno: no había backend
identificado sobre el JAR candidato ni una base PostgreSQL aislada disponible. Los resultados
históricos de esta página no se consideran evidencia del SHA actual.

## Inventario

Se revisó estáticamente todo `bruno/SIGRA`, no solo `seguridad-rf05`:

- 246 archivos de petición HTTP (`.bru`) detectados.
- 6 carpetas con herencia de `Authorization: Bearer {{token...}}`.
- 95 peticiones esperan 200/201/202/204 o 3xx.
- Las 23 regresiones positivas identificadas por la auditoría recibida ya no aparecen sin Bearer directo o heredado.
- Los escenarios de login, health y negativos anónimos conservan su intención y no se les añadió una autenticación que invalidara el caso.

## Cambios

Se añadieron headers Bearer a las carpetas raíz de `asignaturas`, `estudiantes`, `profesores`, `programas` y `semestres`; `resultados-aprendizaje` y las carpetas que ya tenían autenticación se conservaron. Los casos de `seguridad-rf05` mantienen sus tokens por rol y casos negativos.

## Estado de ejecución

**BLOCKED; no se declara PASS por inferencia.** El inventario estático cuenta 246 archivos de
petición `.bru`, pero no prueba respuestas HTTP. Para cerrar esta compuerta se debe:

1. crear una base PostgreSQL desechable y ejecutar los seeds reproducibles;
2. arrancar exactamente `SIGRA/build/libs/SIGRA-0.0.1-SNAPSHOT.jar`, con un puerto libre documentado;
3. registrar PID, puerto, SHA-256 `C69FFA0B91A34F6339AA042FCD165608F72ADEA30B13F0829D4E87E36311AA37`,
   salida de arranque y exit code;
4. ejecutar `@usebruno/cli` y conservar la salida de seguridad y de la colección global.

No se modificaron reglas académicas ni de autorización durante esta revisión. `.env` sigue ignorado
por Git y no debe entrar en ZIP, logs ni evidencias compartibles.
