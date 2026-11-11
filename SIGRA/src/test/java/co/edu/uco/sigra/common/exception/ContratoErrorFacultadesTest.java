package co.edu.uco.sigra.common.exception;

import co.edu.uco.sigra.facultades.controller.FacultadController;
import co.edu.uco.sigra.facultades.exception.FacultadConProgramasActivosException;
import co.edu.uco.sigra.facultades.exception.FacultadInactivaException;
import co.edu.uco.sigra.facultades.exception.FacultadNoEncontradaException;
import co.edu.uco.sigra.facultades.exception.NombreFacultadDuplicadoException;
import co.edu.uco.sigra.facultades.service.FacultadService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.bind.annotation.ExceptionHandler;

import tools.jackson.databind.JsonNode;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

/**
 * Verifica el contrato de error de {@code common.exception} sobre {@link FacultadController}, igual
 * que {@code ContratoErrorProfesoresTest} lo hace para profesores:
 *
 * <ul>
 *   <li><strong>Peticiones mal formadas</strong> — 415, 406 y 409 de integridad con el cuerpo estándar
 *       y sin filtrar nombres de restricciones (por ejemplo {@code uk_facultad_nombre}, que la base
 *       lanza si dos registros con el mismo nombre llegan a la vez).</li>
 *   <li><strong>No interferencia</strong> — ningún manejador del advice transversal puede atender una
 *       excepción de dominio de facultades.</li>
 *   <li><strong>Precedencia</strong> — las excepciones de dominio dan 404 y 409 con el mensaje del
 *       módulo porque {@code FacultadExceptionHandler} declara {@code @Order(HIGHEST_PRECEDENCE)}.</li>
 * </ul>
 *
 * <p>{@code @WebMvcTest} carga todos los {@code @ControllerAdvice} del proyecto, que es lo que hace
 * falta para verificar precedencias reales; {@code addFilters = false} desactiva la seguridad.
 */
@WebMvcTest(controllers = FacultadController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("Contrato de error de la API sobre FacultadController")
class ContratoErrorFacultadesTest {

    private static final String RUTA = "/api/v1/facultades";

    /** Cuerpo que pasa todas las validaciones del DTO, para que la petición llegue al servicio. */
    private static final String CUERPO_VALIDO = """
            {
              "nombre": "Facultad de Ingeniería"
            }
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private FacultadService facultadService;

    @Nested
    @DisplayName("Errores de petición mal formada que mapea el advice transversal")
    class ErroresDePeticionMalFormada {

        @Test
        @DisplayName("Content-Type no soportado responde 415 con el cuerpo de error estándar")
        void tipoDeContenidoNoSoportadoResponde415() throws Exception {
            MvcResult resultado = mockMvc.perform(post(RUTA)
                            .contentType(MediaType.TEXT_PLAIN)
                            .content("esto no es json"))
                    .andReturn();

            JsonNode cuerpo = AsercionesContratoError.verificarContratoConDetalles(
                    resultado, HttpStatus.UNSUPPORTED_MEDIA_TYPE);
            AsercionesContratoError.verificarOrdenDeClaves(resultado);

            assertThat(cuerpo.get("detalles").toString()).contains("application/json");
        }

        @Test
        @DisplayName("Accept no satisfacible responde 406 con cuerpo JSON")
        void representacionNoAceptableResponde406() throws Exception {
            MvcResult resultado = mockMvc.perform(get(RUTA)
                            .accept(MediaType.APPLICATION_XML))
                    .andReturn();

            JsonNode cuerpo = AsercionesContratoError.verificarContratoConDetalles(
                    resultado, HttpStatus.NOT_ACCEPTABLE);
            AsercionesContratoError.verificarOrdenDeClaves(resultado);

            assertThat(cuerpo.get("detalles").toString()).contains("application/json");
            assertThat(resultado.getResponse().getContentType()).contains(MediaType.APPLICATION_JSON_VALUE);
        }

        @Test
        @DisplayName("Violación de integridad (nombre repetido en carrera) responde 409 sin nombrar la restricción")
        void violacionDeIntegridadResponde409SinNombreDeRestriccion() throws Exception {
            given(facultadService.registrar(any()))
                    .willThrow(new DataIntegrityViolationException(
                            "could not execute statement [ERROR: duplicate key value violates unique "
                                    + "constraint \"uk_facultad_nombre\"; SQLSTATE 23505]"));

            MvcResult resultado = mockMvc.perform(post(RUTA)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(CUERPO_VALIDO))
                    .andReturn();

            JsonNode cuerpo = AsercionesContratoError.verificarContratoSinDetalles(
                    resultado, HttpStatus.CONFLICT);
            AsercionesContratoError.verificarOrdenDeClaves(resultado);

            assertThat(cuerpo.get("mensaje").stringValue()).doesNotContain("uk_facultad_nombre");
        }

        @Test
        @DisplayName("Un 400 de validación respeta el orden de claves del contrato")
        void validacionRespetaOrdenDeClaves() throws Exception {
            MvcResult resultado = mockMvc.perform(post(RUTA)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"nombre\": \"\"}"))
                    .andReturn();

            AsercionesContratoError.verificarContratoConDetalles(resultado, HttpStatus.BAD_REQUEST);
            AsercionesContratoError.verificarOrdenDeClaves(resultado);
        }
    }

    @Nested
    @DisplayName("No interferencia: el advice transversal no atiende lo que es de facultades")
    class NoInterferenciaConElModuloDeFacultades {

        private Set<Class<?>> tiposDeclaradosPorElAdviceTransversal() {
            return Arrays.stream(ManejadorErroresTransversal.class.getDeclaredMethods())
                    .map(metodo -> metodo.getAnnotation(ExceptionHandler.class))
                    .filter(anotacion -> anotacion != null)
                    .flatMap(anotacion -> Arrays.stream(anotacion.value()))
                    .collect(Collectors.toUnmodifiableSet());
        }

        @Test
        @DisplayName("Ninguna excepción de dominio de facultades cae en un manejador del transversal")
        void ningunaExcepcionDeDominioEsAlcanzable() {
            List<Class<?>> excepcionesDeFacultades = List.of(
                    FacultadNoEncontradaException.class,
                    NombreFacultadDuplicadoException.class,
                    FacultadConProgramasActivosException.class,
                    FacultadInactivaException.class);

            Set<Class<?>> declarados = tiposDeclaradosPorElAdviceTransversal();

            for (Class<?> deFacultades : excepcionesDeFacultades) {
                assertThat(declarados)
                        .as("Ningún tipo del advice transversal puede ser supertipo de %s",
                                deFacultades.getSimpleName())
                        .noneMatch(declarado -> declarado.isAssignableFrom(deFacultades));
            }
        }
    }

    @Nested
    @DisplayName("Precedencia: las excepciones de dominio de facultades conservan su código HTTP")
    class PrecedenciaDeFacultades {

        @Test
        @DisplayName("FacultadNoEncontradaException da 404 de forma determinista")
        void noEncontradaDa404() throws Exception {
            UUID id = UUID.fromString("b3d4e5f6-7a8b-49c0-91d2-3e4f5a6b7c8d");
            willThrow(new FacultadNoEncontradaException(id)).given(facultadService).inactivar(id);

            MvcResult resultado = mockMvc.perform(patch(RUTA + "/" + id + "/inactivar")).andReturn();

            AsercionesContratoError.verificarContratoSinDetalles(resultado, HttpStatus.NOT_FOUND);
        }

        @Test
        @DisplayName("NombreFacultadDuplicadoException da 409 con el mensaje del módulo, no el genérico de integridad")
        void nombreDuplicadoDa409DelModulo() throws Exception {
            given(facultadService.actualizarNombre(any(), any()))
                    .willThrow(new NombreFacultadDuplicadoException("Facultad de Ingeniería"));

            MvcResult resultado = mockMvc.perform(put(RUTA + "/" + UUID.randomUUID())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(CUERPO_VALIDO))
                    .andReturn();

            JsonNode cuerpo = AsercionesContratoError.verificarContratoSinDetalles(
                    resultado, HttpStatus.CONFLICT);

            assertThat(cuerpo.get("mensaje").stringValue()).contains("Facultad de Ingeniería");
        }

        @Test
        @DisplayName("FacultadConProgramasActivosException da 409 por regla de negocio del módulo")
        void conProgramasActivosDa409() throws Exception {
            UUID id = UUID.randomUUID();
            given(facultadService.inactivar(id))
                    .willThrow(new FacultadConProgramasActivosException("Facultad de Ingeniería"));

            MvcResult resultado = mockMvc.perform(patch(RUTA + "/" + id + "/inactivar")).andReturn();

            JsonNode cuerpo = AsercionesContratoError.verificarContratoSinDetalles(
                    resultado, HttpStatus.CONFLICT);

            assertThat(cuerpo.get("mensaje").stringValue()).contains("programas activos");
        }
    }
}
