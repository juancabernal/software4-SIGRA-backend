package co.edu.uco.sigra.estudiantes.controller;

import co.edu.uco.sigra.auth.entity.Usuario;
import co.edu.uco.sigra.auth.repository.UsuarioRepository;
import co.edu.uco.sigra.auth.security.CurrentUser;
import co.edu.uco.sigra.auth.service.JwtService;
import co.edu.uco.sigra.common.enums.EstadoRegistro;
import co.edu.uco.sigra.common.enums.RolUsuario;
import co.edu.uco.sigra.config.SecurityConfig;
import co.edu.uco.sigra.estudiantes.dto.*;
import co.edu.uco.sigra.estudiantes.exception.MatriculaNoEncontradaException;
import co.edu.uco.sigra.estudiantes.service.MiProcesoService;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = MiProcesoController.class)
@Import({SecurityConfig.class, CurrentUser.class})
class MiProcesoControllerTest {

    private static final UUID MATRICULA_ID = UUID.randomUUID();

    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private MiProcesoService service;
    @MockitoBean
    private JwtService jwtService;
    @MockitoBean
    private UsuarioRepository usuarioRepository;

    static Stream<String> rutas() {
        return Stream.of(
                "/api/v1/estudiantes/me/materias",
                "/api/v1/estudiantes/me/materias/" + MATRICULA_ID,
                "/api/v1/estudiantes/me/progreso",
                "/api/v1/estudiantes/me/historial");
    }

    static Stream<Arguments> rolesYRutas() {
        return Stream.of(RolUsuario.ADMINISTRADOR, RolUsuario.PROFESOR)
                .flatMap(rol -> rutas().map(ruta -> Arguments.of(rol, ruta)));
    }

    @ParameterizedTest
    @MethodSource("rutas")
    void estudianteObtiene200EnLasCuatroRutasSinCamposProhibidos(String ruta) throws Exception {
        configurarRespuesta(ruta);
        configurarToken("estudiante-token", RolUsuario.ESTUDIANTE);

        mockMvc.perform(get(ruta).header("Authorization", "Bearer estudiante-token"))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("estudianteId"))))
                .andExpect(content().string(not(containsString("usuarioId"))))
                .andExpect(content().string(not(containsString("asignaturaId"))))
                .andExpect(content().string(not(containsString("semestreId"))))
                .andExpect(content().string(not(containsString("fueraDeFecha"))))
                .andExpect(content().string(not(containsString("rolUsuario"))))
                .andExpect(result -> {
                    if (ruta.contains("/materias/") || ruta.endsWith("/progreso")) {
                        org.assertj.core.api.Assertions.assertThat(result.getResponse().getContentAsString())
                                .contains("SIN_EVALUAR");
                    }
                });
    }

    @ParameterizedTest
    @MethodSource("rutas")
    void sinTokenRecibe401EnLasCuatroRutas(String ruta) throws Exception {
        mockMvc.perform(get(ruta)).andExpect(status().isUnauthorized());
    }

    @ParameterizedTest
    @MethodSource("rolesYRutas")
    void rolesDistintosDeEstudianteReciben403EnLasCuatroRutas(RolUsuario rol, String ruta) throws Exception {
        String token = rol.name().toLowerCase() + "-token";
        configurarToken(token, rol);

        mockMvc.perform(get(ruta).header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void uuidInvalidoRecibe400() throws Exception {
        configurarToken("estudiante-token", RolUsuario.ESTUDIANTE);

        mockMvc.perform(get("/api/v1/estudiantes/me/materias/no-es-uuid")
                        .header("Authorization", "Bearer estudiante-token"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void materiaNoEncontradaRecibe404() throws Exception {
        configurarToken("estudiante-token", RolUsuario.ESTUDIANTE);
        when(service.consultarMateria(any())).thenThrow(MatriculaNoEncontradaException.porId(UUID.randomUUID()));

        mockMvc.perform(get("/api/v1/estudiantes/me/materias/" + MATRICULA_ID)
                        .header("Authorization", "Bearer estudiante-token"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensaje").value("No existe la matrícula solicitada."));
    }

    private void configurarRespuesta(String ruta) {
        MiMateriaDTO materia = new MiMateriaDTO(
                MATRICULA_ID, "MAT-1", "Materia", "Programa", "2026-1",
                LocalDate.of(2026, 1, 1), SituacionMateria.EN_CURSO);
        MiEvaluacionDTO evaluacion = new MiEvaluacionDTO(
                "Evaluación", "Descripción", LocalDate.of(2026, 2, 1),
                java.math.BigDecimal.TEN, null, null);
        MiResultadoAprendizajeDTO resultado = new MiResultadoAprendizajeDTO(
                "RA-1", "Resultado", MiNivelLogro.SIN_EVALUAR, null, 0, 1,
                List.of(evaluacion));
        MiMateriaDetalleDTO detalle = new MiMateriaDetalleDTO(
                MATRICULA_ID, "MAT-1", "Materia", "Programa", "2026-1",
                LocalDate.of(2026, 1, 1), SituacionMateria.EN_CURSO, List.of(resultado));
        if (ruta.endsWith("/materias")) {
            when(service.listarMaterias()).thenReturn(List.of(materia));
        } else if (ruta.contains("/materias/")) {
            when(service.consultarMateria(MATRICULA_ID)).thenReturn(detalle);
        } else if (ruta.endsWith("/progreso")) {
            when(service.consultarProgreso()).thenReturn(List.of(detalle));
        } else {
            when(service.consultarHistorial()).thenReturn(List.of(materia));
        }
    }

    private void configurarToken(String token, RolUsuario rol) {
        UUID id = UUID.randomUUID();
        Claims claims = mock(Claims.class);
        when(jwtService.extraerClaims(token)).thenReturn(claims);
        when(claims.getSubject()).thenReturn(id.toString());
        when(claims.get("rol", String.class)).thenReturn(rol.name());
        when(claims.get("correo", String.class)).thenReturn(rol.name().toLowerCase() + "@uco.net.co");
        Usuario usuario = new UsuarioStub(id, rol.name().toLowerCase() + "@uco.net.co", rol);
        when(usuarioRepository.findById(id)).thenReturn(Optional.of(usuario));
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
