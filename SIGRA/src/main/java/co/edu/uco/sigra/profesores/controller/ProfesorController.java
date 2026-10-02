package co.edu.uco.sigra.profesores.controller;

import co.edu.uco.sigra.profesores.dto.ProfesorRequestDTO;
import co.edu.uco.sigra.profesores.dto.ProfesorResponseDTO;
import co.edu.uco.sigra.profesores.service.ProfesorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/profesores")
@RequiredArgsConstructor
public class ProfesorController {

    private final ProfesorService profesorService;

    @PostMapping
    //@PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<ProfesorResponseDTO> registrarProfesor(@Valid @RequestBody ProfesorRequestDTO dto) {
        var creado = profesorService.registrarProfesor(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(creado);
    }

    @GetMapping
    //@PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<List<ProfesorResponseDTO>> consultarProfesores(@RequestParam(required = false) String filtro) {
        return ResponseEntity.ok(profesorService.consultar(filtro));
    }

    @PutMapping("/{id}")
    //@PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<ProfesorResponseDTO> modificarProfesor(@PathVariable UUID id, @Valid @RequestBody ProfesorRequestDTO dto) {
        return ResponseEntity.ok(profesorService.modificarProfesor(id, dto));
    }

    @DeleteMapping("/{id}")
    //@PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<Void> inactivarProfesor(@PathVariable UUID id) {
        profesorService.inactivar(id);
        return ResponseEntity.noContent().build();
    }

}
