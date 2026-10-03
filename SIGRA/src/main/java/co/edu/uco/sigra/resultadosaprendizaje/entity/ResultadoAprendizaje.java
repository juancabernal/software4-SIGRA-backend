package co.edu.uco.sigra.resultadosaprendizaje.entity;

import co.edu.uco.sigra.asignaturas.entity.Asignatura;
import co.edu.uco.sigra.shared.enums.EstadoRegistro;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

/**
 * Contrato mínimo del resultado de aprendizaje (RF-06). El módulo de resultados de aprendizaje
 * lo completará sin cambiar los nombres ni los tipos de estos campos.
 */
@Entity
@Table(name = "resultado_aprendizaje",
        uniqueConstraints = @UniqueConstraint(name = "uk_ra_asignatura_codigo",
                columnNames = {"asignatura_id", "codigo"}))
@Getter
@Setter
public class ResultadoAprendizaje {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "asignatura_id", nullable = false)
    private Asignatura asignatura;

    @Column(nullable = false, length = 20)
    private String codigo;

    @Column(nullable = false, length = 2000)
    private String descripcion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoRegistro estado = EstadoRegistro.ACTIVO;
}
