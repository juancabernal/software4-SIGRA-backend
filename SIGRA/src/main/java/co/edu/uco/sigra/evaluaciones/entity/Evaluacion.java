package co.edu.uco.sigra.evaluaciones.entity;

import co.edu.uco.sigra.resultadosaprendizaje.entity.ResultadoAprendizaje;
import co.edu.uco.sigra.semestres.entity.Semestre;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "evaluacion")
@Getter
@Setter
public class Evaluacion {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "resultado_aprendizaje_id", nullable = false, updatable = false)
    private ResultadoAprendizaje resultadoAprendizaje;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "semestre_id", nullable = false, updatable = false)
    private Semestre semestre;

    @Column(nullable = false, length = 150)
    private String nombre;

    @Column(name = "fecha_tentativa", nullable = false)
    private LocalDate fechaTentativa;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal porcentaje;

    @Column(nullable = false, length = 500)
    private String descripcion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private EstadoEvaluacion estado = EstadoEvaluacion.ACTIVA;
}