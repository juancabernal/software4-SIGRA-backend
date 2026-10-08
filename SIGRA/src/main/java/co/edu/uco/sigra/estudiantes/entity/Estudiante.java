package co.edu.uco.sigra.estudiantes.entity;

import co.edu.uco.sigra.auth.entity.Usuario;
import co.edu.uco.sigra.common.enums.RolUsuario;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "estudiante")
@PrimaryKeyJoinColumn(name = "id")
@Getter
@Setter
public class Estudiante extends Usuario {

    @Column(name = "numero_documento", nullable = false, length = 10)
    private String numeroDocumento;

    @Override
    public RolUsuario getRol() {
        return RolUsuario.ESTUDIANTE;
    }
}
