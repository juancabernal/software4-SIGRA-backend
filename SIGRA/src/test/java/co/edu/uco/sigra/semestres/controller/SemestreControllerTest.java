package co.edu.uco.sigra.semestres.controller;

import co.edu.uco.sigra.common.exception.GlobalExceptionHandler;
import co.edu.uco.sigra.semestres.dto.SemestreFechaFinRequestDTO;
import co.edu.uco.sigra.semestres.dto.SemestreRequestDTO;
import co.edu.uco.sigra.semestres.dto.SemestreResponseDTO;
import co.edu.uco.sigra.semestres.entity.EstadoSemestre;
import co.edu.uco.sigra.semestres.exception.SemestreDuplicadoException;
import co.edu.uco.sigra.semestres.exception.SemestreExceptionHandler;
import co.edu.uco.sigra.semestres.exception.SemestreNoEncontradoException;
import co.edu.uco.sigra.semestres.exception.SemestreReglaNegocioException;
import co.edu.uco.sigra.semestres.exception.SemestreSolapadoException;
import co.edu.uco.sigra.semestres.service.SemestreService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Pruebas de la capa web en modo standalone: no levanta contexto Spring, base de datos ni seguridad.
 * Verifica rutas, códigos HTTP, validaciones del DTO y el formato de error. Se registran el
 * manejador del módulo y el global de common, en el mismo orden de prioridad que en ejecución.
 */
@ExtendWith(MockitoExtension.class)
class SemestreControllerTest {

    private static final String URL = "/api/v1/semestres";
    private static final String CODIGO = "2027-1";
    private static final String CODIGO_OTRO = "2026-2";
    private static final String CODIGO_INEXISTENTE = "2030-1";
    private static final String URL_CODIGO = URL + "/" + CODIGO;
    private static final String URL_INEXISTENTE = URL + "/" + CODIGO_INEXISTENTE;
    private static final String ESTADO_INACTIVO = EstadoSemestre.INACTIVO.name();

    private static final String JSON_MENSAJE = "$.mensaje";
    private static final String JSON_STATUS = "$.status";
    private static final String JSON_CODIGO = "$.codigo";

    /** Cuerpo de creación válido, reutilizado por los casos que delegan el resultado al servicio. */
    private static final String CUERPO_CREACION_VALIDO = """
            {"codigo":"2027-1","fechaInicio":"2027-01-20","fechaFin":"2027-06-10"}
            """;

    @Mock
    private SemestreService service;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders.standaloneSetup(new SemestreController(service))
                .setControllerAdvice(new SemestreExceptionHandler(), new GlobalExceptionHandler())
                .setValidator(validator)
                .build();
    }

    private static SemestreResponseDTO respuesta(String codigo) {
        return new SemestreResponseDTO(UUID.randomUUID(), codigo,
                LocalDate.of(2027, 1, 20), LocalDate.of(2027, 6, 10), ESTADO_INACTIVO);
    }

    @Test
    void crearDevuelve201() throws Exception {
        when(service.crear(any(SemestreRequestDTO.class))).thenReturn(respuesta(CODIGO));

        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(CUERPO_CREACION_VALIDO))
                .andExpect(status().isCreated())
                .andExpect(jsonPath(JSON_CODIGO).value(CODIGO))
                .andExpect(jsonPath("$.fechaInicio").value("2027-01-20"))
                .andExpect(jsonPath("$.estado").value(ESTADO_INACTIVO));
    }

    @Test
    void crearConCodigoMalFormadoDevuelve400SinLlamarAlServicio() throws Exception {
        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"codigo":"2027-3","fechaInicio":"2027-01-20","fechaFin":"2027-06-10"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath(JSON_STATUS).value(400))
                .andExpect(jsonPath("$.detalles[0]").value(org.hamcrest.Matchers.startsWith("codigo")));
        verifyNoInteractions(service);
    }

    @Test
    void crearSinFechasDevuelve400() throws Exception {
        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"codigo":"2027-1"}
                                """))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test
    void crearConFechaMalFormadaDevuelve400() throws Exception {
        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"codigo":"2027-1","fechaInicio":"20/01/2027","fechaFin":"2027-06-10"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath(JSON_MENSAJE).exists());
        verifyNoInteractions(service);
    }

    @Test
    void crearDuplicadoDevuelve409() throws Exception {
        when(service.crear(any())).thenThrow(new SemestreDuplicadoException(CODIGO));

        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(CUERPO_CREACION_VALIDO))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("CONFLICT"));
    }

    @Test
    void crearSolapadoDevuelve409() throws Exception {
        when(service.crear(any())).thenThrow(new SemestreSolapadoException());

        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(CUERPO_CREACION_VALIDO))
                .andExpect(status().isConflict());
    }

    @Test
    void reglaDeNegocioDevuelve400() throws Exception {
        String mensaje = "La fecha de inicio debe ser hoy o una fecha futura";
        when(service.crear(any())).thenThrow(new SemestreReglaNegocioException(mensaje));

        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"codigo":"2025-1","fechaInicio":"2025-01-20","fechaFin":"2025-06-10"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath(JSON_MENSAJE).value(mensaje));
    }

    @Test
    void listarDevuelve200() throws Exception {
        when(service.listar()).thenReturn(List.of(respuesta(CODIGO), respuesta(CODIGO_OTRO)));

        mockMvc.perform(get(URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void consultarExistenteDevuelve200() throws Exception {
        when(service.consultarPorCodigo(CODIGO)).thenReturn(respuesta(CODIGO));

        mockMvc.perform(get(URL_CODIGO))
                .andExpect(status().isOk())
                .andExpect(jsonPath(JSON_CODIGO).value(CODIGO));
    }

    @Test
    void consultarInexistenteDevuelve404() throws Exception {
        when(service.consultarPorCodigo(CODIGO_INEXISTENTE))
                .thenThrow(new SemestreNoEncontradoException(CODIGO_INEXISTENTE));

        mockMvc.perform(get(URL_INEXISTENTE))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath(JSON_STATUS).value(404));
    }

    @Test
    void extenderDevuelve200() throws Exception {
        when(service.extenderFechaFin(eq(CODIGO), any(SemestreFechaFinRequestDTO.class)))
                .thenReturn(respuesta(CODIGO));

        mockMvc.perform(patch(URL_CODIGO).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"fechaFin":"2027-06-20"}
                                """))
                .andExpect(status().isOk());
        verify(service).extenderFechaFin(eq(CODIGO), eq(new SemestreFechaFinRequestDTO(LocalDate.of(2027, 6, 20))));
    }

    @Test
    void extenderSinFechaDevuelve400() throws Exception {
        mockMvc.perform(patch(URL_CODIGO).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test
    void errorInesperadoLoResuelveElManejadorGlobalCon500() throws Exception {
        when(service.listar()).thenThrow(new IllegalStateException("fallo inesperado"));

        mockMvc.perform(get(URL))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("INTERNAL_SERVER_ERROR"))
                .andExpect(jsonPath(JSON_MENSAJE).value("Ocurrió un error inesperado en el servidor"));
    }

    @Test
    void accesoDenegadoLoResuelveElManejadorGlobalCon403() throws Exception {
        when(service.crear(any())).thenThrow(new AccessDeniedException("Access Denied"));

        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(CUERPO_CREACION_VALIDO))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath(JSON_STATUS).value(403))
                .andExpect(jsonPath("$.error").value("FORBIDDEN"))
                .andExpect(jsonPath(JSON_MENSAJE).value("No tiene permisos suficientes para realizar esta acción"));
    }
}