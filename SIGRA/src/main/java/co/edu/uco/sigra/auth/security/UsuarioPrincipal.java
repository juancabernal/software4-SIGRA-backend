package co.edu.uco.sigra.auth.security;

import co.edu.uco.sigra.common.enums.RolUsuario;

import java.security.Principal;
import java.util.UUID;

/** Identidad validada por JWT y contrastada con el usuario persistido. */
public record UsuarioPrincipal(UUID id, String correoInstitucional, RolUsuario rol) implements Principal {

    public UsuarioPrincipal(UUID id, RolUsuario rol) {
        this(id, null, rol);
    }

    @Override
    public String getName() {
        return id.toString();
    }
}
