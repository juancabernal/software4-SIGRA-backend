package co.edu.uco.sigra.programas.mapper;

import co.edu.uco.sigra.programas.dto.ProgramaResponseDTO;
import co.edu.uco.sigra.programas.entity.ProgramaAcademico;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ProgramaMapper {

    ProgramaResponseDTO toResponseDTO(ProgramaAcademico programa);
}
