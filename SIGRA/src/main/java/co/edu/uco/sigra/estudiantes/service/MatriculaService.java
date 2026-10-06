package co.edu.uco.sigra.estudiantes.service;

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

    /**
     * Desvincula (retira) una matrícula, dejándola en estado {@code INACTIVO} sin borrarla.
     */
    void desvincular(UUID matriculaId);

    /**
     * Lista las matrículas de una asignatura en un semestre. Si {@code incluirInactivas} es
     * {@code false}, solo trae las activas.
     */
    List<Matricula> listarMatriculados(UUID asignaturaId, UUID semestreId, boolean incluirInactivas);

    /**
     * Lista el historial completo de matrículas de un estudiante (activas e inactivas).
     */
    List<Matricula> listarAsignaturasDeEstudiante(UUID estudianteId);

    /**
     * Resultado de {@link #matricular}: la matrícula resultante y si fue creada de cero o
     * reactivada a partir de una matrícula retirada de la misma terna.
     */
    record ResultadoMatricula(Matricula matricula, boolean reactivada) {
    }
}
