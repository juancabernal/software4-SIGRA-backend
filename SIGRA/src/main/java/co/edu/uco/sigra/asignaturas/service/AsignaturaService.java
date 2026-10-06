package co.edu.uco.sigra.asignaturas.service;

import co.edu.uco.sigra.asignaturas.dto.AsignaturaFiltroDTO;
import co.edu.uco.sigra.asignaturas.dto.AsignaturaRequestDTO;
import co.edu.uco.sigra.asignaturas.dto.AsignaturaResponseDTO;
import co.edu.uco.sigra.asignaturas.dto.AsignaturaUpdateDTO;
import co.edu.uco.sigra.asignaturas.seguridad.IdentidadActual;

import java.util.List;
import java.util.UUID;

public interface AsignaturaService {
    AsignaturaResponseDTO registrarAsignatura(AsignaturaRequestDTO dto);
    List<AsignaturaResponseDTO> consultar(AsignaturaFiltroDTO filtro);

    /**
     * Consulta con alcance por rol. Con un ADMINISTRADOR devuelve todas; con un PROFESOR, solo las
     * asignaturas donde tiene asignación docente ACTIVA. Sin identidad equivale a {@link #consultar(AsignaturaFiltroDTO)}.
     */
    List<AsignaturaResponseDTO> consultar(AsignaturaFiltroDTO filtro, IdentidadActual identidad);

    AsignaturaResponseDTO obtenerAsignatura(UUID id);

    /** Igual alcance que la consulta: un PROFESOR solo obtiene una asignatura suya (si no, 403). */
    AsignaturaResponseDTO obtenerAsignatura(UUID id, IdentidadActual identidad);

    AsignaturaResponseDTO modificarAsignatura(UUID id, AsignaturaUpdateDTO dto);
    AsignaturaResponseDTO activar(UUID id);

    /**
     * Inactiva una asignatura ACTIVA e inactiva en cascada sus resultados de aprendizaje activos.
     * No afecta matrículas, evaluaciones ni calificaciones.
     */
    AsignaturaResponseDTO inactivar(UUID id);
}
