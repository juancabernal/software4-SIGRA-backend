package co.edu.uco.sigra.asignaturas.mapper;

import co.edu.uco.sigra.asignaturas.dto.AsignacionDocenteResponseDTO;
import co.edu.uco.sigra.asignaturas.entity.AsignacionDocente;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface AsignacionDocenteMapper {

    @Mapping(target = "profesorId", source = "profesor.id")
    @Mapping(target = "nombreProfesor", source = "profesor.nombreCompleto")
    @Mapping(target = "asignaturaId", source = "asignatura.id")
    @Mapping(target = "codigoAsignatura", source = "asignatura.codigo")
    @Mapping(target = "nombreAsignatura", source = "asignatura.nombre")
    AsignacionDocenteResponseDTO toResponseDTO(AsignacionDocente asignacion);
}
