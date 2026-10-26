package co.edu.uco.sigra.asignaturas.service;

import co.edu.uco.sigra.asignaturas.dto.AsignacionMasivaRequestDTO;
import co.edu.uco.sigra.asignaturas.dto.ResultadoAsignacionMasivaDTO;
import co.edu.uco.sigra.asignaturas.entity.AsignacionDocente;
import co.edu.uco.sigra.asignaturas.entity.Asignatura;
import co.edu.uco.sigra.asignaturas.entity.EstadoAsignatura;
import co.edu.uco.sigra.asignaturas.exception.AsignacionDocenteNoEncontradaException;
import co.edu.uco.sigra.asignaturas.exception.AsignaturaNoEncontradaException;
import co.edu.uco.sigra.asignaturas.mapper.AsignacionDocenteMapper;
import co.edu.uco.sigra.asignaturas.repository.AsignacionDocenteRepository;
import co.edu.uco.sigra.asignaturas.repository.AsignaturaRepository;
import co.edu.uco.sigra.asignaturas.service.impl.AsignacionDocenteServiceImpl;
import co.edu.uco.sigra.common.enums.EstadoRegistro;
import co.edu.uco.sigra.profesores.entity.Profesor;
import co.edu.uco.sigra.profesores.exception.ProfesorNoEncontradoException;
import co.edu.uco.sigra.profesores.repository.ProfesorRepository;
import co.edu.uco.sigra.programas.entity.ProgramaAcademico;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Servicio de asignación docente (RF-07)")
class AsignacionDocenteServiceImplTest {

    @Mock
    private AsignacionDocenteRepository asignacionRepository;

    @Mock
    private ProfesorRepository profesorRepository;

    @Mock
    private AsignaturaRepository asignaturaRepository;

    private final AsignacionDocenteMapper mapper = Mappers.getMapper(AsignacionDocenteMapper.class);

    private AsignacionDocenteServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new AsignacionDocenteServiceImpl(asignacionRepository, profesorRepository, asignaturaRepository, mapper);
    }

    private static Profesor profesor(EstadoRegistro estado) {
        Profesor p = new Profesor();
        ReflectionTestUtils.setField(p, "id", UUID.randomUUID());
        p.setNombreCompleto("Profesor de Prueba");
        p.setEstado(estado);
        return p;
    }

    private static Asignatura asignatura(EstadoAsignatura estado) {
        ProgramaAcademico programa = new ProgramaAcademico();
        programa.setNombre("Programa de Prueba");
        programa.setCodigo("PRG1");
        Asignatura a = new Asignatura("ASIG01", "Asignatura de Prueba", programa);
        ReflectionTestUtils.setField(a, "id", UUID.randomUUID());
        if (estado != EstadoAsignatura.BORRADOR) {
            a.activar(Asignatura.MIN_RA_ACTIVOS);
        }
        if (estado == EstadoAsignatura.INACTIVA) {
            a.inactivar();
        }
        return a;
    }

    private static AsignacionDocente asignacion(Profesor profesor, Asignatura asignatura, EstadoRegistro estado) {
        AsignacionDocente asignacion = new AsignacionDocente();
        ReflectionTestUtils.setField(asignacion, "id", UUID.randomUUID());
        asignacion.setProfesor(profesor);
        asignacion.setAsignatura(asignatura);
        asignacion.setEstado(estado);
        return asignacion;
    }

    @Nested
    @DisplayName("Asignar masivamente")
    class AsignarMasivo {

        @Test
        @DisplayName("asigna cada profesor a cada asignatura (cartesiano)")
        void asignaCadaProfesorACadaAsignatura() {
            Profesor p1 = profesor(EstadoRegistro.ACTIVO);
            Profesor p2 = profesor(EstadoRegistro.ACTIVO);
            Asignatura a1 = asignatura(EstadoAsignatura.ACTIVA);
            Asignatura a2 = asignatura(EstadoAsignatura.ACTIVA);

            when(profesorRepository.findById(p1.getId())).thenReturn(Optional.of(p1));
            when(profesorRepository.findById(p2.getId())).thenReturn(Optional.of(p2));
            when(asignaturaRepository.findById(a1.getId())).thenReturn(Optional.of(a1));
            when(asignaturaRepository.findById(a2.getId())).thenReturn(Optional.of(a2));
            when(asignacionRepository.findByProfesor_IdAndAsignatura_Id(any(), any())).thenReturn(Optional.empty());
            when(asignacionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            var dto = new AsignacionMasivaRequestDTO(List.of(p1.getId(), p2.getId()), List.of(a1.getId(), a2.getId()));
            ResultadoAsignacionMasivaDTO resultado = service.asignarMasivo(dto);

            assertThat(resultado.creadas()).hasSize(4);
            assertThat(resultado.omitidas()).isEmpty();
            assertThat(resultado.rechazadas()).isEmpty();
            verify(asignacionRepository, org.mockito.Mockito.times(4)).save(any());
        }

        @Test
        @DisplayName("normaliza IDs repetidos en la solicitud sin informar omisiones artificiales")
        void normalizaIdsRepetidosDeLaSolicitud() {
            Profesor profesor = profesor(EstadoRegistro.ACTIVO);
            Asignatura asignatura = asignatura(EstadoAsignatura.ACTIVA);

            when(profesorRepository.findById(profesor.getId())).thenReturn(Optional.of(profesor));
            when(asignaturaRepository.findById(asignatura.getId())).thenReturn(Optional.of(asignatura));
            when(asignacionRepository.findByProfesor_IdAndAsignatura_Id(profesor.getId(), asignatura.getId()))
                    .thenReturn(Optional.empty());
            when(asignacionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            var dto = new AsignacionMasivaRequestDTO(
                    List.of(profesor.getId(), profesor.getId()),
                    List.of(asignatura.getId(), asignatura.getId()));
            ResultadoAsignacionMasivaDTO resultado = service.asignarMasivo(dto);

            assertThat(resultado.creadas()).hasSize(1);
            assertThat(resultado.omitidas()).isEmpty();
            assertThat(resultado.rechazadas()).isEmpty();
            verify(asignacionRepository).save(any());
        }

        @Test
        @DisplayName("omite un par ya asignado (duplicado) y lo informa, sin crear otra fila")
        void omiteDuplicado() {
            Profesor profesor = profesor(EstadoRegistro.ACTIVO);
            Asignatura asignatura = asignatura(EstadoAsignatura.ACTIVA);
            AsignacionDocente existente = asignacion(profesor, asignatura, EstadoRegistro.ACTIVO);

            when(profesorRepository.findById(profesor.getId())).thenReturn(Optional.of(profesor));
            when(asignaturaRepository.findById(asignatura.getId())).thenReturn(Optional.of(asignatura));
            when(asignacionRepository.findByProfesor_IdAndAsignatura_Id(profesor.getId(), asignatura.getId()))
                    .thenReturn(Optional.of(existente));

            var dto = new AsignacionMasivaRequestDTO(List.of(profesor.getId()), List.of(asignatura.getId()));
            ResultadoAsignacionMasivaDTO resultado = service.asignarMasivo(dto);

            assertThat(resultado.creadas()).isEmpty();
            assertThat(resultado.omitidas()).hasSize(1);
            assertThat(resultado.omitidas().get(0).profesorId()).isEqualTo(profesor.getId());
            assertThat(resultado.omitidas().get(0).asignaturaId()).isEqualTo(asignatura.getId());
            assertThat(resultado.rechazadas()).isEmpty();
            verify(asignacionRepository, never()).save(any());
        }

        @Test
        @DisplayName("reactiva una asignación INACTIVA existente en vez de crear una fila nueva")
        void reactivaAsignacionInactiva() {
            Profesor profesor = profesor(EstadoRegistro.ACTIVO);
            Asignatura asignatura = asignatura(EstadoAsignatura.ACTIVA);
            AsignacionDocente existente = asignacion(profesor, asignatura, EstadoRegistro.INACTIVO);
            UUID idExistente = existente.getId();

            when(profesorRepository.findById(profesor.getId())).thenReturn(Optional.of(profesor));
            when(asignaturaRepository.findById(asignatura.getId())).thenReturn(Optional.of(asignatura));
            when(asignacionRepository.findByProfesor_IdAndAsignatura_Id(profesor.getId(), asignatura.getId()))
                    .thenReturn(Optional.of(existente));
            when(asignacionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            var dto = new AsignacionMasivaRequestDTO(List.of(profesor.getId()), List.of(asignatura.getId()));
            ResultadoAsignacionMasivaDTO resultado = service.asignarMasivo(dto);

            assertThat(resultado.creadas()).hasSize(1);
            assertThat(resultado.creadas().get(0).id()).isEqualTo(idExistente);
            assertThat(existente.getEstado()).isEqualTo(EstadoRegistro.ACTIVO);
            verify(asignacionRepository).save(existente);
        }

        @Test
        @DisplayName("rechaza un profesor inactivo y lo informa, sin crear nada")
        void rechazaProfesorInactivo() {
            Profesor profesor = profesor(EstadoRegistro.INACTIVO);
            Asignatura asignatura = asignatura(EstadoAsignatura.ACTIVA);

            when(profesorRepository.findById(profesor.getId())).thenReturn(Optional.of(profesor));
            when(asignaturaRepository.findById(asignatura.getId())).thenReturn(Optional.of(asignatura));

            var dto = new AsignacionMasivaRequestDTO(List.of(profesor.getId()), List.of(asignatura.getId()));
            ResultadoAsignacionMasivaDTO resultado = service.asignarMasivo(dto);

            assertThat(resultado.creadas()).isEmpty();
            assertThat(resultado.rechazadas()).hasSize(1);
            assertThat(resultado.rechazadas().get(0).motivo()).containsIgnoringCase("profesor");
            verify(asignacionRepository, never()).save(any());
        }

        @Test
        @DisplayName("rechaza una asignatura que no está ACTIVA (BORRADOR) y lo informa, sin crear nada")
        void rechazaAsignaturaNoActiva() {
            Profesor profesor = profesor(EstadoRegistro.ACTIVO);
            Asignatura asignatura = asignatura(EstadoAsignatura.BORRADOR);

            when(profesorRepository.findById(profesor.getId())).thenReturn(Optional.of(profesor));
            when(asignaturaRepository.findById(asignatura.getId())).thenReturn(Optional.of(asignatura));

            var dto = new AsignacionMasivaRequestDTO(List.of(profesor.getId()), List.of(asignatura.getId()));
            ResultadoAsignacionMasivaDTO resultado = service.asignarMasivo(dto);

            assertThat(resultado.creadas()).isEmpty();
            assertThat(resultado.rechazadas()).hasSize(1);
            assertThat(resultado.rechazadas().get(0).motivo()).containsIgnoringCase("asignatura");
            verify(asignacionRepository, never()).save(any());
        }

        @Test
        @DisplayName("rechaza una asignatura INACTIVA y lo informa, sin crear nada")
        void rechazaAsignaturaInactiva() {
            Profesor profesor = profesor(EstadoRegistro.ACTIVO);
            Asignatura asignatura = asignatura(EstadoAsignatura.INACTIVA);

            when(profesorRepository.findById(profesor.getId())).thenReturn(Optional.of(profesor));
            when(asignaturaRepository.findById(asignatura.getId())).thenReturn(Optional.of(asignatura));

            var dto = new AsignacionMasivaRequestDTO(List.of(profesor.getId()), List.of(asignatura.getId()));
            ResultadoAsignacionMasivaDTO resultado = service.asignarMasivo(dto);

            assertThat(resultado.rechazadas()).hasSize(1);
            verify(asignacionRepository, never()).save(any());
        }

        @Test
        @DisplayName("un profesor inexistente lanza ProfesorNoEncontradoException y no procesa nada")
        void profesorInexistenteLanzaExcepcion() {
            UUID idInexistente = UUID.randomUUID();
            Asignatura asignatura = asignatura(EstadoAsignatura.ACTIVA);
            when(profesorRepository.findById(idInexistente)).thenReturn(Optional.empty());

            var dto = new AsignacionMasivaRequestDTO(List.of(idInexistente), List.of(asignatura.getId()));

            assertThatThrownBy(() -> service.asignarMasivo(dto))
                    .isInstanceOf(ProfesorNoEncontradoException.class);
            verify(asignacionRepository, never()).save(any());
        }

        @Test
        @DisplayName("una asignatura inexistente lanza AsignaturaNoEncontradaException y no procesa nada")
        void asignaturaInexistenteLanzaExcepcion() {
            Profesor profesor = profesor(EstadoRegistro.ACTIVO);
            UUID idInexistente = UUID.randomUUID();
            when(profesorRepository.findById(profesor.getId())).thenReturn(Optional.of(profesor));
            when(asignaturaRepository.findById(idInexistente)).thenReturn(Optional.empty());

            var dto = new AsignacionMasivaRequestDTO(List.of(profesor.getId()), List.of(idInexistente));

            assertThatThrownBy(() -> service.asignarMasivo(dto))
                    .isInstanceOf(AsignaturaNoEncontradaException.class);
            verify(asignacionRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Desasignar")
    class Desasignar {

        @Test
        @DisplayName("cambia el estado a INACTIVO")
        void desasignaCambiaEstado() {
            Profesor profesor = profesor(EstadoRegistro.ACTIVO);
            Asignatura asignatura = asignatura(EstadoAsignatura.ACTIVA);
            AsignacionDocente existente = asignacion(profesor, asignatura, EstadoRegistro.ACTIVO);
            when(asignacionRepository.findById(existente.getId())).thenReturn(Optional.of(existente));

            service.desasignar(existente.getId());

            assertThat(existente.getEstado()).isEqualTo(EstadoRegistro.INACTIVO);
            verify(asignacionRepository).save(existente);
        }

        @Test
        @DisplayName("es idempotente si ya estaba INACTIVO: no vuelve a guardar")
        void desasignarYaInactivaEsIdempotente() {
            Profesor profesor = profesor(EstadoRegistro.ACTIVO);
            Asignatura asignatura = asignatura(EstadoAsignatura.ACTIVA);
            AsignacionDocente existente = asignacion(profesor, asignatura, EstadoRegistro.INACTIVO);
            when(asignacionRepository.findById(existente.getId())).thenReturn(Optional.of(existente));

            service.desasignar(existente.getId());

            assertThat(existente.getEstado()).isEqualTo(EstadoRegistro.INACTIVO);
            verify(asignacionRepository, never()).save(any());
        }

        @Test
        @DisplayName("una asignación inexistente lanza AsignacionDocenteNoEncontradaException")
        void desasignarInexistenteLanzaExcepcion() {
            UUID idInexistente = UUID.randomUUID();
            when(asignacionRepository.findById(idInexistente)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.desasignar(idInexistente))
                    .isInstanceOf(AsignacionDocenteNoEncontradaException.class);
        }
    }
}
