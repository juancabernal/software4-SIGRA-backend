package co.edu.uco.sigra.profesores.mapper;

import co.edu.uco.sigra.profesores.dto.ProfesorRequestDTO;
import co.edu.uco.sigra.profesores.dto.ProfesorResponseDTO;
import co.edu.uco.sigra.profesores.entity.Profesor;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface ProfesorMapper {
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tipoDocumento", ignore = true)
    @Mapping(target = "estado", ignore = true)
    @Mapping(target = "passwordHash", ignore = true)
    @Mapping(target = "intentosFallidos", ignore = true)
    @Mapping(target = "fechaBloqueo", ignore = true)
    Profesor toEntity(ProfesorRequestDTO dto);

    /**
     * {@code tieneAsignacionesActivas} no es un dato de la entidad: lo calcula el servicio y llega
     * como segundo parámetro, igual que {@code cantidadRa} en AsignaturaMapper. Así ninguna
     * respuesta (registrar, consultar o modificar) puede devolver un valor por defecto falso.
     */
    @Mapping(target = "id", source = "entity.id")
    @Mapping(target = "tipoDocumentoId", source = "entity.tipoDocumento.id")
    @Mapping(target = "tipoDocumentoNombre", source = "entity.tipoDocumento.nombre")
    @Mapping(target = "numeroDocumento", source = "entity.numeroDocumento")
    @Mapping(target = "nombreCompleto", source = "entity.nombreCompleto")
    @Mapping(target = "correoInstitucional", source = "entity.correoInstitucional")
    @Mapping(target = "estado", source = "entity.estado")
    @Mapping(target = "tieneAsignacionesActivas", source = "tieneAsignacionesActivas")
    ProfesorResponseDTO toResponseDTO(Profesor entity, boolean tieneAsignacionesActivas);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tipoDocumento", ignore = true)
    @Mapping(target = "estado", ignore = true)
    @Mapping(target = "passwordHash", ignore = true)
    @Mapping(target = "intentosFallidos", ignore = true)
    @Mapping(target = "fechaBloqueo", ignore = true)
    void updateEntityFromDTO(ProfesorRequestDTO dto, @MappingTarget Profesor entity);
}