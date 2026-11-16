package co.edu.uco.sigra.reportes.controller;

import co.edu.uco.sigra.auth.entity.Usuario;
import co.edu.uco.sigra.auth.repository.UsuarioRepository;
import co.edu.uco.sigra.auth.security.CurrentUser;
import co.edu.uco.sigra.auth.service.JwtService;
import co.edu.uco.sigra.common.enums.EstadoRegistro;
import co.edu.uco.sigra.common.enums.RolUsuario;
import co.edu.uco.sigra.config.SecurityConfig;
import co.edu.uco.sigra.reportes.dto.*;
import co.edu.uco.sigra.reportes.service.AccionesMejoraReporteService;
import co.edu.uco.sigra.reportes.exception.ReporteRecursoNoEncontradoException;
import co.edu.uco.sigra.semestres.entity.EstadoSemestre;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = ReporteAccionesMejoraController.class)
@Import({SecurityConfig.class, CurrentUser.class})
class ReporteAccionesMejoraControllerTest {
    private static final String RUTA = "/api/v1/reportes/acciones-mejora";
    @Autowired private MockMvc mockMvc;
    @MockitoBean private AccionesMejoraReporteService service;
    @MockitoBean private JwtService jwtService;
    @MockitoBean private UsuarioRepository usuarioRepository;

    @Test
    void sinTokenEs401() throws Exception {
        mockMvc.perform(get(RUTA)).andExpect(status().isUnauthorized());
        verifyNoInteractions(service);
    }

    @ParameterizedTest
    @EnumSource(value = RolUsuario.class, names = {"ESTUDIANTE"})
    void otrosRolesNoPuedenConsultar(RolUsuario rol) throws Exception {
        configurarToken("token", rol);
        mockMvc.perform(get(RUTA).header("Authorization", "Bearer token"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(service);
    }

    @Test
    void administradorConsultaYEnviaFiltroFalse() throws Exception {
        configurarToken("token", RolUsuario.ADMINISTRADOR);
        UUID semestre = UUID.randomUUID();
        when(service.consultar(eq(semestre), isNull(), isNull(), eq(false), any())).thenReturn(
            new AccionesMejoraReporteResponseDTO(
                new AccionesMejoraReporteResponseDTO.Resumen(0, 0, 0, 0), List.of()));
        mockMvc.perform(get(RUTA).param("semestreId", semestre.toString()).param("convertida", "false")
                .header("Authorization", "Bearer token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resumen.total").value(0))
                .andExpect(jsonPath("$.filas").isEmpty());
        verify(service).consultar(eq(semestre), isNull(), isNull(), eq(false),
                argThat(p -> p.rol() == RolUsuario.ADMINISTRADOR));
    }

    @Test
    void identificadorInvalidoEs400() throws Exception {
        configurarToken("token", RolUsuario.ADMINISTRADOR);
        mockMvc.perform(get(RUTA).param("semestreId", "invalido")
                .header("Authorization", "Bearer token")).andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }


    @Test
    void profesorPuedeConsultarConSuIdentidadAutenticada() throws Exception {
        configurarToken("profesor-token", RolUsuario.PROFESOR);
        when(service.consultar(isNull(), isNull(), isNull(), isNull(), any())).thenReturn(
                new AccionesMejoraReporteResponseDTO(new AccionesMejoraReporteResponseDTO.Resumen(0, 0, 0, 0), List.of()));
        mockMvc.perform(get(RUTA).header("Authorization", "Bearer profesor-token"))
                .andExpect(status().isOk());
        verify(service).consultar(isNull(), isNull(), isNull(), isNull(),
                argThat(p -> p.rol() == RolUsuario.PROFESOR && p.id() != null));
    }

    private void configurarToken(String token, RolUsuario rol) {
        UUID id = UUID.randomUUID();
        Claims claims = mock(Claims.class);
        when(jwtService.extraerClaims(token)).thenReturn(claims);
        when(claims.getSubject()).thenReturn(id.toString());
        when(claims.get("rol", String.class)).thenReturn(rol.name());
        when(claims.get("correo", String.class)).thenReturn(rol.name().toLowerCase() + "@uco.net.co");
        when(usuarioRepository.findById(id)).thenReturn(Optional.of(new UsuarioStub(id, rol)));
    }

    private static final class UsuarioStub extends Usuario {
        private final RolUsuario rol;
        private UsuarioStub(UUID id, RolUsuario rol) {
            this.rol = rol;
            setId(id);
            setCorreoInstitucional(rol.name().toLowerCase() + "@uco.net.co");
            setEstado(EstadoRegistro.ACTIVO);
        }
        @Override public RolUsuario getRol() { return rol; }
    }
}
