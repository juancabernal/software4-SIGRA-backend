package co.edu.uco.sigra.estudiantes.controller;

import co.edu.uco.sigra.common.openapi.EjemplosError;
import co.edu.uco.sigra.common.openapi.RespuestaErrorDoc;
import co.edu.uco.sigra.config.OpenApiConfig;
import co.edu.uco.sigra.estudiantes.dto.EstudianteConMatriculaResponseDTO;
import co.edu.uco.sigra.estudiantes.dto.EstudianteMatriculadoDTO;
import co.edu.uco.sigra.estudiantes.dto.MatriculaConEstudianteRequestDTO;
import co.edu.uco.sigra.estudiantes.dto.MatriculaRequestDTO;
import co.edu.uco.sigra.estudiantes.dto.MatriculaResponseDTO;
import co.edu.uco.sigra.estudiantes.mapper.MatriculaMapper;
import co.edu.uco.sigra.estudiantes.service.MatriculaService;
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

/**
 * Matriz de acceso por rol (RF-05), aplicada por Spring Security. La autorización por recurso
 * (asignación docente activa) la valida el servicio antes de leer o modificar una matrícula.
 */
@Tag(name = "Matrículas",
        description = "Matrícula, desvinculación y consulta de matrículas de estudiantes en asignaturas "
                + "(RF-08, RF-09, RF-09a). Todos los endpoints requieren el rol ADMINISTRADOR o PROFESOR; un "
                + "PROFESOR solo puede operar sobre asignaturas con una asignación docente ACTIVA.")
@SecurityRequirement(name = OpenApiConfig.ESQUEMA_BEARER)
@RestController
@RequestMapping("/api/v1/matriculas")
@RequiredArgsConstructor
public class MatriculaController {

    private static final String MATRICULA_ID_EJEMPLO = "8f7e6d5c-4b3a-4291-8c7d-6b5a4f3e2d1c";
    private static final String ESTUDIANTE_ID_EJEMPLO = "4e3d2c1b-6a5f-4b8e-9d7c-1f0e9d8c7b6a";
    private static final String ASIGNATURA_ID_EJEMPLO = "7c6b5a4f-9e3d-4c1b-8a09-f8e7d6c5b4a3";
    private static final String SEMESTRE_ID_EJEMPLO = "9d8c7b6a-5f4e-4d3c-b2a1-0f9e8d7c6b5a";
    private static final String TIPO_DOCUMENTO_EJEMPLO = "6b1f9d7e-5c3a-4f21-9e8b-2d4a7c6e1f05";
    private static final String TS = EjemplosError.TIMESTAMP_EJEMPLO;

    // Ejemplos propios del módulo; los mensajes son los que producen las excepciones de estudiantes/matrículas.
    private static final String EJ_ESTUDIANTE_NO_ENCONTRADO =
            "{\"timestamp\":\"" + TS + "\",\"status\":404,\"error\":\"NOT_FOUND\","
                    + "\"mensaje\":\"No existe un estudiante con id " + ESTUDIANTE_ID_EJEMPLO + ".\"}";

    private static final String EJ_ASIGNATURA_NO_ENCONTRADA =
            "{\"timestamp\":\"" + TS + "\",\"status\":404,\"error\":\"NOT_FOUND\","
                    + "\"mensaje\":\"No existe una asignatura con id " + ASIGNATURA_ID_EJEMPLO + ".\"}";

    private static final String EJ_SEMESTRE_NO_ENCONTRADO =
            "{\"timestamp\":\"" + TS + "\",\"status\":404,\"error\":\"NOT_FOUND\","
                    + "\"mensaje\":\"No existe un semestre con id " + SEMESTRE_ID_EJEMPLO + ".\"}";

    private static final String EJ_TIPO_DOCUMENTO_NO_ENCONTRADO =
            "{\"timestamp\":\"" + TS + "\",\"status\":404,\"error\":\"NOT_FOUND\","
                    + "\"mensaje\":\"No existe un tipo de documento con id " + TIPO_DOCUMENTO_EJEMPLO + ".\"}";

    private static final String EJ_MATRICULA_NO_ENCONTRADA =
            "{\"timestamp\":\"" + TS + "\",\"status\":404,\"error\":\"NOT_FOUND\","
                    + "\"mensaje\":\"No existe una matrícula con id " + MATRICULA_ID_EJEMPLO + ".\"}";

    private static final String EJ_ESTUDIANTE_INACTIVO =
            "{\"timestamp\":\"" + TS + "\",\"status\":400,\"error\":\"BAD_REQUEST\","
                    + "\"mensaje\":\"El estudiante con id " + ESTUDIANTE_ID_EJEMPLO + " no está activo.\"}";

    private static final String EJ_ASIGNATURA_NO_ACTIVA =
            "{\"timestamp\":\"" + TS + "\",\"status\":400,\"error\":\"BAD_REQUEST\","
                    + "\"mensaje\":\"La asignatura con id " + ASIGNATURA_ID_EJEMPLO + " no está activa.\"}";

    private static final String EJ_SEMESTRE_NO_VIGENTE =
            "{\"timestamp\":\"" + TS + "\",\"status\":400,\"error\":\"BAD_REQUEST\","
                    + "\"mensaje\":\"El semestre con id " + SEMESTRE_ID_EJEMPLO + " no está vigente.\"}";

    private static final String EJ_MATRICULA_DUPLICADA =
            "{\"timestamp\":\"" + TS + "\",\"status\":409,\"error\":\"CONFLICT\","
                    + "\"mensaje\":\"El estudiante " + ESTUDIANTE_ID_EJEMPLO + " ya está matriculado en la "
                    + "asignatura " + ASIGNATURA_ID_EJEMPLO + " para el semestre " + SEMESTRE_ID_EJEMPLO + ".\"}";

    private static final String EJ_MATRICULA_YA_INACTIVA =
            "{\"timestamp\":\"" + TS + "\",\"status\":409,\"error\":\"CONFLICT\","
                    + "\"mensaje\":\"La matrícula con id " + MATRICULA_ID_EJEMPLO + " ya no está activa.\"}";

    private static final String EJ_CONFIRMACION_REQUERIDA =
            "{\"timestamp\":\"" + TS + "\",\"status\":409,\"error\":\"CONFLICT\","
                    + "\"mensaje\":\"La matrícula con id " + MATRICULA_ID_EJEMPLO + " tiene 3 calificación(es) "
                    + "registrada(s). Debe confirmar la desvinculación para continuar.\"}";

    private static final String EJ_DOCUMENTO_DUPLICADO =
            "{\"timestamp\":\"" + TS + "\",\"status\":409,\"error\":\"CONFLICT\","
                    + "\"mensaje\":\"El estudiante con el documento 1098765432 ya está registrado en el "
                    + "sistema.\"}";

    private static final String EJ_CORREO_DUPLICADO =
            "{\"timestamp\":\"" + TS + "\",\"status\":409,\"error\":\"CONFLICT\","
                    + "\"mensaje\":\"Ya existe un estudiante registrado con el correo mariana.gomez@uco.net.co.\"}";

    private final MatriculaService matriculaService;
    private final MatriculaMapper matriculaMapper;

    @Operation(
            summary = "Matricular un estudiante",
            description = "Matricula al estudiante en la asignatura para el semestre, o reactiva su matrícula de "
                    + "esa misma terna si estaba desvinculada. Responde 201 si crea una matrícula nueva y 200 si "
                    + "reactiva una existente. El estudiante y la asignatura deben estar ACTIVOS y la fecha de hoy "
                    + "debe estar dentro del semestre."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Matrícula reactivada (la terna ya existía desvinculada)",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = MatriculaResponseDTO.class))),
            @ApiResponse(responseCode = "201", description = "Matrícula creada",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = MatriculaResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Datos inválidos, o regla de negocio incumplida: estudiante inactivo, asignatura no activa o semestre no vigente",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = RespuestaErrorDoc.class),
                            examples = {
                                    @ExampleObject(name = "validacion", summary = "Validación del cuerpo",
                                            value = EjemplosError.VALIDACION_400),
                                    @ExampleObject(name = "estudianteInactivo", summary = "Estudiante inactivo",
                                            value = EJ_ESTUDIANTE_INACTIVO),
                                    @ExampleObject(name = "asignaturaNoActiva", summary = "Asignatura no activa",
                                            value = EJ_ASIGNATURA_NO_ACTIVA),
                                    @ExampleObject(name = "semestreNoVigente", summary = "Semestre no vigente",
                                            value = EJ_SEMESTRE_NO_VIGENTE)
                            })),
            @ApiResponse(responseCode = "401", description = "Falta el token o es inválido o expiró",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = RespuestaErrorDoc.class),
                            examples = @ExampleObject(value = EjemplosError.NO_AUTENTICADO_401))),
            @ApiResponse(responseCode = "403", description = "El usuario no tiene el rol ADMINISTRADOR ni PROFESOR, o es un PROFESOR sin asignación docente ACTIVA en la asignatura",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = RespuestaErrorDoc.class),
                            examples = @ExampleObject(value = EjemplosError.PROHIBIDO_403))),
            @ApiResponse(responseCode = "404", description = "El estudiante, la asignatura o el semestre indicados no existen",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = RespuestaErrorDoc.class),
                            examples = {
                                    @ExampleObject(name = "estudiante", summary = "Estudiante inexistente",
                                            value = EJ_ESTUDIANTE_NO_ENCONTRADO),
                                    @ExampleObject(name = "asignatura", summary = "Asignatura inexistente",
                                            value = EJ_ASIGNATURA_NO_ENCONTRADA),
                                    @ExampleObject(name = "semestre", summary = "Semestre inexistente",
                                            value = EJ_SEMESTRE_NO_ENCONTRADO)
                            })),
            @ApiResponse(responseCode = "409", description = "El estudiante ya está matriculado y activo en esa asignatura y semestre",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = RespuestaErrorDoc.class),
                            examples = @ExampleObject(value = EJ_MATRICULA_DUPLICADA)))
    })
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'PROFESOR')")
    public ResponseEntity<MatriculaResponseDTO> matricular(@Valid @RequestBody MatriculaRequestDTO dto) {
        var resultado = matriculaService.matricular(dto.estudianteId(), dto.asignaturaId(), dto.semestreId());
        var respuesta = matriculaMapper.toResponseDTO(resultado.matricula());
        var status = resultado.reactivada() ? HttpStatus.OK : HttpStatus.CREATED;
        return ResponseEntity.status(status).body(respuesta);
    }

    @Operation(
            summary = "Desvincular una matrícula",
            description = "Retira al estudiante de la asignatura dejando la matrícula en estado DESVINCULADA, sin "
                    + "borrarla ni perder las calificaciones ya registradas. Si la matrícula tiene calificaciones "
                    + "registradas, se exige «confirmar=true»: sin confirmar, responde 409 indicando cuántas tiene."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Matrícula desvinculada; no devuelve cuerpo"),
            @ApiResponse(responseCode = "400", description = "El id no es un UUID válido",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = RespuestaErrorDoc.class),
                            examples = @ExampleObject(value = EjemplosError.PARAMETRO_INVALIDO_400))),
            @ApiResponse(responseCode = "401", description = "Falta el token o es inválido o expiró",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = RespuestaErrorDoc.class),
                            examples = @ExampleObject(value = EjemplosError.NO_AUTENTICADO_401))),
            @ApiResponse(responseCode = "403", description = "El usuario no tiene el rol ADMINISTRADOR ni PROFESOR, o es un PROFESOR sin asignación docente ACTIVA en la asignatura de la matrícula",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = RespuestaErrorDoc.class),
                            examples = @ExampleObject(value = EjemplosError.PROHIBIDO_403))),
            @ApiResponse(responseCode = "404", description = "No existe una matrícula con ese id",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = RespuestaErrorDoc.class),
                            examples = @ExampleObject(value = EJ_MATRICULA_NO_ENCONTRADA))),
            @ApiResponse(responseCode = "409", description = "La matrícula ya está desvinculada, o tiene calificaciones registradas y no se confirmó la desvinculación",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = RespuestaErrorDoc.class),
                            examples = {
                                    @ExampleObject(name = "yaDesvinculada", summary = "Ya está desvinculada",
                                            value = EJ_MATRICULA_YA_INACTIVA),
                                    @ExampleObject(name = "confirmacionRequerida", summary = "Tiene calificaciones; falta confirmar",
                                            value = EJ_CONFIRMACION_REQUERIDA)
                            }))
    })
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'PROFESOR')")
    public ResponseEntity<Void> desvincular(
            @Parameter(description = "Identificador (UUID) de la matrícula", example = MATRICULA_ID_EJEMPLO)
            @PathVariable UUID id,
            @Parameter(description = "Confirma la desvinculación aunque la matrícula tenga calificaciones registradas", example = "false")
            @RequestParam(defaultValue = "false") boolean confirmar) {
        matriculaService.desvincular(id, confirmar);
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Consultar los matriculados de una asignatura",
            description = "Lista los estudiantes matriculados en la asignatura para el semestre. Sin "
                    + "«incluirInactivas» (o con valor false) solo trae las matrículas ACTIVAS; con true incluye "
                    + "también las DESVINCULADAS."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Estudiantes matriculados (puede estar vacía)",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            array = @ArraySchema(schema = @Schema(implementation = EstudianteMatriculadoDTO.class)))),
            @ApiResponse(responseCode = "400", description = "Falta un parámetro obligatorio o alguno no es un UUID válido",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = RespuestaErrorDoc.class),
                            examples = @ExampleObject(value = EjemplosError.PARAMETRO_INVALIDO_400))),
            @ApiResponse(responseCode = "401", description = "Falta el token o es inválido o expiró",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = RespuestaErrorDoc.class),
                            examples = @ExampleObject(value = EjemplosError.NO_AUTENTICADO_401))),
            @ApiResponse(responseCode = "403", description = "El usuario no tiene el rol ADMINISTRADOR ni PROFESOR, o es un PROFESOR sin asignación docente ACTIVA en la asignatura",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = RespuestaErrorDoc.class),
                            examples = @ExampleObject(value = EjemplosError.PROHIBIDO_403))),
            @ApiResponse(responseCode = "404", description = "La asignatura o el semestre indicados no existen",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = RespuestaErrorDoc.class),
                            examples = {
                                    @ExampleObject(name = "asignatura", summary = "Asignatura inexistente",
                                            value = EJ_ASIGNATURA_NO_ENCONTRADA),
                                    @ExampleObject(name = "semestre", summary = "Semestre inexistente",
                                            value = EJ_SEMESTRE_NO_ENCONTRADO)
                            }))
    })
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'PROFESOR')")
    public ResponseEntity<List<EstudianteMatriculadoDTO>> listarMatriculados(
            @Parameter(description = "Identificador (UUID) de la asignatura", example = ASIGNATURA_ID_EJEMPLO)
            @RequestParam UUID asignaturaId,
            @Parameter(description = "Identificador (UUID) del semestre", example = SEMESTRE_ID_EJEMPLO)
            @RequestParam UUID semestreId,
            @Parameter(description = "Si es true, incluye también las matrículas desvinculadas", example = "false")
            @RequestParam(defaultValue = "false") boolean incluirInactivas) {
        var matriculas = matriculaService.listarMatriculados(asignaturaId, semestreId, incluirInactivas);
        return ResponseEntity.ok(matriculaMapper.toEstudianteMatriculadoDTOList(matriculas));
    }

    @Operation(
            summary = "Registrar un estudiante y matricularlo",
            description = "Crea un estudiante nuevo y lo matricula en la asignatura y el semestre en una sola "
                    + "operación atómica (RF-09a): si la matrícula falla por cualquier regla, el estudiante no "
                    + "queda creado. Aplica las mismas validaciones del registro de estudiante y de la matrícula."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Estudiante registrado y matriculado",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = EstudianteConMatriculaResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Datos inválidos, o regla de negocio incumplida: asignatura no activa o semestre no vigente",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = RespuestaErrorDoc.class),
                            examples = {
                                    @ExampleObject(name = "validacion", summary = "Validación del cuerpo",
                                            value = EjemplosError.VALIDACION_400),
                                    @ExampleObject(name = "asignaturaNoActiva", summary = "Asignatura no activa",
                                            value = EJ_ASIGNATURA_NO_ACTIVA),
                                    @ExampleObject(name = "semestreNoVigente", summary = "Semestre no vigente",
                                            value = EJ_SEMESTRE_NO_VIGENTE)
                            })),
            @ApiResponse(responseCode = "401", description = "Falta el token o es inválido o expiró",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = RespuestaErrorDoc.class),
                            examples = @ExampleObject(value = EjemplosError.NO_AUTENTICADO_401))),
            @ApiResponse(responseCode = "403", description = "El usuario no tiene el rol ADMINISTRADOR ni PROFESOR, o es un PROFESOR sin asignación docente ACTIVA en la asignatura",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = RespuestaErrorDoc.class),
                            examples = @ExampleObject(value = EjemplosError.PROHIBIDO_403))),
            @ApiResponse(responseCode = "404", description = "El tipo de documento, la asignatura o el semestre indicados no existen",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = RespuestaErrorDoc.class),
                            examples = {
                                    @ExampleObject(name = "tipoDocumento", summary = "Tipo de documento inexistente",
                                            value = EJ_TIPO_DOCUMENTO_NO_ENCONTRADO),
                                    @ExampleObject(name = "asignatura", summary = "Asignatura inexistente",
                                            value = EJ_ASIGNATURA_NO_ENCONTRADA),
                                    @ExampleObject(name = "semestre", summary = "Semestre inexistente",
                                            value = EJ_SEMESTRE_NO_ENCONTRADO)
                            })),
            @ApiResponse(responseCode = "409", description = "Ya existe un estudiante con esa cédula o con ese correo",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = RespuestaErrorDoc.class),
                            examples = {
                                    @ExampleObject(name = "documentoDuplicado", summary = "Cédula repetida",
                                            value = EJ_DOCUMENTO_DUPLICADO),
                                    @ExampleObject(name = "correoDuplicado", summary = "Correo repetido",
                                            value = EJ_CORREO_DUPLICADO)
                            }))
    })
    @PostMapping("/con-estudiante")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'PROFESOR')")
    public ResponseEntity<EstudianteConMatriculaResponseDTO> registrarYMatricular(
            @Valid @RequestBody MatriculaConEstudianteRequestDTO dto) {
        var resultado = matriculaService.registrarYMatricular(dto.estudiante(), dto.asignaturaId(), dto.semestreId());
        var matriculaDTO = matriculaMapper.toResponseDTO(resultado.matricula());
        var respuesta = new EstudianteConMatriculaResponseDTO(resultado.estudiante(), matriculaDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(respuesta);
    }

}
