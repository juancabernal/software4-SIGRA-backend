package co.edu.uco.sigra.programas.repository;

import co.edu.uco.sigra.programas.entity.ProgramaAcademico;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ProgramaAcademicoRepository extends JpaRepository<ProgramaAcademico, UUID> {

    boolean existsByNombreIgnoreCase(String nombre);

    boolean existsByNombreIgnoreCaseAndIdNot(String nombre, UUID id);

    boolean existsByCodigoIgnoreCase(String codigo);
}
