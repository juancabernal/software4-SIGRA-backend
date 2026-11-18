package co.edu.uco.sigra.reportes.repository;

import co.edu.uco.sigra.auditoria.entity.AccionAuditoria;
import co.edu.uco.sigra.auditoria.entity.HistorialAuditoria;
import co.edu.uco.sigra.common.enums.RolUsuario;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * R7: agregados de solo lectura sobre {@code historial_auditoria}. Ninguna consulta devuelve el detalle
 * ni el identificador de la entidad afectada; solo conteos por rol, acción o usuario.
 */
public interface TrazabilidadUsoReporteRepository extends Repository<HistorialAuditoria, UUID> {

    @Query("""
            select h.rolUsuario as rol, h.accion as accion, count(h) as total
            from HistorialAuditoria h
            where h.fechaHora between :inicio and :fin
            group by h.rolUsuario, h.accion
            """)
    List<ConteoRolAccion> contarPorRolYAccion(@Param("inicio") LocalDateTime inicio,
                                              @Param("fin") LocalDateTime fin);

    @Query("""
            select h.rolUsuario as rol, count(distinct h.usuarioId) as usuarios, max(h.fechaHora) as ultimaActividad
            from HistorialAuditoria h
            where h.fechaHora between :inicio and :fin
            group by h.rolUsuario
            """)
    List<ResumenRol> resumirPorRol(@Param("inicio") LocalDateTime inicio,
                                   @Param("fin") LocalDateTime fin);

    @Query("""
            select h.accion as accion, count(h) as total, max(h.fechaHora) as ultimaActividad
            from HistorialAuditoria h
            where h.usuarioId = :usuarioId and h.fechaHora between :inicio and :fin
            group by h.accion
            """)
    List<ConteoAccion> contarPropiasPorAccion(@Param("usuarioId") UUID usuarioId,
                                              @Param("inicio") LocalDateTime inicio,
                                              @Param("fin") LocalDateTime fin);

    @Query("""
            select h.usuarioId as usuarioId, count(h) as total,
                   sum(case when h.accion = :ingreso then 1 else 0 end) as ingresos,
                   max(h.fechaHora) as ultimaActividad
            from HistorialAuditoria h
            where h.usuarioId in :usuarioIds and h.fechaHora between :inicio and :fin
            group by h.usuarioId
            """)
    List<ActividadUsuario> resumirPorUsuarios(@Param("usuarioIds") Collection<UUID> usuarioIds,
                                              @Param("ingreso") AccionAuditoria ingreso,
                                              @Param("inicio") LocalDateTime inicio,
                                              @Param("fin") LocalDateTime fin);

    interface ConteoRolAccion {
        RolUsuario getRol();
        AccionAuditoria getAccion();
        Long getTotal();
    }

    interface ResumenRol {
        RolUsuario getRol();
        Long getUsuarios();
        LocalDateTime getUltimaActividad();
    }

    interface ConteoAccion {
        AccionAuditoria getAccion();
        Long getTotal();
        LocalDateTime getUltimaActividad();
    }

    interface ActividadUsuario {
        UUID getUsuarioId();
        Long getTotal();
        Long getIngresos();
        LocalDateTime getUltimaActividad();
    }
}
