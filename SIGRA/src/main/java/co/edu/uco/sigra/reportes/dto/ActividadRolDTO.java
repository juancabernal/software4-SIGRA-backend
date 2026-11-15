package co.edu.uco.sigra.reportes.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

/** Actividad agregada de un tipo de usuario; nunca identifica a las personas. */
@Schema(name = "ActividadRol")
public record ActividadRolDTO(
        @Schema(description = "ADMINISTRADOR, PROFESOR, ESTUDIANTE o SIN_IDENTIFICAR (intentos de acceso rechazados)")
        String rol,
        long totalEventos,
        long usuariosDistintos,
        LocalDateTime ultimaActividad,
        List<ConteoAccionDTO> acciones
) {
}
