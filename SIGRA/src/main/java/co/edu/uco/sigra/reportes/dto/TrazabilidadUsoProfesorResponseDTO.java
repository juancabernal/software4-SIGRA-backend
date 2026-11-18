package co.edu.uco.sigra.reportes.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Schema(name = "TrazabilidadUsoProfesor",
        description = "Reporte R7 para el profesor: su propia actividad y la de sus estudiantes del semestre")
public record TrazabilidadUsoProfesorResponseDTO(
        LocalDate desde,
        LocalDate hasta,
        UUID semestreId,
        String semestreCodigo,
        ActividadPropiaDTO propia,
        ResumenActividadEstudiantesDTO resumenEstudiantes,
        List<ActividadEstudianteDTO> estudiantes,
        String mensaje
) {
}
