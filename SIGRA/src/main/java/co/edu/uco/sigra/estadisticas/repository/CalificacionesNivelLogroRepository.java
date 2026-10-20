package co.edu.uco.sigra.estadisticas.repository;

import co.edu.uco.sigra.estadisticas.entity.NivelLogroEstudiante;
import co.edu.uco.sigra.estudiantes.entity.EstadoMatricula;
import co.edu.uco.sigra.evaluaciones.entity.EstadoEvaluacion;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Lecturas que el nivel de logro (RF-14 / RF-15) necesita de los módulos de calificaciones, evaluaciones y
 * matrícula, escritas en JPQL con los nombres de sus entidades, igual que
 * {@code EvaluacionRepository#contarCalificacionesPorEvaluacion}. Vive en este módulo para no modificar los
 * repositorios de los demás ni depender de sus servicios (y así evitar ciclos entre beans).
 * <p>
 * Extiende {@link Repository} (sin CRUD): no se usa para escribir {@link NivelLogroEstudiante}.
 */
@org.springframework.stereotype.Repository
public interface CalificacionesNivelLogroRepository extends Repository<NivelLogroEstudiante, UUID> {

    /**
     * Calificaciones que cuentan para el nivel de logro de un (RA, semestre): solo de evaluaciones del RA y del
     * semestre pedidos, en estado ACTIVA y con porcentaje mayor que 0, y solo de matrículas ACTIVA.
     *
     * @return filas {@code [estudianteId (UUID), nota (BigDecimal), porcentajeEvaluacion (BigDecimal)]}
     */
    @Query("select c.matricula.estudiante.id, c.nota, c.evaluacion.porcentaje from Calificacion c "
            + "where c.evaluacion.resultadoAprendizaje.id = :raId and c.evaluacion.semestre.id = :semestreId "
            + "and c.evaluacion.estado = :estadoEvaluacion and c.evaluacion.porcentaje > 0 "
            + "and c.matricula.estado = :estadoMatricula")
    List<Object[]> buscarCalificacionesQueCuentan(@Param("raId") UUID resultadoAprendizajeId,
                                                  @Param("semestreId") UUID semestreId,
                                                  @Param("estadoEvaluacion") EstadoEvaluacion estadoEvaluacion,
                                                  @Param("estadoMatricula") EstadoMatricula estadoMatricula);

    /**
     * Pares (RA, semestre) con al menos una evaluación (en cualquier estado) en un semestre cuyo rango de fechas
     * contiene {@code hoy}. Se incluyen las evaluaciones INACTIVA a propósito: si un RA se queda sin evaluaciones
     * activas, el recálculo debe poner en 0 a quienes tenían nivel.
     *
     * @return filas {@code [resultadoAprendizajeId (UUID), semestreId (UUID)]}
     */
    @Query("select distinct e.resultadoAprendizaje.id, e.semestre.id from Evaluacion e "
            + "where e.semestre.fechaInicio <= :hoy and e.semestre.fechaFin >= :hoy")
    List<Object[]> buscarParesConEvaluacionesEnSemestreEnCurso(@Param("hoy") LocalDate hoy);
}
