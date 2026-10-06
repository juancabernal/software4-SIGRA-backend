package co.edu.uco.sigra.asignaturas.controller;

import co.edu.uco.sigra.asignaturas.dto.AsignaturaFiltroDTO;
import co.edu.uco.sigra.asignaturas.exception.AsignaturaExceptionHandler;
import co.edu.uco.sigra.asignaturas.seguridad.ControlAccesoAsignaturasInterceptor;
import co.edu.uco.sigra.asignaturas.seguridad.IdentidadActual;
import co.edu.uco.sigra.asignaturas.seguridad.ResolvedorIdentidad;
import co.edu.uco.sigra.asignaturas.service.AsignaturaService;
import co.edu.uco.sigra.common.enums.RolUsuario;
import co.edu.uco.sigra.common.exception.GlobalExceptionHandler;
import co.edu.uco.sigra.common.exception.ManejadorErroresTransversal;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Extremo a extremo de la capa web con el control por rol: interceptor real, manejadores de errores
 * reales y un resolvedor que entrega la identidad según el token de prueba («Bearer admin», etc.).
 */
@DisplayName("Controlador de asignaturas con control de acceso por rol")
class AsignaturaControllerRolesTest {

    private static final String URL = "/api/v1/asignaturas";
    private static final Map<String, IdentidadActual> TOKENS = Map.of(
            "admin", new IdentidadActual(UUID.randomUUID(), RolUsuario.ADMINISTRADOR),
            "profesor", new IdentidadActual(UUID.randomUUID(), RolUsuario.PROFESOR),
            "estudiante", new IdentidadActual(UUID.randomUUID(), RolUsuario.ESTUDIANTE));

    private final AsignaturaService service = mock(AsignaturaService.class);
    private final ResolvedorIdentidad resolvedor = mock(ResolvedorIdentidad.class);

    @SuppressWarnings("unchecked")
    private MockMvc mockMvc(boolean encendido) {
        when(resolvedor.resolver(any())).thenAnswer(inv -> {
            String encabezado = ((HttpServletRequest) inv.getArgument(0)).getHeader("Authorization");
            return encabezado == null ? Optional.empty()
                    : Optional.ofNullable(TOKENS.get(encabezado.replace("Bearer ", "")));
        });
        ObjectProvider<ResolvedorIdentidad> proveedor = mock(ObjectProvider.class);
        when(proveedor.getIfAvailable()).thenReturn(resolvedor);

        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        return MockMvcBuilders.standaloneSetup(new AsignaturaController(service))
                .addInterceptors(new ControlAccesoAsignaturasInterceptor(proveedor, encendido))
                .setControllerAdvice(new AsignaturaExceptionHandler(), new ManejadorErroresTransversal(),
                        new GlobalExceptionHandler())
                .setValidator(validator)
                .build();
    }

    @Nested
    @DisplayName("Interruptor encendido")
    class Encendido {

        private final MockMvc mvc = mockMvc(true);

        @Test
        @DisplayName("Sin token responde 401 con el contrato de error y WWW-Authenticate")
        void sinToken() throws Exception {
            mvc.perform(get(URL))
                    .andExpect(status().isUnauthorized())
                    .andExpect(header().string("WWW-Authenticate", "Bearer"))
                    .andExpect(jsonPath("$.status").value(401))
                    .andExpect(jsonPath("$.error").value("UNAUTHORIZED"))
                    .andExpect(jsonPath("$.mensaje").value("Debe iniciar sesión para acceder a este recurso."));
            verifyNoInteractions(service);
        }

        @Test
        @DisplayName("Un token inválido responde 401")
        void tokenInvalido() throws Exception {
            mvc.perform(get(URL).header("Authorization", "Bearer no-es-un-token"))
                    .andExpect(status().isUnauthorized());
            verifyNoInteractions(service);
        }

        @Test
        @DisplayName("Un ESTUDIANTE recibe 403 al consultar y al registrar")
        void estudiante() throws Exception {
            mvc.perform(get(URL).header("Authorization", "Bearer estudiante"))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.error").value("FORBIDDEN"))
                    .andExpect(jsonPath("$.mensaje").value("No tiene permisos suficientes para realizar esta acción."));
            mvc.perform(post(URL).header("Authorization", "Bearer estudiante")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"codigo\":\"MAT-401\",\"nombre\":\"Cálculo\",\"programaId\":\"" + UUID.randomUUID() + "\"}"))
                    .andExpect(status().isForbidden());
            verifyNoInteractions(service);
        }

        @Test
        @DisplayName("Un PROFESOR no registra ni activa (403), pero consulta con su identidad")
        void profesor() throws Exception {
            mvc.perform(patch(URL + "/" + UUID.randomUUID() + "/activar").header("Authorization", "Bearer profesor"))
                    .andExpect(status().isForbidden());
            when(service.consultar(any(AsignaturaFiltroDTO.class), any(IdentidadActual.class))).thenReturn(List.of());

            mvc.perform(get(URL).header("Authorization", "Bearer profesor")).andExpect(status().isOk());

            verify(service).consultar(any(AsignaturaFiltroDTO.class), eq(TOKENS.get("profesor")));
            verify(service, never()).activar(any());
        }

        @Test
        @DisplayName("Un ADMINISTRADOR obtiene el detalle con su identidad")
        void administrador() throws Exception {
            UUID id = UUID.randomUUID();
            mvc.perform(get(URL + "/" + id).header("Authorization", "Bearer admin"));

            verify(service).obtenerAsignatura(id, TOKENS.get("admin"));
        }
    }

    @Nested
    @DisplayName("Interruptor apagado")
    class Apagado {

        private final MockMvc mvc = mockMvc(false);

        @Test
        @DisplayName("Nada cambia: sin token se consulta como siempre y no se lee ningún token")
        void sinCambios() throws Exception {
            when(service.consultar(any(AsignaturaFiltroDTO.class))).thenReturn(List.of());

            mvc.perform(get(URL)).andExpect(status().isOk());
            mvc.perform(get(URL).header("Authorization", "Bearer estudiante")).andExpect(status().isOk());

            verify(service, times(2)).consultar(any(AsignaturaFiltroDTO.class));
            verify(service, never()).consultar(any(), any());
            verifyNoInteractions(resolvedor);
        }
    }
}
