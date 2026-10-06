package co.edu.uco.sigra.estudiantes.exception;

import co.edu.uco.sigra.common.exception.AsercionesContratoError;
import co.edu.uco.sigra.estudiantes.controller.EstudianteController;
import co.edu.uco.sigra.estudiantes.mapper.MatriculaMapper;
import co.edu.uco.sigra.estudiantes.service.EstudianteService;
import co.edu.uco.sigra.estudiantes.service.MatriculaService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/**
 * Comprueba que {@link ManejadorErroresEstudiantes} conserva su precedencia frente al advice
 * global (decisión D6 del diseño de {@code gestion-estudiantes}).
 *
 * <p>{@code ManejadorErroresEstudiantes} está en {@code @Order(Ordered.HIGHEST_PRECEDENCE)} y
 * acotado a {@code co.edu.uco.sigra.estudiantes} con {@code basePackages}. Si alguien le quita el
 * {@code @Order} sin darse cuenta — por ejemplo al copiar el advice para otro módulo y borrar la
 * anotación por accidente —, el desempate con {@code GlobalExceptionHandler} (que atrapa
 * {@code Exception} en {@code LOWEST_PRECEDENCE}) deja de ser determinista y las cuatro
 * excepciones de dominio de la ficha del estudiante pueden terminar en el 500 genérico en vez de
 * su código propio.
 *
 * <p>Por eso cada caso aquí usa {@code AsercionesContratoError.verificarContratoSinDetalles}, que
 * exige el código HTTP exacto: si el advice perdiera su prioridad y la petición cayera al 500 del
 * manejador global, la aserción del código HTTP falla aquí mismo con un mensaje claro, en vez de
 * que el defecto se note solo cuando ya está en producción.
 */
@WebMvcTest(controllers = EstudianteController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("Precedencia de ManejadorErroresEstudiantes frente al advice global")
class PrecedenciaManejadorEstudiantesTest {

    private static final String RUTA = "/api/v1/estudiantes";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EstudianteService estudianteService;

    @MockitoBean
    private MatriculaService matriculaService;

    @MockitoBean
    private MatriculaMapper matriculaMapper;

    @Test
    @DisplayName("EstudianteNoEncontradoException sigue dando 404, no el 500 del advice global")
    void estudianteNoEncontradoSigueDando404() throws Exception {
        UUID id = UUID.fromString("8b9c0d1e-2f3a-4b4c-5d6e-7f8091223344");
        given(estudianteService.consultarPorId(id))
                .willThrow(EstudianteNoEncontradoException.porId(id));

        MvcResult resultado = mockMvc.perform(get(RUTA + "/" + id)).andReturn();

        AsercionesContratoError.verificarContratoSinDetalles(resultado, HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("ReferenciaNoEncontradaException sigue dando 404, no el 500 del advice global")
    void referenciaNoEncontradaSigueDando404() throws Exception {
        UUID idTipoDocumento = UUID.fromString("9c0d1e2f-3a4b-4c5d-6e7f-809122334455");
        given(estudianteService.consultarPorId(any()))
                .willThrow(ReferenciaNoEncontradaException.tipoDocumento(idTipoDocumento));

        MvcResult resultado = mockMvc.perform(
                get(RUTA + "/0c1d2e3f-4a5b-4c6d-7e8f-90a1b2c3d4e5")).andReturn();

        AsercionesContratoError.verificarContratoSinDetalles(resultado, HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("EstudianteYaRegistradoException sigue dando 409, no el 500 del advice global")
    void estudianteYaRegistradoSigueDando409() throws Exception {
        given(estudianteService.consultarPorId(any()))
                .willThrow(EstudianteYaRegistradoException.porDocumento("1098765432"));

        MvcResult resultado = mockMvc.perform(
                get(RUTA + "/1d2e3f4a-5b6c-4d7e-8f90-a1b2c3d4e5f6")).andReturn();

        AsercionesContratoError.verificarContratoSinDetalles(resultado, HttpStatus.CONFLICT);
    }

    @Test
    @DisplayName("CorreoEstudianteDuplicadoException sigue dando 409, no el 500 del advice global")
    void correoDuplicadoSigueDando409() throws Exception {
        given(estudianteService.consultarPorId(any()))
                .willThrow(CorreoEstudianteDuplicadoException.porCorreo("ana.gomez@uco.net.co"));

        MvcResult resultado = mockMvc.perform(
                get(RUTA + "/2e3f4a5b-6c7d-4e8f-90a1-b2c3d4e5f6a7")).andReturn();

        AsercionesContratoError.verificarContratoSinDetalles(resultado, HttpStatus.CONFLICT);
    }
}
