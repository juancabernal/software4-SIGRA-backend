package co.edu.uco.sigra.programas.controller;

import co.edu.uco.sigra.common.enums.EstadoRegistro;
import co.edu.uco.sigra.programas.dto.ProgramaRequestDTO;
import co.edu.uco.sigra.programas.dto.ProgramaResponseDTO;
import co.edu.uco.sigra.programas.dto.ProgramaUpdateDTO;
import co.edu.uco.sigra.programas.service.ProgramaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Gestión de programas académicos (RF-02): registrar, consultar, modificar el nombre e inactivar.
 * El código es inmutable después de creado. Los errores de dominio los atiende ProgramaExceptionHandler.
 * Los @PreAuthorize quedan comentados, igual que en el resto de módulos, hasta que se active RF-05.
 */
@RestController
@RequestMapping("/api/v1/programas")
@RequiredArgsConstructor
public class ProgramaAcademicoController {

    private final ProgramaService programaService;

    @PostMapping
    //@PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<ProgramaResponseDTO> registrar(@Valid @RequestBody ProgramaRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(programaService.registrar(dto));
    }

    @GetMapping
    //@PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<List<ProgramaResponseDTO>> consultarProgramas(
            @RequestParam(required = false) String nombre,
            @RequestParam(required = false) String codigo,
            @RequestParam(required = false) EstadoRegistro estado) {
        return ResponseEntity.ok(programaService.consultar(nombre, codigo, estado));
    }

    @GetMapping("/{id}")
    //@PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<ProgramaResponseDTO> consultarPorId(@PathVariable UUID id) {
        return ResponseEntity.ok(programaService.consultarPorId(id));
    }

    @PutMapping("/{id}")
    //@PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<ProgramaResponseDTO> actualizarNombre(
            @PathVariable UUID id,
            @Valid @RequestBody ProgramaUpdateDTO dto) {
        return ResponseEntity.ok(programaService.actualizarNombre(id, dto));
    }

    @PatchMapping("/{id}/inactivar")
    //@PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<ProgramaResponseDTO> inactivar(@PathVariable UUID id) {
        return ResponseEntity.ok(programaService.inactivar(id));
    }
}
