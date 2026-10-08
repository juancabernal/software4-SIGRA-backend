package co.edu.uco.sigra.auth.entity;

import co.edu.uco.sigra.common.enums.RolUsuario;
import jakarta.persistence.Entity;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;

@Entity
@Table(name = "administrador")
@PrimaryKeyJoinColumn(name = "id")
public class Administrador extends Usuario {

    @Override
    public RolUsuario getRol() {
        return RolUsuario.ADMINISTRADOR;
    }
}
