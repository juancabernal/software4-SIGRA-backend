package co.edu.uco.sigra.mejoras.dto;

import co.edu.uco.sigra.evaluaciones.dto.ReglasEntradaEvaluacion;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Datos para registrar una acción de mejora sobre una conclusión VIGENTE (RF-17). */
public record RegistrarAccionMejoraRequestDTO(

        @NotBlank(message = ReglasEntradaEvaluacion.NOMBRE_OBLIGATORIO)
        @Size(max = ReglasEntradaEvaluacion.NOMBRE_MAX, message = ReglasEntradaEvaluacion.NOMBRE_LONGITUD)
        @Pattern(regexp = ReglasEntradaEvaluacion.PATRON_TEXTO, message = ReglasEntradaEvaluacion.NOMBRE_CARACTERES)
        String nombre,

        @NotBlank(message = ReglasEntradaEvaluacion.DESCRIPCION_OBLIGATORIA)
        @Size(max = ReglasEntradaEvaluacion.DESCRIPCION_MAX, message = ReglasEntradaEvaluacion.DESCRIPCION_LONGITUD)
        @Pattern(regexp = ReglasEntradaEvaluacion.PATRON_TEXTO, message = ReglasEntradaEvaluacion.DESCRIPCION_CARACTERES)
        String descripcion
) {
}
