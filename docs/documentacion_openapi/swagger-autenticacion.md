# Swagger/OpenAPI de autenticación JWT

Esta guía describe el contrato documentado del módulo de autenticación de SIGRA. La implementación existente valida las credenciales, aplica BCrypt, controla el estado y el bloqueo temporal del usuario y emite un JWT firmado. La guía no cambia la lógica ni agrega endpoints.

## Endpoint

`POST /api/v1/auth/login`

El login es público: no requiere JWT, `Authorization` ni CSRF. La operación aparece en Swagger bajo el tag **Autenticación** con el nombre **Iniciar sesión y obtener token JWT**.

El flujo real es:

1. Se valida el cuerpo y el formato del correo.
2. Se normaliza el correo institucional antes de buscarlo.
3. Se comprueba la contraseña mediante BCrypt.
4. Se verifica que el usuario esté `ACTIVO`.
5. Se comprueba que no esté bloqueado temporalmente.
6. Si las credenciales son correctas, se emite un JWT firmado.
7. Se devuelve la información pública del usuario y su rol.
8. La respuesta usa `Cache-Control: no-store` y `Pragma: no-cache`.

El cuerpo real del login está limitado a 16 KiB. El correo admite como máximo 254 caracteres y debe terminar en `@uco.net.co`. La contraseña es obligatoria y admite como máximo 72 bytes UTF-8; este límite se mide en bytes, no en caracteres.

## Solicitud

```json
{
  "correoInstitucional": "administrador.prueba@uco.net.co",
  "contrasena": "ClaveDePrueba2026!"
}
```

`correoInstitucional` y `contrasena` son obligatorios. Se aceptan mayúsculas y espacios laterales en el correo; el backend los normaliza. El ejemplo usa credenciales ficticias: no se deben publicar contraseñas reales, hashes BCrypt ni valores de producción.

## Respuesta exitosa

`200 OK` devuelve el JWT y los datos públicos de la sesión:

```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiI0YjY0b...firma-ficticia",
  "tipo": "Bearer",
  "expiraEn": 3600,
  "usuario": {
    "id": "4b64b64b-4b64-4b64-8b64-4b64b64b64b6",
    "nombreCompleto": "Administradora de Prueba",
    "correoInstitucional": "administrador.prueba@uco.net.co",
    "rol": "ADMINISTRADOR"
  }
}
```

Los roles del contrato son `ADMINISTRADOR`, `PROFESOR` y `ESTUDIANTE`. La respuesta no contiene `passwordHash`, contraseña ni datos internos de seguridad. `expiraEn` expresa segundos de vigencia.

## Respuestas de error

Los errores usan el esquema `RespuestaError`, con `timestamp`, `status`, `error`, `mensaje` y `detalles` solo cuando hay validaciones de campos.

| HTTP | Significado | Mensaje real |
| --- | --- | --- |
| 400 | Cuerpo ausente, mal formado o datos inválidos | `Los datos de entrada no cumplen con las validaciones requeridas` para validación; un cuerpo JSON mal formado usa `El cuerpo de la petición no es válido o está mal formado` |
| 401 | Correo/contraseña incorrectos o usuario inactivo | `Correo o contraseña incorrectos` |
| 423 | Cuenta bloqueada temporalmente | `El usuario está temporalmente bloqueado. Intente nuevamente más tarde.` |
| 413 | El cuerpo real del login supera 16 KiB | `El cuerpo de la petición supera el tamaño permitido` |
| 415 | `Content-Type` no soportado; utilizar `application/json` | `El tipo de contenido de la petición no es soportado por este recurso` |
| 500 | Fallo inesperado | `Ocurrió un error inesperado en el servidor` |

> Precisión de contrato: el endpoint público de login no emite actualmente `503` por indisponibilidad del repositorio. `JwtAuthenticationFilter` sí puede emitir `503` al validar un Bearer en endpoints protegidos, pero excluye explícitamente `POST /api/v1/auth/login`. Si falla el repositorio durante este login, el manejador general responde `500`. El contrato documentado refleja ese comportamiento real; establecer `503` también en login requeriría un cambio funcional distinto y pruebas adicionales.

Ejemplo `401`:

```json
{
  "timestamp": "2026-10-14T12:00:00-05:00",
  "status": 401,
  "error": "UNAUTHORIZED",
  "mensaje": "Correo o contraseña incorrectos"
}
```

Ejemplo `423 LOCKED`:

```json
{
  "timestamp": "2026-10-14T12:00:00-05:00",
  "status": 423,
  "error": "LOCKED",
  "mensaje": "El usuario está temporalmente bloqueado. Intente nuevamente más tarde."
}
```

`timestamp` pertenece al cuerpo estándar de errores y se serializa con offset. No confundirlo con `fechaHora` del historial de auditoría, que es `LocalDateTime`.

## Bloqueo por intentos fallidos

El contador se comporta así:

- Los primeros cuatro intentos incorrectos responden `401`.
- El quinto intento incorrecto registra el bloqueo y también responde `401`.
- Las solicitudes posteriores durante los 15 minutos de bloqueo responden `423`, incluso con la contraseña correcta; la contraseña no se evalúa.
- Tras expirar el bloqueo se permite intentar de nuevo.
- Un login correcto antes del bloqueo reinicia el contador.

Este comportamiento ya está implementado y probado en `AuthServiceImpl`; esta documentación no lo modifica.

## Usar Swagger UI

1. Arranca SIGRA desde `SIGRA/` con `./gradlew.bat bootRun`.
2. Abre [Swagger UI](http://localhost:8080/swagger-ui.html).
3. Expande **Autenticación** y ejecuta `POST /api/v1/auth/login` con una cuenta de prueba válida.
4. Copia únicamente el valor de `token` de la respuesta.
5. Pulsa **Authorize** y pega solo el token; Swagger agrega `Bearer` automáticamente.
6. Ejecuta un endpoint protegido. El login no debe mostrar un requisito Bearer.

La especificación JSON está en [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs). Swagger UI y esa especificación quedan públicas en el entorno configurado por `SecurityConfig`; no se hacen públicas las rutas de negocio.

## Escenarios de prueba

- Correo vacío, correo fuera de `@uco.net.co` o contraseña vacía → `400`.
- Credenciales incorrectas o usuario inactivo → `401`.
- Cinco intentos incorrectos y un intento posterior → quinto `401`, siguientes `423`.
- Contraseña correcta después de que expira el bloqueo → `200`.
- Cuerpo de más de 16 KiB → `413` sin invocar el servicio.
- `Content-Type` no soportado → `415`.
- Respuesta exitosa → verificar `token`, `tipo`, `expiraEn`, `usuario` y ausencia de `passwordHash`.

## Clases implementadas

- `AuthController`: operación y respuestas OpenAPI del login.
- `LoginRequestDTO`: cuerpo y restricciones de validación.
- `LoginResponseDTO` y `UsuarioSesionDTO`: esquema de respuesta pública.
- `AuthServiceImpl`: normalización, BCrypt, contador y bloqueo.
- `OpenApiConfig`: esquema único `bearerAuth` con HTTP Bearer y formato JWT.
- `RespuestaErrorDoc` y `EjemplosError`: esquema y ejemplos de errores.
- `ApiDocumentacionIT`: comprobaciones sobre el JSON generado por `/v3/api-docs`.

## Consideraciones de seguridad

No guardar el JWT ni las credenciales en ejemplos públicos de producción. No enviar manualmente `Bearer` dentro del campo de Swagger UI. El JWT es stateless y debe tratarse como un secreto de sesión; el endpoint de login evita caché y las respuestas de error no exponen trazas, SQL, hashes ni contraseñas.
