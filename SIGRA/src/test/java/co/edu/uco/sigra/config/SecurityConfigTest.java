package co.edu.uco.sigra.config;

import co.edu.uco.sigra.auth.security.JwtAuthenticationFilter;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.servlet.FilterRegistrationBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class SecurityConfigTest {

    @Test
    void jwtSoloSeRegistraEnLaCadenaDeSpringSecurity() {
        JwtAuthenticationFilter filter = mock(JwtAuthenticationFilter.class);

        FilterRegistrationBean<JwtAuthenticationFilter> registration =
                new SecurityConfig().jwtAuthenticationFilterRegistration(filter);

        assertThat(registration.isEnabled()).isFalse();
        assertThat(registration.getFilter()).isSameAs(filter);
    }
}
