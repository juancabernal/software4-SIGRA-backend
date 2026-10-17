package co.edu.uco.sigra.mejoras.controller;

import co.edu.uco.sigra.auth.security.CurrentUser;
import co.edu.uco.sigra.mejoras.dto.AccionMejoraFiltroDTO;
import co.edu.uco.sigra.mejoras.dto.AccionMejoraResponseDTO;
import co.edu.uco.sigra.mejoras.dto.ConvertirEvaluacionRequestDTO;
import co.edu.uco.sigra.mejoras.dto.RegistrarAccionMejoraRequestDTO;
import co.edu.uco.sigra.mejoras.service.AccionMejoraService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/** RF-17/RF-18: acciones de mejora dentro del alcance docente autorizado. */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class AccionMejoraController {

    private final AccionMejoraService accionMejoraService;
    private final CurrentUser currentUser;

    @PostMapping("/conclusiones/{conclusionId}/acciones-mejora")
    @PreAuthorize("hasRole('PROFESOR')")
    public ResponseEntity<AccionMejoraResponseDTO> registrar(
            @PathVariable UUID conclusionId,
            @Valid @RequestBody RegistrarAccionMejoraRequestDTO datos) {
        return ResponseEntity.ok(accionMejoraService.registrarAccionMejora(
                conclusionId, datos, currentUser.required()));
    }

    @GetMapping("/acciones-mejora")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'PROFESOR')")
    public ResponseEntity<List<AccionMejoraResponseDTO>> consultar(
            @RequestParam(required = false) UUID resultadoAprendizajeId,
            @RequestParam(required = false) UUID asignaturaId,
            @RequestParam(required = false) UUID semestreId,
            @RequestParam(required = false) Boolean convertida) {
        AccionMejoraFiltroDTO filtro = new AccionMejoraFiltroDTO(
                resultadoAprendizajeId, asignaturaId, semestreId, convertida);
        return ResponseEntity.ok(accionMejoraService.consultarAcciones(filtro, currentUser.required()));
    }

    @GetMapping("/acciones-mejora/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'PROFESOR')")
    public ResponseEntity<AccionMejoraResponseDTO> consultarPorId(@PathVariable UUID id) {
        return ResponseEntity.ok(accionMejoraService.consultarPorId(id, currentUser.required()));
    }

    @PostMapping("/acciones-mejora/{id}/conversion")
    @PreAuthorize("hasRole('PROFESOR')")
    public ResponseEntity<AccionMejoraResponseDTO> convertirEnEvaluacion(
            @PathVariable UUID id,
            @Valid @RequestBody ConvertirEvaluacionRequestDTO datos) {
        return ResponseEntity.ok(accionMejoraService.convertirEnEvaluacion(
                id, datos, currentUser.required()));
    }
}
