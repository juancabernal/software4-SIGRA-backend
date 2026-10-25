package co.edu.uco.sigra.estudiantes.controller;

import co.edu.uco.sigra.common.openapi.EjemplosError;
import co.edu.uco.sigra.common.openapi.RespuestaErrorDoc;
import co.edu.uco.sigra.config.OpenApiConfig;
import co.edu.uco.sigra.estudiantes.dto.AsignaturaDeEstudianteDTO;
import co.edu.uco.sigra.estudiantes.dto.EstudianteRequestDTO;
import co.edu.uco.sigra.estudiantes.dto.EstudianteResponseDTO;
import co.edu.uco.sigra.estudiantes.mapper.MatriculaMapper;
import co.edu.uco.sigra.estudiantes.service.EstudianteService;
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

@Tag(name = "Estudiantes",
        description = "Registro, consulta, modificación e inactivación de estudiantes (RF-09a). El registro y la "
                + "consulta general requieren el rol ADMINISTRADOR o PROFESOR; la modificación y la inactivación "
                + "son exclusivas de ADMINISTRADOR. La consulta por id y las asignaturas del estudiante también "
                + "están abiertas al propio ESTUDIANTE sobre su propia ficha, o a un PROFESOR con una matrícula "
                + "activa con ese estudiante.")
@SecurityRequirement(name = OpenApiConfig.ESQUEMA_BEARER)
@RestController
@RequestMapping("/api/v1/estudiantes")
@RequiredArgsConstructor
public class EstudianteController {

    private static final String ID_EJEMPLO = "4e3d2c1b-6a5f-4b8e-9d7c-1f0e9d8c7b6a";
    private static final String TIPO_DOCUMENTO_EJEMPLO = "6b1f9d7e-5c3a-4f21-9e8b-2d4a7c6e1f05";
    private static final String TS = EjemplosError.TIMESTAMP_EJEMPLO;

    // Ejemplos propios del módulo; los mensajes son los que producen las excepciones de estudiantes.
    private static final String EJ_ESTUDIANTE_NO_ENCONTRADO =
            "{\"timestamp\":\"" + TS + "\",\"status\":404,\"error\":\"NOT_FOUND\","
                    + "\"mensaje\":\"No existe un estudiante con id " + ID_EJEMPLO + ".\"}";

    private static final String EJ_TIPO_DOCUMENTO_NO_ENCONTRADO =
            "{\"timestamp\":\"" + TS + "\",\"status\":404,\"error\":\"NOT_FOUND\","
                    + "\"mensaje\":\"No existe un tipo de documento con id " + TIPO_DOCUMENTO_EJEMPLO + ".\"}";

    private static final String EJ_DOCUMENTO_DUPLICADO =
            "{\"timestamp\":\"" + TS + "\",\"status\":409,\"error\":\"CONFLICT\","
                    + "\"mensaje\":\"El estudiante con el documento 1098765432 ya está registrado en el sistema.\"}";

    private static final String EJ_CORREO_DUPLICADO =
            "{\"timestamp\":\"" + TS + "\",\"status\":409,\"error\":\"CONFLICT\","
                    + "\"mensaje\":\"Ya existe un estudiante registrado con el correo mariana.gomez@uco.net.co.\"}";

    private static final String EJ_DOCUMENTO_NO_MODIFICABLE =
            "{\"timestamp\":\"" + TS + "\",\"status\":400,\"error\":\"BAD_REQUEST\","
                    + "\"mensaje\":\"El tipo y el número de documento del estudiante no se pueden modificar "
                    + "después del registro.\"}";

    private final EstudianteService estudianteService;
    private final MatriculaService matriculaService;
    private final MatriculaMapper matriculaMapper;

    @Operation(
            summary = "Registrar estudiante",
            description = "Crea un estudiante ACTIVO. La cédula debe tener entre 6 y 10 dígitos y el correo debe "
                    + "ser del dominio @uco.net.co (se guarda en minúsculas y sin espacios). La cédula y el correo "
                    + "no pueden repetirse."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Estudiante registrado",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = EstudianteResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Datos inválidos: cédula fuera de 6 a 10 dígitos, correo fuera del dominio @uco.net.co, campos obligatorios vacíos o cuerpo mal formado",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = RespuestaErrorDoc.class),
                            examples = @ExampleObject(value = EjemplosError.VALIDACION_400))),
            @ApiResponse(responseCode = "401", description = "Falta el token o es inválido o expiró",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = RespuestaErrorDoc.class),
                            examples = @ExampleObject(value = EjemplosError.NO_AUTENTICADO_401))),
            @ApiResponse(responseCode = "403", description = "El usuario autenticado no tiene el rol ADMINISTRADOR ni PROFESOR",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = RespuestaErrorDoc.class),
                            examples = @ExampleObject(value = EjemplosError.PROHIBIDO_403))),
            @ApiResponse(responseCode = "404", description = "El tipo de documento indicado no existe",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = RespuestaErrorDoc.class),
                            examples = @ExampleObject(value = EJ_TIPO_DOCUMENTO_NO_ENCONTRADO))),
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
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'PROFESOR')")
    public ResponseEntity<EstudianteResponseDTO> registrar(@Valid @RequestBody EstudianteRequestDTO dto) {
        var creado = estudianteService.registrar(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(creado);
    }

    @Operation(
            summary = "Consultar estudiantes",
            description = "Lista estudiantes. Un ADMINISTRADOR ve todos, activos e inactivos. Un PROFESOR sin "
                    + "«filtro» ve su propio roster (estudiantes con los que tiene una matrícula activa); con "
                    + "«filtro» busca en todo el directorio de estudiantes ACTIVOS por nombre completo, número de "
                    + "documento o correo, sin distinguir mayúsculas ni tildes, para poder matricular a alguien con "
                    + "quien aún no tiene relación. Sin estudiantes (o sin coincidencias) devuelve una lista vacía."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de estudiantes (puede estar vacía)",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            array = @ArraySchema(schema = @Schema(implementation = EstudianteResponseDTO.class)))),
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
    public ResponseEntity<List<EstudianteResponseDTO>> consultar(
            @Parameter(description = "Texto a buscar en el nombre completo, el número de documento o el correo (ignora tildes)", example = "gómez")
            @RequestParam(required = false) String filtro) {
        return ResponseEntity.ok(estudianteService.consultar(filtro));
    }

    @Operation(
            summary = "Consultar un estudiante por id",
            description = "Devuelve la ficha del estudiante. Accesible para un ADMINISTRADOR, para el propio "
                    + "ESTUDIANTE sobre su propia ficha, o para un PROFESOR con una matrícula activa con ese "
                    + "estudiante."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Ficha del estudiante",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = EstudianteResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "El id no es un UUID válido",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = RespuestaErrorDoc.class),
                            examples = @ExampleObject(value = EjemplosError.PARAMETRO_INVALIDO_400))),
            @ApiResponse(responseCode = "401", description = "Falta el token o es inválido o expiró",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = RespuestaErrorDoc.class),
                            examples = @ExampleObject(value = EjemplosError.NO_AUTENTICADO_401))),
            @ApiResponse(responseCode = "403", description = "El usuario no tiene acceso a este estudiante: no es ADMINISTRADOR, no es el propio estudiante ni un PROFESOR con una matrícula activa con él",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = RespuestaErrorDoc.class),
                            examples = @ExampleObject(value = EjemplosError.PROHIBIDO_403))),
            @ApiResponse(responseCode = "404", description = "No existe un estudiante con ese id",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = RespuestaErrorDoc.class),
                            examples = @ExampleObject(value = EJ_ESTUDIANTE_NO_ENCONTRADO)))
    })
    @GetMapping("/{id}")
    @PreAuthorize("@securityAuthorization.canAccessStudent(#id, authentication)")
    public ResponseEntity<EstudianteResponseDTO> consultarPorId(
            @Parameter(description = "Identificador (UUID) del estudiante", example = ID_EJEMPLO)
            @PathVariable UUID id) {
        return ResponseEntity.ok(estudianteService.consultarPorId(id));
    }

    @Operation(
            summary = "Modificar estudiante",
            description = "Actualiza el nombre, el correo y el tipo de documento de un estudiante. El número de "
                    + "documento no se puede cambiar: debe enviarse el mismo que ya tiene registrado. El correo "
                    + "debe ser del dominio @uco.net.co y no puede estar en uso por otro usuario."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Estudiante actualizado",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = EstudianteResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Datos inválidos, id que no es UUID o intento de cambiar el número de documento",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = RespuestaErrorDoc.class),
                            examples = {
                                    @ExampleObject(name = "validacion", summary = "Datos inválidos",
                                            value = EjemplosError.VALIDACION_400),
                                    @ExampleObject(name = "documentoNoModificable", summary = "Cambio de número de documento",
                                            value = EJ_DOCUMENTO_NO_MODIFICABLE)
                            })),
            @ApiResponse(responseCode = "401", description = "Falta el token o es inválido o expiró",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = RespuestaErrorDoc.class),
                            examples = @ExampleObject(value = EjemplosError.NO_AUTENTICADO_401))),
            @ApiResponse(responseCode = "403", description = "El usuario autenticado no tiene el rol ADMINISTRADOR",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = RespuestaErrorDoc.class),
                            examples = @ExampleObject(value = EjemplosError.PROHIBIDO_403))),
            @ApiResponse(responseCode = "404", description = "No existe el estudiante o el tipo de documento indicado",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = RespuestaErrorDoc.class),
                            examples = {
                                    @ExampleObject(name = "estudianteNoEncontrado", summary = "Estudiante inexistente",
                                            value = EJ_ESTUDIANTE_NO_ENCONTRADO),
                                    @ExampleObject(name = "tipoDocumentoNoEncontrado", summary = "Tipo de documento inexistente",
                                            value = EJ_TIPO_DOCUMENTO_NO_ENCONTRADO)
                            })),
            @ApiResponse(responseCode = "409", description = "El correo ya pertenece a otro usuario",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = RespuestaErrorDoc.class),
                            examples = @ExampleObject(value = EJ_CORREO_DUPLICADO)))
    })
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<EstudianteResponseDTO> modificar(
            @Parameter(description = "Identificador (UUID) del estudiante", example = ID_EJEMPLO)
            @PathVariable UUID id,
            @Valid @RequestBody EstudianteRequestDTO dto) {
        return ResponseEntity.ok(estudianteService.modificar(id, dto));
    }

    @Operation(
            summary = "Inactivar estudiante",
            description = "Cambia el estado del estudiante a INACTIVO sin borrar el registro ni su historial de "
                    + "matrículas."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Estudiante inactivado; no devuelve cuerpo"),
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
            @ApiResponse(responseCode = "404", description = "No existe un estudiante con ese id",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = RespuestaErrorDoc.class),
                            examples = @ExampleObject(value = EJ_ESTUDIANTE_NO_ENCONTRADO)))
    })
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<Void> inactivar(
            @Parameter(description = "Identificador (UUID) del estudiante", example = ID_EJEMPLO)
            @PathVariable UUID id) {
        estudianteService.inactivar(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Consultar las asignaturas de un estudiante",
            description = "Devuelve el historial de matrículas del estudiante (activas e inactivas) con los datos "
                    + "de cada asignatura y semestre. Accesible para un ADMINISTRADOR, para el propio ESTUDIANTE, o "
                    + "para un PROFESOR con una matrícula activa con él."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Asignaturas del estudiante (puede estar vacía)",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            array = @ArraySchema(schema = @Schema(implementation = AsignaturaDeEstudianteDTO.class)))),
            @ApiResponse(responseCode = "400", description = "El id no es un UUID válido",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = RespuestaErrorDoc.class),
                            examples = @ExampleObject(value = EjemplosError.PARAMETRO_INVALIDO_400))),
            @ApiResponse(responseCode = "401", description = "Falta el token o es inválido o expiró",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = RespuestaErrorDoc.class),
                            examples = @ExampleObject(value = EjemplosError.NO_AUTENTICADO_401))),
            @ApiResponse(responseCode = "403", description = "El usuario no tiene acceso a este estudiante: no es ADMINISTRADOR, no es el propio estudiante ni un PROFESOR con una matrícula activa con él",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = RespuestaErrorDoc.class),
                            examples = @ExampleObject(value = EjemplosError.PROHIBIDO_403))),
            @ApiResponse(responseCode = "404", description = "No existe un estudiante con ese id",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = RespuestaErrorDoc.class),
                            examples = @ExampleObject(value = EJ_ESTUDIANTE_NO_ENCONTRADO)))
    })
    @GetMapping("/{id}/asignaturas")
    @PreAuthorize("@securityAuthorization.canAccessStudent(#id, authentication)")
    public ResponseEntity<List<AsignaturaDeEstudianteDTO>> consultarAsignaturas(
            @Parameter(description = "Identificador (UUID) del estudiante", example = ID_EJEMPLO)
            @PathVariable UUID id) {
        var matriculas = matriculaService.listarAsignaturasDeEstudiante(id);
        return ResponseEntity.ok(matriculaMapper.toAsignaturaDeEstudianteDTOList(matriculas));
    }

}
