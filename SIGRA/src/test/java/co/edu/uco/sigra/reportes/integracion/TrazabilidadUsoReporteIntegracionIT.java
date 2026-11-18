package co.edu.uco.sigra.reportes.integracion;

import co.edu.uco.sigra.asignaturas.entity.Asignatura;
import co.edu.uco.sigra.estudiantes.entity.EstadoMatricula;
import co.edu.uco.sigra.estudiantes.entity.Estudiante;
import co.edu.uco.sigra.integracion.BaseIntegracionSigra;
import co.edu.uco.sigra.profesores.entity.Profesor;
import co.edu.uco.sigra.semestres.entity.Semestre;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * R7 contra PostgreSQL real: ejecuta las consultas JPQL de agregados y del listado de estudiantes del
 * profesor. No inserta eventos en {@code historial_auditoria} (es de solo inserción y no se podrían borrar):
 * los usuarios se crean por repositorio y los tokens se firman directamente, sin pasar por el login.
 */
@Tag("integration")
class TrazabilidadUsoReporteIntegracionIT extends BaseIntegracionSigra {

    @Override
    protected String prefijo() {
        return "R7IT";
    }

    @Test
    void administradorConsultaAgregadosPorRol() throws Exception {
        mockMvc.perform(get("/api/v1/reportes/trazabilidad-uso")
                        .header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.porRol[*].rol", hasItems("ADMINISTRADOR", "PROFESOR", "ESTUDIANTE")))
                .andExpect(jsonPath("$.resumen.totalEventos").isNumber());

        mockMvc.perform(get("/api/v1/reportes/trazabilidad-uso").param("rol", "ESTUDIANTE")
                        .header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.porRol", hasSize(1)))
                .andExpect(jsonPath("$.porRol[0].rol").value("ESTUDIANTE"));

        mockMvc.perform(get("/api/v1/reportes/trazabilidad-uso")
                        .param("desde", "2026-11-15").param("hasta", "2026-11-01")
                        .header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isBadRequest());
    }

    @Test
    void profesorVeSoloEstudiantesActivosDeSusAsignaturas() throws Exception {
        Semestre semestre = crearSemestreActivo();
        Asignatura propia = crearAsignatura("PROPIA");
        Asignatura ajena = crearAsignatura("AJENA");
        Profesor profesor = crearProfesor("profe");
        asignar(profesor, propia);
        Estudiante ana = crearEstudiante("ana");
        Estudiante luis = crearEstudiante("luis");
        Estudiante retirado = crearEstudiante("retirado");
        Estudiante deOtro = crearEstudiante("otro");
        crearMatricula(ana, propia, semestre, EstadoMatricula.ACTIVA);
        crearMatricula(luis, propia, semestre, EstadoMatricula.ACTIVA);
        crearMatricula(retirado, propia, semestre, EstadoMatricula.DESVINCULADA);
        crearMatricula(deOtro, ajena, semestre, EstadoMatricula.ACTIVA);

        mockMvc.perform(get("/api/v1/reportes/trazabilidad-uso/profesor")
                        .param("semestreId", semestre.getId().toString())
                        .header("Authorization", "Bearer " + token(profesor)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.semestreId").value(semestre.getId().toString()))
                .andExpect(jsonPath("$.estudiantes", hasSize(2)))
                .andExpect(jsonPath("$.estudiantes[*].estudianteId",
                        containsInAnyOrder(ana.getId().toString(), luis.getId().toString())))
                .andExpect(jsonPath("$.estudiantes[0].asignaturas[0]").value(propia.getCodigo()))
                .andExpect(jsonPath("$.estudiantes[0].ingresos").value(0))
                .andExpect(jsonPath("$.resumenEstudiantes.sinActividad").value(2));

        mockMvc.perform(get("/api/v1/reportes/trazabilidad-uso/profesor")
                        .header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isForbidden());
    }
}
