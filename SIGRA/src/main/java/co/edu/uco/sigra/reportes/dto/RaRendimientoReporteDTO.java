package co.edu.uco.sigra.reportes.dto;

import co.edu.uco.sigra.estadisticas.dto.DistribucionNivelLogroDTO;
import co.edu.uco.sigra.estadisticas.entity.NivelLogro;
import co.edu.uco.sigra.resultadosaprendizaje.dto.EstadoRendimiento;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.UUID;

@Schema(name = "RaRendimientoReporte", description = "Rendimiento de un resultado de aprendizaje dentro del reporte R1")
public record RaRendimientoReporteDTO(
        @Schema(description = "Identificador del RA", example = "1a2b3c4d-5e6f-4a7b-8c9d-0e1f2a3b4c5d")
        UUID resultadoAprendizajeId,
        @Schema(description = "Código del RA", example = "RA-01")
        String codigo,
        @Schema(description = "Descripción del RA")
        String descripcion,
        @Schema(description = "Cantidad de calificaciones registradas en el RA y el semestre", example = "12")
        long cantidadCalificaciones,
        @Schema(description = "SIN_EVALUAR (sin calificaciones), DATOS_INSUFICIENTES (nadie llega a 3 calificaciones) o CON_NIVEL")
        EstadoRendimiento estado,
        @Schema(description = "Nivel de logro del RA. Nulo si el estado no es CON_NIVEL", example = "ALTO", nullable = true)
        NivelLogro nivelLogro,
        @Schema(description = "Promedio (0.0 - 5.0) de los estudiantes evaluados. Nulo si no hay evaluados", example = "4.05", nullable = true)
        Double promedioNota,
        @Schema(description = "Estudiantes con nivel calculado (3 o más calificaciones)", example = "20")
        long evaluados,
        @Schema(description = "Estudiantes con 1 o 2 calificaciones", example = "3")
        long sinDatosSuficientes,
        @Schema(description = "Estudiantes sin ninguna calificación en el RA", example = "2")
        long sinEvaluar,
        @Schema(description = "Los 4 niveles en orden (BAJO, MEDIO, ALTO, SOBRESALIENTE), con cantidad y porcentaje sobre los evaluados")
        List<DistribucionNivelLogroDTO> distribucion,
        @Schema(description = "Aviso para el usuario: «Sin evaluar» o «Sin datos suficientes para calcular». Nulo si hay nivel", nullable = true)
        String mensaje
) {
}
