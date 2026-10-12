package co.edu.uco.sigra.auth;

import co.edu.uco.sigra.auth.entity.Usuario;
import co.edu.uco.sigra.common.enums.RolUsuario;
import co.edu.uco.sigra.profesores.entity.Profesor;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

// UNIT: lógica de intentos y bloqueo de la entidad, sin base de datos.
class UsuarioTest {

    private static final Duration DURACION_BLOQUEO = Duration.ofMinutes(15);

    @Test
    void profesorImplementaRolProfesor() {
        Profesor profesor = new Profesor();
        profesor.setNumeroDocumento("12345678");

        assertThat(profesor.getRol()).isEqualTo(RolUsuario.PROFESOR);
    }

    @Test
    void registrarIntentoFallidoIncrementaContador() {
        Usuario usuario = new Profesor();
        LocalDateTime ahora = LocalDateTime.of(2026, 10, 4, 12, 0);

        usuario.registrarIntentoFallido(ahora, 5);

        assertThat(usuario.getIntentosFallidos()).isEqualTo(1);
        assertThat(usuario.getFechaBloqueo()).isNull();
    }

    @Test
    void quintoIntentoFallidoEstableceFechaBloqueo() {
        Usuario usuario = new Profesor();
        usuario.setIntentosFallidos(4);
        LocalDateTime ahora = LocalDateTime.of(2026, 10, 4, 12, 0);

        usuario.registrarIntentoFallido(ahora, 5);

        assertThat(usuario.getIntentosFallidos()).isEqualTo(5);
        assertThat(usuario.getFechaBloqueo()).isEqualTo(ahora);
    }

    @Test
    void estaBloqueadoDevuelveTrueDentroDeLos15Minutos() {
        Usuario usuario = new Profesor();
        LocalDateTime inicioBloqueo = LocalDateTime.of(2026, 10, 4, 12, 0);
        usuario.setFechaBloqueo(inicioBloqueo);

        LocalDateTime duranteBloqueo = inicioBloqueo.plusMinutes(10);
        assertThat(usuario.estaBloqueado(duranteBloqueo, DURACION_BLOQUEO)).isTrue();
    }

    @Test
    void estaBloqueadoUnaMilesimaAntesDeLosQuinceMinutosSigueBloqueado() {
        Usuario usuario = new Profesor();
        LocalDateTime inicioBloqueo = LocalDateTime.of(2026, 10, 4, 12, 0);
        usuario.setFechaBloqueo(inicioBloqueo);

        LocalDateTime unaMilesimaAntesDelFin = inicioBloqueo.plus(DURACION_BLOQUEO).minus(Duration.ofMillis(1));
        assertThat(usuario.estaBloqueado(unaMilesimaAntesDelFin, DURACION_BLOQUEO)).isTrue();
    }

    @Test
    void enElInstanteExactoDeLosQuinceMinutosYaNoEstaBloqueado() {
        Usuario usuario = new Profesor();
        LocalDateTime inicioBloqueo = LocalDateTime.of(2026, 10, 4, 12, 0);
        usuario.setFechaBloqueo(inicioBloqueo);

        LocalDateTime finDelBloqueo = inicioBloqueo.plus(DURACION_BLOQUEO);
        assertThat(usuario.estaBloqueado(finDelBloqueo, DURACION_BLOQUEO)).isFalse();
    }

    @Test
    void estaBloqueadoDevuelveFalseDespuesDeLos15Minutos() {
        Usuario usuario = new Profesor();
        LocalDateTime inicioBloqueo = LocalDateTime.of(2026, 10, 4, 12, 0);
        usuario.setFechaBloqueo(inicioBloqueo);

        LocalDateTime despuesDeBloqueo = inicioBloqueo.plusMinutes(15).plusSeconds(1);
        assertThat(usuario.estaBloqueado(despuesDeBloqueo, DURACION_BLOQUEO)).isFalse();
    }

    @Test
    void estaBloqueadoDevuelveFalseSiFechaBloqueoEsNull() {
        Usuario usuario = new Profesor();
        LocalDateTime ahora = LocalDateTime.of(2026, 10, 4, 12, 0);

        assertThat(usuario.estaBloqueado(ahora, DURACION_BLOQUEO)).isFalse();
    }

    @Test
    void reiniciarContadorLimpiaIntentosYFechaBloqueo() {
        Usuario usuario = new Profesor();
        usuario.setIntentosFallidos(5);
        usuario.setFechaBloqueo(LocalDateTime.now());

        usuario.reiniciarContadorIntentos();

        assertThat(usuario.getIntentosFallidos()).isZero();
        assertThat(usuario.getFechaBloqueo()).isNull();
    }
}
