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
     * Desvincula (retira) una matrícula, dejándola en estado {@code DESVINCULADA} sin borrarla.
     * <p>
     * Si la matrícula no tiene calificaciones registradas, se desvincula sin más. Si tiene
     * alguna y {@code confirmado} es {@code false}, se rechaza con
     * {@link co.edu.uco.sigra.estudiantes.exception.ConfirmacionRequeridaException} y la
     * matrícula queda intacta. Si tiene alguna y {@code confirmado} es {@code true}, se
     * desvincula igual que en el primer caso: la confirmación no exime de las demás reglas
     * (la matrícula debe existir y estar activa).
     *
     * @param matriculaId identificador de la matrícula a desvincular
     * @param confirmado  {@code true} si el cliente ya confirmó la desvinculación a pesar de
     *                    que la matrícula tenga calificaciones registradas
     */
    void desvincular(UUID matriculaId, boolean confirmado);

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
