package co.edu.uco.sigra.resultadosaprendizaje.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import static co.edu.uco.sigra.resultadosaprendizaje.dto.ReglasEntradaResultadoAprendizaje.*;


@Schema(name = "ResultadoAprendizajeCrearRequest", description = "Datos para registrar un resultado de aprendizaje en una asignatura")
public record ResultadoAprendizajeCrearRequestDTO(

        @Schema(description = "Código del RA, único dentro de la asignatura. Se guarda en mayúsculas; admite letras sin tilde, números y guiones entre ellos (máximo 10 caracteres)", example = "RA-01")
        @NotBlank(message = CODIGO_OBLIGATORIO)
        @Size(max = CODIGO_MAX, message = CODIGO_LONGITUD)
        @Pattern(regexp = PATRON_CODIGO, message = CODIGO_FORMATO)
        String codigo,

        @Schema(description = "Descripción del resultado de aprendizaje (de 10 a 100 caracteres, sin < ni >)", example = "Aplica los conceptos básicos de programación orientada a objetos")
        @NotBlank(message = DESCRIPCION_OBLIGATORIA)
        @Size(min = DESCRIPCION_MIN, max = DESCRIPCION_MAX, message = DESCRIPCION_LONGITUD)
        @Pattern(regexp = PATRON_DESCRIPCION, message = DESCRIPCION_CARACTERES)
        String descripcion
) {
    public ResultadoAprendizajeCrearRequestDTO {
        codigo = normalizarCodigo(codigo);
        descripcion = normalizarDescripcion(descripcion);
    }
}
