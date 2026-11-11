package co.edu.uco.sigra.reportes.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.UUID;

@Schema(name = "EstudianteRiesgo",
        description = "Estudiante con RA no alcanzados en una asignatura del semestre")
public record EstudianteRiesgoDTO(
        @Schema(description = "Identificador del estudiante")
        UUID estudianteId,
        @Schema(description = "Nombre para mostrar", example = "Mariana Salazar Duque")
        String nombreCompleto,
        @Schema(description = "Primer nombre")
        String primerNombre,
        @Schema(description = "Segundo nombre", nullable = true)
        String segundoNombre,
        @Schema(description = "Primer apellido")
        String primerApellido,
        @Schema(description = "Segundo apellido")
        String segundoApellido,
        @Schema(description = "Correo institucional", example = "mariana.salazar@uco.net.co")
        String correoInstitucional,
        @Schema(description = "Número de documento", example = "10458921")
        String numeroDocumento,
        @Schema(description = "Identificador de la asignatura")
        UUID asignaturaId,
        @Schema(description = "Código de la asignatura", example = "QA26-PROG1")
        String asignaturaCodigo,
        @Schema(description = "Nombre de la asignatura")
        String asignaturaNombre,
        @Schema(description = "Nombre del programa académico")
        String programaNombre,
        @Schema(description = "Cantidad de RA con nivel BAJO o MEDIO")
        int cantidadRaNoAlcanzados,
        @Schema(description = "Severidad según la cantidad de RA no alcanzados")
        SeveridadRiesgo severidad,
        @Schema(description = "Detalle de los RA no alcanzados, ordenados por código")
        List<RaNoAlcanzadoDTO> ras
) {
}
