package co.edu.uco.sigra.mejoras.controller;

import co.edu.uco.sigra.asignaturas.seguridad.IdentidadActual;
import co.edu.uco.sigra.asignaturas.seguridad.RolesPermitidos;
import co.edu.uco.sigra.common.enums.RolUsuario;
import co.edu.uco.sigra.mejoras.dto.AccionMejoraFiltroDTO;
import co.edu.uco.sigra.mejoras.dto.AccionMejoraResponseDTO;
import co.edu.uco.sigra.mejoras.dto.ConvertirEvaluacionRequestDTO;
import co.edu.uco.sigra.mejoras.dto.RegistrarAccionMejoraRequestDTO;
import co.edu.uco.sigra.mejoras.service.AccionMejoraService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * RF-17: acciones de mejora sobre conclusiones VIGENTES.
 * <p>
 * Mismo control de acceso temporal por rol que {@code ConclusionController} (ver {@link RolesPermitidos}
 * y WebConfig): el PROFESOR solo accede a las acciones de mejora de las asignaturas donde tiene
 * asignación docente ACTIVA, alcance que decide el servicio con la identidad recibida.
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class AccionMejoraController {

    private final AccionMejoraService accionMejoraService;

    @PostMapping("/conclusiones/{conclusionId}/acciones-mejora")
    @RolesPermitidos({RolUsuario.ADMINISTRADOR, RolUsuario.PROFESOR})
    public ResponseEntity<AccionMejoraResponseDTO> registrar(
            @PathVariable UUID conclusionId,
            @Valid @RequestBody RegistrarAccionMejoraRequestDTO datos,
            @RequestAttribute(name = IdentidadActual.ATRIBUTO, required = false) IdentidadActual identidad) {
        return ResponseEntity.ok(accionMejoraService.registrarAccionMejora(conclusionId, datos, identidad));
    }

    @GetMapping("/acciones-mejora")
    @RolesPermitidos({RolUsuario.ADMINISTRADOR, RolUsuario.PROFESOR})
    public ResponseEntity<List<AccionMejoraResponseDTO>> consultar(
            @RequestAttribute(name = IdentidadActual.ATRIBUTO, required = false) IdentidadActual identidad,
            @RequestParam(required = false) UUID resultadoAprendizajeId,
            @RequestParam(required = false) UUID asignaturaId,
            @RequestParam(required = false) Boolean convertida) {
        AccionMejoraFiltroDTO filtro = new AccionMejoraFiltroDTO(resultadoAprendizajeId, asignaturaId, convertida);
        return ResponseEntity.ok(identidad == null
                ? accionMejoraService.consultarAcciones(filtro)
                : accionMejoraService.consultarAcciones(filtro, identidad));
    }

    @GetMapping("/acciones-mejora/{id}")
    @RolesPermitidos({RolUsuario.ADMINISTRADOR, RolUsuario.PROFESOR})
    public ResponseEntity<AccionMejoraResponseDTO> consultarPorId(@PathVariable UUID id) {
        return ResponseEntity.ok(accionMejoraService.consultarPorId(id));
    }

    @PostMapping("/acciones-mejora/{id}/conversion")
    @RolesPermitidos({RolUsuario.ADMINISTRADOR, RolUsuario.PROFESOR})
    public ResponseEntity<AccionMejoraResponseDTO> convertirEnEvaluacion(
            @PathVariable UUID id,
            @Valid @RequestBody ConvertirEvaluacionRequestDTO datos,
            @RequestAttribute(name = IdentidadActual.ATRIBUTO, required = false) IdentidadActual identidad) {
        return ResponseEntity.ok(accionMejoraService.convertirEnEvaluacion(id, datos, identidad));
    }
}
