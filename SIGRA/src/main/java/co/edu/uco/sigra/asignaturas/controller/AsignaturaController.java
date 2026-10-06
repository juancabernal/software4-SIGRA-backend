package co.edu.uco.sigra.asignaturas.controller;

import co.edu.uco.sigra.asignaturas.dto.AsignaturaFiltroDTO;
import co.edu.uco.sigra.asignaturas.dto.AsignaturaRequestDTO;
import co.edu.uco.sigra.asignaturas.dto.AsignaturaResponseDTO;
import co.edu.uco.sigra.asignaturas.dto.AsignaturaUpdateDTO;
import co.edu.uco.sigra.asignaturas.dto.ReglasEntrada;
import co.edu.uco.sigra.asignaturas.entity.EstadoAsignatura;
import co.edu.uco.sigra.asignaturas.seguridad.IdentidadActual;
import co.edu.uco.sigra.asignaturas.seguridad.RolesPermitidos;
import co.edu.uco.sigra.asignaturas.service.AsignaturaService;
import co.edu.uco.sigra.common.enums.RolUsuario;
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
 * Matriz de acceso por rol (RF-05). Mientras RF-05 no exista, la aplica el interceptor temporal del
 * módulo según {@link RolesPermitidos}, solo con {@code sigra.seguridad.roles.habilitado=true}; los
 * {@code @PreAuthorize} quedan comentados con la misma matriz para que RF-05 solo los descomente.
 * El Profesor consulta únicamente las asignaturas donde tiene asignación docente ACTIVA (SRS 3.2.3b):
 * ese alcance lo decide el servicio con la identidad recibida.
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
    @RolesPermitidos(RolUsuario.ADMINISTRADOR)
    public ResponseEntity<AsignaturaResponseDTO> registrarAsignatura(@Valid @RequestBody AsignaturaRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(asignaturaService.registrarAsignatura(dto));
    }

    @GetMapping
    //@PreAuthorize("hasAnyRole('ADMINISTRADOR', 'PROFESOR')") // el PROFESOR solo ve sus asignaturas
    @RolesPermitidos({RolUsuario.ADMINISTRADOR, RolUsuario.PROFESOR})
    public ResponseEntity<List<AsignaturaResponseDTO>> consultarAsignaturas(
            @RequestAttribute(name = IdentidadActual.ATRIBUTO, required = false) IdentidadActual identidad,
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
        // Sin identidad (control por rol apagado) se usa la misma llamada de siempre.
        return ResponseEntity.ok(identidad == null
                ? asignaturaService.consultar(filtro)
                : asignaturaService.consultar(filtro, identidad));
    }

    @GetMapping("/{id}")
    //@PreAuthorize("hasAnyRole('ADMINISTRADOR', 'PROFESOR')") // el PROFESOR solo si es suya
    @RolesPermitidos({RolUsuario.ADMINISTRADOR, RolUsuario.PROFESOR})
    public ResponseEntity<AsignaturaResponseDTO> obtenerAsignatura(
            @PathVariable UUID id,
            @RequestAttribute(name = IdentidadActual.ATRIBUTO, required = false) IdentidadActual identidad) {
        return ResponseEntity.ok(identidad == null
                ? asignaturaService.obtenerAsignatura(id)
                : asignaturaService.obtenerAsignatura(id, identidad));
    }

    @PutMapping("/{id}")
    //@PreAuthorize("hasRole('ADMINISTRADOR')")
    @RolesPermitidos(RolUsuario.ADMINISTRADOR)
    public ResponseEntity<AsignaturaResponseDTO> modificarAsignatura(@PathVariable UUID id,
                                                                     @Valid @RequestBody AsignaturaUpdateDTO dto) {
        return ResponseEntity.ok(asignaturaService.modificarAsignatura(id, dto));
    }

    @PatchMapping("/{id}/activar")
    //@PreAuthorize("hasRole('ADMINISTRADOR')")
    @RolesPermitidos(RolUsuario.ADMINISTRADOR)
    public ResponseEntity<AsignaturaResponseDTO> activarAsignatura(@PathVariable UUID id) {
        return ResponseEntity.ok(asignaturaService.activar(id));
    }

    @PatchMapping("/{id}/inactivar")
    //@PreAuthorize("hasRole('ADMINISTRADOR')")
    @RolesPermitidos(RolUsuario.ADMINISTRADOR)
    public ResponseEntity<AsignaturaResponseDTO> inactivarAsignatura(@PathVariable UUID id) {
        return ResponseEntity.ok(asignaturaService.inactivar(id));
    }
}
