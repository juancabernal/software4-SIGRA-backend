package co.edu.uco.sigra.estadisticas.controller;

import co.edu.uco.sigra.auth.security.CurrentUser;
import co.edu.uco.sigra.common.openapi.EjemplosError;
import co.edu.uco.sigra.common.openapi.RespuestaErrorDoc;
import co.edu.uco.sigra.config.OpenApiConfig;
import co.edu.uco.sigra.estadisticas.dto.ConclusionEstadoResponseDTO;
import co.edu.uco.sigra.estadisticas.dto.ConclusionFiltroDTO;
import co.edu.uco.sigra.estadisticas.dto.ConclusionResponseDTO;
import co.edu.uco.sigra.estadisticas.dto.GenerarConclusionResponseDTO;
import co.edu.uco.sigra.estadisticas.entity.EstadoConclusion;
import co.edu.uco.sigra.estadisticas.service.ConclusionService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/** RF-16: conclusiones automáticas con autorización RBAC y alcance por asignación docente. */
@Tag(name = "Conclusiones",
        description = "Generación y consulta de conclusiones automáticas por resultado de aprendizaje y semestre "
                + "(RF-16). Todos los endpoints requieren el rol ADMINISTRADOR o PROFESOR; un PROFESOR solo accede "
                + "a sus asignaturas con asignación docente ACTIVA.")
@SecurityRequirement(name = OpenApiConfig.ESQUEMA_BEARER)
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ConclusionController {

    private static final String RA_ID_EJEMPLO = "6f7a8b9c-0d1e-4f2a-3b4c-5d6e7f8a9b0c";
    private static final String SEMESTRE_ID_EJEMPLO = "7a8b9c0d-1e2f-4a3b-4c5d-6e7f8a9b0c1d";
    private static final String CONCLUSION_ID_EJEMPLO = "5e6f7a8b-9c0d-4e1f-2a3b-4c5d6e7f8a9b";
    private static final String TS = EjemplosError.TIMESTAMP_EJEMPLO;

    // Ejemplos propios del módulo; los mensajes son los que producen las excepciones de estadísticas.
    private static final String EJ_RA_NO_ENCONTRADO =
            "{\"timestamp\":\"" + TS + "\",\"status\":404,\"error\":\"NOT_FOUND\","
                    + "\"mensaje\":\"No existe un resultado de aprendizaje con id " + RA_ID_EJEMPLO + ".\"}";

    private static final String EJ_SEMESTRE_NO_ENCONTRADO =
            "{\"timestamp\":\"" + TS + "\",\"status\":404,\"error\":\"NOT_FOUND\","
                    + "\"mensaje\":\"No existe un semestre con id " + SEMESTRE_ID_EJEMPLO + ".\"}";

    private static final String EJ_CONCLUSION_NO_ENCONTRADA =
            "{\"timestamp\":\"" + TS + "\",\"status\":404,\"error\":\"NOT_FOUND\","
                    + "\"mensaje\":\"No existe una conclusión con id " + CONCLUSION_ID_EJEMPLO + ".\"}";

    private final ConclusionService conclusionService;
    private final CurrentUser currentUser;

    @Operation(
            summary = "Generar conclusión",
            description = "Evalúa el desempeño del resultado de aprendizaje en el semestre y, si supera el 40% de "
                    + "estudiantes en nivel Bajo o Medio con al menos 3 calificaciones, genera una conclusión "
                    + "VIGENTE. No genera nada (y no es un error) si los datos son insuficientes, el desempeño está "
                    + "dentro del umbral o ya existe una conclusión vigente para ese (RA, semestre)."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Resultado del intento de generación (generada puede ser false)",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = GenerarConclusionResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "El raId o el semestreId no son un UUID",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = RespuestaErrorDoc.class),
                            examples = @ExampleObject(value = EjemplosError.PARAMETRO_INVALIDO_400))),
            @ApiResponse(responseCode = "401", description = "Falta el token o es inválido o expiró",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = RespuestaErrorDoc.class),
                            examples = @ExampleObject(value = EjemplosError.NO_AUTENTICADO_401))),
            @ApiResponse(responseCode = "403", description = "El usuario autenticado no tiene el rol ADMINISTRADOR ni PROFESOR, o es un PROFESOR sin asignación docente activa sobre la asignatura del resultado de aprendizaje",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = RespuestaErrorDoc.class),
                            examples = @ExampleObject(value = EjemplosError.PROHIBIDO_403))),
            @ApiResponse(responseCode = "404", description = "No existe el resultado de aprendizaje o el semestre indicado",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = RespuestaErrorDoc.class),
                            examples = {
                                    @ExampleObject(name = "raNoEncontrado", summary = "Resultado de aprendizaje inexistente",
                                            value = EJ_RA_NO_ENCONTRADO),
                                    @ExampleObject(name = "semestreNoEncontrado", summary = "Semestre inexistente",
                                            value = EJ_SEMESTRE_NO_ENCONTRADO)
                            }))
    })
    @PostMapping("/resultados-aprendizaje/{raId}/semestres/{semestreId}/conclusiones")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'PROFESOR')")
    public ResponseEntity<GenerarConclusionResponseDTO> generar(
            @Parameter(description = "Identificador (UUID) del resultado de aprendizaje", example = RA_ID_EJEMPLO)
            @PathVariable UUID raId,
            @Parameter(description = "Identificador (UUID) del semestre", example = SEMESTRE_ID_EJEMPLO)
            @PathVariable UUID semestreId) {
        return ResponseEntity.ok(conclusionService.generarConclusion(raId, semestreId, currentUser.required()));
    }

    @Operation(
            summary = "Consultar conclusiones",
            description = "Lista las conclusiones visibles para el usuario autenticado: el ADMINISTRADOR ve todas; "
                    + "el PROFESOR solo ve las de sus asignaturas con asignación docente ACTIVA. Admite filtrar "
                    + "por resultado de aprendizaje, semestre, asignatura y estado."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de conclusiones (puede estar vacía)",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            array = @ArraySchema(schema = @Schema(implementation = ConclusionResponseDTO.class)))),
            @ApiResponse(responseCode = "400", description = "Un parámetro tiene formato inválido (UUID o estado)",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = RespuestaErrorDoc.class),
                            examples = @ExampleObject(value = EjemplosError.PARAMETRO_INVALIDO_400))),
            @ApiResponse(responseCode = "401", description = "Falta el token o es inválido o expiró",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = RespuestaErrorDoc.class),
                            examples = @ExampleObject(value = EjemplosError.NO_AUTENTICADO_401))),
            @ApiResponse(responseCode = "403", description = "El usuario autenticado no tiene el rol ADMINISTRADOR ni PROFESOR",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = RespuestaErrorDoc.class),
                            examples = @ExampleObject(value = EjemplosError.PROHIBIDO_403)))
    })
    @GetMapping("/conclusiones")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'PROFESOR')")
    public ResponseEntity<List<ConclusionResponseDTO>> consultar(
            @Parameter(description = "Filtra por el identificador (UUID) del resultado de aprendizaje", example = RA_ID_EJEMPLO)
            @RequestParam(required = false) UUID resultadoAprendizajeId,
            @Parameter(description = "Filtra por el identificador (UUID) del semestre", example = SEMESTRE_ID_EJEMPLO)
            @RequestParam(required = false) UUID semestreId,
            @Parameter(description = "Filtra por el identificador (UUID) de la asignatura", example = "3c4d5e6f-7a8b-4c9d-0e1f-2a3b4c5d6e7f")
            @RequestParam(required = false) UUID asignaturaId,
            @Parameter(description = "Filtra por el estado de la conclusión", example = "VIGENTE")
            @RequestParam(required = false) EstadoConclusion estado) {
        ConclusionFiltroDTO filtro = new ConclusionFiltroDTO(resultadoAprendizajeId, semestreId, asignaturaId, estado);
        return ResponseEntity.ok(conclusionService.consultarConclusiones(filtro, currentUser.required()));
    }

    @Operation(
            summary = "Obtener una conclusión",
            description = "Devuelve el detalle de una conclusión. El ADMINISTRADOR puede consultar cualquiera; un "
                    + "PROFESOR solo puede consultar las de asignaturas con asignación docente ACTIVA."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Conclusión encontrada",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ConclusionResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "El id no es un UUID",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = RespuestaErrorDoc.class),
                            examples = @ExampleObject(value = EjemplosError.PARAMETRO_INVALIDO_400))),
            @ApiResponse(responseCode = "401", description = "Falta el token o es inválido o expiró",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = RespuestaErrorDoc.class),
                            examples = @ExampleObject(value = EjemplosError.NO_AUTENTICADO_401))),
            @ApiResponse(responseCode = "403", description = "El usuario autenticado no tiene el rol ADMINISTRADOR ni PROFESOR, o es un PROFESOR consultando una conclusión fuera de su alcance",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = RespuestaErrorDoc.class),
                            examples = @ExampleObject(value = EjemplosError.PROHIBIDO_403))),
            @ApiResponse(responseCode = "404", description = "No existe una conclusión con ese id",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = RespuestaErrorDoc.class),
                            examples = @ExampleObject(value = EJ_CONCLUSION_NO_ENCONTRADA)))
    })
    @GetMapping("/conclusiones/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'PROFESOR')")
    public ResponseEntity<ConclusionResponseDTO> consultarPorId(
            @Parameter(description = "Identificador (UUID) de la conclusión", example = CONCLUSION_ID_EJEMPLO)
            @PathVariable UUID id) {
        return ResponseEntity.ok(conclusionService.consultarPorId(id, currentUser.required()));
    }

    @Operation(
            summary = "Consultar el estado de un resultado de aprendizaje",
            description = "Devuelve, sin generar ni mutar nada, el estado observable del (RA, semestre): si hay "
                    + "una conclusión VIGENTE o ATENDIDA, si los datos son insuficientes o si el desempeño es "
                    + "SATISFACTORIO."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Estado del resultado de aprendizaje en el semestre",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ConclusionEstadoResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "El raId o el semestreId no son un UUID",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = RespuestaErrorDoc.class),
                            examples = @ExampleObject(value = EjemplosError.PARAMETRO_INVALIDO_400))),
            @ApiResponse(responseCode = "401", description = "Falta el token o es inválido o expiró",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = RespuestaErrorDoc.class),
                            examples = @ExampleObject(value = EjemplosError.NO_AUTENTICADO_401))),
            @ApiResponse(responseCode = "403", description = "El usuario autenticado no tiene el rol ADMINISTRADOR ni PROFESOR, o es un PROFESOR sin asignación docente activa sobre la asignatura del resultado de aprendizaje",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = RespuestaErrorDoc.class),
                            examples = @ExampleObject(value = EjemplosError.PROHIBIDO_403))),
            @ApiResponse(responseCode = "404", description = "No existe el resultado de aprendizaje o el semestre indicado",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = RespuestaErrorDoc.class),
                            examples = {
                                    @ExampleObject(name = "raNoEncontrado", summary = "Resultado de aprendizaje inexistente",
                                            value = EJ_RA_NO_ENCONTRADO),
                                    @ExampleObject(name = "semestreNoEncontrado", summary = "Semestre inexistente",
                                            value = EJ_SEMESTRE_NO_ENCONTRADO)
                            }))
    })
    @GetMapping("/resultados-aprendizaje/{raId}/semestres/{semestreId}/conclusiones/estado")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'PROFESOR')")
    public ResponseEntity<ConclusionEstadoResponseDTO> consultarEstado(
            @Parameter(description = "Identificador (UUID) del resultado de aprendizaje", example = RA_ID_EJEMPLO)
            @PathVariable UUID raId,
            @Parameter(description = "Identificador (UUID) del semestre", example = SEMESTRE_ID_EJEMPLO)
            @PathVariable UUID semestreId) {
        return ResponseEntity.ok(conclusionService.consultarEstado(raId, semestreId, currentUser.required()));
    }
}
