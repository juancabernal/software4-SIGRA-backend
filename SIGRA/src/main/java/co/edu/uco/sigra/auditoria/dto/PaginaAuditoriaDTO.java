package co.edu.uco.sigra.auditoria.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(name = "PaginaAuditoria", description = "Una página del reporte de historial, del más reciente al más antiguo")
public record PaginaAuditoriaDTO(
        List<RegistroAuditoriaDTO> contenido,
        @Schema(description = "Número de página, empezando en 0", example = "0")
        int pagina,
        @Schema(description = "Registros por página", example = "20")
        int tamano,
        @Schema(description = "Total de registros que cumplen los filtros", example = "135")
        long totalElementos,
        @Schema(description = "Total de páginas", example = "7")
        int totalPaginas
) {}