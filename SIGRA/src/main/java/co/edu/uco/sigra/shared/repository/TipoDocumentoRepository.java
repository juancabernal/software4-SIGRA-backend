package co.edu.uco.sigra.shared.repository;

import co.edu.uco.sigra.shared.entity.TipoDocumento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface TipoDocumentoRepository extends JpaRepository<TipoDocumento, UUID> {
}
