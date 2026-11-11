package co.edu.uco.sigra.facultades.entity;

import co.edu.uco.sigra.common.enums.EstadoRegistro;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

/** Facultad a la que pertenecen los programas académicos (incidencia #0000036). */
@Entity
@Table(name = "facultad",
        uniqueConstraints = @UniqueConstraint(name = "uk_facultad_nombre", columnNames = "nombre"))
@Getter
@Setter
public class Facultad {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 150)
    private String nombre;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoRegistro estado = EstadoRegistro.ACTIVO;
}
