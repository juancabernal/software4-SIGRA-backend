package co.edu.uco.sigra.asignaturas.controller;

import co.edu.uco.sigra.asignaturas.dto.ResultadoAsignacionMasivaDTO;
import co.edu.uco.sigra.asignaturas.service.AsignacionDocenteService;
import co.edu.uco.sigra.auth.entity.Usuario;
import co.edu.uco.sigra.auth.repository.UsuarioRepository;
import co.edu.uco.sigra.auth.security.CurrentUser;
import co.edu.uco.sigra.auth.service.JwtService;
import co.edu.uco.sigra.common.enums.EstadoRegistro;
import co.edu.uco.sigra.common.enums.RolUsuario;
import co.edu.uco.sigra.config.SecurityConfig;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Prueba RBAC real para RF-07, sin PostgreSQL: token JWT, filtros y @PreAuthorize activos. */
@WebMvcTest(controllers = AsignacionDocenteController.class)
@Import({SecurityConfig.class, CurrentUser.class})
class AsignacionDocenteSecurityWebTest {

    private static final String URL = "/api/v1/asignaciones-docentes";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AsignacionDocenteService service;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UsuarioRepository usuarioRepository;

    @Test
    void postSinTokenResponde401SinInvocarServicio() throws Exception {
        mockMvc.perform(post(URL).contentType("application/json").content(cuerpoValido()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("UNAUTHORIZED"));

        verifyNoInteractions(service);
    }

    @Test
    void postConRolProfesorResponde403SinInvocarServicio() throws Exception {
        configurarToken("profesor-token", RolUsuario.PROFESOR);

        mockMvc.perform(post(URL).header("Authorization", "Bearer profesor-token")
                        .contentType("application/json").content(cuerpoValido()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("FORBIDDEN"));

        verifyNoInteractions(service);
    }

    @Test
    void postConAdministradorResponde201YDelega() throws Exception {
        configurarToken("admin-post-token", RolUsuario.ADMINISTRADOR);
        when(service.asignarMasivo(any())).thenReturn(new ResultadoAsignacionMasivaDTO(List.of(), List.of(), List.of()));

        mockMvc.perform(post(URL).header("Authorization", "Bearer admin-post-token")
                        .contentType("application/json").content(cuerpoValido()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.creadas").isArray());

        verify(service).asignarMasivo(any());
    }

    @Test
    void patchSinTokenResponde401SinInvocarServicio() throws Exception {
        mockMvc.perform(patch(URL + "/" + UUID.randomUUID() + "/desasignar"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("UNAUTHORIZED"));

        verifyNoInteractions(service);
    }

    @Test
    void patchConRolEstudianteResponde403SinInvocarServicio() throws Exception {
        configurarToken("estudiante-token", RolUsuario.ESTUDIANTE);

        mockMvc.perform(patch(URL + "/" + UUID.randomUUID() + "/desasignar")
                        .header("Authorization", "Bearer estudiante-token"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("FORBIDDEN"));

        verifyNoInteractions(service);
    }

    @Test
    void patchConAdministradorResponde204YDelega() throws Exception {
        configurarToken("admin-patch-token", RolUsuario.ADMINISTRADOR);
        UUID id = UUID.randomUUID();

        mockMvc.perform(patch(URL + "/" + id + "/desasignar")
                        .header("Authorization", "Bearer admin-patch-token"))
                .andExpect(status().isNoContent());

        verify(service).desasignar(id);
    }

    private static String cuerpoValido() {
        return "{\"profesorIds\":[\"" + UUID.randomUUID() + "\"],\"asignaturaIds\":[\""
                + UUID.randomUUID() + "\"]}";
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

        @Override
        public RolUsuario getRol() {
            return rol;
        }
    }
}
