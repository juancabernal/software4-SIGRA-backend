package co.edu.uco.sigra.estudiantes.service;

import co.edu.uco.sigra.auth.security.UsuarioPrincipal;
import co.edu.uco.sigra.estudiantes.entity.Matricula;

import java.util.List;
import java.util.UUID;

public interface MatriculaService {

    /**
     * Matricula a un estudiante en una asignatura para un semestre, o reactiva la matrícula
     * existente de esa terna si estaba retirada (design.md, D3). El resultado indica si la
     * matrícula devuelta se creó o se reactivó, para que el controlador responda 201 o 200.
     */
    ResultadoMatricula matricular(UUID estudianteId, UUID asignaturaId, UUID semestreId);
    ResultadoMatricula matricular(UUID estudianteId, UUID asignaturaId, UUID semestreId, UsuarioPrincipal principal);

    /**
     * Desvincula (retira) una matrícula, dejándola en estado {@code INACTIVO} sin borrarla.
     */
    void desvincular(UUID matriculaId);
    void desvincular(UUID matriculaId, UsuarioPrincipal principal);

    /**
     * Lista las matrículas de una asignatura en un semestre. Si {@code incluirInactivas} es
     * {@code false}, solo trae las activas.
     */
    List<Matricula> listarMatriculados(UUID asignaturaId, UUID semestreId, boolean incluirInactivas);
    List<Matricula> listarMatriculados(UUID asignaturaId, UUID semestreId, boolean incluirInactivas,
                                       UsuarioPrincipal principal);

    /**
     * Lista el historial completo de matrículas de un estudiante (activas e inactivas).
     */
    List<Matricula> listarAsignaturasDeEstudiante(UUID estudianteId);
    List<Matricula> listarAsignaturasDeEstudiante(UUID estudianteId, UsuarioPrincipal principal);

    /**
     * Resultado de {@link #matricular}: la matrícula resultante y si fue creada de cero o
     * reactivada a partir de una matrícula retirada de la misma terna.
     */
    record ResultadoMatricula(Matricula matricula, boolean reactivada) {
    }
}
