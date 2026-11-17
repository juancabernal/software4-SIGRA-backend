package co.edu.uco.sigra.reportes.dto;

public record TrazabilidadUsoResumenDTO(
        long totalEventos,
        long usuariosActivos,
        long loginsExitosos,
        long loginsFallidos
) {
}
