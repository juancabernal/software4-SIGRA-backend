package co.edu.uco.sigra.evaluaciones.mapper;

import co.edu.uco.sigra.evaluaciones.dto.EvaluacionCrearRequestDTO;
import co.edu.uco.sigra.evaluaciones.dto.EvaluacionResponseDTO;
import co.edu.uco.sigra.evaluaciones.entity.Evaluacion;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface EvaluacionMapper {

    /** RA, semestre y estado los asigna el servicio. */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "resultadoAprendizaje", ignore = true)
    @Mapping(target = "semestre", ignore = true)
    @Mapping(target = "estado", ignore = true)
    @Mapping(target = "inactivadaConRa", ignore = true)
    Evaluacion toEntity(EvaluacionCrearRequestDTO dto);

    /** {@code tieneCalificaciones} no está en la entidad: lo entrega el servicio (puerto de calificaciones). */
    @Mapping(target = "resultadoAprendizajeId", source = "entity.resultadoAprendizaje.id")
    @Mapping(target = "semestreId", source = "entity.semestre.id")
    @Mapping(target = "semestreCodigo", source = "entity.semestre.codigo")
    EvaluacionResponseDTO toResponseDTO(Evaluacion entity, boolean tieneCalificaciones);
}
