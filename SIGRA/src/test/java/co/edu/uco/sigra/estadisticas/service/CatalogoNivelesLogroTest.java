package co.edu.uco.sigra.estadisticas.service;

import co.edu.uco.sigra.estadisticas.entity.NivelLogro;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Catálogo de 4 rangos de nivel de logro (RF-14)")
class CatalogoNivelesLogroTest {

    @ParameterizedTest(name = "{0} → {1}")
    @CsvSource({
            "0.0, BAJO", "2.5, BAJO", "2.99, BAJO", "2.995, BAJO",
            "3.0, MEDIO", "3.5, MEDIO", "3.99, MEDIO",
            "4.0, ALTO", "4.25, ALTO", "4.49, ALTO",
            "4.5, SOBRESALIENTE", "4.8, SOBRESALIENTE", "5.0, SOBRESALIENTE"})
    @DisplayName("Clasifica cada nota en su rango, con los límites confirmados por el equipo")
    void clasifica(double nota, NivelLogro esperado) {
        assertThat(CatalogoNivelesLogro.clasificar(nota)).isEqualTo(esperado);
    }

    @ParameterizedTest
    @ValueSource(doubles = {-0.01, 5.01, Double.NaN})
    @DisplayName("Rechaza notas fuera de la escala 0.0 – 5.0")
    void fueraDeEscala(double nota) {
        assertThatThrownBy(() -> CatalogoNivelesLogro.clasificar(nota))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Expone los 4 rangos en orden y sin huecos entre ellos")
    void rangosContiguos() {
        var rangos = CatalogoNivelesLogro.rangos();
        assertThat(rangos).extracting(CatalogoNivelesLogro.Rango::nivel)
                .containsExactly(NivelLogro.BAJO, NivelLogro.MEDIO, NivelLogro.ALTO, NivelLogro.SOBRESALIENTE);
        for (int i = 1; i < rangos.size(); i++) {
            assertThat(rangos.get(i).notaMinima()).isEqualTo(rangos.get(i - 1).notaLimite());
        }
        assertThat(rangos.get(0).notaMinima()).isEqualTo(0.0);
        assertThat(rangos.get(3).notaLimite()).isEqualTo(5.0);
        assertThat(rangos.get(3).limiteIncluido()).isTrue();
    }
}
