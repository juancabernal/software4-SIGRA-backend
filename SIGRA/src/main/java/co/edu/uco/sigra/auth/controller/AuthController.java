package co.edu.uco.sigra.auth.controller;

import co.edu.uco.sigra.auth.dto.LoginRequestDTO;
import co.edu.uco.sigra.auth.dto.LoginResponseDTO;
import co.edu.uco.sigra.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Tag(name = "Autenticación", description = "Endpoints para inicio de sesión y emisión de tokens de sesión (RF-04)")
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(
            summary = "Iniciar sesión",
            description = "Autentica un usuario del sistema mediante sus credenciales institucionales y emite un JWT firmado con HS256."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Login exitoso",
                    content = @Content(schema = @Schema(implementation = LoginResponseDTO.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Datos de entrada inválidos",
                    content = @Content(
                            schema = @Schema(implementation = Map.class),
                            examples = @ExampleObject(
                                    value = "{\"timestamp\":\"2026-10-04T12:00:00\",\"status\":400,\"error\":\"BAD_REQUEST\",\"mensaje\":\"Los datos de entrada no cumplen con las validaciones requeridas\",\"detalles\":[\"correoInstitucional: El correo institucional es obligatorio\"]}"
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Credenciales incorrectas o usuario inactivo",
                    content = @Content(
                            schema = @Schema(implementation = Map.class),
                            examples = @ExampleObject(
                                    value = "{\"timestamp\":\"2026-10-04T12:00:00\",\"status\":401,\"error\":\"UNAUTHORIZED\",\"mensaje\":\"Correo o contraseña incorrectos\"}"
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "423",
                    description = "Usuario temporalmente bloqueado tras 5 intentos fallidos",
                    content = @Content(
                            schema = @Schema(implementation = Map.class),
                            examples = @ExampleObject(
                                    value = "{\"timestamp\":\"2026-10-04T12:00:00\",\"status\":423,\"error\":\"LOCKED\",\"mensaje\":\"El usuario está temporalmente bloqueado. Intente nuevamente más tarde.\"}"
                            )
                    )
            )
    })
    @PostMapping("/login")
    public ResponseEntity<LoginResponseDTO> login(@Valid @RequestBody LoginRequestDTO request) {
        LoginResponseDTO response = authService.autenticar(request);
        return ResponseEntity.ok(response);
    }
}
