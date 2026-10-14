package co.edu.uco.sigra.evaluaciones.controller;

import co.edu.uco.sigra.asignaturas.seguridad.IdentidadActual;
import co.edu.uco.sigra.asignaturas.seguridad.RolesPermitidos;
import co.edu.uco.sigra.common.enums.RolUsuario;
import co.edu.uco.sigra.evaluaciones.dto.EvaluacionActualizarRequestDTO;
import co.edu.uco.sigra.evaluaciones.dto.EvaluacionCrearRequestDTO;
import co.edu.uco.sigra.evaluaciones.dto.EvaluacionResponseDTO;
import co.edu.uco.sigra.evaluaciones.dto.EvaluacionesRaResumenDTO;
import co.edu.uco.sigra.evaluaciones.service.EvaluacionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Gestión de evaluaciones por RA (RF-10 / RF-11). Según la matriz RBAC solo el PROFESOR gestiona
 * evaluaciones, y solo de sus asignaturas.
 * <p>
 * El control por rol es el compartido ({@link RolesPermitidos}), activo solo con
 * {@code sigra.seguridad.roles.habilitado=true}. Con el interruptor apagado no llega identidad y el
 * servicio no filtra por dueño, igual que en conclusiones.
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class EvaluacionController {

    private final EvaluacionService evaluacionService;

    @PostMapping("/resultados-aprendizaje/{raId}/evaluaciones")
    @RolesPermitidos(RolUsuario.PROFESOR)
    public ResponseEntity<EvaluacionResponseDTO> crear(
            @PathVariable UUID raId,
            @Valid @RequestBody EvaluacionCrearRequestDTO dto,
            @RequestAttribute(name = IdentidadActual.ATRIBUTO, required = false) IdentidadActual identidad) {
        return ResponseEntity.status(HttpStatus.CREATED).body(evaluacionService.crear(raId, dto, identidad));
    }

    /** Vista del profesor: evaluaciones del RA con el % acumulado. Sin semestreId, el semestre activo. */
    @GetMapping("/resultados-aprendizaje/{raId}/evaluaciones")
    @RolesPermitidos(RolUsuario.PROFESOR)
    public ResponseEntity<EvaluacionesRaResumenDTO> consultarPorResultadoAprendizaje(
            @PathVariable UUID raId,
            @RequestParam(required = false) UUID semestreId,
            @RequestAttribute(name = IdentidadActual.ATRIBUTO, required = false) IdentidadActual identidad) {
        return ResponseEntity.ok(evaluacionService.consultarPorResultadoAprendizaje(raId, semestreId, identidad));
    }

    @GetMapping("/evaluaciones/{id}")
    @RolesPermitidos(RolUsuario.PROFESOR)
    public ResponseEntity<EvaluacionResponseDTO> consultarPorId(
            @PathVariable UUID id,
            @RequestAttribute(name = IdentidadActual.ATRIBUTO, required = false) IdentidadActual identidad) {
        return ResponseEntity.ok(evaluacionService.consultarPorId(id, identidad));
    }

    @PutMapping("/evaluaciones/{id}")
    @RolesPermitidos(RolUsuario.PROFESOR)
    public ResponseEntity<EvaluacionResponseDTO> modificar(
            @PathVariable UUID id,
            @Valid @RequestBody EvaluacionActualizarRequestDTO dto,
            @RequestAttribute(name = IdentidadActual.ATRIBUTO, required = false) IdentidadActual identidad) {
        return ResponseEntity.ok(evaluacionService.modificar(id, dto, identidad));
    }

    @PatchMapping("/evaluaciones/{id}/inactivar")
    @RolesPermitidos(RolUsuario.PROFESOR)
    public ResponseEntity<EvaluacionResponseDTO> inactivar(
            @PathVariable UUID id,
            @RequestAttribute(name = IdentidadActual.ATRIBUTO, required = false) IdentidadActual identidad) {
        return ResponseEntity.ok(evaluacionService.inactivar(id, identidad));
    }

    @PatchMapping("/evaluaciones/{id}/reactivar")
    @RolesPermitidos(RolUsuario.PROFESOR)
    public ResponseEntity<EvaluacionResponseDTO> reactivar(
            @PathVariable UUID id,
            @RequestAttribute(name = IdentidadActual.ATRIBUTO, required = false) IdentidadActual identidad) {
        return ResponseEntity.ok(evaluacionService.reactivar(id, identidad));
    }
}
