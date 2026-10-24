package co.edu.uco.sigra.evaluaciones.controller;

import co.edu.uco.sigra.auth.security.CurrentUser;
import co.edu.uco.sigra.common.openapi.EjemplosError;
import co.edu.uco.sigra.common.openapi.RespuestaErrorDoc;
import co.edu.uco.sigra.config.OpenApiConfig;
import co.edu.uco.sigra.evaluaciones.dto.EvaluacionActualizarRequestDTO;
import co.edu.uco.sigra.evaluaciones.dto.EvaluacionCrearRequestDTO;
import co.edu.uco.sigra.evaluaciones.dto.EvaluacionResponseDTO;
import co.edu.uco.sigra.evaluaciones.dto.EvaluacionesRaResumenDTO;
import co.edu.uco.sigra.evaluaciones.exception.NombreEvaluacionDuplicadoException;
import co.edu.uco.sigra.evaluaciones.service.EvaluacionService;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Tag(name = "Evaluaciones",
        description = "Creación, consulta, modificación, inactivación y reactivación de evaluaciones por resultado de "
                + "aprendizaje (RF-10, RF-11). Todos los endpoints requieren el rol PROFESOR con asignación docente "
                + "ACTIVA en la asignatura del RA.")
@SecurityRequirement(name = OpenApiConfig.ESQUEMA_BEARER)
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class EvaluacionController {

    private static final String ID_EJEMPLO = "3f2b8c1d-6a4e-4b9f-a7d5-1c0e9f8a7b6d";
    private static final String RA_EJEMPLO = "1a2b3c4d-5e6f-4a7b-8c9d-0e1f2a3b4c5d";
    private static final String SEMESTRE_EJEMPLO = "9d8c7b6a-5f4e-4d3c-b2a1-0f9e8d7c6b5a";
    private static final String TS = EjemplosError.TIMESTAMP_EJEMPLO;
    private static final String P400 = "{\"timestamp\":\"" + TS + "\",\"status\":400,\"error\":\"BAD_REQUEST\",\"mensaje\":\"";
    private static final String P404 = "{\"timestamp\":\"" + TS + "\",\"status\":404,\"error\":\"NOT_FOUND\",\"mensaje\":\"";
    private static final String FIN = "\"}";

    // Ejemplos propios del módulo; los mensajes son los que producen sus excepciones.
    private static final String EJ_VALIDACION =
            "{\"timestamp\":\"" + TS + "\",\"status\":400,\"error\":\"BAD_REQUEST\","
                    + "\"mensaje\":\"Los datos de entrada no cumplen con las validaciones requeridas\","
                    + "\"detalles\":[\"porcentaje: El porcentaje debe ser mayor que 0 y máximo 100\"]}";
    private static final String EJ_EVALUACION_NO_ENCONTRADA = P404 + "No existe una evaluación con id " + ID_EJEMPLO + "." + FIN;
    private static final String EJ_RA_NO_ENCONTRADO = P404 + "No existe un resultado de aprendizaje con id " + RA_EJEMPLO + "." + FIN;
    private static final String EJ_SEMESTRE_NO_ENCONTRADO = P404 + "No existe un semestre con id " + SEMESTRE_EJEMPLO + "." + FIN;
    private static final String EJ_NOMBRE_DUPLICADO =
            "{\"timestamp\":\"" + TS + "\",\"status\":409,\"error\":\"CONFLICT\","
                    + "\"mensaje\":\"" + NombreEvaluacionDuplicadoException.MENSAJE + "\"}";
    private static final String EJ_PORCENTAJE_EXCEDIDO = P400
            + "La suma de porcentajes de las evaluaciones activas de este resultado de aprendizaje en el semestre superaría el 100%. Porcentaje disponible: 40%." + FIN;
    private static final String EJ_FECHA_NO_POSTERIOR = P400 + "La fecha tentativa debe ser posterior a la fecha de hoy." + FIN;
    private static final String EJ_FECHA_FUERA_SEMESTRE = P400 + "La fecha tentativa debe estar dentro del semestre 2026-2 (entre 2026-08-03 y 2026-12-05)." + FIN;
    private static final String EJ_RA_INACTIVO = P400 + "El resultado de aprendizaje está inactivo; no se pueden crear ni reactivar evaluaciones." + FIN;
    private static final String EJ_ASIGNATURA_NO_ACTIVA = P400 + "La asignatura no está activa; no se pueden crear ni reactivar evaluaciones." + FIN;
    private static final String EJ_SIN_SEMESTRE_ACTIVO = P400 + "No hay un semestre activo; no se pueden crear evaluaciones." + FIN;
    private static final String EJ_CAMPOS_BLOQUEADOS = P400
            + "La evaluación ya tiene calificaciones registradas: no se pueden modificar la fecha tentativa ni el porcentaje, solo el nombre y la descripción." + FIN;
    private static final String EJ_MODIFICAR_INACTIVA = P400 + "No se puede modificar una evaluación inactiva." + FIN;
    private static final String EJ_SEMESTRE_NO_ACTIVO = P400 + "La evaluación pertenece a un semestre que no está activo; solo se puede consultar." + FIN;
    private static final String EJ_YA_ACTIVA = P400 + "La evaluación ya se encuentra activa." + FIN;
    private static final String EJ_YA_INACTIVA = P400 + "La evaluación ya se encuentra inactiva." + FIN;

    private final EvaluacionService evaluacionService;
    private final CurrentUser currentUser;

    @Operation(
            summary = "Crear evaluación",
            description = "Crea una evaluación ACTIVA para el RA en el semestre activo (RF-10a, RF-11). El nombre tiene máximo 30 caracteres y es "
                    + "único por RA y semestre sin distinguir mayúsculas; el porcentaje es mayor que 0 y máximo 100, con hasta 2 decimales; "
                    + "la descripción tiene máximo 100. La fecha tentativa debe ser posterior a hoy y estar dentro del semestre activo. "
                    + "La suma de porcentajes de las evaluaciones activas del RA en el semestre no puede superar 100%. El RA y la asignatura "
                    + "deben estar activos."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Evaluación creada",
                        content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                schema = @Schema(implementation = EvaluacionResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Datos inválidos, o regla de negocio incumplida: suma mayor a 100%, fecha fuera del semestre o no posterior a hoy, RA inactivo, asignatura no activa o sin semestre activo",
                        content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                schema = @Schema(implementation = RespuestaErrorDoc.class),
                                examples = {
                                                @ExampleObject(name = "validacion", summary = "Validación del cuerpo",
                                                value = EJ_VALIDACION),
                                                @ExampleObject(name = "porcentajeExcedido", summary = "Suma mayor a 100%",
                                                value = EJ_PORCENTAJE_EXCEDIDO),
                                                @ExampleObject(name = "fechaNoPosterior", summary = "Fecha no posterior a hoy",
                                                value = EJ_FECHA_NO_POSTERIOR),
                                                @ExampleObject(name = "fechaFueraDelSemestre", summary = "Fecha fuera del semestre",
                                                value = EJ_FECHA_FUERA_SEMESTRE),
                                                @ExampleObject(name = "raInactivo", summary = "RA inactivo",
                                                value = EJ_RA_INACTIVO),
                                                @ExampleObject(name = "asignaturaNoActiva", summary = "Asignatura no activa",
                                                value = EJ_ASIGNATURA_NO_ACTIVA),
                                                @ExampleObject(name = "sinSemestreActivo", summary = "Sin semestre activo",
                                                value = EJ_SIN_SEMESTRE_ACTIVO)})),
            @ApiResponse(responseCode = "401", description = "Falta el token o es inválido o expiró",
                        content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                schema = @Schema(implementation = RespuestaErrorDoc.class),
                                examples = @ExampleObject(value = EjemplosError.NO_AUTENTICADO_401))),
            @ApiResponse(responseCode = "403", description = "El usuario no tiene el rol PROFESOR, o es un PROFESOR sin asignación docente ACTIVA en la asignatura del RA",
                        content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                schema = @Schema(implementation = RespuestaErrorDoc.class),
                                examples = @ExampleObject(value = EjemplosError.PROHIBIDO_403))),
            @ApiResponse(responseCode = "404", description = "El resultado de aprendizaje no existe",
                        content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                schema = @Schema(implementation = RespuestaErrorDoc.class),
                                examples = @ExampleObject(value = EJ_RA_NO_ENCONTRADO))),
            @ApiResponse(responseCode = "409", description = "Ya existe una evaluación con ese nombre para el RA en el semestre",
                        content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                schema = @Schema(implementation = RespuestaErrorDoc.class),
                                examples = @ExampleObject(value = EJ_NOMBRE_DUPLICADO)))
    })
    @PostMapping("/resultados-aprendizaje/{raId}/evaluaciones")
    @PreAuthorize("hasRole('PROFESOR')")
    public ResponseEntity<EvaluacionResponseDTO> crear(
            @Parameter(description = "Identificador (UUID) del resultado de aprendizaje", example = RA_EJEMPLO)
            @PathVariable UUID raId,
            @Valid @RequestBody EvaluacionCrearRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(evaluacionService.crear(raId, dto, currentUser.required()));
    }

    @Operation(
            summary = "Consultar las evaluaciones de un RA",
            description = "Lista las evaluaciones del RA en un semestre, con el porcentaje asignado (suma de las activas) y el disponible hasta 100. "
                    + "Sin «semestreId» usa el semestre activo."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Evaluaciones del RA con su resumen de porcentajes",
                        content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                schema = @Schema(implementation = EvaluacionesRaResumenDTO.class))),
            @ApiResponse(responseCode = "400", description = "Alguno de los identificadores no es un UUID válido",
                        content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                schema = @Schema(implementation = RespuestaErrorDoc.class),
                                examples = @ExampleObject(value = EjemplosError.PARAMETRO_INVALIDO_400))),
            @ApiResponse(responseCode = "401", description = "Falta el token o es inválido o expiró",
                        content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                schema = @Schema(implementation = RespuestaErrorDoc.class),
                                examples = @ExampleObject(value = EjemplosError.NO_AUTENTICADO_401))),
            @ApiResponse(responseCode = "403", description = "El usuario no tiene el rol PROFESOR, o es un PROFESOR sin asignación docente ACTIVA en la asignatura del RA",
                        content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                schema = @Schema(implementation = RespuestaErrorDoc.class),
                                examples = @ExampleObject(value = EjemplosError.PROHIBIDO_403))),
            @ApiResponse(responseCode = "404", description = "El resultado de aprendizaje o el semestre indicado no existen",
                        content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                schema = @Schema(implementation = RespuestaErrorDoc.class),
                                examples = {
                                                @ExampleObject(name = "ra", summary = "RA inexistente",
                                                value = EJ_RA_NO_ENCONTRADO),
                                                @ExampleObject(name = "semestre", summary = "Semestre inexistente",
                                                value = EJ_SEMESTRE_NO_ENCONTRADO)}))
    })
    @GetMapping("/resultados-aprendizaje/{raId}/evaluaciones")
    @PreAuthorize("hasRole('PROFESOR')")
    public ResponseEntity<EvaluacionesRaResumenDTO> consultarPorResultadoAprendizaje(
            @Parameter(description = "Identificador (UUID) del resultado de aprendizaje", example = RA_EJEMPLO)
            @PathVariable UUID raId,
            @Parameter(description = "Semestre a consultar (UUID). Sin valor se usa el semestre activo", example = SEMESTRE_EJEMPLO)
            @RequestParam(required = false) UUID semestreId) {
        return ResponseEntity.ok(evaluacionService.consultarPorResultadoAprendizaje(
                raId, semestreId, currentUser.required()));
    }

    @Operation(
            summary = "Consultar una evaluación por id",
            description = "Devuelve una evaluación e indica con «tieneCalificaciones» si su fecha y su porcentaje están bloqueados (RF-10b)."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Evaluación",
                        content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                schema = @Schema(implementation = EvaluacionResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "El identificador no es un UUID válido",
                        content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                schema = @Schema(implementation = RespuestaErrorDoc.class),
                                examples = @ExampleObject(value = EjemplosError.PARAMETRO_INVALIDO_400))),
            @ApiResponse(responseCode = "401", description = "Falta el token o es inválido o expiró",
                        content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                schema = @Schema(implementation = RespuestaErrorDoc.class),
                                examples = @ExampleObject(value = EjemplosError.NO_AUTENTICADO_401))),
            @ApiResponse(responseCode = "403", description = "El usuario no tiene el rol PROFESOR, o es un PROFESOR sin asignación docente ACTIVA en la asignatura de la evaluación",
                        content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                schema = @Schema(implementation = RespuestaErrorDoc.class),
                                examples = @ExampleObject(value = EjemplosError.PROHIBIDO_403))),
            @ApiResponse(responseCode = "404", description = "La evaluación no existe",
                        content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                schema = @Schema(implementation = RespuestaErrorDoc.class),
                                examples = @ExampleObject(value = EJ_EVALUACION_NO_ENCONTRADA)))
    })
    @GetMapping("/evaluaciones/{id}")
    @PreAuthorize("hasRole('PROFESOR')")
    public ResponseEntity<EvaluacionResponseDTO> consultarPorId(
            @Parameter(description = "Identificador (UUID) de la evaluación", example = ID_EJEMPLO)
            @PathVariable UUID id) {
        return ResponseEntity.ok(evaluacionService.consultarPorId(id, currentUser.required()));
    }

    @Operation(
            summary = "Modificar una evaluación",
            description = "Edita la evaluación (RF-10b). Se envían todos los campos. Si ya tiene calificaciones solo se pueden cambiar el nombre y "
                    + "la descripción: la fecha y el porcentaje deben llegar iguales. Si cambia la fecha debe ser posterior a hoy y estar dentro "
                    + "de su semestre, y la suma de porcentajes de las activas del RA no puede superar 100%. No se modifica una evaluación "
                    + "INACTIVA ni de un semestre que ya no está activo."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Evaluación modificada",
                        content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                schema = @Schema(implementation = EvaluacionResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Datos inválidos, o regla de negocio incumplida: campos bloqueados por calificaciones, evaluación inactiva, semestre no activo, suma mayor a 100% o fecha inválida",
                        content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                schema = @Schema(implementation = RespuestaErrorDoc.class),
                                examples = {
                                                @ExampleObject(name = "validacion", summary = "Validación del cuerpo",
                                                value = EJ_VALIDACION),
                                                @ExampleObject(name = "camposBloqueados", summary = "Con calificaciones registradas",
                                                value = EJ_CAMPOS_BLOQUEADOS),
                                                @ExampleObject(name = "evaluacionInactiva", summary = "Evaluación inactiva",
                                                value = EJ_MODIFICAR_INACTIVA),
                                                @ExampleObject(name = "semestreNoActivo", summary = "Semestre no activo",
                                                value = EJ_SEMESTRE_NO_ACTIVO),
                                                @ExampleObject(name = "porcentajeExcedido", summary = "Suma mayor a 100%",
                                                value = EJ_PORCENTAJE_EXCEDIDO),
                                                @ExampleObject(name = "fechaNoPosterior", summary = "Fecha no posterior a hoy",
                                                value = EJ_FECHA_NO_POSTERIOR)})),
            @ApiResponse(responseCode = "401", description = "Falta el token o es inválido o expiró",
                        content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                schema = @Schema(implementation = RespuestaErrorDoc.class),
                                examples = @ExampleObject(value = EjemplosError.NO_AUTENTICADO_401))),
            @ApiResponse(responseCode = "403", description = "El usuario no tiene el rol PROFESOR, o es un PROFESOR sin asignación docente ACTIVA en la asignatura de la evaluación",
                        content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                schema = @Schema(implementation = RespuestaErrorDoc.class),
                                examples = @ExampleObject(value = EjemplosError.PROHIBIDO_403))),
            @ApiResponse(responseCode = "404", description = "La evaluación no existe",
                        content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                schema = @Schema(implementation = RespuestaErrorDoc.class),
                                examples = @ExampleObject(value = EJ_EVALUACION_NO_ENCONTRADA))),
            @ApiResponse(responseCode = "409", description = "Ya existe otra evaluación con ese nombre para el RA en el semestre",
                        content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                schema = @Schema(implementation = RespuestaErrorDoc.class),
                                examples = @ExampleObject(value = EJ_NOMBRE_DUPLICADO)))
    })
    @PutMapping("/evaluaciones/{id}")
    @PreAuthorize("hasRole('PROFESOR')")
    public ResponseEntity<EvaluacionResponseDTO> modificar(
            @Parameter(description = "Identificador (UUID) de la evaluación", example = ID_EJEMPLO)
            @PathVariable UUID id,
            @Valid @RequestBody EvaluacionActualizarRequestDTO dto) {
        return ResponseEntity.ok(evaluacionService.modificar(id, dto, currentUser.required()));
    }

    @Operation(
            summary = "Inactivar una evaluación",
            description = "Eliminación lógica (RF-10c): la evaluación queda INACTIVA y conserva las calificaciones ya registradas. Deja de sumar "
                    + "en el porcentaje del RA. Solo aplica al semestre activo."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Evaluación inactivada",
                        content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                schema = @Schema(implementation = EvaluacionResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "El identificador no es válido, la evaluación ya está inactiva o su semestre no está activo",
                        content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                schema = @Schema(implementation = RespuestaErrorDoc.class),
                                examples = {
                                                @ExampleObject(name = "yaInactiva", summary = "Ya inactiva",
                                                value = EJ_YA_INACTIVA),
                                                @ExampleObject(name = "semestreNoActivo", summary = "Semestre no activo",
                                                value = EJ_SEMESTRE_NO_ACTIVO)})),
            @ApiResponse(responseCode = "401", description = "Falta el token o es inválido o expiró",
                        content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                schema = @Schema(implementation = RespuestaErrorDoc.class),
                                examples = @ExampleObject(value = EjemplosError.NO_AUTENTICADO_401))),
            @ApiResponse(responseCode = "403", description = "El usuario no tiene el rol PROFESOR, o es un PROFESOR sin asignación docente ACTIVA en la asignatura de la evaluación",
                        content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                schema = @Schema(implementation = RespuestaErrorDoc.class),
                                examples = @ExampleObject(value = EjemplosError.PROHIBIDO_403))),
            @ApiResponse(responseCode = "404", description = "La evaluación no existe",
                        content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                schema = @Schema(implementation = RespuestaErrorDoc.class),
                                examples = @ExampleObject(value = EJ_EVALUACION_NO_ENCONTRADA)))
    })
    @PatchMapping("/evaluaciones/{id}/inactivar")
    @PreAuthorize("hasRole('PROFESOR')")
    public ResponseEntity<EvaluacionResponseDTO> inactivar(
            @Parameter(description = "Identificador (UUID) de la evaluación", example = ID_EJEMPLO)
            @PathVariable UUID id) {
        return ResponseEntity.ok(evaluacionService.inactivar(id, currentUser.required()));
    }

    @Operation(
            summary = "Reactivar una evaluación",
            description = "Vuelve a dejar ACTIVA una evaluación inactiva. Se rechaza si el RA está inactivo, si la asignatura no está activa, "
                    + "si el semestre ya no está activo o si, al sumarla, el RA superaría 100%."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Evaluación reactivada",
                        content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                schema = @Schema(implementation = EvaluacionResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "El identificador no es válido o no se puede reactivar: ya está activa, RA inactivo, asignatura no activa, semestre no activo o suma mayor a 100%",
                        content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                schema = @Schema(implementation = RespuestaErrorDoc.class),
                                examples = {
                                                @ExampleObject(name = "yaActiva", summary = "Ya activa",
                                                value = EJ_YA_ACTIVA),
                                                @ExampleObject(name = "raInactivo", summary = "RA inactivo",
                                                value = EJ_RA_INACTIVO),
                                                @ExampleObject(name = "asignaturaNoActiva", summary = "Asignatura no activa",
                                                value = EJ_ASIGNATURA_NO_ACTIVA),
                                                @ExampleObject(name = "semestreNoActivo", summary = "Semestre no activo",
                                                value = EJ_SEMESTRE_NO_ACTIVO),
                                                @ExampleObject(name = "porcentajeExcedido", summary = "Suma mayor a 100%",
                                                value = EJ_PORCENTAJE_EXCEDIDO)})),
            @ApiResponse(responseCode = "401", description = "Falta el token o es inválido o expiró",
                        content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                schema = @Schema(implementation = RespuestaErrorDoc.class),
                                examples = @ExampleObject(value = EjemplosError.NO_AUTENTICADO_401))),
            @ApiResponse(responseCode = "403", description = "El usuario no tiene el rol PROFESOR, o es un PROFESOR sin asignación docente ACTIVA en la asignatura de la evaluación",
                        content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                schema = @Schema(implementation = RespuestaErrorDoc.class),
                                examples = @ExampleObject(value = EjemplosError.PROHIBIDO_403))),
            @ApiResponse(responseCode = "404", description = "La evaluación no existe",
                        content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                schema = @Schema(implementation = RespuestaErrorDoc.class),
                                examples = @ExampleObject(value = EJ_EVALUACION_NO_ENCONTRADA)))
    })
    @PatchMapping("/evaluaciones/{id}/reactivar")
    @PreAuthorize("hasRole('PROFESOR')")
    public ResponseEntity<EvaluacionResponseDTO> reactivar(
            @Parameter(description = "Identificador (UUID) de la evaluación", example = ID_EJEMPLO)
            @PathVariable UUID id) {
        return ResponseEntity.ok(evaluacionService.reactivar(id, currentUser.required()));
    }
}
