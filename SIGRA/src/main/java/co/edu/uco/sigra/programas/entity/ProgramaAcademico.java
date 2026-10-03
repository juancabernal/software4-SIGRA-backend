package co.edu.uco.sigra.programas.entity;

import co.edu.uco.sigra.shared.enums.EstadoRegistro;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

/**
 * Contrato mínimo del programa académico (RF-02). El módulo de programas lo completará
 * sin cambiar los nombres ni los tipos de estos campos.
 */
@Entity
@Table(name = "programa_academico",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_programa_nombre", columnNames = "nombre"),
                @UniqueConstraint(name = "uk_programa_codigo", columnNames = "codigo")
        })
@Getter
@Setter
public class ProgramaAcademico {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(nullable = false, length = 10, updatable = false)
    private String codigo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoRegistro estado = EstadoRegistro.ACTIVO;
}
