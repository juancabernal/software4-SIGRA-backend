# Guía de documentación Swagger (OpenAPI)

Cómo se aplicó a `EstudianteController` el patrón de documentación OpenAPI de SIGRA, definido primero con **Profesores** (ver [`swagger-profesores.md`](swagger-profesores.md)); `AuthController` conserva su propio estilo.

## Ver la documentación

1. Arranca el backend (`.\gradlew.bat bootRun` dentro de `SIGRA/`).
2. Abre `http://localhost:8080/swagger-ui.html` (el JSON está en `/v3/api-docs`).
3. Haz login: `POST /api/v1/auth/login` → *Try it out* con un administrador de prueba y copia el campo `token`.
4. Pulsa **Authorize** y pega solo el token (sin la palabra `Bearer`). Desde ahí, *Try it out* envía el token en cada petición.

> Para que Swagger UI cargue sin token, `SecurityConfig` debe permitir esas rutas (ver «Requisito en `SecurityConfig`»).

## Piezas compartidas

| Clase | Para qué sirve |
| --- | --- |
| `config/OpenApiConfig` | Datos generales de la API y el esquema de seguridad `bearerAuth`. |
| `common/openapi/RespuestaErrorDoc` | Esquema del cuerpo de error (`timestamp`, `status`, `error`, `mensaje`, `detalles`). Solo documenta: el cuerpo real lo arma `ErrorResponseBuilder`. |
| `common/openapi/EjemplosError` | Ejemplos de los errores transversales: validación (400), parámetro inválido (400), 401 y 403. |

## Patrón para un controlador

1. **Clase:** `@Tag(name, description)` (con el RF y el rol requerido) y `@SecurityRequirement(name = OpenApiConfig.ESQUEMA_BEARER)`.
2. **Cada método:** `@Operation(summary, description)` con las reglas de negocio, y `@ApiResponses` con los códigos de la tabla de abajo.
3. **Parámetros de ruta y de consulta:** `@Parameter(description, example)`.
4. **DTO:** `@Schema(description, example)` en cada campo. Las restricciones de Jakarta Validation (`@NotBlank`, `@Pattern`...) se reflejan solas.
5. **Ejemplos de error propios del módulo:** constantes `private static final String` en el controlador, con el mismo mensaje que produce la excepción y `EjemplosError.TIMESTAMP_EJEMPLO` como fecha. Deben ser constantes de compilación (las anotaciones no aceptan valores calculados).

```java
@ApiResponse(responseCode = "401", description = "Falta el token o es inválido o expiró",
        content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = RespuestaErrorDoc.class),
                examples = @ExampleObject(value = EjemplosError.NO_AUTENTICADO_401)))
```

Si un mismo código tiene varias causas (por ejemplo, un 409 por documento repetido o por correo repetido), usa varios `@ExampleObject` con `name` y `summary`.

## Qué códigos documentar

Se documentan los códigos que el endpoint **realmente puede devolver**; no se agrega un 400 o un 404 a un endpoint que no los produce.

| Código | Cuándo se documenta |
| --- | --- |
| 200 / 201 / 204 | El de éxito del endpoint. |
| 400 | Recibe cuerpo o parámetros que pueden ser inválidos (validación, UUID o enum con formato inválido) o una regla de negocio responde 400. |
| 401 | Todo endpoint protegido. |
| 403 | Todo endpoint con `@PreAuthorize`. |
| 404 | Busca por id o referencia otra entidad que puede no existir. |
| 409 | Puede chocar con una restricción de unicidad. |

## Verificación automática

`EstudianteApiDocumentacionIT` lee `/v3/api-docs` y comprueba que exista el esquema `bearerAuth` y que cada endpoint de Estudiantes exija Bearer y documente sus códigos. Para otro módulo, copia la clase y cambia las rutas y los códigos esperados. Es una prueba de integración: se ejecuta con `./gradlew.bat integrationTest --tests "*ApiDocumentacionIT*"`.

## Requisito en `SecurityConfig`

La cadena de seguridad solo deja pasar sin token el login, `OPTIONS` y el health. Para abrir Swagger UI hay que agregar:

```java
.requestMatchers(HttpMethod.GET, "/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs", "/v3/api-docs/**").permitAll()
```

Esto expone la documentación de la API sin autenticación. Está bien para desarrollo; en un despliegue real se desactiva con `springdoc.api-docs.enabled=false` y `springdoc.swagger-ui.enabled=false` en el perfil de producción.