package co.edu.uco.sigra.calificaciones.entity;

import co.edu.uco.sigra.common.enums.RolUsuario;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Immutable
@Table(name = "historial_calificacion")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class HistorialCalificacion {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "calificacion_id", nullable = false, updatable = false)
    private UUID calificacionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_evento", nullable = false, length = 20, updatable = false)
    private TipoEventoCalificacion tipoEvento;

    @Column(name = "nota_anterior", precision = 2, scale = 1, updatable = false)
    private BigDecimal notaAnterior;

    @Column(name = "nota_nueva", nullable = false, precision = 2, scale = 1, updatable = false)
    private BigDecimal notaNueva;

    @Column(name = "observacion_anterior", length = 500, updatable = false)
    private String observacionAnterior;

    @Column(name = "observacion_nueva", nullable = false, length = 500, updatable = false)
    private String observacionNueva;

    @Column(name = "fuera_de_fecha", nullable = false, updatable = false)
    private boolean fueraDeFecha;

    @Column(name = "usuario_id", nullable = false, updatable = false)
    private UUID usuarioId;

    @Enumerated(EnumType.STRING)
    @Column(name = "rol_usuario", nullable = false, length = 20, updatable = false)
    private RolUsuario rolUsuario;

    @Column(name = "fecha_evento", nullable = false, updatable = false)
    private LocalDateTime fechaEvento;

    private HistorialCalificacion(UUID calificacionId, TipoEventoCalificacion tipoEvento,
                                  BigDecimal notaAnterior, BigDecimal notaNueva,
                                  String observacionAnterior, String observacionNueva,
                                  boolean fueraDeFecha, UUID usuarioId, RolUsuario rolUsuario,
                                  LocalDateTime fechaEvento) {
        this.calificacionId = calificacionId;
        this.tipoEvento = tipoEvento;
        this.notaAnterior = notaAnterior;
        this.notaNueva = notaNueva;
        this.observacionAnterior = observacionAnterior;
        this.observacionNueva = observacionNueva;
        this.fueraDeFecha = fueraDeFecha;
        this.usuarioId = usuarioId;
        this.rolUsuario = rolUsuario;
        this.fechaEvento = fechaEvento;
    }

    public static HistorialCalificacion registro(Calificacion calificacion, UUID usuarioId, RolUsuario rol,
                                                 boolean fueraDeFecha, LocalDateTime fecha) {
        return new HistorialCalificacion(calificacion.getId(), TipoEventoCalificacion.REGISTRO,
                null, calificacion.getNota(), null, calificacion.getObservacion(),
                fueraDeFecha, usuarioId, rol, fecha);
    }

    public static HistorialCalificacion modificacion(UUID calificacionId, BigDecimal notaAnterior,
                                                     String observacionAnterior, BigDecimal notaNueva,
                                                     String observacionNueva, UUID usuarioId, RolUsuario rol,
                                                     boolean fueraDeFecha, LocalDateTime fecha) {
        return new HistorialCalificacion(calificacionId, TipoEventoCalificacion.MODIFICACION,
                notaAnterior, notaNueva, observacionAnterior, observacionNueva,
                fueraDeFecha, usuarioId, rol, fecha);
    }
}