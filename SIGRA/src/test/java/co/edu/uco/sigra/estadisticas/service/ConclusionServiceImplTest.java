package co.edu.uco.sigra.estadisticas.service;

import co.edu.uco.sigra.asignaturas.entity.Asignatura;
import co.edu.uco.sigra.asignaturas.entity.AsignacionDocente;
import co.edu.uco.sigra.asignaturas.repository.AsignacionDocenteRepository;
import co.edu.uco.sigra.auth.security.UsuarioPrincipal;
import co.edu.uco.sigra.common.enums.EstadoRegistro;
import co.edu.uco.sigra.common.enums.RolUsuario;
import co.edu.uco.sigra.estadisticas.dto.ConclusionEstadoResponseDTO;
import co.edu.uco.sigra.estadisticas.dto.ConclusionFiltroDTO;
import co.edu.uco.sigra.estadisticas.dto.EstadoConsultaConclusion;
import co.edu.uco.sigra.estadisticas.dto.GenerarConclusionResponseDTO;
import co.edu.uco.sigra.estadisticas.entity.Conclusion;
import co.edu.uco.sigra.estadisticas.entity.EstadoConclusion;
import co.edu.uco.sigra.estadisticas.exception.ConclusionNoEncontradaException;
import co.edu.uco.sigra.estadisticas.exception.PermisoInsuficienteException;
import co.edu.uco.sigra.estadisticas.exception.SemestreNoEncontradoException;
import co.edu.uco.sigra.estadisticas.mapper.ConclusionMapper;
import co.edu.uco.sigra.estadisticas.repository.ConclusionDiagnosticoRepository;
import co.edu.uco.sigra.estadisticas.repository.ConclusionRepository;
import co.edu.uco.sigra.estadisticas.service.impl.ConclusionServiceImpl;
import co.edu.uco.sigra.programas.entity.ProgramaAcademico;
import co.edu.uco.sigra.resultadosaprendizaje.entity.ResultadoAprendizaje;
import co.edu.uco.sigra.resultadosaprendizaje.exception.ResultadoAprendizajeNoEncontradoException;
import co.edu.uco.sigra.resultadosaprendizaje.repository.ResultadoAprendizajeRepository;
import co.edu.uco.sigra.semestres.entity.Semestre;
import co.edu.uco.sigra.semestres.repository.SemestreRepository;
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
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Pruebas unitarias del servicio de conclusiones automáticas (RF-16). Los repositorios y el
 * proveedor de niveles de logro se simulan con Mockito; el mapper es el real generado por MapStruct.
 * Cubre los casos del enunciado: datos insuficientes, 40% (no genera), 50% (genera), conclusión
 * Vigente previa (no duplica) y un semestre nuevo (genera otra).
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Servicio de conclusiones automáticas (RF-16)")
class ConclusionServiceImplTest {

    private static final UsuarioPrincipal ADMIN =
            new UsuarioPrincipal(UUID.randomUUID(), RolUsuario.ADMINISTRADOR);

    private static final UUID ID_RA = UUID.randomUUID();
    private static final UUID ID_ASIGNATURA = UUID.randomUUID();
    private static final UUID ID_SEMESTRE = UUID.randomUUID();
    private static final UUID ID_SEMESTRE_2 = UUID.randomUUID();

    @Mock
    private ConclusionRepository conclusionRepository;
    @Mock
    private ResultadoAprendizajeRepository resultadoAprendizajeRepository;
    @Mock
    private SemestreRepository semestreRepository;
    @Mock
    private AsignacionDocenteRepository asignacionDocenteRepository;
    @Mock
    private ProveedorNivelesLogro proveedorNivelesLogro;
    @Mock
    private ConclusionDiagnosticoRepository conclusionDiagnosticoRepository;

    private final ConclusionMapper mapper = Mappers.getMapper(ConclusionMapper.class);

    private ConclusionServiceImpl service;
    private Asignatura asignatura;
    private ResultadoAprendizaje resultadoAprendizaje;
    private Semestre semestre;
    private Semestre semestre2;

    @BeforeEach
    void setUp() {
        service = new ConclusionServiceImpl(conclusionRepository, resultadoAprendizajeRepository,
                semestreRepository, asignacionDocenteRepository, proveedorNivelesLogro,
                conclusionDiagnosticoRepository, mapper);

        ProgramaAcademico programa = new ProgramaAcademico();
        programa.setId(UUID.randomUUID());
        programa.setNombre("Ingeniería de Sistemas");
        asignatura = new Asignatura("ISW4", "Ingeniería de Software IV", programa);
        ReflectionTestUtils.setField(asignatura, "id", ID_ASIGNATURA);

        resultadoAprendizaje = new ResultadoAprendizaje();
        resultadoAprendizaje.setId(ID_RA);
        resultadoAprendizaje.setAsignatura(asignatura);
        resultadoAprendizaje.setCodigo("RA-01");
        resultadoAprendizaje.setDescripcion("Analiza los requisitos de un sistema");
        resultadoAprendizaje.setEstado(EstadoRegistro.ACTIVO);

        semestre = new Semestre();
        semestre.setId(ID_SEMESTRE);
        semestre.setCodigo("2025-2");

        semestre2 = new Semestre();
        semestre2.setId(ID_SEMESTRE_2);
        semestre2.setCodigo("2026-1");
    }

    private void evaluados(UUID semestreId, long total, long enBajoOMedio) {
        when(proveedorNivelesLogro.contarEvaluados(ID_RA, semestreId)).thenReturn(total);
        when(proveedorNivelesLogro.contarEnBajoOMedio(ID_RA, semestreId)).thenReturn(enBajoOMedio);
    }

    private void sinVigentePrevia(UUID semestreId) {
        when(conclusionRepository.findByResultadoAprendizaje_IdAndSemestre_IdAndEstado(
                ID_RA, semestreId, EstadoConclusion.VIGENTE)).thenReturn(Optional.empty());
    }

    @Nested
    @DisplayName("generarConclusion (RF-16)")
    class Generar {

        @Test
        @DisplayName("Con 2 calificaciones (menos del mínimo de 3) no genera por datos insuficientes")
        void datosInsuficientes() {
            when(resultadoAprendizajeRepository.findById(ID_RA)).thenReturn(Optional.of(resultadoAprendizaje));
            when(semestreRepository.findById(ID_SEMESTRE)).thenReturn(Optional.of(semestre));
            // Con menos del mínimo se corta antes de consultar contarEnBajoOMedio: solo se stubea contarEvaluados.
            when(proveedorNivelesLogro.contarEvaluados(ID_RA, ID_SEMESTRE)).thenReturn(2L);

            GenerarConclusionResponseDTO respuesta = service.generarConclusion(ID_RA, ID_SEMESTRE, ADMIN);

            assertThat(respuesta.generada()).isFalse();
            assertThat(respuesta.motivo()).isEqualTo(ConclusionServiceImpl.MOTIVO_DATOS_INSUFICIENTES);
            assertThat(respuesta.conclusion()).isNull();
            verify(conclusionRepository, never()).save(any());
            // Con datos insuficientes ni siquiera se consulta si ya hay una Vigente.
            verify(conclusionRepository, never()).findByResultadoAprendizaje_IdAndSemestre_IdAndEstado(any(), any(), any());
        }

        @Test
        @DisplayName("Con 10 estudiantes y 4 en Bajo/Medio (40%) no genera: el umbral es estricto")
        void cuarentaPorCientoNoGenera() {
            when(resultadoAprendizajeRepository.findById(ID_RA)).thenReturn(Optional.of(resultadoAprendizaje));
            when(semestreRepository.findById(ID_SEMESTRE)).thenReturn(Optional.of(semestre));
            sinVigentePrevia(ID_SEMESTRE);
            evaluados(ID_SEMESTRE, 10, 4);

            GenerarConclusionResponseDTO respuesta = service.generarConclusion(ID_RA, ID_SEMESTRE, ADMIN);

            assertThat(respuesta.generada()).isFalse();
            assertThat(respuesta.motivo()).isEqualTo(ConclusionServiceImpl.MOTIVO_DENTRO_DEL_UMBRAL);
            verify(conclusionRepository, never()).save(any());
        }

        @Test
        @DisplayName("Con 10 estudiantes y 5 en Bajo/Medio (50%) genera una conclusión Vigente")
        void cincuentaPorCientoGenera() {
            when(resultadoAprendizajeRepository.findById(ID_RA)).thenReturn(Optional.of(resultadoAprendizaje));
            when(semestreRepository.findById(ID_SEMESTRE)).thenReturn(Optional.of(semestre));
            when(conclusionRepository.save(any(Conclusion.class))).thenAnswer(inv -> inv.getArgument(0));
            sinVigentePrevia(ID_SEMESTRE);
            evaluados(ID_SEMESTRE, 10, 5);

            GenerarConclusionResponseDTO respuesta = service.generarConclusion(ID_RA, ID_SEMESTRE, ADMIN);

            assertThat(respuesta.generada()).isTrue();
            assertThat(respuesta.motivo()).isEqualTo(ConclusionServiceImpl.MOTIVO_GENERADA);
            assertThat(respuesta.conclusion()).isNotNull();
            assertThat(respuesta.conclusion().estado()).isEqualTo(EstadoConclusion.VIGENTE.name());
            assertThat(respuesta.conclusion().mensaje()).isEqualTo(Conclusion.MENSAJE_INSUFICIENTE);
            assertThat(respuesta.conclusion().porcentajeInsuficiente()).isEqualTo(50.0);
            assertThat(respuesta.conclusion().resultadoAprendizajeId()).isEqualTo(ID_RA);
            assertThat(respuesta.conclusion().asignaturaId()).isEqualTo(ID_ASIGNATURA);
            assertThat(respuesta.conclusion().semestreId()).isEqualTo(ID_SEMESTRE);
            verify(conclusionRepository).save(any(Conclusion.class));
        }

        @Test
        @DisplayName("Con una conclusión Vigente previa para el mismo (RA, semestre) no duplica")
        void yaExisteVigenteNoDuplica() {
            when(resultadoAprendizajeRepository.findById(ID_RA)).thenReturn(Optional.of(resultadoAprendizaje));
            when(semestreRepository.findById(ID_SEMESTRE)).thenReturn(Optional.of(semestre));
            // Se corta por la Vigente previa antes de llegar a contarEnBajoOMedio: solo se stubea contarEvaluados.
            when(proveedorNivelesLogro.contarEvaluados(ID_RA, ID_SEMESTRE)).thenReturn(10L);
            Conclusion existente = new Conclusion(resultadoAprendizaje, semestre, 70.0);
            when(conclusionRepository.findByResultadoAprendizaje_IdAndSemestre_IdAndEstado(
                    ID_RA, ID_SEMESTRE, EstadoConclusion.VIGENTE)).thenReturn(Optional.of(existente));

            GenerarConclusionResponseDTO respuesta = service.generarConclusion(ID_RA, ID_SEMESTRE, ADMIN);

            assertThat(respuesta.generada()).isFalse();
            assertThat(respuesta.motivo()).isEqualTo(ConclusionServiceImpl.MOTIVO_YA_VIGENTE);
            verify(conclusionRepository, never()).save(any());
        }

        @Test
        @DisplayName("En un semestre nuevo, aunque haya una Atendida en uno anterior, se genera otra")
        void otroSemestreGeneraNueva() {
            when(resultadoAprendizajeRepository.findById(ID_RA)).thenReturn(Optional.of(resultadoAprendizaje));
            when(semestreRepository.findById(ID_SEMESTRE_2)).thenReturn(Optional.of(semestre2));
            when(conclusionRepository.save(any(Conclusion.class))).thenAnswer(inv -> inv.getArgument(0));
            // La Vigente previa (si la hay) es de otro semestre: la búsqueda del semestre nuevo no la encuentra.
            sinVigentePrevia(ID_SEMESTRE_2);
            evaluados(ID_SEMESTRE_2, 10, 6);

            GenerarConclusionResponseDTO respuesta = service.generarConclusion(ID_RA, ID_SEMESTRE_2, ADMIN);

            assertThat(respuesta.generada()).isTrue();
            assertThat(respuesta.conclusion().semestreId()).isEqualTo(ID_SEMESTRE_2);
            verify(conclusionRepository).save(any(Conclusion.class));
        }

        @Test
        @DisplayName("Con un resultado de aprendizaje inexistente falla y no evalúa nada")
        void raInexistente() {
            when(resultadoAprendizajeRepository.findById(ID_RA)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.generarConclusion(ID_RA, ID_SEMESTRE, ADMIN))
                    .isInstanceOf(ResultadoAprendizajeNoEncontradoException.class);
            verifyNoInteractions(proveedorNivelesLogro);
        }

        @Test
        @DisplayName("Con un semestre inexistente falla y no evalúa nada")
        void semestreInexistente() {
            when(resultadoAprendizajeRepository.findById(ID_RA)).thenReturn(Optional.of(resultadoAprendizaje));
            when(semestreRepository.findById(ID_SEMESTRE)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.generarConclusion(ID_RA, ID_SEMESTRE, ADMIN))
                    .isInstanceOf(SemestreNoEncontradoException.class);
            verifyNoInteractions(proveedorNivelesLogro);
        }
    }

    @Nested
    @DisplayName("consultarConclusiones: alcance por rol")
    class Alcance {

        private final ConclusionFiltroDTO sinFiltros = new ConclusionFiltroDTO(null, null, null, null);

        @Test
        @DisplayName("Un PROFESOR solo ve las conclusiones de sus asignaturas con asignación docente activa")
        void profesorVeSoloLasSuyas() {
            UUID profesorId = UUID.randomUUID();
            Conclusion suya = new Conclusion(resultadoAprendizaje, semestre, 60.0);

            ResultadoAprendizaje raAjeno = new ResultadoAprendizaje();
            raAjeno.setId(UUID.randomUUID());
            ProgramaAcademico programa = new ProgramaAcademico();
            Asignatura asignaturaAjena = new Asignatura("AJE1", "Ajena", programa);
            ReflectionTestUtils.setField(asignaturaAjena, "id", UUID.randomUUID());
            raAjeno.setAsignatura(asignaturaAjena);
            Conclusion ajena = new Conclusion(raAjeno, semestre, 60.0);

            when(conclusionRepository.findAll()).thenReturn(List.of(suya, ajena));

            AsignacionDocente asignacion = new AsignacionDocente();
            asignacion.setAsignatura(asignatura);
            asignacion.setEstado(EstadoRegistro.ACTIVO);
            when(asignacionDocenteRepository.findByProfesor_IdAndEstado(profesorId, EstadoRegistro.ACTIVO))
                    .thenReturn(List.of(asignacion));

            List<co.edu.uco.sigra.estadisticas.dto.ConclusionResponseDTO> resultado = service.consultarConclusiones(
                    sinFiltros, new UsuarioPrincipal(profesorId, RolUsuario.PROFESOR));

            assertThat(resultado).hasSize(1);
            assertThat(resultado.get(0).asignaturaId()).isEqualTo(ID_ASIGNATURA);
        }

        @Test
        @DisplayName("Un ADMINISTRADOR ve todas y no se consultan asignaciones docentes")
        void administradorVeTodas() {
            Conclusion unaConclusion = new Conclusion(resultadoAprendizaje, semestre, 60.0);
            when(conclusionRepository.findAll()).thenReturn(List.of(unaConclusion));

            List<co.edu.uco.sigra.estadisticas.dto.ConclusionResponseDTO> resultado = service.consultarConclusiones(
                    sinFiltros, new UsuarioPrincipal(UUID.randomUUID(), RolUsuario.ADMINISTRADOR));

            assertThat(resultado).hasSize(1);
            verifyNoInteractions(asignacionDocenteRepository);
        }

        @Test
        @DisplayName("Sin identidad se rechaza por cierre de seguridad")
        void sinIdentidad() {
            Conclusion unaConclusion = new Conclusion(resultadoAprendizaje, semestre, 60.0);
            when(conclusionRepository.findAll()).thenReturn(List.of(unaConclusion));

            assertThat(service.consultarConclusiones(sinFiltros, ADMIN)).hasSize(1);
            assertThatThrownBy(() -> service.consultarConclusiones(sinFiltros, null))
                    .isInstanceOf(PermisoInsuficienteException.class);
            verifyNoInteractions(asignacionDocenteRepository);
        }

        @Test
        @DisplayName("Un ESTUDIANTE no consulta conclusiones")
        void estudianteNoConsulta() {
            assertThatThrownBy(() -> service.consultarConclusiones(sinFiltros,
                    new UsuarioPrincipal(UUID.randomUUID(), RolUsuario.ESTUDIANTE)))
                    .isInstanceOf(PermisoInsuficienteException.class);
            verifyNoInteractions(conclusionRepository);
        }
    }

    @Nested
    @DisplayName("Recomendación del diagnóstico (RF-16, calculada al leer)")
    class Recomendacion {

        @Test
        @DisplayName("Predomina Bajo: recomienda reforzar fundamentos")
        void predominaBajo() {
            Conclusion conclusion = new Conclusion(resultadoAprendizaje, semestre, 60.0);
            conclusion.fijarDiagnostico(10, 6, 2, 40.0, null, null);

            assertThat(mapper.toResponseDTO(conclusion).recomendacion())
                    .isEqualTo(ConclusionMapper.RECOMENDACION_FUNDAMENTOS);
        }

        @Test
        @DisplayName("Predomina Medio: recomienda más práctica aplicada")
        void predominaMedio() {
            Conclusion conclusion = new Conclusion(resultadoAprendizaje, semestre, 60.0);
            conclusion.fijarDiagnostico(10, 2, 6, 40.0, null, null);

            assertThat(mapper.toResponseDTO(conclusion).recomendacion())
                    .isEqualTo(ConclusionMapper.RECOMENDACION_APLICACION);
        }

        @Test
        @DisplayName("Empate entre Bajo y Medio: recomienda combinar ambas cosas")
        void empate() {
            Conclusion conclusion = new Conclusion(resultadoAprendizaje, semestre, 60.0);
            conclusion.fijarDiagnostico(10, 4, 4, 40.0, null, null);

            assertThat(mapper.toResponseDTO(conclusion).recomendacion())
                    .isEqualTo(ConclusionMapper.RECOMENDACION_COMBINADA);
        }
    }

    @Nested
    @DisplayName("consultarPorId")
    class ConsultarPorId {

        @Test
        @DisplayName("Por id existente devuelve la conclusión")
        void existente() {
            UUID id = UUID.randomUUID();
            Conclusion conclusion = new Conclusion(resultadoAprendizaje, semestre, 55.0);
            ReflectionTestUtils.setField(conclusion, "id", id);
            when(conclusionRepository.findById(id)).thenReturn(Optional.of(conclusion));

            assertThat(service.consultarPorId(id, ADMIN).id()).isEqualTo(id);
        }

        @Test
        @DisplayName("Por id inexistente falla")
        void inexistente() {
            UUID id = UUID.randomUUID();
            when(conclusionRepository.findById(id)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.consultarPorId(id, ADMIN))
                    .isInstanceOf(ConclusionNoEncontradaException.class);
        }
    }

    @Nested
    @DisplayName("consultarEstado (RF-16, solo lectura: no genera ni modifica nada)")
    class ConsultarEstado {

        @Test
        @DisplayName("Con una conclusión Vigente devuelve VIGENTE con esa conclusión, sin recalcular niveles")
        void vigente() {
            when(resultadoAprendizajeRepository.findById(ID_RA)).thenReturn(Optional.of(resultadoAprendizaje));
            when(semestreRepository.findById(ID_SEMESTRE)).thenReturn(Optional.of(semestre));
            Conclusion existente = new Conclusion(resultadoAprendizaje, semestre, 70.0);
            when(conclusionRepository.findByResultadoAprendizaje_IdAndSemestre_IdAndEstado(
                    ID_RA, ID_SEMESTRE, EstadoConclusion.VIGENTE)).thenReturn(Optional.of(existente));

            ConclusionEstadoResponseDTO respuesta = service.consultarEstado(ID_RA, ID_SEMESTRE, ADMIN);

            assertThat(respuesta.estado()).isEqualTo(EstadoConsultaConclusion.VIGENTE);
            assertThat(respuesta.conclusion()).isNotNull();
            assertThat(respuesta.conclusion().porcentajeInsuficiente()).isEqualTo(70.0);
            assertThat(respuesta.porcentajeInsuficiente()).isEqualTo(70.0);
            verifyNoInteractions(proveedorNivelesLogro);
        }

        @Test
        @DisplayName("Sin Vigente, superando el umbral y con una Atendida previa devuelve ATENDIDA")
        void atendida() {
            when(resultadoAprendizajeRepository.findById(ID_RA)).thenReturn(Optional.of(resultadoAprendizaje));
            when(semestreRepository.findById(ID_SEMESTRE)).thenReturn(Optional.of(semestre));
            sinVigentePrevia(ID_SEMESTRE);
            evaluados(ID_SEMESTRE, 10, 5);
            Conclusion atendidaPrevia = new Conclusion(resultadoAprendizaje, semestre, 50.0);
            when(conclusionRepository.findByResultadoAprendizaje_IdAndSemestre_IdAndEstado(
                    ID_RA, ID_SEMESTRE, EstadoConclusion.ATENDIDA)).thenReturn(Optional.of(atendidaPrevia));

            ConclusionEstadoResponseDTO respuesta = service.consultarEstado(ID_RA, ID_SEMESTRE, ADMIN);

            assertThat(respuesta.estado()).isEqualTo(EstadoConsultaConclusion.ATENDIDA);
            assertThat(respuesta.conclusion()).isNotNull();
            assertThat(respuesta.totalEvaluados()).isEqualTo(10);
            assertThat(respuesta.porcentajeInsuficiente()).isEqualTo(50.0);
        }

        @Test
        @DisplayName("Con menos del mínimo de evaluados devuelve SIN_DATOS_SUFICIENTES sin conclusión")
        void sinDatosSuficientes() {
            when(resultadoAprendizajeRepository.findById(ID_RA)).thenReturn(Optional.of(resultadoAprendizaje));
            when(semestreRepository.findById(ID_SEMESTRE)).thenReturn(Optional.of(semestre));
            sinVigentePrevia(ID_SEMESTRE);
            when(proveedorNivelesLogro.contarEvaluados(ID_RA, ID_SEMESTRE)).thenReturn(2L);

            ConclusionEstadoResponseDTO respuesta = service.consultarEstado(ID_RA, ID_SEMESTRE, ADMIN);

            assertThat(respuesta.estado()).isEqualTo(EstadoConsultaConclusion.SIN_DATOS_SUFICIENTES);
            assertThat(respuesta.conclusion()).isNull();
            assertThat(respuesta.totalEvaluados()).isEqualTo(2);
        }

        @Test
        @DisplayName("Con datos suficientes y porcentaje dentro del umbral devuelve SATISFACTORIO")
        void satisfactorio() {
            when(resultadoAprendizajeRepository.findById(ID_RA)).thenReturn(Optional.of(resultadoAprendizaje));
            when(semestreRepository.findById(ID_SEMESTRE)).thenReturn(Optional.of(semestre));
            sinVigentePrevia(ID_SEMESTRE);
            evaluados(ID_SEMESTRE, 10, 3);

            ConclusionEstadoResponseDTO respuesta = service.consultarEstado(ID_RA, ID_SEMESTRE, ADMIN);

            assertThat(respuesta.estado()).isEqualTo(EstadoConsultaConclusion.SATISFACTORIO);
            assertThat(respuesta.conclusion()).isNull();
            assertThat(respuesta.porcentajeInsuficiente()).isEqualTo(30.0);
        }
    }
}
