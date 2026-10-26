package co.edu.uco.sigra.programas.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Solo el nombre es modificable; el código es inmutable para preservar trazabilidad (SRS, RF-02). */
@Schema(name = "ProgramaUpdate", description = "Datos para modificar el nombre de un programa académico")
public record ProgramaUpdateDTO(
        @Schema(description = "Nuevo nombre del programa. No puede repetirse (sin distinguir mayúsculas)",
                example = "Ingeniería de Sistemas y Computación")
        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 100, message = "El nombre no puede superar los 100 caracteres")
        String nombre
) {}
