package co.edu.uco.sigra.reportes.dto;

import co.edu.uco.sigra.estadisticas.entity.NivelLogro;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(name = "RaNoAlcanzado", description = "RA con nivel insuficiente (BAJO o MEDIO) para el estudiante")
public record RaNoAlcanzadoDTO(
        @Schema(description = "Identificador del resultado de aprendizaje")
        UUID resultadoAprendizajeId,
        @Schema(description = "Código del RA", example = "RA-01")
        String codigo,
        @Schema(description = "Descripción del RA")
        String descripcion,
        @Schema(description = "Nivel de logro insuficiente", allowableValues = {"BAJO", "MEDIO"})
        NivelLogro nivelLogro,
        @Schema(description = "Promedio ponderado del estudiante en el RA (0.0–5.0)", nullable = true)
        Double promedio
) {
}
