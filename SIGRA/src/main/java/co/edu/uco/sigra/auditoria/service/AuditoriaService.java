package co.edu.uco.sigra.auditoria.service;

import co.edu.uco.sigra.auditoria.dto.PaginaAuditoriaDTO;
import co.edu.uco.sigra.auditoria.entity.AccionAuditoria;
import co.edu.uco.sigra.auditoria.entity.TipoEntidadAuditoria;
import co.edu.uco.sigra.common.enums.RolUsuario;

import java.time.LocalDate;
import java.util.UUID;

public interface AuditoriaService {

    void registrarEvento(TipoEntidadAuditoria tipoEntidad, UUID idEntidad, AccionAuditoria accion, String detalle);

    void registrarEvento(TipoEntidadAuditoria tipoEntidad, UUID idEntidad, AccionAuditoria accion, String detalle,
                         UUID usuarioId, RolUsuario rolUsuario);

    PaginaAuditoriaDTO consultar(LocalDate desde, LocalDate hasta, TipoEntidadAuditoria tipoEntidad,
                                 int pagina, int tamano);
}