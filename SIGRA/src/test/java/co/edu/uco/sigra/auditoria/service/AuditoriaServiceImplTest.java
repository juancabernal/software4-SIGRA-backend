package co.edu.uco.sigra.auditoria.service;

import co.edu.uco.sigra.auditoria.config.AuditoriaConfig;
import co.edu.uco.sigra.auditoria.dto.PaginaAuditoriaDTO;
import co.edu.uco.sigra.auditoria.entity.AccionAuditoria;
import co.edu.uco.sigra.auditoria.entity.HistorialAuditoria;
import co.edu.uco.sigra.auditoria.entity.TipoEntidadAuditoria;
import co.edu.uco.sigra.auditoria.exception.RangoFechasInvalidoException;
import co.edu.uco.sigra.auditoria.mapper.AuditoriaMapper;
import co.edu.uco.sigra.auditoria.repository.HistorialAuditoriaRepository;
import co.edu.uco.sigra.auditoria.service.impl.AuditoriaServiceImpl;
import co.edu.uco.sigra.auth.security.UsuarioPrincipal;
import co.edu.uco.sigra.common.enums.RolUsuario;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/** Servicio de auditoría con repositorio simulado, mapper real y reloj fijo en hora de Colombia. */
@ExtendWith(MockitoExtension.class)
@DisplayName("Servicio de auditoría")
class AuditoriaServiceImplTest {

    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 10, 20, 9, 30);
    private static final UUID ID_ENTIDAD = UUID.randomUUID();

    @Mock
    private HistorialAuditoriaRepository repositorio;

    private final AuditoriaMapper mapper = Mappers.getMapper(AuditoriaMapper.class);

    private AuditoriaServiceImpl service;

    @BeforeEach
    void setUp() {
        Clock reloj = Clock.fixed(Instant.parse("2026-10-20T14:30:00Z"), AuditoriaConfig.ZONA_COLOMBIA);
        service = new AuditoriaServiceImpl(repositorio, mapper, reloj);
    }

    @AfterEach
    void limpiarSesion() {
        SecurityContextHolder.clearContext();
    }

    private HistorialAuditoria guardado() {
        ArgumentCaptor<HistorialAuditoria> captor = ArgumentCaptor.forClass(HistorialAuditoria.class);
        verify(repositorio).save(captor.capture());
        return captor.getValue();
    }

    @Nested
    @DisplayName("registrarEvento (RF-19)")
    class Registrar {

        @Test
        @DisplayName("Toma el usuario y el rol de la sesión y guarda entidad, acción, fecha y detalle")
        void conSesion() {
            UUID admin = UUID.randomUUID();
            SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                    new UsuarioPrincipal(admin, RolUsuario.ADMINISTRADOR), null, List.of()));

            service.registrarEvento(TipoEntidadAuditoria.ASIGNATURA, ID_ENTIDAD, AccionAuditoria.CAMBIO_ESTADO,
                    "  Cambio de estado: inactivar  ");

            HistorialAuditoria registro = guardado();
            assertThat(registro.getUsuarioId()).isEqualTo(admin);
            assertThat(registro.getRolUsuario()).isEqualTo(RolUsuario.ADMINISTRADOR);
            assertThat(registro.getTipoEntidad()).isEqualTo(TipoEntidadAuditoria.ASIGNATURA);
            assertThat(registro.getIdEntidad()).isEqualTo(ID_ENTIDAD);
            assertThat(registro.getAccion()).isEqualTo(AccionAuditoria.CAMBIO_ESTADO);
            assertThat(registro.getFechaHora()).isEqualTo(AHORA);
            assertThat(registro.getDetalle()).isEqualTo("Cambio de estado: inactivar");
        }

        @Test
        @DisplayName("Sin sesión guarda el evento sin usuario")
        void sinSesion() {
            service.registrarEvento(TipoEntidadAuditoria.PROGRAMA, ID_ENTIDAD, AccionAuditoria.CAMBIO_ESTADO, "x");

            HistorialAuditoria registro = guardado();
            assertThat(registro.getUsuarioId()).isNull();
            assertThat(registro.getRolUsuario()).isNull();
        }

        @Test
        @DisplayName("Con usuario explícito (login) usa ese usuario aunque no haya sesión")
        void usuarioExplicito() {
            UUID usuario = UUID.randomUUID();

            service.registrarEvento(TipoEntidadAuditoria.USUARIO, usuario, AccionAuditoria.LOGIN_EXITOSO,
                    "Inicio de sesión exitoso", usuario, RolUsuario.PROFESOR);

            HistorialAuditoria registro = guardado();
            assertThat(registro.getUsuarioId()).isEqualTo(usuario);
            assertThat(registro.getRolUsuario()).isEqualTo(RolUsuario.PROFESOR);
        }

        @Test
        @DisplayName("Recorta el detalle a 500 caracteres y nunca lo deja vacío")
        void detalle() {
            service.registrarEvento(TipoEntidadAuditoria.OTRO, null, AccionAuditoria.CAMBIO_ESTADO, "x".repeat(600));
            assertThat(guardado().getDetalle()).hasSize(HistorialAuditoria.DETALLE_MAX);

            reset(repositorio);
            service.registrarEvento(TipoEntidadAuditoria.OTRO, null, AccionAuditoria.CAMBIO_ESTADO, "   ");
            assertThat(guardado().getDetalle()).isEqualTo("Sin detalle");
        }

        @Test
        @DisplayName("Tipo de entidad y acción son obligatorios")
        void obligatorios() {
            assertThatThrownBy(() -> service.registrarEvento(null, ID_ENTIDAD, AccionAuditoria.CAMBIO_ESTADO, "x"))
                    .isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> service.registrarEvento(TipoEntidadAuditoria.OTRO, ID_ENTIDAD, null, "x"))
                    .isInstanceOf(NullPointerException.class);
            verifyNoInteractions(repositorio);
        }
    }

    @Nested
    @DisplayName("consultar (RF-21c)")
    class Consultar {

        private final HistorialAuditoria registro = new HistorialAuditoria(AHORA, null, null,
                AccionAuditoria.CAMBIO_ESTADO, TipoEntidadAuditoria.ASIGNATURA, ID_ENTIDAD, "Cambio de estado");

        @Test
        @DisplayName("Sin filtros: rango amplio, del más reciente al más antiguo, y devuelve los datos de página")
        void sinFiltros() {
            when(repositorio.findByFechaHoraBetween(any(), any(), any()))
                    .thenAnswer(inv -> new PageImpl<>(List.of(registro), inv.getArgument(2, Pageable.class), 41));

            PaginaAuditoriaDTO pagina = service.consultar(null, null, null, 2, 20);

            ArgumentCaptor<Pageable> solicitud = ArgumentCaptor.forClass(Pageable.class);
            verify(repositorio).findByFechaHoraBetween(any(), any(), solicitud.capture());
            assertThat(solicitud.getValue().getPageNumber()).isEqualTo(2);
            assertThat(solicitud.getValue().getPageSize()).isEqualTo(20);
            assertThat(solicitud.getValue().getSort().getOrderFor("fechaHora").getDirection())
                    .isEqualTo(Sort.Direction.DESC);
            assertThat(pagina.contenido()).hasSize(1);
            assertThat(pagina.contenido().get(0).accion()).isEqualTo("CAMBIO_ESTADO");
            assertThat(pagina.totalElementos()).isEqualTo(41);
            assertThat(pagina.totalPaginas()).isEqualTo(3);
            verify(repositorio, never()).findByTipoEntidadAndFechaHoraBetween(any(), any(), any(), any());
        }

        @Test
        @DisplayName("Rango de fechas inclusivo: desde el inicio del primer día hasta el final del último")
        void rangoInclusivo() {
            when(repositorio.findByTipoEntidadAndFechaHoraBetween(any(), any(), any(), any()))
                    .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 10), 0));

            service.consultar(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 20),
                    TipoEntidadAuditoria.CALIFICACION, 0, 10);

            verify(repositorio).findByTipoEntidadAndFechaHoraBetween(eq(TipoEntidadAuditoria.CALIFICACION),
                    eq(LocalDateTime.of(2026, 10, 1, 0, 0)),
                    eq(LocalDateTime.of(2026, 10, 20, 23, 59, 59, 999_999_000)), any());
        }

        @Test
        @DisplayName("Fecha inicial posterior a la final: RangoFechasInvalidoException")
        void rangoInvertido() {
            assertThatThrownBy(() -> service.consultar(LocalDate.of(2026, 10, 21), LocalDate.of(2026, 10, 20),
                    null, 0, 10)).isInstanceOf(RangoFechasInvalidoException.class);
            verifyNoInteractions(repositorio);
        }
    }
}