package co.edu.uco.sigra.asignaturas.service;

import co.edu.uco.sigra.asignaturas.dto.AsignaturaFiltroDTO;
import co.edu.uco.sigra.asignaturas.dto.AsignaturaRequestDTO;
import co.edu.uco.sigra.asignaturas.dto.AsignaturaResponseDTO;
import co.edu.uco.sigra.asignaturas.dto.AsignaturaUpdateDTO;
import co.edu.uco.sigra.asignaturas.dto.MiAsignaturaDTO;
import co.edu.uco.sigra.auth.security.UsuarioPrincipal;

import java.util.List;
import java.util.UUID;

public interface AsignaturaService {
    AsignaturaResponseDTO registrarAsignatura(AsignaturaRequestDTO dto);
    List<AsignaturaResponseDTO> consultar(AsignaturaFiltroDTO filtro);

    /**
     * Consulta con alcance por rol. Con un ADMINISTRADOR devuelve todas; con un PROFESOR, solo las
     * asignaturas donde tiene asignación docente ACTIVA. La identidad se obtiene del contexto de
     * Spring Security cuando se usa {@link #consultar(AsignaturaFiltroDTO)}.
     */
    List<AsignaturaResponseDTO> consultar(AsignaturaFiltroDTO filtro, UsuarioPrincipal identidad);

    AsignaturaResponseDTO obtenerAsignatura(UUID id);

    /** Igual alcance que la consulta: un PROFESOR solo obtiene una asignatura suya (si no, 403). */
    AsignaturaResponseDTO obtenerAsignatura(UUID id, UsuarioPrincipal identidad);

    AsignaturaResponseDTO modificarAsignatura(UUID id, AsignaturaUpdateDTO dto);
    AsignaturaResponseDTO activar(UUID id);

    /**
     * Inactiva una asignatura ACTIVA e inactiva en cascada sus resultados de aprendizaje activos.
     * No afecta matrículas ni calificaciones.
     * <p>
     * Si la asignatura tiene evaluaciones ACTIVAS en el semestre en curso y {@code confirmar} es
     * {@code false}, se rechaza con
     * {@link co.edu.uco.sigra.asignaturas.exception.ConfirmacionRequeridaException} y la
     * asignatura queda intacta. Si no hay semestre en curso, o no tiene evaluaciones abiertas, o
     * {@code confirmar} es {@code true}, procede igual que hoy.
     */
    AsignaturaResponseDTO inactivar(UUID id, boolean confirmar);

    /**
     * Asignaturas de quien pregunta, ordenadas por nombre: las del PROFESOR con asignación docente
     * ACTIVA, o las del ESTUDIANTE con matrícula ACTIVA. Otro rol recibe 403.
     */
    List<MiAsignaturaDTO> misAsignaturas(UsuarioPrincipal identidad);
}
