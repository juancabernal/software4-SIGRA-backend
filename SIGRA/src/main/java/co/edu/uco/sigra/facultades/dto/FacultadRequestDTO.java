package co.edu.uco.sigra.facultades.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(name = "FacultadRequest", description = "Datos para registrar o modificar una facultad")
public record FacultadRequestDTO(
        @Schema(description = "Nombre de la facultad. No puede repetirse (sin distinguir mayúsculas)",
                example = "Facultad de Ingeniería")
        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 150, message = "El nombre no puede superar los 150 caracteres")
        String nombre
) {}
