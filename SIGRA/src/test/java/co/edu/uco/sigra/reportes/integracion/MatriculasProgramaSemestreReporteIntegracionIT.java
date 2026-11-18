package co.edu.uco.sigra.reportes.integracion;

import co.edu.uco.sigra.asignaturas.entity.Asignatura;
import co.edu.uco.sigra.estudiantes.entity.EstadoMatricula;
import co.edu.uco.sigra.estudiantes.entity.Estudiante;
import co.edu.uco.sigra.integracion.BaseIntegracionSigra;
import co.edu.uco.sigra.profesores.entity.Profesor;
import co.edu.uco.sigra.programas.entity.ProgramaAcademico;
import co.edu.uco.sigra.semestres.entity.Semestre;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Tag("integration")
class MatriculasProgramaSemestreReporteIntegracionIT extends BaseIntegracionSigra {

    @Override
    protected String prefijo() {
        return "R2IT";
    }

    @Test
    void agrupaFiltraOrdenaYProtegeElReporte() throws Exception {
        ProgramaAcademico programaDos = crearPrograma();
        Semestre semestreReciente = crearSemestreActivo();
        Semestre semestreAnterior = crearSemestrePasado();
        Asignatura asignaturaUno = crearAsignatura("UNO");
        asignaturaUno = asignaturaRepository.save(asignaturaUno);
        Asignatura asignaturaDos = new Asignatura(prefijo() + "-DOS", prefijo() + " Asignatura DOS", programaDos);
        asignaturaDos = asignaturaRepository.save(asignaturaDos);
        Estudiante estudianteCompartido = crearEstudiante("compartido");
        Estudiante estudianteOtro = crearEstudiante("otro");

        crearMatricula(estudianteCompartido, asignaturaUno, semestreReciente, EstadoMatricula.ACTIVA);
        crearMatricula(estudianteCompartido, asignaturaDos, semestreReciente, EstadoMatricula.DESVINCULADA);
        crearMatricula(estudianteOtro, asignaturaUno, semestreAnterior, EstadoMatricula.ACTIVA);

        String base = "/api/v1/reportes/matriculas-programa-semestre";
        String respuesta = mockMvc.perform(get(base).param("semestreId", semestreReciente.getId().toString())
                        .header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        List<java.util.Map<String, Object>> filas = com.jayway.jsonpath.JsonPath.read(respuesta, "$.filas");
        assertThat(filas).hasSize(2);
        assertThat(filas).extracting(f -> f.get("programaId"))
                .containsExactlyInAnyOrder(programa.getId().toString(), programaDos.getId().toString());
        assertThat(filas.stream().map(f -> f.get("programaNombre").toString()).toList()).isSorted();
        assertThat(filas).filteredOn(f -> f.get("programaId").equals(programa.getId().toString()))
                .singleElement().satisfies(f -> {
                    assertThat(f).containsEntry("totalMatriculas", 1)
                            .containsEntry("activas", 1).containsEntry("desvinculadas", 0)
                            .containsEntry("estudiantes", 1).containsEntry("asignaturas", 1);
                });
        assertThat(filas).filteredOn(f -> f.get("programaId").equals(programaDos.getId().toString()))
                .singleElement().satisfies(f -> assertThat(f).containsEntry("totalMatriculas", 1)
                        .containsEntry("activas", 0).containsEntry("desvinculadas", 1)
                        .containsEntry("estudiantes", 1).containsEntry("asignaturas", 1));
        assertThat((Number) com.jayway.jsonpath.JsonPath.read(respuesta, "$.resumen.estudiantesDistintos"))
                .isEqualTo(1);

        String programaFiltrado = mockMvc.perform(get(base).param("programaId", programaDos.getId().toString())
                        .header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(com.jayway.jsonpath.JsonPath.<List<?>>read(programaFiltrado, "$.filas")).hasSize(1);
        String combinado = mockMvc.perform(get(base)
                        .param("semestreId", semestreReciente.getId().toString())
                        .param("programaId", programaDos.getId().toString())
                        .header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(com.jayway.jsonpath.JsonPath.<Number>read(combinado, "$.resumen.totalMatriculas")).isEqualTo(1);

        String programaBase = mockMvc.perform(get(base)
                        .param("programaId", programa.getId().toString())
                        .header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        List<java.util.Map<String, Object>> baseFilas =
                com.jayway.jsonpath.JsonPath.read(programaBase, "$.filas");
        assertThat(baseFilas).hasSize(2);
        assertThat(baseFilas).extracting(f -> f.get("semestreId"))
                .containsExactly(semestreReciente.getId().toString(), semestreAnterior.getId().toString());
        assertThat(com.jayway.jsonpath.JsonPath.<Number>read(
                programaBase, "$.resumen.estudiantesDistintos")).isEqualTo(2);

        String facultadFiltrada = mockMvc.perform(get(base)
                        .param("facultadId", programa.getFacultad().getId().toString())
                        .header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        List<java.util.Map<String, Object>> facultadFilas =
                com.jayway.jsonpath.JsonPath.read(facultadFiltrada, "$.filas");
        assertThat(facultadFilas).anySatisfy(f ->
                assertThat(f.get("programaId")).isEqualTo(programa.getId().toString()));

        mockMvc.perform(get(base).header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isOk());
        Profesor profesor = crearProfesor("profesor");
        mockMvc.perform(get(base).header("Authorization", "Bearer " + token(profesor)))
                .andExpect(status().isForbidden());
        String tokenEstudiante = token(crearEstudiante("seguridad"));
        mockMvc.perform(get(base).header("Authorization", "Bearer " + tokenEstudiante))
                .andExpect(status().isForbidden());
        mockMvc.perform(get(base)).andExpect(status().isUnauthorized());
        mockMvc.perform(get(base).param("semestreId", UUID.randomUUID().toString())
                        .header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isNotFound());
        mockMvc.perform(get(base).param("programaId", UUID.randomUUID().toString())
                        .header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isNotFound());
    }
}
