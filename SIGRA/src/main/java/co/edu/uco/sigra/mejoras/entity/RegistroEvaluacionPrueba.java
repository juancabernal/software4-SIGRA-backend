package co.edu.uco.sigra.mejoras.entity;

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

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Evaluación generada al convertir una acción de mejora (RF-18), insertada como DATO DE PRUEBA.
 * <p>
 * El módulo {@code evaluaciones} todavía no existe (solo tiene el {@code .gitkeep}); mientras tanto,
 * esta tabla hace de evaluación real solo para poder validar la regla del 100% de porcentaje por
 * (RA, semestre). Se retira cuando el equipo de evaluaciones entregue el módulo real.
 */
@Entity
@Table(name = "registro_evaluacion_prueba")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RegistroEvaluacionPrueba {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "resultado_aprendizaje_id", nullable = false, updatable = false)
    private ResultadoAprendizaje resultadoAprendizaje;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "semestre_id", nullable = false, updatable = false)
    private Semestre semestre;

    @Column(nullable = false, length = 500)
    private String descripcion;

    @Column(nullable = false)
    private double porcentaje;

    @Column(name = "fecha_generacion", nullable = false, updatable = false)
    private LocalDateTime fechaGeneracion;

    public RegistroEvaluacionPrueba(ResultadoAprendizaje resultadoAprendizaje, Semestre semestre,
                                     String descripcion, double porcentaje) {
        this.resultadoAprendizaje = resultadoAprendizaje;
        this.semestre = semestre;
        this.descripcion = descripcion;
        this.porcentaje = porcentaje;
        this.fechaGeneracion = LocalDateTime.now();
    }
}
