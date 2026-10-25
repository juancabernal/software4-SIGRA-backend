package co.edu.uco.sigra.evaluaciones.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Datos de salida de una evaluación. {@code tieneCalificaciones} le indica al frontend si la fecha
 * tentativa y el porcentaje están bloqueados (RF-10b).
 */
@Schema(name = "EvaluacionResponse", description = "Evaluación de un resultado de aprendizaje en un semestre")
public record EvaluacionResponseDTO(
        @Schema(description = "Identificador de la evaluación", example = "3f2b8c1d-6a4e-4b9f-a7d5-1c0e9f8a7b6d")
        UUID id,
        @Schema(description = "Identificador del RA evaluado", example = "1a2b3c4d-5e6f-4a7b-8c9d-0e1f2a3b4c5d")
        UUID resultadoAprendizajeId,
        @Schema(description = "Identificador del semestre de la evaluación", example = "9d8c7b6a-5f4e-4d3c-b2a1-0f9e8d7c6b5a")
        UUID semestreId,
        @Schema(description = "Código del semestre", example = "2026-2")
        String semestreCodigo,
        @Schema(description = "Nombre de la evaluación", example = "Parcial 1")
        String nombre,
        @Schema(description = "Fecha tentativa (AAAA-MM-DD)", example = "2026-11-20")
        LocalDate fechaTentativa,
        @Schema(description = "Porcentaje de la evaluación", example = "30")
        BigDecimal porcentaje,
        @Schema(description = "Descripción de la evaluación", example = "Examen escrito sobre los temas del primer corte")
        String descripcion,
        @Schema(description = "ACTIVA o INACTIVA (eliminación lógica)", example = "ACTIVA", allowableValues = {"ACTIVA", "INACTIVA"})
        String estado,
        @Schema(description = "true si ya tiene calificaciones registradas: entonces la fecha y el porcentaje están bloqueados (RF-10b)", example = "false")
        boolean tieneCalificaciones
) {
}
