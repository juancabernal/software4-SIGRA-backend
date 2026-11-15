package co.edu.uco.sigra.asignaturas;

import co.edu.uco.sigra.asignaturas.dto.AsignaturaFiltroDTO;
import co.edu.uco.sigra.asignaturas.dto.AsignaturaResponseDTO;
import co.edu.uco.sigra.asignaturas.entity.Asignatura;
import co.edu.uco.sigra.asignaturas.service.AsignaturaService;
import co.edu.uco.sigra.auth.security.UsuarioPrincipal;
import co.edu.uco.sigra.common.enums.RolUsuario;
import co.edu.uco.sigra.evaluaciones.entity.EstadoEvaluacion;
import co.edu.uco.sigra.integracion.BaseIntegracionSigra;
import co.edu.uco.sigra.semestres.entity.Semestre;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** Contexto completo: valida las consultas con Hibernate y PostgreSQL, sin mocks del repositorio. */
@Tag("integration")
class AsignaturasPorSemestreIT extends BaseIntegracionSigra {
    @Autowired private AsignaturaService service;

    @Override
    protected String prefijo() { return "IT44"; }

    private List<UUID> consultar(Semestre semestre, UsuarioPrincipal identidad) {
        return service.consultar(new AsignaturaFiltroDTO(null, programa.getId(), null,
                        null, null, semestre == null ? null : semestre.getId()), identidad)
                .stream().map(AsignaturaResponseDTO::id).toList();
    }

    private UsuarioPrincipal admin() {
        return new UsuarioPrincipal(UUID.randomUUID(), RolUsuario.ADMINISTRADOR);
    }

    @Test
    void conservaCatalogoSinFiltroYExcluyeAsignacionesPosterioresAlSemestre() {
        var profesor = crearProfesor("docente");
        var materia = crearAsignatura("ACT");
        asignar(profesor, materia);
        var actual = crearSemestreActivo();
        var pasado = crearSemestrePasado();

        assertThat(consultar(null, admin())).containsExactly(materia.getId());
        assertThat(consultar(actual, admin())).containsExactly(materia.getId());
        assertThat(consultar(pasado, admin())).isEmpty();
    }

    @Test
    void uneEvaluacionesYMatriculasHistoricasSinDuplicarAsignaturas() {
        var pasado = crearSemestrePasado();
        var profesor = crearProfesor("docente");
        var estudiante = crearEstudiante("alumno");
        var evaluada = crearAsignatura("EVA");
        var matriculada = crearAsignatura("MAT");
        asignar(profesor, evaluada);
        asignar(profesor, matriculada);
        crearEvaluacion(crearRa(evaluada, "RA1"), pasado, 20, EstadoEvaluacion.ACTIVA);
        matriculaDe(estudiante, evaluada, pasado);
        matriculaDe(estudiante, matriculada, pasado);

        assertThat(consultar(pasado, admin()))
                .containsExactlyInAnyOrder(evaluada.getId(), matriculada.getId());
        assertThat(consultar(pasado, new UsuarioPrincipal(profesor.getId(), RolUsuario.PROFESOR)))
                .containsExactlyInAnyOrder(evaluada.getId(), matriculada.getId());
    }

    @Test
    void actividadDeOtroProfesorNoAmpliaElAlcanceDelDocente() {
        var actual = crearSemestreActivo();
        var profesor = crearProfesor("docente");
        var otro = crearProfesor("otro");
        var propia = crearAsignatura("PRO");
        var ajena = crearAsignatura("OTR");
        var sinDocente = crearAsignatura("SIN");
        asignar(profesor, propia);
        asignar(otro, ajena);
        crearEvaluacion(crearRa(ajena, "RA1"), actual, 20, EstadoEvaluacion.ACTIVA);
        crearEvaluacion(crearRa(sinDocente, "RA1"), actual, 20, EstadoEvaluacion.ACTIVA);

        assertThat(consultar(actual, admin()))
                .containsExactlyInAnyOrder(propia.getId(), ajena.getId(), sinDocente.getId());
        assertThat(consultar(actual, new UsuarioPrincipal(profesor.getId(), RolUsuario.PROFESOR)))
                .containsExactly(propia.getId());
    }

    @Test
    void semestreInexistenteDevuelveListaVacia() {
        Asignatura materia = crearAsignatura("VAC");
        asignar(crearProfesor("docente"), materia);
        Semestre inexistente = new Semestre();
        inexistente.setId(UUID.randomUUID());
        assertThat(consultar(inexistente, admin())).isEmpty();
    }
}
