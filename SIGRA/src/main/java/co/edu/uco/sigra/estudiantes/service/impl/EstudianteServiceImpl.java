package co.edu.uco.sigra.estudiantes.service.impl;

import co.edu.uco.sigra.auth.repository.UsuarioRepository;
import co.edu.uco.sigra.auth.security.CurrentUser;
import co.edu.uco.sigra.auth.security.UsuarioPrincipal;
import co.edu.uco.sigra.common.enums.EstadoRegistro;
import co.edu.uco.sigra.common.enums.RolUsuario;
import co.edu.uco.sigra.common.repository.TipoDocumentoRepository;
import co.edu.uco.sigra.common.util.Correos;
import co.edu.uco.sigra.estudiantes.dto.EstudianteRequestDTO;
import co.edu.uco.sigra.estudiantes.dto.EstudianteResponseDTO;
import co.edu.uco.sigra.estudiantes.entity.Estudiante;
import co.edu.uco.sigra.estudiantes.exception.CorreoEstudianteDuplicadoException;
import co.edu.uco.sigra.estudiantes.exception.DocumentoNoModificableException;
import co.edu.uco.sigra.estudiantes.exception.EstudianteNoEncontradoException;
import co.edu.uco.sigra.estudiantes.exception.EstudianteYaRegistradoException;
import co.edu.uco.sigra.estudiantes.exception.ReferenciaNoEncontradaException;
import co.edu.uco.sigra.estudiantes.mapper.EstudianteMapper;
import co.edu.uco.sigra.estudiantes.repository.EstudianteRepository;
import co.edu.uco.sigra.estudiantes.service.EstudianteService;
import jakarta.transaction.Transactional;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class EstudianteServiceImpl implements EstudianteService {

    private final EstudianteRepository estudianteRepository;
    private final TipoDocumentoRepository tipoDocumentoRepository;
    private final EstudianteMapper estudianteMapper;
    private final UsuarioRepository usuarioRepository;
    private final CurrentUser currentUser;

    @Autowired
    public EstudianteServiceImpl(EstudianteRepository estudianteRepository,
                                 TipoDocumentoRepository tipoDocumentoRepository,
                                 EstudianteMapper estudianteMapper,
                                 UsuarioRepository usuarioRepository,
                                 CurrentUser currentUser) {
        this.estudianteRepository = estudianteRepository;
        this.tipoDocumentoRepository = tipoDocumentoRepository;
        this.estudianteMapper = estudianteMapper;
        this.usuarioRepository = usuarioRepository;
        this.currentUser = currentUser;
    }

    /** Constructor reducido conservado para pruebas unitarias; las consultas protegidas siguen fallando cerrado. */
    public EstudianteServiceImpl(EstudianteRepository estudianteRepository,
                                 TipoDocumentoRepository tipoDocumentoRepository,
                                 EstudianteMapper estudianteMapper,
                                 UsuarioRepository usuarioRepository) {
        this(estudianteRepository, tipoDocumentoRepository, estudianteMapper, usuarioRepository, new CurrentUser());
    }

    @Override
    @Transactional
    public EstudianteResponseDTO registrar(EstudianteRequestDTO dto) {
        if (estudianteRepository.existsByTipoDocumento_IdAndNumeroDocumento(dto.tipoDocumentoId(), dto.numeroDocumento())) {
            throw EstudianteYaRegistradoException.porDocumento(dto.numeroDocumento());
        }

        String correo = Correos.normalizar(dto.correoInstitucional());
        if (usuarioRepository.existsByCorreoInstitucionalIgnoreCase(correo)) {
            throw CorreoEstudianteDuplicadoException.porCorreo(correo);
        }

        var tipoDocumento = tipoDocumentoRepository.findById(dto.tipoDocumentoId())
                .orElseThrow(() -> ReferenciaNoEncontradaException.tipoDocumento(dto.tipoDocumentoId()));

        Estudiante estudiante = estudianteMapper.toEntity(dto);
        estudiante.setCorreoInstitucional(correo);
        estudiante.setTipoDocumento(tipoDocumento);

        Estudiante guardado = estudianteRepository.save(estudiante);
        return estudianteMapper.toResponseDTO(guardado);
    }

    @Override
    @Transactional
    public List<EstudianteResponseDTO> consultar(String filtro) {
        return consultar(filtro, currentUser.required());
    }

    @Override
    @Transactional
    public List<EstudianteResponseDTO> consultar(String filtro, UsuarioPrincipal principal) {
        validarPrincipal(principal);
        String criterio = (filtro == null || filtro.isBlank()) ? "" : filtro;
        List<Estudiante> estudiantes = principal.rol() == RolUsuario.PROFESOR
                ? estudianteRepository.buscarVisiblesParaProfesor(
                principal.id(), criterio, EstadoRegistro.ACTIVO, EstadoRegistro.ACTIVO, EstadoRegistro.ACTIVO)
                : (criterio.isEmpty()
                ? estudianteRepository.findAll()
                : estudianteRepository.buscarPorNombreODocumento(criterio));
        return estudiantes.stream()
                .map(estudianteMapper::toResponseDTO)
                .toList();
    }

    @Override
    @Transactional
    public EstudianteResponseDTO consultarPorId(UUID id) {
        return consultarPorId(id, currentUser.required());
    }

    @Override
    @Transactional
    public EstudianteResponseDTO consultarPorId(UUID id, UsuarioPrincipal principal) {
        asegurarAcceso(id, principal);
        Estudiante estudiante = estudianteRepository.findById(id)
                .orElseThrow(() -> EstudianteNoEncontradoException.porId(id));
        return estudianteMapper.toResponseDTO(estudiante);
    }

    private void validarPrincipal(UsuarioPrincipal principal) {
        if (principal == null || (principal.rol() != RolUsuario.ADMINISTRADOR
                && principal.rol() != RolUsuario.PROFESOR)) {
            throw new AccessDeniedException("El usuario no puede consultar estudiantes");
        }
    }

    private void asegurarAcceso(UUID estudianteId, UsuarioPrincipal principal) {
        if (principal == null) {
            throw new AccessDeniedException("No hay una identidad autenticada válida");
        }
        if (principal.rol() == RolUsuario.ADMINISTRADOR
                || (principal.rol() == RolUsuario.ESTUDIANTE && principal.id().equals(estudianteId))) {
            return;
        }
        if (principal.rol() == RolUsuario.PROFESOR
                && estudianteRepositoryVisibleToProfessor(estudianteId, principal.id())) {
            return;
        }
        throw new AccessDeniedException("El usuario no tiene acceso a este estudiante");
    }

    private boolean estudianteRepositoryVisibleToProfessor(UUID estudianteId, UUID profesorId) {
        return estudianteRepository.existsVisibleToProfessor(
                estudianteId, profesorId, EstadoRegistro.ACTIVO, EstadoRegistro.ACTIVO, EstadoRegistro.ACTIVO);
    }

    @Override
    @Transactional
    public EstudianteResponseDTO modificar(UUID id, EstudianteRequestDTO dto) {
        Estudiante estudiante = estudianteRepository.findById(id)
                .orElseThrow(() -> EstudianteNoEncontradoException.porId(id));

        if (!estudiante.getTipoDocumento().getId().equals(dto.tipoDocumentoId())
                || !estudiante.getNumeroDocumento().equals(dto.numeroDocumento())) {
            throw DocumentoNoModificableException.paraEstudiante();
        }

        String correo = Correos.normalizar(dto.correoInstitucional());
        if (!correo.equals(estudiante.getCorreoInstitucional())
                && usuarioRepository.existsByCorreoInstitucionalIgnoreCaseAndIdNot(correo, id)) {
            throw CorreoEstudianteDuplicadoException.porCorreo(correo);
        }

        estudianteMapper.updateEntityFromDTO(dto, estudiante);
        estudiante.setCorreoInstitucional(correo);

        return estudianteMapper.toResponseDTO(estudianteRepository.save(estudiante));
    }

    @Override
    @Transactional
    public void inactivar(UUID id) {
        Estudiante estudiante = estudianteRepository.findById(id)
                .orElseThrow(() -> EstudianteNoEncontradoException.porId(id));

        estudiante.setEstado(EstadoRegistro.INACTIVO);
        estudianteRepository.save(estudiante);
    }
}
