package co.edu.uco.sigra.reportes.controller;

import co.edu.uco.sigra.common.openapi.EjemplosError;
import co.edu.uco.sigra.common.openapi.RespuestaErrorDoc;
import co.edu.uco.sigra.config.OpenApiConfig;
import co.edu.uco.sigra.reportes.dto.MatriculasProgramaSemestreReporteResponseDTO;
import co.edu.uco.sigra.reportes.service.MatriculasProgramaSemestreReporteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Tag(name = "Reportes", description = "Reportes académicos agregados para coordinación académica")
@SecurityRequirement(name = OpenApiConfig.ESQUEMA_BEARER)
@RestController
@RequestMapping("/api/v1/reportes")
@RequiredArgsConstructor
public class ReporteMatriculasProgramaSemestreController {

    private final ObjectProvider<MatriculasProgramaSemestreReporteService> service;

    @Operation(summary = "R2 - Matrículas por programa y semestre",
            description = "Devuelve únicamente conteos de matrículas agrupados por semestre y programa.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Reporte generado",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = MatriculasProgramaSemestreReporteResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Algún identificador no es un UUID válido",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = RespuestaErrorDoc.class),
                            examples = @ExampleObject(value = EjemplosError.PARAMETRO_INVALIDO_400))),
            @ApiResponse(responseCode = "401", description = "Falta el token o es inválido",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = RespuestaErrorDoc.class),
                            examples = @ExampleObject(value = EjemplosError.NO_AUTENTICADO_401))),
            @ApiResponse(responseCode = "403", description = "El rol no tiene permiso",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = RespuestaErrorDoc.class),
                            examples = @ExampleObject(value = EjemplosError.PROHIBIDO_403))),
            @ApiResponse(responseCode = "404", description = "El semestre o programa indicado no existe",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = RespuestaErrorDoc.class)))
    })
    @GetMapping("/matriculas-programa-semestre")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<MatriculasProgramaSemestreReporteResponseDTO> consultar(
            @Parameter(description = "Filtra por semestre (UUID)") @RequestParam(required = false) UUID semestreId,
            @Parameter(description = "Filtra por programa académico (UUID)") @RequestParam(required = false) UUID programaId,
            @Parameter(description = "Filtra por facultad del programa (UUID)") @RequestParam(required = false) UUID facultadId) {
        return ResponseEntity.ok(service.getObject().consultar(semestreId, programaId, facultadId));
    }
}
