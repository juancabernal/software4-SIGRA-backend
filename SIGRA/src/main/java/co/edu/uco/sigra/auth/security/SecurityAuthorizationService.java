package co.edu.uco.sigra.auth.security;

import co.edu.uco.sigra.asignaturas.repository.AsignacionDocenteRepository;
import co.edu.uco.sigra.common.enums.EstadoRegistro;
import co.edu.uco.sigra.common.enums.RolUsuario;
import co.edu.uco.sigra.resultadosaprendizaje.repository.ResultadoAprendizajeRepository;
import co.edu.uco.sigra.estudiantes.repository.MatriculaRepository;
import co.edu.uco.sigra.estadisticas.repository.ConclusionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/** Expresiones pequeñas y explícitas para comprobar propiedad desde Spring Method Security. */
@Component("securityAuthorization")
@RequiredArgsConstructor
public class SecurityAuthorizationService {

    private final AsignacionDocenteRepository asignacionDocenteRepository;
    private final ResultadoAprendizajeRepository resultadoAprendizajeRepository;
    private final MatriculaRepository matriculaRepository;
    private final ConclusionRepository conclusionRepository;

    public boolean isSelf(UUID requestedId, Authentication authentication) {
        if (requestedId == null || authentication == null
                || !(authentication.getPrincipal() instanceof UsuarioPrincipal principal)) {
            return false;
        }
        return principal.rol() == RolUsuario.ESTUDIANTE && requestedId.equals(principal.id());
    }

    @Transactional(readOnly = true)
    public boolean canAccessStudent(UUID studentId, Authentication authentication) {
        UsuarioPrincipal principal = principal(authentication);
        if (studentId == null || principal == null) {
            return false;
        }
        if (principal.rol() == RolUsuario.ADMINISTRADOR) {
            return true;
        }
        if (principal.rol() == RolUsuario.ESTUDIANTE) {
            return studentId.equals(principal.id());
        }
        return principal.rol() == RolUsuario.PROFESOR
                && matriculaRepository.countActiveByStudentAndProfessor(
                studentId, principal.id(), EstadoRegistro.ACTIVO, EstadoRegistro.ACTIVO,
                EstadoRegistro.ACTIVO) > 0;
    }

    public boolean hasActiveAssignment(UUID asignaturaId, Authentication authentication) {
        UsuarioPrincipal principal = principal(authentication);
        return principal != null && principal.rol() == RolUsuario.PROFESOR
                && asignacionDocenteRepository.existsByProfesor_IdAndAsignatura_IdAndEstado(
                principal.id(), asignaturaId, EstadoRegistro.ACTIVO);
    }

    @Transactional(readOnly = true)
    public boolean hasActiveAssignmentForResultado(UUID resultadoId, Authentication authentication) {
        UsuarioPrincipal principal = principal(authentication);
        if (principal == null || principal.rol() != RolUsuario.PROFESOR) {
            return false;
        }
        return resultadoAprendizajeRepository.findById(resultadoId)
                .map(resultado -> hasActiveAssignment(resultado.getAsignatura().getId(), authentication))
                .orElse(false);
    }

    @Transactional(readOnly = true)
    public boolean hasActiveAssignmentForConclusion(UUID conclusionId, Authentication authentication) {
        UsuarioPrincipal principal = principal(authentication);
        if (principal == null) {
            return false;
        }
        if (principal.rol() == RolUsuario.ADMINISTRADOR) {
            return true;
        }
        if (principal.rol() != RolUsuario.PROFESOR) {
            return false;
        }
        return conclusionRepository.findById(conclusionId)
                .map(conclusion -> hasActiveAssignment(
                        conclusion.getResultadoAprendizaje().getAsignatura().getId(), authentication))
                .orElse(false);
    }

    private static UsuarioPrincipal principal(Authentication authentication) {
        return authentication != null && authentication.getPrincipal() instanceof UsuarioPrincipal p ? p : null;
    }
}
