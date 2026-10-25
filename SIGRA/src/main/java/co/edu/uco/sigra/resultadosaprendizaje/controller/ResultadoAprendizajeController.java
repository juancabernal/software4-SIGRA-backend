package co.edu.uco.sigra.resultadosaprendizaje.controller;

import co.edu.uco.sigra.common.enums.EstadoRegistro;
import co.edu.uco.sigra.common.openapi.EjemplosError;
import co.edu.uco.sigra.common.openapi.RespuestaErrorDoc;
import co.edu.uco.sigra.config.OpenApiConfig;
import co.edu.uco.sigra.resultadosaprendizaje.dto.ResultadoAprendizajeActualizarRequestDTO;
import co.edu.uco.sigra.resultadosaprendizaje.dto.ResultadoAprendizajeCrearRequestDTO;
import co.edu.uco.sigra.resultadosaprendizaje.dto.ResultadoAprendizajeResponseDTO;
import co.edu.uco.sigra.resultadosaprendizaje.service.ResultadoAprendizajeService;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Tag(name = "Resultados de aprendizaje",
        description = "Registro, consulta, modificación, inactivación y reactivación de resultados de aprendizaje (RF-06). "
                + "Las escrituras son del rol ADMINISTRADOR; la consulta también la hace el PROFESOR asignado a la asignatura.")
@SecurityRequirement(name = OpenApiConfig.ESQUEMA_BEARER)
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ResultadoAprendizajeController {

    private static final String ID_EJEMPLO = "1a2b3c4d-5e6f-4a7b-8c9d-0e1f2a3b4c5d";
    private static final String ASIGNATURA_EJEMPLO = "7c9e6679-7425-40de-944b-e07fc1f90ae7";
    private static final String TS = EjemplosError.TIMESTAMP_EJEMPLO;
    private static final String P400 = "{\"timestamp\":\"" + TS + "\",\"status\":400,\"error\":\"BAD_REQUEST\",\"mensaje\":\"";
    private static final String P404 = "{\"timestamp\":\"" + TS + "\",\"status\":404,\"error\":\"NOT_FOUND\",\"mensaje\":\"";
    private static final String FIN = "\"}";

    // Ejemplos propios del módulo; los mensajes son los que producen sus excepciones.
    private static final String EJ_VALIDACION =
            "{\"timestamp\":\"" + TS + "\",\"status\":400,\"error\":\"BAD_REQUEST\","
                    + "\"mensaje\":\"Los datos de entrada no cumplen con las validaciones requeridas\","
                    + "\"detalles\":[\"descripcion: La descripción debe tener entre 10 y 100 caracteres\"]}";
    private static final String EJ_RA_NO_ENCONTRADO = P404 + "No existe un resultado de aprendizaje con id " + ID_EJEMPLO + "." + FIN;
    private static final String EJ_ASIGNATURA_NO_ENCONTRADA = P404 + "No existe una asignatura con id " + ASIGNATURA_EJEMPLO + "." + FIN;
    private static final String EJ_CODIGO_DUPLICADO =
            "{\"timestamp\":\"" + TS + "\",\"status\":409,\"error\":\"CONFLICT\","
                    + "\"mensaje\":\"Ya existe un resultado de aprendizaje con el código RA-01 en esta asignatura.\"}";
    private static final String EJ_MAXIMO_ACTIVOS = P400 + "La asignatura ya tiene 7 resultados de aprendizaje activos, que es el máximo permitido." + FIN;
    private static final String EJ_MINIMO_ACTIVOS = P400 + "No se puede inactivar: una asignatura activa debe tener mínimo 5 resultados de aprendizaje activos." + FIN;
    private static final String EJ_ASIGNATURA_INACTIVA = P400 + "La asignatura está inactiva; no se pueden registrar ni reactivar resultados de aprendizaje." + FIN;
    private static final String EJ_YA_INACTIVO = P400 + "El resultado de aprendizaje RA-01 ya se encuentra inactivo." + FIN;
    private static final String EJ_YA_ACTIVO = P400 + "El resultado de aprendizaje RA-01 ya se encuentra activo." + FIN;
    private static final String EJ_DESCRIPCION_DE_INACTIVO = P400 + "No se puede modificar la descripción del resultado de aprendizaje RA-01 porque está inactivo." + FIN;

    private final ResultadoAprendizajeService resultadoAprendizajeService;

    @Operation(
            summary = "Registrar resultado de aprendizaje",
            description = "Crea un RA ACTIVO en la asignatura (RF-06a). El código es único dentro de la asignatura, se guarda en mayúsculas "
                    + "y solo admite letras sin tilde, números y guiones (por ejemplo RA-01). La descripción tiene entre 10 y 100 caracteres. "
                    + "Una asignatura puede tener como máximo 7 RA activos y no se registran RA en una asignatura INACTIVA."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Resultado de aprendizaje registrado",
                        content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                schema = @Schema(implementation = ResultadoAprendizajeResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Datos inválidos (código o descripción), asignatura inactiva o máximo de 7 RA activos alcanzado",
                        content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                schema = @Schema(implementation = RespuestaErrorDoc.class),
                                examples = {
                                                @ExampleObject(name = "validacion", summary = "Validación del cuerpo",
                                                value = EJ_VALIDACION),
                                                @ExampleObject(name = "maximo", summary = "Máximo de RA activos",
                                                value = EJ_MAXIMO_ACTIVOS),
                                                @ExampleObject(name = "asignaturaInactiva", summary = "Asignatura inactiva",
                                                value = EJ_ASIGNATURA_INACTIVA)})),
            @ApiResponse(responseCode = "401", description = "Falta el token o es inválido o expiró",
                        content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                schema = @Schema(implementation = RespuestaErrorDoc.class),
                                examples = @ExampleObject(value = EjemplosError.NO_AUTENTICADO_401))),
            @ApiResponse(responseCode = "403", description = "El usuario autenticado no tiene el rol ADMINISTRADOR",
                        content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                schema = @Schema(implementation = RespuestaErrorDoc.class),
                                examples = @ExampleObject(value = EjemplosError.PROHIBIDO_403))),
            @ApiResponse(responseCode = "404", description = "La asignatura no existe",
                        content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                schema = @Schema(implementation = RespuestaErrorDoc.class),
                                examples = @ExampleObject(value = EJ_ASIGNATURA_NO_ENCONTRADA))),
            @ApiResponse(responseCode = "409", description = "Ya existe un RA con ese código en la asignatura",
                        content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                schema = @Schema(implementation = RespuestaErrorDoc.class),
                                examples = @ExampleObject(value = EJ_CODIGO_DUPLICADO)))
    })
    @PostMapping("/asignaturas/{asignaturaId}/resultados-aprendizaje")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<ResultadoAprendizajeResponseDTO> registrar(
            @Parameter(description = "Identificador (UUID) de la asignatura", example = ASIGNATURA_EJEMPLO)
            @PathVariable UUID asignaturaId,
            @Valid @RequestBody ResultadoAprendizajeCrearRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(resultadoAprendizajeService.registrar(asignaturaId, dto));
    }

    @Operation(
            summary = "Consultar los RA de una asignatura",
            description = "Lista los RA de la asignatura ordenados por código (RF-06b). Sin «estado» devuelve activos e inactivos. "
                    + "El administrador consulta cualquier asignatura; el profesor, solo las que tiene asignadas con asignación ACTIVA. "
                    + "Una asignatura sin RA devuelve una lista vacía."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de RA de la asignatura (puede estar vacía)",
                        content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                array = @ArraySchema(schema = @Schema(implementation = ResultadoAprendizajeResponseDTO.class)))),
            @ApiResponse(responseCode = "400", description = "El «estado» o el identificador de la asignatura tienen un formato inválido",
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
            @ApiResponse(responseCode = "404", description = "La asignatura no existe",
                        content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                schema = @Schema(implementation = RespuestaErrorDoc.class),
                                examples = @ExampleObject(value = EJ_ASIGNATURA_NO_ENCONTRADA)))
    })
    @GetMapping("/asignaturas/{asignaturaId}/resultados-aprendizaje")
    @PreAuthorize("hasRole('ADMINISTRADOR') or @securityAuthorization.hasActiveAssignment(#asignaturaId, authentication)")
    public ResponseEntity<List<ResultadoAprendizajeResponseDTO>> consultarPorAsignatura(
            @Parameter(description = "Identificador (UUID) de la asignatura", example = ASIGNATURA_EJEMPLO)
            @PathVariable UUID asignaturaId,
            @Parameter(description = "Filtra por estado del RA. Sin valor devuelve todos", example = "ACTIVO")
            @RequestParam(required = false) EstadoRegistro estado) {
        return ResponseEntity.ok(resultadoAprendizajeService.consultarPorAsignatura(asignaturaId, estado));
    }

    @Operation(
            summary = "Consultar todos los RA",
            description = "Lista los RA de todas las asignaturas ordenados por código. Sin «estado» devuelve activos e inactivos. "
                    + "Solo el administrador."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de RA (puede estar vacía)",
                        content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                array = @ArraySchema(schema = @Schema(implementation = ResultadoAprendizajeResponseDTO.class)))),
            @ApiResponse(responseCode = "400", description = "El «estado» tiene un formato inválido",
                        content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                schema = @Schema(implementation = RespuestaErrorDoc.class),
                                examples = @ExampleObject(value = EjemplosError.PARAMETRO_INVALIDO_400))),
            @ApiResponse(responseCode = "401", description = "Falta el token o es inválido o expiró",
                        content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                schema = @Schema(implementation = RespuestaErrorDoc.class),
                                examples = @ExampleObject(value = EjemplosError.NO_AUTENTICADO_401))),
            @ApiResponse(responseCode = "403", description = "El usuario autenticado no tiene el rol ADMINISTRADOR",
                        content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                schema = @Schema(implementation = RespuestaErrorDoc.class),
                                examples = @ExampleObject(value = EjemplosError.PROHIBIDO_403)))
    })
    @GetMapping("/resultados-aprendizaje")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<List<ResultadoAprendizajeResponseDTO>> consultarTodos(
            @Parameter(description = "Filtra por estado del RA. Sin valor devuelve todos", example = "ACTIVO")
            @RequestParam(required = false) EstadoRegistro estado) {
        return ResponseEntity.ok(resultadoAprendizajeService.consultarTodos(estado));
    }

    @Operation(
            summary = "Consultar un RA por id",
            description = "Devuelve un RA. El administrador consulta cualquiera; el profesor, solo los de asignaturas que tiene asignadas "
                    + "con asignación ACTIVA."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Resultado de aprendizaje",
                        content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                schema = @Schema(implementation = ResultadoAprendizajeResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "El identificador no es un UUID válido",
                        content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                schema = @Schema(implementation = RespuestaErrorDoc.class),
                                examples = @ExampleObject(value = EjemplosError.PARAMETRO_INVALIDO_400))),
            @ApiResponse(responseCode = "401", description = "Falta el token o es inválido o expiró",
                        content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                schema = @Schema(implementation = RespuestaErrorDoc.class),
                                examples = @ExampleObject(value = EjemplosError.NO_AUTENTICADO_401))),
            @ApiResponse(responseCode = "403", description = "El rol no tiene permiso, o es un PROFESOR sin asignación docente ACTIVA en la asignatura del RA",
                        content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                schema = @Schema(implementation = RespuestaErrorDoc.class),
                                examples = @ExampleObject(value = EjemplosError.PROHIBIDO_403))),
            @ApiResponse(responseCode = "404", description = "El RA no existe",
                        content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                schema = @Schema(implementation = RespuestaErrorDoc.class),
                                examples = @ExampleObject(value = EJ_RA_NO_ENCONTRADO)))
    })
    @GetMapping("/resultados-aprendizaje/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR') or @securityAuthorization.hasActiveAssignmentForResultado(#id, authentication)")
    public ResponseEntity<ResultadoAprendizajeResponseDTO> consultarPorId(
            @Parameter(description = "Identificador (UUID) del resultado de aprendizaje", example = ID_EJEMPLO)
            @PathVariable UUID id) {
        return ResponseEntity.ok(resultadoAprendizajeService.consultarPorId(id));
    }

    @Operation(
            summary = "Modificar la descripción de un RA",
            description = "Cambia únicamente la descripción (RF-06c); el código y la asignatura no se pueden modificar. "
                    + "No se puede modificar un RA INACTIVO."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Descripción actualizada",
                        content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                schema = @Schema(implementation = ResultadoAprendizajeResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Descripción inválida (obligatoria, de 10 a 100 caracteres, sin < ni >) o RA inactivo",
                        content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                schema = @Schema(implementation = RespuestaErrorDoc.class),
                                examples = {
                                                @ExampleObject(name = "validacion", summary = "Validación del cuerpo",
                                                value = EJ_VALIDACION),
                                                @ExampleObject(name = "raInactivo", summary = "RA inactivo",
                                                value = EJ_DESCRIPCION_DE_INACTIVO)})),
            @ApiResponse(responseCode = "401", description = "Falta el token o es inválido o expiró",
                        content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                schema = @Schema(implementation = RespuestaErrorDoc.class),
                                examples = @ExampleObject(value = EjemplosError.NO_AUTENTICADO_401))),
            @ApiResponse(responseCode = "403", description = "El usuario autenticado no tiene el rol ADMINISTRADOR",
                        content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                schema = @Schema(implementation = RespuestaErrorDoc.class),
                                examples = @ExampleObject(value = EjemplosError.PROHIBIDO_403))),
            @ApiResponse(responseCode = "404", description = "El RA no existe",
                        content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                schema = @Schema(implementation = RespuestaErrorDoc.class),
                                examples = @ExampleObject(value = EJ_RA_NO_ENCONTRADO)))
    })
    @PutMapping("/resultados-aprendizaje/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<ResultadoAprendizajeResponseDTO> actualizarDescripcion(
            @Parameter(description = "Identificador (UUID) del resultado de aprendizaje", example = ID_EJEMPLO)
            @PathVariable UUID id,
            @Valid @RequestBody ResultadoAprendizajeActualizarRequestDTO dto) {
        return ResponseEntity.ok(resultadoAprendizajeService.actualizarDescripcion(id, dto));
    }

    @Operation(
            summary = "Inactivar un RA",
            description = "Eliminación lógica (RF-06c): el RA queda INACTIVO y sus evaluaciones activas del semestre en curso pasan a INACTIVA, "
                    + "marcadas para restaurarse si el RA se reactiva. Si la asignatura está ACTIVA debe conservar al menos 5 RA activos, "
                    + "por lo que la operación se rechaza cuando quedaría por debajo."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "RA inactivado",
                        content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                schema = @Schema(implementation = ResultadoAprendizajeResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "El identificador no es válido, el RA ya está inactivo o la asignatura quedaría con menos de 5 RA activos",
                        content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                schema = @Schema(implementation = RespuestaErrorDoc.class),
                                examples = {
                                                @ExampleObject(name = "yaInactivo", summary = "RA ya inactivo",
                                                value = EJ_YA_INACTIVO),
                                                @ExampleObject(name = "minimo", summary = "Mínimo de RA activos",
                                                value = EJ_MINIMO_ACTIVOS)})),
            @ApiResponse(responseCode = "401", description = "Falta el token o es inválido o expiró",
                        content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                schema = @Schema(implementation = RespuestaErrorDoc.class),
                                examples = @ExampleObject(value = EjemplosError.NO_AUTENTICADO_401))),
            @ApiResponse(responseCode = "403", description = "El usuario autenticado no tiene el rol ADMINISTRADOR",
                        content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                schema = @Schema(implementation = RespuestaErrorDoc.class),
                                examples = @ExampleObject(value = EjemplosError.PROHIBIDO_403))),
            @ApiResponse(responseCode = "404", description = "El RA no existe",
                        content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                schema = @Schema(implementation = RespuestaErrorDoc.class),
                                examples = @ExampleObject(value = EJ_RA_NO_ENCONTRADO)))
    })
    @PatchMapping("/resultados-aprendizaje/{id}/inactivar")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<ResultadoAprendizajeResponseDTO> inactivar(
            @Parameter(description = "Identificador (UUID) del resultado de aprendizaje", example = ID_EJEMPLO)
            @PathVariable UUID id) {
        return ResponseEntity.ok(resultadoAprendizajeService.inactivar(id));
    }

    @Operation(
            summary = "Reactivar un RA",
            description = "Vuelve a dejar ACTIVO un RA inactivo y restaura sus evaluaciones inactivadas con él en el semestre en curso. "
                    + "Se rechaza si la asignatura está INACTIVA o si ya tiene 7 RA activos."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "RA reactivado",
                        content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                schema = @Schema(implementation = ResultadoAprendizajeResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "El identificador no es válido, el RA ya está activo, la asignatura está inactiva o ya tiene 7 RA activos",
                        content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                schema = @Schema(implementation = RespuestaErrorDoc.class),
                                examples = {
                                                @ExampleObject(name = "yaActivo", summary = "RA ya activo",
                                                value = EJ_YA_ACTIVO),
                                                @ExampleObject(name = "asignaturaInactiva", summary = "Asignatura inactiva",
                                                value = EJ_ASIGNATURA_INACTIVA),
                                                @ExampleObject(name = "maximo", summary = "Máximo de RA activos",
                                                value = EJ_MAXIMO_ACTIVOS)})),
            @ApiResponse(responseCode = "401", description = "Falta el token o es inválido o expiró",
                        content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                schema = @Schema(implementation = RespuestaErrorDoc.class),
                                examples = @ExampleObject(value = EjemplosError.NO_AUTENTICADO_401))),
            @ApiResponse(responseCode = "403", description = "El usuario autenticado no tiene el rol ADMINISTRADOR",
                        content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                schema = @Schema(implementation = RespuestaErrorDoc.class),
                                examples = @ExampleObject(value = EjemplosError.PROHIBIDO_403))),
            @ApiResponse(responseCode = "404", description = "El RA no existe",
                        content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                schema = @Schema(implementation = RespuestaErrorDoc.class),
                                examples = @ExampleObject(value = EJ_RA_NO_ENCONTRADO)))
    })
    @PatchMapping("/resultados-aprendizaje/{id}/reactivar")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<ResultadoAprendizajeResponseDTO> reactivar(
            @Parameter(description = "Identificador (UUID) del resultado de aprendizaje", example = ID_EJEMPLO)
            @PathVariable UUID id) {
        return ResponseEntity.ok(resultadoAprendizajeService.reactivar(id));
    }
}
