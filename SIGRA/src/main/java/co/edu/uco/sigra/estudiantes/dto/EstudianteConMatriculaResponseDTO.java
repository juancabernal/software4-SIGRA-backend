package co.edu.uco.sigra.estudiantes.dto;

/**
 * Respuesta del alta de un estudiante y su matrícula en un solo paso (RF-09a).
 * <p>
 * Reutiliza {@link EstudianteResponseDTO} y {@link MatriculaResponseDTO} tal cual existen: el
 * estudiante creado y su matrícula con identificador, fecha y estado ya los cubre cada uno sin
 * cambios.
 */
public record EstudianteConMatriculaResponseDTO(
        EstudianteResponseDTO estudiante,
        MatriculaResponseDTO matricula
) {
}
