package co.edu.uco.sigra.resultadosaprendizaje.controller;

import co.edu.uco.sigra.common.enums.EstadoRegistro;
import co.edu.uco.sigra.resultadosaprendizaje.dto.ResultadoAprendizajeActualizarRequestDTO;
import co.edu.uco.sigra.resultadosaprendizaje.dto.ResultadoAprendizajeCrearRequestDTO;
import co.edu.uco.sigra.resultadosaprendizaje.dto.ResultadoAprendizajeResponseDTO;
import co.edu.uco.sigra.resultadosaprendizaje.service.ResultadoAprendizajeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Endpoints REST de la gestión de resultados de aprendizaje (RF-06).
 * <p>
 * Solo recibe la petición, valida el formato de los DTOs (@Valid) y delega al servicio.
 * No contiene reglas de negocio ni accede al repositorio.
 * <p>
 * Los @PreAuthorize quedan comentados, igual que en el resto de módulos, hasta que el módulo de
 * seguridad (RF-05) active JWT y roles. Según la Matriz RBAC: registrar, modificar e inactivar es
 * solo del Administrador; el Profesor consulta los RA de sus asignaturas asignadas (esa restricción
 * por asignación queda pendiente de RF-05 y RF-07).
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ResultadoAprendizajeController {

    private final ResultadoAprendizajeService resultadoAprendizajeService;

    /** RF-06a: registra un RA en la asignatura de la URL. */
    @PostMapping("/asignaturas/{asignaturaId}/resultados-aprendizaje")
    //@PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<ResultadoAprendizajeResponseDTO> registrar(@PathVariable UUID asignaturaId,
                                                                     @Valid @RequestBody ResultadoAprendizajeCrearRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(resultadoAprendizajeService.registrar(asignaturaId, dto));
    }

    /** RF-06b: RA de una asignatura. Sin "estado" devuelve todos; lista vacía si no tiene RA. */
    @GetMapping("/asignaturas/{asignaturaId}/resultados-aprendizaje")
    //@PreAuthorize("hasAnyRole('ADMINISTRADOR', 'PROFESOR')")
    public ResponseEntity<List<ResultadoAprendizajeResponseDTO>> consultarPorAsignatura(
            @PathVariable UUID asignaturaId,
            @RequestParam(required = false) EstadoRegistro estado) {
        return ResponseEntity.ok(resultadoAprendizajeService.consultarPorAsignatura(asignaturaId, estado));
    }

    /** RF-06b (HU-20): todos los RA del sistema, para el Administrador. */
    @GetMapping("/resultados-aprendizaje")
    //@PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<List<ResultadoAprendizajeResponseDTO>> consultarTodos(
            @RequestParam(required = false) EstadoRegistro estado) {
        return ResponseEntity.ok(resultadoAprendizajeService.consultarTodos(estado));
    }

    /** RF-06b: un RA por su id. */
    @GetMapping("/resultados-aprendizaje/{id}")
    //@PreAuthorize("hasAnyRole('ADMINISTRADOR', 'PROFESOR')")
    public ResponseEntity<ResultadoAprendizajeResponseDTO> consultarPorId(@PathVariable UUID id) {
        return ResponseEntity.ok(resultadoAprendizajeService.consultarPorId(id));
    }

    /** RF-06c: modifica solo la descripción de un RA activo. */
    @PutMapping("/resultados-aprendizaje/{id}")
    //@PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<ResultadoAprendizajeResponseDTO> actualizarDescripcion(
            @PathVariable UUID id,
            @Valid @RequestBody ResultadoAprendizajeActualizarRequestDTO dto) {
        return ResponseEntity.ok(resultadoAprendizajeService.actualizarDescripcion(id, dto));
    }

    /** RF-06c: inactivación lógica (no se borra el registro). */
    @PatchMapping("/resultados-aprendizaje/{id}/inactivar")
    //@PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<ResultadoAprendizajeResponseDTO> inactivar(@PathVariable UUID id) {
        return ResponseEntity.ok(resultadoAprendizajeService.inactivar(id));
    }

    /** Reactiva un RA inactivo elegido por el Administrador. */
    @PatchMapping("/resultados-aprendizaje/{id}/reactivar")
    //@PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<ResultadoAprendizajeResponseDTO> reactivar(@PathVariable UUID id) {
        return ResponseEntity.ok(resultadoAprendizajeService.reactivar(id));
    }
}
