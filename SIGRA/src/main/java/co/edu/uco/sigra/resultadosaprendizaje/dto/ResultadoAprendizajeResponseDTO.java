package co.edu.uco.sigra.resultadosaprendizaje.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;


@Schema(name = "ResultadoAprendizajeResponse", description = "Resultado de aprendizaje")
public record ResultadoAprendizajeResponseDTO(
        @Schema(description = "Identificador del RA", example = "1a2b3c4d-5e6f-4a7b-8c9d-0e1f2a3b4c5d")
        UUID id,
        @Schema(description = "Identificador de la asignatura a la que pertenece", example = "7c9e6679-7425-40de-944b-e07fc1f90ae7")
        UUID asignaturaId,
        @Schema(description = "Código del RA, único dentro de la asignatura", example = "RA-01")
        String codigo,
        @Schema(description = "Descripción del RA", example = "Aplica los conceptos básicos de programación orientada a objetos")
        String descripcion,
        @Schema(description = "ACTIVO o INACTIVO (eliminación lógica)", example = "ACTIVO", allowableValues = {"ACTIVO", "INACTIVO"})
        String estado
) {
}
