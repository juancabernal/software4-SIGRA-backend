package co.edu.uco.sigra.calificaciones.mapper;

import co.edu.uco.sigra.calificaciones.dto.CalificacionResponseDTO;
import co.edu.uco.sigra.calificaciones.dto.HistorialCalificacionDTO;
import co.edu.uco.sigra.calificaciones.entity.Calificacion;
import co.edu.uco.sigra.calificaciones.entity.HistorialCalificacion;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface CalificacionMapper {

    @Mapping(target = "evaluacionId", source = "evaluacion.id")
    @Mapping(target = "matriculaId", source = "matricula.id")
    @Mapping(target = "estudianteId", source = "matricula.estudiante.id")
    @Mapping(target = "estudianteNombre", source = "matricula.estudiante.nombreCompleto")
    CalificacionResponseDTO toResponseDTO(Calificacion calificacion);

    HistorialCalificacionDTO toHistorialDTO(HistorialCalificacion historial);

    List<HistorialCalificacionDTO> toHistorialDTOList(List<HistorialCalificacion> historial);
}