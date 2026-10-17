package co.edu.uco.sigra.estadisticas.entity;

import co.edu.uco.sigra.estudiantes.entity.Estudiante;
import co.edu.uco.sigra.resultadosaprendizaje.entity.ResultadoAprendizaje;
import co.edu.uco.sigra.semestres.entity.Semestre;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Nivel de logro calculado de un estudiante en un (RA, semestre) — RF-14 / RF-15.
 * <p>
 * Es un dato derivado: lo escribe únicamente el recálculo de estadísticas a partir de las
 * calificaciones del estudiante en las evaluaciones del RA. Hay una fila por terna
 * (RA, semestre, estudiante), que se actualiza en cada recálculo.
 * <ul>
 *   <li>{@code cantidadCalificaciones >= 3}: tiene promedio ponderado y nivel de logro.</li>
 *   <li>{@code cantidadCalificaciones} entre 1 y 2: «sin datos suficientes para calcular»;
 *       promedio y nivel quedan en null.</li>
 *   <li>{@code cantidadCalificaciones = 0}: el estudiante tuvo calificaciones y ya no las tiene;
 *       la fila se conserva (nada se borra físicamente) con promedio y nivel en null.</li>
 * </ul>
 */
@Entity
@Table(name = "nivel_logro_estudiante",
        uniqueConstraints = @UniqueConstraint(name = "uk_nivel_logro_estudiante_terna",
                columnNames = {"resultado_aprendizaje_id", "semestre_id", "estudiante_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NivelLogroEstudiante {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "resultado_aprendizaje_id", nullable = false, updatable = false)
    private ResultadoAprendizaje resultadoAprendizaje;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "semestre_id", nullable = false, updatable = false)
    private Semestre semestre;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "estudiante_id", nullable = false, updatable = false)
    private Estudiante estudiante;

    @Column(name = "cantidad_calificaciones", nullable = false)
    private int cantidadCalificaciones;

    /** Promedio ponderado normalizado (0.0 – 5.0); null si no hay datos suficientes. */
    @Column(name = "promedio_ponderado")
    private Double promedioPonderado;

    /** Nivel según el catálogo de 4 rangos; null si no hay datos suficientes. */
    @Enumerated(EnumType.STRING)
    @Column(name = "nivel_logro", length = 20)
    private NivelLogro nivelLogro;

    @Column(name = "fecha_calculo", nullable = false)
    private LocalDateTime fechaCalculo;

    public NivelLogroEstudiante(ResultadoAprendizaje resultadoAprendizaje, Semestre semestre, Estudiante estudiante) {
        this.resultadoAprendizaje = resultadoAprendizaje;
        this.semestre = semestre;
        this.estudiante = estudiante;
    }

    /** Guarda el resultado de un recálculo. Promedio y nivel van juntos: ambos null o ambos con valor. */
    public void actualizar(int cantidadCalificaciones, Double promedioPonderado, NivelLogro nivelLogro,
                           LocalDateTime fechaCalculo) {
        if ((promedioPonderado == null) != (nivelLogro == null)) {
            throw new IllegalArgumentException("El promedio y el nivel de logro deben venir juntos.");
        }
        this.cantidadCalificaciones = cantidadCalificaciones;
        this.promedioPonderado = promedioPonderado;
        this.nivelLogro = nivelLogro;
        this.fechaCalculo = fechaCalculo;
    }

    public UUID getEstudianteId() {
        return estudiante.getId();
    }
}
