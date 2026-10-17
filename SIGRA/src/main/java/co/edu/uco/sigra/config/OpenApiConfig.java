package co.edu.uco.sigra.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

/**
 * Metadatos generales de la documentación OpenAPI y esquema de seguridad Bearer JWT.
 *
 * <p>Cada controlador protegido se marca con
 * {@code @SecurityRequirement(name = OpenApiConfig.ESQUEMA_BEARER)}: Swagger UI muestra entonces el
 * botón «Authorize» y envía el token en el encabezado {@code Authorization: Bearer <token>}.
 * El token se obtiene con {@code POST /api/v1/auth/login}.
 */
@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "SIGRA API",
                version = "v1",
                description = "API REST del Sistema de Gestión de Resultados de Aprendizaje (SIGRA). "
                        + "Los errores usan un cuerpo estándar con timestamp, status, error, mensaje y, "
                        + "en validaciones, detalles."
        )
)
@SecurityScheme(
        name = OpenApiConfig.ESQUEMA_BEARER,
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT",
        description = "Token JWT obtenido en POST /api/v1/auth/login. Pegue solo el token, sin el prefijo «Bearer»."
)
public class OpenApiConfig {

    /** Nombre del esquema de seguridad; se referencia desde {@code @SecurityRequirement}. */
    public static final String ESQUEMA_BEARER = "bearerAuth";
}