package co.edu.uco.sigra.calificaciones.entity;

import co.edu.uco.sigra.estudiantes.entity.Matricula;
import co.edu.uco.sigra.evaluaciones.entity.Evaluacion;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "calificacion",
        uniqueConstraints = @UniqueConstraint(name = "uk_calificacion_evaluacion_matricula",
                columnNames = {"evaluacion_id", "matricula_id"}))
@Getter
@Setter
public class Calificacion {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "evaluacion_id", nullable = false, updatable = false)
    private Evaluacion evaluacion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "matricula_id", nullable = false, updatable = false)
    private Matricula matricula;

    @Column(nullable = false, precision = 2, scale = 1)
    private BigDecimal nota;

    @Column(nullable = false, length = 500)
    private String observacion;

    @Column(name = "fecha_registro", nullable = false, updatable = false)
    private LocalDateTime fechaRegistro;

    @Column(name = "fecha_modificacion")
    private LocalDateTime fechaModificacion;

    @Column(name = "fuera_de_fecha", nullable = false)
    private boolean fueraDeFecha;
}