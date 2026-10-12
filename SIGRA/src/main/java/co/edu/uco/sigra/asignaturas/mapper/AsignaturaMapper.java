package co.edu.uco.sigra.asignaturas.mapper;

import co.edu.uco.sigra.asignaturas.dto.AsignaturaResponseDTO;
import co.edu.uco.sigra.asignaturas.entity.Asignatura;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface AsignaturaMapper {

    @Mapping(target = "id", source = "asignatura.id")
    @Mapping(target = "codigo", source = "asignatura.codigo")
    @Mapping(target = "nombre", source = "asignatura.nombre")
    @Mapping(target = "programaId", source = "asignatura.programa.id")
    @Mapping(target = "programaNombre", source = "asignatura.programa.nombre")
    @Mapping(target = "estado", source = "asignatura.estado")
    @Mapping(target = "cantidadRa", source = "cantidadRa")
    AsignaturaResponseDTO toResponseDTO(Asignatura asignatura, long cantidadRa);
}
