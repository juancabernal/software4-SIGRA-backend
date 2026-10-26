package co.edu.uco.sigra.estadisticas.dto;

import co.edu.uco.sigra.estadisticas.entity.NivelLogro;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Un rango del catálogo de niveles de logro: desde {@code notaMinima} (incluida) hasta
 * {@code notaLimite} (incluida solo si {@code limiteIncluido}; únicamente en Sobresaliente, con 5.0).
 */
@Schema(name = "RangoNivelLogroResponse", description = "Rango del catálogo de niveles de logro (escala 0.0 – 5.0)")
public record RangoNivelLogroResponseDTO(
        @Schema(description = "Nivel de logro", example = "MEDIO")
        NivelLogro nivel,

        @Schema(description = "Nota mínima del rango (incluida)", example = "3.0")
        double notaMinima,

        @Schema(description = "Nota límite del rango; se incluye solo si limiteIncluido es verdadero", example = "4.0")
        double notaLimite,

        @Schema(description = "Verdadero si notaLimite pertenece al rango (solo Sobresaliente, con 5.0)", example = "false")
        boolean limiteIncluido
) {
}
