package co.edu.uco.sigra.programas.mapper;

import co.edu.uco.sigra.programas.dto.ProgramaResponseDTO;
import co.edu.uco.sigra.programas.entity.ProgramaAcademico;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ProgramaMapper {

    @Mapping(target = "facultadId", source = "facultad.id")
    @Mapping(target = "facultadNombre", source = "facultad.nombre")
    ProgramaResponseDTO toResponseDTO(ProgramaAcademico programa);
}