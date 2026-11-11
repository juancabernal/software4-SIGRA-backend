package co.edu.uco.sigra.programas.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/**
 * Datos modificables de un programa: el nombre y la facultad. El código es inmutable para preservar
 * trazabilidad (SRS, RF-02). La facultad es opcional: si se omite (o es la misma), se conserva la actual.
 */
@Schema(name = "ProgramaUpdate", description = "Datos para modificar el nombre y, opcionalmente, la facultad de un programa académico")
public record ProgramaUpdateDTO(
        @Schema(description = "Nuevo nombre del programa. No puede repetirse (sin distinguir mayúsculas)",
                example = "Ingeniería de Sistemas y Computación")
        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 100, message = "El nombre no puede superar los 100 caracteres")
        String nombre,

        @Schema(description = "Identificador (UUID) de la nueva facultad. Opcional: si se omite o es la misma, "
                + "el programa conserva su facultad. Si cambia, la nueva facultad debe existir y estar activa",
                example = "7c8d9e0f-1a2b-4c3d-8e4f-5a6b7c8d9e0f", nullable = true)
        UUID facultadId
) {

    /** Solo cambia el nombre y conserva la facultad actual. */
    public ProgramaUpdateDTO(String nombre) {
        this(nombre, null);
    }
}
