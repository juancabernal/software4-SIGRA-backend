package co.edu.uco.sigra.estudiantes.controller;

import co.edu.uco.sigra.config.OpenApiConfig;
import co.edu.uco.sigra.estudiantes.dto.MiMateriaDTO;
import co.edu.uco.sigra.estudiantes.dto.MiMateriaDetalleDTO;
import co.edu.uco.sigra.estudiantes.service.MiProcesoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Tag(name = "Estudiantes", description = "Consultas de solo lectura del proceso académico propio del estudiante.")
@SecurityRequirement(name = OpenApiConfig.ESQUEMA_BEARER)
@RestController
@RequestMapping("/api/v1/estudiantes/me")
public class MiProcesoController {

    private final ObjectProvider<MiProcesoService> miProcesoService;

    public MiProcesoController(ObjectProvider<MiProcesoService> miProcesoService) {
        this.miProcesoService = miProcesoService;
    }

    @Operation(summary = "Consultar materias actuales",
            description = "Devuelve las materias EN_CURSO y PROGRAMADA del estudiante autenticado, agrupadas en ese orden. El nivel corresponde al último cálculo del sistema.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Materias en curso",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            array = @ArraySchema(schema = @Schema(implementation = MiMateriaDTO.class)))),
            @ApiResponse(responseCode = "401", description = "Falta el token o es inválido"),
            @ApiResponse(responseCode = "403", description = "El usuario no tiene el rol ESTUDIANTE")
    })
    @GetMapping("/materias")
    @PreAuthorize("hasRole('ESTUDIANTE')")
    public ResponseEntity<List<MiMateriaDTO>> materias() {
        return ResponseEntity.ok(miProcesoService.getObject().listarMaterias());
    }

    @Operation(summary = "Consultar detalle de una materia",
            description = "Devuelve el detalle de una matrícula propia en cualquier situación. El nivel corresponde al último cálculo del sistema.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Detalle de la materia",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = MiMateriaDetalleDTO.class))),
            @ApiResponse(responseCode = "400", description = "El identificador no es un UUID válido"),
            @ApiResponse(responseCode = "401", description = "Falta el token o es inválido"),
            @ApiResponse(responseCode = "403", description = "El usuario no tiene el rol ESTUDIANTE"),
            @ApiResponse(responseCode = "404", description = "La materia no existe en el registro académico del estudiante")
    })
    @GetMapping("/materias/{matriculaId}")
    @PreAuthorize("hasRole('ESTUDIANTE')")
    public ResponseEntity<MiMateriaDetalleDTO> materia(
            @Parameter(description = "Identificador de la matrícula", example = "8f7e6d5c-4b3a-4291-8c7d-6b5a4f3e2d1c")
            @PathVariable UUID matriculaId) {
        return ResponseEntity.ok(miProcesoService.getObject().consultarMateria(matriculaId));
    }

    @Operation(summary = "Consultar progreso académico",
            description = "Devuelve el detalle de todas las materias EN_CURSO del estudiante autenticado. El nivel corresponde al último cálculo del sistema.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Progreso académico",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            array = @ArraySchema(schema = @Schema(implementation = MiMateriaDetalleDTO.class)))),
            @ApiResponse(responseCode = "401", description = "Falta el token o es inválido"),
            @ApiResponse(responseCode = "403", description = "El usuario no tiene el rol ESTUDIANTE")
    })
    @GetMapping("/progreso")
    @PreAuthorize("hasRole('ESTUDIANTE')")
    public ResponseEntity<List<MiMateriaDetalleDTO>> progreso() {
        return ResponseEntity.ok(miProcesoService.getObject().consultarProgreso());
    }

    @Operation(summary = "Consultar historial académico",
            description = "Devuelve las materias CURSADA y DESVINCULADA del estudiante autenticado. El nivel corresponde al último cálculo del sistema.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Historial académico",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            array = @ArraySchema(schema = @Schema(implementation = MiMateriaDTO.class)))),
            @ApiResponse(responseCode = "401", description = "Falta el token o es inválido"),
            @ApiResponse(responseCode = "403", description = "El usuario no tiene el rol ESTUDIANTE")
    })
    @GetMapping("/historial")
    @PreAuthorize("hasRole('ESTUDIANTE')")
    public ResponseEntity<List<MiMateriaDTO>> historial() {
        return ResponseEntity.ok(miProcesoService.getObject().consultarHistorial());
    }
}
