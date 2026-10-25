package co.edu.uco.sigra.auth.controller;

import co.edu.uco.sigra.auth.dto.LoginRequestDTO;
import co.edu.uco.sigra.auth.dto.LoginResponseDTO;
import co.edu.uco.sigra.auth.service.AuthService;
import co.edu.uco.sigra.common.openapi.EjemplosError;
import co.edu.uco.sigra.common.openapi.RespuestaErrorDoc;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Autenticación", description = "Endpoints de autenticación y emisión de tokens de sesión (RF-02)")
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(
            summary = "Iniciar sesión y obtener token JWT",
            description = "Autentica un usuario con su correo institucional y contraseña. Valida la estructura de la solicitud, "
                    + "normaliza el correo, comprueba la contraseña mediante BCrypt, verifica que el usuario esté ACTIVO "
                    + "y que no se encuentre bloqueado temporalmente. Si las credenciales son válidas, genera un JWT "
                    + "firmado y devuelve la información básica del usuario y su rol. El token permite utilizar los "
                    + "endpoints protegidos. Después de cuatro intentos fallidos se sigue respondiendo 401; el quinto "
                    + "intento incorrecto también responde 401 y activa un bloqueo de 15 minutos. Durante el bloqueo, "
                    + "incluso la contraseña correcta recibe 423. Un login correcto reinicia el contador. La respuesta "
                    + "no se almacena en caché y el cuerpo real del login está limitado a 16 KiB (RF-02/RF-03)."
    )
    @SecurityRequirements
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Autenticación exitosa y JWT generado",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = LoginResponseDTO.class),
                            examples = @ExampleObject(name = "loginExitoso", summary = "Sesión iniciada",
                                    value = "{\"token\":\"eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiI0YjY0b...firma-ficticia\",\"tipo\":\"Bearer\",\"expiraEn\":3600,\"usuario\":{\"id\":\"4b64b64b-4b64-4b64-8b64-4b64b64b64b6\",\"nombreCompleto\":\"Administradora de Prueba\",\"correoInstitucional\":\"administrador.prueba@uco.net.co\",\"rol\":\"ADMINISTRADOR\"}}"))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Datos del formulario inválidos: correo ausente, con formato distinto de @uco.net.co o de más de 254 caracteres; contraseña ausente o de más de 72 bytes UTF-8",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = RespuestaErrorDoc.class),
                            examples = @ExampleObject(name = "validacion", summary = "Validación del correo",
                                    value = EjemplosError.VALIDACION_LOGIN_400))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Credenciales incorrectas o usuario inactivo",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = RespuestaErrorDoc.class),
                            examples = @ExampleObject(name = "credencialesInvalidas", summary = "Credenciales rechazadas",
                                    value = EjemplosError.CREDENCIALES_INVALIDAS_401))
            ),
            @ApiResponse(
                    responseCode = "423",
                    description = "Cuenta temporalmente bloqueada durante 15 minutos después de cinco intentos fallidos; la contraseña no se evalúa mientras dura el bloqueo",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = RespuestaErrorDoc.class),
                            examples = @ExampleObject(name = "cuentaBloqueada", summary = "Bloqueo temporal",
                                    value = EjemplosError.USUARIO_BLOQUEADO_423))
            ),
            @ApiResponse(
                    responseCode = "413",
                    description = "El cuerpo real del login supera el límite de 16 KiB",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = RespuestaErrorDoc.class),
                            examples = @ExampleObject(name = "cuerpoDemasiadoGrande", summary = "Límite del cuerpo",
                                    value = EjemplosError.PAYLOAD_TOO_LARGE_413))
            ),
            @ApiResponse(
                    responseCode = "503",
                    description = "Indisponibilidad temporal del repositorio durante la autenticación",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = RespuestaErrorDoc.class),
                            examples = @ExampleObject(name = "servicioNoDisponible", summary = "Persistencia temporalmente no disponible",
                                    value = EjemplosError.SERVICIO_NO_DISPONIBLE_503))
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Fallo inesperado del servidor sin detalles técnicos",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = RespuestaErrorDoc.class),
                            examples = @ExampleObject(name = "errorInterno", summary = "Error inesperado",
                                    value = EjemplosError.ERROR_INTERNO_500))
            )
    })
    @PostMapping("/login")
    public ResponseEntity<LoginResponseDTO> login(
            @Valid
            @RequestBody
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Correo institucional y contraseña de la cuenta. No incluya tokens ni hashes.",
                    required = true,
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = LoginRequestDTO.class),
                            examples = @ExampleObject(name = "credencialesFicticias", summary = "Solicitud de ejemplo",
                                    value = "{\"correoInstitucional\":\"administrador.prueba@uco.net.co\",\"contrasena\":\"ClaveDePrueba2026!\"}")))
            LoginRequestDTO request) {
        LoginResponseDTO response = authService.autenticar(request);
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .header("Pragma", "no-cache")
                .body(response);
    }
}
