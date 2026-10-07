package co.edu.uco.sigra.resultadosaprendizaje.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import static co.edu.uco.sigra.resultadosaprendizaje.dto.ReglasEntradaResultadoAprendizaje.*;

/**
 * Datos de entrada para registrar un resultado de aprendizaje (RF-06a).
 * La asignatura no viaja aquí: llega en la URL (/asignaturas/{asignaturaId}/resultados-aprendizaje).
 * El código y la descripción se normalizan aquí, antes de validarse (ver {@link ReglasEntradaResultadoAprendizaje}).
 */
public record ResultadoAprendizajeCrearRequestDTO(

        @NotBlank(message = CODIGO_OBLIGATORIO)
        @Size(max = CODIGO_MAX, message = CODIGO_LONGITUD)
        @Pattern(regexp = PATRON_CODIGO, message = CODIGO_FORMATO)
        String codigo,

        @NotBlank(message = DESCRIPCION_OBLIGATORIA)
        @Size(max = DESCRIPCION_MAX, message = DESCRIPCION_LONGITUD)
        @Pattern(regexp = PATRON_DESCRIPCION, message = DESCRIPCION_CARACTERES)
        String descripcion
) {
    public ResultadoAprendizajeCrearRequestDTO {
        codigo = normalizarCodigo(codigo);
        descripcion = normalizarDescripcion(descripcion);
    }
}
