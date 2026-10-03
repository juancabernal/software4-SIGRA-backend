package co.edu.uco.sigra.asignaturas.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record AsignaturaRequestDTO(
        @NotBlank(message = "El código es obligatorio")
        @Size(max = 20, message = "El código no puede superar los 20 caracteres")
        @Pattern(regexp = "^[A-Za-z0-9-]+$", message = "El código solo puede contener letras, números y guion")
        String codigo,

        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 100, message = "El nombre no puede superar los 100 caracteres")
        String nombre,

        @NotNull(message = "Debe seleccionar un programa académico")
        UUID programaId
) {}
