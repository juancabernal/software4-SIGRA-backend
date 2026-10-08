package co.edu.uco.sigra.profesores.entity;

import co.edu.uco.sigra.auth.entity.Usuario;
import co.edu.uco.sigra.common.enums.RolUsuario;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "profesor")
@PrimaryKeyJoinColumn(name = "id")
@Getter
@Setter
public class Profesor extends Usuario {

    @Column(name = "numero_documento", nullable = false, length = 10)
    private String numeroDocumento;

    @Override
    public RolUsuario getRol() {
        return RolUsuario.PROFESOR;
    }
}
