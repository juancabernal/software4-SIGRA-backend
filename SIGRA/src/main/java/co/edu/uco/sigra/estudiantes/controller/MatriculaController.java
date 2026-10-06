package co.edu.uco.sigra.estudiantes.controller;

import co.edu.uco.sigra.estudiantes.dto.EstudianteMatriculadoDTO;
import co.edu.uco.sigra.estudiantes.dto.MatriculaRequestDTO;
import co.edu.uco.sigra.estudiantes.dto.MatriculaResponseDTO;
import co.edu.uco.sigra.estudiantes.mapper.MatriculaMapper;
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
@RequestMapping("/api/v1/matriculas")
@RequiredArgsConstructor
public class MatriculaController {

    private final MatriculaService matriculaService;
    private final MatriculaMapper matriculaMapper;

    @PostMapping
    //@PreAuthorize("hasAnyRole('ADMINISTRADOR', 'PROFESOR')")
    public ResponseEntity<MatriculaResponseDTO> matricular(@Valid @RequestBody MatriculaRequestDTO dto) {
        var resultado = matriculaService.matricular(dto.estudianteId(), dto.asignaturaId(), dto.semestreId());
        var respuesta = matriculaMapper.toResponseDTO(resultado.matricula());
        var status = resultado.reactivada() ? HttpStatus.OK : HttpStatus.CREATED;
        return ResponseEntity.status(status).body(respuesta);
    }

    @DeleteMapping("/{id}")
    //@PreAuthorize("hasAnyRole('ADMINISTRADOR', 'PROFESOR')")
    public ResponseEntity<Void> desvincular(@PathVariable UUID id) {
        matriculaService.desvincular(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    //@PreAuthorize("hasAnyRole('ADMINISTRADOR', 'PROFESOR')")
    public ResponseEntity<List<EstudianteMatriculadoDTO>> listarMatriculados(
            @RequestParam UUID asignaturaId,
            @RequestParam UUID semestreId,
            @RequestParam(defaultValue = "false") boolean incluirInactivas) {
        var matriculas = matriculaService.listarMatriculados(asignaturaId, semestreId, incluirInactivas);
        return ResponseEntity.ok(matriculaMapper.toEstudianteMatriculadoDTOList(matriculas));
    }

}
