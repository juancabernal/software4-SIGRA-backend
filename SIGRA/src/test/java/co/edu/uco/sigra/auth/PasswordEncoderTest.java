package co.edu.uco.sigra.auth;

import co.edu.uco.sigra.config.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

// UNIT: prueba el PasswordEncoder REAL que SigraApplication registra en SecurityConfig.
// Si alguien cambia new BCryptPasswordEncoder(12) por otro costo, estas pruebas fallan.
class PasswordEncoderTest {

    private static final Pattern BCRYPT_COSTO_12 = Pattern.compile("^\\$2[aby]\\$12\\$.*");
    private static final String PASSWORD_DE_TESTING = "PasswordSeguro123*";

    // Hash real de bruno/SIGRA/seed/seed-auth.sql. Es determinista: no se regenera ni se imprime.
    private static final String HASH_REAL_DEL_SEED = "$2a$12$0xeKtqgxyBKM4Ks0ZWgxJu0ooSpm0OFeZ7hILuTE4henvC4dtDmjW";

    private final PasswordEncoder encoder = new SecurityConfig().passwordEncoder();

    @Test
    void encoderConfiguradoEsBCrypt() {
        assertThat(encoder).isInstanceOf(BCryptPasswordEncoder.class);
    }

    @Test
    void encoderConfiguradoGeneraHashBcryptDeCosto12() {
        assertThat(encoder.encode(PASSWORD_DE_TESTING)).matches(BCRYPT_COSTO_12);
    }

    @Test
    void encoderConfiguradoAceptaLaContraseniaCorrectaYRechazaUnaIncorrecta() {
        String hash = encoder.encode(PASSWORD_DE_TESTING);

        assertThat(encoder.matches(PASSWORD_DE_TESTING, hash)).isTrue();
        assertThat(encoder.matches("ClaveErronea123*", hash)).isFalse();
    }

    @Test
    void hashRealDelSeedCoincideConLaContraseniaDeTestingYTieneCosto12() {
        assertThat(HASH_REAL_DEL_SEED).matches(BCRYPT_COSTO_12);
        assertThat(encoder.matches(PASSWORD_DE_TESTING, HASH_REAL_DEL_SEED)).isTrue();
    }
}
