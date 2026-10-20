package co.edu.uco.sigra.calificaciones.controller;

import co.edu.uco.sigra.auth.security.CurrentUser;
import co.edu.uco.sigra.calificaciones.dto.CalificacionModificarRequestDTO;
import co.edu.uco.sigra.calificaciones.dto.CalificacionRequestDTO;
import co.edu.uco.sigra.calificaciones.dto.CalificacionResponseDTO;
import co.edu.uco.sigra.calificaciones.dto.EvaluacionCalificableDTO;
import co.edu.uco.sigra.calificaciones.dto.HistorialCalificacionDTO;
import co.edu.uco.sigra.calificaciones.dto.PlanillaCalificacionesDTO;
import co.edu.uco.sigra.calificaciones.service.CalificacionService;
import co.edu.uco.sigra.config.OpenApiConfig;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Calificaciones", description = "RF-12 / RF-13: notas por evaluación, planilla del profesor e historial "
        + "de cambios. Rol requerido: PROFESOR con asignación docente activa en la asignatura.")
@SecurityRequirement(name = OpenApiConfig.ESQUEMA_BEARER)
public class CalificacionController {

    private final CalificacionService calificacionService;
    private final CurrentUser currentUser;

    @Operation(summary = "Evaluaciones de un RA en un semestre",
            description = "Lista las evaluaciones (activas e inactivas) que se pueden elegir en la planilla de notas.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Evaluaciones ordenadas por fecha tentativa"),
            @ApiResponse(responseCode = "400", description = "Falta un parámetro o no es un UUID válido"),
            @ApiResponse(responseCode = "401", description = "Falta el token o es inválido o expiró"),
            @ApiResponse(responseCode = "403", description = "No es PROFESOR o el RA no es de una asignatura a su cargo")
    })
    @GetMapping("/calificaciones/evaluaciones")
    @PreAuthorize("hasRole('PROFESOR') and @securityAuthorization.hasActiveAssignmentForResultado(#resultadoAprendizajeId, authentication)")
    public ResponseEntity<List<EvaluacionCalificableDTO>> listarEvaluaciones(
            @Parameter(description = "Id del resultado de aprendizaje") @RequestParam UUID resultadoAprendizajeId,
            @Parameter(description = "Id del semestre") @RequestParam UUID semestreId) {
        return ResponseEntity.ok(calificacionService.listarEvaluaciones(resultadoAprendizajeId, semestreId));
    }

    @Operation(summary = "Planilla de notas de una evaluación",
            description = "Una fila por estudiante matriculado en la asignatura y el semestre de la evaluación, con su "
                    + "nota si ya la tiene. Incluye las matrículas desvinculadas que conservan nota (solo lectura). "
                    + "Ordenada por nombre.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Planilla de la evaluación"),
            @ApiResponse(responseCode = "400", description = "El id no es un UUID válido"),
            @ApiResponse(responseCode = "401", description = "Falta el token o es inválido o expiró"),
            @ApiResponse(responseCode = "403", description = "No es PROFESOR o la asignatura no está a su cargo"),
            @ApiResponse(responseCode = "404", description = "La evaluación no existe")
    })
    @GetMapping("/evaluaciones/{evaluacionId}/calificaciones")
    @PreAuthorize("hasRole('PROFESOR')")
    public ResponseEntity<PlanillaCalificacionesDTO> consultarPlanilla(
            @Parameter(description = "Id de la evaluación") @PathVariable UUID evaluacionId) {
        return ResponseEntity.ok(calificacionService.consultarPlanilla(evaluacionId, currentUser.required()));
    }

    @Operation(summary = "Registrar la nota de un estudiante",
            description = "Nota entre 0.0 y 5.0 con un decimal y observación de 10 a 500 caracteres. La matrícula debe "
                    + "estar ACTIVA y ser de la misma asignatura y semestre de la evaluación, que debe estar ACTIVA. "
                    + "Una sola nota por (evaluación, estudiante). Si se registra después de la fecha tentativa, "
                    + "queda con fueraDeFecha = true. Deja el evento REGISTRO en el historial.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Nota registrada"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos, matrícula no activa, de otra asignatura "
                    + "u otro semestre, o evaluación inactiva"),
            @ApiResponse(responseCode = "401", description = "Falta el token o es inválido o expiró"),
            @ApiResponse(responseCode = "403", description = "No es PROFESOR o la asignatura no está a su cargo"),
            @ApiResponse(responseCode = "404", description = "La evaluación o la matrícula no existen"),
            @ApiResponse(responseCode = "409", description = "El estudiante ya tiene nota en esta evaluación")
    })
    @PostMapping("/evaluaciones/{evaluacionId}/calificaciones")
    @PreAuthorize("hasRole('PROFESOR')")
    public ResponseEntity<CalificacionResponseDTO> registrar(
            @Parameter(description = "Id de la evaluación") @PathVariable UUID evaluacionId,
            @Valid @RequestBody CalificacionRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(calificacionService.registrar(evaluacionId, dto, currentUser.required()));
    }

    @Operation(summary = "Modificar una nota",
            description = "Cambia la nota y la observación. Deja en el historial el valor anterior y el nuevo, quién y "
                    + "cuándo. Sin cambios reales no se registra evento. Si se modifica después de la fecha tentativa, "
                    + "la calificación queda con fueraDeFecha = true.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Nota modificada"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos, matrícula desvinculada o evaluación inactiva"),
            @ApiResponse(responseCode = "401", description = "Falta el token o es inválido o expiró"),
            @ApiResponse(responseCode = "403", description = "No es PROFESOR o la asignatura no está a su cargo"),
            @ApiResponse(responseCode = "404", description = "La calificación no existe")
    })
    @PutMapping("/calificaciones/{id}")
    @PreAuthorize("hasRole('PROFESOR')")
    public ResponseEntity<CalificacionResponseDTO> modificar(
            @Parameter(description = "Id de la calificación") @PathVariable UUID id,
            @Valid @RequestBody CalificacionModificarRequestDTO dto) {
        return ResponseEntity.ok(calificacionService.modificar(id, dto, currentUser.required()));
    }

    @Operation(summary = "Historial de una nota", description = "Eventos REGISTRO y MODIFICACION en orden cronológico.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Historial de la calificación"),
            @ApiResponse(responseCode = "400", description = "El id no es un UUID válido"),
            @ApiResponse(responseCode = "401", description = "Falta el token o es inválido o expiró"),
            @ApiResponse(responseCode = "403", description = "No es PROFESOR o la asignatura no está a su cargo"),
            @ApiResponse(responseCode = "404", description = "La calificación no existe")
    })
    @GetMapping("/calificaciones/{id}/historial")
    @PreAuthorize("hasRole('PROFESOR')")
    public ResponseEntity<List<HistorialCalificacionDTO>> consultarHistorial(
            @Parameter(description = "Id de la calificación") @PathVariable UUID id) {
        return ResponseEntity.ok(calificacionService.consultarHistorial(id, currentUser.required()));
    }
}