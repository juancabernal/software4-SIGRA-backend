package co.edu.uco.sigra.estadisticas.dto;

import co.edu.uco.sigra.estadisticas.entity.NivelLogro;

/** Cuántos estudiantes evaluados quedaron en un nivel y qué porcentaje representan de los evaluados. */
public record DistribucionNivelLogroDTO(NivelLogro nivel, long cantidad, double porcentaje) {
}
