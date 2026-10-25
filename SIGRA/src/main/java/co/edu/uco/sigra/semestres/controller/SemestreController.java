package co.edu.uco.sigra.semestres.controller;

import co.edu.uco.sigra.common.openapi.EjemplosError;
import co.edu.uco.sigra.common.openapi.RespuestaErrorDoc;
import co.edu.uco.sigra.config.OpenApiConfig;
import co.edu.uco.sigra.semestres.dto.SemestreFechaFinRequestDTO;
import co.edu.uco.sigra.semestres.dto.SemestreRequestDTO;
import co.edu.uco.sigra.semestres.dto.SemestreResponseDTO;
import co.edu.uco.sigra.semestres.service.SemestreService;
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
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;

/**
 * Las reglas @PreAuthorize aplican la matriz RBAC de RF-05. Ver Matriz RBAC del SRS.
 * Documentado con el patrón de docs/documentacion_openapi/swagger-profesores.md.
 */
@Tag(name = "Semestres",
        description = "Registro, consulta y extensión de la fecha de fin de los semestres académicos. "
                + "Crear y extender: ADMINISTRADOR. Listar y consultar: ADMINISTRADOR, PROFESOR y ESTUDIANTE.")
@SecurityRequirement(name = OpenApiConfig.ESQUEMA_BEARER)
@RestController
@RequestMapping("/api/v1/semestres")
@RequiredArgsConstructor
public class SemestreController {

    private static final String CODIGO_EJEMPLO = "2027-1";
    private static final String TS = EjemplosError.TIMESTAMP_EJEMPLO;

    // Ejemplos propios del módulo; los mensajes son los que producen las excepciones de semestres.
    private static final String EJ_SEMESTRE_NO_ENCONTRADO =
            "{\"timestamp\":\"" + TS + "\",\"status\":404,\"error\":\"NOT_FOUND\","
                    + "\"mensaje\":\"No se encontró el semestre con código " + CODIGO_EJEMPLO + "\"}";

    private static final String EJ_CODIGO_DUPLICADO =
            "{\"timestamp\":\"" + TS + "\",\"status\":409,\"error\":\"CONFLICT\","
                    + "\"mensaje\":\"Ya existe un semestre con el código " + CODIGO_EJEMPLO + "\"}";

    private static final String EJ_SOLAPADO =
            "{\"timestamp\":\"" + TS + "\",\"status\":409,\"error\":\"CONFLICT\","
                    + "\"mensaje\":\"El rango de fechas se cruza con el de otro semestre registrado\"}";

    private static final String EJ_VALIDACION_CODIGO =
            "{\"timestamp\":\"" + TS + "\",\"status\":400,\"error\":\"BAD_REQUEST\","
                    + "\"mensaje\":\"Los datos de entrada no cumplen con las validaciones requeridas\","
                    + "\"detalles\":[\"codigo: El código debe tener el formato AAAA-1 o AAAA-2\"]}";

    private static final String EJ_VALIDACION_FECHA_FIN =
            "{\"timestamp\":\"" + TS + "\",\"status\":400,\"error\":\"BAD_REQUEST\","
                    + "\"mensaje\":\"Los datos de entrada no cumplen con las validaciones requeridas\","
                    + "\"detalles\":[\"fechaFin: La nueva fecha de fin es obligatoria\"]}";

    private static final String EJ_FECHA_MAL_FORMADA =
            "{\"timestamp\":\"" + TS + "\",\"status\":400,\"error\":\"BAD_REQUEST\","
                    + "\"mensaje\":\"El cuerpo de la petición no es válido. Las fechas deben tener formato AAAA-MM-DD\"}";

    private static final String EJ_FIN_ANTES_DE_INICIO =
            "{\"timestamp\":\"" + TS + "\",\"status\":400,\"error\":\"BAD_REQUEST\","
                    + "\"mensaje\":\"La fecha de fin debe ser posterior a la fecha de inicio\"}";

    private static final String EJ_INICIO_PASADO =
            "{\"timestamp\":\"" + TS + "\",\"status\":400,\"error\":\"BAD_REQUEST\","
                    + "\"mensaje\":\"La fecha de inicio debe ser hoy o una fecha futura\"}";

    private static final String EJ_ANIO_NO_COINCIDE =
            "{\"timestamp\":\"" + TS + "\",\"status\":400,\"error\":\"BAD_REQUEST\","
                    + "\"mensaje\":\"El año del código debe coincidir con el año de la fecha de inicio\"}";

    private static final String EJ_SEMESTRE_TERMINADO =
            "{\"timestamp\":\"" + TS + "\",\"status\":400,\"error\":\"BAD_REQUEST\","
                    + "\"mensaje\":\"No se puede modificar un semestre que ya terminó\"}";

    private static final String EJ_NO_EXTIENDE =
            "{\"timestamp\":\"" + TS + "\",\"status\":400,\"error\":\"BAD_REQUEST\","
                    + "\"mensaje\":\"La nueva fecha de fin debe ser posterior a la actual; solo se permite extender el semestre\"}";

    private final SemestreService semestreService;

    @Operation(
            summary = "Registrar semestre",
            description = "Crea un semestre académico. El código tiene el formato AAAA-1 o AAAA-2 y su año debe "
                    + "coincidir con el de la fecha de inicio. La fecha de inicio debe ser hoy o futura y la de fin, "
                    + "posterior a la de inicio. El rango no puede cruzarse, intersectarse ni contener a otro semestre "
                    + "(los extremos cuentan como cruce). El estado se calcula por fechas: ACTIVO si hoy está dentro "
                    + "del rango, INACTIVO en otro caso."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Semestre registrado",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = SemestreResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Datos inválidos: código fuera del formato AAAA-1/AAAA-2, campos vacíos, fecha mal formada, fecha de fin no posterior a la de inicio, fecha de inicio pasada o año del código distinto al de la fecha de inicio",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = RespuestaErrorDoc.class),
                            examples = {
                                    @ExampleObject(name = "validacion", summary = "Código con formato inválido",
                                            value = EJ_VALIDACION_CODIGO),
                                    @ExampleObject(name = "fechaMalFormada", summary = "Fecha que no es AAAA-MM-DD",
                                            value = EJ_FECHA_MAL_FORMADA),
                                    @ExampleObject(name = "finAntesDeInicio", summary = "Fecha de fin no posterior a la de inicio",
                                            value = EJ_FIN_ANTES_DE_INICIO),
                                    @ExampleObject(name = "inicioPasado", summary = "Fecha de inicio pasada",
                                            value = EJ_INICIO_PASADO),
                                    @ExampleObject(name = "anioNoCoincide", summary = "Año del código distinto al de la fecha de inicio",
                                            value = EJ_ANIO_NO_COINCIDE)
                            })),
            @ApiResponse(responseCode = "401", description = "Falta el token o es inválido o expiró",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = RespuestaErrorDoc.class),
                            examples = @ExampleObject(value = EjemplosError.NO_AUTENTICADO_401))),
            @ApiResponse(responseCode = "403", description = "El usuario autenticado no tiene el rol ADMINISTRADOR",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = RespuestaErrorDoc.class),
                            examples = @ExampleObject(value = EjemplosError.PROHIBIDO_403))),
            @ApiResponse(responseCode = "409", description = "Ya existe un semestre con ese código o el rango de fechas se cruza con otro semestre",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = RespuestaErrorDoc.class),
                            examples = {
                                    @ExampleObject(name = "codigoDuplicado", summary = "Código repetido",
                                            value = EJ_CODIGO_DUPLICADO),
                                    @ExampleObject(name = "solapado", summary = "Fechas cruzadas con otro semestre",
                                            value = EJ_SOLAPADO)
                            }))
    })
    @PostMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<SemestreResponseDTO> crearSemestre(@Valid @RequestBody SemestreRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(semestreService.crear(dto));
    }

    @Operation(
            summary = "Listar semestres",
            description = "Devuelve todos los semestres ordenados por fecha de inicio, del más reciente al más "
                    + "antiguo, sin paginación. El estado de cada uno se recalcula con la fecha actual antes de "
                    + "responder. Sin semestres registrados devuelve una lista vacía."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de semestres (puede estar vacía)",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            array = @ArraySchema(schema = @Schema(implementation = SemestreResponseDTO.class)))),
            @ApiResponse(responseCode = "401", description = "Falta el token o es inválido o expiró",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = RespuestaErrorDoc.class),
                            examples = @ExampleObject(value = EjemplosError.NO_AUTENTICADO_401))),
            @ApiResponse(responseCode = "403", description = "El usuario autenticado no tiene un rol válido",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = RespuestaErrorDoc.class),
                            examples = @ExampleObject(value = EjemplosError.PROHIBIDO_403)))
    })
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'PROFESOR', 'ESTUDIANTE')")
    public ResponseEntity<List<SemestreResponseDTO>> listarSemestres() {
        return ResponseEntity.ok(semestreService.listar());
    }

    @Operation(
            summary = "Consultar semestre por código",
            description = "Devuelve el semestre con el código indicado (por ejemplo, 2027-1), con su estado "
                    + "recalculado según la fecha actual."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Semestre encontrado",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = SemestreResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "Falta el token o es inválido o expiró",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = RespuestaErrorDoc.class),
                            examples = @ExampleObject(value = EjemplosError.NO_AUTENTICADO_401))),
            @ApiResponse(responseCode = "403", description = "El usuario autenticado no tiene un rol válido",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = RespuestaErrorDoc.class),
                            examples = @ExampleObject(value = EjemplosError.PROHIBIDO_403))),
            @ApiResponse(responseCode = "404", description = "No existe un semestre con ese código",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = RespuestaErrorDoc.class),
                            examples = @ExampleObject(value = EJ_SEMESTRE_NO_ENCONTRADO)))
    })
    @GetMapping("/{codigo}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'PROFESOR', 'ESTUDIANTE')")
    public ResponseEntity<SemestreResponseDTO> consultarSemestre(
            @Parameter(description = "Código del semestre (AAAA-1 o AAAA-2)", example = CODIGO_EJEMPLO)
            @PathVariable String codigo) {
        return ResponseEntity.ok(semestreService.consultarPorCodigo(codigo));
    }

    @Operation(
            summary = "Extender la fecha de fin de un semestre",
            description = "Única modificación permitida sobre un semestre: mover su fecha de fin a una fecha "
                    + "posterior a la actual (no se puede acortar). Se rechaza si el semestre ya terminó o si el "
                    + "nuevo rango se cruza con otro semestre. El estado se recalcula con las nuevas fechas."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Fecha de fin extendida",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = SemestreResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Fecha de fin vacía o mal formada, semestre ya terminado o nueva fecha de fin no posterior a la actual",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = RespuestaErrorDoc.class),
                            examples = {
                                    @ExampleObject(name = "validacion", summary = "Fecha de fin vacía",
                                            value = EJ_VALIDACION_FECHA_FIN),
                                    @ExampleObject(name = "fechaMalFormada", summary = "Fecha que no es AAAA-MM-DD",
                                            value = EJ_FECHA_MAL_FORMADA),
                                    @ExampleObject(name = "semestreTerminado", summary = "El semestre ya terminó",
                                            value = EJ_SEMESTRE_TERMINADO),
                                    @ExampleObject(name = "noExtiende", summary = "La nueva fecha no extiende el semestre",
                                            value = EJ_NO_EXTIENDE)
                            })),
            @ApiResponse(responseCode = "401", description = "Falta el token o es inválido o expiró",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = RespuestaErrorDoc.class),
                            examples = @ExampleObject(value = EjemplosError.NO_AUTENTICADO_401))),
            @ApiResponse(responseCode = "403", description = "El usuario autenticado no tiene el rol ADMINISTRADOR",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = RespuestaErrorDoc.class),
                            examples = @ExampleObject(value = EjemplosError.PROHIBIDO_403))),
            @ApiResponse(responseCode = "404", description = "No existe un semestre con ese código",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = RespuestaErrorDoc.class),
                            examples = @ExampleObject(value = EJ_SEMESTRE_NO_ENCONTRADO))),
            @ApiResponse(responseCode = "409", description = "El nuevo rango de fechas se cruza con otro semestre",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = RespuestaErrorDoc.class),
                            examples = @ExampleObject(value = EJ_SOLAPADO)))
    })
    @PatchMapping("/{codigo}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<SemestreResponseDTO> extenderFechaFin(
            @Parameter(description = "Código del semestre (AAAA-1 o AAAA-2)", example = CODIGO_EJEMPLO)
            @PathVariable String codigo,
            @Valid @RequestBody SemestreFechaFinRequestDTO dto) {
        return ResponseEntity.ok(semestreService.extenderFechaFin(codigo, dto));
    }
}
