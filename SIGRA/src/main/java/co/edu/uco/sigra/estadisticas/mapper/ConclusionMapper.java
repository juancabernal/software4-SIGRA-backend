package co.edu.uco.sigra.estadisticas.mapper;

import co.edu.uco.sigra.estadisticas.dto.ConclusionResponseDTO;
import co.edu.uco.sigra.estadisticas.entity.Conclusion;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ConclusionMapper {

    @Mapping(target = "resultadoAprendizajeId", source = "resultadoAprendizaje.id")
    @Mapping(target = "resultadoAprendizajeCodigo", source = "resultadoAprendizaje.codigo")
    @Mapping(target = "asignaturaId", source = "resultadoAprendizaje.asignatura.id")
    @Mapping(target = "semestreId", source = "semestre.id")
    @Mapping(target = "semestreCodigo", source = "semestre.codigo")
    ConclusionResponseDTO toResponseDTO(Conclusion entity);

    List<ConclusionResponseDTO> toResponseDTOList(List<Conclusion> entities);
}
