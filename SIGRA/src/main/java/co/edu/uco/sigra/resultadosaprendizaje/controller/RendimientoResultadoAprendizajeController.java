package co.edu.uco.sigra.resultadosaprendizaje.controller;

import co.edu.uco.sigra.common.openapi.EjemplosError;
import co.edu.uco.sigra.common.openapi.RespuestaErrorDoc;
import co.edu.uco.sigra.config.OpenApiConfig;
import co.edu.uco.sigra.resultadosaprendizaje.dto.RendimientoAsignaturaResponseDTO;
import co.edu.uco.sigra.resultadosaprendizaje.service.RendimientoResultadoAprendizajeService;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Tag(name = "Rendimiento de resultados de aprendizaje",
        description = "Rendimiento de los RA activos de una asignatura (RF-14, RF-15). Lo consultan el ADMINISTRADOR y "
                + "el PROFESOR con asignación docente ACTIVA en la asignatura.")
@SecurityRequirement(name = OpenApiConfig.ESQUEMA_BEARER)
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class RendimientoResultadoAprendizajeController {

    private static final String ASIGNATURA_EJEMPLO = "7c9e6679-7425-40de-944b-e07fc1f90ae7";
    private static final String SEMESTRE_EJEMPLO = "9d8c7b6a-5f4e-4d3c-b2a1-0f9e8d7c6b5a";
    private static final String TS = EjemplosError.TIMESTAMP_EJEMPLO;
    private static final String P404 = "{\"timestamp\":\"" + TS + "\",\"status\":404,\"error\":\"NOT_FOUND\",\"mensaje\":\"";
    private static final String FIN = "\"}";

    private static final String EJ_ASIGNATURA_NO_ENCONTRADA = P404 + "No existe una asignatura con id " + ASIGNATURA_EJEMPLO + "." + FIN;
    private static final String EJ_SEMESTRE_NO_ENCONTRADO = P404 + "No existe un semestre con id " + SEMESTRE_EJEMPLO + "." + FIN;

    private final RendimientoResultadoAprendizajeService rendimientoService;

    @Operation(
            summary = "Consultar el rendimiento de los RA de una asignatura",
            description = "Calcula, para cada RA activo de la asignatura, su estado (SIN_EVALUAR, DATOS_INSUFICIENTES o "
                    + "CON_NIVEL), el nivel de logro y el promedio (promedio de los promedios ponderados de los estudiantes "
                    + "con nivel calculado, mínimo 3 calificaciones por estudiante), el porcentaje asignado y el evaluado, "
                    + "las evaluaciones activas y si está completo (al menos 3 evaluaciones activas que suman 100% y están "
                    + "calificadas a todos los matriculados activos). Sin «semestreId» usa el semestre activo; si no hay "
                    + "semestre activo devuelve la lista vacía con semestre nulo. Con «semestreId» consulta ese semestre "
                    + "(por ejemplo uno pasado), con el último nivel calculado de ese semestre."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Rendimiento de los RA activos (la lista puede estar vacía)",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = RendimientoAsignaturaResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "El identificador de la asignatura o el «semestreId» no son UUID válidos",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = RespuestaErrorDoc.class),
                            examples = @ExampleObject(value = EjemplosError.PARAMETRO_INVALIDO_400))),
            @ApiResponse(responseCode = "401", description = "Falta el token o es inválido o expiró",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = RespuestaErrorDoc.class),
                            examples = @ExampleObject(value = EjemplosError.NO_AUTENTICADO_401))),
            @ApiResponse(responseCode = "403", description = "El rol no tiene permiso, o es un PROFESOR sin asignación docente ACTIVA en la asignatura",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = RespuestaErrorDoc.class),
                            examples = @ExampleObject(value = EjemplosError.PROHIBIDO_403))),
            @ApiResponse(responseCode = "404", description = "La asignatura o el semestre indicado no existen",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = RespuestaErrorDoc.class),
                            examples = {
                                    @ExampleObject(name = "asignatura", summary = "Asignatura inexistente",
                                            value = EJ_ASIGNATURA_NO_ENCONTRADA),
                                    @ExampleObject(name = "semestre", summary = "Semestre inexistente",
                                            value = EJ_SEMESTRE_NO_ENCONTRADO)}))
    })
    @GetMapping("/asignaturas/{asignaturaId}/resultados-aprendizaje/rendimiento")
    @PreAuthorize("hasRole('ADMINISTRADOR') or @securityAuthorization.hasActiveAssignment(#asignaturaId, authentication)")
    public ResponseEntity<RendimientoAsignaturaResponseDTO> consultar(
            @Parameter(description = "Identificador (UUID) de la asignatura", example = ASIGNATURA_EJEMPLO)
            @PathVariable UUID asignaturaId,
            @Parameter(description = "Semestre a consultar (UUID). Sin valor se usa el semestre activo", example = SEMESTRE_EJEMPLO)
            @RequestParam(required = false) UUID semestreId) {
        return ResponseEntity.ok(rendimientoService.consultarPorAsignatura(asignaturaId, semestreId));
    }
}
