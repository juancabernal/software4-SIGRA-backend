package co.edu.uco.sigra.config;

import co.edu.uco.sigra.auth.security.JsonAccessDeniedHandler;
import co.edu.uco.sigra.auth.security.JsonAuthenticationEntryPoint;
import co.edu.uco.sigra.auth.security.JwtAuthenticationFilter;
import co.edu.uco.sigra.auth.security.RequestCorrelationFilter;
import co.edu.uco.sigra.auth.security.RequestSizeLimitFilter;
import tools.jackson.databind.json.JsonMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public JsonAuthenticationEntryPoint authenticationEntryPoint(JsonMapper objectMapper) {
        return new JsonAuthenticationEntryPoint(objectMapper);
    }

    @Bean
    public JsonAccessDeniedHandler accessDeniedHandler(JsonMapper objectMapper) {
        return new JsonAccessDeniedHandler(objectMapper);
    }

    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter(
            co.edu.uco.sigra.auth.service.JwtService jwtService,
            co.edu.uco.sigra.auth.repository.UsuarioRepository usuarioRepository,
            JsonAuthenticationEntryPoint authenticationEntryPoint,
            JsonMapper objectMapper) {
        return new JwtAuthenticationFilter(jwtService, usuarioRepository, authenticationEntryPoint, objectMapper);
    }

    @Bean
    public RequestCorrelationFilter requestCorrelationFilter() {
        return new RequestCorrelationFilter();
    }

    @Bean
    public RequestSizeLimitFilter requestSizeLimitFilter(
            JsonMapper objectMapper,
            @Value("${security.max-request-body-bytes:16384}") long maxRequestBodyBytes) {
        return new RequestSizeLimitFilter(objectMapper, maxRequestBodyBytes);
    }

    /**
     * El filtro es un bean para poder inyectarlo en la cadena, pero no debe registrarse como filtro
     * servlet global además de {@code addFilterBefore}. La cadena de Spring Security es su único
     * punto de ejecución.
     */
    @Bean
    public FilterRegistrationBean<JwtAuthenticationFilter> jwtAuthenticationFilterRegistration(
            JwtAuthenticationFilter filter) {
        FilterRegistrationBean<JwtAuthenticationFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }

    @Bean
    public FilterRegistrationBean<RequestCorrelationFilter> requestCorrelationFilterRegistration(
            RequestCorrelationFilter filter) {
        FilterRegistrationBean<RequestCorrelationFilter> registration = new FilterRegistrationBean<>(filter);
        // Debe ejecutarse antes del CorsFilter de Spring Security para que incluso un rechazo CORS
        // tenga un identificador correlacionable en los logs del servidor.
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return registration;
    }

    @Bean
    public FilterRegistrationBean<RequestSizeLimitFilter> requestSizeLimitFilterRegistration(
            RequestSizeLimitFilter filter) {
        FilterRegistrationBean<RequestSizeLimitFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http,
                                           ObjectProvider<JwtAuthenticationFilter> jwtAuthenticationFilter,
                                           ObjectProvider<RequestCorrelationFilter> requestCorrelationFilter,
                                           ObjectProvider<RequestSizeLimitFilter> requestSizeLimitFilter,
                                           JsonAuthenticationEntryPoint authenticationEntryPoint,
                                           JsonAccessDeniedHandler accessDeniedHandler,
                                           CorsConfigurationSource corsConfigurationSource) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .requestCache(AbstractHttpConfigurer::disable)
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/login").permitAll()
                        .requestMatchers(HttpMethod.GET, "/actuator/health").permitAll()
                        .requestMatchers(HttpMethod.GET, "/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs", "/v3/api-docs/**").permitAll()
                        .anyRequest().authenticated()
                )
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable);

        JwtAuthenticationFilter filter = jwtAuthenticationFilter.getIfAvailable();
        if (filter != null) {
            http.addFilterBefore(filter, UsernamePasswordAuthenticationFilter.class);
        }
        RequestSizeLimitFilter sizeLimitFilter = requestSizeLimitFilter.getIfAvailable();
        if (sizeLimitFilter != null) {
            http.addFilterBefore(sizeLimitFilter,
                    filter != null ? filter.getClass() : UsernamePasswordAuthenticationFilter.class);
        }
        // Conserva la ejecución en la cadena para contextos de prueba o despliegues que no
        // materialicen FilterRegistrationBean; OncePerRequestFilter evita una segunda ejecución
        // cuando el registro servlet ya asignó la correlación antes de CORS.
        RequestCorrelationFilter correlationFilter = requestCorrelationFilter.getIfAvailable();
        if (correlationFilter != null) {
            http.addFilterBefore(correlationFilter,
                    sizeLimitFilter != null ? sizeLimitFilter.getClass() :
                            (filter != null ? filter.getClass() : UsernamePasswordAuthenticationFilter.class));
        }
        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource(
            @Value("${cors.allowed-origins:http://localhost:4200}") String configuredOrigins) {
        List<String> origins = Arrays.stream(configuredOrigins.split(","))
                .map(String::trim)
                .filter(origin -> !origin.isBlank())
                .toList();
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(origins);
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept", "Origin"));
        configuration.setExposedHeaders(List.of("WWW-Authenticate", RequestCorrelationFilter.HEADER));
        configuration.setAllowCredentials(false);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
