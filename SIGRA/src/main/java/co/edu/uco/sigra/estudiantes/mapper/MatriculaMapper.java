package co.edu.uco.sigra.estudiantes.mapper;

import co.edu.uco.sigra.estudiantes.dto.AsignaturaDeEstudianteDTO;
import co.edu.uco.sigra.estudiantes.dto.EstudianteMatriculadoDTO;
import co.edu.uco.sigra.estudiantes.dto.MatriculaResponseDTO;
import co.edu.uco.sigra.estudiantes.entity.Matricula;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface MatriculaMapper {

    @Mapping(target = "id", source = "id")
    @Mapping(target = "estudianteId", source = "estudiante.id")
    @Mapping(target = "asignaturaId", source = "asignatura.id")
    @Mapping(target = "semestreId", source = "semestre.id")
    MatriculaResponseDTO toResponseDTO(Matricula matricula);

    @Mapping(target = "matriculaId", source = "id")
    @Mapping(target = "numeroDocumento", source = "estudiante.numeroDocumento")
    @Mapping(target = "nombreCompleto", source = "estudiante.nombreCompleto")
    EstudianteMatriculadoDTO toEstudianteMatriculadoDTO(Matricula matricula);

    List<EstudianteMatriculadoDTO> toEstudianteMatriculadoDTOList(List<Matricula> matriculas);

    @Mapping(target = "matriculaId", source = "id")
    @Mapping(target = "asignaturaNombre", source = "asignatura.nombre")
    @Mapping(target = "semestreCodigo", source = "semestre.codigo")
    AsignaturaDeEstudianteDTO toAsignaturaDeEstudianteDTO(Matricula matricula);

    List<AsignaturaDeEstudianteDTO> toAsignaturaDeEstudianteDTOList(List<Matricula> matriculas);
}
