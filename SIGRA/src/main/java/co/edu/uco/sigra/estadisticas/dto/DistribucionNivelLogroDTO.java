package co.edu.uco.sigra.estadisticas.dto;

import co.edu.uco.sigra.estadisticas.entity.NivelLogro;
import io.swagger.v3.oas.annotations.media.Schema;

/** Cuántos estudiantes evaluados quedaron en un nivel y qué porcentaje representan de los evaluados. */
@Schema(name = "DistribucionNivelLogro", description = "Estudiantes evaluados que quedaron en un nivel de logro")
public record DistribucionNivelLogroDTO(
        @Schema(description = "Nivel de logro", example = "ALTO")
        NivelLogro nivel,

        @Schema(description = "Cantidad de estudiantes evaluados en este nivel", example = "8")
        long cantidad,

        @Schema(description = "Porcentaje sobre los evaluados (0 – 100, dos decimales)", example = "40.0")
        double porcentaje
) {
}
