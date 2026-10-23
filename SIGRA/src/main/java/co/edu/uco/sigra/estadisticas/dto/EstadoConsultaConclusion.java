package co.edu.uco.sigra.estadisticas.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Estado observable de un (RA, semestre) para RF-16: distingue las cuatro respuestas que exige el
 * requisito (conclusión vigente, ya atendida, datos insuficientes o desempeño satisfactorio) sin
 * mutar nada, a diferencia de {@code generarConclusion}.
 */
@Schema(description = "Estado observable de un resultado de aprendizaje en un semestre")
public enum EstadoConsultaConclusion {
    VIGENTE,
    ATENDIDA,
    SIN_DATOS_SUFICIENTES,
    SATISFACTORIO
}
