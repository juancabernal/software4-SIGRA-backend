package co.edu.uco.sigra.asignaturas.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import static co.edu.uco.sigra.asignaturas.dto.ReglasEntrada.*;

/**
 * Única modificación permitida sobre una asignatura: su nombre (código y programa son inmutables).
 * El nombre se normaliza antes de validarse, con las mismas reglas que al registrar.
 */
public record AsignaturaUpdateDTO(
        @NotBlank(message = NOMBRE_OBLIGATORIO)
        @Size(min = NOMBRE_MIN, max = NOMBRE_MAX, message = NOMBRE_LONGITUD)
        @Pattern(regexp = PATRON_NOMBRE, message = NOMBRE_CARACTERES)
        String nombre
) {
    public AsignaturaUpdateDTO {
        nombre = normalizarNombre(nombre);
    }
}
