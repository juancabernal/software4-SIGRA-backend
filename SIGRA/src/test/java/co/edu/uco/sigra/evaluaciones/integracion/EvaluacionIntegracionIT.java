package co.edu.uco.sigra.evaluaciones.integracion;

import co.edu.uco.sigra.asignaturas.entity.Asignatura;
import co.edu.uco.sigra.evaluaciones.entity.EstadoEvaluacion;
import co.edu.uco.sigra.evaluaciones.entity.Evaluacion;
import co.edu.uco.sigra.integracion.BaseIntegracionSigra;
import co.edu.uco.sigra.profesores.entity.Profesor;
import co.edu.uco.sigra.resultadosaprendizaje.entity.ResultadoAprendizaje;
import co.edu.uco.sigra.semestres.entity.Semestre;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// INTEGRATION: contexto completo + PostgreSQL real. Ejecutar con: .\gradlew.bat integrationTest
// Cubre lo que los unitarios no pueden: reglas de fecha y de suma por HTTP, la concurrencia sobre el
// bloqueo de la fila del RA y la cascada de inactivar/reactivar RA limitada al semestre activo.
@Tag("integration")
class EvaluacionIntegracionIT extends BaseIntegracionSigra {

    private static final String URL_EVALUACIONES = "/api/v1/resultados-aprendizaje/{ra}/evaluaciones";

    @Override
    protected String prefijo() {
        return "ITV";
    }

    private static String cuerpo(String nombre, LocalDate fecha, String porcentaje) {
        return "{\"nombre\":\"" + nombre + "\",\"fechaTentativa\":\"" + fecha
                + "\",\"porcentaje\":" + porcentaje + ",\"descripcion\":\"Evaluacion de prueba\"}";
    }

    private int crear(ResultadoAprendizaje ra, String bearer, String nombre, LocalDate fecha, String porcentaje)
            throws Exception {
        return mockMvc.perform(post(URL_EVALUACIONES, ra.getId())
                        .header("Authorization", "Bearer " + bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo(nombre, fecha, porcentaje)))
                .andReturn().getResponse().getStatus();
    }

    // ---------------------------------------------------------------- creacion

    @Test
    @DisplayName("Crear: manana es valida (201, semestre activo) y hoy o una fecha pasada se rechazan (400)")
    void fechaPosteriorAHoy() throws Exception {
        Semestre semestre = crearSemestreActivo();
        Asignatura asignatura = crearAsignaturaActiva("V1");
        ResultadoAprendizaje ra = crearRa(asignatura, "ITV-RA-1");
        Profesor profesor = crearProfesor("prof");
        asignar(profesor, asignatura);
        String bearer = token(profesor);

        mockMvc.perform(post(URL_EVALUACIONES, ra.getId())
                        .header("Authorization", "Bearer " + bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo("Parcial uno", LocalDate.now().plusDays(1), "30")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.semestreId").value(semestre.getId().toString()))
                .andExpect(jsonPath("$.estado").value("ACTIVA"));

        assertThat(crear(ra, bearer, "Parcial hoy", LocalDate.now(), "10")).isEqualTo(400);
        assertThat(crear(ra, bearer, "Parcial ayer", LocalDate.now().minusDays(1), "10")).isEqualTo(400);
        Integer total = jdbc.queryForObject(
                "select count(*) from evaluacion where resultado_aprendizaje_id = ?", Integer.class, ra.getId());
        assertThat(total).isEqualTo(1);
    }

    @Test
    @DisplayName("Crear: un profesor sin asignacion en la asignatura recibe 403 y no se crea nada")
    void profesorSinAsignacion() throws Exception {
        crearSemestreActivo();
        Asignatura asignatura = crearAsignaturaActiva("V2");
        ResultadoAprendizaje ra = crearRa(asignatura, "ITV-RA-2");
        String ajeno = token(crearProfesor("ajeno"));

        assertThat(crear(ra, ajeno, "Parcial ajeno", LocalDate.now().plusDays(1), "30")).isEqualTo(403);
    }

    @Test
    @DisplayName("Crear: la suma de porcentajes de las activas del RA no puede superar 100")
    void sumaNoSuperaCien() throws Exception {
        crearSemestreActivo();
        Asignatura asignatura = crearAsignaturaActiva("V3");
        ResultadoAprendizaje ra = crearRa(asignatura, "ITV-RA-3");
        Profesor profesor = crearProfesor("prof");
        asignar(profesor, asignatura);
        String bearer = token(profesor);
        LocalDate manana = LocalDate.now().plusDays(1);

        assertThat(crear(ra, bearer, "Primera", manana, "60")).isEqualTo(201);
        assertThat(crear(ra, bearer, "Segunda", manana, "50")).isEqualTo(400);
        assertThat(crear(ra, bearer, "Tercera", manana, "40")).isEqualTo(201);
    }

    @Test
    @DisplayName("Concurrencia: dos creaciones simultaneas de 60% -> exactamente una se crea (bloqueo del RA)")
    void concurrenciaSobreElMismoRa() throws Exception {
        crearSemestreActivo();
        Asignatura asignatura = crearAsignaturaActiva("V4");
        ResultadoAprendizaje ra = crearRa(asignatura, "ITV-RA-4");
        Profesor profesor = crearProfesor("prof");
        asignar(profesor, asignatura);
        String bearer = token(profesor);
        LocalDate manana = LocalDate.now().plusDays(1);

        CountDownLatch salida = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            List<Future<Integer>> futuros = new ArrayList<>();
            for (String nombre : List.of("Carrera uno", "Carrera dos")) {
                Callable<Integer> peticion = () -> {
                    salida.await();
                    return crear(ra, bearer, nombre, manana, "60");
                };
                futuros.add(pool.submit(peticion));
            }
            salida.countDown();
            List<Integer> estados = new ArrayList<>();
            for (Future<Integer> f : futuros) {
                estados.add(f.get());
            }
            Collections.sort(estados);
            assertThat(estados).containsExactly(201, 400);
        } finally {
            pool.shutdownNow();
        }
    }

    // ---------------------------------------------------------------- cascada por RA

    @Test
    @DisplayName("Inactivar/reactivar RA: la cascada solo toca el semestre activo; los semestres pasados no cambian")
    void cascadaSoloEnSemestreActivo() throws Exception {
        Semestre activo = crearSemestreActivo();
        Semestre pasado = crearSemestrePasado();
        Asignatura asignatura = crearAsignaturaActiva("V5");
        ResultadoAprendizaje ra = crearRa(asignatura, "ITV-RA-5");
        for (int i = 6; i <= 10; i++) {   // 6 RA activos en total: inactivar uno no baja del minimo
            crearRa(asignatura, "ITV-RA-" + i);
        }
        Evaluacion enActivo = crearEvaluacion(ra, activo, 50, EstadoEvaluacion.ACTIVA);
        Evaluacion enPasado = crearEvaluacion(ra, pasado, 50, EstadoEvaluacion.ACTIVA);
        Evaluacion pasadaInactiva = crearEvaluacion(ra, pasado, 20, EstadoEvaluacion.INACTIVA);
        pasadaInactiva.setInactivadaConRa(true);
        evaluacionRepository.save(pasadaInactiva);

        mockMvc.perform(patch("/api/v1/resultados-aprendizaje/{id}/inactivar", ra.getId())
                        .header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isOk());

        assertThat(estado(enActivo)).isEqualTo(EstadoEvaluacion.INACTIVA);
        assertThat(estado(enPasado)).isEqualTo(EstadoEvaluacion.ACTIVA);
        assertThat(estado(pasadaInactiva)).isEqualTo(EstadoEvaluacion.INACTIVA);

        mockMvc.perform(patch("/api/v1/resultados-aprendizaje/{id}/reactivar", ra.getId())
                        .header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isOk());

        assertThat(estado(enActivo)).isEqualTo(EstadoEvaluacion.ACTIVA);
        assertThat(estado(enPasado)).isEqualTo(EstadoEvaluacion.ACTIVA);
        assertThat(estado(pasadaInactiva)).isEqualTo(EstadoEvaluacion.INACTIVA);
    }

    private EstadoEvaluacion estado(Evaluacion e) {
        return evaluacionRepository.findById(e.getId()).orElseThrow().getEstado();
    }
}
