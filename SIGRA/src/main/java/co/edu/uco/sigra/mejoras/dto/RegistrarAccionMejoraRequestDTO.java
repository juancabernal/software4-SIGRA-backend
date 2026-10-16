package co.edu.uco.sigra.mejoras.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Datos para registrar una acción de mejora sobre una conclusión VIGENTE (RF-17). */
public record RegistrarAccionMejoraRequestDTO(

        @NotBlank(message = "El nombre de la acción de mejora es obligatorio")
        @Size(max = 150, message = "El nombre no puede superar los 150 caracteres")
        String nombre,

        @NotBlank(message = "La descripción de la acción de mejora es obligatoria")
        @Size(max = 500, message = "La descripción no puede superar los 500 caracteres")
        String descripcion
) {
}
