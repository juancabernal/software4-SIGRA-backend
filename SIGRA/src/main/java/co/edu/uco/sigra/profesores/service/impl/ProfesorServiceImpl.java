package co.edu.uco.sigra.profesores.service.impl;

import co.edu.uco.sigra.asignaturas.entity.AsignacionDocente;
import co.edu.uco.sigra.asignaturas.repository.AsignacionDocenteRepository;
import co.edu.uco.sigra.auth.repository.UsuarioRepository;
import co.edu.uco.sigra.common.enums.EstadoRegistro;
import co.edu.uco.sigra.common.repository.TipoDocumentoRepository;
import co.edu.uco.sigra.common.util.Correos;
import co.edu.uco.sigra.common.util.Textos;
import co.edu.uco.sigra.profesores.dto.AsignaturaProfesorResponseDTO;
import co.edu.uco.sigra.profesores.dto.ProfesorRequestDTO;
import co.edu.uco.sigra.profesores.dto.ProfesorResponseDTO;
import co.edu.uco.sigra.profesores.entity.Profesor;
import co.edu.uco.sigra.profesores.exception.*;
import co.edu.uco.sigra.profesores.mapper.ProfesorMapper;
import co.edu.uco.sigra.profesores.repository.ProfesorRepository;
import co.edu.uco.sigra.profesores.service.ProfesorService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProfesorServiceImpl implements ProfesorService {

    private final ProfesorRepository profesorRepository;
    private final UsuarioRepository usuarioRepository;
    private final TipoDocumentoRepository tipoDocumentoRepository;
    private final AsignacionDocenteRepository asignacionDocenteRepository;
    private final ProfesorMapper profesorMapper;

    @Override
    @Transactional
    public ProfesorResponseDTO registrarProfesor(ProfesorRequestDTO dto) {
        if (profesorRepository.existsByTipoDocumento_IdAndNumeroDocumento(dto.tipoDocumentoId(), dto.numeroDocumento())) {
            throw new DocumentoDuplicadoException(dto.numeroDocumento());
        }
        String correo = Correos.normalizar(dto.correoInstitucional());
        if (usuarioRepository.existsByCorreoInstitucionalIgnoreCase(correo)) {
            throw new CorreoDuplicadoException(correo);
        }
        var tipoDocumento = tipoDocumentoRepository.findById(dto.tipoDocumentoId())
                .orElseThrow(() -> new TipoDocumentoNoEncontradoException(dto.tipoDocumentoId()));

        Profesor profesor = profesorMapper.toEntity(dto);
        profesor.setCorreoInstitucional(correo);
        profesor.setTipoDocumento(tipoDocumento);

        Profesor guardado = profesorRepository.save(profesor);
        // Un profesor recién creado no puede tener asignaciones docentes todavía.
        return profesorMapper.toResponseDTO(guardado, false);
    }

    /**
     * Coincide si el término aparece en el nombre, el número de documento o el correo, sin
     * distinguir mayúsculas ni tildes («gomez» encuentra a «Gómez»). Un término vacío no filtra.
     */
    @Override
    @Transactional
    public List<ProfesorResponseDTO> consultar(String filtro) {
        String termino = Textos.terminoDeBusqueda(filtro);
        return profesorRepository.findAll().stream()
                .filter(p -> termino == null || coincide(p, termino))
                .map(p -> profesorMapper.toResponseDTO(p, tieneAsignacionesActivas(p.getId())))
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
        String correo = Correos.normalizar(dto.correoInstitucional());
        if (usuarioRepository.existsByCorreoInstitucionalIgnoreCaseAndIdNot(correo, id)) {
            throw new CorreoDuplicadoException(correo);
        }

        var tipoDocumento = tipoDocumentoRepository.findById(dto.tipoDocumentoId())
                .orElseThrow(() -> new TipoDocumentoNoEncontradoException(dto.tipoDocumentoId()));

        profesorMapper.updateEntityFromDTO(dto, profesor);
        profesor.setCorreoInstitucional(correo);
        profesor.setTipoDocumento(tipoDocumento);
        Profesor guardado = profesorRepository.save(profesor);
        return profesorMapper.toResponseDTO(guardado, tieneAsignacionesActivas(id));
    }

    @Override
    @Transactional
    public void inactivar(UUID id) {
        Profesor profesor = profesorRepository.findById(id)
                .orElseThrow(() -> new ProfesorNoEncontradoException(id));

        if (tieneAsignacionesActivas(id)) {
            throw new ProfesorConAsignacionesActivasException(id);
        }

        profesor.setEstado(EstadoRegistro.INACTIVO);
        profesorRepository.save(profesor);
    }

    private boolean tieneAsignacionesActivas(UUID profesorId) {
        return asignacionDocenteRepository.existsByProfesor_IdAndEstado(profesorId, EstadoRegistro.ACTIVO);
    }

    private static boolean coincide(Profesor profesor, String termino) {
        return contiene(profesor.getNombreCompleto(), termino)
                || contiene(profesor.getNumeroDocumento(), termino)
                || contiene(profesor.getCorreoInstitucional(), termino);
    }

    private static boolean contiene(String valor, String termino) {
        return valor != null && Textos.normalizarBusqueda(valor).contains(termino);
    }
}