package co.edu.uco.sigra.auditoria.aspect;

import co.edu.uco.sigra.auditoria.entity.AccionAuditoria;
import co.edu.uco.sigra.auditoria.entity.TipoEntidadAuditoria;
import co.edu.uco.sigra.auditoria.prueba.service.ProgramaService;
import co.edu.uco.sigra.auditoria.prueba.service.ProgramaServiceImpl;
import co.edu.uco.sigra.auditoria.service.AuditoriaService;
import co.edu.uco.sigra.auth.dto.LoginRequestDTO;
import co.edu.uco.sigra.auth.dto.LoginResponseDTO;
import co.edu.uco.sigra.auth.dto.UsuarioSesionDTO;
import co.edu.uco.sigra.auth.exception.CredencialesInvalidasException;
import co.edu.uco.sigra.auth.exception.UsuarioBloqueadoException;
import co.edu.uco.sigra.auth.service.AuthService;
import co.edu.uco.sigra.calificaciones.dto.CalificacionModificarRequestDTO;
import co.edu.uco.sigra.calificaciones.dto.CalificacionRequestDTO;
import co.edu.uco.sigra.calificaciones.dto.CalificacionResponseDTO;
import co.edu.uco.sigra.calificaciones.service.CalificacionService;
import co.edu.uco.sigra.common.enums.RolUsuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;

/**
 * El aspecto real aplicado con {@link AspectJProxyFactory} (el mismo motor de Spring AOP, sin levantar
 * el contexto): comprueba qué eventos se auditan y que una falla al auditar no tumba la operación.
 */
@DisplayName("Aspecto de auditoría")
class AuditoriaAspectTest {

    private AuditoriaService auditoria;

    @BeforeEach
    void setUp() {
        auditoria = mock(AuditoriaService.class);
    }

    @SuppressWarnings("unchecked")
    private <T> T conAspecto(T objetivo) {
        AspectJProxyFactory fabrica = new AspectJProxyFactory(objetivo);
        fabrica.addAspect(new AuditoriaAspect(auditoria));
        return (T) fabrica.getProxy();
    }

    @Nested
    @DisplayName("Cambios de estado")
    class CambiosDeEstado {

        @Test
        @DisplayName("inactivar en un *Service queda auditado con la entidad, el id y el estado resultante")
        void inactivar() {
            ProgramaService servicio = conAspecto(new ProgramaServiceImpl());
            UUID id = UUID.randomUUID();

            servicio.inactivar(id);

            verify(auditoria).registrarEvento(TipoEntidadAuditoria.PROGRAMA, id, AccionAuditoria.CAMBIO_ESTADO,
                    "Cambio de estado: inactivar (nuevo estado: INACTIVO)");
        }

        @Test
        @DisplayName("Un método void (desvincular) también se audita, con el primer UUID como entidad")
        void metodoVoid() {
            ProgramaService servicio = conAspecto(new ProgramaServiceImpl());
            UUID id = UUID.randomUUID();

            servicio.desvincular(id, "retiro");

            verify(auditoria).registrarEvento(TipoEntidadAuditoria.PROGRAMA, id, AccionAuditoria.CAMBIO_ESTADO,
                    "Cambio de estado: desvincular");
        }

        @Test
        @DisplayName("Una consulta no es un cambio de estado: no se audita")
        void consultaNoSeAudita() {
            ProgramaService servicio = conAspecto(new ProgramaServiceImpl());

            servicio.consultar(UUID.randomUUID());

            verifyNoInteractions(auditoria);
        }

        @Test
        @DisplayName("Si la operación falla no se audita nada: no hubo cambio")
        void operacionFallida() {
            ProgramaService servicio = conAspecto(new ProgramaServiceImpl());

            assertThatThrownBy(() -> servicio.inactivar(ProgramaServiceImpl.ID_QUE_FALLA))
                    .isInstanceOf(IllegalStateException.class);
            verifyNoInteractions(auditoria);
        }

        @Test
        @DisplayName("Si auditar falla, la operación ya hecha igual responde")
        void fallaAlAuditar() {
            doThrow(new IllegalStateException("base caída")).when(auditoria)
                    .registrarEvento(any(), any(), any(), any());
            ProgramaService servicio = conAspecto(new ProgramaServiceImpl());
            UUID id = UUID.randomUUID();

            assertThat(servicio.inactivar(id).estado()).isEqualTo("INACTIVO");
        }
    }

    @Nested
    @DisplayName("Inicio de sesión")
    class Login {

        private final LoginRequestDTO solicitud = new LoginRequestDTO("profesor@uco.net.co", "Clave123*");

        @Test
        @DisplayName("Login exitoso: evento con el usuario y su rol")
        void exitoso() {
            UUID usuario = UUID.randomUUID();
            AuthService auth = mock(AuthService.class);
            when(auth.autenticar(solicitud)).thenReturn(new LoginResponseDTO("jwt", "Bearer", 3600,
                    new UsuarioSesionDTO(usuario, "Profesor", "profesor@uco.net.co", RolUsuario.PROFESOR)));

            conAspecto(auth).autenticar(solicitud);

            verify(auditoria).registrarEvento(eq(TipoEntidadAuditoria.USUARIO), eq(usuario),
                    eq(AccionAuditoria.LOGIN_EXITOSO), contains("PROFESOR"), eq(usuario), eq(RolUsuario.PROFESOR));
        }

        @Test
        @DisplayName("Credenciales inválidas: evento LOGIN_FALLIDO sin usuario ni correo")
        void credencialesInvalidas() {
            AuthService auth = mock(AuthService.class);
            when(auth.autenticar(solicitud)).thenThrow(new CredencialesInvalidasException());

            assertThatThrownBy(() -> conAspecto(auth).autenticar(solicitud))
                    .isInstanceOf(CredencialesInvalidasException.class);

            verify(auditoria).registrarEvento(eq(TipoEntidadAuditoria.USUARIO), isNull(),
                    eq(AccionAuditoria.LOGIN_FALLIDO), contains("credenciales"), isNull(), isNull());
        }

        @Test
        @DisplayName("Usuario bloqueado: evento LOGIN_FALLIDO con ese motivo")
        void bloqueado() {
            AuthService auth = mock(AuthService.class);
            when(auth.autenticar(solicitud)).thenThrow(new UsuarioBloqueadoException());

            assertThatThrownBy(() -> conAspecto(auth).autenticar(solicitud))
                    .isInstanceOf(UsuarioBloqueadoException.class);

            verify(auditoria).registrarEvento(eq(TipoEntidadAuditoria.USUARIO), isNull(),
                    eq(AccionAuditoria.LOGIN_FALLIDO), contains("bloqueado"), isNull(), isNull());
        }

        @Test
        @DisplayName("Un error que no es de credenciales (p. ej. base caída) no cuenta como intento de login")
        void otroError() {
            AuthService auth = mock(AuthService.class);
            when(auth.autenticar(solicitud)).thenThrow(new IllegalStateException("base caída"));

            assertThatThrownBy(() -> conAspecto(auth).autenticar(solicitud))
                    .isInstanceOf(IllegalStateException.class);
            verifyNoInteractions(auditoria);
        }
    }

    @Nested
    @DisplayName("Calificaciones")
    class Calificaciones {

        private final UUID evaluacion = UUID.randomUUID();

        private CalificacionResponseDTO respuesta(UUID id, boolean fueraDeFecha) {
            return new CalificacionResponseDTO(id, evaluacion, UUID.randomUUID(), UUID.randomUUID(), "Ana",
                    new BigDecimal("3.5"), "Buen desarrollo del parcial", LocalDateTime.now(), null, fueraDeFecha);
        }

        @Test
        @DisplayName("Una nota registrada fuera de fecha se audita")
        void fueraDeFecha() {
            UUID id = UUID.randomUUID();
            CalificacionService servicio = mock(CalificacionService.class);
            when(servicio.registrar(any(), any(), any())).thenReturn(respuesta(id, true));

            conAspecto(servicio).registrar(evaluacion,
                    new CalificacionRequestDTO(UUID.randomUUID(), new BigDecimal("3.5"), "Buen desarrollo"), null);

            verify(auditoria).registrarEvento(eq(TipoEntidadAuditoria.CALIFICACION), eq(id),
                    eq(AccionAuditoria.CALIFICACION_FUERA_DE_FECHA), contains("después de la fecha tentativa"));
        }

        @Test
        @DisplayName("Una nota registrada a tiempo no genera evento de auditoría")
        void aTiempo() {
            CalificacionService servicio = mock(CalificacionService.class);
            when(servicio.registrar(any(), any(), any())).thenReturn(respuesta(UUID.randomUUID(), false));

            conAspecto(servicio).registrar(evaluacion,
                    new CalificacionRequestDTO(UUID.randomUUID(), new BigDecimal("3.5"), "Buen desarrollo"), null);

            verifyNoInteractions(auditoria);
        }

        @Test
        @DisplayName("Toda modificación de una nota se audita")
        void modificacion() {
            UUID id = UUID.randomUUID();
            CalificacionService servicio = mock(CalificacionService.class);
            when(servicio.modificar(any(), any(), any())).thenReturn(respuesta(id, false));

            conAspecto(servicio).modificar(id,
                    new CalificacionModificarRequestDTO(new BigDecimal("3.5"), "Buen desarrollo"), null);

            verify(auditoria).registrarEvento(eq(TipoEntidadAuditoria.CALIFICACION), eq(id),
                    eq(AccionAuditoria.CALIFICACION_MODIFICADA), contains("3.5"));
        }
    }
}