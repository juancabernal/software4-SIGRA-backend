package co.edu.uco.sigra.asignaturas.controller;

import co.edu.uco.sigra.asignaturas.dto.AsignaturaFiltroDTO;
import co.edu.uco.sigra.asignaturas.dto.AsignaturaRequestDTO;
import co.edu.uco.sigra.asignaturas.dto.AsignaturaResponseDTO;
import co.edu.uco.sigra.asignaturas.dto.AsignaturaUpdateDTO;
import co.edu.uco.sigra.asignaturas.dto.ReglasEntrada;
import co.edu.uco.sigra.asignaturas.entity.EstadoAsignatura;
import co.edu.uco.sigra.asignaturas.service.AsignaturaService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Los @PreAuthorize quedan comentados, igual que en el resto de módulos, hasta que
 * el módulo de seguridad (RF-05) active JWT y roles. Ver Matriz RBAC del SRS.
 * El SRS 3.2.3b indica que el Profesor asignado puede consultar asignaturas, pero la matriz RBAC
 * no se lo permite; por ahora solo se contempla al Administrador.
 * <p>
 * {@code @Validated} activa la validación de los parámetros de consulta: un valor fuera de rango
 * lanza ConstraintViolationException, que el manejador transversal traduce a 400.
 */
@Validated
@RestController
@RequestMapping("/api/v1/asignaturas")
@RequiredArgsConstructor
public class AsignaturaController {

    private final AsignaturaService asignaturaService;

    @PostMapping
    //@PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<AsignaturaResponseDTO> registrarAsignatura(@Valid @RequestBody AsignaturaRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(asignaturaService.registrarAsignatura(dto));
    }

    @GetMapping
    //@PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<List<AsignaturaResponseDTO>> consultarAsignaturas(
            @RequestParam(required = false)
            @Size(max = ReglasEntrada.TEXTO_BUSQUEDA_MAX, message = ReglasEntrada.TEXTO_LONGITUD) String texto,
            @RequestParam(required = false) UUID programaId,
            @RequestParam(required = false) EstadoAsignatura estado,
            @RequestParam(required = false)
            @Min(value = 0, message = ReglasEntrada.RA_NEGATIVO)
            @Max(value = ReglasEntrada.RA_FILTRO_MAX, message = ReglasEntrada.RA_MAXIMO) Integer raMin,
            @RequestParam(required = false)
            @Min(value = 0, message = ReglasEntrada.RA_NEGATIVO)
            @Max(value = ReglasEntrada.RA_FILTRO_MAX, message = ReglasEntrada.RA_MAXIMO) Integer raMax) {
        return ResponseEntity.ok(asignaturaService.consultar(
                new AsignaturaFiltroDTO(texto, programaId, estado, raMin, raMax)));
    }

    @GetMapping("/{id}")
    //@PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<AsignaturaResponseDTO> obtenerAsignatura(@PathVariable UUID id) {
        return ResponseEntity.ok(asignaturaService.obtenerAsignatura(id));
    }

    @PutMapping("/{id}")
    //@PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<AsignaturaResponseDTO> modificarAsignatura(@PathVariable UUID id,
                                                                     @Valid @RequestBody AsignaturaUpdateDTO dto) {
        return ResponseEntity.ok(asignaturaService.modificarAsignatura(id, dto));
    }

    @PatchMapping("/{id}/activar")
    //@PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<AsignaturaResponseDTO> activarAsignatura(@PathVariable UUID id) {
        return ResponseEntity.ok(asignaturaService.activar(id));
    }

    @PatchMapping("/{id}/inactivar")
    //@PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<AsignaturaResponseDTO> inactivarAsignatura(@PathVariable UUID id) {
        return ResponseEntity.ok(asignaturaService.inactivar(id));
    }
}
