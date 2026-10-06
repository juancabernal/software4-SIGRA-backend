package co.edu.uco.sigra.programas.controller;

import co.edu.uco.sigra.common.enums.EstadoRegistro;
import co.edu.uco.sigra.programas.dto.ProgramaResponseDTO;
import co.edu.uco.sigra.programas.service.ProgramaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Listado de solo lectura de programas académicos que necesita el selector de asignaturas (RF-03).
 * El módulo de programas (RF-02) lo extenderá con registro, modificación e inactivación sin romper
 * este contrato. Un estado inválido en la URL lo atiende el manejador global (400).
 * El @PreAuthorize queda comentado, igual que en el resto de módulos, hasta que se active RF-05.
 */
@RestController
@RequestMapping("/api/v1/programas")
@RequiredArgsConstructor
public class ProgramaAcademicoController {

    private final ProgramaService programaService;

    @GetMapping
    //@PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<List<ProgramaResponseDTO>> consultarProgramas(
            @RequestParam(required = false) EstadoRegistro estado) {
        return ResponseEntity.ok(programaService.consultar(estado));
    }
}
