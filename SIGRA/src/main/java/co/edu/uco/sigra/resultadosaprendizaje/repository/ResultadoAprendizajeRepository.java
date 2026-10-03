package co.edu.uco.sigra.resultadosaprendizaje.repository;

import co.edu.uco.sigra.resultadosaprendizaje.entity.ResultadoAprendizaje;
import co.edu.uco.sigra.shared.enums.EstadoRegistro;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ResultadoAprendizajeRepository extends JpaRepository<ResultadoAprendizaje, UUID> {
    long countByAsignatura_IdAndEstado(UUID asignaturaId, EstadoRegistro estado);
}
