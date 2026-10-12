package co.edu.uco.sigra.mejoras.repository;

import co.edu.uco.sigra.mejoras.entity.AccionMejora;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface AccionMejoraRepository extends JpaRepository<AccionMejora, UUID> {
}
