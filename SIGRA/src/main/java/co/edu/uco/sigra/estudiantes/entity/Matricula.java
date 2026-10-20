package co.edu.uco.sigra.estudiantes.entity;

import co.edu.uco.sigra.asignaturas.entity.Asignatura;
import co.edu.uco.sigra.semestres.entity.Semestre;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Vínculo entre un estudiante, una asignatura y un semestre (RF-08, RF-09).
 * <p>
 * La terna (estudiante, asignatura, semestre) es única sin importar el estado: desvincular
 * no borra el registro, lo pasa a {@link EstadoMatricula#DESVINCULADA}, y volver a matricular
 * esa misma terna reactiva el registro existente en vez de crear uno nuevo, de modo que las
 * calificaciones sigan colgando de la misma matrícula.
 * <p>
 * {@code fechaMatricula} es obligatoria y la fija el servicio con su reloj inyectado, nunca el
 * cliente. En una rematrícula se actualiza a la fecha en que el estudiante volvió a quedar
 * matriculado: es la fecha en que el vínculo vigente empezó.
 * <p>
 * Las relaciones hacia {@link Asignatura} y {@link Semestre} son unidireccionales: esas
 * entidades no conocen esta clase.
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

    @Column(name = "fecha_matricula", nullable = false)
    private LocalDate fechaMatricula;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoMatricula estado = EstadoMatricula.ACTIVA;
}
