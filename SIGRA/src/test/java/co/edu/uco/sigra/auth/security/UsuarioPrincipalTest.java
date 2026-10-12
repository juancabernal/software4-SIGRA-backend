package co.edu.uco.sigra.auth.security;

import co.edu.uco.sigra.common.enums.RolUsuario;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class UsuarioPrincipalTest {

    @Test
    void authenticationGetNameUsaElUuidYNoElToStringDelRecord() {
        UUID id = UUID.randomUUID();
        UsuarioPrincipal principal = new UsuarioPrincipal(id, "usuario@uco.net.co", RolUsuario.PROFESOR);

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(principal, null);

        assertThat(principal).isInstanceOf(java.security.Principal.class);
        assertThat(authentication.getName()).isEqualTo(id.toString());
    }
}
