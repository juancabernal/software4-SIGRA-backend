package co.edu.uco.sigra.estadisticas.service;

import co.edu.uco.sigra.estadisticas.entity.NivelLogro;
import co.edu.uco.sigra.estadisticas.repository.NivelLogroEstudianteRepository;
import co.edu.uco.sigra.estadisticas.service.impl.ProveedorNivelesLogroImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Proveedor real de niveles de logro para RF-16")
class ProveedorNivelesLogroImplTest {

    private static final UUID RA = UUID.randomUUID();
    private static final UUID SEMESTRE = UUID.randomUUID();

    @Mock
    private NivelLogroEstudianteRepository repository;

    @InjectMocks
    private ProveedorNivelesLogroImpl proveedor;

    @Test
    @DisplayName("contarEvaluados cuenta solo a quienes tienen nivel calculado")
    void contarEvaluados() {
        when(repository.countByResultadoAprendizaje_IdAndSemestre_IdAndNivelLogroIsNotNull(RA, SEMESTRE)).thenReturn(7L);

        assertThat(proveedor.contarEvaluados(RA, SEMESTRE)).isEqualTo(7L);
    }

    @Test
    @DisplayName("contarEnBajoOMedio cuenta los niveles BAJO y MEDIO")
    void contarEnBajoOMedio() {
        when(repository.countByResultadoAprendizaje_IdAndSemestre_IdAndNivelLogroIn(
                RA, SEMESTRE, List.of(NivelLogro.BAJO, NivelLogro.MEDIO))).thenReturn(3L);

        assertThat(proveedor.contarEnBajoOMedio(RA, SEMESTRE)).isEqualTo(3L);
    }
}
