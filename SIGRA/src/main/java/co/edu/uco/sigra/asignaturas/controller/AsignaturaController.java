package co.edu.uco.sigra.asignaturas.controller;

import co.edu.uco.sigra.asignaturas.dto.AsignaturaFiltroDTO;
import co.edu.uco.sigra.asignaturas.dto.AsignaturaRequestDTO;
import co.edu.uco.sigra.asignaturas.dto.AsignaturaResponseDTO;
import co.edu.uco.sigra.asignaturas.dto.AsignaturaUpdateDTO;
import co.edu.uco.sigra.asignaturas.dto.MiAsignaturaDTO;
import co.edu.uco.sigra.asignaturas.dto.ReglasEntrada;
import co.edu.uco.sigra.asignaturas.entity.EstadoAsignatura;
import co.edu.uco.sigra.auth.security.CurrentUser;
import co.edu.uco.sigra.auth.security.UsuarioPrincipal;
import co.edu.uco.sigra.asignaturas.service.AsignaturaService;
import co.edu.uco.sigra.common.openapi.EjemplosError;
import co.edu.uco.sigra.common.openapi.RespuestaErrorDoc;
import co.edu.uco.sigra.config.OpenApiConfig;
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
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Matriz de acceso RF-05. El rol se autoriza con Spring Security y el alcance del profesor se
 * comprueba en el servicio contra sus asignaciones docentes activas.
 * <p>
 * {@code @Validated} activa la validación de los parámetros de consulta: un valor fuera de rango
 * lanza ConstraintViolationException, que el manejador transversal traduce a 400.
 */
@Tag(name = "Asignaturas",
        description = "Registro, consulta, modificación, activación e inactivación de asignaturas (RF-03). "
                + "El rol requerido varía por endpoint: ver el detalle de cada uno.")
@SecurityRequirement(name = OpenApiConfig.ESQUEMA_BEARER)
@Validated
@RestController
@RequestMapping("/api/v1/asignaturas")
public class AsignaturaController {

    private static final String ID_EJEMPLO = "3c4d5e6f-7a8b-4c9d-0e1f-2a3b4c5d6e7f";
    private static final String PROGRAMA_ID_EJEMPLO = "4d5e6f7a-8b9c-4d0e-1f2a-3b4c5d6e7f8a";
    private static final String TS = EjemplosError.TIMESTAMP_EJEMPLO;

    // Ejemplos propios del módulo; los mensajes son los que producen las excepciones de asignaturas.
    private static final String EJ_ASIGNATURA_NO_ENCONTRADA =
            "{\"timestamp\":\"" + TS + "\",\"status\":404,\"error\":\"NOT_FOUND\","
                    + "\"mensaje\":\"No existe una asignatura con id " + ID_EJEMPLO + ".\"}";

    private static final String EJ_PROGRAMA_NO_ENCONTRADO =
            "{\"timestamp\":\"" + TS + "\",\"status\":404,\"error\":\"NOT_FOUND\","
                    + "\"mensaje\":\"No existe un programa académico con id " + PROGRAMA_ID_EJEMPLO + ".\"}";

    private static final String EJ_CODIGO_DUPLICADO =
            "{\"timestamp\":\"" + TS + "\",\"status\":409,\"error\":\"CONFLICT\","
                    + "\"mensaje\":\"Ya existe una asignatura con el código MAT-401.\"}";

    private static final String EJ_PROGRAMA_INACTIVO =
            "{\"timestamp\":\"" + TS + "\",\"status\":400,\"error\":\"BAD_REQUEST\","
                    + "\"mensaje\":\"El programa académico \\\"Ingeniería de Software\\\" está inactivo y no admite "
                    + "nuevas asignaturas.\"}";

    private static final String EJ_TRANSICION_INVALIDA_ACTIVAR =
            "{\"timestamp\":\"" + TS + "\",\"status\":409,\"error\":\"CONFLICT\","
                    + "\"mensaje\":\"No se puede activar una asignatura en estado ACTIVA. Solo una asignatura en "
                    + "estado BORRADOR o INACTIVA puede activarse.\"}";

    private static final String EJ_TRANSICION_INVALIDA_INACTIVAR =
            "{\"timestamp\":\"" + TS + "\",\"status\":409,\"error\":\"CONFLICT\","
                    + "\"mensaje\":\"No se puede inactivar una asignatura en estado BORRADOR. Solo se pueden "
                    + "inactivar asignaturas en estado ACTIVA.\"}";

    private static final String EJ_RANGO_RA_POCOS =
            "{\"timestamp\":\"" + TS + "\",\"status\":400,\"error\":\"BAD_REQUEST\","
                    + "\"mensaje\":\"Para activar la asignatura se requieren al menos 5 resultados de aprendizaje "
                    + "activos y actualmente tiene 3.\"}";

    private static final String EJ_RANGO_RA_MUCHOS =
            "{\"timestamp\":\"" + TS + "\",\"status\":400,\"error\":\"BAD_REQUEST\","
                    + "\"mensaje\":\"Una asignatura admite como máximo 7 resultados de aprendizaje activos y "
                    + "actualmente tiene 9.\"}";

    private static final String EJ_FILTRO_RANGO_INVALIDO =
            "{\"timestamp\":\"" + TS + "\",\"status\":400,\"error\":\"BAD_REQUEST\","
                    + "\"mensaje\":\"La cantidad mínima de RA no puede ser mayor que la máxima.\"}";

    private static final String EJ_PARAMETROS_INVALIDOS =
            "{\"timestamp\":\"" + TS + "\",\"status\":400,\"error\":\"BAD_REQUEST\","
                    + "\"mensaje\":\"Los parámetros de la petición no cumplen con las validaciones requeridas\","
                    + "\"detalles\":[\"raMin: " + ReglasEntrada.RA_NEGATIVO + "\"]}";

    private final AsignaturaService asignaturaService;
    private final CurrentUser currentUser;

    @Autowired
    public AsignaturaController(AsignaturaService asignaturaService, CurrentUser currentUser) {
        this.asignaturaService = asignaturaService;
        this.currentUser = currentUser;
    }

    /** Constructor reducido conservado para pruebas standalone; la identidad sigue siendo obligatoria. */
    public AsignaturaController(AsignaturaService asignaturaService) {
        this(asignaturaService, new CurrentUser());
    }

    @Operation(
            summary = "Registrar asignatura",
            description = "Crea una asignatura en estado BORRADOR dentro de un programa académico ACTIVO. El "
                    + "código debe ser único, solo admite letras sin tilde, números y guiones entre ellos, y se "
                    + "guarda en mayúsculas."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Asignatura registrada",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = AsignaturaResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Datos inválidos o el programa académico indicado está inactivo",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = RespuestaErrorDoc.class),
                            examples = {
                                    @ExampleObject(name = "validacion", summary = "Datos inválidos",
                                            value = EjemplosError.VALIDACION_400),
                                    @ExampleObject(name = "programaInactivo", summary = "Programa inactivo",
                                            value = EJ_PROGRAMA_INACTIVO)
                            })),
            @ApiResponse(responseCode = "401", description = "Falta el token o es inválido o expiró",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = RespuestaErrorDoc.class),
                            examples = @ExampleObject(value = EjemplosError.NO_AUTENTICADO_401))),
            @ApiResponse(responseCode = "403", description = "El usuario autenticado no tiene el rol ADMINISTRADOR",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = RespuestaErrorDoc.class),
                            examples = @ExampleObject(value = EjemplosError.PROHIBIDO_403))),
            @ApiResponse(responseCode = "404", description = "El programa académico indicado no existe",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = RespuestaErrorDoc.class),
                            examples = @ExampleObject(value = EJ_PROGRAMA_NO_ENCONTRADO))),
            @ApiResponse(responseCode = "409", description = "Ya existe una asignatura con ese código",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = RespuestaErrorDoc.class),
                            examples = @ExampleObject(value = EJ_CODIGO_DUPLICADO)))
    })
    @PostMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<AsignaturaResponseDTO> registrarAsignatura(@Valid @RequestBody AsignaturaRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(asignaturaService.registrarAsignatura(dto));
    }

    @Operation(
            summary = "Consultar asignaturas",
            description = "Lista las asignaturas visibles para el usuario autenticado: el ADMINISTRADOR ve todas; "
                    + "el PROFESOR solo ve las que tiene asignadas con asignación docente ACTIVA. Admite filtrar "
                    + "por texto (nombre o código, sin distinguir tildes), por programa, por estado y por un rango "
                    + "de cantidad de resultados de aprendizaje activos."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de asignaturas (puede estar vacía)",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            array = @ArraySchema(schema = @Schema(implementation = AsignaturaResponseDTO.class)))),
            @ApiResponse(responseCode = "400", description = "Un parámetro tiene formato inválido (programaId o estado), raMin o raMax están fuera de rango, o raMin es mayor que raMax",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = RespuestaErrorDoc.class),
                            examples = {
                                    @ExampleObject(name = "parametroInvalido", summary = "Formato inválido",
                                            value = EjemplosError.PARAMETRO_INVALIDO_400),
                                    @ExampleObject(name = "parametrosFueraDeRango", summary = "raMin o raMax fuera de rango",
                                            value = EJ_PARAMETROS_INVALIDOS),
                                    @ExampleObject(name = "rangoInvertido", summary = "raMin mayor que raMax",
                                            value = EJ_FILTRO_RANGO_INVALIDO)
                            })),
            @ApiResponse(responseCode = "401", description = "Falta el token o es inválido o expiró",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = RespuestaErrorDoc.class),
                            examples = @ExampleObject(value = EjemplosError.NO_AUTENTICADO_401))),
            @ApiResponse(responseCode = "403", description = "El usuario autenticado no tiene el rol ADMINISTRADOR ni PROFESOR",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = RespuestaErrorDoc.class),
                            examples = @ExampleObject(value = EjemplosError.PROHIBIDO_403)))
    })
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'PROFESOR')")
    public ResponseEntity<List<AsignaturaResponseDTO>> consultarAsignaturas(
            @Parameter(description = "Texto a buscar en el nombre o el código de la asignatura (ignora tildes)", example = "cálculo")
            @RequestParam(required = false)
            @Size(max = ReglasEntrada.TEXTO_BUSQUEDA_MAX, message = ReglasEntrada.TEXTO_LONGITUD) String texto,
            @Parameter(description = "Filtra por el identificador (UUID) del programa académico", example = PROGRAMA_ID_EJEMPLO)
            @RequestParam(required = false) UUID programaId,
            @Parameter(description = "Filtra por el estado de la asignatura", example = "ACTIVA")
            @RequestParam(required = false) EstadoAsignatura estado,
            @Parameter(description = "Cantidad mínima de resultados de aprendizaje activos (0 a 999)", example = "5")
            @RequestParam(required = false)
            @Min(value = 0, message = ReglasEntrada.RA_NEGATIVO)
            @Max(value = ReglasEntrada.RA_FILTRO_MAX, message = ReglasEntrada.RA_MAXIMO) Integer raMin,
            @Parameter(description = "Cantidad máxima de resultados de aprendizaje activos (0 a 999)", example = "7")
            @RequestParam(required = false)
            @Min(value = 0, message = ReglasEntrada.RA_NEGATIVO)
            @Max(value = ReglasEntrada.RA_FILTRO_MAX, message = ReglasEntrada.RA_MAXIMO) Integer raMax) {
        AsignaturaFiltroDTO filtro = new AsignaturaFiltroDTO(texto, programaId, estado, raMin, raMax);
        UsuarioPrincipal identidad = currentUser.required();
        return ResponseEntity.ok(asignaturaService.consultar(filtro, identidad));
    }

    /**
     * «Mis asignaturas» del PROFESOR (asignación docente ACTIVA) o del ESTUDIANTE (matrícula ACTIVA).
     * Siempre exige un token válido porque depende de quién pregunta.
     */
    @Operation(
            summary = "Consultar mis asignaturas",
            description = "Devuelve las asignaturas del usuario autenticado: para un PROFESOR, aquellas con "
                    + "asignación docente ACTIVA; para un ESTUDIANTE, aquellas con matrícula ACTIVA. Incluye la "
                    + "cantidad de resultados de aprendizaje activos, la cantidad de matrículas activas y los "
                    + "profesores asignados."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Asignaturas del usuario autenticado (puede estar vacía)",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            array = @ArraySchema(schema = @Schema(implementation = MiAsignaturaDTO.class)))),
            @ApiResponse(responseCode = "401", description = "Falta el token o es inválido o expiró",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = RespuestaErrorDoc.class),
                            examples = @ExampleObject(value = EjemplosError.NO_AUTENTICADO_401))),
            @ApiResponse(responseCode = "403", description = "El usuario autenticado no tiene el rol PROFESOR ni ESTUDIANTE",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = RespuestaErrorDoc.class),
                            examples = @ExampleObject(value = EjemplosError.PROHIBIDO_403)))
    })
    @GetMapping("/mias")
    @PreAuthorize("hasAnyRole('PROFESOR', 'ESTUDIANTE')")
    public ResponseEntity<List<MiAsignaturaDTO>> misAsignaturas() {
        return ResponseEntity.ok(asignaturaService.misAsignaturas(currentUser.required()));
    }

    @Operation(
            summary = "Obtener una asignatura",
            description = "Devuelve el detalle de una asignatura. El ADMINISTRADOR puede consultar cualquiera; "
                    + "un PROFESOR solo puede consultar las que tiene asignadas con asignación docente ACTIVA."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Asignatura encontrada",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = AsignaturaResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "El id no es un UUID",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = RespuestaErrorDoc.class),
                            examples = @ExampleObject(value = EjemplosError.PARAMETRO_INVALIDO_400))),
            @ApiResponse(responseCode = "401", description = "Falta el token o es inválido o expiró",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = RespuestaErrorDoc.class),
                            examples = @ExampleObject(value = EjemplosError.NO_AUTENTICADO_401))),
            @ApiResponse(responseCode = "403", description = "El usuario autenticado no tiene el rol ADMINISTRADOR ni PROFESOR, o es un PROFESOR consultando una asignatura fuera de su alcance",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = RespuestaErrorDoc.class),
                            examples = @ExampleObject(value = EjemplosError.PROHIBIDO_403))),
            @ApiResponse(responseCode = "404", description = "No existe una asignatura con ese id",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = RespuestaErrorDoc.class),
                            examples = @ExampleObject(value = EJ_ASIGNATURA_NO_ENCONTRADA)))
    })
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'PROFESOR')")
    public ResponseEntity<AsignaturaResponseDTO> obtenerAsignatura(
            @Parameter(description = "Identificador (UUID) de la asignatura", example = ID_EJEMPLO)
            @PathVariable UUID id) {
        UsuarioPrincipal identidad = currentUser.required();
        return ResponseEntity.ok(asignaturaService.obtenerAsignatura(id, identidad));
    }

    @Operation(
            summary = "Modificar asignatura",
            description = "Actualiza el nombre de una asignatura. El código y el programa académico son "
                    + "inmutables y no se pueden cambiar."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Asignatura actualizada",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = AsignaturaResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Datos inválidos o el id no es un UUID",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = RespuestaErrorDoc.class),
                            examples = {
                                    @ExampleObject(name = "validacion", summary = "Datos inválidos",
                                            value = EjemplosError.VALIDACION_400),
                                    @ExampleObject(name = "idInvalido", summary = "Id con formato inválido",
                                            value = EjemplosError.PARAMETRO_INVALIDO_400)
                            })),
            @ApiResponse(responseCode = "401", description = "Falta el token o es inválido o expiró",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = RespuestaErrorDoc.class),
                            examples = @ExampleObject(value = EjemplosError.NO_AUTENTICADO_401))),
            @ApiResponse(responseCode = "403", description = "El usuario autenticado no tiene el rol ADMINISTRADOR",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = RespuestaErrorDoc.class),
                            examples = @ExampleObject(value = EjemplosError.PROHIBIDO_403))),
            @ApiResponse(responseCode = "404", description = "No existe una asignatura con ese id",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = RespuestaErrorDoc.class),
                            examples = @ExampleObject(value = EJ_ASIGNATURA_NO_ENCONTRADA)))
    })
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<AsignaturaResponseDTO> modificarAsignatura(
            @Parameter(description = "Identificador (UUID) de la asignatura", example = ID_EJEMPLO)
            @PathVariable UUID id,
            @Valid @RequestBody AsignaturaUpdateDTO dto) {
        return ResponseEntity.ok(asignaturaService.modificarAsignatura(id, dto));
    }

    @Operation(
            summary = "Activar asignatura",
            description = "Cambia el estado de la asignatura a ACTIVA. Solo puede activarse una asignatura en "
                    + "estado BORRADOR o INACTIVA, y debe tener entre 5 y 7 resultados de aprendizaje activos. Al "
                    + "reactivar una asignatura INACTIVA, primero se restauran a ACTIVO los resultados de "
                    + "aprendizaje que se inactivaron junto con ella."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Asignatura activada",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = AsignaturaResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "El id no es un UUID, o la cantidad de resultados de aprendizaje activos está fuera del rango permitido",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = RespuestaErrorDoc.class),
                            examples = {
                                    @ExampleObject(name = "idInvalido", summary = "Id con formato inválido",
                                            value = EjemplosError.PARAMETRO_INVALIDO_400),
                                    @ExampleObject(name = "pocosRa", summary = "Menos de 5 RA activos",
                                            value = EJ_RANGO_RA_POCOS),
                                    @ExampleObject(name = "muchosRa", summary = "Más de 7 RA activos",
                                            value = EJ_RANGO_RA_MUCHOS)
                            })),
            @ApiResponse(responseCode = "401", description = "Falta el token o es inválido o expiró",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = RespuestaErrorDoc.class),
                            examples = @ExampleObject(value = EjemplosError.NO_AUTENTICADO_401))),
            @ApiResponse(responseCode = "403", description = "El usuario autenticado no tiene el rol ADMINISTRADOR",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = RespuestaErrorDoc.class),
                            examples = @ExampleObject(value = EjemplosError.PROHIBIDO_403))),
            @ApiResponse(responseCode = "404", description = "No existe una asignatura con ese id",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = RespuestaErrorDoc.class),
                            examples = @ExampleObject(value = EJ_ASIGNATURA_NO_ENCONTRADA))),
            @ApiResponse(responseCode = "409", description = "La asignatura ya está en estado ACTIVA",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = RespuestaErrorDoc.class),
                            examples = @ExampleObject(value = EJ_TRANSICION_INVALIDA_ACTIVAR)))
    })
    @PatchMapping("/{id}/activar")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<AsignaturaResponseDTO> activarAsignatura(
            @Parameter(description = "Identificador (UUID) de la asignatura", example = ID_EJEMPLO)
            @PathVariable UUID id) {
        return ResponseEntity.ok(asignaturaService.activar(id));
    }

    @Operation(
            summary = "Inactivar asignatura",
            description = "Cambia el estado de la asignatura a INACTIVA. Solo puede inactivarse una asignatura en "
                    + "estado ACTIVA. En la misma operación, sus resultados de aprendizaje activos pasan a "
                    + "INACTIVO (y sus evaluaciones activas también), sin borrar nada ni afectar matrículas ni "
                    + "calificaciones."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Asignatura inactivada",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = AsignaturaResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "El id no es un UUID",
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
            @ApiResponse(responseCode = "404", description = "No existe una asignatura con ese id",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = RespuestaErrorDoc.class),
                            examples = @ExampleObject(value = EJ_ASIGNATURA_NO_ENCONTRADA))),
            @ApiResponse(responseCode = "409", description = "La asignatura no está en estado ACTIVA",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = RespuestaErrorDoc.class),
                            examples = @ExampleObject(value = EJ_TRANSICION_INVALIDA_INACTIVAR)))
    })
    @PatchMapping("/{id}/inactivar")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<AsignaturaResponseDTO> inactivarAsignatura(
            @Parameter(description = "Identificador (UUID) de la asignatura", example = ID_EJEMPLO)
            @PathVariable UUID id) {
        return ResponseEntity.ok(asignaturaService.inactivar(id));
    }
}
