package co.edu.uco.sigra.reportes.controller;

import co.edu.uco.sigra.auth.security.CurrentUser;
import co.edu.uco.sigra.common.enums.RolUsuario;
import co.edu.uco.sigra.common.openapi.EjemplosError;
import co.edu.uco.sigra.common.openapi.RespuestaErrorDoc;
import co.edu.uco.sigra.config.OpenApiConfig;
import co.edu.uco.sigra.reportes.dto.TrazabilidadUsoProfesorResponseDTO;
import co.edu.uco.sigra.reportes.dto.TrazabilidadUsoReporteResponseDTO;
import co.edu.uco.sigra.reportes.service.TrazabilidadUsoReporteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.UUID;

@Tag(name = "Reportes", description = "R7: trazabilidad de uso por rol, a partir del historial de auditoría")
@SecurityRequirement(name = OpenApiConfig.ESQUEMA_BEARER)
@RestController
@RequestMapping("/api/v1/reportes/trazabilidad-uso")
@RequiredArgsConstructor
public class ReporteTrazabilidadUsoController {

    private static final String EJ_RANGO_INVALIDO = "{\"timestamp\":\"" + EjemplosError.TIMESTAMP_EJEMPLO
            + "\",\"status\":400,\"error\":\"BAD_REQUEST\","
            + "\"mensaje\":\"La fecha inicial no puede ser posterior a la fecha final.\"}";

    private final ObjectProvider<TrazabilidadUsoReporteService> service;
    private final CurrentUser currentUser;

    @Operation(summary = "R7 - Trazabilidad de uso por rol (administrador)",
            description = "Resume los eventos auditados (inicios de sesión, cambios de estado y calificaciones "
                    + "modificadas o fuera de fecha) agrupados por tipo de usuario. Solo devuelve conteos: no "
                    + "incluye identificadores de usuario ni el detalle de los eventos. Con «semestreId» el rango "
                    + "base son las fechas del semestre y «desde»/«hasta» lo acotan; sin semestre, por defecto se "
                    + "consultan los últimos 30 días. El rango no puede superar 366 días. Los intentos de acceso "
                    + "rechazados aparecen como SIN_IDENTIFICAR.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Reporte generado",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = TrazabilidadUsoReporteResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Fecha, rol o UUID mal formados, o rango inválido",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = RespuestaErrorDoc.class),
                            examples = @ExampleObject(value = EJ_RANGO_INVALIDO))),
            @ApiResponse(responseCode = "401", description = "Falta el token o es inválido",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = RespuestaErrorDoc.class),
                            examples = @ExampleObject(value = EjemplosError.NO_AUTENTICADO_401))),
            @ApiResponse(responseCode = "403", description = "El rol no es ADMINISTRADOR",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = RespuestaErrorDoc.class),
                            examples = @ExampleObject(value = EjemplosError.PROHIBIDO_403))),
            @ApiResponse(responseCode = "404", description = "El semestre indicado no existe",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = RespuestaErrorDoc.class)))
    })
    @GetMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<TrazabilidadUsoReporteResponseDTO> consultarGeneral(
            @Parameter(description = "Fecha inicial inclusiva (YYYY-MM-DD)", example = "2026-10-01")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @Parameter(description = "Fecha final inclusiva (YYYY-MM-DD)", example = "2026-10-31")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @Parameter(description = "Semestre (UUID); sus fechas definen el rango base")
            @RequestParam(required = false) UUID semestreId,
            @Parameter(description = "Limita el reporte a un tipo de usuario")
            @RequestParam(required = false) RolUsuario rol) {
        return ResponseEntity.ok(service.getObject().consultarGeneral(desde, hasta, semestreId, rol));
    }

    @Operation(summary = "R7 - Trazabilidad de uso (profesor)",
            description = "Devuelve la actividad auditada del profesor autenticado y la de cada estudiante con "
                    + "matrícula activa en sus asignaturas asignadas (ingresos al sistema, total de eventos y "
                    + "último acceso). No incluye documento ni detalle de los eventos. Sin «semestreId» usa el "
                    + "semestre activo, cuyas fechas definen el rango base; «desde»/«hasta» lo acotan.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Reporte generado",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = TrazabilidadUsoProfesorResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Fecha o UUID mal formados, o rango inválido",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = RespuestaErrorDoc.class),
                            examples = @ExampleObject(value = EJ_RANGO_INVALIDO))),
            @ApiResponse(responseCode = "401", description = "Falta el token o es inválido",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = RespuestaErrorDoc.class),
                            examples = @ExampleObject(value = EjemplosError.NO_AUTENTICADO_401))),
            @ApiResponse(responseCode = "403", description = "El rol no es PROFESOR",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = RespuestaErrorDoc.class),
                            examples = @ExampleObject(value = EjemplosError.PROHIBIDO_403))),
            @ApiResponse(responseCode = "404", description = "El semestre indicado no existe",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = RespuestaErrorDoc.class)))
    })
    @GetMapping("/profesor")
    @PreAuthorize("hasRole('PROFESOR')")
    public ResponseEntity<TrazabilidadUsoProfesorResponseDTO> consultarProfesor(
            @Parameter(description = "Fecha inicial inclusiva (YYYY-MM-DD)", example = "2026-10-01")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @Parameter(description = "Fecha final inclusiva (YYYY-MM-DD)", example = "2026-10-31")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @Parameter(description = "Semestre (UUID). Sin valor se usa el semestre activo")
            @RequestParam(required = false) UUID semestreId) {
        return ResponseEntity.ok(service.getObject().consultarProfesor(desde, hasta, semestreId, currentUser.required()));
    }
}
