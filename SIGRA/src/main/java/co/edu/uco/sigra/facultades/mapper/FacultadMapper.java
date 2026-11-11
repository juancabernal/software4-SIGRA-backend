package co.edu.uco.sigra.facultades.mapper;

import co.edu.uco.sigra.facultades.dto.FacultadResponseDTO;
import co.edu.uco.sigra.facultades.entity.Facultad;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface FacultadMapper {

    FacultadResponseDTO toResponseDTO(Facultad facultad);
}
