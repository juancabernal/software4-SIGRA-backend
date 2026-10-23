package co.edu.uco.sigra.common.exception;

import co.edu.uco.sigra.asignaturas.controller.AsignacionDocenteController;
import co.edu.uco.sigra.asignaturas.exception.AsignacionDocenteNoEncontradaException;
import co.edu.uco.sigra.asignaturas.exception.AsignaturaNoEncontradaException;
import co.edu.uco.sigra.asignaturas.service.AsignacionDocenteService;
import co.edu.uco.sigra.profesores.exception.ProfesorNoEncontradoException;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * Verifica el contrato de error de {@code common.exception} sobre un controlador real:
 * {@link AsignacionDocenteController}. Replica, para RF-07, el mismo cierre de H-02 que
 * {@code ContratoErrorProfesoresTest} ya hace para el módulo de Profesores.
 *
 * <p>{@code AsignacionDocenteExceptionHandler} no declara {@code Exception}, no declara
 * {@code DataIntegrityViolationException} y no declara {@code @Order}, así que antes de este
 * cambio un {@code Content-Type} inválido, un {@code Accept} no satisfacible o un choque de
 * integridad terminaban en el respaldo genérico del advice global y salían como 500.
 *
 * <p>La prueba tiene tres partes que conviene leer juntas:
 *
 * <ul>
 *   <li><strong>Lo que el advice transversal arregla</strong> — 415, 406 y 409, antes 500.</li>
 *   <li><strong>Lo que el advice transversal no puede tocar</strong> — por reflexión sobre los
 *       tipos que declara, no tiene manejador para ninguna excepción de dominio de RF-07 ni un
 *       catch-all.</li>
 *   <li><strong>Lo que debe conservarse</strong> — las tres excepciones de dominio de RF-07 dan 404
 *       de forma determinista porque {@code AsignacionDocenteExceptionHandler} declara
 *       {@code @Order(Ordered.HIGHEST_PRECEDENCE)}.</li>
 * </ul>
 */
@WebMvcTest(controllers = AsignacionDocenteController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("Contrato de error de la API sobre AsignacionDocenteController")
class ContratoErrorAsignacionDocenteTest {

    private static final String RUTA = "/api/v1/asignaciones-docentes";

    /** Cuerpo que pasa todas las validaciones del DTO, para que la petición llegue al servicio. */
    private static final String CUERPO_VALIDO = """
            {
              "profesorIds": ["b3d4e5f6-7a8b-49c0-91d2-3e4f5a6b7c8d"],
              "asignaturaIds": ["c4e5f6a7-8b9c-4d01-a2e3-4f5a6b7c8d9e"]
            }
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AsignacionDocenteService asignacionDocenteService;

    private static co.edu.uco.sigra.asignaturas.dto.ResultadoAsignacionMasivaDTO cuerpoDeEjemplo() {
        return new co.edu.uco.sigra.asignaturas.dto.ResultadoAsignacionMasivaDTO(List.of(), List.of(), List.of());
    }

    @Nested
    @DisplayName("Errores de petición mal formada que el advice transversal mapea (antes 500)")
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

            assertThat(cuerpo.get("detalles").toString())
                    .as("Los detalles deben decirle al cliente con qué tipo reintentar")
                    .contains("application/json");
        }

        @Test
        @DisplayName("Accept no satisfacible responde 406 con el cuerpo de error estándar")
        void representacionNoAceptableResponde406() throws Exception {
            // Sin stub, el mock devuelve null para este tipo (no es una colección) y Spring responde
            // la petición con cuerpo vacío sin negociar el Accept: hace falta un cuerpo real para que
            // la negociación de contenido se ejecute y dispare el 406.
            given(asignacionDocenteService.asignarMasivo(any()))
                    .willReturn(cuerpoDeEjemplo());

            MvcResult resultado = mockMvc.perform(post(RUTA)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(CUERPO_VALIDO)
                            .accept(MediaType.APPLICATION_XML))
                    .andReturn();

            JsonNode cuerpo = AsercionesContratoError.verificarContratoConDetalles(
                    resultado, HttpStatus.NOT_ACCEPTABLE);
            AsercionesContratoError.verificarOrdenDeClaves(resultado);

            assertThat(cuerpo.get("detalles").toString())
                    .as("Un 406 que no dice qué representaciones hay no le sirve al cliente")
                    .contains("application/json");
        }

        @Test
        @DisplayName("El 406 entrega cuerpo aunque el cliente no acepte JSON, o el contrato se rompe")
        void el406NoPuedeSalirSinCuerpo() throws Exception {
            given(asignacionDocenteService.asignarMasivo(any()))
                    .willReturn(cuerpoDeEjemplo());

            MvcResult resultado = mockMvc.perform(post(RUTA)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(CUERPO_VALIDO)
                            .accept(MediaType.APPLICATION_XML))
                    .andReturn();

            assertThat(resultado.getResponse().getContentAsString())
                    .as("Toda respuesta de error entrega un cuerpo, incluido el 406")
                    .isNotBlank();
            assertThat(resultado.getResponse().getContentType())
                    .as("El manejador de 406 debe declarar el tipo de la respuesta a mano")
                    .contains(MediaType.APPLICATION_JSON_VALUE);
        }

        @Test
        @DisplayName("Violación de integridad de datos responde 409 sin nombrar la restricción")
        void violacionDeIntegridadResponde409SinNombreDeRestriccion() throws Exception {
            // El mensaje que trae la excepción real de la base de datos: nada de esto puede salir.
            given(asignacionDocenteService.asignarMasivo(any()))
                    .willThrow(new DataIntegrityViolationException(
                            "could not execute statement [ERROR: duplicate key value violates unique "
                                    + "constraint \"uk_asignacion_docente_profesor_asignatura\"; "
                                    + "SQLSTATE 23505]"));

            MvcResult resultado = mockMvc.perform(post(RUTA)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(CUERPO_VALIDO))
                    .andReturn();

            JsonNode cuerpo = AsercionesContratoError.verificarContratoSinDetalles(
                    resultado, HttpStatus.CONFLICT);
            AsercionesContratoError.verificarOrdenDeClaves(resultado);

            assertThat(cuerpo.get("mensaje").stringValue())
                    .as("El mensaje debe describir el conflicto en términos del negocio")
                    .doesNotContain("uk_asignacion_docente_profesor_asignatura");
        }
    }

    @Nested
    @DisplayName("No interferencia: el advice transversal no puede atender lo que es de RF-07")
    class NoInterferenciaConRf07 {

        /** Los seis tipos de la tabla D2 del diseño, y solo esos. */
        private static final Set<Class<?>> TIPOS_ESPERADOS = Set.of(
                org.springframework.web.HttpMediaTypeNotSupportedException.class,
                org.springframework.web.HttpMediaTypeNotAcceptableException.class,
                org.springframework.web.bind.MissingServletRequestParameterException.class,
                org.springframework.web.multipart.support.MissingServletRequestPartException.class,
                jakarta.validation.ConstraintViolationException.class,
                DataIntegrityViolationException.class);

        private Set<Class<?>> tiposDeclaradosPorElAdviceTransversal() {
            return Arrays.stream(ManejadorErroresTransversal.class.getDeclaredMethods())
                    .map(metodo -> metodo.getAnnotation(ExceptionHandler.class))
                    .filter(anotacion -> anotacion != null)
                    .flatMap(anotacion -> Arrays.stream(anotacion.value()))
                    .collect(Collectors.toUnmodifiableSet());
        }

        @Test
        @DisplayName("Declara exactamente los seis tipos del diseño, ni uno más")
        void declaraSoloLosSeisTiposDelDiseno() {
            assertThat(tiposDeclaradosPorElAdviceTransversal())
                    .as("Agregar un tipo que ya declare un advice de módulo le robaría el manejo; "
                            + "quitar uno reabre el 500 que este cambio cerró")
                    .containsExactlyInAnyOrderElementsOf(TIPOS_ESPERADOS);
        }

        @Test
        @DisplayName("No declara un catch-all: con @Order(0) convertiría los 404 ajenos en 500")
        void noDeclaraCatchAll() {
            assertThat(tiposDeclaradosPorElAdviceTransversal())
                    .as("Exception o Throwable en este advice le ganarían a "
                            + "AsignacionDocenteExceptionHandler, que está en HIGHEST_PRECEDENCE, y "
                            + "romperían el módulo de un compañero")
                    .doesNotContain(Exception.class, RuntimeException.class, Throwable.class);
        }

        @Test
        @DisplayName("Ninguna excepción de dominio de RF-07 cae en un manejador del transversal")
        void ningunaExcepcionDeDominioDeRf07EsAlcanzable() {
            List<Class<?>> excepcionesDeRf07 = List.of(
                    ProfesorNoEncontradoException.class,
                    AsignaturaNoEncontradaException.class,
                    AsignacionDocenteNoEncontradaException.class);

            Set<Class<?>> declarados = tiposDeclaradosPorElAdviceTransversal();

            for (Class<?> deRf07 : excepcionesDeRf07) {
                assertThat(declarados)
                        .as("Ningún tipo declarado por el advice transversal puede ser supertipo de "
                                + "%s, porque entonces lo interceptaría", deRf07.getSimpleName())
                        .noneMatch(declarado -> declarado.isAssignableFrom(deRf07));
            }
        }
    }

    @Nested
    @DisplayName("H-02 cerrado: las excepciones de dominio de RF-07 conservan su código HTTP")
    class HallazgoH02PrecedenciaDeRf07 {

        @Test
        @DisplayName("ProfesorNoEncontradoException en la asignación debe dar 404 de forma determinista")
        void profesorNoEncontradoDebeDar404() throws Exception {
            UUID id = UUID.fromString("b3d4e5f6-7a8b-49c0-91d2-3e4f5a6b7c8d");
            given(asignacionDocenteService.asignarMasivo(any()))
                    .willThrow(new ProfesorNoEncontradoException(id));

            MvcResult resultado = mockMvc.perform(post(RUTA)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(CUERPO_VALIDO))
                    .andReturn();

            AsercionesContratoError.verificarContratoSinDetalles(resultado, HttpStatus.NOT_FOUND);
        }

        @Test
        @DisplayName("AsignaturaNoEncontradaException debe dar 404 de forma determinista")
        void asignaturaNoEncontradaDebeDar404() throws Exception {
            UUID id = UUID.fromString("c4e5f6a7-8b9c-4d01-a2e3-4f5a6b7c8d9e");
            given(asignacionDocenteService.asignarMasivo(any()))
                    .willThrow(new AsignaturaNoEncontradaException(id));

            MvcResult resultado = mockMvc.perform(post(RUTA)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(CUERPO_VALIDO))
                    .andReturn();

            AsercionesContratoError.verificarContratoSinDetalles(resultado, HttpStatus.NOT_FOUND);
        }

        @Test
        @DisplayName("AsignacionDocenteNoEncontradaException al desasignar debe dar 404 de forma determinista")
        void asignacionNoEncontradaDebeDar404() throws Exception {
            UUID id = UUID.fromString("d5e6f7a8-9b0c-4d12-93e4-5f6a7b8c9d0e");
            willThrow(new AsignacionDocenteNoEncontradaException(id))
                    .given(asignacionDocenteService).desasignar(id);

            MvcResult resultado = mockMvc.perform(patch(RUTA + "/" + id + "/desasignar")).andReturn();

            AsercionesContratoError.verificarContratoSinDetalles(resultado, HttpStatus.NOT_FOUND);
        }
    }
}
