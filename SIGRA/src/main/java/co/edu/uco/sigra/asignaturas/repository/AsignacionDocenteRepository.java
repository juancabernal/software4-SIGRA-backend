package co.edu.uco.sigra.asignaturas.repository;

import co.edu.uco.sigra.asignaturas.entity.AsignacionDocente;
import co.edu.uco.sigra.common.enums.EstadoRegistro;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AsignacionDocenteRepository extends JpaRepository <AsignacionDocente, UUID> {
    boolean existsByProfesor_IdAndEstado(UUID profesorId, EstadoRegistro estado);
    boolean existsByProfesor_IdAndAsignatura_IdAndEstado(UUID profesorId, UUID asignaturaId, EstadoRegistro estado);
    List<AsignacionDocente> findByProfesor_IdAndEstado(UUID profesorId, EstadoRegistro estado);
    List<AsignacionDocente> findByProfesor_Id(UUID profesorId);
    /** Para detectar duplicados y reactivar una asignación ya existente en vez de crear otra fila. */
    Optional<AsignacionDocente> findByProfesor_IdAndAsignatura_Id(UUID profesorId, UUID asignaturaId);
}
