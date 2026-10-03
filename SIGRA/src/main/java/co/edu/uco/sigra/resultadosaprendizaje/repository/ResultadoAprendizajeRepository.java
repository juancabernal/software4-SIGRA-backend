package co.edu.uco.sigra.resultadosaprendizaje.repository;

import co.edu.uco.sigra.resultadosaprendizaje.entity.ResultadoAprendizaje;
import co.edu.uco.sigra.shared.enums.EstadoRegistro;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ResultadoAprendizajeRepository extends JpaRepository<ResultadoAprendizaje, UUID> {


    long countByAsignatura_IdAndEstado(UUID asignaturaId, EstadoRegistro estado);


    boolean existsByAsignatura_IdAndCodigo(UUID asignaturaId, String codigo);


    List<ResultadoAprendizaje> findByAsignatura_IdOrderByCodigoAsc(UUID asignaturaId);


    List<ResultadoAprendizaje> findByAsignatura_IdAndEstadoOrderByCodigoAsc(UUID asignaturaId, EstadoRegistro estado);


    List<ResultadoAprendizaje> findByEstadoOrderByCodigoAsc(EstadoRegistro estado);
}
