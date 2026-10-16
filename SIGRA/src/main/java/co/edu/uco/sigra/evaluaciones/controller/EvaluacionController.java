package co.edu.uco.sigra.evaluaciones.controller;

import co.edu.uco.sigra.auth.security.CurrentUser;
import co.edu.uco.sigra.evaluaciones.dto.EvaluacionActualizarRequestDTO;
import co.edu.uco.sigra.evaluaciones.dto.EvaluacionCrearRequestDTO;
import co.edu.uco.sigra.evaluaciones.dto.EvaluacionResponseDTO;
import co.edu.uco.sigra.evaluaciones.dto.EvaluacionesRaResumenDTO;
import co.edu.uco.sigra.evaluaciones.service.EvaluacionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
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

/** Gestión de evaluaciones por RA (RF-10/RF-11), únicamente para profesores asignados. */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class EvaluacionController {

    private final EvaluacionService evaluacionService;
    private final CurrentUser currentUser;

    @PostMapping("/resultados-aprendizaje/{raId}/evaluaciones")
    @PreAuthorize("hasRole('PROFESOR')")
    public ResponseEntity<EvaluacionResponseDTO> crear(@PathVariable UUID raId,
                                                        @Valid @RequestBody EvaluacionCrearRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(evaluacionService.crear(raId, dto, currentUser.required()));
    }

    @GetMapping("/resultados-aprendizaje/{raId}/evaluaciones")
    @PreAuthorize("hasRole('PROFESOR')")
    public ResponseEntity<EvaluacionesRaResumenDTO> consultarPorResultadoAprendizaje(
            @PathVariable UUID raId, @RequestParam(required = false) UUID semestreId) {
        return ResponseEntity.ok(evaluacionService.consultarPorResultadoAprendizaje(
                raId, semestreId, currentUser.required()));
    }

    @GetMapping("/evaluaciones/{id}")
    @PreAuthorize("hasRole('PROFESOR')")
    public ResponseEntity<EvaluacionResponseDTO> consultarPorId(@PathVariable UUID id) {
        return ResponseEntity.ok(evaluacionService.consultarPorId(id, currentUser.required()));
    }

    @PutMapping("/evaluaciones/{id}")
    @PreAuthorize("hasRole('PROFESOR')")
    public ResponseEntity<EvaluacionResponseDTO> modificar(@PathVariable UUID id,
                                                            @Valid @RequestBody EvaluacionActualizarRequestDTO dto) {
        return ResponseEntity.ok(evaluacionService.modificar(id, dto, currentUser.required()));
    }

    @PatchMapping("/evaluaciones/{id}/inactivar")
    @PreAuthorize("hasRole('PROFESOR')")
    public ResponseEntity<EvaluacionResponseDTO> inactivar(@PathVariable UUID id) {
        return ResponseEntity.ok(evaluacionService.inactivar(id, currentUser.required()));
    }

    @PatchMapping("/evaluaciones/{id}/reactivar")
    @PreAuthorize("hasRole('PROFESOR')")
    public ResponseEntity<EvaluacionResponseDTO> reactivar(@PathVariable UUID id) {
        return ResponseEntity.ok(evaluacionService.reactivar(id, currentUser.required()));
    }
}
