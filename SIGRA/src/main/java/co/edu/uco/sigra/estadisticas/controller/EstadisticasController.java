package co.edu.uco.sigra.estadisticas.controller;

import co.edu.uco.sigra.auth.security.CurrentUser;
import co.edu.uco.sigra.common.openapi.EjemplosError;
import co.edu.uco.sigra.common.openapi.RespuestaErrorDoc;
import co.edu.uco.sigra.config.OpenApiConfig;
import co.edu.uco.sigra.estadisticas.dto.EstadisticasResultadoAprendizajeResponseDTO;
import co.edu.uco.sigra.estadisticas.dto.RangoNivelLogroResponseDTO;
import co.edu.uco.sigra.estadisticas.dto.RecalculoEstadisticasResponseDTO;
import co.edu.uco.sigra.estadisticas.service.NivelLogroService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * RF-14 / RF-15: nivel de logro y estadísticas descriptivas por RA y semestre, con autorización RBAC
 * ({@code @PreAuthorize}) y alcance por asignación docente en el servicio, igual que
 * {@link ConclusionController}. Documentado con el patrón de docs/documentacion_openapi/swagger-profesores.md.
 */
@Tag(name = "Nivel de logro y estadísticas",
        description = "Nivel de logro por estudiante y estadísticas descriptivas por RA y semestre (RF-14, RF-15). "
                + "Estadísticas y recálculo: ADMINISTRADOR, o PROFESOR solo en asignaturas con asignación docente "
                + "ACTIVA. Catálogo de niveles: los tres roles.")
@SecurityRequirement(name = OpenApiConfig.ESQUEMA_BEARER)
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class EstadisticasController {

    private static final String RA_EJEMPLO = "1a2b3c4d-5e6f-4a7b-8c9d-0e1f2a3b4c5d";
    private static final String SEMESTRE_EJEMPLO = "6b1f9d7e-5c3a-4f21-9e8b-2d4a7c6e1f05";
    private static final String TS = EjemplosError.TIMESTAMP_EJEMPLO;

    // Mensajes iguales a los de ResultadoAprendizajeNoEncontradoException y SemestreNoEncontradoException.
    private static final String EJ_RA_NO_ENCONTRADO =
            "{\"timestamp\":\"" + TS + "\",\"status\":404,\"error\":\"NOT_FOUND\","
                    + "\"mensaje\":\"No existe un resultado de aprendizaje con id " + RA_EJEMPLO + ".\"}";
    private static final String EJ_SEMESTRE_NO_ENCONTRADO =
            "{\"timestamp\":\"" + TS + "\",\"status\":404,\"error\":\"NOT_FOUND\","
                    + "\"mensaje\":\"No existe un semestre con id " + SEMESTRE_EJEMPLO + ".\"}";

    private final NivelLogroService nivelLogroService;
    private final CurrentUser currentUser;

    @Operation(
            summary = "Consultar las estadísticas de un RA en un semestre",
            description = "Devuelve el resultado del último recálculo: total de estudiantes (matriculados activos más "
                    + "quienes tienen calificaciones), evaluados (3 o más calificaciones), sin datos suficientes "
                    + "(1 o 2), sin evaluar (ninguna), promedio en nota y en porcentaje, y la distribución por los 4 "
                    + "niveles de logro (siempre en orden Bajo, Medio, Alto, Sobresaliente; porcentaje sobre los "
                    + "evaluados). Sin evaluados, el promedio es null."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Estadísticas del RA en el semestre",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = EstadisticasResultadoAprendizajeResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "El id del RA o del semestre no es un UUID",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = RespuestaErrorDoc.class),
                            examples = @ExampleObject(value = EjemplosError.PARAMETRO_INVALIDO_400))),
            @ApiResponse(responseCode = "401", description = "Falta el token o es inválido o expiró",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = RespuestaErrorDoc.class),
                            examples = @ExampleObject(value = EjemplosError.NO_AUTENTICADO_401))),
            @ApiResponse(responseCode = "403", description = "Rol sin permiso, o PROFESOR sin asignación docente ACTIVA en la asignatura del RA",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = RespuestaErrorDoc.class),
                            examples = @ExampleObject(value = EjemplosError.PROHIBIDO_403))),
            @ApiResponse(responseCode = "404", description = "No existe el RA o el semestre",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = RespuestaErrorDoc.class),
                            examples = {
                                    @ExampleObject(name = "ra", summary = "RA inexistente", value = EJ_RA_NO_ENCONTRADO),
                                    @ExampleObject(name = "semestre", summary = "Semestre inexistente",
                                            value = EJ_SEMESTRE_NO_ENCONTRADO)}))
    })
    @GetMapping("/resultados-aprendizaje/{raId}/semestres/{semestreId}/estadisticas")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'PROFESOR')")
    public ResponseEntity<EstadisticasResultadoAprendizajeResponseDTO> consultar(
            @Parameter(description = "Identificador (UUID) del resultado de aprendizaje", example = RA_EJEMPLO)
            @PathVariable UUID raId,
            @Parameter(description = "Identificador (UUID) del semestre", example = SEMESTRE_EJEMPLO)
            @PathVariable UUID semestreId) {
        return ResponseEntity.ok(nivelLogroService.consultarEstadisticas(raId, semestreId, currentUser.required()));
    }

    @Operation(
            summary = "Recalcular el nivel de logro y las estadísticas de un RA",
            description = "Recalcula el nivel de cada estudiante (promedio ponderado por el porcentaje de cada "
                    + "evaluación, normalizado, con mínimo 3 calificaciones) y dispara la conclusión automática de "
                    + "RF-16. Es idempotente. En operación normal lo dispara el registro de calificaciones; este "
                    + "endpoint permite forzarlo."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Estadísticas recalculadas y resultado de la conclusión (generada=false no es un error)",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = RecalculoEstadisticasResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "El id del RA o del semestre no es un UUID",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = RespuestaErrorDoc.class),
                            examples = @ExampleObject(value = EjemplosError.PARAMETRO_INVALIDO_400))),
            @ApiResponse(responseCode = "401", description = "Falta el token o es inválido o expiró",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = RespuestaErrorDoc.class),
                            examples = @ExampleObject(value = EjemplosError.NO_AUTENTICADO_401))),
            @ApiResponse(responseCode = "403", description = "Rol sin permiso, o PROFESOR sin asignación docente ACTIVA en la asignatura del RA",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = RespuestaErrorDoc.class),
                            examples = @ExampleObject(value = EjemplosError.PROHIBIDO_403))),
            @ApiResponse(responseCode = "404", description = "No existe el RA o el semestre",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = RespuestaErrorDoc.class),
                            examples = {
                                    @ExampleObject(name = "ra", summary = "RA inexistente", value = EJ_RA_NO_ENCONTRADO),
                                    @ExampleObject(name = "semestre", summary = "Semestre inexistente",
                                            value = EJ_SEMESTRE_NO_ENCONTRADO)}))
    })
    @PostMapping("/resultados-aprendizaje/{raId}/semestres/{semestreId}/estadisticas/recalculo")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'PROFESOR')")
    public ResponseEntity<RecalculoEstadisticasResponseDTO> recalcular(
            @Parameter(description = "Identificador (UUID) del resultado de aprendizaje", example = RA_EJEMPLO)
            @PathVariable UUID raId,
            @Parameter(description = "Identificador (UUID) del semestre", example = SEMESTRE_EJEMPLO)
            @PathVariable UUID semestreId) {
        return ResponseEntity.ok(nivelLogroService.recalcular(raId, semestreId, currentUser.required()));
    }

    @Operation(
            summary = "Consultar el catálogo de niveles de logro",
            description = "Los 4 rangos sobre la escala 0.0 – 5.0: Bajo [0, 3), Medio [3, 4), Alto [4, 4.5) y "
                    + "Sobresaliente [4.5, 5]. Solo Sobresaliente incluye su límite superior."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Los 4 rangos en orden ascendente",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            array = @ArraySchema(schema = @Schema(implementation = RangoNivelLogroResponseDTO.class)))),
            @ApiResponse(responseCode = "401", description = "Falta el token o es inválido o expiró",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = RespuestaErrorDoc.class),
                            examples = @ExampleObject(value = EjemplosError.NO_AUTENTICADO_401))),
            @ApiResponse(responseCode = "403", description = "El usuario autenticado no tiene un rol válido",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = RespuestaErrorDoc.class),
                            examples = @ExampleObject(value = EjemplosError.PROHIBIDO_403)))
    })
    @GetMapping("/niveles-logro")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'PROFESOR', 'ESTUDIANTE')")
    public ResponseEntity<List<RangoNivelLogroResponseDTO>> catalogo() {
        return ResponseEntity.ok(nivelLogroService.consultarCatalogo());
    }
}
