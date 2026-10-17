package co.edu.uco.sigra.asignaturas.controller;

import co.edu.uco.sigra.asignaturas.dto.AsignacionMasivaRequestDTO;
import co.edu.uco.sigra.asignaturas.dto.ResultadoAsignacionMasivaDTO;
import co.edu.uco.sigra.asignaturas.service.AsignacionDocenteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Asignación docente (RF-07): asigna varios profesores a varias asignaturas en una sola operación
 * y permite desasignar. Los errores de dominio los atiende AsignacionDocenteExceptionHandler.
 * El endpoint es administrativo y queda protegido por la misma capa RBAC que el resto del API.
 */
@RestController
@RequestMapping("/api/v1/asignaciones-docentes")
@RequiredArgsConstructor
public class AsignacionDocenteController {

    private final AsignacionDocenteService asignacionDocenteService;

    @PostMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<ResultadoAsignacionMasivaDTO> asignar(@Valid @RequestBody AsignacionMasivaRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(asignacionDocenteService.asignarMasivo(dto));
    }

    @PatchMapping("/{id}/desasignar")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<Void> desasignar(@PathVariable UUID id) {
        asignacionDocenteService.desasignar(id);
        return ResponseEntity.noContent().build();
    }
}
