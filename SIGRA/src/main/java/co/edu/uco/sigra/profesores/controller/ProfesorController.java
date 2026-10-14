package co.edu.uco.sigra.profesores.controller;

import co.edu.uco.sigra.common.enums.EstadoRegistro;
import co.edu.uco.sigra.common.openapi.EjemplosError;
import co.edu.uco.sigra.common.openapi.RespuestaErrorDoc;
import co.edu.uco.sigra.config.OpenApiConfig;
import co.edu.uco.sigra.profesores.dto.AsignaturaProfesorResponseDTO;
import co.edu.uco.sigra.profesores.dto.ProfesorRequestDTO;
import co.edu.uco.sigra.profesores.dto.ProfesorResponseDTO;
import co.edu.uco.sigra.profesores.service.ProfesorService;
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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@Tag(name = "Profesores",
        description = "Registro, consulta, modificación e inactivación de profesores (RF-01). "
                + "Todos los endpoints requieren el rol ADMINISTRADOR.")
@SecurityRequirement(name = OpenApiConfig.ESQUEMA_BEARER)
@RestController
@RequestMapping("/api/v1/profesores")
@RequiredArgsConstructor
public class ProfesorController {

    private static final String ID_EJEMPLO = "1a2b3c4d-5e6f-4a7b-8c9d-0e1f2a3b4c5d";
    private static final String TS = EjemplosError.TIMESTAMP_EJEMPLO;

    // Ejemplos propios del módulo; los mensajes son los que producen las excepciones de profesores.
    private static final String EJ_PROFESOR_NO_ENCONTRADO =
            "{\"timestamp\":\"" + TS + "\",\"status\":404,\"error\":\"NOT_FOUND\","
                    + "\"mensaje\":\"No se encontró el profesor con id " + ID_EJEMPLO + "\"}";

    private static final String EJ_TIPO_DOCUMENTO_NO_ENCONTRADO =
            "{\"timestamp\":\"" + TS + "\",\"status\":404,\"error\":\"NOT_FOUND\","
                    + "\"mensaje\":\"No se encontró el tipo de documento con id 6b1f9d7e-5c3a-4f21-9e8b-2d4a7c6e1f05\"}";

    private static final String EJ_DOCUMENTO_DUPLICADO =
            "{\"timestamp\":\"" + TS + "\",\"status\":409,\"error\":\"CONFLICT\","
                    + "\"mensaje\":\"Ya existe un profesor registrado con el documento 1098765432\"}";

    private static final String EJ_CORREO_DUPLICADO =
            "{\"timestamp\":\"" + TS + "\",\"status\":409,\"error\":\"CONFLICT\","
                    + "\"mensaje\":\"Ya existe un profesor registrado con el correo ana.gomez@uco.net.co\"}";

    private static final String EJ_CAMPO_NO_MODIFICABLE =
            "{\"timestamp\":\"" + TS + "\",\"status\":400,\"error\":\"BAD_REQUEST\","
                    + "\"mensaje\":\"El campo 'numeroDocumento' no puede modificarse una vez registrado\"}";

    private static final String EJ_ASIGNACIONES_ACTIVAS =
            "{\"timestamp\":\"" + TS + "\",\"status\":400,\"error\":\"BAD_REQUEST\","
                    + "\"mensaje\":\"No se puede inactivar el profesor " + ID_EJEMPLO
                    + " porque tiene asignaturas activas asociadas\"}";

    private final ProfesorService profesorService;

    @Operation(
            summary = "Registrar profesor",
            description = "Crea un profesor ACTIVO y disponible para asignarle asignaturas. La cédula debe tener "
                    + "entre 6 y 10 dígitos y el correo debe ser del dominio @uco.net.co (se guarda en minúsculas "
                    + "y sin espacios). La cédula y el correo no pueden repetirse."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Profesor registrado",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ProfesorResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Datos inválidos: cédula fuera de 6 a 10 dígitos, correo fuera del dominio @uco.net.co, campos obligatorios vacíos o cuerpo mal formado",
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
            @ApiResponse(responseCode = "404", description = "El tipo de documento indicado no existe",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = RespuestaErrorDoc.class),
                            examples = @ExampleObject(value = EJ_TIPO_DOCUMENTO_NO_ENCONTRADO))),
            @ApiResponse(responseCode = "409", description = "Ya existe un profesor con esa cédula o con ese correo",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = RespuestaErrorDoc.class),
                            examples = {
                                    @ExampleObject(name = "documentoDuplicado", summary = "Cédula repetida",
                                            value = EJ_DOCUMENTO_DUPLICADO),
                                    @ExampleObject(name = "correoDuplicado", summary = "Correo repetido",
                                            value = EJ_CORREO_DUPLICADO)
                            }))
    })
    @PostMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<ProfesorResponseDTO> registrarProfesor(@Valid @RequestBody ProfesorRequestDTO dto) {
        var creado = profesorService.registrarProfesor(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(creado);
    }

    @Operation(
            summary = "Consultar profesores",
            description = "Lista todos los profesores, activos e inactivos, con su estado. Si se envía «filtro», "
                    + "devuelve solo los que coinciden en el nombre completo o en el número de documento, sin "
                    + "distinguir mayúsculas. Sin profesores (o sin coincidencias) devuelve una lista vacía."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de profesores (puede estar vacía)",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            array = @ArraySchema(schema = @Schema(implementation = ProfesorResponseDTO.class)))),
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
    public ResponseEntity<List<ProfesorResponseDTO>> consultarProfesores(
            @Parameter(description = "Texto a buscar en el nombre completo o en el número de documento",
                    example = "gómez")
            @RequestParam(required = false) String filtro) {
        return ResponseEntity.ok(profesorService.consultar(filtro));
    }

    @Operation(
            summary = "Consultar las asignaturas de un profesor",
            description = "Devuelve las asignaciones docentes del profesor con los datos de cada asignatura. "
                    + "Sin «estado» devuelve todas (incluido el historial de un profesor inactivo); con «estado» "
                    + "devuelve solo las ACTIVO o solo las INACTIVO."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Asignaturas del profesor (puede estar vacía)",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            array = @ArraySchema(schema = @Schema(implementation = AsignaturaProfesorResponseDTO.class)))),
            @ApiResponse(responseCode = "400", description = "El id no es un UUID o el estado no es ACTIVO ni INACTIVO",
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
            @ApiResponse(responseCode = "404", description = "No existe un profesor con ese id",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = RespuestaErrorDoc.class),
                            examples = @ExampleObject(value = EJ_PROFESOR_NO_ENCONTRADO)))
    })
    @GetMapping("/{id}/asignaturas")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<List<AsignaturaProfesorResponseDTO>> consultarAsignaturasProfesor(
            @Parameter(description = "Identificador (UUID) del profesor", example = ID_EJEMPLO)
            @PathVariable UUID id,
            @Parameter(description = "Filtra las asignaciones por estado. Si se omite, devuelve todas",
                    example = "ACTIVO")
            @RequestParam(required = false) EstadoRegistro estado) {
        return ResponseEntity.ok(profesorService.consultarAsignaturas(id, estado));
    }

    @Operation(
            summary = "Modificar profesor",
            description = "Actualiza el nombre, el correo y el tipo de documento de un profesor. El número de "
                    + "documento no se puede cambiar: debe enviarse el mismo que ya tiene registrado. El correo "
                    + "debe ser del dominio @uco.net.co y no puede estar en uso por otro usuario."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Profesor actualizado",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ProfesorResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Datos inválidos, id que no es UUID o intento de cambiar el número de documento",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = RespuestaErrorDoc.class),
                            examples = {
                                    @ExampleObject(name = "validacion", summary = "Datos inválidos",
                                            value = EjemplosError.VALIDACION_400),
                                    @ExampleObject(name = "documentoNoModificable", summary = "Cambio de número de documento",
                                            value = EJ_CAMPO_NO_MODIFICABLE)
                            })),
            @ApiResponse(responseCode = "401", description = "Falta el token o es inválido o expiró",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = RespuestaErrorDoc.class),
                            examples = @ExampleObject(value = EjemplosError.NO_AUTENTICADO_401))),
            @ApiResponse(responseCode = "403", description = "El usuario autenticado no tiene el rol ADMINISTRADOR",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = RespuestaErrorDoc.class),
                            examples = @ExampleObject(value = EjemplosError.PROHIBIDO_403))),
            @ApiResponse(responseCode = "404", description = "No existe el profesor o el tipo de documento indicado",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = RespuestaErrorDoc.class),
                            examples = {
                                    @ExampleObject(name = "profesorNoEncontrado", summary = "Profesor inexistente",
                                            value = EJ_PROFESOR_NO_ENCONTRADO),
                                    @ExampleObject(name = "tipoDocumentoNoEncontrado", summary = "Tipo de documento inexistente",
                                            value = EJ_TIPO_DOCUMENTO_NO_ENCONTRADO)
                            })),
            @ApiResponse(responseCode = "409", description = "La cédula o el correo ya pertenecen a otro usuario",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = RespuestaErrorDoc.class),
                            examples = {
                                    @ExampleObject(name = "documentoDuplicado", summary = "Cédula repetida",
                                            value = EJ_DOCUMENTO_DUPLICADO),
                                    @ExampleObject(name = "correoDuplicado", summary = "Correo repetido",
                                            value = EJ_CORREO_DUPLICADO)
                            }))
    })
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<ProfesorResponseDTO> modificarProfesor(
            @Parameter(description = "Identificador (UUID) del profesor", example = ID_EJEMPLO)
            @PathVariable UUID id,
            @Valid @RequestBody ProfesorRequestDTO dto) {
        return ResponseEntity.ok(profesorService.modificarProfesor(id, dto));
    }

    @Operation(
            summary = "Inactivar profesor",
            description = "Cambia el estado del profesor a INACTIVO sin borrar el registro ni su historial. "
                    + "Se rechaza si el profesor tiene asignaturas activas asociadas. Un profesor inactivo "
                    + "conserva su historial de asignaturas."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Profesor inactivado; no devuelve cuerpo"),
            @ApiResponse(responseCode = "400", description = "El id no es un UUID o el profesor tiene asignaturas activas asociadas",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = RespuestaErrorDoc.class),
                            examples = {
                                    @ExampleObject(name = "asignacionesActivas", summary = "Tiene asignaturas activas",
                                            value = EJ_ASIGNACIONES_ACTIVAS),
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
            @ApiResponse(responseCode = "404", description = "No existe un profesor con ese id",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = RespuestaErrorDoc.class),
                            examples = @ExampleObject(value = EJ_PROFESOR_NO_ENCONTRADO)))
    })
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<Void> inactivarProfesor(
            @Parameter(description = "Identificador (UUID) del profesor", example = ID_EJEMPLO)
            @PathVariable UUID id) {
        profesorService.inactivar(id);
        return ResponseEntity.noContent().build();
    }

}