package co.edu.uco.sigra.resultadosaprendizaje.integracion;

import co.edu.uco.sigra.asignaturas.entity.Asignatura;
import co.edu.uco.sigra.common.enums.EstadoRegistro;
import co.edu.uco.sigra.estudiantes.entity.EstadoMatricula;
import co.edu.uco.sigra.estudiantes.entity.Estudiante;
import co.edu.uco.sigra.estudiantes.entity.Matricula;
import co.edu.uco.sigra.evaluaciones.entity.EstadoEvaluacion;
import co.edu.uco.sigra.evaluaciones.entity.Evaluacion;
import co.edu.uco.sigra.integracion.BaseIntegracionSigra;
import co.edu.uco.sigra.profesores.entity.Profesor;
import co.edu.uco.sigra.resultadosaprendizaje.entity.ResultadoAprendizaje;
import co.edu.uco.sigra.semestres.entity.Semestre;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// INTEGRATION: contexto completo + PostgreSQL real. Ejecutar con: .\gradlew.bat integrationTest
// Prueba de punta a punta del rendimiento por RA (RF-06): datos reales (evaluaciones, matriculas y
// calificaciones) -> recalculo del nivel de logro (modulo de estadisticas) -> GET /rendimiento.
@Tag("integration")
class RendimientoIntegracionIT extends BaseIntegracionSigra {

    private static final String URL_RENDIMIENTO = "/api/v1/asignaturas/{id}/resultados-aprendizaje/rendimiento";
    private static final String URL_RECALCULO =
            "/api/v1/resultados-aprendizaje/{ra}/semestres/{sem}/estadisticas/recalculo";

    @Override
    protected String prefijo() {
        return "ITR";
    }

    // ---------------------------------------------------------------- ayudas

    private void recalcular(ResultadoAprendizaje ra, Semestre semestre) throws Exception {
        mockMvc.perform(post(URL_RECALCULO, ra.getId(), semestre.getId())
                        .header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isOk());
    }

    private String consultar(Asignatura asignatura, String bearer) throws Exception {
        return mockMvc.perform(get(URL_RENDIMIENTO, asignatura.getId()).header("Authorization", "Bearer " + bearer))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }

    private Map<String, Object> fila(String json, String codigoRa) {
        List<Map<String, Object>> filas = JsonPath.read(json, "$.resultados[?(@.codigo=='" + codigoRa + "')]");
        assertThat(filas).as("fila del RA " + codigoRa).hasSize(1);
        return filas.get(0);
    }

    private static double numero(Object valor) {
        return ((Number) valor).doubleValue();
    }

    /** Tres evaluaciones activas 30/30/40 sobre el RA. */
    private Evaluacion[] tresEvaluaciones(ResultadoAprendizaje ra, Semestre semestre) {
        return new Evaluacion[]{
                crearEvaluacion(ra, semestre, 30, EstadoEvaluacion.ACTIVA),
                crearEvaluacion(ra, semestre, 30, EstadoEvaluacion.ACTIVA),
                crearEvaluacion(ra, semestre, 40, EstadoEvaluacion.ACTIVA)};
    }

    private void calificarTodas(Evaluacion[] evaluaciones, Matricula matricula, double... notas) {
        for (int i = 0; i < evaluaciones.length; i++) {
            calificar(evaluaciones[i], matricula, notas[i]);
        }
    }

    // ---------------------------------------------------------------- flujo de punta a punta

    @Test
    @DisplayName("Con 3 evaluaciones al 100% calificadas a todos: CON_NIVEL, promedio de estudiantes y completo")
    void flujoCompleto() throws Exception {
        Semestre semestre = crearSemestreActivo();
        Asignatura asignatura = crearAsignatura("A1");
        ResultadoAprendizaje ra = crearRa(asignatura, "ITR-RA-1");
        Evaluacion[] evaluaciones = tresEvaluaciones(ra, semestre);

        Estudiante ana = crearEstudiante("ana");
        Estudiante beto = crearEstudiante("beto");
        Estudiante retirado = crearEstudiante("retirado");
        calificarTodas(evaluaciones, matriculaDe(ana, asignatura, semestre), 3.0, 4.0, 5.0);   // 4.10
        calificarTodas(evaluaciones, matriculaDe(beto, asignatura, semestre), 4.0, 4.0, 4.0);  // 4.00
        // Desvinculado y sin notas: no debe impedir que el RA figure como evaluado al 100%.
        crearMatricula(retirado, asignatura, semestre, EstadoMatricula.DESVINCULADA);

        recalcular(ra, semestre);
        Map<String, Object> f = fila(consultar(asignatura, tokenAdmin), "ITR-RA-1");

        assertThat(f.get("estado")).isEqualTo("CON_NIVEL");
        assertThat(f.get("nivelLogro")).isEqualTo("ALTO");
        assertThat(numero(f.get("promedioNota"))).isCloseTo(4.05, within(0.011));
        assertThat(new BigDecimal(f.get("porcentajeAsignado").toString())).isEqualByComparingTo("100");
        assertThat(new BigDecimal(f.get("porcentajeEvaluado").toString())).isEqualByComparingTo("100");
        assertThat(f.get("evaluacionesActivas")).isEqualTo(3);
        assertThat(f.get("minimoEvaluaciones")).isEqualTo(3);
        assertThat(f.get("completo")).isEqualTo(true);
    }

    @Test
    @DisplayName("Parcial: la ultima evaluacion solo calificada a uno -> hay nivel pero evaluado 60% y no completo")
    void rendimientoParcial() throws Exception {
        Semestre semestre = crearSemestreActivo();
        Asignatura asignatura = crearAsignatura("A2");
        ResultadoAprendizaje ra = crearRa(asignatura, "ITR-RA-2");
        Evaluacion[] evaluaciones = tresEvaluaciones(ra, semestre);

        Matricula ana = matriculaDe(crearEstudiante("ana"), asignatura, semestre);
        Matricula beto = matriculaDe(crearEstudiante("beto"), asignatura, semestre);
        calificarTodas(evaluaciones, ana, 4.0, 4.0, 4.0);
        calificar(evaluaciones[0], beto, 3.0);
        calificar(evaluaciones[1], beto, 3.0);   // la tercera de Beto queda sin calificar

        recalcular(ra, semestre);
        Map<String, Object> f = fila(consultar(asignatura, tokenAdmin), "ITR-RA-2");

        assertThat(f.get("estado")).isEqualTo("CON_NIVEL");
        assertThat(new BigDecimal(f.get("porcentajeAsignado").toString())).isEqualByComparingTo("100");
        assertThat(new BigDecimal(f.get("porcentajeEvaluado").toString())).isEqualByComparingTo("60");
        assertThat(f.get("completo")).isEqualTo(false);
    }

    @Test
    @DisplayName("Con solo 2 evaluaciones (aunque sumen 100% y esten calificadas) -> DATOS_INSUFICIENTES y no completo")
    void conDosEvaluacionesNoHayNivel() throws Exception {
        Semestre semestre = crearSemestreActivo();
        Asignatura asignatura = crearAsignatura("A3");
        ResultadoAprendizaje ra = crearRa(asignatura, "ITR-RA-3");
        Evaluacion e1 = crearEvaluacion(ra, semestre, 50, EstadoEvaluacion.ACTIVA);
        Evaluacion e2 = crearEvaluacion(ra, semestre, 50, EstadoEvaluacion.ACTIVA);
        Matricula ana = matriculaDe(crearEstudiante("ana"), asignatura, semestre);
        calificar(e1, ana, 4.0);
        calificar(e2, ana, 4.0);

        recalcular(ra, semestre);
        Map<String, Object> f = fila(consultar(asignatura, tokenAdmin), "ITR-RA-3");

        assertThat(f.get("estado")).isEqualTo("DATOS_INSUFICIENTES");
        assertThat(f.get("nivelLogro")).isNull();
        assertThat(f.get("evaluacionesActivas")).isEqualTo(2);
        assertThat(f.get("completo")).isEqualTo(false);
    }

    @Test
    @DisplayName("RA con evaluaciones pero sin ninguna calificacion -> SIN_EVALUAR")
    void sinCalificaciones() throws Exception {
        Semestre semestre = crearSemestreActivo();
        Asignatura asignatura = crearAsignatura("A4");
        ResultadoAprendizaje ra = crearRa(asignatura, "ITR-RA-4");
        tresEvaluaciones(ra, semestre);
        matriculaDe(crearEstudiante("ana"), asignatura, semestre);

        recalcular(ra, semestre);
        Map<String, Object> f = fila(consultar(asignatura, tokenAdmin), "ITR-RA-4");

        assertThat(f.get("estado")).isEqualTo("SIN_EVALUAR");
        assertThat(f.get("nivelLogro")).isNull();
        assertThat(f.get("completo")).isEqualTo(false);
    }

    @Test
    @DisplayName("Un RA INACTIVO no aparece y una evaluacion INACTIVA no cuenta en lo asignado")
    void inactivosNoCuentan() throws Exception {
        Semestre semestre = crearSemestreActivo();
        Asignatura asignatura = crearAsignatura("A5");
        ResultadoAprendizaje activo = crearRa(asignatura, "ITR-RA-5");
        crearRa(asignatura, "ITR-RA-6", EstadoRegistro.INACTIVO);
        tresEvaluaciones(activo, semestre);
        crearEvaluacion(activo, semestre, 25, EstadoEvaluacion.INACTIVA);

        recalcular(activo, semestre);
        String json = consultar(asignatura, tokenAdmin);

        List<String> codigos = JsonPath.read(json, "$.resultados[*].codigo");
        assertThat(codigos).containsExactly("ITR-RA-5");
        Map<String, Object> f = fila(json, "ITR-RA-5");
        assertThat(new BigDecimal(f.get("porcentajeAsignado").toString())).isEqualByComparingTo("100");
        assertThat(f.get("evaluacionesActivas")).isEqualTo(3);
    }

    @Test
    @DisplayName("Con semestreId se consulta ese semestre (pasado) sin mezclar datos del semestre activo")
    void semestrePasadoPorParametro() throws Exception {
        Semestre activo = crearSemestreActivo();
        Semestre pasado = crearSemestrePasado();
        Asignatura asignatura = crearAsignatura("A7");
        ResultadoAprendizaje ra = crearRa(asignatura, "ITR-RA-8");
        tresEvaluaciones(ra, activo);                                   // el activo no tiene notas
        Evaluacion[] enPasado = tresEvaluaciones(ra, pasado);
        calificarTodas(enPasado, matriculaDe(crearEstudiante("ana"), asignatura, pasado), 4.0, 4.0, 4.0);

        String json = mockMvc.perform(get(URL_RENDIMIENTO, asignatura.getId())
                        .param("semestreId", pasado.getId().toString())
                        .header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat((String) JsonPath.read(json, "$.semestreId")).isEqualTo(pasado.getId().toString());
        Map<String, Object> f = fila(json, "ITR-RA-8");
        assertThat(new BigDecimal(f.get("porcentajeEvaluado").toString())).isEqualByComparingTo("100");
        // Hay notas reales (ya no es SIN_EVALUAR) aunque nadie haya recalculado el nivel de ese semestre.
        assertThat(f.get("estado")).isEqualTo("DATOS_INSUFICIENTES");
        // Y el semestre activo sigue sin notas.
        assertThat(fila(consultar(asignatura, tokenAdmin), "ITR-RA-8").get("estado")).isEqualTo("SIN_EVALUAR");
    }

    @Test
    @DisplayName("semestreId inexistente -> 404")
    void semestreInexistente() throws Exception {
        crearSemestreActivo();
        Asignatura asignatura = crearAsignatura("A8");
        mockMvc.perform(get(URL_RENDIMIENTO, asignatura.getId()).param("semestreId", UUID.randomUUID().toString())
                        .header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isNotFound());
    }

    // ---------------------------------------------------------------- seguridad y errores

    @Test
    @DisplayName("Seguridad: sin token 401, estudiante 403, profesor sin asignacion 403, con asignacion y admin 200")
    void seguridad() throws Exception {
        crearSemestreActivo();
        Asignatura asignatura = crearAsignatura("A6");
        crearRa(asignatura, "ITR-RA-7");
        Profesor asignado = crearProfesor("asignado");
        Profesor ajeno = crearProfesor("ajeno");
        asignar(asignado, asignatura);

        mockMvc.perform(get(URL_RENDIMIENTO, asignatura.getId())).andExpect(status().isUnauthorized());
        mockMvc.perform(get(URL_RENDIMIENTO, asignatura.getId())
                        .header("Authorization", "Bearer " + token(crearEstudiante("alumno"))))
                .andExpect(status().isForbidden());
        mockMvc.perform(get(URL_RENDIMIENTO, asignatura.getId())
                        .header("Authorization", "Bearer " + token(ajeno)))
                .andExpect(status().isForbidden());
        consultar(asignatura, token(asignado));
        consultar(asignatura, tokenAdmin);
    }

    @Test
    @DisplayName("Asignatura inexistente -> 404")
    void asignaturaInexistente() throws Exception {
        mockMvc.perform(get(URL_RENDIMIENTO, UUID.randomUUID()).header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isNotFound());
    }
}
