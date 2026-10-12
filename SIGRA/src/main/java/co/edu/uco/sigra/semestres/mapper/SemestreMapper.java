package co.edu.uco.sigra.semestres.mapper;

import co.edu.uco.sigra.semestres.dto.SemestreRequestDTO;
import co.edu.uco.sigra.semestres.dto.SemestreResponseDTO;
import co.edu.uco.sigra.semestres.entity.Semestre;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface SemestreMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "estado", ignore = true)
    Semestre toEntity(SemestreRequestDTO dto);

    SemestreResponseDTO toResponseDTO(Semestre entity);
}
