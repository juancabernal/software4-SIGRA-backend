package co.edu.uco.sigra.asignaturas.service.impl;

import co.edu.uco.sigra.asignaturas.dto.AsignacionDocenteResponseDTO;
import co.edu.uco.sigra.asignaturas.dto.AsignacionMasivaRequestDTO;
import co.edu.uco.sigra.asignaturas.dto.AsignacionNoRealizadaDTO;
import co.edu.uco.sigra.asignaturas.dto.ResultadoAsignacionMasivaDTO;
import co.edu.uco.sigra.asignaturas.entity.AsignacionDocente;
import co.edu.uco.sigra.asignaturas.entity.Asignatura;
import co.edu.uco.sigra.asignaturas.entity.EstadoAsignatura;
import co.edu.uco.sigra.asignaturas.exception.AsignacionDocenteNoEncontradaException;
import co.edu.uco.sigra.asignaturas.exception.AsignaturaNoEncontradaException;
import co.edu.uco.sigra.asignaturas.mapper.AsignacionDocenteMapper;
import co.edu.uco.sigra.asignaturas.repository.AsignacionDocenteRepository;
import co.edu.uco.sigra.asignaturas.repository.AsignaturaRepository;
import co.edu.uco.sigra.asignaturas.service.AsignacionDocenteService;
import co.edu.uco.sigra.common.enums.EstadoRegistro;
import co.edu.uco.sigra.profesores.entity.Profesor;
import co.edu.uco.sigra.profesores.exception.ProfesorNoEncontradoException;
import co.edu.uco.sigra.profesores.repository.ProfesorRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AsignacionDocenteServiceImpl implements AsignacionDocenteService {

    private final AsignacionDocenteRepository asignacionRepository;
    private final ProfesorRepository profesorRepository;
    private final AsignaturaRepository asignaturaRepository;
    private final AsignacionDocenteMapper asignacionMapper;

    @Override
    @Transactional
    public ResultadoAsignacionMasivaDTO asignarMasivo(AsignacionMasivaRequestDTO dto) {
        List<Profesor> profesores = dto.profesorIds().stream()
                .map(this::buscarProfesorOLanzar)
                .toList();
        List<Asignatura> asignaturas = dto.asignaturaIds().stream()
                .map(this::buscarAsignaturaOLanzar)
                .toList();

        List<AsignacionDocenteResponseDTO> creadas = new ArrayList<>();
        List<AsignacionNoRealizadaDTO> omitidas = new ArrayList<>();
        List<AsignacionNoRealizadaDTO> rechazadas = new ArrayList<>();

        for (Profesor profesor : profesores) {
            for (Asignatura asignatura : asignaturas) {
                procesarPar(profesor, asignatura, creadas, omitidas, rechazadas);
            }
        }

        return new ResultadoAsignacionMasivaDTO(creadas, omitidas, rechazadas);
    }

    private void procesarPar(Profesor profesor, Asignatura asignatura,
                              List<AsignacionDocenteResponseDTO> creadas,
                              List<AsignacionNoRealizadaDTO> omitidas,
                              List<AsignacionNoRealizadaDTO> rechazadas) {
        if (profesor.getEstado() != EstadoRegistro.ACTIVO) {
            rechazadas.add(new AsignacionNoRealizadaDTO(profesor.getId(), asignatura.getId(),
                    "El profesor está inactivo"));
            return;
        }
        if (asignatura.getEstado() != EstadoAsignatura.ACTIVA) {
            rechazadas.add(new AsignacionNoRealizadaDTO(profesor.getId(), asignatura.getId(),
                    "La asignatura no está activa"));
            return;
        }

        AsignacionDocente existente = asignacionRepository
                .findByProfesor_IdAndAsignatura_Id(profesor.getId(), asignatura.getId())
                .orElse(null);
        if (existente != null && existente.getEstado() == EstadoRegistro.ACTIVO) {
            omitidas.add(new AsignacionNoRealizadaDTO(profesor.getId(), asignatura.getId(),
                    "El profesor ya está asignado a esta asignatura"));
            return;
        }

        // Si ya existía una asignación INACTIVA para este par, se reactiva en lugar de crear otra fila.
        AsignacionDocente asignacion = existente != null ? existente : new AsignacionDocente();
        asignacion.setProfesor(profesor);
        asignacion.setAsignatura(asignatura);
        asignacion.setEstado(EstadoRegistro.ACTIVO);
        asignacion.setFechaAsignacion(LocalDate.now());
        creadas.add(asignacionMapper.toResponseDTO(asignacionRepository.save(asignacion)));
    }

    @Override
    @Transactional
    public void desasignar(UUID id) {
        AsignacionDocente asignacion = asignacionRepository.findById(id)
                .orElseThrow(() -> new AsignacionDocenteNoEncontradaException(id));
        if (asignacion.getEstado() == EstadoRegistro.INACTIVO) {
            return;
        }
        asignacion.setEstado(EstadoRegistro.INACTIVO);
        asignacionRepository.save(asignacion);
    }

    private Profesor buscarProfesorOLanzar(UUID id) {
        return profesorRepository.findById(id).orElseThrow(() -> new ProfesorNoEncontradoException(id));
    }

    private Asignatura buscarAsignaturaOLanzar(UUID id) {
        return asignaturaRepository.findById(id).orElseThrow(() -> new AsignaturaNoEncontradaException(id));
    }
}
