package co.edu.uco.sigra.reportes.dto;

import co.edu.uco.sigra.auditoria.entity.AccionAuditoria;

/** Cantidad de eventos de una acción auditada. */
public record ConteoAccionDTO(AccionAuditoria accion, long total) {
}
