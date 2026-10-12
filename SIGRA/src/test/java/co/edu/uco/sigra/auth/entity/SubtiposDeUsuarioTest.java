package co.edu.uco.sigra.auth.entity;

import co.edu.uco.sigra.common.entity.TipoDocumento;
import co.edu.uco.sigra.common.enums.EstadoRegistro;
import co.edu.uco.sigra.common.enums.RolUsuario;
import co.edu.uco.sigra.estudiantes.entity.Estudiante;
import co.edu.uco.sigra.profesores.entity.Profesor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Subtipos de Usuario")
class SubtiposDeUsuarioTest {

    private static final int MAX_INTENTOS = 5;
    private static final Duration DURACION_BLOQUEO = Duration.ofMinutes(15);

    static Stream<Arguments> subtipos() {
        return Stream.of(
                Arguments.of("Administrador", (Supplier<Usuario>) Administrador::new, RolUsuario.ADMINISTRADOR),
                Arguments.of("Profesor", (Supplier<Usuario>) Profesor::new, RolUsuario.PROFESOR),
                Arguments.of("Estudiante", (Supplier<Usuario>) Estudiante::new, RolUsuario.ESTUDIANTE));
    }

    @ParameterizedTest(name = "{0} devuelve el rol {2}")
    @MethodSource("subtipos")
    @DisplayName("Cada subtipo devuelve su rol")
    void devuelveSuRol(String nombre, Supplier<Usuario> crear, RolUsuario rolEsperado) {
        assertThat(crear.get().getRol()).isEqualTo(rolEsperado);
    }

    @ParameterizedTest(name = "{0} es un Usuario")
    @MethodSource("subtipos")
    @DisplayName("Cada subtipo es instancia de Usuario")
    void esUsuario(String nombre, Supplier<Usuario> crear, RolUsuario rol) {
        assertThat(crear.get()).isInstanceOf(Usuario.class);
    }

    @ParameterizedTest(name = "{0} hereda los campos comunes")
    @MethodSource("subtipos")
    @DisplayName("Los campos heredados se asignan y se leen en cada subtipo")
    void camposHeredados(String nombre, Supplier<Usuario> crear, RolUsuario rol) {
        Usuario usuario = crear.get();
        TipoDocumento cedula = new TipoDocumento();
        cedula.setNombre("Cédula de Ciudadanía");

        usuario.setNombreCompleto("Persona de Prueba");
        usuario.setCorreoInstitucional("persona.prueba@uco.net.co");
        usuario.setTipoDocumento(cedula);

        assertThat(usuario.getNombreCompleto()).isEqualTo("Persona de Prueba");
        assertThat(usuario.getCorreoInstitucional()).isEqualTo("persona.prueba@uco.net.co");
        assertThat(usuario.getTipoDocumento()).isSameAs(cedula);
        assertThat(usuario.getEstado()).isEqualTo(EstadoRegistro.ACTIVO);
    }

    @Test
    @DisplayName("Estudiante conserva su número de documento")
    void estudianteConservaNumeroDocumento() {
        Estudiante estudiante = new Estudiante();
        estudiante.setNumeroDocumento("1122334455");

        assertThat(estudiante.getNumeroDocumento()).isEqualTo("1122334455");
    }

    @ParameterizedTest(name = "{0} se bloquea al quinto intento fallido")
    @MethodSource("subtipos")
    @DisplayName("El bloqueo por intentos fallidos funciona igual en cada subtipo")
    void bloqueoPorIntentosFallidos(String nombre, Supplier<Usuario> crear, RolUsuario rol) {
        Usuario usuario = crear.get();
        LocalDateTime ahora = LocalDateTime.of(2026, 10, 5, 12, 0);

        for (int i = 1; i < MAX_INTENTOS; i++) {
            usuario.registrarIntentoFallido(ahora, MAX_INTENTOS);
        }
        assertThat(usuario.estaBloqueado(ahora, DURACION_BLOQUEO)).isFalse();

        usuario.registrarIntentoFallido(ahora, MAX_INTENTOS);

        assertThat(usuario.getIntentosFallidos()).isEqualTo(MAX_INTENTOS);
        assertThat(usuario.estaBloqueado(ahora.plusMinutes(14), DURACION_BLOQUEO)).isTrue();
        assertThat(usuario.estaBloqueado(ahora.plus(DURACION_BLOQUEO), DURACION_BLOQUEO)).isFalse();
    }
}
