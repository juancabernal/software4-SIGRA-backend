package co.edu.uco.sigra.mejoras.controller;

import co.edu.uco.sigra.auth.security.CurrentUser;
import co.edu.uco.sigra.common.openapi.EjemplosError;
import co.edu.uco.sigra.common.openapi.RespuestaErrorDoc;
import co.edu.uco.sigra.config.OpenApiConfig;
import co.edu.uco.sigra.mejoras.dto.AccionMejoraFiltroDTO;
import co.edu.uco.sigra.mejoras.dto.AccionMejoraResponseDTO;
import co.edu.uco.sigra.mejoras.dto.ConvertirEvaluacionRequestDTO;
import co.edu.uco.sigra.mejoras.dto.RegistrarAccionMejoraRequestDTO;
import co.edu.uco.sigra.mejoras.service.AccionMejoraService;
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
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/** RF-17/RF-18: acciones de mejora dentro del alcance docente autorizado. */
@Tag(name = "Acciones de mejora",
        description = "Registro y consulta de acciones de mejora sobre conclusiones VIGENTES (RF-17), y su "
                + "conversión en evaluación (RF-18). Registrar y convertir requieren el rol PROFESOR; las consultas "
                + "admiten también ADMINISTRADOR. Un PROFESOR solo opera sobre sus asignaturas con asignación "
                + "docente ACTIVA.")
@SecurityRequirement(name = OpenApiConfig.ESQUEMA_BEARER)
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class AccionMejoraController {

    private static final String CONCLUSION_ID_EJEMPLO = "5e6f7a8b-9c0d-4e1f-2a3b-4c5d6e7f8a9b";
    private static final String ACCION_ID_EJEMPLO = "8b9c0d1e-2f3a-4b4c-5d6e-7f8a9b0c1d2e";
    private static final String TS = EjemplosError.TIMESTAMP_EJEMPLO;

    // Ejemplos propios del módulo; los mensajes son los que producen las excepciones de mejoras y las
    // de evaluaciones que reutiliza la conversión (RF-18).
    private static final String EJ_ACCION_NO_ENCONTRADA =
            "{\"timestamp\":\"" + TS + "\",\"status\":404,\"error\":\"NOT_FOUND\","
                    + "\"mensaje\":\"No existe una acción de mejora con id " + ACCION_ID_EJEMPLO + ".\"}";

    private static final String EJ_CONCLUSION_NO_ENCONTRADA =
            "{\"timestamp\":\"" + TS + "\",\"status\":404,\"error\":\"NOT_FOUND\","
                    + "\"mensaje\":\"No existe una conclusión con id " + CONCLUSION_ID_EJEMPLO + ".\"}";

    private static final String EJ_CONCLUSION_NO_VIGENTE =
            "{\"timestamp\":\"" + TS + "\",\"status\":400,\"error\":\"BAD_REQUEST\","
                    + "\"mensaje\":\"La conclusión " + CONCLUSION_ID_EJEMPLO + " no está en estado VIGENTE.\"}";

    private static final String EJ_ACCION_NO_CONVERTIBLE =
            "{\"timestamp\":\"" + TS + "\",\"status\":400,\"error\":\"BAD_REQUEST\","
                    + "\"mensaje\":\"La acción de mejora no se puede convertir en evaluación: descripcion: La "
                    + "descripción debe tener máximo 100 caracteres\"}";

    private static final String EJ_PORCENTAJE_EXCEDIDO =
            "{\"timestamp\":\"" + TS + "\",\"status\":400,\"error\":\"BAD_REQUEST\","
                    + "\"mensaje\":\"La suma de porcentajes de las evaluaciones activas de este resultado de "
                    + "aprendizaje en el semestre superaría el 100%. Porcentaje disponible: 15%.\"}";

    private static final String EJ_FECHA_FUERA_DEL_SEMESTRE =
            "{\"timestamp\":\"" + TS + "\",\"status\":400,\"error\":\"BAD_REQUEST\","
                    + "\"mensaje\":\"La fecha tentativa debe estar dentro del semestre 2026-1 (entre 2026-01-20 y "
                    + "2026-05-30).\"}";

    private static final String EJ_ACCION_YA_CONVERTIDA =
            "{\"timestamp\":\"" + TS + "\",\"status\":409,\"error\":\"CONFLICT\","
                    + "\"mensaje\":\"La acción de mejora " + ACCION_ID_EJEMPLO + " ya fue convertida en evaluación.\"}";

    private static final String EJ_NOMBRE_EVALUACION_DUPLICADO =
            "{\"timestamp\":\"" + TS + "\",\"status\":409,\"error\":\"CONFLICT\","
                    + "\"mensaje\":\"Ya existe una evaluación con ese nombre para este resultado de aprendizaje en "
                    + "el semestre.\"}";

    private final AccionMejoraService accionMejoraService;
    private final CurrentUser currentUser;

    @Operation(
            summary = "Registrar acción de mejora",
            description = "Registra una acción de mejora sobre una conclusión VIGENTE y la marca como ATENDIDA. "
                    + "Solo el PROFESOR con asignación docente ACTIVA sobre la asignatura de la conclusión puede "
                    + "registrarla."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Acción de mejora registrada",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = AccionMejoraResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Datos inválidos o la conclusión no está en estado VIGENTE",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = RespuestaErrorDoc.class),
                            examples = {
                                    @ExampleObject(name = "validacion", summary = "Datos inválidos",
                                            value = EjemplosError.VALIDACION_400),
                                    @ExampleObject(name = "conclusionNoVigente", summary = "Conclusión no vigente",
                                            value = EJ_CONCLUSION_NO_VIGENTE)
                            })),
            @ApiResponse(responseCode = "401", description = "Falta el token o es inválido o expiró",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = RespuestaErrorDoc.class),
                            examples = @ExampleObject(value = EjemplosError.NO_AUTENTICADO_401))),
            @ApiResponse(responseCode = "403", description = "El usuario autenticado no tiene el rol PROFESOR, o es un PROFESOR sin asignación docente activa sobre la asignatura de la conclusión",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = RespuestaErrorDoc.class),
                            examples = @ExampleObject(value = EjemplosError.PROHIBIDO_403))),
            @ApiResponse(responseCode = "404", description = "No existe una conclusión con ese id",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = RespuestaErrorDoc.class),
                            examples = @ExampleObject(value = EJ_CONCLUSION_NO_ENCONTRADA)))
    })
    @PostMapping("/conclusiones/{conclusionId}/acciones-mejora")
    @PreAuthorize("hasRole('PROFESOR')")
    public ResponseEntity<AccionMejoraResponseDTO> registrar(
            @Parameter(description = "Identificador (UUID) de la conclusión VIGENTE", example = CONCLUSION_ID_EJEMPLO)
            @PathVariable UUID conclusionId,
            @Valid @RequestBody RegistrarAccionMejoraRequestDTO datos) {
        return ResponseEntity.ok(accionMejoraService.registrarAccionMejora(
                conclusionId, datos, currentUser.required()));
    }

    @Operation(
            summary = "Consultar acciones de mejora",
            description = "Lista las acciones de mejora visibles para el usuario autenticado: el ADMINISTRADOR ve "
                    + "todas; el PROFESOR solo ve las de sus asignaturas con asignación docente ACTIVA. Admite "
                    + "filtrar por resultado de aprendizaje, asignatura, semestre de la conclusión origen y si ya "
                    + "fue convertida en evaluación."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de acciones de mejora (puede estar vacía)",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            array = @ArraySchema(schema = @Schema(implementation = AccionMejoraResponseDTO.class)))),
            @ApiResponse(responseCode = "400", description = "Un parámetro tiene formato inválido (UUID o booleano)",
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
    @GetMapping("/acciones-mejora")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'PROFESOR')")
    public ResponseEntity<List<AccionMejoraResponseDTO>> consultar(
            @Parameter(description = "Filtra por el identificador (UUID) del resultado de aprendizaje", example = "6f7a8b9c-0d1e-4f2a-3b4c-5d6e7f8a9b0c")
            @RequestParam(required = false) UUID resultadoAprendizajeId,
            @Parameter(description = "Filtra por el identificador (UUID) de la asignatura", example = "3c4d5e6f-7a8b-4c9d-0e1f-2a3b4c5d6e7f")
            @RequestParam(required = false) UUID asignaturaId,
            @Parameter(description = "Filtra por el identificador (UUID) del semestre de la conclusión origen", example = "7a8b9c0d-1e2f-4a3b-4c5d-6e7f8a9b0c1d")
            @RequestParam(required = false) UUID semestreId,
            @Parameter(description = "Filtra por si la acción ya fue convertida en evaluación", example = "false")
            @RequestParam(required = false) Boolean convertida) {
        AccionMejoraFiltroDTO filtro = new AccionMejoraFiltroDTO(
                resultadoAprendizajeId, asignaturaId, semestreId, convertida);
        return ResponseEntity.ok(accionMejoraService.consultarAcciones(filtro, currentUser.required()));
    }

    @Operation(
            summary = "Obtener una acción de mejora",
            description = "Devuelve el detalle de una acción de mejora. El ADMINISTRADOR puede consultar "
                    + "cualquiera; un PROFESOR solo puede consultar las de asignaturas con asignación docente ACTIVA."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Acción de mejora encontrada",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = AccionMejoraResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "El id no es un UUID",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = RespuestaErrorDoc.class),
                            examples = @ExampleObject(value = EjemplosError.PARAMETRO_INVALIDO_400))),
            @ApiResponse(responseCode = "401", description = "Falta el token o es inválido o expiró",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = RespuestaErrorDoc.class),
                            examples = @ExampleObject(value = EjemplosError.NO_AUTENTICADO_401))),
            @ApiResponse(responseCode = "403", description = "El usuario autenticado no tiene el rol ADMINISTRADOR ni PROFESOR, o es un PROFESOR consultando una acción de mejora fuera de su alcance",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = RespuestaErrorDoc.class),
                            examples = @ExampleObject(value = EjemplosError.PROHIBIDO_403))),
            @ApiResponse(responseCode = "404", description = "No existe una acción de mejora con ese id",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = RespuestaErrorDoc.class),
                            examples = @ExampleObject(value = EJ_ACCION_NO_ENCONTRADA)))
    })
    @GetMapping("/acciones-mejora/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'PROFESOR')")
    public ResponseEntity<AccionMejoraResponseDTO> consultarPorId(
            @Parameter(description = "Identificador (UUID) de la acción de mejora", example = ACCION_ID_EJEMPLO)
            @PathVariable UUID id) {
        return ResponseEntity.ok(accionMejoraService.consultarPorId(id, currentUser.required()));
    }

    @Operation(
            summary = "Convertir una acción de mejora en evaluación",
            description = "Crea una evaluación a partir de la acción de mejora (hereda nombre y descripción) con "
                    + "la fecha tentativa y el porcentaje indicados, y la marca como convertida. Aplica las mismas "
                    + "reglas de negocio que crear una evaluación (RF-10/RF-11): semestre activo, fecha dentro del "
                    + "semestre, resultado de aprendizaje y asignatura activos, nombre único y suma de porcentajes "
                    + "activos hasta 100%. Solo el PROFESOR con asignación docente ACTIVA puede convertir, y una "
                    + "acción solo se puede convertir una vez."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Acción de mejora convertida en evaluación",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = AccionMejoraResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Datos inválidos, el nombre o la descripción heredados no cumplen las reglas de una evaluación, o se incumple una regla de negocio de evaluaciones (semestre, fechas, porcentaje disponible, RA o asignatura inactivos)",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = RespuestaErrorDoc.class),
                            examples = {
                                    @ExampleObject(name = "validacion", summary = "Datos inválidos",
                                            value = EjemplosError.VALIDACION_400),
                                    @ExampleObject(name = "noConvertible", summary = "Nombre o descripción heredados inválidos",
                                            value = EJ_ACCION_NO_CONVERTIBLE),
                                    @ExampleObject(name = "porcentajeExcedido", summary = "Porcentaje disponible superado",
                                            value = EJ_PORCENTAJE_EXCEDIDO),
                                    @ExampleObject(name = "fechaFueraDelSemestre", summary = "Fecha fuera del semestre",
                                            value = EJ_FECHA_FUERA_DEL_SEMESTRE)
                            })),
            @ApiResponse(responseCode = "401", description = "Falta el token o es inválido o expiró",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = RespuestaErrorDoc.class),
                            examples = @ExampleObject(value = EjemplosError.NO_AUTENTICADO_401))),
            @ApiResponse(responseCode = "403", description = "El usuario autenticado no tiene el rol PROFESOR, o es un PROFESOR sin asignación docente activa sobre la asignatura de la acción de mejora",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = RespuestaErrorDoc.class),
                            examples = @ExampleObject(value = EjemplosError.PROHIBIDO_403))),
            @ApiResponse(responseCode = "404", description = "No existe una acción de mejora con ese id",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = RespuestaErrorDoc.class),
                            examples = @ExampleObject(value = EJ_ACCION_NO_ENCONTRADA))),
            @ApiResponse(responseCode = "409", description = "La acción de mejora ya fue convertida en evaluación, o ya existe una evaluación con el mismo nombre heredado para ese resultado de aprendizaje en el semestre",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = RespuestaErrorDoc.class),
                            examples = {
                                    @ExampleObject(name = "yaConvertida", summary = "Acción ya convertida",
                                            value = EJ_ACCION_YA_CONVERTIDA),
                                    @ExampleObject(name = "nombreDuplicado", summary = "Nombre de evaluación duplicado",
                                            value = EJ_NOMBRE_EVALUACION_DUPLICADO)
                            }))
    })
    @PostMapping("/acciones-mejora/{id}/conversion")
    @PreAuthorize("hasRole('PROFESOR')")
    public ResponseEntity<AccionMejoraResponseDTO> convertirEnEvaluacion(
            @Parameter(description = "Identificador (UUID) de la acción de mejora", example = ACCION_ID_EJEMPLO)
            @PathVariable UUID id,
            @Valid @RequestBody ConvertirEvaluacionRequestDTO datos) {
        return ResponseEntity.ok(accionMejoraService.convertirEnEvaluacion(
                id, datos, currentUser.required()));
    }
}
