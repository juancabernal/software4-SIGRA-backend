package co.edu.uco.sigra.auditoria.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(name = "PaginaAuditoria", description = "Una página del reporte de historial, del más reciente al más antiguo")
public record PaginaAuditoriaDTO(
        @Schema(description = "Registros de auditoría de la página, ordenados del más reciente al más antiguo.",
                example = "[{\"id\":\"4b64b64b-4b64-4b64-8b64-4b64b64b64b6\",\"fechaHora\":\"2026-10-20T14:35:12.123\",\"accion\":\"LOGIN_EXITOSO\",\"tipoEntidad\":\"USUARIO\",\"idEntidad\":\"4b64b64b-4b64-4b64-8b64-4b64b64b64b6\",\"usuarioId\":\"4b64b64b-4b64-4b64-8b64-4b64b64b64b6\",\"rolUsuario\":\"ADMINISTRADOR\",\"detalle\":\"Inicio de sesión exitoso como ADMINISTRADOR\"}]",
                requiredMode = Schema.RequiredMode.REQUIRED)
        List<RegistroAuditoriaDTO> contenido,
        @Schema(description = "Número de página, empezando en 0", example = "0",
                requiredMode = Schema.RequiredMode.REQUIRED)
        int pagina,
        @Schema(description = "Registros por página", example = "20",
                requiredMode = Schema.RequiredMode.REQUIRED)
        int tamano,
        @Schema(description = "Total de registros que cumplen los filtros", example = "135",
                requiredMode = Schema.RequiredMode.REQUIRED)
        long totalElementos,
        @Schema(description = "Total de páginas", example = "7",
                requiredMode = Schema.RequiredMode.REQUIRED)
        int totalPaginas
) {}
