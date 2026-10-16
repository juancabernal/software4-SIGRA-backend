# Riesgos y decisiones RF-05

## Decisiones

- Se conserva el contrato de login RF-02/RF-03 y sus DTOs.
- Se mantiene HS256 y JWT stateless; no se agregan refresh tokens, Redis ni infraestructura adicional.
- Se adopta ROLE_ como prefijo interno de Spring; los roles de negocio siguen siendo ADMINISTRADOR, PROFESOR y ESTUDIANTE.
- CORS vive únicamente en SecurityConfig.
- Swagger/OpenAPI no se incluye en las rutas públicas por defecto. Health sí queda público para operación básica.
- ddl-auto por defecto es validate; los cambios de esquema siguen siendo SQL explícito.

## Riesgos pendientes

- La fuerza bruta distribuida por múltiples cuentas/IP no queda resuelta por el bloqueo por usuario; debe complementarse con rate limiting de gateway/WAF.
- La revocación inmediata de JWT no existe; requerirla implica una política de sesiones o denylist.
- La autorización de profesor depende de asignaciones docentes ACTIVAS correctamente persistidas; se comprobó con dos profesores y dos estudiantes en PostgreSQL.
- La integración Gradle con PostgreSQL y Bruno no se ejecutaron en esta pasada: requieren un destino
  desechable explícitamente confirmado y una instancia arrancada desde el JAR candidato. Ver
  `VALIDATION.md` y `BRUNO_AUDIT.md`.
- CI remoto no se ejecutó desde esta sesión. SCA quedó configurado en el workflow mediante
  `dependency-review-action`; solo revisa dependencias nuevas o cambiadas en un pull request y no
  sustituye un scanner integral del inventario existente.
