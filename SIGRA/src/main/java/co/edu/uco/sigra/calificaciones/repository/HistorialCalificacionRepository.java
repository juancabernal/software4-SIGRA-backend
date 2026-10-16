package co.edu.uco.sigra.calificaciones.repository;

import co.edu.uco.sigra.calificaciones.entity.HistorialCalificacion;
import org.springframework.data.repository.Repository;

import java.util.List;
import java.util.UUID;

public interface HistorialCalificacionRepository extends Repository<HistorialCalificacion, UUID> {

    HistorialCalificacion save(HistorialCalificacion historial);

    List<HistorialCalificacion> findByCalificacionIdOrderByFechaEventoAsc(UUID calificacionId);

    long countByCalificacionId(UUID calificacionId);
}
