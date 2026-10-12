package co.edu.uco.sigra.common.exception;

import co.edu.uco.sigra.asignaturas.controller.AsignaturaController;
import co.edu.uco.sigra.asignaturas.exception.AsignaturaNoEncontradaException;
import co.edu.uco.sigra.asignaturas.exception.CodigoAsignaturaDuplicadoException;
import co.edu.uco.sigra.asignaturas.exception.RangoRaInvalidoException;
import co.edu.uco.sigra.asignaturas.service.AsignaturaService;
import co.edu.uco.sigra.semestres.controller.SemestreController;
import co.edu.uco.sigra.semestres.exception.SemestreDuplicadoException;
import co.edu.uco.sigra.semestres.exception.SemestreNoEncontradoException;
import co.edu.uco.sigra.semestres.exception.SemestreReglaNegocioException;
import co.edu.uco.sigra.semestres.service.SemestreService;
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

import tools.jackson.databind.JsonNode;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * Comprueba que los advices de módulo conservan su precedencia después de agregar
 * {@link ManejadorErroresTransversal} con {@code @Order(0)}.
 *
 * <p>Es la prueba que respalda la decisión D1 del diseño. {@code AsignaturaExceptionHandler} y
 * {@code SemestreExceptionHandler} están en {@code HIGHEST_PRECEDENCE} y acotados a su propio
 * controlador con {@code assignableTypes}; el advice transversal está en {@code 0}, es decir por
 * debajo. Si esa relación se invirtiera, las excepciones de dominio de estos dos módulos cambiarían
 * de código HTTP o de mensaje, y sus dueños —Juan Camilo y Santiago— se encontrarían con que su
 * módulo responde distinto sin haber tocado nada.
 *
 * <p>El caso que más importa es {@code DataIntegrityViolationException}: es el único tipo que
 * declaran a la vez el advice de módulo y el transversal. Ahí se ve la precedencia en el mensaje —
 * si gana el módulo, habla de «una asignatura existente» o «un semestre existente»; si ganara el
 * transversal, saldría su texto genérico. Por eso cada prueba de integridad afirma el mensaje y no
 * solo el 409.
 *
 * <p>Los controladores de los compañeros se <strong>importan</strong>, nunca se editan: son el
 * sujeto de prueba.
 */
@DisplayName("Precedencia de los advices de módulo frente al advice transversal")
class PrecedenciaAdvicesTest {

    @Nested
    @WebMvcTest(controllers = AsignaturaController.class)
    @AutoConfigureMockMvc(addFilters = false)
    @DisplayName("asignaturas conserva su manejo propio (advice en HIGHEST_PRECEDENCE)")
    class PrecedenciaDeAsignaturas {

        private static final String RUTA = "/api/v1/asignaturas";

        private static final String CUERPO_VALIDO = """
                {
                  "codigo": "MAT-101",
                  "nombre": "Cálculo diferencial",
                  "programaId": "7c2e8a1b-4d5f-4a60-8e91-1b2c3d4e5f60"
                }
                """;

        @Autowired
        private MockMvc mockMvc;

        @MockitoBean
        private AsignaturaService asignaturaService;

        @Test
        @DisplayName("AsignaturaNoEncontradaException sigue dando 404")
        void asignaturaNoEncontradaSigueDando404() throws Exception {
            UUID id = UUID.fromString("1a2b3c4d-5e6f-4a7b-8c9d-0e1f2a3b4c5d");
            given(asignaturaService.obtenerAsignatura(id))
                    .willThrow(new AsignaturaNoEncontradaException(id));

            MvcResult resultado = mockMvc.perform(get(RUTA + "/" + id)).andReturn();

            AsercionesContratoError.verificarContratoSinDetalles(resultado, HttpStatus.NOT_FOUND);
        }

        @Test
        @DisplayName("CodigoAsignaturaDuplicadoException sigue dando 409")
        void codigoDuplicadoSigueDando409() throws Exception {
            given(asignaturaService.registrarAsignatura(any()))
                    .willThrow(new CodigoAsignaturaDuplicadoException("MAT-101"));

            MvcResult resultado = mockMvc.perform(post(RUTA)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(CUERPO_VALIDO))
                    .andReturn();

            JsonNode cuerpo = AsercionesContratoError.verificarContratoSinDetalles(
                    resultado, HttpStatus.CONFLICT);

            assertThat(cuerpo.get("mensaje").stringValue())
                    .as("El mensaje debe seguir siendo el del módulo, con el código duplicado")
                    .contains("MAT-101");
        }

        @Test
        @DisplayName("RangoRaInvalidoException sigue dando 400 por regla de negocio del módulo")
        void rangoRaInvalidoSigueDando400() throws Exception {
            UUID id = UUID.fromString("2b3c4d5e-6f7a-4b8c-9d0e-1f2a3b4c5d6e");
            given(asignaturaService.activar(id))
                    .willThrow(RangoRaInvalidoException.pocosRa(3, 1));

            MvcResult resultado = mockMvc.perform(patch(RUTA + "/" + id + "/activar")).andReturn();

            AsercionesContratoError.verificarContratoSinDetalles(resultado, HttpStatus.BAD_REQUEST);
        }

        @Test
        @DisplayName("El 409 de integridad lo sigue dando el advice del módulo, no el transversal")
        void integridadLaSigueManejandoElAdviceDelModulo() throws Exception {
            given(asignaturaService.registrarAsignatura(any()))
                    .willThrow(new DataIntegrityViolationException(
                            "duplicate key value violates unique constraint \"uk_asignatura_codigo\""));

            MvcResult resultado = mockMvc.perform(post(RUTA)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(CUERPO_VALIDO))
                    .andReturn();

            JsonNode cuerpo = AsercionesContratoError.verificarContratoSinDetalles(
                    resultado, HttpStatus.CONFLICT);

            assertThat(cuerpo.get("mensaje").stringValue())
                    .as("Con HIGHEST_PRECEDENCE el advice de asignaturas gana al transversal, que "
                            + "está en 0: debe salir su mensaje y no el genérico del transversal")
                    .contains("asignatura");
        }
    }

    @Nested
    @WebMvcTest(controllers = SemestreController.class)
    @AutoConfigureMockMvc(addFilters = false)
    @DisplayName("semestres conserva su manejo propio (advice en HIGHEST_PRECEDENCE)")
    class PrecedenciaDeSemestres {

        private static final String RUTA = "/api/v1/semestres";

        private static final String CUERPO_VALIDO = """
                {
                  "codigo": "2026-1",
                  "fechaInicio": "2026-01-19",
                  "fechaFin": "2026-05-23"
                }
                """;

        @Autowired
        private MockMvc mockMvc;

        @MockitoBean
        private SemestreService semestreService;

        @Test
        @DisplayName("SemestreNoEncontradoException sigue dando 404")
        void semestreNoEncontradoSigueDando404() throws Exception {
            given(semestreService.consultarPorCodigo("2026-1"))
                    .willThrow(new SemestreNoEncontradoException("2026-1"));

            MvcResult resultado = mockMvc.perform(get(RUTA + "/2026-1")).andReturn();

            AsercionesContratoError.verificarContratoSinDetalles(resultado, HttpStatus.NOT_FOUND);
        }

        @Test
        @DisplayName("SemestreDuplicadoException sigue dando 409")
        void semestreDuplicadoSigueDando409() throws Exception {
            given(semestreService.crear(any()))
                    .willThrow(new SemestreDuplicadoException("2026-1"));

            MvcResult resultado = mockMvc.perform(post(RUTA)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(CUERPO_VALIDO))
                    .andReturn();

            JsonNode cuerpo = AsercionesContratoError.verificarContratoSinDetalles(
                    resultado, HttpStatus.CONFLICT);

            assertThat(cuerpo.get("mensaje").stringValue())
                    .as("El mensaje debe seguir siendo el del módulo, con el código duplicado")
                    .contains("2026-1");
        }

        @Test
        @DisplayName("SemestreReglaNegocioException sigue dando 400")
        void reglaDeNegocioSigueDando400() throws Exception {
            given(semestreService.extenderFechaFin(any(), any()))
                    .willThrow(new SemestreReglaNegocioException(
                            "La nueva fecha de fin debe ser posterior a la actual"));

            MvcResult resultado = mockMvc.perform(patch(RUTA + "/2026-1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"fechaFin\": \"2026-06-30\"}"))
                    .andReturn();

            AsercionesContratoError.verificarContratoSinDetalles(resultado, HttpStatus.BAD_REQUEST);
        }

        @Test
        @DisplayName("El 409 de integridad lo sigue dando el advice del módulo, no el transversal")
        void integridadLaSigueManejandoElAdviceDelModulo() throws Exception {
            given(semestreService.crear(any()))
                    .willThrow(new DataIntegrityViolationException(
                            "duplicate key value violates unique constraint \"uk_semestre_codigo\""));

            MvcResult resultado = mockMvc.perform(post(RUTA)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(CUERPO_VALIDO))
                    .andReturn();

            JsonNode cuerpo = AsercionesContratoError.verificarContratoSinDetalles(
                    resultado, HttpStatus.CONFLICT);

            assertThat(cuerpo.get("mensaje").stringValue())
                    .as("Con HIGHEST_PRECEDENCE el advice de semestres gana al transversal, que está "
                            + "en 0: debe salir su mensaje y no el genérico del transversal")
                    .contains("semestre");
        }
    }
}
