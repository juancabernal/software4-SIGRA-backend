package co.edu.uco.sigra.resultadosaprendizaje.controller;

import co.edu.uco.sigra.resultadosaprendizaje.dto.RendimientoAsignaturaResponseDTO;
import co.edu.uco.sigra.resultadosaprendizaje.service.RendimientoResultadoAprendizajeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class RendimientoResultadoAprendizajeController {

    private final RendimientoResultadoAprendizajeService rendimientoService;

    @GetMapping("/asignaturas/{asignaturaId}/resultados-aprendizaje/rendimiento")
    @PreAuthorize("hasRole('ADMINISTRADOR') or @securityAuthorization.hasActiveAssignment(#asignaturaId, authentication)")
    public ResponseEntity<RendimientoAsignaturaResponseDTO> consultar(
            @PathVariable UUID asignaturaId,
            @RequestParam(required = false) UUID semestreId) {
        return ResponseEntity.ok(rendimientoService.consultarPorAsignatura(asignaturaId, semestreId));
    }
}
