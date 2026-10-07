package co.edu.uco.sigra.resultadosaprendizaje.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import static co.edu.uco.sigra.resultadosaprendizaje.dto.ReglasEntradaResultadoAprendizaje.*;

/**
 * Datos de entrada para modificar un resultado de aprendizaje (RF-06c).
 * Solo la descripción es modificable; el código y la asignatura no cambian una vez creados.
 * La descripción se normaliza aquí, antes de validarse (ver {@link ReglasEntradaResultadoAprendizaje}).
 */
public record ResultadoAprendizajeActualizarRequestDTO(

        @NotBlank(message = DESCRIPCION_OBLIGATORIA)
        @Size(max = DESCRIPCION_MAX, message = DESCRIPCION_LONGITUD)
        @Pattern(regexp = PATRON_DESCRIPCION, message = DESCRIPCION_CARACTERES)
        String descripcion
) {
    public ResultadoAprendizajeActualizarRequestDTO {
        descripcion = normalizarDescripcion(descripcion);
    }
}
