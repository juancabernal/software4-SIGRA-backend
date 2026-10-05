package co.edu.uco.sigra.auth.repository;

import co.edu.uco.sigra.auth.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, UUID> {

    Optional<Usuario> findByCorreoInstitucionalIgnoreCase(String correoInstitucional);

    // El correo es único entre TODOS los subtipos de Usuario (p. ej. Profesor), no solo dentro de uno.
    boolean existsByCorreoInstitucionalIgnoreCase(String correoInstitucional);

    boolean existsByCorreoInstitucionalIgnoreCaseAndIdNot(String correoInstitucional, UUID id);
}
