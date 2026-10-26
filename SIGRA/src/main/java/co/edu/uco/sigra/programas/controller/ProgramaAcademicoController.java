package co.edu.uco.sigra.programas.controller;

import co.edu.uco.sigra.common.enums.EstadoRegistro;
import co.edu.uco.sigra.common.openapi.EjemplosError;
import co.edu.uco.sigra.common.openapi.RespuestaErrorDoc;
import co.edu.uco.sigra.config.OpenApiConfig;
import co.edu.uco.sigra.programas.dto.ProgramaRequestDTO;
import co.edu.uco.sigra.programas.dto.ProgramaResponseDTO;
import co.edu.uco.sigra.programas.dto.ProgramaUpdateDTO;
import co.edu.uco.sigra.programas.service.ProgramaService;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Gestión de programas académicos (RF-02): registrar, consultar, modificar el nombre e inactivar.
 * El código es inmutable después de creado. Los errores de dominio los atiende ProgramaExceptionHandler.
 * Las reglas @PreAuthorize aplican la matriz RBAC de RF-05.
 */
@Tag(name = "Programas",
        description = "Registro, consulta, modificación del nombre e inactivación de programas académicos "
                + "(RF-02). Todos los endpoints requieren el rol ADMINISTRADOR.")
@SecurityRequirement(name = OpenApiConfig.ESQUEMA_BEARER)
@RestController
@RequestMapping("/api/v1/programas")
@RequiredArgsConstructor
public class ProgramaAcademicoController {

    private static final String ID_EJEMPLO = "1a2b3c4d-5e6f-4a7b-8c9d-0e1f2a3b4c5d";
    private static final String TS = EjemplosError.TIMESTAMP_EJEMPLO;

    // Ejemplos propios del módulo; los mensajes son los que producen las excepciones de programas.
    private static final String EJ_PROGRAMA_NO_ENCONTRADO =
            "{\"timestamp\":\"" + TS + "\",\"status\":404,\"error\":\"NOT_FOUND\","
                    + "\"mensaje\":\"No existe un programa académico con el identificador " + ID_EJEMPLO + "\"}";

    private static final String EJ_NOMBRE_DUPLICADO =
            "{\"timestamp\":\"" + TS + "\",\"status\":409,\"error\":\"CONFLICT\","
                    + "\"mensaje\":\"Ya existe un programa académico con el nombre 'Ingeniería de Sistemas'\"}";

    private static final String EJ_CODIGO_DUPLICADO =
            "{\"timestamp\":\"" + TS + "\",\"status\":409,\"error\":\"CONFLICT\","
                    + "\"mensaje\":\"Ya existe un programa académico con el código 'ING-SIS'\"}";

    private static final String EJ_ASIGNATURAS_ACTIVAS =
            "{\"timestamp\":\"" + TS + "\",\"status\":409,\"error\":\"CONFLICT\","
                    + "\"mensaje\":\"No se puede inactivar el programa 'Ingeniería de Sistemas' porque tiene asignaturas activas\"}";

    private final ProgramaService programaService;

    @Operation(
            summary = "Registrar programa académico",
            description = "Crea un programa ACTIVO. El nombre y el código no pueden repetirse (sin distinguir "
                    + "mayúsculas); el código queda fijo y no se puede modificar después de creado."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Programa registrado",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ProgramaResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Datos inválidos: nombre o código vacíos, código fuera "
                    + "de 4 a 10 caracteres o con símbolos no permitidos, o cuerpo mal formado",
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
            @ApiResponse(responseCode = "409", description = "Ya existe un programa con ese nombre o con ese código",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = RespuestaErrorDoc.class),
                            examples = {
                                    @ExampleObject(name = "nombreDuplicado", summary = "Nombre repetido",
                                            value = EJ_NOMBRE_DUPLICADO),
                                    @ExampleObject(name = "codigoDuplicado", summary = "Código repetido",
                                            value = EJ_CODIGO_DUPLICADO)
                            }))
    })
    @PostMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<ProgramaResponseDTO> registrar(@Valid @RequestBody ProgramaRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(programaService.registrar(dto));
    }

    @Operation(
            summary = "Consultar programas",
            description = "Lista los programas, activos e inactivos. Los filtros «nombre» y «código» buscan por "
                    + "coincidencia parcial sin distinguir mayúsculas; «estado» filtra por ACTIVO o INACTIVO. "
                    + "Sin programas (o sin coincidencias) devuelve una lista vacía."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de programas (puede estar vacía)",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            array = @ArraySchema(schema = @Schema(implementation = ProgramaResponseDTO.class)))),
            @ApiResponse(responseCode = "400", description = "El parámetro 'estado' no es ACTIVO ni INACTIVO",
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
    @GetMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<List<ProgramaResponseDTO>> consultarProgramas(
            @Parameter(description = "Texto a buscar en el nombre (coincidencia parcial, ignora mayúsculas)", example = "sistemas")
            @RequestParam(required = false) String nombre,
            @Parameter(description = "Texto a buscar en el código (coincidencia parcial, ignora mayúsculas)", example = "ing")
            @RequestParam(required = false) String codigo,
            @Parameter(description = "Filtra por estado. Si se omite, devuelve todos", example = "ACTIVO")
            @RequestParam(required = false) EstadoRegistro estado) {
        return ResponseEntity.ok(programaService.consultar(nombre, codigo, estado));
    }

    @Operation(
            summary = "Consultar programa por id",
            description = "Devuelve un programa por su identificador."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Programa encontrado",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ProgramaResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "El id no es un UUID válido",
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
            @ApiResponse(responseCode = "404", description = "No existe un programa con ese id",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = RespuestaErrorDoc.class),
                            examples = @ExampleObject(value = EJ_PROGRAMA_NO_ENCONTRADO)))
    })
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<ProgramaResponseDTO> consultarPorId(
            @Parameter(description = "Identificador (UUID) del programa", example = ID_EJEMPLO)
            @PathVariable UUID id) {
        return ResponseEntity.ok(programaService.consultarPorId(id));
    }

    @Operation(
            summary = "Modificar nombre del programa",
            description = "Actualiza el nombre de un programa. El código no se puede modificar: es inmutable "
                    + "desde el registro. El nuevo nombre no puede estar en uso por otro programa."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Programa actualizado",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ProgramaResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Nombre vacío, demasiado largo, id que no es UUID o cuerpo mal formado",
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
            @ApiResponse(responseCode = "404", description = "No existe un programa con ese id",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = RespuestaErrorDoc.class),
                            examples = @ExampleObject(value = EJ_PROGRAMA_NO_ENCONTRADO))),
            @ApiResponse(responseCode = "409", description = "Ya existe otro programa con ese nombre",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = RespuestaErrorDoc.class),
                            examples = @ExampleObject(value = EJ_NOMBRE_DUPLICADO)))
    })
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<ProgramaResponseDTO> actualizarNombre(
            @Parameter(description = "Identificador (UUID) del programa", example = ID_EJEMPLO)
            @PathVariable UUID id,
            @Valid @RequestBody ProgramaUpdateDTO dto) {
        return ResponseEntity.ok(programaService.actualizarNombre(id, dto));
    }

    @Operation(
            summary = "Inactivar programa",
            description = "Cambia el estado del programa a INACTIVO sin borrar el registro. Es idempotente: "
                    + "si ya estaba INACTIVO, responde 200 sin cambios. Se rechaza si el programa tiene "
                    + "asignaturas activas asociadas."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Programa inactivado (o ya estaba inactivo)",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ProgramaResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "El id no es un UUID válido",
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
            @ApiResponse(responseCode = "404", description = "No existe un programa con ese id",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = RespuestaErrorDoc.class),
                            examples = @ExampleObject(value = EJ_PROGRAMA_NO_ENCONTRADO))),
            @ApiResponse(responseCode = "409", description = "El programa tiene asignaturas activas asociadas",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = RespuestaErrorDoc.class),
                            examples = @ExampleObject(value = EJ_ASIGNATURAS_ACTIVAS)))
    })
    @PatchMapping("/{id}/inactivar")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<ProgramaResponseDTO> inactivar(
            @Parameter(description = "Identificador (UUID) del programa", example = ID_EJEMPLO)
            @PathVariable UUID id) {
        return ResponseEntity.ok(programaService.inactivar(id));
    }
}
