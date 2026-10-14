package co.edu.uco.sigra.estudiantes.seguridad;

import co.edu.uco.sigra.asignaturas.seguridad.IdentidadActual;
import co.edu.uco.sigra.asignaturas.seguridad.ResolvedorIdentidad;
import co.edu.uco.sigra.common.enums.RolUsuario;
import co.edu.uco.sigra.estudiantes.controller.MatriculaController;
import co.edu.uco.sigra.estudiantes.exception.AutenticacionRequeridaException;
import co.edu.uco.sigra.estudiantes.exception.PermisoInsuficienteException;
import co.edu.uco.sigra.estudiantes.mapper.MatriculaMapper;
import co.edu.uco.sigra.estudiantes.service.MatriculaService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.method.HandlerMethod;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("Control de acceso por rol de matrículas (temporal hasta RF-05)")
class ControlAccesoMatriculaInterceptorTest {

    private static final MatriculaController CONTROLADOR =
            new MatriculaController(mock(MatriculaService.class), mock(MatriculaMapper.class));

    /**
     * Los cuatro endpoints reales de {@link MatriculaController}: todos con la misma matriz
     * ({@code ADMINISTRADOR}, {@code PROFESOR}). Se recorren los cuatro -y no uno genérico- para
     * detectar si a alguno se le olvidó la anotación {@link RolesPermitidosMatricula}.
     */
    private static final Map<String, String> METODOS = Map.of(
            "matricular", "POST",
            "desvincular", "DELETE",
            "listarMatriculados", "GET",
            "registrarYMatricular", "POST");

    private enum Resultado { PASA, NO_AUTENTICADO, SIN_PERMISO }

    static Stream<Arguments> combinaciones() {
        List<Arguments> casos = new ArrayList<>();
        List<RolUsuario> identidades = new ArrayList<>(Arrays.asList(RolUsuario.values()));
        identidades.add(null);
        for (String metodo : METODOS.keySet()) {
            for (RolUsuario rol : identidades) {
                for (boolean encendido : new boolean[]{true, false}) {
                    Resultado esperado;
                    if (!encendido) {
                        esperado = Resultado.PASA;
                    } else if (rol == null) {
                        esperado = Resultado.NO_AUTENTICADO;
                    } else if (rol == RolUsuario.ADMINISTRADOR || rol == RolUsuario.PROFESOR) {
                        esperado = Resultado.PASA;
                    } else {
                        esperado = Resultado.SIN_PERMISO;
                    }
                    casos.add(Arguments.of(metodo, rol, encendido, esperado));
                }
            }
        }
        return casos.stream();
    }

    private static HandlerMethod manejador(String nombre) {
        Method metodo = Arrays.stream(MatriculaController.class.getMethods())
                .filter(m -> m.getName().equals(nombre)).findFirst().orElseThrow();
        return new HandlerMethod(CONTROLADOR, metodo);
    }

    @SuppressWarnings("unchecked")
    private static ControlAccesoMatriculaInterceptor interceptor(RolUsuario rol, boolean encendido,
                                                                  ResolvedorIdentidad resolvedor) {
        when(resolvedor.resolver(any())).thenReturn(rol == null ? Optional.empty()
                : Optional.of(new IdentidadActual(UUID.randomUUID(), rol)));
        ObjectProvider<ResolvedorIdentidad> proveedor = mock(ObjectProvider.class);
        when(proveedor.getIfAvailable()).thenReturn(resolvedor);
        return new ControlAccesoMatriculaInterceptor(proveedor, encendido);
    }

    @ParameterizedTest(name = "{0} con rol {1}, interruptor encendido={2} → {3}")
    @MethodSource("combinaciones")
    @DisplayName("Matriz completa: endpoint × rol × interruptor")
    void matriz(String metodo, RolUsuario rol, boolean encendido, Resultado esperado) {
        ResolvedorIdentidad resolvedor = mock(ResolvedorIdentidad.class);
        ControlAccesoMatriculaInterceptor control = interceptor(rol, encendido, resolvedor);
        MockHttpServletRequest peticion = new MockHttpServletRequest(METODOS.get(metodo), "/api/v1/matriculas");

        switch (esperado) {
            case PASA -> {
                assertThat(control.preHandle(peticion, new MockHttpServletResponse(), manejador(metodo))).isTrue();
                if (!encendido) {
                    // Apagado no cambia nada: ni siquiera se consulta al resolvedor.
                    verifyNoInteractions(resolvedor);
                }
            }
            case NO_AUTENTICADO -> assertThatThrownBy(() ->
                    control.preHandle(peticion, new MockHttpServletResponse(), manejador(metodo)))
                    .isInstanceOf(AutenticacionRequeridaException.class);
            case SIN_PERMISO -> assertThatThrownBy(() ->
                    control.preHandle(peticion, new MockHttpServletResponse(), manejador(metodo)))
                    .isInstanceOf(PermisoInsuficienteException.class);
        }
    }

    @Test
    @DisplayName("Las peticiones preflight (OPTIONS) siempre pasan, aun sin token y con el interruptor encendido")
    void preflight() {
        ResolvedorIdentidad resolvedor = mock(ResolvedorIdentidad.class);
        ControlAccesoMatriculaInterceptor control = interceptor(null, true, resolvedor);

        boolean pasa = control.preHandle(new MockHttpServletRequest("OPTIONS", "/api/v1/matriculas"),
                new MockHttpServletResponse(), manejador("matricular"));

        assertThat(pasa).isTrue();
        verifyNoInteractions(resolvedor);
    }

    @Test
    @DisplayName("Si el resolvedor no está disponible, exigir identidad responde 401 y no una excepción distinta")
    void sinResolvedor() {
        @SuppressWarnings("unchecked")
        ObjectProvider<ResolvedorIdentidad> vacio = mock(ObjectProvider.class);
        when(vacio.getIfAvailable()).thenReturn(null);
        ControlAccesoMatriculaInterceptor control = new ControlAccesoMatriculaInterceptor(vacio, true);

        assertThatThrownBy(() -> control.preHandle(new MockHttpServletRequest("GET", "/api/v1/matriculas"),
                new MockHttpServletResponse(), manejador("listarMatriculados")))
                .isInstanceOf(AutenticacionRequeridaException.class);
    }

    @Test
    @DisplayName("Un manejador que no es un método de controlador pasa sin revisar")
    void manejadorNoControlador() {
        ResolvedorIdentidad resolvedor = mock(ResolvedorIdentidad.class);
        ControlAccesoMatriculaInterceptor control = interceptor(null, true, resolvedor);

        assertThat(control.preHandle(new MockHttpServletRequest("GET", "/api/v1/matriculas"),
                new MockHttpServletResponse(), new Object())).isTrue();
        verifyNoInteractions(resolvedor);
    }
}
