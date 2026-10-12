package co.edu.uco.sigra.mejoras.entity;

import co.edu.uco.sigra.estadisticas.entity.Conclusion;
import co.edu.uco.sigra.resultadosaprendizaje.entity.ResultadoAprendizaje;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Acción de mejora propuesta sobre una {@link Conclusion} (RF-17), convertible luego en evaluación (RF-18).
 */
@Entity
@Table(name = "accion_mejora")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AccionMejora {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 150)
    private String nombre;

    @Column(nullable = false, length = 500)
    private String descripcion;

    @Column(name = "fecha_propuesta", nullable = false, updatable = false)
    private LocalDateTime fechaPropuesta;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "resultado_aprendizaje_id", nullable = false, updatable = false)
    private ResultadoAprendizaje resultadoAprendizaje;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "conclusion_origen_id", nullable = false, updatable = false)
    private Conclusion conclusionOrigen;

    /** Id de la evaluación creada por el módulo evaluaciones (aún no existe): sin relación JPA ni FK a propósito. */
    @Column(name = "evaluacion_generada_id")
    private UUID evaluacionGeneradaId;

    public AccionMejora(String nombre, String descripcion, ResultadoAprendizaje resultadoAprendizaje,
                         Conclusion conclusionOrigen) {
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.resultadoAprendizaje = resultadoAprendizaje;
        this.conclusionOrigen = conclusionOrigen;
        this.fechaPropuesta = LocalDateTime.now();
    }

    public boolean estaConvertida() {
        return evaluacionGeneradaId != null;
    }
}
