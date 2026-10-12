package co.edu.uco.sigra.asignaturas.seguridad;

import co.edu.uco.sigra.asignaturas.controller.AsignaturaController;
import co.edu.uco.sigra.asignaturas.exception.AutenticacionRequeridaException;
import co.edu.uco.sigra.asignaturas.exception.PermisoInsuficienteException;
import co.edu.uco.sigra.asignaturas.service.AsignaturaService;
import co.edu.uco.sigra.common.enums.RolUsuario;
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
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("Control de acceso por rol de asignaturas (temporal hasta RF-05)")
class ControlAccesoAsignaturasInterceptorTest {

    private static final AsignaturaController CONTROLADOR = new AsignaturaController(mock(AsignaturaService.class));

    /**
     * Matriz: ruta (método del controlador) → verbo HTTP, roles permitidos y si exige identidad aunque el
     * interruptor esté apagado («mis asignaturas» depende de quién pregunta).
     */
    private static final List<Object[]> MATRIZ = List.of(
            new Object[]{"registrarAsignatura", "POST", Set.of(RolUsuario.ADMINISTRADOR), false},
            new Object[]{"modificarAsignatura", "PUT", Set.of(RolUsuario.ADMINISTRADOR), false},
            new Object[]{"activarAsignatura", "PATCH", Set.of(RolUsuario.ADMINISTRADOR), false},
            new Object[]{"inactivarAsignatura", "PATCH", Set.of(RolUsuario.ADMINISTRADOR), false},
            new Object[]{"consultarAsignaturas", "GET", Set.of(RolUsuario.ADMINISTRADOR, RolUsuario.PROFESOR), false},
            new Object[]{"obtenerAsignatura", "GET", Set.of(RolUsuario.ADMINISTRADOR, RolUsuario.PROFESOR), false},
            new Object[]{"misAsignaturas", "GET", Set.of(RolUsuario.PROFESOR, RolUsuario.ESTUDIANTE), true});

    private enum Resultado { PASA, NO_AUTENTICADO, SIN_PERMISO }

    static Stream<Arguments> combinaciones() {
        List<Arguments> casos = new ArrayList<>();
        List<RolUsuario> identidades = new ArrayList<>(Arrays.asList(RolUsuario.values()));
        identidades.add(null);
        for (Object[] fila : MATRIZ) {
            @SuppressWarnings("unchecked")
            Set<RolUsuario> permitidos = (Set<RolUsuario>) fila[2];
            boolean siempre = (boolean) fila[3];
            for (RolUsuario rol : identidades) {
                for (boolean encendido : new boolean[]{true, false}) {
                    Resultado esperado;
                    if (!encendido && !siempre) {
                        esperado = Resultado.PASA;
                    } else if (rol == null) {
                        esperado = Resultado.NO_AUTENTICADO;
                    } else {
                        esperado = permitidos.contains(rol) ? Resultado.PASA : Resultado.SIN_PERMISO;
                    }
                    casos.add(Arguments.of(fila[0], fila[1], rol, encendido, siempre, esperado));
                }
            }
        }
        return casos.stream();
    }

    private static HandlerMethod manejador(String nombre) {
        Method metodo = Arrays.stream(AsignaturaController.class.getMethods())
                .filter(m -> m.getName().equals(nombre)).findFirst().orElseThrow();
        return new HandlerMethod(CONTROLADOR, metodo);
    }

    @SuppressWarnings("unchecked")
    private static ControlAccesoAsignaturasInterceptor interceptor(RolUsuario rol, boolean encendido,
                                                                   ResolvedorIdentidad resolvedor) {
        when(resolvedor.resolver(any())).thenReturn(rol == null ? Optional.empty()
                : Optional.of(new IdentidadActual(UUID.randomUUID(), rol)));
        ObjectProvider<ResolvedorIdentidad> proveedor = mock(ObjectProvider.class);
        when(proveedor.getIfAvailable()).thenReturn(resolvedor);
        return new ControlAccesoAsignaturasInterceptor(proveedor, encendido);
    }

    @ParameterizedTest(name = "{1} {0} con rol {2}, interruptor encendido={3} → {5}")
    @MethodSource("combinaciones")
    @DisplayName("Matriz completa: método × ruta × rol × interruptor")
    void matriz(String metodo, String verbo, RolUsuario rol, boolean encendido, boolean siempre,
                Resultado esperado) throws Exception {
        ResolvedorIdentidad resolvedor = mock(ResolvedorIdentidad.class);
        ControlAccesoAsignaturasInterceptor control = interceptor(rol, encendido, resolvedor);
        MockHttpServletRequest peticion = new MockHttpServletRequest(verbo, "/api/v1/asignaturas");

        switch (esperado) {
            case PASA -> {
                assertThat(control.preHandle(peticion, new MockHttpServletResponse(), manejador(metodo))).isTrue();
                if (encendido || siempre) {
                    IdentidadActual dejada = (IdentidadActual) peticion.getAttribute(IdentidadActual.ATRIBUTO);
                    assertThat(dejada).isNotNull();
                    assertThat(dejada.rol()).isEqualTo(rol);
                } else {
                    // Apagado no cambia nada: ni se lee el token ni se deja identidad.
                    assertThat(peticion.getAttribute(IdentidadActual.ATRIBUTO)).isNull();
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
    void preflight() throws Exception {
        ResolvedorIdentidad resolvedor = mock(ResolvedorIdentidad.class);
        ControlAccesoAsignaturasInterceptor control = interceptor(null, true, resolvedor);

        boolean pasa = control.preHandle(new MockHttpServletRequest("OPTIONS", "/api/v1/asignaturas"),
                new MockHttpServletResponse(), manejador("registrarAsignatura"));

        assertThat(pasa).isTrue();
        verifyNoInteractions(resolvedor);
    }

    @Test
    @DisplayName("Si el resolvedor no está disponible (pruebas de otros módulos), exigir identidad responde 401")
    void sinResolvedor() {
        @SuppressWarnings("unchecked")
        ObjectProvider<ResolvedorIdentidad> vacio = mock(ObjectProvider.class);
        ControlAccesoAsignaturasInterceptor control = new ControlAccesoAsignaturasInterceptor(vacio, true);

        assertThatThrownBy(() -> control.preHandle(new MockHttpServletRequest("GET", "/api/v1/asignaturas"),
                new MockHttpServletResponse(), manejador("consultarAsignaturas")))
                .isInstanceOf(AutenticacionRequeridaException.class);
    }

    @Test
    @DisplayName("Un manejador que no es un método de controlador pasa sin revisar")
    void manejadorNoControlador() throws Exception {
        ResolvedorIdentidad resolvedor = mock(ResolvedorIdentidad.class);
        ControlAccesoAsignaturasInterceptor control = interceptor(null, true, resolvedor);

        assertThat(control.preHandle(new MockHttpServletRequest("GET", "/api/v1/asignaturas"),
                new MockHttpServletResponse(), new Object())).isTrue();
    }
}
