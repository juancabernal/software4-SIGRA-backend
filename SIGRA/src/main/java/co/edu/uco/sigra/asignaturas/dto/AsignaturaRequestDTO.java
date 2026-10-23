package co.edu.uco.sigra.asignaturas.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.UUID;

import static co.edu.uco.sigra.asignaturas.dto.ReglasEntrada.*;

/** El código y el nombre se normalizan aquí, antes de validarse (ver {@link ReglasEntrada}). */
@Schema(name = "AsignaturaRequest", description = "Datos para registrar una asignatura")
public record AsignaturaRequestDTO(
        @Schema(description = "Código único de la asignatura: letras sin tilde, números y guiones entre ellos. Se guarda en mayúsculas",
                example = "MAT-401")
        @NotBlank(message = CODIGO_OBLIGATORIO)
        @Size(min = CODIGO_MIN, max = CODIGO_MAX, message = CODIGO_LONGITUD)
        @Pattern(regexp = PATRON_CODIGO, message = CODIGO_FORMATO)
        String codigo,

        @Schema(description = "Nombre de la asignatura", example = "Cálculo Diferencial")
        @NotBlank(message = NOMBRE_OBLIGATORIO)
        @Size(min = NOMBRE_MIN, max = NOMBRE_MAX, message = NOMBRE_LONGITUD)
        @Pattern(regexp = PATRON_NOMBRE, message = NOMBRE_CARACTERES)
        String nombre,

        @Schema(description = "Identificador (UUID) del programa académico al que pertenece la asignatura. Debe estar ACTIVO",
                example = "4d5e6f7a-8b9c-4d0e-1f2a-3b4c5d6e7f8a")
        @NotNull(message = "Debe seleccionar un programa académico")
        UUID programaId
) {
    public AsignaturaRequestDTO {
        codigo = normalizarCodigo(codigo);
        nombre = normalizarNombre(nombre);
    }
}
