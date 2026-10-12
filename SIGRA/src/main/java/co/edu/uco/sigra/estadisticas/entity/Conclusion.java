package co.edu.uco.sigra.estadisticas.entity;

import co.edu.uco.sigra.resultadosaprendizaje.entity.ResultadoAprendizaje;
import co.edu.uco.sigra.semestres.entity.Semestre;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Alerta automática de desempeño insuficiente en un (RA, semestre) — RF-16.
 * <p>
 * Se evalúa por (RA, semestre), nunca de forma acumulada histórica (contexto del ciclo de negocio).
 * Nace en estado VIGENTE; el módulo de mejoras (RF-17) la pasa a ATENDIDA al registrar una acción de
 * mejora sobre ella, y no vuelve a VIGENTE. Si el mismo RA vuelve a superar el umbral en un semestre
 * posterior, se crea una Conclusion nueva: por eso no hay una restricción de unicidad en base de
 * datos sobre (RA, semestre), solo la regla de negocio de no duplicar la que está VIGENTE.
 */
@Entity
@Table(name = "conclusion")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Conclusion {

    public static final String MENSAJE_INSUFICIENTE =
            "Nivel de logro insuficiente, se recomienda acción de mejora";

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "resultado_aprendizaje_id", nullable = false, updatable = false)
    private ResultadoAprendizaje resultadoAprendizaje;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "semestre_id", nullable = false, updatable = false)
    private Semestre semestre;

    @Column(nullable = false, length = 200, updatable = false)
    private String mensaje;

    @Column(name = "tiene_datos_suficientes", nullable = false, updatable = false)
    private boolean tieneDatosSuficientes;

    @Column(name = "fecha_generacion", nullable = false, updatable = false)
    private LocalDateTime fechaGeneracion;

    @Column(name = "porcentaje_insuficiente", nullable = false, updatable = false)
    private double porcentajeInsuficiente;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoConclusion estado;

    /** Solo nace así: VIGENTE, con el mensaje fijo de RF-16, la fecha y el porcentaje que la originaron. */
    public Conclusion(ResultadoAprendizaje resultadoAprendizaje, Semestre semestre, double porcentajeInsuficiente) {
        this.resultadoAprendizaje = resultadoAprendizaje;
        this.semestre = semestre;
        this.mensaje = MENSAJE_INSUFICIENTE;
        this.tieneDatosSuficientes = true;
        this.fechaGeneracion = LocalDateTime.now();
        this.porcentajeInsuficiente = porcentajeInsuficiente;
        this.estado = EstadoConclusion.VIGENTE;
    }

    public UUID getId() {
        return id;
    }

    public ResultadoAprendizaje getResultadoAprendizaje() {
        return resultadoAprendizaje;
    }

    public Semestre getSemestre() {
        return semestre;
    }

    public String getMensaje() {
        return mensaje;
    }

    public boolean isTieneDatosSuficientes() {
        return tieneDatosSuficientes;
    }

    public LocalDateTime getFechaGeneracion() {
        return fechaGeneracion;
    }

    public double getPorcentajeInsuficiente() {
        return porcentajeInsuficiente;
    }

    public EstadoConclusion getEstado() {
        return estado;
    }
}
