package co.edu.uco.sigra.auth.service;

import co.edu.uco.sigra.auth.dto.LoginRequestDTO;
import co.edu.uco.sigra.auth.dto.LoginResponseDTO;

public interface AuthService {
    LoginResponseDTO autenticar(LoginRequestDTO request);
}
