package co.edu.uco.sigra.asignaturas.controller;

import co.edu.uco.sigra.asignaturas.dto.AsignacionMasivaRequestDTO;
import co.edu.uco.sigra.asignaturas.dto.ResultadoAsignacionMasivaDTO;
import co.edu.uco.sigra.asignaturas.service.AsignacionDocenteService;
import co.edu.uco.sigra.common.openapi.EjemplosError;
import co.edu.uco.sigra.common.openapi.RespuestaErrorDoc;
import co.edu.uco.sigra.config.OpenApiConfig;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Asignación docente (RF-07): asigna varios profesores a varias asignaturas en una sola operación
 * y permite desasignar. Los errores de dominio los atiende AsignacionDocenteExceptionHandler.
 * El endpoint es administrativo y queda protegido por la misma capa RBAC que el resto del API.
 */
@Tag(name = "Asignación docente",
        description = "RF-07: asignación masiva y desasignación lógica de profesores en asignaturas. "
                + "Todos los endpoints requieren el rol ADMINISTRADOR.")
@SecurityRequirement(name = OpenApiConfig.ESQUEMA_BEARER)
@RestController
@RequestMapping("/api/v1/asignaciones-docentes")
@RequiredArgsConstructor
public class AsignacionDocenteController {

    private static final String PROFESOR_ID_EJEMPLO = "b3d4e5f6-7a8b-49c0-91d2-3e4f5a6b7c8d";
    private static final String ASIGNATURA_ID_EJEMPLO = "c4e5f6a7-8b9c-4d01-a2e3-4f5a6b7c8d9e";
    private static final String ASIGNACION_ID_EJEMPLO = "1a2b3c4d-5e6f-4a7b-8c9d-0e1f2a3b4c5d";
    private static final String TS = EjemplosError.TIMESTAMP_EJEMPLO;

    // Ejemplos propios del módulo; los mensajes son los que producen las excepciones de cada dominio.
    private static final String EJ_PROFESOR_NO_ENCONTRADO =
            "{\"timestamp\":\"" + TS + "\",\"status\":404,\"error\":\"NOT_FOUND\","
                    + "\"mensaje\":\"No se encontró el profesor con id " + PROFESOR_ID_EJEMPLO + "\"}";

    private static final String EJ_ASIGNATURA_NO_ENCONTRADA =
            "{\"timestamp\":\"" + TS + "\",\"status\":404,\"error\":\"NOT_FOUND\","
                    + "\"mensaje\":\"No existe una asignatura con id " + ASIGNATURA_ID_EJEMPLO + ".\"}";

    private static final String EJ_ASIGNACION_NO_ENCONTRADA =
            "{\"timestamp\":\"" + TS + "\",\"status\":404,\"error\":\"NOT_FOUND\","
                    + "\"mensaje\":\"No existe una asignación docente con id " + ASIGNACION_ID_EJEMPLO + ".\"}";

    private static final String EJ_RESULTADO_EXITOSO =
            "{\"creadas\":[{\"id\":\"" + ASIGNACION_ID_EJEMPLO + "\",\"profesorId\":\"" + PROFESOR_ID_EJEMPLO
                    + "\",\"nombreProfesor\":\"Ana Gómez\",\"asignaturaId\":\"" + ASIGNATURA_ID_EJEMPLO
                    + "\",\"codigoAsignatura\":\"MAT-101\",\"nombreAsignatura\":\"Cálculo\","
                    + "\"estado\":\"ACTIVO\",\"fechaAsignacion\":\"2026-10-14\"}],"
                    + "\"omitidas\":[{\"profesorId\":\"" + PROFESOR_ID_EJEMPLO + "\",\"asignaturaId\":\""
                    + ASIGNATURA_ID_EJEMPLO + "\",\"motivo\":\"El profesor ya está asignado a esta asignatura\"}],"
                    + "\"rechazadas\":[{\"profesorId\":\"" + PROFESOR_ID_EJEMPLO + "\",\"asignaturaId\":\""
                    + ASIGNATURA_ID_EJEMPLO + "\",\"motivo\":\"El profesor está inactivo\"}]}";

    private final AsignacionDocenteService asignacionDocenteService;

    @Operation(
            summary = "Asignar profesores a asignaturas",
            description = "Asigna cada profesor indicado a cada asignatura indicada. Un par que ya está ACTIVO "
                    + "se informa en «omitidas»; una asignación INACTIVA previa se reactiva y aparece en "
                    + "«creadas». Los pares con profesor o asignatura inactivos se informan en «rechazadas», "
                    + "sin impedir que se procesen los demás pares válidos."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Asignación masiva procesada",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ResultadoAsignacionMasivaDTO.class),
                            examples = @ExampleObject(name = "resultado", summary = "Con creadas, omitidas y rechazadas",
                                    value = EJ_RESULTADO_EXITOSO))),
            @ApiResponse(responseCode = "400", description = "Cuerpo inválido, listas vacías o identificadores nulos",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = RespuestaErrorDoc.class),
                            examples = @ExampleObject(value = EjemplosError.VALIDACION_400))),
            @ApiResponse(responseCode = "401", description = "Falta el token o es inválido o expiró",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = RespuestaErrorDoc.class),
                            examples = @ExampleObject(value = EjemplosError.NO_AUTENTICADO_401))),
            @ApiResponse(responseCode = "403", description = "El usuario autenticado no tiene el rol ADMINISTRADOR",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = RespuestaErrorDoc.class),
                            examples = @ExampleObject(value = EjemplosError.PROHIBIDO_403))),
            @ApiResponse(responseCode = "404", description = "No existe alguno de los profesores o asignaturas indicados",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = RespuestaErrorDoc.class),
                            examples = {
                                    @ExampleObject(name = "profesorNoEncontrado", summary = "Profesor inexistente",
                                            value = EJ_PROFESOR_NO_ENCONTRADO),
                                    @ExampleObject(name = "asignaturaNoEncontrada", summary = "Asignatura inexistente",
                                            value = EJ_ASIGNATURA_NO_ENCONTRADA)
                            }))
    })
    @PostMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<ResultadoAsignacionMasivaDTO> asignar(@Valid @RequestBody AsignacionMasivaRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(asignacionDocenteService.asignarMasivo(dto));
    }

    @Operation(
            summary = "Desasignar profesor de una asignatura",
            description = "Inactiva lógicamente la asignación. Si ya está INACTIVA, conserva ese estado y "
                    + "responde igualmente sin contenido."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Asignación desasignada o ya inactiva"),
            @ApiResponse(responseCode = "400", description = "El identificador no tiene formato UUID",
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
                            examples = @ExampleObject(value = EjemplosError.PROHIBIDO_403))),
            @ApiResponse(responseCode = "404", description = "No existe una asignación con el identificador indicado",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = RespuestaErrorDoc.class),
                            examples = @ExampleObject(value = EJ_ASIGNACION_NO_ENCONTRADA)))
    })
    @PatchMapping("/{id}/desasignar")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<Void> desasignar(
            @Parameter(description = "Identificador UUID de la asignación docente", example = ASIGNACION_ID_EJEMPLO)
            @PathVariable UUID id) {
        asignacionDocenteService.desasignar(id);
        return ResponseEntity.noContent().build();
    }
}
