package co.edu.uco.sigra.asignaturas.entity;

import co.edu.uco.sigra.asignaturas.exception.RangoRaInvalidoException;
import co.edu.uco.sigra.asignaturas.exception.TransicionEstadoInvalidaException;
import co.edu.uco.sigra.programas.entity.ProgramaAcademico;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Asignatura de un programa académico (RF-03).
 * <p>
 * El código y el programa son inmutables; solo el nombre puede modificarse. El ciclo de vida es
 * BORRADOR → ACTIVA → INACTIVA: activar exige entre {@value #MIN_RA_ACTIVOS} y {@value #MAX_RA_ACTIVOS}
 * resultados de aprendizaje activos. Al inactivar, la entidad solo cambia su propio estado; el servicio
 * inactiva en cascada los RA activos, y matrículas, evaluaciones y calificaciones no se tocan.
 * Una asignatura INACTIVA no se reactiva, pues el SRS solo define Borrador → Activa.
 */
@Entity
@Table(name = "asignatura",
        uniqueConstraints = @UniqueConstraint(name = "uk_asignatura_codigo", columnNames = "codigo"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Asignatura {

    public static final int MIN_RA_ACTIVOS = 5;
    public static final int MAX_RA_ACTIVOS = 7;

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 20, updatable = false)
    private String codigo;

    @Column(nullable = false, length = 100)
    private String nombre;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "programa_id", nullable = false, updatable = false)
    private ProgramaAcademico programa;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoAsignatura estado = EstadoAsignatura.BORRADOR;

    public Asignatura(String codigo, String nombre, ProgramaAcademico programa) {
        this.codigo = codigo;
        this.nombre = nombre;
        this.programa = programa;
        this.estado = EstadoAsignatura.BORRADOR;
    }

    public boolean tieneRangoValidoDeRA(long cantidadRaActivos) {
        return cantidadRaActivos >= MIN_RA_ACTIVOS && cantidadRaActivos <= MAX_RA_ACTIVOS;
    }

    public void activar(long cantidadRaActivos) {
        if (estado != EstadoAsignatura.BORRADOR) {
            throw new TransicionEstadoInvalidaException(estado, "activar",
                    "Solo se pueden activar asignaturas en estado BORRADOR.");
        }
        if (cantidadRaActivos < MIN_RA_ACTIVOS) {
            throw RangoRaInvalidoException.pocosRa(MIN_RA_ACTIVOS, cantidadRaActivos);
        }
        if (cantidadRaActivos > MAX_RA_ACTIVOS) {
            throw RangoRaInvalidoException.demasiadosRa(MAX_RA_ACTIVOS, cantidadRaActivos);
        }
        estado = EstadoAsignatura.ACTIVA;
    }

    public void inactivar() {
        if (estado != EstadoAsignatura.ACTIVA) {
            throw new TransicionEstadoInvalidaException(estado, "inactivar",
                    "Solo se pueden inactivar asignaturas en estado ACTIVA.");
        }
        estado = EstadoAsignatura.INACTIVA;
    }

    public void renombrar(String nuevoNombre) {
        this.nombre = nuevoNombre;
    }
}
