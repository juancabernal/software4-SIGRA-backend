package co.edu.uco.sigra.profesores.repository;

import co.edu.uco.sigra.profesores.entity.Profesor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ProfesorRepository extends JpaRepository<Profesor, UUID> {
    public boolean existsByTipoDocumentoIdAndNumeroDocumento(UUID tipoDocumentoId, String numeroDocumento);
    public Optional<Profesor> findByCorreoInstitucional(String correo);
    boolean existsByTipoDocumento_IdAndNumeroDocumentoAndIdNot(
            UUID tipoDocumentoId, String numeroDocumento, UUID id);
    boolean existsByCorreoInstitucionalAndIdNot(String correoInstitucional, UUID id);
}
