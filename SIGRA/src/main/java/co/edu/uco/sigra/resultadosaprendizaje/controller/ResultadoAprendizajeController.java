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


@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ResultadoAprendizajeController {

    private final ResultadoAprendizajeService resultadoAprendizajeService;


    @PostMapping("/asignaturas/{asignaturaId}/resultados-aprendizaje")
    //@PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<ResultadoAprendizajeResponseDTO> registrar(@PathVariable UUID asignaturaId,
                                                                     @Valid @RequestBody ResultadoAprendizajeCrearRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(resultadoAprendizajeService.registrar(asignaturaId, dto));
    }

    @GetMapping("/asignaturas/{asignaturaId}/resultados-aprendizaje")
    //@PreAuthorize("hasAnyRole('ADMINISTRADOR', 'PROFESOR')")
    public ResponseEntity<List<ResultadoAprendizajeResponseDTO>> consultarPorAsignatura(
            @PathVariable UUID asignaturaId,
            @RequestParam(required = false) EstadoRegistro estado) {
        return ResponseEntity.ok(resultadoAprendizajeService.consultarPorAsignatura(asignaturaId, estado));
    }


    @GetMapping("/resultados-aprendizaje")
    //@PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<List<ResultadoAprendizajeResponseDTO>> consultarTodos(
            @RequestParam(required = false) EstadoRegistro estado) {
        return ResponseEntity.ok(resultadoAprendizajeService.consultarTodos(estado));
    }


    @GetMapping("/resultados-aprendizaje/{id}")
    //@PreAuthorize("hasAnyRole('ADMINISTRADOR', 'PROFESOR')")
    public ResponseEntity<ResultadoAprendizajeResponseDTO> consultarPorId(@PathVariable UUID id) {
        return ResponseEntity.ok(resultadoAprendizajeService.consultarPorId(id));
    }


    @PutMapping("/resultados-aprendizaje/{id}")
    //@PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<ResultadoAprendizajeResponseDTO> actualizarDescripcion(
            @PathVariable UUID id,
            @Valid @RequestBody ResultadoAprendizajeActualizarRequestDTO dto) {
        return ResponseEntity.ok(resultadoAprendizajeService.actualizarDescripcion(id, dto));
    }


    @PatchMapping("/resultados-aprendizaje/{id}/inactivar")
    //@PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<ResultadoAprendizajeResponseDTO> inactivar(@PathVariable UUID id) {
        return ResponseEntity.ok(resultadoAprendizajeService.inactivar(id));
    }


    @PatchMapping("/resultados-aprendizaje/{id}/reactivar")
    //@PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<ResultadoAprendizajeResponseDTO> reactivar(@PathVariable UUID id) {
        return ResponseEntity.ok(resultadoAprendizajeService.reactivar(id));
    }
}
