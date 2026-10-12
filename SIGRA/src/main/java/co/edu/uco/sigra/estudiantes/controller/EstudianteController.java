package co.edu.uco.sigra.estudiantes.controller;

import co.edu.uco.sigra.estudiantes.dto.AsignaturaDeEstudianteDTO;
import co.edu.uco.sigra.estudiantes.dto.EstudianteRequestDTO;
import co.edu.uco.sigra.estudiantes.dto.EstudianteResponseDTO;
import co.edu.uco.sigra.estudiantes.mapper.MatriculaMapper;
import co.edu.uco.sigra.estudiantes.service.EstudianteService;
import co.edu.uco.sigra.estudiantes.service.MatriculaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/estudiantes")
@RequiredArgsConstructor
public class EstudianteController {

    private final EstudianteService estudianteService;
    private final MatriculaService matriculaService;
    private final MatriculaMapper matriculaMapper;

    @PostMapping
    //@PreAuthorize("hasAnyRole('ADMINISTRADOR', 'PROFESOR')")
    public ResponseEntity<EstudianteResponseDTO> registrar(@Valid @RequestBody EstudianteRequestDTO dto) {
        var creado = estudianteService.registrar(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(creado);
    }

    @GetMapping
    //@PreAuthorize("hasAnyRole('ADMINISTRADOR', 'PROFESOR')")
    public ResponseEntity<List<EstudianteResponseDTO>> consultar(@RequestParam(required = false) String filtro) {
        return ResponseEntity.ok(estudianteService.consultar(filtro));
    }

    @GetMapping("/{id}")
    //@PreAuthorize("hasAnyRole('ADMINISTRADOR', 'PROFESOR')")
    public ResponseEntity<EstudianteResponseDTO> consultarPorId(@PathVariable UUID id) {
        return ResponseEntity.ok(estudianteService.consultarPorId(id));
    }

    @PutMapping("/{id}")
    //@PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<EstudianteResponseDTO> modificar(@PathVariable UUID id, @Valid @RequestBody EstudianteRequestDTO dto) {
        return ResponseEntity.ok(estudianteService.modificar(id, dto));
    }

    @DeleteMapping("/{id}")
    //@PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<Void> inactivar(@PathVariable UUID id) {
        estudianteService.inactivar(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/asignaturas")
    //@PreAuthorize("hasAnyRole('ADMINISTRADOR', 'PROFESOR')")
    public ResponseEntity<List<AsignaturaDeEstudianteDTO>> consultarAsignaturas(@PathVariable UUID id) {
        var matriculas = matriculaService.listarAsignaturasDeEstudiante(id);
        return ResponseEntity.ok(matriculaMapper.toAsignaturaDeEstudianteDTOList(matriculas));
    }

}
