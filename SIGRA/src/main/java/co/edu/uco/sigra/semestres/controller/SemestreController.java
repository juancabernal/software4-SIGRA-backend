package co.edu.uco.sigra.semestres.controller;

import co.edu.uco.sigra.semestres.dto.SemestreFechaFinRequestDTO;
import co.edu.uco.sigra.semestres.dto.SemestreRequestDTO;
import co.edu.uco.sigra.semestres.dto.SemestreResponseDTO;
import co.edu.uco.sigra.semestres.service.SemestreService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Los @PreAuthorize quedan comentados, igual que en el resto de módulos, hasta que
 * el módulo de seguridad (RF-05) active JWT y roles. Ver Matriz RBAC del SRS.
 */
@RestController
@RequestMapping("/api/v1/semestres")
@RequiredArgsConstructor
public class SemestreController {

    private final SemestreService semestreService;

    @PostMapping
    //@PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<SemestreResponseDTO> crearSemestre(@Valid @RequestBody SemestreRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(semestreService.crear(dto));
    }

    @GetMapping
    //@PreAuthorize("hasAnyRole('ADMINISTRADOR', 'PROFESOR', 'ESTUDIANTE')")
    public ResponseEntity<List<SemestreResponseDTO>> listarSemestres() {
        return ResponseEntity.ok(semestreService.listar());
    }

    @GetMapping("/{codigo}")
    //@PreAuthorize("hasAnyRole('ADMINISTRADOR', 'PROFESOR', 'ESTUDIANTE')")
    public ResponseEntity<SemestreResponseDTO> consultarSemestre(@PathVariable String codigo) {
        return ResponseEntity.ok(semestreService.consultarPorCodigo(codigo));
    }

    @PatchMapping("/{codigo}")
    //@PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<SemestreResponseDTO> extenderFechaFin(@PathVariable String codigo,
                                                                @Valid @RequestBody SemestreFechaFinRequestDTO dto) {
        return ResponseEntity.ok(semestreService.extenderFechaFin(codigo, dto));
    }
}
