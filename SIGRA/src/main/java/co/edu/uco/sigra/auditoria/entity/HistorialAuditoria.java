package co.edu.uco.sigra.auditoria.entity;

import co.edu.uco.sigra.common.enums.RolUsuario;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Immutable
@Table(name = "historial_auditoria")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class HistorialAuditoria {

    public static final int DETALLE_MAX = 500;

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "fecha_hora", nullable = false, updatable = false)
    private LocalDateTime fechaHora;

    @Column(name = "usuario_id", updatable = false)
    private UUID usuarioId;

    @Enumerated(EnumType.STRING)
    @Column(name = "rol_usuario", length = 20, updatable = false)
    private RolUsuario rolUsuario;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40, updatable = false)
    private AccionAuditoria accion;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_entidad", nullable = false, length = 40, updatable = false)
    private TipoEntidadAuditoria tipoEntidad;

    @Column(name = "id_entidad", updatable = false)
    private UUID idEntidad;

    @Column(nullable = false, length = DETALLE_MAX, updatable = false)
    private String detalle;

    public HistorialAuditoria(LocalDateTime fechaHora, UUID usuarioId, RolUsuario rolUsuario,
                              AccionAuditoria accion, TipoEntidadAuditoria tipoEntidad, UUID idEntidad,
                              String detalle) {
        this.fechaHora = fechaHora;
        this.usuarioId = usuarioId;
        this.rolUsuario = rolUsuario;
        this.accion = accion;
        this.tipoEntidad = tipoEntidad;
        this.idEntidad = idEntidad;
        this.detalle = detalle;
    }
}