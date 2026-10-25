package co.edu.uco.sigra.estadisticas.entity;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ConclusionTest {

    @Test
    void generarMensajeInsuficienteIncluyeElCodigoDelRa() {
        String mensaje = Conclusion.generarMensajeInsuficiente("RA-01");

        assertThat(mensaje).isEqualTo("Nivel de logro insuficiente en RA-01: se recomienda registrar una acción de mejora");
    }
}
