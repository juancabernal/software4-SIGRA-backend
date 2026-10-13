package co.edu.uco.sigra.estadisticas.controller;

import co.edu.uco.sigra.asignaturas.seguridad.IdentidadActual;
import co.edu.uco.sigra.asignaturas.seguridad.RolesPermitidos;
import co.edu.uco.sigra.common.enums.RolUsuario;
import co.edu.uco.sigra.estadisticas.dto.ConclusionFiltroDTO;
import co.edu.uco.sigra.estadisticas.dto.ConclusionResponseDTO;
import co.edu.uco.sigra.estadisticas.dto.GenerarConclusionResponseDTO;
import co.edu.uco.sigra.estadisticas.entity.EstadoConclusion;
import co.edu.uco.sigra.estadisticas.service.ConclusionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
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
 * Matriz de acceso por rol (RF-05). Mientras RF-05 no exista, la aplica el interceptor temporal
 * compartido con el módulo de asignaturas ({@link RolesPermitidos}), solo con
 * {@code sigra.seguridad.roles.habilitado=true} (ver WebConfig). El PROFESOR consulta únicamente
 * las conclusiones de las asignaturas donde tiene asignación docente ACTIVA: ese alcance lo decide
 * el servicio con la identidad recibida.
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ConclusionController {

    private final ConclusionService conclusionService;

    /**
     * Generar (RF-16). En producción la dispara el recálculo de estadísticas del RA; mientras ese
     * módulo no exista, se invoca aquí directamente. {@code generada=false} no es un error.
     */
    @PostMapping("/resultados-aprendizaje/{raId}/semestres/{semestreId}/conclusiones")
    @RolesPermitidos({RolUsuario.ADMINISTRADOR, RolUsuario.PROFESOR})
    public ResponseEntity<GenerarConclusionResponseDTO> generar(@PathVariable UUID raId,
                                                                  @PathVariable UUID semestreId) {
        return ResponseEntity.ok(conclusionService.generarConclusion(raId, semestreId));
    }

    @GetMapping("/conclusiones")
    @RolesPermitidos({RolUsuario.ADMINISTRADOR, RolUsuario.PROFESOR})
    public ResponseEntity<List<ConclusionResponseDTO>> consultar(
            @RequestAttribute(name = IdentidadActual.ATRIBUTO, required = false) IdentidadActual identidad,
            @RequestParam(required = false) UUID resultadoAprendizajeId,
            @RequestParam(required = false) UUID semestreId,
            @RequestParam(required = false) UUID asignaturaId,
            @RequestParam(required = false) EstadoConclusion estado) {
        ConclusionFiltroDTO filtro = new ConclusionFiltroDTO(resultadoAprendizajeId, semestreId, asignaturaId, estado);
        // Sin identidad (control por rol apagado) se usa la misma llamada de siempre.
        return ResponseEntity.ok(identidad == null
                ? conclusionService.consultarConclusiones(filtro)
                : conclusionService.consultarConclusiones(filtro, identidad));
    }

    @GetMapping("/conclusiones/{id}")
    @RolesPermitidos({RolUsuario.ADMINISTRADOR, RolUsuario.PROFESOR})
    public ResponseEntity<ConclusionResponseDTO> consultarPorId(@PathVariable UUID id) {
        return ResponseEntity.ok(conclusionService.consultarPorId(id));
    }
}
