package co.edu.uco.sigra.resultadosaprendizaje.entity;

import co.edu.uco.sigra.asignaturas.entity.Asignatura;
import co.edu.uco.sigra.common.enums.EstadoRegistro;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;


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

    @Column(name = "inactivado_con_asignatura", nullable = false)
    private boolean inactivadoConAsignatura = false;
}
