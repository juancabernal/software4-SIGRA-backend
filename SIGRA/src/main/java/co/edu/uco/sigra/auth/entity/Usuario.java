package co.edu.uco.sigra.auth.entity;

import co.edu.uco.sigra.common.entity.TipoDocumento;
import co.edu.uco.sigra.common.enums.EstadoRegistro;
import co.edu.uco.sigra.common.enums.RolUsuario;
import co.edu.uco.sigra.common.util.Correos;
import co.edu.uco.sigra.common.util.NombresPersona;
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

    @Column(name = "primer_nombre", nullable = false, length = 50)
    private String primerNombre;

    @Column(name = "segundo_nombre", length = 50)
    private String segundoNombre;

    @Column(name = "primer_apellido", nullable = false, length = 50)
    private String primerApellido;

    @Column(name = "segundo_apellido", nullable = false, length = 50)
    private String segundoApellido;

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

    /** Nombre para mostrar (concatenación de las partes no vacías). No se persiste. */
    @Transient
    public String getNombreCompleto() {
        return NombresPersona.completo(primerNombre, segundoNombre, primerApellido, segundoApellido);
    }

    @PrePersist
    @PreUpdate
    void normalizarCamposUsuario() {
        this.correoInstitucional = Correos.normalizar(this.correoInstitucional);
        this.primerNombre = NombresPersona.blankToNull(this.primerNombre);
        this.segundoNombre = NombresPersona.blankToNull(this.segundoNombre);
        this.primerApellido = NombresPersona.blankToNull(this.primerApellido);
        this.segundoApellido = NombresPersona.blankToNull(this.segundoApellido);
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
