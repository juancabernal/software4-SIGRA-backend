package co.edu.uco.sigra.estudiantes.service;

import co.edu.uco.sigra.estudiantes.dto.EstudianteRequestDTO;
import co.edu.uco.sigra.estudiantes.dto.EstudianteResponseDTO;
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
     * Registra un estudiante nuevo y lo matricula en una asignatura y semestre, ambas cosas en
     * una sola operación atómica (RF-09a, design.md D6).
     * <p>
     * Vive aquí y no en {@code EstudianteService} porque la razón de ser de la operación es el
     * vínculo: lo que se pide es una matrícula que, de paso, crea al estudiante que todavía no
     * existe. Si el vínculo falla, no debe quedar nada. Ponerla en {@code EstudianteService}
     * invertiría la dependencia natural —el servicio de la ficha tendría que depender del de
     * matrícula para su caso principal— y el registro dejaría de poder usarse solo.
     * <p>
     * No reimplementa ninguna validación: delega el registro en
     * {@link EstudianteService#registrar(EstudianteRequestDTO)}, que ya aplica el formato, la
     * unicidad de documento y de correo, y delega la matrícula en {@link #matricular}, que ya
     * aplica las reglas de asignatura y semestre. El orden importa: primero se registra el
     * estudiante y después se matricula, de modo que un fallo de matrícula (asignatura o
     * semestre inexistentes, no activos, o la terna ya matriculada) deshaga también el alta del
     * estudiante. Un único {@code @Transactional} en la implementación basta para esa
     * atomicidad, porque ambas escrituras comparten el mismo {@code EntityManager} y toda
     * excepción de negocio del módulo es {@code RuntimeException}, así que marca la transacción
     * entera para rollback.
     */
    ResultadoAltaConMatricula registrarYMatricular(EstudianteRequestDTO estudianteDTO, UUID asignaturaId, UUID semestreId);

    /**
     * Resultado de {@link #matricular}: la matrícula resultante y si fue creada de cero o
     * reactivada a partir de una matrícula retirada de la misma terna.
     */
    record ResultadoMatricula(Matricula matricula, boolean reactivada) {
    }

    /**
     * Resultado de {@link #registrarYMatricular}: el estudiante recién registrado y la
     * matrícula creada para él. El controlador construye el {@code MatriculaResponseDTO} con el
     * {@code MatriculaMapper}, igual que ya hace en {@link #matricular}.
     */
    record ResultadoAltaConMatricula(EstudianteResponseDTO estudiante, Matricula matricula) {
    }
}
