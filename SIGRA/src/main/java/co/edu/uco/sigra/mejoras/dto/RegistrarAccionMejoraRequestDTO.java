package co.edu.uco.sigra.mejoras.dto;

import co.edu.uco.sigra.evaluaciones.dto.ReglasEntradaEvaluacion;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Datos para registrar una acción de mejora sobre una conclusión VIGENTE (RF-17). */
@Schema(name = "RegistrarAccionMejoraRequest", description = "Datos para registrar una acción de mejora sobre una conclusión VIGENTE")
public record RegistrarAccionMejoraRequestDTO(

        @Schema(description = "Nombre de la acción de mejora; se hereda como nombre de la evaluación si se convierte",
                example = "Taller de refuerzo de derivadas")
        @NotBlank(message = ReglasEntradaEvaluacion.NOMBRE_OBLIGATORIO)
        @Size(max = ReglasEntradaEvaluacion.NOMBRE_MAX, message = ReglasEntradaEvaluacion.NOMBRE_LONGITUD)
        @Pattern(regexp = ReglasEntradaEvaluacion.PATRON_TEXTO, message = ReglasEntradaEvaluacion.NOMBRE_CARACTERES)
        String nombre,

        @Schema(description = "Descripción de la acción de mejora propuesta", example = "Sesiones de práctica dirigida sobre reglas de derivación")
        @NotBlank(message = ReglasEntradaEvaluacion.DESCRIPCION_OBLIGATORIA)
        @Size(max = ReglasEntradaEvaluacion.DESCRIPCION_MAX, message = ReglasEntradaEvaluacion.DESCRIPCION_LONGITUD)
        @Pattern(regexp = ReglasEntradaEvaluacion.PATRON_TEXTO, message = ReglasEntradaEvaluacion.DESCRIPCION_CARACTERES)
        String descripcion
) {
}
