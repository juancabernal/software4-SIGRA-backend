package co.edu.uco.sigra.auditoria.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.UUID;

@Schema(name = "RegistroAuditoria", description = "Un evento del historial de auditoría")
public record RegistroAuditoriaDTO(
        UUID id,
        @Schema(description = "Fecha y hora exacta del evento (hora de Colombia)", example = "2026-10-20T14:35:12.123")
        LocalDateTime fechaHora,
        @Schema(description = "Acción registrada", example = "CAMBIO_ESTADO")
        String accion,
        @Schema(description = "Tipo de entidad afectada", example = "ASIGNATURA")
        String tipoEntidad,
        @Schema(description = "Id de la entidad afectada; nulo en un login rechazado")
        UUID idEntidad,
        @Schema(description = "Usuario que hizo la acción; nulo si no había sesión")
        UUID usuarioId,
        @Schema(description = "Rol del usuario que hizo la acción", example = "ADMINISTRADOR")
        String rolUsuario,
        @Schema(description = "Detalle del evento", example = "Cambio de estado: inactivar (nuevo estado: INACTIVA)")
        String detalle
) {}