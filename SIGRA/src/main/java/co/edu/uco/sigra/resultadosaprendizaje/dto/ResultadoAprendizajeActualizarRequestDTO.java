package co.edu.uco.sigra.resultadosaprendizaje.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import static co.edu.uco.sigra.resultadosaprendizaje.dto.ReglasEntradaResultadoAprendizaje.*;


public record ResultadoAprendizajeActualizarRequestDTO(

        @NotBlank(message = DESCRIPCION_OBLIGATORIA)
        @Size(min = DESCRIPCION_MIN, max = DESCRIPCION_MAX, message = DESCRIPCION_LONGITUD)
        @Pattern(regexp = PATRON_DESCRIPCION, message = DESCRIPCION_CARACTERES)
        String descripcion
) {
    public ResultadoAprendizajeActualizarRequestDTO {
        descripcion = normalizarDescripcion(descripcion);
    }
}
