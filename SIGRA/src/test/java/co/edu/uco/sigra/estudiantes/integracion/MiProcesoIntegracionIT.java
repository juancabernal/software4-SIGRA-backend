package co.edu.uco.sigra.estudiantes.integracion;

import co.edu.uco.sigra.auth.entity.Administrador;
import co.edu.uco.sigra.auth.entity.Usuario;
import co.edu.uco.sigra.profesores.entity.Profesor;
import co.edu.uco.sigra.auth.security.UsuarioPrincipal;
import co.edu.uco.sigra.common.enums.EstadoRegistro;
import co.edu.uco.sigra.common.enums.RolUsuario;
import co.edu.uco.sigra.estadisticas.entity.NivelLogro;
import co.edu.uco.sigra.estadisticas.entity.NivelLogroEstudiante;
import co.edu.uco.sigra.estudiantes.entity.EstadoMatricula;
import co.edu.uco.sigra.estudiantes.entity.Estudiante;
import co.edu.uco.sigra.estudiantes.entity.Matricula;
import co.edu.uco.sigra.evaluaciones.entity.EstadoEvaluacion;
import co.edu.uco.sigra.evaluaciones.entity.Evaluacion;
import co.edu.uco.sigra.resultadosaprendizaje.entity.ResultadoAprendizaje;
import co.edu.uco.sigra.asignaturas.entity.Asignatura;
import co.edu.uco.sigra.semestres.entity.EstadoSemestre;
import co.edu.uco.sigra.semestres.entity.Semestre;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

@Tag("integration")
@AutoConfigureMockMvc
class MiProcesoIntegracionIT extends co.edu.uco.sigra.integracion.BaseIntegracionSigra {

    @Override
    protected String prefijo() {
        return "MIP";
    }

    @Test
    void filtraPorEstudianteYProtegeDetalleAjeno() throws Exception {
        Estudiante estudianteA = crearEstudiante("a");
        Estudiante estudianteB = crearEstudiante("b");
        Asignatura asignatura = crearAsignatura("FILTRO");
        Semestre semestre = crearSemestreActivo();
        Matricula matriculaA = crearMatricula(estudianteA, asignatura, semestre, EstadoMatricula.ACTIVA);

        assertThat(get("/api/v1/estudiantes/me/materias", token(estudianteB)).getResponse().getContentAsString())
                .isEqualTo("[]");
        assertThat(get("/api/v1/estudiantes/me/progreso", token(estudianteB)).getResponse().getContentAsString())
                .isEqualTo("[]");
        assertThat(get("/api/v1/estudiantes/me/historial", token(estudianteB)).getResponse().getContentAsString())
                .isEqualTo("[]");
        assertThat(get("/api/v1/estudiantes/me/materias/" + matriculaA.getId(), token(estudianteB))
                .getResponse().getStatus()).isEqualTo(404);
    }

    @Test
    void detalleIncluyeRaEvaluacionNotaObservacionYNiveles() throws Exception {
        Estudiante estudiante = crearEstudiante("detalle");
        Asignatura asignatura = crearAsignatura("DETALLE");
        Semestre semestre = crearSemestreActivo();
        Matricula matricula = matriculaDe(estudiante, asignatura, semestre);
        ResultadoAprendizaje ra = crearRa(asignatura, "RA-1");
        Evaluacion evaluacion = crearEvaluacion(ra, semestre, 50, EstadoEvaluacion.ACTIVA);
        calificar(evaluacion, matricula, 4.5);
        NivelLogroEstudiante nivel = new NivelLogroEstudiante(ra, semestre, estudiante);
        nivel.actualizar(3, 4.5, NivelLogro.ALTO, LocalDateTime.now());
        nivelLogroRepository.save(nivel);

        MvcResult resultado = get("/api/v1/estudiantes/me/materias/" + matricula.getId(), token(estudiante));
        String json = resultado.getResponse().getContentAsString();
        assertThat(resultado.getResponse().getStatus()).isEqualTo(200);
        assertThat((String) JsonPath.read(json, "$.resultadosAprendizaje[0].codigo")).isEqualTo("RA-1");
        assertThat((String) JsonPath.read(json, "$.resultadosAprendizaje[0].nivelLogro")).isEqualTo("ALTO");
        assertThat(((Number) JsonPath.read(json, "$.resultadosAprendizaje[0].evaluacionesCalificadas")).intValue())
                .isEqualTo(1);
        assertThat(((Number) JsonPath.read(json, "$.resultadosAprendizaje[0].evaluaciones[0].nota")).doubleValue())
                .isEqualTo(4.5);
        assertThat((String) JsonPath.read(json, "$.resultadosAprendizaje[0].evaluaciones[0].observacion"))
                .isEqualTo("MIP observacion de prueba");
    }

    @Test
    void listasVaciasParaEstudianteSinMatriculas() throws Exception {
        Estudiante estudiante = crearEstudiante("vacio");
        for (String ruta : List.of("materias", "progreso", "historial")) {
            MvcResult resultado = get("/api/v1/estudiantes/me/" + ruta, token(estudiante));
            assertThat(resultado.getResponse().getStatus()).isEqualTo(200);
            assertThat(resultado.getResponse().getContentAsString()).isEqualTo("[]");
        }
    }

    @Test
    void historialClasificaDesvinculadaYPasadaYMateriasIncluyeProgramada() throws Exception {
        Estudiante estudiante = crearEstudiante("situaciones");
        Asignatura asignatura = crearAsignatura("SITUACIONES");
        Matricula desvinculada = crearMatricula(estudiante, asignatura, crearSemestrePasado(),
                EstadoMatricula.DESVINCULADA);
        Semestre pasada = crearSemestrePasado();
        Matricula cursada = crearMatricula(estudiante, asignatura, pasada, EstadoMatricula.ACTIVA);
        Semestre futura = crearSemestre("F", LocalDate.now().plusDays(1), LocalDate.now().plusDays(30));
        Matricula programada = crearMatricula(estudiante, asignatura, futura, EstadoMatricula.ACTIVA);

        String historial = get("/api/v1/estudiantes/me/historial", token(estudiante))
                .getResponse().getContentAsString();
        String materias = get("/api/v1/estudiantes/me/materias", token(estudiante))
                .getResponse().getContentAsString();
        assertThat(historial).contains(desvinculada.getId().toString(), cursada.getId().toString())
                .doesNotContain(programada.getId().toString());
        assertThat(materias).contains(programada.getId().toString())
                .doesNotContain(desvinculada.getId().toString(), cursada.getId().toString());
    }

    @Test
    void nivelesAusenteYNuloSonSinEvaluarYNivelPresenteSeExpone() throws Exception {
        Estudiante estudiante = crearEstudiante("niveles");
        Asignatura asignatura = crearAsignatura("NIVELES");
        Semestre semestre = crearSemestreActivo();
        Matricula matricula = matriculaDe(estudiante, asignatura, semestre);
        ResultadoAprendizaje sinFila = crearRa(asignatura, "RA-1");
        ResultadoAprendizaje nulo = crearRa(asignatura, "RA-2");
        ResultadoAprendizaje presente = crearRa(asignatura, "RA-3");
        NivelLogroEstudiante filaNula = new NivelLogroEstudiante(nulo, semestre, estudiante);
        filaNula.actualizar(1, null, null, LocalDateTime.now());
        nivelLogroRepository.save(filaNula);
        NivelLogroEstudiante filaPresente = new NivelLogroEstudiante(presente, semestre, estudiante);
        filaPresente.actualizar(3, 4.0, NivelLogro.MEDIO, LocalDateTime.now());
        nivelLogroRepository.save(filaPresente);

        String json = get("/api/v1/estudiantes/me/materias/" + matricula.getId(), token(estudiante))
                .getResponse().getContentAsString();
        assertThat(json).contains("\"codigo\":\"" + sinFila.getCodigo() + "\",\"descripcion\"")
                .contains("\"nivelLogro\":\"SIN_EVALUAR\"")
                .contains("\"codigo\":\"" + presente.getCodigo() + "\"")
                .contains("\"nivelLogro\":\"MEDIO\"");
    }

    @Test
    void raYEvaluacionInactivosNoAparecen() throws Exception {
        Estudiante estudiante = crearEstudiante("inactivos");
        Asignatura asignatura = crearAsignatura("INACTIVOS");
        Semestre semestre = crearSemestreActivo();
        Matricula matricula = matriculaDe(estudiante, asignatura, semestre);
        ResultadoAprendizaje raActivo = crearRa(asignatura, "RA-ACTIVO");
        ResultadoAprendizaje raInactivo = crearRa(asignatura, "RA-INACTIVO", EstadoRegistro.INACTIVO);
        crearEvaluacion(raActivo, semestre, 50, EstadoEvaluacion.INACTIVA);
        crearEvaluacion(raInactivo, semestre, 50, EstadoEvaluacion.ACTIVA);

        String json = get("/api/v1/estudiantes/me/materias/" + matricula.getId(), token(estudiante))
                .getResponse().getContentAsString();
        assertThat(json).contains("RA-ACTIVO").doesNotContain("RA-INACTIVO");
        assertThat(json).doesNotContain("MIP Eval");
    }

    @Test
    void rolesNoEstudianteDan403YSinToken401() throws Exception {
        Administrador administrador = crearUsuario(new Administrador(), "admin");
        assertThat(get("/api/v1/estudiantes/me/materias", token(administrador)).getResponse().getStatus())
                .isEqualTo(403);
        Profesor profesor = crearProfesor("profesor");
        assertThat(get("/api/v1/estudiantes/me/materias", token(profesor)).getResponse().getStatus())
                .isEqualTo(403);
        assertThat(get("/api/v1/estudiantes/me/materias", null).getResponse().getStatus())
                .isEqualTo(401);
    }

    private MvcResult get(String ruta, String token) throws Exception {
        var request = org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(ruta);
        if (token != null) {
            request.header("Authorization", "Bearer " + token);
        }
        return mockMvc.perform(request).andReturn();
    }

    private Semestre crearSemestre(String codigo, LocalDate inicio, LocalDate fin) {
        Semestre semestre = new Semestre();
        semestre.setCodigo(prefijo() + "-S-" + codigo + "-" + UUID.randomUUID().toString().substring(0, 8));
        semestre.setFechaInicio(inicio);
        semestre.setFechaFin(fin);
        semestre.setEstado(EstadoSemestre.INACTIVO);
        return semestreRepository.save(semestre);
    }
}
