# Diseño de autenticación

- POST /api/v1/auth/login normaliza el correo y valida el DTO.
- BCrypt factor 12 verifica la contraseña.
- JwtAuthenticationFilter valida cada Bearer y consulta nuevamente el usuario.
- UsuarioPrincipal se crea desde el usuario persistido.

## Topología del filtro

- `JwtAuthenticationFilter` es un bean de Spring Security y se inserta una sola vez con `addFilterBefore(..., UsernamePasswordAuthenticationFilter.class)`.
- Su `FilterRegistrationBean` servlet está deshabilitado para evitar doble ejecución.
- Solo el parsing/validación del Bearer captura excepciones JWT. `filterChain.doFilter` queda fuera de ese bloque; los fallos de BD, negocio o controladores conservan su semántica.
- Login, health GET y OPTIONS se excluyen del parsing JWT; todas las demás solicitudes con un `Authorization` defectuoso se rechazan como 401.
- El contexto se limpia antes y después de cada request.

## Política HTTP

- STATELESS; sin sesión de servidor.
- CSRF deshabilitado porque la API usa Bearer en encabezado y no cookies.
- CORS se configura en una única CorsConfigurationSource.
- 401 para ausencia o invalidez; 403 para rol o alcance insuficiente; 423 para bloqueo.
