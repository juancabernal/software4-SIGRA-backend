package co.edu.uco.sigra.auditoria.repository;

import co.edu.uco.sigra.auditoria.entity.HistorialAuditoria;
import co.edu.uco.sigra.auditoria.entity.TipoEntidadAuditoria;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.Repository;

import java.time.LocalDateTime;
import java.util.UUID;

public interface HistorialAuditoriaRepository extends Repository<HistorialAuditoria, UUID> {

    HistorialAuditoria save(HistorialAuditoria registro);

    Page<HistorialAuditoria> findByFechaHoraBetween(LocalDateTime desde, LocalDateTime hasta, Pageable pagina);

    Page<HistorialAuditoria> findByTipoEntidadAndFechaHoraBetween(TipoEntidadAuditoria tipoEntidad,
                                                                  LocalDateTime desde, LocalDateTime hasta,
                                                                  Pageable pagina);
}