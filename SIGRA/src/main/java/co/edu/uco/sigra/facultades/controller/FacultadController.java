package co.edu.uco.sigra.facultades.controller;

import co.edu.uco.sigra.common.enums.EstadoRegistro;
import co.edu.uco.sigra.common.openapi.EjemplosError;
import co.edu.uco.sigra.common.openapi.RespuestaErrorDoc;
import co.edu.uco.sigra.config.OpenApiConfig;
import co.edu.uco.sigra.facultades.dto.FacultadRequestDTO;
import co.edu.uco.sigra.facultades.dto.FacultadResponseDTO;
import co.edu.uco.sigra.facultades.service.FacultadService;
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
 * Gestión de facultades (incidencia #0000036): registrar, consultar, modificar el nombre e inactivar.
 * Los errores de dominio los atiende FacultadExceptionHandler.
 */
@Tag(name = "Facultades",
        description = "Registro, consulta, modificación del nombre e inactivación de facultades. "
                + "Todos los endpoints requieren el rol ADMINISTRADOR.")
@SecurityRequirement(name = OpenApiConfig.ESQUEMA_BEARER)
@RestController
@RequestMapping("/api/v1/facultades")
@RequiredArgsConstructor
public class FacultadController {

    private static final String ID_EJEMPLO = "7c8d9e0f-1a2b-4c3d-8e4f-5a6b7c8d9e0f";
    private static final String TS = EjemplosError.TIMESTAMP_EJEMPLO;

    private static final String EJ_FACULTAD_NO_ENCONTRADA =
            "{\"timestamp\":\"" + TS + "\",\"status\":404,\"error\":\"NOT_FOUND\","
                    + "\"mensaje\":\"No existe una facultad con el identificador " + ID_EJEMPLO + "\"}";

    private static final String EJ_NOMBRE_DUPLICADO =
            "{\"timestamp\":\"" + TS + "\",\"status\":409,\"error\":\"CONFLICT\","
                    + "\"mensaje\":\"Ya existe una facultad con el nombre 'Facultad de Ingeniería'\"}";

    private static final String EJ_PROGRAMAS_ACTIVOS =
            "{\"timestamp\":\"" + TS + "\",\"status\":409,\"error\":\"CONFLICT\","
                    + "\"mensaje\":\"No se puede inactivar la facultad 'Facultad de Ingeniería' porque tiene programas activos\"}";

    private final FacultadService facultadService;

    @Operation(
            summary = "Registrar facultad",
            description = "Crea una facultad ACTIVA. El nombre no puede repetirse (sin distinguir mayúsculas)."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Facultad registrada",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = FacultadResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Nombre vacío o de más de 150 caracteres, o cuerpo mal formado",
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
            @ApiResponse(responseCode = "409", description = "Ya existe una facultad con ese nombre",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = RespuestaErrorDoc.class),
                            examples = @ExampleObject(value = EJ_NOMBRE_DUPLICADO)))
    })
    @PostMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<FacultadResponseDTO> registrar(@Valid @RequestBody FacultadRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(facultadService.registrar(dto));
    }

    @Operation(
            summary = "Consultar facultades",
            description = "Lista las facultades, activas e inactivas, ordenadas por nombre. El filtro «nombre» busca por "
                    + "coincidencia parcial sin distinguir mayúsculas ni tildes; «estado» filtra por ACTIVO o INACTIVO. "
                    + "Sin facultades (o sin coincidencias) devuelve una lista vacía."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de facultades (puede estar vacía)",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            array = @ArraySchema(schema = @Schema(implementation = FacultadResponseDTO.class)))),
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
    public ResponseEntity<List<FacultadResponseDTO>> consultarFacultades(
            @Parameter(description = "Texto a buscar en el nombre (coincidencia parcial, ignora mayúsculas y tildes)", example = "ingenieria")
            @RequestParam(required = false) String nombre,
            @Parameter(description = "Filtra por estado. Si se omite, devuelve todas", example = "ACTIVO")
            @RequestParam(required = false) EstadoRegistro estado) {
        return ResponseEntity.ok(facultadService.consultar(nombre, estado));
    }

    @Operation(
            summary = "Consultar facultad por id",
            description = "Devuelve una facultad por su identificador."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Facultad encontrada",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = FacultadResponseDTO.class))),
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
            @ApiResponse(responseCode = "404", description = "No existe una facultad con ese id",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = RespuestaErrorDoc.class),
                            examples = @ExampleObject(value = EJ_FACULTAD_NO_ENCONTRADA)))
    })
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<FacultadResponseDTO> consultarPorId(
            @Parameter(description = "Identificador (UUID) de la facultad", example = ID_EJEMPLO)
            @PathVariable UUID id) {
        return ResponseEntity.ok(facultadService.consultarPorId(id));
    }

    @Operation(
            summary = "Modificar nombre de la facultad",
            description = "Actualiza el nombre de una facultad. El nuevo nombre no puede estar en uso por otra facultad."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Facultad actualizada",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = FacultadResponseDTO.class))),
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
            @ApiResponse(responseCode = "404", description = "No existe una facultad con ese id",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = RespuestaErrorDoc.class),
                            examples = @ExampleObject(value = EJ_FACULTAD_NO_ENCONTRADA))),
            @ApiResponse(responseCode = "409", description = "Ya existe otra facultad con ese nombre",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = RespuestaErrorDoc.class),
                            examples = @ExampleObject(value = EJ_NOMBRE_DUPLICADO)))
    })
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<FacultadResponseDTO> actualizarNombre(
            @Parameter(description = "Identificador (UUID) de la facultad", example = ID_EJEMPLO)
            @PathVariable UUID id,
            @Valid @RequestBody FacultadRequestDTO dto) {
        return ResponseEntity.ok(facultadService.actualizarNombre(id, dto));
    }

    @Operation(
            summary = "Inactivar facultad",
            description = "Cambia el estado de la facultad a INACTIVO sin borrar el registro. Es idempotente: "
                    + "si ya estaba INACTIVA, responde 200 sin cambios. Se rechaza si la facultad tiene "
                    + "programas activos asociados."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Facultad inactivada (o ya estaba inactiva)",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = FacultadResponseDTO.class))),
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
            @ApiResponse(responseCode = "404", description = "No existe una facultad con ese id",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = RespuestaErrorDoc.class),
                            examples = @ExampleObject(value = EJ_FACULTAD_NO_ENCONTRADA))),
            @ApiResponse(responseCode = "409", description = "La facultad tiene programas activos asociados",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = RespuestaErrorDoc.class),
                            examples = @ExampleObject(value = EJ_PROGRAMAS_ACTIVOS)))
    })
    @PatchMapping("/{id}/inactivar")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<FacultadResponseDTO> inactivar(
            @Parameter(description = "Identificador (UUID) de la facultad", example = ID_EJEMPLO)
            @PathVariable UUID id) {
        return ResponseEntity.ok(facultadService.inactivar(id));
    }
}
