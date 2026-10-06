package co.edu.uco.sigra.auth.dto;

public record LoginResponseDTO(
        String token,
        String tipo,
        long expiraEn,
        UsuarioSesionDTO usuario
) {
}
