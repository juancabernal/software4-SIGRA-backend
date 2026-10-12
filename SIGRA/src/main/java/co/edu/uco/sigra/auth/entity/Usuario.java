package co.edu.uco.sigra.auth.entity;

import co.edu.uco.sigra.common.entity.TipoDocumento;
import co.edu.uco.sigra.common.enums.EstadoRegistro;
import co.edu.uco.sigra.common.enums.RolUsuario;
import co.edu.uco.sigra.common.util.Correos;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "usuario")
@Inheritance(strategy = InheritanceType.JOINED)
@Getter
@Setter
public abstract class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tipo_documento_id", nullable = false)
    private TipoDocumento tipoDocumento;

    @Column(name = "nombre_completo", nullable = false)
    private String nombreCompleto;

    @Column(name = "correo_institucional", unique = true, nullable = false)
    private String correoInstitucional;

    @Column(name = "password_hash")
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoRegistro estado = EstadoRegistro.ACTIVO;

    @Column(name = "intentos_fallidos", nullable = false)
    private int intentosFallidos = 0;

    @Column(name = "fecha_bloqueo")
    private LocalDateTime fechaBloqueo;

    @Transient
    public abstract RolUsuario getRol();

    // Garantiza que el correo se persista siempre normalizado, sin importar quién llame al repositorio.
    // No convierte un correo nulo: la restricción nullable = false sigue rechazándolo.
    @PrePersist
    @PreUpdate
    void normalizarCorreoInstitucional() {
        this.correoInstitucional = Correos.normalizar(this.correoInstitucional);
    }

    public void registrarIntentoFallido(LocalDateTime ahora, int maxIntentos) {
        this.intentosFallidos++;
        if (this.intentosFallidos >= maxIntentos) {
            this.fechaBloqueo = ahora;
        }
    }

    public void reiniciarContadorIntentos() {
        this.intentosFallidos = 0;
        this.fechaBloqueo = null;
    }

    public boolean estaBloqueado(LocalDateTime ahora, Duration duracionBloqueo) {
        if (this.fechaBloqueo == null) {
            return false;
        }
        return ahora.isBefore(this.fechaBloqueo.plus(duracionBloqueo));
    }
}
