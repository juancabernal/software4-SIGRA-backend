package co.edu.uco.sigra.asignaturas.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.UUID;

import static co.edu.uco.sigra.asignaturas.dto.ReglasEntrada.*;

/** El código y el nombre se normalizan aquí, antes de validarse (ver {@link ReglasEntrada}). */
public record AsignaturaRequestDTO(
        @NotBlank(message = CODIGO_OBLIGATORIO)
        @Size(min = CODIGO_MIN, max = CODIGO_MAX, message = CODIGO_LONGITUD)
        @Pattern(regexp = PATRON_CODIGO, message = CODIGO_FORMATO)
        String codigo,

        @NotBlank(message = NOMBRE_OBLIGATORIO)
        @Size(min = NOMBRE_MIN, max = NOMBRE_MAX, message = NOMBRE_LONGITUD)
        @Pattern(regexp = PATRON_NOMBRE, message = NOMBRE_CARACTERES)
        String nombre,

        @NotNull(message = "Debe seleccionar un programa académico")
        UUID programaId
) {
    public AsignaturaRequestDTO {
        codigo = normalizarCodigo(codigo);
        nombre = normalizarNombre(nombre);
    }
}
