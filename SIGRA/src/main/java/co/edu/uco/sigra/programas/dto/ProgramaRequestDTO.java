package co.edu.uco.sigra.programas.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(name = "ProgramaRequest", description = "Datos para registrar un programa académico")
public record ProgramaRequestDTO(
        @Schema(description = "Nombre del programa. No puede repetirse (sin distinguir mayúsculas)",
                example = "Ingeniería de Sistemas")
        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 100, message = "El nombre no puede superar los 100 caracteres")
        String nombre,

        @Schema(description = "Código del programa: letras, números y guion, de 4 a 10 caracteres. "
                + "No puede repetirse (sin distinguir mayúsculas) y es inmutable una vez creado",
                example = "ING-SIS")
        @NotBlank(message = "El código es obligatorio")
        @Size(min = 4, max = 10, message = "El código debe tener entre 4 y 10 caracteres")
        @Pattern(regexp = "^[A-Za-z0-9-]+$", message = "El código solo puede tener letras, números y guion")
        String codigo
) {}
