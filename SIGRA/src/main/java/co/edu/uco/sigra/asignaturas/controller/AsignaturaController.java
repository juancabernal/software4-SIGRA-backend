package co.edu.uco.sigra.asignaturas.controller;

import co.edu.uco.sigra.asignaturas.dto.AsignaturaFiltroDTO;
import co.edu.uco.sigra.asignaturas.dto.AsignaturaRequestDTO;
import co.edu.uco.sigra.asignaturas.dto.AsignaturaResponseDTO;
import co.edu.uco.sigra.asignaturas.dto.AsignaturaUpdateDTO;
import co.edu.uco.sigra.asignaturas.dto.MiAsignaturaDTO;
import co.edu.uco.sigra.asignaturas.dto.ReglasEntrada;
import co.edu.uco.sigra.asignaturas.entity.EstadoAsignatura;
import co.edu.uco.sigra.auth.security.CurrentUser;
import co.edu.uco.sigra.auth.security.UsuarioPrincipal;
import co.edu.uco.sigra.asignaturas.service.AsignaturaService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Matriz de acceso RF-05. El rol se autoriza con Spring Security y el alcance del profesor se
 * comprueba en el servicio contra sus asignaciones docentes activas.
 * <p>
 * {@code @Validated} activa la validación de los parámetros de consulta: un valor fuera de rango
 * lanza ConstraintViolationException, que el manejador transversal traduce a 400.
 */
@Validated
@RestController
@RequestMapping("/api/v1/asignaturas")
public class AsignaturaController {

    private final AsignaturaService asignaturaService;
    private final CurrentUser currentUser;

    @Autowired
    public AsignaturaController(AsignaturaService asignaturaService, CurrentUser currentUser) {
        this.asignaturaService = asignaturaService;
        this.currentUser = currentUser;
    }

    /** Constructor reducido conservado para pruebas standalone; la identidad sigue siendo obligatoria. */
    public AsignaturaController(AsignaturaService asignaturaService) {
        this(asignaturaService, new CurrentUser());
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<AsignaturaResponseDTO> registrarAsignatura(@Valid @RequestBody AsignaturaRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(asignaturaService.registrarAsignatura(dto));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'PROFESOR')")
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
        AsignaturaFiltroDTO filtro = new AsignaturaFiltroDTO(texto, programaId, estado, raMin, raMax);
        UsuarioPrincipal identidad = currentUser.required();
        return ResponseEntity.ok(asignaturaService.consultar(filtro, identidad));
    }

    /**
     * «Mis asignaturas» del PROFESOR (asignación docente ACTIVA) o del ESTUDIANTE (matrícula ACTIVA).
     * Siempre exige un token válido porque depende de quién pregunta.
     */
    @GetMapping("/mias")
    @PreAuthorize("hasAnyRole('PROFESOR', 'ESTUDIANTE')")
    public ResponseEntity<List<MiAsignaturaDTO>> misAsignaturas() {
        return ResponseEntity.ok(asignaturaService.misAsignaturas(currentUser.required()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'PROFESOR')")
    public ResponseEntity<AsignaturaResponseDTO> obtenerAsignatura(@PathVariable UUID id) {
        UsuarioPrincipal identidad = currentUser.required();
        return ResponseEntity.ok(asignaturaService.obtenerAsignatura(id, identidad));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<AsignaturaResponseDTO> modificarAsignatura(@PathVariable UUID id,
                                                                     @Valid @RequestBody AsignaturaUpdateDTO dto) {
        return ResponseEntity.ok(asignaturaService.modificarAsignatura(id, dto));
    }

    @PatchMapping("/{id}/activar")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<AsignaturaResponseDTO> activarAsignatura(@PathVariable UUID id) {
        return ResponseEntity.ok(asignaturaService.activar(id));
    }

    @PatchMapping("/{id}/inactivar")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<AsignaturaResponseDTO> inactivarAsignatura(@PathVariable UUID id) {
        return ResponseEntity.ok(asignaturaService.inactivar(id));
    }
}
