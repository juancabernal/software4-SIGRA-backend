package co.edu.uco.sigra.resultadosaprendizaje.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Datos de entrada para modificar un resultado de aprendizaje (RF-06c).
 * Solo la descripción es modificable; el código y la asignatura no cambian una vez creados.
 */
public record ResultadoAprendizajeActualizarRequestDTO(

        @NotBlank(message = "La descripción es obligatoria")
        @Size(max = 500, message = "La descripción debe tener máximo 500 caracteres")
        String descripcion
) {
}
