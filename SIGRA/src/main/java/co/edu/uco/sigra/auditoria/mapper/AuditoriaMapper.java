package co.edu.uco.sigra.auditoria.mapper;

import co.edu.uco.sigra.auditoria.dto.RegistroAuditoriaDTO;
import co.edu.uco.sigra.auditoria.entity.HistorialAuditoria;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface AuditoriaMapper {
    RegistroAuditoriaDTO toDTO(HistorialAuditoria registro);
}