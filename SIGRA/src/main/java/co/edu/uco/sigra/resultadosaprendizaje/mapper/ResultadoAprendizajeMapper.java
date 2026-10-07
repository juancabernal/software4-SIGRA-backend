package co.edu.uco.sigra.resultadosaprendizaje.mapper;

import co.edu.uco.sigra.resultadosaprendizaje.dto.ResultadoAprendizajeCrearRequestDTO;
import co.edu.uco.sigra.resultadosaprendizaje.dto.ResultadoAprendizajeResponseDTO;
import co.edu.uco.sigra.resultadosaprendizaje.entity.ResultadoAprendizaje;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;


@Mapper(componentModel = "spring")
public interface ResultadoAprendizajeMapper {


    @Mapping(target = "id", ignore = true)
    @Mapping(target = "asignatura", ignore = true)
    @Mapping(target = "estado", ignore = true)
    @Mapping(target = "inactivadoConAsignatura", ignore = true)
    ResultadoAprendizaje toEntity(ResultadoAprendizajeCrearRequestDTO dto);

    @Mapping(target = "asignaturaId", source = "asignatura.id")
    ResultadoAprendizajeResponseDTO toResponseDTO(ResultadoAprendizaje entity);

    List<ResultadoAprendizajeResponseDTO> toResponseDTOList(List<ResultadoAprendizaje> entities);
}
