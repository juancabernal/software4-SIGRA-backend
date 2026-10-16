# Empaquetado seguro

Se generó únicamente el JAR local de verificación con `bootJar`; no se generó ni compartió un ZIP.
El artefacto está en `SIGRA/build/libs/SIGRA-0.0.1-SNAPSHOT.jar` y su SHA-256 es
`C69FFA0B91A34F6339AA042FCD165608F72ADEA30B13F0829D4E87E36311AA37`. Para cualquier ZIP, imagen o
artefacto de entrega se deben excluir explícitamente:

- `SIGRA/.env` y cualquier `.env*` con secretos;
- `.git/`, `.gradle/`, `build/` y logs locales;
- credenciales, tokens y dumps de bases de datos.

La verificación actual confirma que `SIGRA/.env` existe solo como configuración local,
está ignorado por Git, no está trackeado y no aparece dentro del JAR. Si una credencial llegara a
aparecer en un artefacto compartido, la acción obligatoria sería revocarla y rotarla antes de continuar.
