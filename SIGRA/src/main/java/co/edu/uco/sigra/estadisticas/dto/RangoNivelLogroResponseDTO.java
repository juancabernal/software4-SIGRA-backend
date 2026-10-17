package co.edu.uco.sigra.estadisticas.dto;

import co.edu.uco.sigra.estadisticas.entity.NivelLogro;

/**
 * Un rango del catálogo de niveles de logro: desde {@code notaMinima} (incluida) hasta
 * {@code notaLimite} (incluida solo si {@code limiteIncluido}; únicamente en Sobresaliente, con 5.0).
 */
public record RangoNivelLogroResponseDTO(NivelLogro nivel, double notaMinima, double notaLimite,
                                         boolean limiteIncluido) {
}
