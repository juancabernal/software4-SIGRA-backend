package co.edu.uco.sigra.resultadosaprendizaje.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import static co.edu.uco.sigra.resultadosaprendizaje.dto.ReglasEntradaResultadoAprendizaje.*;


@Schema(name = "ResultadoAprendizajeActualizarRequest", description = "Datos para modificar un RA: solo se puede cambiar la descripción")
public record ResultadoAprendizajeActualizarRequestDTO(

        @Schema(description = "Nueva descripción (de 10 a 100 caracteres, sin < ni >)", example = "Aplica los conceptos básicos de programación orientada a objetos")
        @NotBlank(message = DESCRIPCION_OBLIGATORIA)
        @Size(min = DESCRIPCION_MIN, max = DESCRIPCION_MAX, message = DESCRIPCION_LONGITUD)
        @Pattern(regexp = PATRON_DESCRIPCION, message = DESCRIPCION_CARACTERES)
        String descripcion
) {
    public ResultadoAprendizajeActualizarRequestDTO {
        descripcion = normalizarDescripcion(descripcion);
    }
}
