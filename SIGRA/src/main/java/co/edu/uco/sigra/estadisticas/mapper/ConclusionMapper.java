package co.edu.uco.sigra.estadisticas.mapper;

import co.edu.uco.sigra.estadisticas.dto.ConclusionResponseDTO;
import co.edu.uco.sigra.estadisticas.entity.Conclusion;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ConclusionMapper {

    String RECOMENDACION_FUNDAMENTOS =
            "El problema es de fundamentos: se recomienda reforzarlos antes de avanzar.";
    String RECOMENDACION_APLICACION =
            "El problema es de aplicación: se recomienda más práctica aplicada.";
    String RECOMENDACION_COMBINADA =
            "Se recomienda combinar el refuerzo de fundamentos con más práctica aplicada.";

    @Mapping(target = "resultadoAprendizajeId", source = "resultadoAprendizaje.id")
    @Mapping(target = "resultadoAprendizajeCodigo", source = "resultadoAprendizaje.codigo")
    @Mapping(target = "asignaturaId", source = "resultadoAprendizaje.asignatura.id")
    @Mapping(target = "semestreId", source = "semestre.id")
    @Mapping(target = "semestreCodigo", source = "semestre.codigo")
    @Mapping(target = "recomendacion", expression = "java(recomendacionDe(entity))")
    ConclusionResponseDTO toResponseDTO(Conclusion entity);

    List<ConclusionResponseDTO> toResponseDTOList(List<Conclusion> entities);

    /**
     * Se calcula al leer, no se guarda: así se puede mejorar la redacción después sin otra
     * migración. Null si la conclusión es de antes de este cambio (sin contadores).
     */
    default String recomendacionDe(Conclusion entity) {
        Integer enBajo = entity.getEnNivelBajo();
        Integer enMedio = entity.getEnNivelMedio();
        if (enBajo == null || enMedio == null) {
            return null;
        }
        if (enBajo > enMedio) {
            return RECOMENDACION_FUNDAMENTOS;
        }
        if (enMedio > enBajo) {
            return RECOMENDACION_APLICACION;
        }
        return RECOMENDACION_COMBINADA;
    }
}
