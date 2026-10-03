package co.edu.uco.sigra.resultadosaprendizaje.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Datos de entrada para registrar un resultado de aprendizaje (RF-06a).
 * La asignatura no viaja aquí: llega en la URL (/asignaturas/{asignaturaId}/resultados-aprendizaje).
 * Longitudes según el MR: código hasta 10 caracteres y descripción hasta 500.
 */
public record ResultadoAprendizajeCrearRequestDTO(

        @NotBlank(message = "El código es obligatorio")
        @Size(max = 10, message = "El código debe tener máximo 10 caracteres")
        @Pattern(regexp = "^\\s*\\S+\\s*$", message = "El código no puede contener espacios internos")
        String codigo,

        @NotBlank(message = "La descripción es obligatoria")
        @Size(max = 500, message = "La descripción debe tener máximo 500 caracteres")
        String descripcion
) {
}
