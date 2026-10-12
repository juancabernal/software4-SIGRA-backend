package co.edu.uco.sigra.auth.security;

import co.edu.uco.sigra.asignaturas.repository.AsignacionDocenteRepository;
import co.edu.uco.sigra.auth.repository.UsuarioRepository;
import co.edu.uco.sigra.common.enums.EstadoRegistro;
import co.edu.uco.sigra.common.enums.RolUsuario;
import co.edu.uco.sigra.estudiantes.repository.MatriculaRepository;
import co.edu.uco.sigra.resultadosaprendizaje.repository.ResultadoAprendizajeRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SecurityAuthorizationServiceTest {

    @Mock private AsignacionDocenteRepository asignacionDocenteRepository;
    @Mock private ResultadoAprendizajeRepository resultadoAprendizajeRepository;
    @Mock private MatriculaRepository matriculaRepository;

    @Test
    void estudianteSoloPuedeVerseASiMismo() {
        UUID id = UUID.randomUUID();
        SecurityAuthorizationService service = service();

        assertThat(service.canAccessStudent(id, authentication(id, RolUsuario.ESTUDIANTE))).isTrue();
        assertThat(service.canAccessStudent(UUID.randomUUID(), authentication(id, RolUsuario.ESTUDIANTE))).isFalse();
    }

    @Test
    void profesorSoloPuedeVerEstudianteConMatriculaYAsignacionActivas() {
        UUID profesor = UUID.randomUUID();
        UUID estudiante = UUID.randomUUID();
        Authentication authentication = authentication(profesor, RolUsuario.PROFESOR);
        when(matriculaRepository.countActiveByStudentAndProfessor(
                estudiante, profesor, EstadoRegistro.ACTIVO, EstadoRegistro.ACTIVO, EstadoRegistro.ACTIVO))
                .thenReturn(1L);
        SecurityAuthorizationService service = service();

        assertThat(service.canAccessStudent(estudiante, authentication)).isTrue();

    }

    @Test
    void administradorTieneAlcanceGlobalSinConsultarMatrículas() {
        SecurityAuthorizationService service = service();
        assertThat(service.canAccessStudent(UUID.randomUUID(), authentication(UUID.randomUUID(), RolUsuario.ADMINISTRADOR)))
                .isTrue();
    }

    @Test
    void principalDesconocidoFallaCerrado() {
        SecurityAuthorizationService service = service();
        Authentication authentication = new UsernamePasswordAuthenticationToken("texto", null);
        assertThat(service.canAccessStudent(UUID.randomUUID(), authentication)).isFalse();
    }

    private SecurityAuthorizationService service() {
        return new SecurityAuthorizationService(asignacionDocenteRepository,
                resultadoAprendizajeRepository, matriculaRepository);
    }

    private Authentication authentication(UUID id, RolUsuario rol) {
        return new UsernamePasswordAuthenticationToken(new UsuarioPrincipal(id, "usuario@uco.net.co", rol), null);
    }
}
