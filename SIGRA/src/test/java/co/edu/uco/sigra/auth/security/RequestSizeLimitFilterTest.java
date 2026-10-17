package co.edu.uco.sigra.auth.security;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import tools.jackson.databind.json.JsonMapper;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class RequestSizeLimitFilterTest {

    private static final int LIMITE = 16_384;

    private final RequestSizeLimitFilter filter = new RequestSizeLimitFilter(
            JsonMapper.builder().build(), LIMITE);

    @Test
    void aceptaExactamente16384BytesAunqueElTransporteNoDeclareLongitud() throws Exception {
        byte[] body = bytes(LIMITE);
        MockHttpServletRequest request = loginRequest(body, true);
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<byte[]> recibido = new AtomicReference<>();
        FilterChain chain = (wrapped, ignored) -> recibido.set(wrapped.getInputStream().readAllBytes());

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(recibido.get()).isEqualTo(body);
    }

    @Test
    void rechaza16385BytesAunqueElTransporteNoDeclareLongitud() throws Exception {
        MockHttpServletRequest request = loginRequest(bytes(LIMITE + 1), true);
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, noContinuar());

        assertThat(response.getStatus()).isEqualTo(413);
        assertThat(response.getContentAsString()).contains("PAYLOAD_TOO_LARGE")
                .doesNotContain("Exception", "co.edu");
    }

    @Test
    void rechazaLongitudDeclaradaMayorSinLeerElCuerpo() throws Exception {
        MockHttpServletRequest request = loginRequest(bytes(LIMITE + 1), false);
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, noContinuar());

        assertThat(response.getStatus()).isEqualTo(413);
    }

    @Test
    void noLimitaCargasDeRutasAcademicas() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/estudiantes");
        request.setContent(bytes(LIMITE + 1));
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<jakarta.servlet.ServletRequest> recibido = new AtomicReference<>();

        filter.doFilter(request, response, (wrapped, ignored) -> recibido.set(wrapped));

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(recibido.get()).isSameAs(request);
    }

    private MockHttpServletRequest loginRequest(byte[] body, boolean unknownLength) {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/auth/login") {
            @Override
            public int getContentLength() {
                return unknownLength ? -1 : super.getContentLength();
            }

            @Override
            public long getContentLengthLong() {
                return unknownLength ? -1 : super.getContentLengthLong();
            }
        };
        request.setContentType("application/json");
        request.setContent(body);
        if (unknownLength) {
            request.addHeader("Transfer-Encoding", "chunked");
        }
        return request;
    }

    private FilterChain noContinuar() {
        return (request, response) -> {
            throw new AssertionError("La cadena no debe continuar para un cuerpo sobredimensionado");
        };
    }

    private byte[] bytes(int length) {
        byte[] body = new byte[length];
        for (int i = 0; i < body.length; i++) {
            body[i] = 'a';
        }
        return body;
    }
}
