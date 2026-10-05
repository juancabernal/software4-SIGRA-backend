package co.edu.uco.sigra.profesores.service.impl;

import co.edu.uco.sigra.asignaturas.entity.AsignacionDocente;
import co.edu.uco.sigra.asignaturas.repository.AsignacionDocenteRepository;
import co.edu.uco.sigra.profesores.dto.AsignaturaProfesorResponseDTO;
import co.edu.uco.sigra.profesores.dto.ProfesorRequestDTO;
import co.edu.uco.sigra.profesores.dto.ProfesorResponseDTO;
import co.edu.uco.sigra.profesores.entity.Profesor;
import co.edu.uco.sigra.profesores.exception.*;
import co.edu.uco.sigra.profesores.mapper.ProfesorMapper;
import co.edu.uco.sigra.profesores.repository.ProfesorRepository;
import co.edu.uco.sigra.profesores.service.ProfesorService;
import co.edu.uco.sigra.common.enums.EstadoRegistro;
import co.edu.uco.sigra.common.repository.TipoDocumentoRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProfesorServiceImpl implements ProfesorService {

    private final ProfesorRepository profesorRepository;
    private final TipoDocumentoRepository tipoDocumentoRepository;
    private final AsignacionDocenteRepository asignacionDocenteRepository;
    private final ProfesorMapper profesorMapper;

    @Override
    @Transactional
    public ProfesorResponseDTO registrarProfesor(ProfesorRequestDTO dto) {
        if (profesorRepository.existsByTipoDocumentoIdAndNumeroDocumento(dto.tipoDocumentoId(), dto.numeroDocumento())) {
            throw new DocumentoDuplicadoException(dto.numeroDocumento());
        }
        if (profesorRepository.findByCorreoInstitucional(dto.correoInstitucional()).isPresent()) {
            throw new CorreoDuplicadoException(dto.correoInstitucional());
        }
        var tipoDocumento = tipoDocumentoRepository.findById(dto.tipoDocumentoId())
                .orElseThrow(() -> new TipoDocumentoNoEncontradoException(dto.tipoDocumentoId()));

        Profesor profesor = profesorMapper.toEntity(dto);
        profesor.setTipoDocumento(tipoDocumento);

        Profesor guardado = profesorRepository.save(profesor);
        return profesorMapper.toResponseDTO(guardado);
    }

    @Override
    @Transactional
    public List<ProfesorResponseDTO> consultar(String filtro) {
        return profesorRepository.findAll().stream()
                .filter(p -> filtro == null
                        || p.getNombreCompleto().toLowerCase().contains(filtro.toLowerCase())
                        || p.getNumeroDocumento().contains(filtro))
                .map(this::mapToDTO)
                .toList();
    }

    @Override
    @Transactional
    public List<AsignaturaProfesorResponseDTO> consultarAsignaturas(UUID profesorId, EstadoRegistro estado) {
        if (!profesorRepository.existsById(profesorId)) {
            throw new ProfesorNoEncontradoException(profesorId);
        }

        List<AsignacionDocente> asignaciones = (estado != null)
                ? asignacionDocenteRepository.findByProfesor_IdAndEstado(profesorId, estado)
                : asignacionDocenteRepository.findByProfesor_Id(profesorId);

        return asignaciones.stream()
                .map(a -> new AsignaturaProfesorResponseDTO(
                        a.getId(),
                        a.getAsignatura().getId(),
                        a.getAsignatura().getCodigo(),
                        a.getAsignatura().getNombre(),
                        a.getEstado()
                ))
                .toList();
    }

    @Override
    @Transactional
    public ProfesorResponseDTO modificarProfesor(UUID id, ProfesorRequestDTO dto) {
        Profesor profesor = profesorRepository.findById(id)
                .orElseThrow(() -> new ProfesorNoEncontradoException(id));

        if (!profesor.getNumeroDocumento().equals(dto.numeroDocumento())) {
            throw new CampoNoModificableException("numeroDocumento");
        }

        if (profesorRepository.existsByTipoDocumento_IdAndNumeroDocumentoAndIdNot(
                dto.tipoDocumentoId(), dto.numeroDocumento(), id)) {
            throw new DocumentoDuplicadoException(dto.numeroDocumento());
        }
        if (profesorRepository.existsByCorreoInstitucionalAndIdNot(dto.correoInstitucional(), id)) {
            throw new CorreoDuplicadoException(dto.correoInstitucional());
        }

        var tipoDocumento = tipoDocumentoRepository.findById(dto.tipoDocumentoId())
                .orElseThrow(() -> new TipoDocumentoNoEncontradoException(dto.tipoDocumentoId()));

        profesorMapper.updateEntityFromDTO(dto, profesor);
        profesor.setTipoDocumento(tipoDocumento);
        return profesorMapper.toResponseDTO(profesorRepository.save(profesor));
    }

    @Override
    @Transactional
    public void inactivar(UUID id) {
        Profesor profesor = profesorRepository.findById(id)
                .orElseThrow(() -> new ProfesorNoEncontradoException(id));

        if (asignacionDocenteRepository.existsByProfesor_IdAndEstado(id, EstadoRegistro.ACTIVO)) {
            throw new ProfesorConAsignacionesActivasException(id);
        }

        profesor.setEstado(EstadoRegistro.INACTIVO);
        profesorRepository.save(profesor);
    }

    private ProfesorResponseDTO mapToDTO(Profesor profesor) {
        boolean tieneAsignaciones = asignacionDocenteRepository
                .existsByProfesor_IdAndEstado(profesor.getId(), EstadoRegistro.ACTIVO);

        return new ProfesorResponseDTO(
                profesor.getId(),
                profesor.getTipoDocumento() != null ? profesor.getTipoDocumento().getNombre() : null,
                profesor.getNumeroDocumento(),
                profesor.getNombreCompleto(),
                profesor.getCorreoInstitucional(),
                profesor.getEstado().name(),
                tieneAsignaciones
        );
    }
}
