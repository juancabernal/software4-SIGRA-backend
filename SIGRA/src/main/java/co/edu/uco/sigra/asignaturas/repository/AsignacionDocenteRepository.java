package co.edu.uco.sigra.asignaturas.repository;

import co.edu.uco.sigra.asignaturas.entity.AsignacionDocente;
import co.edu.uco.sigra.common.enums.EstadoRegistro;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface AsignacionDocenteRepository extends JpaRepository <AsignacionDocente, UUID> {
    boolean existsByProfesor_IdAndEstado(UUID profesorId, EstadoRegistro estado);
}