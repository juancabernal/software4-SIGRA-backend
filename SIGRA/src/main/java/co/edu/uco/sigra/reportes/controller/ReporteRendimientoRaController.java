package co.edu.uco.sigra.reportes.controller;

import co.edu.uco.sigra.auth.security.CurrentUser;
import co.edu.uco.sigra.common.openapi.EjemplosError;
import co.edu.uco.sigra.common.openapi.RespuestaErrorDoc;
import co.edu.uco.sigra.config.OpenApiConfig;
import co.edu.uco.sigra.reportes.dto.RendimientoRaReporteResponseDTO;
import co.edu.uco.sigra.reportes.service.RendimientoRaReporteService;
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
        description = "R1: rendimiento académico por RA y asignatura. Lo consultan el ADMINISTRADOR (todas las "
                + "asignaturas) y el PROFESOR (solo las asignadas con asignación docente ACTIVA).")
@SecurityRequirement(name = OpenApiConfig.ESQUEMA_BEARER)
@RestController
@RequestMapping("/api/v1/reportes")
@RequiredArgsConstructor
public class ReporteRendimientoRaController {

    private static final String SEMESTRE_EJEMPLO = "9d8c7b6a-5f4e-4d3c-b2a1-0f9e8d7c6b5a";
    private static final String PROGRAMA_EJEMPLO = "5a4b3c2d-1e0f-4a9b-8c7d-6e5f4a3b2c1d";
    private static final String ASIGNATURA_EJEMPLO = "7c9e6679-7425-40de-944b-e07fc1f90ae7";
    private static final String EJ_NO_ENCONTRADO = "{\"timestamp\":\"" + EjemplosError.TIMESTAMP_EJEMPLO
            + "\",\"status\":404,\"error\":\"NOT_FOUND\",\"mensaje\":\"La asignatura indicada no existe.\"}";

    private final RendimientoRaReporteService reporteService;
    private final CurrentUser currentUser;

    @Operation(
            summary = "R1 – Rendimiento académico por RA y asignatura",
            description = "Para cada asignatura con RA activos devuelve, por RA: cantidad de calificaciones, promedio, "
                    + "distribución Bajo/Medio/Alto/Sobresaliente (cantidad y porcentaje) y nivel de logro, con los "
                    + "mismos cálculos del módulo de estadísticas. Sin calificaciones el RA queda «Sin evaluar»; con "
                    + "1 o 2 calificaciones por estudiante, «Sin datos suficientes para calcular»; si no hay "
                    + "asignaturas que mostrar, el mensaje es «Sin datos disponibles». Sin «semestreId» usa el semestre "
                    + "activo. Los filtros «programaId» y «asignaturaId» son opcionales y combinables."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Reporte generado (las asignaturas pueden venir vacías)",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = RendimientoRaReporteResponseDTO.class))),
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
    @GetMapping("/rendimiento-ra")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'PROFESOR')")
    public ResponseEntity<RendimientoRaReporteResponseDTO> rendimientoPorRa(
            @Parameter(description = "Semestre (UUID). Sin valor se usa el semestre activo", example = SEMESTRE_EJEMPLO)
            @RequestParam(required = false) UUID semestreId,
            @Parameter(description = "Filtra por programa académico (UUID)", example = PROGRAMA_EJEMPLO)
            @RequestParam(required = false) UUID programaId,
            @Parameter(description = "Filtra por asignatura (UUID)", example = ASIGNATURA_EJEMPLO)
            @RequestParam(required = false) UUID asignaturaId) {
        return ResponseEntity.ok(reporteService.consultar(semestreId, programaId, asignaturaId, currentUser.required()));
    }
}
