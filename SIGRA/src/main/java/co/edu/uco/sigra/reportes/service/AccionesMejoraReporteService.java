package co.edu.uco.sigra.reportes.service;

import co.edu.uco.sigra.reportes.dto.AccionesMejoraReporteResponseDTO;
import co.edu.uco.sigra.reportes.dto.AccionesMejoraReporteResponseDTO.Fila;
import co.edu.uco.sigra.reportes.dto.AccionesMejoraReporteResponseDTO.Resumen;
import co.edu.uco.sigra.reportes.repository.AccionesMejoraReporteRepository;
import co.edu.uco.sigra.asignaturas.repository.AsignacionDocenteRepository;
import co.edu.uco.sigra.auth.security.UsuarioPrincipal;
import co.edu.uco.sigra.common.enums.EstadoRegistro;
import co.edu.uco.sigra.common.enums.RolUsuario;
import co.edu.uco.sigra.reportes.exception.PermisoReporteInsuficienteException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.util.Set;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AccionesMejoraReporteService {
    private final AccionesMejoraReporteRepository repository;
    private final AsignacionDocenteRepository asignacionRepository;

    @Transactional(readOnly = true)
    public AccionesMejoraReporteResponseDTO consultar(UUID semestreId, UUID asignaturaId,
                                                      UUID resultadoAprendizajeId, Boolean convertida,
                                                      UsuarioPrincipal principal) {
        if (principal == null || principal.id() == null || (principal.rol() != RolUsuario.ADMINISTRADOR
                && principal.rol() != RolUsuario.PROFESOR)) {
            throw new PermisoReporteInsuficienteException();
        }
        Set<UUID> alcance = principal.rol() == RolUsuario.ADMINISTRADOR ? null
                : asignacionRepository.findByProfesor_IdAndEstado(principal.id(), EstadoRegistro.ACTIVO)
                    .stream().map(a -> a.getAsignatura().getId()).collect(Collectors.toSet());
        if (alcance != null && asignaturaId != null && !alcance.contains(asignaturaId)) {
            throw new PermisoReporteInsuficienteException();
        }
        if (alcance != null && alcance.isEmpty()) {
            return new AccionesMejoraReporteResponseDTO(new Resumen(0, 0, 0, 0), List.of());
        }
        var filas = repository.consultar(semestreId, asignaturaId, resultadoAprendizajeId, convertida)
                .stream()
                .filter(a -> alcance == null || alcance.contains(a.getResultadoAprendizaje().getAsignatura().getId()))
                .map(a -> {
                    var ra = a.getResultadoAprendizaje();
                    var materia = ra.getAsignatura();
                    var semestre = a.getConclusionOrigen().getSemestre();
                    return new Fila(a.getId(), a.getNombre(), a.getDescripcion(), a.getFechaPropuesta(),
                            materia.getId(), materia.getCodigo(), materia.getNombre(), ra.getId(), ra.getCodigo(),
                            semestre.getId(), semestre.getCodigo(), a.getEvaluacionGeneradaId(), a.estaConvertida());
                }).toList();
        long total = filas.size();
        long convertidas = filas.stream().filter(Fila::convertida).count();
        double porcentaje = total == 0 ? 0 : Math.round(convertidas * 10000.0 / total) / 100.0;
        return new AccionesMejoraReporteResponseDTO(
                new Resumen(total, convertidas, total - convertidas, porcentaje), filas);
    }
}
