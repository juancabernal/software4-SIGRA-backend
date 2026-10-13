package co.edu.uco.sigra.estadisticas.controller;

import co.edu.uco.sigra.asignaturas.seguridad.IdentidadActual;
import co.edu.uco.sigra.asignaturas.seguridad.RolesPermitidos;
import co.edu.uco.sigra.common.enums.RolUsuario;
import co.edu.uco.sigra.estadisticas.dto.EstadisticasResultadoAprendizajeResponseDTO;
import co.edu.uco.sigra.estadisticas.dto.RangoNivelLogroResponseDTO;
import co.edu.uco.sigra.estadisticas.dto.RecalculoEstadisticasResponseDTO;
import co.edu.uco.sigra.estadisticas.service.NivelLogroService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * RF-14 / RF-15: nivel de logro y estadísticas descriptivas por RA y semestre.
 * <p>
 * Matriz de acceso por rol con el interceptor temporal compartido ({@link RolesPermitidos}), igual que
 * {@link ConclusionController}: ADMINISTRADOR y PROFESOR; el PROFESOR, solo en sus asignaturas.
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class EstadisticasController {

    private final NivelLogroService nivelLogroService;

    @GetMapping("/resultados-aprendizaje/{raId}/semestres/{semestreId}/estadisticas")
    @RolesPermitidos({RolUsuario.ADMINISTRADOR, RolUsuario.PROFESOR})
    public ResponseEntity<EstadisticasResultadoAprendizajeResponseDTO> consultar(
            @RequestAttribute(name = IdentidadActual.ATRIBUTO, required = false) IdentidadActual identidad,
            @PathVariable UUID raId,
            @PathVariable UUID semestreId) {
        return ResponseEntity.ok(nivelLogroService.consultarEstadisticas(raId, semestreId, identidad));
    }

    /**
     * Recálculo manual. En operación normal lo dispara el módulo de calificaciones al registrar o
     * actualizar una nota; mientras ese módulo no exista, se invoca aquí directamente.
     */
    @PostMapping("/resultados-aprendizaje/{raId}/semestres/{semestreId}/estadisticas/recalculo")
    @RolesPermitidos({RolUsuario.ADMINISTRADOR, RolUsuario.PROFESOR})
    public ResponseEntity<RecalculoEstadisticasResponseDTO> recalcular(
            @RequestAttribute(name = IdentidadActual.ATRIBUTO, required = false) IdentidadActual identidad,
            @PathVariable UUID raId,
            @PathVariable UUID semestreId) {
        return ResponseEntity.ok(nivelLogroService.recalcular(raId, semestreId, identidad));
    }

    /** Catálogo de los 4 rangos de nivel de logro (lo usa el frontend para los badges). */
    @GetMapping("/niveles-logro")
    public ResponseEntity<List<RangoNivelLogroResponseDTO>> catalogo() {
        return ResponseEntity.ok(nivelLogroService.consultarCatalogo());
    }
}
