package co.edu.uco.sigra.estudiantes.entity;

import co.edu.uco.sigra.asignaturas.entity.Asignatura;
import co.edu.uco.sigra.common.enums.EstadoRegistro;
import co.edu.uco.sigra.semestres.entity.Semestre;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

/**
 * Vínculo entre un estudiante, una asignatura y un semestre (RF-08, RF-09).
 * <p>
 * La terna (estudiante, asignatura, semestre) es única sin importar el estado: desvincular
 * no borra el registro, lo pasa a {@link EstadoRegistro#INACTIVO}, y volver a matricular esa
 * misma terna reactiva el registro existente en vez de crear uno nuevo. Las relaciones hacia
 * {@link Asignatura} y {@link Semestre} son unidireccionales: esas entidades no conocen esta clase.
 */
@Entity
@Table(name = "matricula",
        uniqueConstraints = @UniqueConstraint(name = "uk_matricula_terna",
                columnNames = {"estudiante_id", "asignatura_id", "semestre_id"}))
@Getter
@Setter
public class Matricula {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "estudiante_id", nullable = false)
    private Estudiante estudiante;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "asignatura_id", nullable = false)
    private Asignatura asignatura;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "semestre_id", nullable = false)
    private Semestre semestre;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoRegistro estado = EstadoRegistro.ACTIVO;
}
