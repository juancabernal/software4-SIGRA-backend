package co.edu.uco.sigra.auditoria.controller;

import co.edu.uco.sigra.auditoria.dto.PaginaAuditoriaDTO;
import co.edu.uco.sigra.auditoria.entity.TipoEntidadAuditoria;
import co.edu.uco.sigra.auditoria.service.AuditoriaService;
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
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@Validated
@RestController
@RequestMapping("/api/v1/auditoria")
@RequiredArgsConstructor
@Tag(name = "Auditoría", description = "RF-19 / RF-21c: historial de auditoría de solo inserción. Rol requerido: ADMINISTRADOR.")
@SecurityRequirement(name = OpenApiConfig.ESQUEMA_BEARER)
public class AuditoriaController {

    public static final int TAMANO_MAXIMO = 100;

    private static final String EJEMPLO_RANGO_INVALIDO_400 =
            "{\"timestamp\":\"2026-10-14T12:00:00-05:00\",\"status\":400,\"error\":\"BAD_REQUEST\","
                    + "\"mensaje\":\"La fecha inicial del filtro no puede ser posterior a la fecha final.\"}";

    private static final String EJEMPLO_PAGINA = "{"
            + "\"contenido\":["
            + "{\"id\":\"4b64b64b-4b64-4b64-8b64-4b64b64b64b6\","
            + "\"fechaHora\":\"2026-10-20T14:35:12.123\","
            + "\"accion\":\"LOGIN_EXITOSO\",\"tipoEntidad\":\"USUARIO\","
            + "\"idEntidad\":\"4b64b64b-4b64-4b64-8b64-4b64b64b64b6\","
            + "\"usuarioId\":\"4b64b64b-4b64-4b64-8b64-4b64b64b64b6\","
            + "\"rolUsuario\":\"ADMINISTRADOR\","
            + "\"detalle\":\"Inicio de sesión exitoso como ADMINISTRADOR\"},"
            + "{\"id\":\"5c75c75c-5c75-5c75-9c75-5c75c75c75c7\","
            + "\"fechaHora\":\"2026-10-20T14:30:08.012\","
            + "\"accion\":\"LOGIN_FALLIDO\",\"tipoEntidad\":\"USUARIO\","
            + "\"idEntidad\":null,\"usuarioId\":null,\"rolUsuario\":null,"
            + "\"detalle\":\"Inicio de sesión rechazado: credenciales inválidas\"}] ,"
            + "\"pagina\":0,\"tamano\":20,\"totalElementos\":2,\"totalPaginas\":1}";

    private final AuditoriaService auditoriaService;

    @Operation(summary = "Consultar historial de auditoría",
            description = "Consulta el historial general de eventos registrados por el sistema, ordenados del más reciente "
                    + "al más antiguo. Incluye LOGIN_EXITOSO, LOGIN_FALLIDO, CAMBIO_ESTADO, "
                    + "CALIFICACION_MODIFICADA y CALIFICACION_FUERA_DE_FECHA. Los eventos de login exitoso guardan el "
                    + "identificador del usuario, su rol, fecha y hora, tipo de entidad, acción y detalle; en un login "
                    + "rechazado, idEntidad, usuarioId y rolUsuario pueden ser nulos intencionalmente. El endpoint es un "
                    + "historial de auditoría general, no un recurso separado exclusivo para sesiones. Permite filtrar "
                    + "por fechas inclusivas y tipo de entidad, y pagina desde 0 con entre 1 y " + TAMANO_MAXIMO + " registros.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Consulta correcta; resultados ordenados del evento más reciente al más antiguo",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = PaginaAuditoriaDTO.class),
                            examples = @ExampleObject(name = "historialDeLogin", summary = "Login exitoso y rechazado",
                                    value = EJEMPLO_PAGINA))),
            @ApiResponse(responseCode = "400", description = "Fecha mal formada, rango invertido, tipo de entidad desconocido o paginación fuera de rango",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = RespuestaErrorDoc.class),
                            examples = {
                                    @ExampleObject(name = "tipoEntidadInvalido", summary = "Enum desconocido",
                                            value = EjemplosError.TIPO_ENTIDAD_INVALIDO_400),
                                    @ExampleObject(name = "rangoDeFechasInvalido", summary = "Rango invertido",
                                            value = EJEMPLO_RANGO_INVALIDO_400)
                            })),
            @ApiResponse(responseCode = "401", description = "Falta el JWT, es inválido o expiró",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = RespuestaErrorDoc.class),
                            examples = @ExampleObject(name = "noAutenticado", summary = "JWT ausente o inválido",
                                    value = EjemplosError.NO_AUTENTICADO_401))),
            @ApiResponse(responseCode = "403", description = "El JWT es válido, pero el usuario no tiene el rol ADMINISTRADOR",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = RespuestaErrorDoc.class),
                            examples = @ExampleObject(name = "rolInsuficiente", summary = "Acceso reservado al administrador",
                                    value = EjemplosError.PROHIBIDO_403)))
    })
    @GetMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<PaginaAuditoriaDTO> consultar(
            @Parameter(name = "desde", description = "Fecha inicial inclusiva del filtro, en formato YYYY-MM-DD.", example = "2026-10-01",
                    schema = @Schema(type = "string", format = "date"))
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @Parameter(name = "hasta", description = "Fecha final inclusiva del filtro, en formato YYYY-MM-DD.", example = "2026-10-20",
                    schema = @Schema(type = "string", format = "date"))
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @Parameter(name = "tipoEntidad", description = "Tipo de entidad auditada. USUARIO incluye LOGIN_EXITOSO, LOGIN_FALLIDO y cualquier otro evento de usuario.",
                    example = "USUARIO", schema = @Schema(implementation = TipoEntidadAuditoria.class))
            @RequestParam(required = false) TipoEntidadAuditoria tipoEntidad,
            @Parameter(name = "pagina", description = "Número de página, empezando en 0.", example = "0",
                    schema = @Schema(type = "integer", format = "int32", minimum = "0", defaultValue = "0"))
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "La página no puede ser negativa") int pagina,
            @Parameter(name = "tamano", description = "Cantidad de registros por página, entre 1 y " + TAMANO_MAXIMO + ".", example = "20",
                    schema = @Schema(type = "integer", format = "int32", minimum = "1", maximum = "100", defaultValue = "20"))
            @RequestParam(defaultValue = "20")
            @Min(value = 1, message = "El tamaño de página debe ser al menos 1")
            @Max(value = TAMANO_MAXIMO, message = "El tamaño de página no puede superar 100") int tamano) {
        return ResponseEntity.ok(auditoriaService.consultar(desde, hasta, tipoEntidad, pagina, tamano));
    }
}
