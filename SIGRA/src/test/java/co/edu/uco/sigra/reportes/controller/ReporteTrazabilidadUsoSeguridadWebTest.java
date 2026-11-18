package co.edu.uco.sigra.reportes.controller;

import co.edu.uco.sigra.auditoria.entity.AccionAuditoria;
import co.edu.uco.sigra.auth.entity.Usuario;
import co.edu.uco.sigra.auth.repository.UsuarioRepository;
import co.edu.uco.sigra.auth.security.CurrentUser;
import co.edu.uco.sigra.auth.service.JwtService;
import co.edu.uco.sigra.common.enums.EstadoRegistro;
import co.edu.uco.sigra.common.enums.RolUsuario;
import co.edu.uco.sigra.config.SecurityConfig;
import co.edu.uco.sigra.reportes.dto.ActividadPropiaDTO;
import co.edu.uco.sigra.reportes.dto.ActividadRolDTO;
import co.edu.uco.sigra.reportes.dto.ConteoAccionDTO;
import co.edu.uco.sigra.reportes.dto.ResumenActividadEstudiantesDTO;
import co.edu.uco.sigra.reportes.dto.TrazabilidadUsoProfesorResponseDTO;
import co.edu.uco.sigra.reportes.dto.TrazabilidadUsoReporteResponseDTO;
import co.edu.uco.sigra.reportes.dto.TrazabilidadUsoResumenDTO;
import co.edu.uco.sigra.reportes.exception.RangoReporteInvalidoException;
import co.edu.uco.sigra.reportes.service.TrazabilidadUsoReporteService;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ReporteTrazabilidadUsoController.class)
@Import({SecurityConfig.class, CurrentUser.class})
@DisplayName("Seguridad del reporte R7 de trazabilidad de uso (cadena real)")
class ReporteTrazabilidadUsoSeguridadWebTest {

    private static final String RUTA = "/api/v1/reportes/trazabilidad-uso";
    private static final String RUTA_PROFESOR = RUTA + "/profesor";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TrazabilidadUsoReporteService service;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UsuarioRepository usuarioRepository;

    @Test
    @DisplayName("Sin token ambas rutas devuelven 401")
    void anonimoEs401() throws Exception {
        mockMvc.perform(get(RUTA)).andExpect(status().isUnauthorized());
        mockMvc.perform(get(RUTA_PROFESOR)).andExpect(status().isUnauthorized());
        verifyNoInteractions(service);
    }

    @Test
    @DisplayName("Profesor y estudiante no ven el reporte general")
    void generalSoloAdministrador() throws Exception {
        configurarToken("token-profesor", RolUsuario.PROFESOR);
        configurarToken("token-estudiante", RolUsuario.ESTUDIANTE);

        mockMvc.perform(get(RUTA).header("Authorization", "Bearer token-profesor"))
                .andExpect(status().isForbidden());
        mockMvc.perform(get(RUTA).header("Authorization", "Bearer token-estudiante"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(service);
    }

    @Test
    @DisplayName("Administrador y estudiante no ven la vista del profesor")
    void vistaProfesorSoloProfesor() throws Exception {
        configurarToken("token-admin", RolUsuario.ADMINISTRADOR);
        configurarToken("token-estudiante", RolUsuario.ESTUDIANTE);

        mockMvc.perform(get(RUTA_PROFESOR).header("Authorization", "Bearer token-admin"))
                .andExpect(status().isForbidden());
        mockMvc.perform(get(RUTA_PROFESOR).header("Authorization", "Bearer token-estudiante"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(service);
    }

    @Test
    @DisplayName("El administrador consulta el resumen por rol sin identificadores de usuario")
    void administradorConsulta() throws Exception {
        configurarToken("token-admin", RolUsuario.ADMINISTRADOR);
        LocalDate hoy = LocalDate.of(2026, 11, 15);
        when(service.consultarGeneral(eq(hoy.minusDays(6)), eq(hoy), isNull(), eq(RolUsuario.PROFESOR)))
                .thenReturn(new TrazabilidadUsoReporteResponseDTO(hoy.minusDays(6), hoy, null, null,
                        RolUsuario.PROFESOR, new TrazabilidadUsoResumenDTO(3, 1, 3, 0),
                        List.of(new ActividadRolDTO("PROFESOR", 3, 1, null,
                                List.of(new ConteoAccionDTO(AccionAuditoria.LOGIN_EXITOSO, 3))))));

        mockMvc.perform(get(RUTA).param("desde", "2026-11-09").param("hasta", "2026-11-15")
                        .param("rol", "PROFESOR").header("Authorization", "Bearer token-admin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resumen.totalEventos").value(3))
                .andExpect(jsonPath("$.porRol[0].rol").value("PROFESOR"))
                .andExpect(jsonPath("$.porRol[0].acciones[0].accion").value("LOGIN_EXITOSO"))
                .andExpect(jsonPath("$..usuarioId").isEmpty())
                .andExpect(jsonPath("$..detalle").isEmpty());
    }

    @Test
    @DisplayName("Un rol o una fecha mal formados devuelven 400")
    void parametrosMalFormados() throws Exception {
        configurarToken("token-admin", RolUsuario.ADMINISTRADOR);

        mockMvc.perform(get(RUTA).param("rol", "INVITADO").header("Authorization", "Bearer token-admin"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get(RUTA).param("desde", "15/11/2026").header("Authorization", "Bearer token-admin"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test
    @DisplayName("Un rango inválido del servicio se traduce a 400")
    void rangoInvalidoEs400() throws Exception {
        configurarToken("token-admin", RolUsuario.ADMINISTRADOR);
        when(service.consultarGeneral(any(), any(), any(), any()))
                .thenThrow(new RangoReporteInvalidoException("La fecha inicial no puede ser posterior a la fecha final."));

        mockMvc.perform(get(RUTA).header("Authorization", "Bearer token-admin"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("BAD_REQUEST"));
    }

    @Test
    @DisplayName("El profesor consulta su vista con su propia identidad")
    void profesorConsulta() throws Exception {
        UUID profesorId = configurarToken("token-profesor", RolUsuario.PROFESOR);
        when(service.consultarProfesor(any(), any(), any(), any()))
                .thenReturn(new TrazabilidadUsoProfesorResponseDTO(LocalDate.of(2026, 7, 1),
                        LocalDate.of(2026, 12, 15), null, null, new ActividadPropiaDTO(0, null, List.of()),
                        new ResumenActividadEstudiantesDTO(0, 0, 0, 0), List.of(), null));

        mockMvc.perform(get(RUTA_PROFESOR).header("Authorization", "Bearer token-profesor"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.propia.totalEventos").value(0));
        verify(service).consultarProfesor(isNull(), isNull(), isNull(),
                org.mockito.ArgumentMatchers.argThat(p -> p.id().equals(profesorId)));
    }

    private UUID configurarToken(String token, RolUsuario rol) {
        UUID id = UUID.randomUUID();
        String correo = rol.name().toLowerCase() + "@uco.net.co";
        Claims claims = mock(Claims.class);
        when(jwtService.extraerClaims(token)).thenReturn(claims);
        when(claims.getSubject()).thenReturn(id.toString());
        when(claims.get("rol", String.class)).thenReturn(rol.name());
        when(claims.get("correo", String.class)).thenReturn(correo);
        when(usuarioRepository.findById(id)).thenReturn(Optional.of(new UsuarioStub(id, correo, rol)));
        return id;
    }

    private static final class UsuarioStub extends Usuario {
        private final RolUsuario rol;

        private UsuarioStub(UUID id, String correo, RolUsuario rol) {
            this.rol = rol;
            setId(id);
            setCorreoInstitucional(correo);
            setEstado(EstadoRegistro.ACTIVO);
        }

        @Override
        public RolUsuario getRol() {
            return rol;
        }
    }
}
