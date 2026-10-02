package co.edu.uco.sigra.profesores.service.impl;

import co.edu.uco.sigra.asignaturas.repository.AsignacionDocenteRepository;
import co.edu.uco.sigra.profesores.dto.ProfesorRequestDTO;
import co.edu.uco.sigra.profesores.dto.ProfesorResponseDTO;
import co.edu.uco.sigra.profesores.entity.Profesor;
import co.edu.uco.sigra.profesores.exception.*;
import co.edu.uco.sigra.profesores.mapper.ProfesorMapper;
import co.edu.uco.sigra.profesores.repository.ProfesorRepository;
import co.edu.uco.sigra.profesores.service.ProfesorService;
import co.edu.uco.sigra.shared.enums.EstadoRegistro;
import co.edu.uco.sigra.shared.repository.TipoDocumentoRepository;
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
                .map(profesorMapper::toResponseDTO)
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
}
