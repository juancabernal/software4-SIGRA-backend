package co.edu.uco.sigra.mejoras.mapper;

import co.edu.uco.sigra.mejoras.dto.AccionMejoraResponseDTO;
import co.edu.uco.sigra.mejoras.entity.AccionMejora;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface AccionMejoraMapper {

    @Mapping(target = "resultadoAprendizajeId", source = "resultadoAprendizaje.id")
    @Mapping(target = "resultadoAprendizajeCodigo", source = "resultadoAprendizaje.codigo")
    @Mapping(target = "asignaturaId", source = "resultadoAprendizaje.asignatura.id")
    @Mapping(target = "conclusionOrigenId", source = "conclusionOrigen.id")
    @Mapping(target = "semestreId", source = "conclusionOrigen.semestre.id")
    @Mapping(target = "semestreCodigo", source = "conclusionOrigen.semestre.codigo")
    @Mapping(target = "convertida", expression = "java(entity.estaConvertida())")
    AccionMejoraResponseDTO toResponseDTO(AccionMejora entity);

    List<AccionMejoraResponseDTO> toResponseDTOList(List<AccionMejora> entities);
}
