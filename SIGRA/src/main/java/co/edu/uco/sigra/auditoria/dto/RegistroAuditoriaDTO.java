package co.edu.uco.sigra.auditoria.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.UUID;

@Schema(name = "RegistroAuditoria", description = "Un evento del historial de auditoría")
public record RegistroAuditoriaDTO(
        @Schema(description = "Identificador único del registro de auditoría.", example = "4b64b64b-4b64-4b64-8b64-4b64b64b64b6",
                requiredMode = Schema.RequiredMode.REQUIRED)
        UUID id,
        @Schema(description = "Fecha y hora exacta del evento (hora de Colombia)", example = "2026-10-20T14:35:12.123",
                requiredMode = Schema.RequiredMode.REQUIRED)
        LocalDateTime fechaHora,
        @Schema(description = "Acción registrada. Valores actuales: LOGIN_EXITOSO, LOGIN_FALLIDO, CAMBIO_ESTADO, "
                + "CALIFICACION_FUERA_DE_FECHA y CALIFICACION_MODIFICADA.", example = "LOGIN_EXITOSO",
                allowableValues = {"LOGIN_EXITOSO", "LOGIN_FALLIDO", "CAMBIO_ESTADO", "CALIFICACION_FUERA_DE_FECHA", "CALIFICACION_MODIFICADA"},
                requiredMode = Schema.RequiredMode.REQUIRED)
        String accion,
        @Schema(description = "Tipo de entidad afectada. En un filtro USUARIO se incluyen todos los eventos de usuario, no solo los logins.",
                example = "USUARIO", requiredMode = Schema.RequiredMode.REQUIRED)
        String tipoEntidad,
        @Schema(description = "Identificador de la entidad afectada. Puede ser nulo en un LOGIN_FALLIDO.",
                example = "4b64b64b-4b64-4b64-8b64-4b64b64b64b6", nullable = true)
        UUID idEntidad,
        @Schema(description = "Identificador del usuario que hizo la acción. Puede ser nulo en un intento de login rechazado.",
                example = "4b64b64b-4b64-4b64-8b64-4b64b64b64b6", nullable = true)
        UUID usuarioId,
        @Schema(description = "Rol del usuario que hizo la acción. Puede ser nulo cuando no había una sesión autenticada.",
                example = "ADMINISTRADOR", allowableValues = {"ADMINISTRADOR", "PROFESOR", "ESTUDIANTE"}, nullable = true)
        String rolUsuario,
        @Schema(description = "Descripción del evento sin correo, dirección IP, dispositivo ni geolocalización.",
                example = "Inicio de sesión rechazado: credenciales inválidas", requiredMode = Schema.RequiredMode.REQUIRED)
        String detalle
) {}
