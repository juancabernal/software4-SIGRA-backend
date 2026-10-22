package co.edu.uco.sigra.auditoria.controller;

import co.edu.uco.sigra.auditoria.dto.PaginaAuditoriaDTO;
import co.edu.uco.sigra.auditoria.entity.TipoEntidadAuditoria;
import co.edu.uco.sigra.auditoria.service.AuditoriaService;
import co.edu.uco.sigra.config.OpenApiConfig;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@Validated
@RestController
@RequestMapping("/api/v1/auditoria")
@RequiredArgsConstructor
@Tag(name = "Auditoría", description = "RF-19 / RF-21c: historial de auditoría de solo inserción. Rol requerido: ADMINISTRADOR.")
@SecurityRequirement(name = OpenApiConfig.ESQUEMA_BEARER)
public class AuditoriaController {

    public static final int TAMANO_MAXIMO = 100;

    private final AuditoriaService auditoriaService;

    @Operation(summary = "Reporte del historial de auditoría",
            description = "Todos los eventos con entidad afectada, acción, fecha y hora, usuario y detalle, del más "
                    + "reciente al más antiguo. Filtros opcionales por rango de fechas (inclusivo) y tipo de "
                    + "entidad. Paginado: página desde 0 y hasta " + TAMANO_MAXIMO + " registros por página.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Página del historial"),
            @ApiResponse(responseCode = "400", description = "Fecha mal formada, rango invertido, tipo de entidad "
                    + "desconocido o paginación fuera de rango"),
            @ApiResponse(responseCode = "401", description = "Falta el token o es inválido o expiró"),
            @ApiResponse(responseCode = "403", description = "El usuario no es ADMINISTRADOR")
    })
    @GetMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<PaginaAuditoriaDTO> consultar(
            @Parameter(description = "Fecha inicial, inclusiva (AAAA-MM-DD)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @Parameter(description = "Fecha final, inclusiva (AAAA-MM-DD)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @Parameter(description = "Tipo de entidad afectada")
            @RequestParam(required = false) TipoEntidadAuditoria tipoEntidad,
            @Parameter(description = "Número de página, empezando en 0")
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "La página no puede ser negativa") int pagina,
            @Parameter(description = "Registros por página (1 a " + TAMANO_MAXIMO + ")")
            @RequestParam(defaultValue = "20")
            @Min(value = 1, message = "El tamaño de página debe ser al menos 1")
            @Max(value = TAMANO_MAXIMO, message = "El tamaño de página no puede superar 100") int tamano) {
        return ResponseEntity.ok(auditoriaService.consultar(desde, hasta, tipoEntidad, pagina, tamano));
    }
}