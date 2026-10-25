package co.edu.uco.sigra.semestres.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.util.UUID;

@Schema(name = "SemestreResponse", description = "Semestre registrado")
public record SemestreResponseDTO(
        @Schema(description = "Identificador (UUID) del semestre", example = "6b1f9d7e-5c3a-4f21-9e8b-2d4a7c6e1f05")
        UUID id,

        @Schema(description = "Código del semestre", example = "2027-1")
        String codigo,

        @Schema(description = "Fecha de inicio", example = "2027-01-25")
        LocalDate fechaInicio,

        @Schema(description = "Fecha de fin", example = "2027-06-05")
        LocalDate fechaFin,

        @Schema(description = "Estado calculado por fechas: ACTIVO si hoy está dentro del rango, INACTIVO si aún no empieza o ya terminó",
                allowableValues = {"ACTIVO", "INACTIVO"}, example = "INACTIVO")
        String estado
) {}
