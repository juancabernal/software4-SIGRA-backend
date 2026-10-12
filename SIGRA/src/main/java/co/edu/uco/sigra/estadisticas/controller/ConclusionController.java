package co.edu.uco.sigra.estadisticas.controller;

import co.edu.uco.sigra.auth.security.CurrentUser;
import co.edu.uco.sigra.auth.security.UsuarioPrincipal;
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
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * RF-16: conclusiones automáticas.
 * <p>
 * Matriz de acceso por rol (RF-05). El PROFESOR consulta únicamente las conclusiones de las
 * asignaturas donde tiene asignación docente ACTIVA; esa condición se verifica en method security
 * y nuevamente en el servicio con la identidad validada por JWT.
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ConclusionController {

    private final ConclusionService conclusionService;
    private final CurrentUser currentUser;

    /**
     * Generar (RF-16). En producción la dispara el recálculo de estadísticas del RA; mientras ese
     * módulo no exista, se invoca aquí directamente. {@code generada=false} no es un error.
     */
    @PostMapping("/resultados-aprendizaje/{raId}/semestres/{semestreId}/conclusiones")
    @PreAuthorize("hasRole('ADMINISTRADOR') or @securityAuthorization.hasActiveAssignmentForResultado(#raId, authentication)")
    public ResponseEntity<GenerarConclusionResponseDTO> generar(@PathVariable UUID raId,
                                                                  @PathVariable UUID semestreId) {
        UsuarioPrincipal principal = currentUser.required();
        return ResponseEntity.ok(conclusionService.generarConclusion(raId, semestreId, principal));
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
    @PreAuthorize("@securityAuthorization.hasActiveAssignmentForConclusion(#id, authentication)")
    public ResponseEntity<ConclusionResponseDTO> consultarPorId(@PathVariable UUID id) {
        return ResponseEntity.ok(conclusionService.consultarPorId(id, currentUser.required()));
    }
}
