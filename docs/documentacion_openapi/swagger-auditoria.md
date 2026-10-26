# Swagger/OpenAPI del historial de auditoría

Esta guía describe `GET /api/v1/auditoria`, el endpoint del módulo RF-19/RF-21c que consulta el historial general de eventos registrados por SIGRA. No es un endpoint separado dedicado exclusivamente a sesiones: los eventos de login conviven con cambios de estado y eventos de calificaciones.

## Endpoint y autorización

`GET /api/v1/auditoria`

La operación aparece bajo el tag **Auditoría** con el nombre **Consultar historial de auditoría**. Está protegida por el único esquema OpenAPI `bearerAuth` (HTTP Bearer, formato JWT) y su implementación conserva:

```java
@PreAuthorize("hasRole('ADMINISTRADOR')")
```

Por tanto:

- un JWT ausente, inválido o expirado produce `401`;
- un usuario autenticado sin rol `ADMINISTRADOR` produce `403`;
- únicamente un administrador puede consultar el historial.

## Parámetros

Todos los parámetros son opcionales. Los valores por defecto son `pagina=0` y `tamano=20`.

| Parámetro | Descripción | Ejemplo |
| --- | --- | --- |
| `desde` | Fecha inicial inclusiva, formato `YYYY-MM-DD` | `2026-10-01` |
| `hasta` | Fecha final inclusiva, formato `YYYY-MM-DD` | `2026-10-20` |
| `tipoEntidad` | Tipo de entidad auditada; por ejemplo `USUARIO` | `USUARIO` |
| `pagina` | Número de página desde 0 | `0` |
| `tamano` | Cantidad de registros por página, entre 1 y 100 | `20` |

La consulta:

```text
GET /api/v1/auditoria?tipoEntidad=USUARIO&pagina=0&tamano=20
```

filtra por entidad `USUARIO`, pero no exclusivamente por logins exitosos: incluye los eventos de usuario que existan, como `LOGIN_EXITOSO` y `LOGIN_FALLIDO`. Actualmente no existen parámetros públicos para `accion`, `usuarioId` ni rol; no deben agregarse al cliente como si fueran parte del contrato.

El rango de fechas es inclusivo. La respuesta se ordena desde el evento más reciente hasta el más antiguo. Un rango donde `desde` sea posterior a `hasta`, un enum desconocido, una fecha mal formada, una página negativa o un tamaño fuera de 1–100 produce `400`.

## Respuesta `200 OK`

```json
{
  "contenido": [
    {
      "id": "4b64b64b-4b64-4b64-8b64-4b64b64b64b6",
      "fechaHora": "2026-10-20T14:35:12.123",
      "accion": "LOGIN_EXITOSO",
      "tipoEntidad": "USUARIO",
      "idEntidad": "4b64b64b-4b64-4b64-8b64-4b64b64b64b6",
      "usuarioId": "4b64b64b-4b64-4b64-8b64-4b64b64b64b6",
      "rolUsuario": "ADMINISTRADOR",
      "detalle": "Inicio de sesión exitoso como ADMINISTRADOR"
    },
    {
      "id": "5c75c75c-5c75-5c75-9c75-5c75c75c75c7",
      "fechaHora": "2026-10-20T14:30:08.012",
      "accion": "LOGIN_FALLIDO",
      "tipoEntidad": "USUARIO",
      "idEntidad": null,
      "usuarioId": null,
      "rolUsuario": null,
      "detalle": "Inicio de sesión rechazado: credenciales inválidas"
    }
  ],
  "pagina": 0,
  "tamano": 20,
  "totalElementos": 2,
  "totalPaginas": 1
}
```

`fechaHora` representa `LocalDateTime` con el reloj de Colombia y se serializa como fecha/hora local, por ejemplo `2026-10-20T14:35:12.123`; no incluye automáticamente un offset `-05:00`. El `timestamp` de una respuesta de error sí pertenece a `OffsetDateTime` y puede incluir offset.

## Eventos de login

`AuditoriaAspect` registra actualmente:

- `LOGIN_EXITOSO`: se asocia al usuario autenticado y contiene `idEntidad`, `usuarioId`, `rolUsuario`, `tipoEntidad=USUARIO`, fecha/hora, acción y el detalle del rol de ingreso.
- `LOGIN_FALLIDO`: representa credenciales inválidas o bloqueo temporal. Para no exponer la identidad intentada, `idEntidad`, `usuarioId` y `rolUsuario` pueden ser nulos. Esto es intencional.

Los demás valores actuales de `AccionAuditoria` son `CAMBIO_ESTADO`, `CALIFICACION_FUERA_DE_FECHA` y `CALIFICACION_MODIFICADA`. No se deben documentar acciones adicionales. El historial no registra, como parte de este DTO, correo intentado, dirección IP, dispositivo ni geolocalización.

## Respuestas HTTP

| HTTP | Significado | Mensaje de ejemplo |
| --- | --- | --- |
| 200 | Consulta correcta | Cuerpo `PaginaAuditoria` |
| 400 | Filtro, rango o paginación inválidos | `La fecha inicial del filtro no puede ser posterior a la fecha final.` o `El valor del parámetro 'tipoEntidad' no es válido` |
| 401 | Falta o no es válido el JWT | `Debe iniciar sesión para acceder a este recurso` |
| 403 | JWT válido sin rol administrador | `No tiene permisos suficientes para realizar esta acción` |

Todos los errores usan `RespuestaErrorDoc` y ejemplos de `EjemplosError`; los detalles técnicos no forman parte del contrato público.

## Usar Swagger UI

1. Arranca el backend desde `SIGRA/` con `./gradlew.bat bootRun`.
2. Abre [Swagger UI](http://localhost:8080/swagger-ui.html).
3. En **Autenticación**, ejecuta el login con una cuenta de prueba válida.
4. Copia solamente el campo `token` de la respuesta `200`.
5. Pulsa **Authorize** y pega el token sin escribir `Bearer`.
6. Expande **Auditoría** y ejecuta `GET /api/v1/auditoria`.
7. Comprueba que una cuenta `ADMINISTRADOR` obtiene `200`.
8. Repite sin token para verificar `401` y con un token válido de `PROFESOR` o `ESTUDIANTE` para verificar `403`.

La especificación JSON está en [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs). `SecurityConfig` permite cargar Swagger UI y `/v3/api-docs` en el entorno configurado, pero conserva autenticadas las rutas de negocio.

## Escenarios de prueba

- Consulta sin parámetros: `pagina=0`, `tamano=20`.
- Consulta de usuarios: `?tipoEntidad=USUARIO&pagina=0&tamano=20`.
- Rango inclusivo: `?desde=2026-10-01&hasta=2026-10-20`.
- `tamano=0`, `tamano=101` o `pagina=-1` → `400`.
- `tipoEntidad=NO_EXISTE` o fecha `20/10/2026` → `400`.
- `desde` posterior a `hasta` → `400` con el mensaje de `RangoFechasInvalidoException`.
- Sin JWT → `401`; JWT de profesor o estudiante → `403`; JWT de administrador → `200`.
- Verificar que los registros estén en orden descendente por `fechaHora` y que los intentos rechazados puedan tener campos de identidad nulos.

## Clases implementadas

- `AuditoriaController`: endpoint, parámetros, autorización y respuestas OpenAPI.
- `PaginaAuditoriaDTO`: contenido y metadatos de paginación.
- `RegistroAuditoriaDTO`: representación de cada evento y sus campos nulables.
- `AuditoriaServiceImpl`: filtros inclusivos, orden y paginación.
- `AuditoriaAspect`: eventos de login, cambios de estado y calificaciones.
- `AccionAuditoria` y `TipoEntidadAuditoria`: valores permitidos por el dominio.
- `OpenApiConfig`, `RespuestaErrorDoc` y `EjemplosError`: seguridad, esquema de errores y ejemplos compartidos.
- `ApiDocumentacionIT`: verificación automática del documento generado.

## Consideraciones de seguridad

El historial contiene información de actividad del sistema y está reservado al administrador. No exponga respuestas de auditoría en logs, capturas públicas ni ejemplos con UUIDs o datos reales. El acceso debe ejecutarse con un JWT vigente y el filtro `tipoEntidad=USUARIO` no debe interpretarse como un filtro exclusivo de sesiones.
