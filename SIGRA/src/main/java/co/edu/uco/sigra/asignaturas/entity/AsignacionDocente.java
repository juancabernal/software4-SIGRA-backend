package co.edu.uco.sigra.asignaturas.entity;

import co.edu.uco.sigra.profesores.entity.Profesor;
import co.edu.uco.sigra.common.enums.EstadoRegistro;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "asignacion_docente")
@NoArgsConstructor
@Getter
@Setter
public class AsignacionDocente {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "profesor_id", nullable = false)
    private Profesor profesor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "asignatura_id", nullable = false)
    private Asignatura asignatura;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoRegistro estado;
}