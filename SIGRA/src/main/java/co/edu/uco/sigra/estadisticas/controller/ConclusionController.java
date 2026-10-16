package co.edu.uco.sigra.estadisticas.controller;

import co.edu.uco.sigra.auth.security.CurrentUser;
import co.edu.uco.sigra.estadisticas.dto.ConclusionFiltroDTO;
import co.edu.uco.sigra.estadisticas.dto.ConclusionResponseDTO;
import co.edu.uco.sigra.estadisticas.dto.GenerarConclusionResponseDTO;
import co.edu.uco.sigra.estadisticas.entity.EstadoConclusion;
import co.edu.uco.sigra.estadisticas.service.ConclusionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/** RF-16: conclusiones automáticas con autorización RBAC y alcance por asignación docente. */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ConclusionController {

    private final ConclusionService conclusionService;
    private final CurrentUser currentUser;

    @PostMapping("/resultados-aprendizaje/{raId}/semestres/{semestreId}/conclusiones")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'PROFESOR')")
    public ResponseEntity<GenerarConclusionResponseDTO> generar(@PathVariable UUID raId,
                                                                  @PathVariable UUID semestreId) {
        return ResponseEntity.ok(conclusionService.generarConclusion(raId, semestreId, currentUser.required()));
    }

    @GetMapping("/conclusiones")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'PROFESOR')")
    public ResponseEntity<List<ConclusionResponseDTO>> consultar(
            @RequestParam(required = false) UUID resultadoAprendizajeId,
            @RequestParam(required = false) UUID semestreId,
            @RequestParam(required = false) UUID asignaturaId,
            @RequestParam(required = false) EstadoConclusion estado) {
        ConclusionFiltroDTO filtro = new ConclusionFiltroDTO(resultadoAprendizajeId, semestreId, asignaturaId, estado);
        return ResponseEntity.ok(conclusionService.consultarConclusiones(filtro, currentUser.required()));
    }

    @GetMapping("/conclusiones/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'PROFESOR')")
    public ResponseEntity<ConclusionResponseDTO> consultarPorId(@PathVariable UUID id) {
        return ResponseEntity.ok(conclusionService.consultarPorId(id, currentUser.required()));
    }
}
