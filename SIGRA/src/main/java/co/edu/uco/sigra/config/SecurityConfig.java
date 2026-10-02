package co.edu.uco.sigra.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity // Permite ignorar o manejar @PreAuthorize
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable) // Desactiva la protección CSRF
                .authorizeHttpRequests(auth -> auth
                        .anyRequest().permitAll() // PERMITE TODAS LAS PETICIONES SIN AUTENTICACIÓN
                )
                .formLogin(AbstractHttpConfigurer::disable) // Desactiva la pantalla de Login HTML
                .httpBasic(AbstractHttpConfigurer::disable);

        return http.build();
    }
}