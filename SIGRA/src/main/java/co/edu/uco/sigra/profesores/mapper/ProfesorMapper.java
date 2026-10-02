package co.edu.uco.sigra.profesores.mapper;

import co.edu.uco.sigra.profesores.dto.ProfesorRequestDTO;
import co.edu.uco.sigra.profesores.dto.ProfesorResponseDTO;
import co.edu.uco.sigra.profesores.entity.Profesor;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface ProfesorMapper {
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tipoDocumento", ignore = true)
    @Mapping(target = "estado", ignore = true)
    @Mapping(target = "intentosFallidos", ignore = true)
    @Mapping(target = "fechaBloqueo", ignore = true)
    Profesor toEntity(ProfesorRequestDTO dto);

    @Mapping(target = "tipoDocumentoNombre", source = "tipoDocumento.nombre")
    ProfesorResponseDTO toResponseDTO(Profesor entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tipoDocumento", ignore = true)
    @Mapping(target = "estado", ignore = true)
    @Mapping(target = "intentosFallidos", ignore = true)
    @Mapping(target = "fechaBloqueo", ignore = true)
    void updateEntityFromDTO(ProfesorRequestDTO dto, @MappingTarget Profesor entity);
}
