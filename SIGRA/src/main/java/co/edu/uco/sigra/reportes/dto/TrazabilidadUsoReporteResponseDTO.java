package co.edu.uco.sigra.reportes.dto;

import co.edu.uco.sigra.common.enums.RolUsuario;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Schema(name = "TrazabilidadUsoReporte",
        description = "Reporte R7: resumen de actividad por tipo de usuario, sin datos personales")
public record TrazabilidadUsoReporteResponseDTO(
        LocalDate desde,
        LocalDate hasta,
        UUID semestreId,
        String semestreCodigo,
        RolUsuario rol,
        TrazabilidadUsoResumenDTO resumen,
        List<ActividadRolDTO> porRol
) {
}
