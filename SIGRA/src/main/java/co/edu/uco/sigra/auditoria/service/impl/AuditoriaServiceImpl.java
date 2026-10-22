package co.edu.uco.sigra.auditoria.service.impl;

import co.edu.uco.sigra.auditoria.config.AuditoriaConfig;
import co.edu.uco.sigra.auditoria.dto.PaginaAuditoriaDTO;
import co.edu.uco.sigra.auditoria.entity.AccionAuditoria;
import co.edu.uco.sigra.auditoria.entity.HistorialAuditoria;
import co.edu.uco.sigra.auditoria.entity.TipoEntidadAuditoria;
import co.edu.uco.sigra.auditoria.exception.RangoFechasInvalidoException;
import co.edu.uco.sigra.auditoria.mapper.AuditoriaMapper;
import co.edu.uco.sigra.auditoria.repository.HistorialAuditoriaRepository;
import co.edu.uco.sigra.auditoria.service.AuditoriaService;
import co.edu.uco.sigra.auth.security.UsuarioPrincipal;
import co.edu.uco.sigra.common.enums.RolUsuario;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * Implementación del historial de auditoría.
 * <p>
 * {@code registrarEvento} se une a la transacción en curso si la hay (el evento se confirma o se
 * deshace junto con el cambio que lo originó) o abre una propia si se llama después de confirmar.
 */
@Service
public class AuditoriaServiceImpl implements AuditoriaService {

    /** Límites del filtro cuando no se indica fecha: cubren cualquier dato real del sistema. */
    private static final LocalDateTime INICIO_SIN_FILTRO = LocalDateTime.of(2000, 1, 1, 0, 0);
    private static final LocalDateTime FIN_SIN_FILTRO = LocalDateTime.of(9999, 12, 31, 23, 59, 59);
    /** Último instante del día con la precisión de PostgreSQL (microsegundos), sin redondear al día siguiente. */
    private static final int MICROSEGUNDO_FINAL_EN_NANOS = 999_999_000;

    private final HistorialAuditoriaRepository repositorio;
    private final AuditoriaMapper mapper;
    private final Clock reloj;

    public AuditoriaServiceImpl(HistorialAuditoriaRepository repositorio, AuditoriaMapper mapper,
                                @Qualifier(AuditoriaConfig.RELOJ_AUDITORIA) Clock reloj) {
        this.repositorio = repositorio;
        this.mapper = mapper;
        this.reloj = reloj;
    }

    @Override
    @Transactional
    public void registrarEvento(TipoEntidadAuditoria tipoEntidad, UUID idEntidad, AccionAuditoria accion,
                                String detalle) {
        UsuarioPrincipal principal = principalActual();
        registrarEvento(tipoEntidad, idEntidad, accion, detalle,
                principal == null ? null : principal.id(),
                principal == null ? null : principal.rol());
    }

    @Override
    @Transactional
    public void registrarEvento(TipoEntidadAuditoria tipoEntidad, UUID idEntidad, AccionAuditoria accion,
                                String detalle, UUID usuarioId, RolUsuario rolUsuario) {
        Objects.requireNonNull(tipoEntidad, "tipoEntidad");
        Objects.requireNonNull(accion, "accion");
        repositorio.save(new HistorialAuditoria(LocalDateTime.now(reloj), usuarioId, rolUsuario, accion,
                tipoEntidad, idEntidad, recortar(detalle)));
    }

    @Override
    @Transactional(readOnly = true)
    public PaginaAuditoriaDTO consultar(LocalDate desde, LocalDate hasta, TipoEntidadAuditoria tipoEntidad,
                                        int pagina, int tamano) {
        if (desde != null && hasta != null && desde.isAfter(hasta)) {
            throw new RangoFechasInvalidoException();
        }
        LocalDateTime inicio = desde == null ? INICIO_SIN_FILTRO : desde.atStartOfDay();
        LocalDateTime fin = hasta == null ? FIN_SIN_FILTRO : hasta.atTime(23, 59, 59, MICROSEGUNDO_FINAL_EN_NANOS);
        Pageable solicitud = PageRequest.of(pagina, tamano,
                Sort.by(Sort.Order.desc("fechaHora"), Sort.Order.desc("id")));

        Page<HistorialAuditoria> resultado = tipoEntidad == null
                ? repositorio.findByFechaHoraBetween(inicio, fin, solicitud)
                : repositorio.findByTipoEntidadAndFechaHoraBetween(tipoEntidad, inicio, fin, solicitud);

        return new PaginaAuditoriaDTO(resultado.getContent().stream().map(mapper::toDTO).toList(),
                resultado.getNumber(), resultado.getSize(), resultado.getTotalElements(),
                resultado.getTotalPages());
    }

    private static UsuarioPrincipal principalActual() {
        Authentication autenticacion = SecurityContextHolder.getContext().getAuthentication();
        return autenticacion != null && autenticacion.getPrincipal() instanceof UsuarioPrincipal principal
                ? principal : null;
    }

    private static String recortar(String detalle) {
        String texto = detalle == null || detalle.isBlank() ? "Sin detalle" : detalle.strip();
        return texto.length() <= HistorialAuditoria.DETALLE_MAX
                ? texto : texto.substring(0, HistorialAuditoria.DETALLE_MAX);
    }
}