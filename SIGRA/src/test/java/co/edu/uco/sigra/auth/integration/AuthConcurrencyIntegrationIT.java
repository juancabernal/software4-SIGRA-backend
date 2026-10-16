package co.edu.uco.sigra.auth.integration;

import co.edu.uco.sigra.auth.dto.LoginRequestDTO;
import co.edu.uco.sigra.auth.entity.Usuario;
import co.edu.uco.sigra.auth.exception.CredencialesInvalidasException;
import co.edu.uco.sigra.auth.exception.UsuarioBloqueadoException;
import co.edu.uco.sigra.auth.repository.UsuarioRepository;
import co.edu.uco.sigra.auth.service.AuthService;
import co.edu.uco.sigra.auth.service.impl.AuthServiceImpl;
import co.edu.uco.sigra.common.entity.TipoDocumento;
import co.edu.uco.sigra.common.enums.EstadoRegistro;
import co.edu.uco.sigra.common.repository.TipoDocumentoRepository;
import co.edu.uco.sigra.profesores.entity.Profesor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;

/** Verifica con PostgreSQL que el bloqueo pesimista no pierde incrementos bajo concurrencia. */
@Tag("integration")
@SpringBootTest
class AuthConcurrencyIntegrationIT {

    private static final String PREFIX = "r5c-";

    @Autowired private AuthService authService;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private TipoDocumentoRepository tipoDocumentoRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    private Profesor usuario;
    private TipoDocumento tipoDocumento;

    @BeforeEach
    void preparar() {
        usuarioRepository.findAll().stream()
                .filter(u -> u.getCorreoInstitucional() != null
                        && u.getCorreoInstitucional().startsWith(PREFIX))
                .forEach(usuarioRepository::delete);
        tipoDocumentoRepository.findAll().stream()
                .filter(t -> t.getNombre().startsWith(PREFIX))
                .forEach(tipoDocumentoRepository::delete);

        tipoDocumento = new TipoDocumento();
        tipoDocumento.setNombre(PREFIX + UUID.randomUUID().toString().replace("-", "").substring(0, 12));
        tipoDocumento = tipoDocumentoRepository.save(tipoDocumento);

        usuario = new Profesor();
        usuario.setTipoDocumento(tipoDocumento);
        usuario.setNombreCompleto("Profesor concurrencia");
        usuario.setCorreoInstitucional(PREFIX + UUID.randomUUID() + "@uco.net.co");
        usuario.setPasswordHash(passwordEncoder.encode("PasswordSeguro123*"));
        usuario.setEstado(EstadoRegistro.ACTIVO);
        usuario.setNumeroDocumento("990000099");
        usuario = usuarioRepository.save(usuario);
    }

    @AfterEach
    void limpiar() {
        if (usuario != null && usuario.getId() != null) {
            usuarioRepository.deleteById(usuario.getId());
        }
        if (tipoDocumento != null && tipoDocumento.getId() != null) {
            tipoDocumentoRepository.deleteById(tipoDocumento.getId());
        }
    }

    @Test
    @Timeout(45)
    @DisplayName("12 intentos simultáneos no pierden incrementos y bloquean exactamente al quinto")
    void intentosConcurrentesConservanElContador() throws Exception {
        int totalIntentos = 12;
        ExecutorService executor = Executors.newFixedThreadPool(totalIntentos);
        CountDownLatch listos = new CountDownLatch(totalIntentos);
        CountDownLatch iniciar = new CountDownLatch(1);
        List<Future<?>> futures = new ArrayList<>();

        try {
            for (int i = 0; i < totalIntentos; i++) {
                futures.add(executor.submit(() -> {
                    listos.countDown();
                    iniciar.await();
                    try {
                        authService.autenticar(new LoginRequestDTO(
                                usuario.getCorreoInstitucional(), "ClaveIncorrecta999*"));
                    } catch (CredencialesInvalidasException | UsuarioBloqueadoException esperado) {
                        // Ambos estados son correctos: los primeros cinco cuentan y los siguientes
                        // se rechazan mientras el bloqueo permanece activo.
                    }
                    return null;
                }));
            }
            listos.await();
            iniciar.countDown();
            for (Future<?> future : futures) {
                future.get();
            }
        } finally {
            executor.shutdownNow();
        }

        Usuario persistido = usuarioRepository.findById(usuario.getId()).orElseThrow();
        assertThat(persistido.getIntentosFallidos()).isEqualTo(AuthServiceImpl.MAX_INTENTOS_FALLIDOS);
        assertThat(persistido.getFechaBloqueo()).isNotNull();
    }
}
