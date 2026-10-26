package co.edu.uco.sigra.estudiantes.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Respuesta del alta de un estudiante y su matrícula en un solo paso (RF-09a).
 * <p>
 * Reutiliza {@link EstudianteResponseDTO} y {@link MatriculaResponseDTO} tal cual existen: el
 * estudiante creado y su matrícula con identificador, fecha y estado ya los cubre cada uno sin
 * cambios.
 */
public record EstudianteConMatriculaResponseDTO(
        @Schema(description = "Estudiante recién registrado")
        EstudianteResponseDTO estudiante,

        @Schema(description = "Matrícula creada para el estudiante")
        MatriculaResponseDTO matricula
) {
}
