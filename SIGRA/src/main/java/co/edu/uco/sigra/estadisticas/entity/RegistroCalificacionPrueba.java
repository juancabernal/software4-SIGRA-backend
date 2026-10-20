package co.edu.uco.sigra.estadisticas.entity;

import co.edu.uco.sigra.estudiantes.entity.Estudiante;
import co.edu.uco.sigra.resultadosaprendizaje.entity.ResultadoAprendizaje;
import co.edu.uco.sigra.semestres.entity.Semestre;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Calificación de un estudiante en una evaluación de un (RA, semestre), insertada como DATO DE PRUEBA.
 * <p>
 * Los módulos {@code evaluaciones} (RF-10/RF-11) y {@code calificaciones} (RF-12/RF-13) todavía no
 * existen (solo tienen los {@code .gitkeep}). Mientras tanto, {@code FuenteCalificacionesPruebaImpl}
 * lee esta tabla para poder calcular el nivel de logro (RF-14/RF-15). Cada fila trae la nota (0.0 – 5.0)
 * y el porcentaje de la evaluación a la que pertenece, que es lo único que necesita el promedio ponderado.
 * <p>
 * Pendiente de integrar: cuando existan las evaluaciones y calificaciones reales, se agrega una nueva
 * implementación de {@link co.edu.uco.sigra.estadisticas.service.FuenteCalificaciones} que las consulte,
 * y esta entidad, su repositorio y {@code FuenteCalificacionesPruebaImpl} se retiran. El cálculo
 * ({@code NivelLogroServiceImpl}) no cambia.
 */
@Entity
@Table(name = "registro_calificacion_prueba")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RegistroCalificacionPrueba {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "resultado_aprendizaje_id", nullable = false)
    private ResultadoAprendizaje resultadoAprendizaje;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "semestre_id", nullable = false)
    private Semestre semestre;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "estudiante_id", nullable = false)
    private Estudiante estudiante;

    /** Nota de 0.0 a 5.0. */
    @Column(nullable = false)
    private double nota;

    /** Porcentaje (mayor que 0) de la evaluación a la que pertenece la calificación. */
    @Column(name = "porcentaje_evaluacion", nullable = false)
    private double porcentajeEvaluacion;

    public RegistroCalificacionPrueba(ResultadoAprendizaje resultadoAprendizaje, Semestre semestre,
                                      Estudiante estudiante, double nota, double porcentajeEvaluacion) {
        this.resultadoAprendizaje = resultadoAprendizaje;
        this.semestre = semestre;
        this.estudiante = estudiante;
        this.nota = nota;
        this.porcentajeEvaluacion = porcentajeEvaluacion;
    }
}
