package co.edu.uco.sigra.reportes.controller;

import co.edu.uco.sigra.auth.security.CurrentUser;
import co.edu.uco.sigra.common.openapi.EjemplosError;
import co.edu.uco.sigra.common.openapi.RespuestaErrorDoc;
import co.edu.uco.sigra.config.OpenApiConfig;
import co.edu.uco.sigra.reportes.dto.RiesgoAcademicoReporteResponseDTO;
import co.edu.uco.sigra.reportes.service.RiesgoAcademicoReporteService;
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
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Tag(name = "Reportes",
        description = "R1: rendimiento por RA. R6: estudiantes con RA no alcanzados (riesgo académico). "
                + "ADMINISTRADOR (todas las asignaturas) y PROFESOR (solo asignación docente ACTIVA).")
@SecurityRequirement(name = OpenApiConfig.ESQUEMA_BEARER)
@RestController
@RequestMapping("/api/v1/reportes")
@RequiredArgsConstructor
public class ReporteRiesgoAcademicoController {

    private static final String SEMESTRE_EJEMPLO = "9d8c7b6a-5f4e-4d3c-b2a1-0f9e8d7c6b5a";
    private static final String PROGRAMA_EJEMPLO = "5a4b3c2d-1e0f-4a9b-8c7d-6e5f4a3b2c1d";
    private static final String ASIGNATURA_EJEMPLO = "7c9e6679-7425-40de-944b-e07fc1f90ae7";
    private static final String EJ_NO_ENCONTRADO = "{\"timestamp\":\"" + EjemplosError.TIMESTAMP_EJEMPLO
            + "\",\"status\":404,\"error\":\"NOT_FOUND\",\"mensaje\":\"La asignatura indicada no existe.\"}";

    private final RiesgoAcademicoReporteService reporteService;
    private final CurrentUser currentUser;

    @Operation(
            summary = "R6 – Estudiantes con RA no alcanzados (riesgo académico)",
            description = "Lista estudiantes con nivel de logro BAJO o MEDIO (mismo criterio de insuficiente que "
                    + "RF-16) en el semestre, agrupados por estudiante y asignatura. Incluye detalle de RA, "
                    + "promedio y severidad (ATENCION / RIESGO / ALTO_RIESGO). Sin «semestreId» usa el semestre "
                    + "activo. «minRaNoAlcanzados» filtra por cantidad mínima de RA insuficientes (por defecto 1)."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Reporte generado (la lista puede venir vacía)",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = RiesgoAcademicoReporteResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Alguno de los identificadores no es un UUID válido",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = RespuestaErrorDoc.class),
                            examples = @ExampleObject(value = EjemplosError.PARAMETRO_INVALIDO_400))),
            @ApiResponse(responseCode = "401", description = "Falta el token o es inválido o expiró",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = RespuestaErrorDoc.class),
                            examples = @ExampleObject(value = EjemplosError.NO_AUTENTICADO_401))),
            @ApiResponse(responseCode = "403", description = "El rol no tiene permiso, o el PROFESOR pidió una asignatura que no tiene asignada",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = RespuestaErrorDoc.class),
                            examples = @ExampleObject(value = EjemplosError.PROHIBIDO_403))),
            @ApiResponse(responseCode = "404", description = "El semestre, el programa o la asignatura del filtro no existen",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = RespuestaErrorDoc.class),
                            examples = @ExampleObject(value = EJ_NO_ENCONTRADO)))
    })
    @GetMapping("/riesgo-academico")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'PROFESOR')")
    public ResponseEntity<RiesgoAcademicoReporteResponseDTO> riesgoAcademico(
            @Parameter(description = "Semestre (UUID). Sin valor se usa el semestre activo", example = SEMESTRE_EJEMPLO)
            @RequestParam(required = false) UUID semestreId,
            @Parameter(description = "Filtra por programa académico (UUID)", example = PROGRAMA_EJEMPLO)
            @RequestParam(required = false) UUID programaId,
            @Parameter(description = "Filtra por asignatura (UUID)", example = ASIGNATURA_EJEMPLO)
            @RequestParam(required = false) UUID asignaturaId,
            @Parameter(description = "Mínimo de RA no alcanzados por fila (default 1)", example = "1")
            @RequestParam(required = false) Integer minRaNoAlcanzados) {
        return ResponseEntity.ok(reporteService.consultar(
                semestreId, programaId, asignaturaId, minRaNoAlcanzados, currentUser.required()));
    }
}
