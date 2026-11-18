package co.edu.uco.sigra.reportes.controller;

import co.edu.uco.sigra.config.OpenApiConfig;
import co.edu.uco.sigra.auth.security.CurrentUser;
import co.edu.uco.sigra.reportes.dto.AccionesMejoraReporteResponseDTO;
import co.edu.uco.sigra.reportes.service.AccionesMejoraReporteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/reportes")
@RequiredArgsConstructor
@Tag(name = "Reportes")
@SecurityRequirement(name = OpenApiConfig.ESQUEMA_BEARER)
public class ReporteAccionesMejoraController {
    private final ObjectProvider<AccionesMejoraReporteService> service;
    private final CurrentUser currentUser;

    @GetMapping("/acciones-mejora")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'PROFESOR')")
    @Operation(summary = "R4 - Estado de acciones de mejora y su conversión",
            description = "Consulta de solo lectura. Los conteos corresponden a los filtros aplicados. "
                    + "El administrador consulta todas las asignaturas; el profesor solo sus asignaciones activas. "
                    + "El semestre es el de la conclusión origen. Sin coincidencias devuelve conteos cero y filas vacías.")
    public AccionesMejoraReporteResponseDTO consultar(@RequestParam(required = false) UUID semestreId,
            @RequestParam(required = false) UUID asignaturaId,
            @RequestParam(required = false) UUID resultadoAprendizajeId,
            @RequestParam(required = false) Boolean convertida) {
        return service.getObject().consultar(semestreId, asignaturaId, resultadoAprendizajeId, convertida,
                currentUser.required());
    }
}
