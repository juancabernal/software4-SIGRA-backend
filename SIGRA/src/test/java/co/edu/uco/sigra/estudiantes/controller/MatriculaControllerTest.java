package co.edu.uco.sigra.estudiantes.controller;

import co.edu.uco.sigra.asignaturas.entity.Asignatura;
import co.edu.uco.sigra.common.enums.EstadoRegistro;
import co.edu.uco.sigra.common.exception.AsercionesContratoError;
import co.edu.uco.sigra.estudiantes.entity.Estudiante;
import co.edu.uco.sigra.estudiantes.entity.Matricula;
import co.edu.uco.sigra.estudiantes.exception.EstudianteNoEncontradoException;
import co.edu.uco.sigra.estudiantes.exception.MatriculaDuplicadaException;
import co.edu.uco.sigra.estudiantes.exception.MatriculaNoEncontradaException;
import co.edu.uco.sigra.estudiantes.exception.MatriculaYaInactivaException;
import co.edu.uco.sigra.estudiantes.exception.ReferenciaNoEncontradaException;
import co.edu.uco.sigra.estudiantes.exception.ReglaDeMatriculaException;
import co.edu.uco.sigra.estudiantes.mapper.MatriculaMapperImpl;
import co.edu.uco.sigra.estudiantes.service.MatriculaService;
import co.edu.uco.sigra.estudiantes.service.MatriculaService.ResultadoMatricula;
import co.edu.uco.sigra.semestres.entity.Semestre;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import tools.jackson.databind.JsonNode;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Prueba de capa web con contexto de Spring (RF-08, RF-09): verifica que
 * {@link MatriculaController} deserializa, valida con Jakarta Validation, elige 201 frente a 200
 * según {@code reactivada} y traduce las excepciones de dominio al contrato de error estándar, vía
 * {@link co.edu.uco.sigra.estudiantes.exception.ManejadorErroresEstudiantes}.
 *
 * <p>{@code @WebMvcTest} con {@link MatriculaService} mockeado y {@link MatriculaMapperImpl} real
 * (vía {@code @Import}) es el nivel mínimo suficiente: no hace falta base de datos, pero sí el
 * contexto de Spring para ejercer {@code @Valid} y el advice real, y el mapper real para no simular
 * con un mock la traducción de entidad a DTO que es, justamente, lo que esta prueba cubre.
 */
@WebMvcTest(controllers = MatriculaController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(MatriculaMapperImpl.class)
@DisplayName("MatriculaController: deserialización, validación y códigos HTTP (RF-08, RF-09)")
class MatriculaControllerTest {

    private static final String RUTA = "/api/v1/matriculas";

    private static final UUID ID_ESTUDIANTE = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID ID_ASIGNATURA = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID ID_SEMESTRE = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID ID_MATRICULA = UUID.fromString("44444444-4444-4444-4444-444444444444");

    private static final String CUERPO_VALIDO = """
            {
              "estudianteId": "11111111-1111-1111-1111-111111111111",
              "asignaturaId": "22222222-2222-2222-2222-222222222222",
              "semestreId": "33333333-3333-3333-3333-333333333333"
            }
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MatriculaService matriculaService;

    private static Matricula matriculaCompleta(UUID id, UUID estudianteId, UUID asignaturaId, UUID semestreId,
                                                 EstadoRegistro estado) {
        Estudiante estudiante = new Estudiante();
        ReflectionTestUtils.setField(estudiante, "id", estudianteId);
        estudiante.setNumeroDocumento("1098765432");
        estudiante.setNombreCompleto("Ana Gómez");
        estudiante.setEstado(EstadoRegistro.ACTIVO);

        Asignatura asignatura = new Asignatura("ASIG-001", "Cálculo I", null);
        ReflectionTestUtils.setField(asignatura, "id", asignaturaId);

        Semestre semestre = new Semestre();
        semestre.setId(semestreId);
        semestre.setCodigo("2026-1");

        Matricula matricula = new Matricula();
        matricula.setId(id);
        matricula.setEstudiante(estudiante);
        matricula.setAsignatura(asignatura);
        matricula.setSemestre(semestre);
        matricula.setEstado(estado);
        return matricula;
    }

    private static Matricula matriculaDeListado(UUID id, String numeroDocumento, String nombreCompleto,
                                                  EstadoRegistro estado) {
        Estudiante estudiante = new Estudiante();
        estudiante.setNumeroDocumento(numeroDocumento);
        estudiante.setNombreCompleto(nombreCompleto);

        Matricula matricula = new Matricula();
        matricula.setId(id);
        matricula.setEstudiante(estudiante);
        matricula.setEstado(estado);
        return matricula;
    }

    @Nested
    @DisplayName("POST /api/v1/matriculas")
    class Matricular {

        @Test
        @DisplayName("Cuerpo válido y matrícula nueva devuelve 201 con la matrícula creada")
        void cuerpoValidoMatriculaNuevaDevuelve201() throws Exception {
            Matricula matricula = matriculaCompleta(ID_MATRICULA, ID_ESTUDIANTE, ID_ASIGNATURA, ID_SEMESTRE,
                    EstadoRegistro.ACTIVO);
            given(matriculaService.matricular(ID_ESTUDIANTE, ID_ASIGNATURA, ID_SEMESTRE))
                    .willReturn(new ResultadoMatricula(matricula, false));

            mockMvc.perform(post(RUTA)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(CUERPO_VALIDO))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(ID_MATRICULA.toString()))
                    .andExpect(jsonPath("$.estudianteId").value(ID_ESTUDIANTE.toString()))
                    .andExpect(jsonPath("$.asignaturaId").value(ID_ASIGNATURA.toString()))
                    .andExpect(jsonPath("$.semestreId").value(ID_SEMESTRE.toString()))
                    .andExpect(jsonPath("$.estado").value("ACTIVO"));
        }

        @Test
        @DisplayName("Cuerpo válido y matrícula reactivada devuelve 200, no 201")
        void cuerpoValidoMatriculaReactivadaDevuelve200() throws Exception {
            Matricula matricula = matriculaCompleta(ID_MATRICULA, ID_ESTUDIANTE, ID_ASIGNATURA, ID_SEMESTRE,
                    EstadoRegistro.ACTIVO);
            given(matriculaService.matricular(ID_ESTUDIANTE, ID_ASIGNATURA, ID_SEMESTRE))
                    .willReturn(new ResultadoMatricula(matricula, true));

            mockMvc.perform(post(RUTA)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(CUERPO_VALIDO))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(ID_MATRICULA.toString()))
                    .andExpect(jsonPath("$.estado").value("ACTIVO"));
        }

        @Test
        @DisplayName("Campo faltante (semestreId) devuelve 400 con el campo señalado en detalles")
        void campoFaltanteDevuelve400() throws Exception {
            String cuerpoInvalido = """
                    {
                      "estudianteId": "11111111-1111-1111-1111-111111111111",
                      "asignaturaId": "22222222-2222-2222-2222-222222222222"
                    }
                    """;

            MvcResult resultado = mockMvc.perform(post(RUTA)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(cuerpoInvalido))
                    .andReturn();

            JsonNode cuerpo = AsercionesContratoError.verificarContratoConDetalles(resultado, HttpStatus.BAD_REQUEST);

            assertThat(cuerpo.get("detalles").toString())
                    .as("El detalle debe nombrar el campo faltante")
                    .contains("semestreId");
        }

        @Test
        @DisplayName("Terna ya matriculada y activa devuelve 409 sin detalles")
        void ternaYaMatriculadaDevuelve409() throws Exception {
            given(matriculaService.matricular(ID_ESTUDIANTE, ID_ASIGNATURA, ID_SEMESTRE))
                    .willThrow(MatriculaDuplicadaException.porTerna(ID_ESTUDIANTE, ID_ASIGNATURA, ID_SEMESTRE));

            MvcResult resultado = mockMvc.perform(post(RUTA)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(CUERPO_VALIDO))
                    .andReturn();

            AsercionesContratoError.verificarContratoSinDetalles(resultado, HttpStatus.CONFLICT);
        }

        @Test
        @DisplayName("Estudiante inactivo devuelve 400 sin detalles")
        void estudianteInactivoDevuelve400() throws Exception {
            given(matriculaService.matricular(ID_ESTUDIANTE, ID_ASIGNATURA, ID_SEMESTRE))
                    .willThrow(ReglaDeMatriculaException.estudianteInactivo(ID_ESTUDIANTE));

            MvcResult resultado = mockMvc.perform(post(RUTA)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(CUERPO_VALIDO))
                    .andReturn();

            AsercionesContratoError.verificarContratoSinDetalles(resultado, HttpStatus.BAD_REQUEST);
        }

        @Test
        @DisplayName("Asignatura no activa devuelve 400 sin detalles")
        void asignaturaNoActivaDevuelve400() throws Exception {
            given(matriculaService.matricular(ID_ESTUDIANTE, ID_ASIGNATURA, ID_SEMESTRE))
                    .willThrow(ReglaDeMatriculaException.asignaturaNoActiva(ID_ASIGNATURA));

            MvcResult resultado = mockMvc.perform(post(RUTA)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(CUERPO_VALIDO))
                    .andReturn();

            AsercionesContratoError.verificarContratoSinDetalles(resultado, HttpStatus.BAD_REQUEST);
        }

        @Test
        @DisplayName("Semestre no activo devuelve 400 sin detalles")
        void semestreNoActivoDevuelve400() throws Exception {
            given(matriculaService.matricular(ID_ESTUDIANTE, ID_ASIGNATURA, ID_SEMESTRE))
                    .willThrow(ReglaDeMatriculaException.semestreNoActivo(ID_SEMESTRE));

            MvcResult resultado = mockMvc.perform(post(RUTA)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(CUERPO_VALIDO))
                    .andReturn();

            AsercionesContratoError.verificarContratoSinDetalles(resultado, HttpStatus.BAD_REQUEST);
        }

        @Test
        @DisplayName("Estudiante inexistente devuelve 404 sin detalles")
        void estudianteInexistenteDevuelve404() throws Exception {
            given(matriculaService.matricular(ID_ESTUDIANTE, ID_ASIGNATURA, ID_SEMESTRE))
                    .willThrow(EstudianteNoEncontradoException.porId(ID_ESTUDIANTE));

            MvcResult resultado = mockMvc.perform(post(RUTA)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(CUERPO_VALIDO))
                    .andReturn();

            AsercionesContratoError.verificarContratoSinDetalles(resultado, HttpStatus.NOT_FOUND);
        }

        @Test
        @DisplayName("Asignatura inexistente devuelve 404 sin detalles")
        void asignaturaInexistenteDevuelve404() throws Exception {
            given(matriculaService.matricular(ID_ESTUDIANTE, ID_ASIGNATURA, ID_SEMESTRE))
                    .willThrow(ReferenciaNoEncontradaException.asignatura(ID_ASIGNATURA));

            MvcResult resultado = mockMvc.perform(post(RUTA)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(CUERPO_VALIDO))
                    .andReturn();

            AsercionesContratoError.verificarContratoSinDetalles(resultado, HttpStatus.NOT_FOUND);
        }

        @Test
        @DisplayName("Semestre inexistente devuelve 404 sin detalles")
        void semestreInexistenteDevuelve404() throws Exception {
            given(matriculaService.matricular(ID_ESTUDIANTE, ID_ASIGNATURA, ID_SEMESTRE))
                    .willThrow(ReferenciaNoEncontradaException.semestre(ID_SEMESTRE));

            MvcResult resultado = mockMvc.perform(post(RUTA)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(CUERPO_VALIDO))
                    .andReturn();

            AsercionesContratoError.verificarContratoSinDetalles(resultado, HttpStatus.NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/matriculas/{id}")
    class Desvincular {

        @Test
        @DisplayName("Caso feliz devuelve 204 sin cuerpo")
        void casoFelizDevuelve204() throws Exception {
            mockMvc.perform(delete(RUTA + "/" + ID_MATRICULA))
                    .andExpect(status().isNoContent())
                    .andExpect(content().string(""));

            verify(matriculaService).desvincular(ID_MATRICULA);
        }

        @Test
        @DisplayName("Matrícula ya inactiva devuelve 409 sin detalles")
        void matriculaYaInactivaDevuelve409() throws Exception {
            willThrow(MatriculaYaInactivaException.porId(ID_MATRICULA))
                    .given(matriculaService).desvincular(ID_MATRICULA);

            MvcResult resultado = mockMvc.perform(delete(RUTA + "/" + ID_MATRICULA)).andReturn();

            AsercionesContratoError.verificarContratoSinDetalles(resultado, HttpStatus.CONFLICT);
        }

        @Test
        @DisplayName("Matrícula inexistente devuelve 404 sin detalles")
        void matriculaInexistenteDevuelve404() throws Exception {
            willThrow(MatriculaNoEncontradaException.porId(ID_MATRICULA))
                    .given(matriculaService).desvincular(ID_MATRICULA);

            MvcResult resultado = mockMvc.perform(delete(RUTA + "/" + ID_MATRICULA)).andReturn();

            AsercionesContratoError.verificarContratoSinDetalles(resultado, HttpStatus.NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("GET /api/v1/matriculas?asignaturaId=&semestreId=&incluirInactivas=")
    class ListarMatriculados {

        @Test
        @DisplayName("Sin incluirInactivas devuelve 200 y llama al servicio con incluirInactivas=false")
        void sinIncluirInactivasDevuelve200() throws Exception {
            Matricula matricula = matriculaDeListado(ID_MATRICULA, "1098765432", "Ana Gómez", EstadoRegistro.ACTIVO);
            given(matriculaService.listarMatriculados(ID_ASIGNATURA, ID_SEMESTRE, false))
                    .willReturn(List.of(matricula));

            mockMvc.perform(get(RUTA)
                            .param("asignaturaId", ID_ASIGNATURA.toString())
                            .param("semestreId", ID_SEMESTRE.toString()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].matriculaId").value(ID_MATRICULA.toString()))
                    .andExpect(jsonPath("$[0].numeroDocumento").value("1098765432"))
                    .andExpect(jsonPath("$[0].nombreCompleto").value("Ana Gómez"))
                    .andExpect(jsonPath("$[0].estado").value("ACTIVO"));

            verify(matriculaService).listarMatriculados(ID_ASIGNATURA, ID_SEMESTRE, false);
        }

        @Test
        @DisplayName("Con incluirInactivas=true llama al servicio con ese valor")
        void conIncluirInactivasDevuelve200() throws Exception {
            given(matriculaService.listarMatriculados(ID_ASIGNATURA, ID_SEMESTRE, true))
                    .willReturn(List.of());

            mockMvc.perform(get(RUTA)
                            .param("asignaturaId", ID_ASIGNATURA.toString())
                            .param("semestreId", ID_SEMESTRE.toString())
                            .param("incluirInactivas", "true"))
                    .andExpect(status().isOk());

            verify(matriculaService).listarMatriculados(ID_ASIGNATURA, ID_SEMESTRE, true);
        }

        @Test
        @DisplayName("Lista vacía devuelve 200 con un arreglo vacío, no 404")
        void listaVaciaDevuelve200() throws Exception {
            given(matriculaService.listarMatriculados(ID_ASIGNATURA, ID_SEMESTRE, false))
                    .willReturn(List.of());

            mockMvc.perform(get(RUTA)
                            .param("asignaturaId", ID_ASIGNATURA.toString())
                            .param("semestreId", ID_SEMESTRE.toString()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$.length()").value(0));
        }
    }
}
