package co.edu.uco.sigra.programas.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Solo el nombre es modificable; el código es inmutable para preservar trazabilidad (SRS, RF-02). */
public record ProgramaUpdateDTO(
        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 100, message = "El nombre no puede superar los 100 caracteres")
        String nombre
) {}
