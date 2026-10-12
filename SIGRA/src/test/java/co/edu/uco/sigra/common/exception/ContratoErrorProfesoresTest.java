package co.edu.uco.sigra.common.exception;

import co.edu.uco.sigra.profesores.controller.ProfesorController;
import co.edu.uco.sigra.profesores.exception.CampoNoModificableException;
import co.edu.uco.sigra.profesores.exception.CorreoDuplicadoException;
import co.edu.uco.sigra.profesores.exception.ProfesorNoEncontradoException;
import co.edu.uco.sigra.profesores.service.ProfesorService;
import org.junit.jupiter.api.Disabled;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

/**
 * Verifica el contrato de error de {@code common.exception} sobre un controlador real:
 * {@link ProfesorController}.
 *
 * <p>{@code profesores} es el módulo donde el advice transversal aporta de verdad. Su advice propio
 * no declara {@code Exception}, no declara {@code DataIntegrityViolationException} y no declara
 * {@code @Order}, así que antes de este cambio un {@code Content-Type} inválido, un {@code Accept}
 * no satisfacible o un choque de integridad terminaban en el respaldo genérico del advice global y
 * salían como 500.
 *
 * <p>La prueba tiene tres partes que conviene leer juntas:
 *
 * <ul>
 *   <li><strong>Lo que el advice transversal arregla</strong> — 415, 406 y 409, antes 500.</li>
 *   <li><strong>Lo que el advice transversal no puede tocar</strong> — se comprueba, por reflexión
 *       sobre los tipos que declara, que no tiene manejador para ninguna excepción de dominio de
 *       {@code profesores} ni un catch-all. Es la prueba de no interferencia, y se hace así a
 *       propósito: ver el código HTTP de esas excepciones no sirve hoy, porque H-02 las deja en 500
 *       por una razón ajena a este cambio.</li>
 *   <li><strong>Lo que debería pasar y no pasa</strong> — las tres excepciones de dominio deberían
 *       dar 404, 409 y 400. Esas pruebas están escritas y {@code @Disabled} con el número de
 *       hallazgo y el dueño, listas para que las habilite al corregir.</li>
 * </ul>
 *
 * <p>{@code @WebMvcTest} carga <strong>todos</strong> los {@code @ControllerAdvice} del proyecto, que
 * es justo lo que hace falta para verificar precedencias reales. El servicio se sustituye con
 * {@code @MockitoBean}: no hace falta base de datos para comprobar que una excepción se traduce en un
 * código HTTP y un cuerpo. {@code addFilters = false} desactiva la cadena de seguridad, porque
 * {@code SecurityConfig} es un {@code @Configuration} plano que {@code @WebMvcTest} no carga y en su
 * lugar aplicaría la seguridad por defecto, que respondería 401 en todas las pruebas.
 *
 * <p><strong>El controlador de Jean Paul se importa, no se modifica.</strong> Es el sujeto de prueba.
 */
@WebMvcTest(controllers = ProfesorController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("Contrato de error de la API sobre ProfesorController")
class ContratoErrorProfesoresTest {

    private static final String RUTA = "/api/v1/profesores";

    /** Cuerpo que pasa todas las validaciones del DTO, para que la petición llegue al servicio. */
    private static final String CUERPO_VALIDO = """
            {
              "tipoDocumentoId": "6b1f9d7e-5c3a-4f21-9e8b-2d4a7c6e1f05",
              "numeroDocumento": "1098765",
              "nombreCompleto": "Ana Gómez",
              "correoInstitucional": "ana.gomez@uco.net.co"
            }
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProfesorService profesorService;

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
            MvcResult resultado = mockMvc.perform(get(RUTA)
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
            MvcResult resultado = mockMvc.perform(get(RUTA)
                            .accept(MediaType.APPLICATION_XML))
                    .andReturn();

            // Si la negociación de contenido decide el tipo de esta respuesta, sale vacía: el cliente
            // dijo que solo acepta XML. El manejador fija application/json a mano justamente por eso.
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
            given(profesorService.registrarProfesor(any()))
                    .willThrow(new DataIntegrityViolationException(
                            "could not execute statement [ERROR: duplicate key value violates unique "
                                    + "constraint \"uk_profesor_correo\"; SQLSTATE 23505]"));

            MvcResult resultado = mockMvc.perform(post(RUTA)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(CUERPO_VALIDO))
                    .andReturn();

            // verificarContratoSinDetalles ya falla si el cuerpo trae 'constraint', 'uk_',
            // 'duplicate key', 'sqlstate' o un nombre de clase: la causa va solo al log.
            JsonNode cuerpo = AsercionesContratoError.verificarContratoSinDetalles(
                    resultado, HttpStatus.CONFLICT);
            AsercionesContratoError.verificarOrdenDeClaves(resultado);

            assertThat(cuerpo.get("mensaje").stringValue())
                    .as("El mensaje debe describir el conflicto en términos del negocio")
                    .doesNotContain("uk_profesor_correo");
        }
    }

    @Nested
    @DisplayName("No interferencia: el advice transversal no puede atender lo que es de profesores")
    class NoInterferenciaConElModuloDeProfesores {

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
        @DisplayName("No declara un catch-all: con @Order(0) convertiría los 404 y 409 ajenos en 500")
        void noDeclaraCatchAll() {
            assertThat(tiposDeclaradosPorElAdviceTransversal())
                    .as("Exception o Throwable en este advice le ganarían a ProfesorExceptionHandler, "
                            + "que está en LOWEST_PRECEDENCE, y romperían el módulo de un compañero")
                    .doesNotContain(Exception.class, RuntimeException.class, Throwable.class);
        }

        @Test
        @DisplayName("Ninguna excepción de dominio de profesores cae en un manejador del transversal")
        void ningunaExcepcionDeDominioDeProfesoresEsAlcanzable() {
            List<Class<?>> excepcionesDeProfesores = List.of(
                    ProfesorNoEncontradoException.class,
                    CorreoDuplicadoException.class,
                    CampoNoModificableException.class);

            Set<Class<?>> declarados = tiposDeclaradosPorElAdviceTransversal();

            for (Class<?> deProfesores : excepcionesDeProfesores) {
                assertThat(declarados)
                        .as("Ningún tipo declarado por el advice transversal puede ser supertipo de "
                                + "%s, porque entonces lo interceptaría", deProfesores.getSimpleName())
                        .noneMatch(declarado -> declarado.isAssignableFrom(deProfesores));
            }
        }
    }

    /**
     * Comportamiento correcto según el spec, que hoy no se cumple por <strong>H-02</strong>.
     *
     * <p>{@code ProfesorExceptionHandler} no declara {@code @Order}, así que queda en
     * {@code LOWEST_PRECEDENCE} empatado con {@code GlobalExceptionHandler}, que sí declara
     * {@code Exception}. El desempate lo decide el orden de escaneo del classpath y en esta máquina
     * lo gana el global: las tres excepciones de dominio salen como <strong>500</strong> en lugar de
     * 404, 409 y 400.
     *
     * <p>Comprobado que es ajeno a este cambio: las tres responden 500 igual con
     * {@code ManejadorErroresTransversal} retirado del árbol. La corrección es una línea en el advice
     * de {@code profesores} —{@code @Order(Ordered.HIGHEST_PRECEDENCE)}— y le corresponde a su dueño.
     *
     * <p>Para habilitarlas: quitar el {@code @Disabled} de esta clase después de corregir.
     */
    @Nested
    @Disabled("H-02 (media) — ProfesorExceptionHandler, dueño: Jean Paul. Empata en LOWEST_PRECEDENCE "
            + "con GlobalExceptionHandler, que declara Exception, y el desempate lo decide el "
            + "classpath. Corrección: @Order(Ordered.HIGHEST_PRECEDENCE) en el advice de profesores.")
    @DisplayName("H-02: las excepciones de dominio de profesores deben conservar su código HTTP")
    class HallazgoH02PrecedenciaDeProfesores {

        @Test
        @DisplayName("ProfesorNoEncontradoException debe dar 404 de forma determinista")
        void profesorNoEncontradoDebeDar404() throws Exception {
            UUID id = UUID.fromString("b3d4e5f6-7a8b-49c0-91d2-3e4f5a6b7c8d");
            willThrow(new ProfesorNoEncontradoException(id))
                    .given(profesorService).inactivar(id);

            MvcResult resultado = mockMvc.perform(delete(RUTA + "/" + id)).andReturn();

            AsercionesContratoError.verificarContratoSinDetalles(resultado, HttpStatus.NOT_FOUND);
        }

        @Test
        @DisplayName("CorreoDuplicadoException debe dar 409 con el mensaje del módulo")
        void correoDuplicadoDebeDar409DelModulo() throws Exception {
            given(profesorService.registrarProfesor(any()))
                    .willThrow(new CorreoDuplicadoException("ana.gomez@uco.net.co"));

            MvcResult resultado = mockMvc.perform(post(RUTA)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(CUERPO_VALIDO))
                    .andReturn();

            JsonNode cuerpo = AsercionesContratoError.verificarContratoSinDetalles(
                    resultado, HttpStatus.CONFLICT);

            assertThat(cuerpo.get("mensaje").stringValue())
                    .as("El 409 debe ser el del módulo, con su mensaje propio, y no el mensaje "
                            + "genérico de integridad del advice transversal")
                    .contains("ana.gomez@uco.net.co");
        }

        @Test
        @DisplayName("CampoNoModificableException debe dar 400 por regla de negocio del módulo")
        void campoNoModificableDebeDar400() throws Exception {
            UUID id = UUID.fromString("c4e5f6a7-8b9c-4d01-a2e3-4f5a6b7c8d9e");
            given(profesorService.modificarProfesor(any(), any()))
                    .willThrow(new CampoNoModificableException("numeroDocumento"));

            MvcResult resultado = mockMvc.perform(put(RUTA + "/" + id)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(CUERPO_VALIDO))
                    .andReturn();

            AsercionesContratoError.verificarContratoSinDetalles(resultado, HttpStatus.BAD_REQUEST);
        }
    }
}
