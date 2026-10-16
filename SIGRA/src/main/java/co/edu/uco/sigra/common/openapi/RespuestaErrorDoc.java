package co.edu.uco.sigra.common.openapi;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Esquema del cuerpo de error que devuelve la API. Existe solo para documentar (Swagger): el cuerpo
 * real lo arman {@code ErrorResponseBuilder} y {@code JsonSecurityResponse}.
 */
@Schema(name = "RespuestaError", description = "Cuerpo estándar de todos los errores de la API")
public record RespuestaErrorDoc(

        @Schema(description = "Momento del error, en ISO-8601 con zona horaria",
                example = "2026-10-14T12:00:00-05:00")
        OffsetDateTime timestamp,

        @Schema(description = "Código de estado HTTP", example = "400")
        int status,

        @Schema(description = "Nombre del estado HTTP", example = "BAD_REQUEST")
        String error,

        @Schema(description = "Mensaje en español, sin detalles técnicos",
                example = "Los datos de entrada no cumplen con las validaciones requeridas")
        String mensaje,

        @Schema(description = "Un texto «campo: mensaje» por cada validación fallida. Solo aparece en errores de validación (400)")
        List<String> detalles
) {
}
