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
import org.springframework.http.CacheControl;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Tag(name = "Autenticación", description = "Endpoints de autenticación y emisión de tokens de sesión (RF-02)")
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(
            summary = "Iniciar sesión",
            description = "Autentica un usuario del sistema mediante sus credenciales institucionales y emite un JWT firmado con HS256 (RF-02). El cuerpo del login admite hasta 16 KiB reales."
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
                                    value = "{\"timestamp\":\"2026-10-04T12:00:00-05:00\",\"status\":400,\"error\":\"BAD_REQUEST\",\"mensaje\":\"Los datos de entrada no cumplen con las validaciones requeridas\",\"detalles\":[\"correoInstitucional: El correo institucional es obligatorio\"]}"
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Credenciales incorrectas o usuario inactivo",
                    content = @Content(
                            schema = @Schema(implementation = Map.class),
                            examples = @ExampleObject(
                                    value = "{\"timestamp\":\"2026-10-04T12:00:00-05:00\",\"status\":401,\"error\":\"UNAUTHORIZED\",\"mensaje\":\"Correo o contraseña incorrectos\"}"
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "423",
                    description = "Usuario temporalmente bloqueado tras 5 intentos fallidos",
                    content = @Content(
                            schema = @Schema(implementation = Map.class),
                            examples = @ExampleObject(
                                    value = "{\"timestamp\":\"2026-10-04T12:00:00-05:00\",\"status\":423,\"error\":\"LOCKED\",\"mensaje\":\"El usuario está temporalmente bloqueado. Intente nuevamente más tarde.\"}"
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "413",
                    description = "El cuerpo real del login supera el límite de 16 KiB",
                    content = @Content(
                            schema = @Schema(implementation = Map.class),
                            examples = @ExampleObject(
                                    value = "{\"timestamp\":\"2026-10-04T12:00:00-05:00\",\"status\":413,\"error\":\"PAYLOAD_TOO_LARGE\",\"mensaje\":\"El cuerpo de la petición supera el tamaño permitido\"}"
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "503",
                    description = "Indisponibilidad temporal del repositorio durante la autenticación",
                    content = @Content(
                            schema = @Schema(implementation = Map.class),
                            examples = @ExampleObject(
                                    value = "{\"timestamp\":\"2026-10-04T12:00:00-05:00\",\"status\":503,\"error\":\"SERVICE_UNAVAILABLE\",\"mensaje\":\"El servicio no está disponible temporalmente\"}"
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Fallo inesperado del servidor sin detalles técnicos",
                    content = @Content(
                            schema = @Schema(implementation = Map.class),
                            examples = @ExampleObject(
                                    value = "{\"timestamp\":\"2026-10-04T12:00:00-05:00\",\"status\":500,\"error\":\"INTERNAL_SERVER_ERROR\",\"mensaje\":\"Ocurrió un error inesperado en el servidor\"}"
                            )
                    )
            )
    })
    @PostMapping("/login")
    public ResponseEntity<LoginResponseDTO> login(@Valid @RequestBody LoginRequestDTO request) {
        LoginResponseDTO response = authService.autenticar(request);
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .header("Pragma", "no-cache")
                .body(response);
    }
}
