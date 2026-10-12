package co.edu.uco.sigra.profesores.repository;

import co.edu.uco.sigra.profesores.entity.Profesor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ProfesorRepository extends JpaRepository<Profesor, UUID> {

    boolean existsByTipoDocumento_IdAndNumeroDocumento(UUID tipoDocumentoId, String numeroDocumento);

    boolean existsByTipoDocumento_IdAndNumeroDocumentoAndIdNot(UUID tipoDocumentoId, String numeroDocumento, UUID id);
}
