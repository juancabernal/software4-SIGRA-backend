package co.edu.uco.sigra.programas.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ProgramaRequestDTO(
        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 100, message = "El nombre no puede superar los 100 caracteres")
        String nombre,

        @NotBlank(message = "El código es obligatorio")
        @Size(min = 4, max = 10, message = "El código debe tener entre 4 y 10 caracteres")
        @Pattern(regexp = "^[A-Za-z0-9-]+$", message = "El código solo puede tener letras, números y guion")
        String codigo
) {}
