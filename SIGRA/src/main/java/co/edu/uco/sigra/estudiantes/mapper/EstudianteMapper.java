package co.edu.uco.sigra.estudiantes.mapper;

import co.edu.uco.sigra.estudiantes.dto.EstudianteRequestDTO;
import co.edu.uco.sigra.estudiantes.dto.EstudianteResponseDTO;
import co.edu.uco.sigra.estudiantes.entity.Estudiante;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface EstudianteMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tipoDocumento", ignore = true)
    @Mapping(target = "estado", ignore = true)
    Estudiante toEntity(EstudianteRequestDTO dto);

    @Mapping(target = "tipoDocumentoNombre", source = "tipoDocumento.nombre")
    EstudianteResponseDTO toResponseDTO(Estudiante entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tipoDocumento", ignore = true)
    @Mapping(target = "estado", ignore = true)
    void updateEntityFromDTO(EstudianteRequestDTO dto, @MappingTarget Estudiante entity);
}
