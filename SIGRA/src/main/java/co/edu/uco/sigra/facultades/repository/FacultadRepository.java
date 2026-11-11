package co.edu.uco.sigra.facultades.repository;

import co.edu.uco.sigra.facultades.entity.Facultad;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface FacultadRepository extends JpaRepository<Facultad, UUID> {

    boolean existsByNombreIgnoreCase(String nombre);

    boolean existsByNombreIgnoreCaseAndIdNot(String nombre, UUID id);
}
