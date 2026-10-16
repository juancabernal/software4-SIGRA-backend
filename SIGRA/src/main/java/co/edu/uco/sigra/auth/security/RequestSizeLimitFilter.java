package co.edu.uco.sigra.auth.security;

import tools.jackson.databind.json.JsonMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * Limita el cuerpo real del login, incluidos requests chunked o sin {@code Content-Length}.
 *
 * <p>El límite no se aplica a la API académica: esas cargas tienen su propio contrato y el límite
 * de credenciales no debe convertirse accidentalmente en un límite global de negocio.</p>
 */
public class RequestSizeLimitFilter extends OncePerRequestFilter {

    private final JsonMapper objectMapper;
    private final int maxBytes;

    public RequestSizeLimitFilter(JsonMapper objectMapper, long maxBytes) {
        if (maxBytes <= 0 || maxBytes > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("El límite del cuerpo debe ser positivo y caber en memoria");
        }
        this.objectMapper = objectMapper;
        this.maxBytes = (int) maxBytes;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if (!esLogin(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        long contentLength = request.getContentLengthLong();
        if (contentLength > maxBytes) {
            escribirDemasiadoGrande(response);
            return;
        }

        CachedBodyHttpServletRequest wrappedRequest = new CachedBodyHttpServletRequest(request,
                leerHastaElLimite(request.getInputStream()));
        if (wrappedRequest.body().length > maxBytes) {
            escribirDemasiadoGrande(response);
            return;
        }

        filterChain.doFilter(wrappedRequest, response);
    }

    private byte[] leerHastaElLimite(InputStream inputStream) throws IOException {
        ByteArrayOutputStream body = new ByteArrayOutputStream(Math.min(maxBytes, 8192));
        byte[] buffer = new byte[8192];
        int total = 0;

        while (true) {
            int remaining = maxBytes - total;
            int requested = Math.min(buffer.length, remaining + 1);
            int read = inputStream.read(buffer, 0, requested);
            if (read == -1) {
                return body.toByteArray();
            }
            body.write(buffer, 0, read);
            total += read;
            if (total > maxBytes) {
                return body.toByteArray();
            }
        }
    }

    private void escribirDemasiadoGrande(HttpServletResponse response) throws IOException {
        JsonSecurityResponse.write(response, HttpStatus.PAYLOAD_TOO_LARGE,
                "PAYLOAD_TOO_LARGE", "El cuerpo de la petición supera el tamaño permitido",
                objectMapper);
    }

    private boolean esLogin(HttpServletRequest request) {
        String path = request.getServletPath();
        if (path == null || path.isBlank()) {
            path = request.getRequestURI();
        }
        return "POST".equalsIgnoreCase(request.getMethod())
                && "/api/v1/auth/login".equals(path);
    }

    private static final class CachedBodyHttpServletRequest extends HttpServletRequestWrapper {
        private final byte[] body;

        private CachedBodyHttpServletRequest(HttpServletRequest request, byte[] body) {
            super(request);
            this.body = Objects.requireNonNull(body);
        }

        private byte[] body() {
            return body;
        }

        @Override
        public ServletInputStream getInputStream() {
            return new CachedBodyServletInputStream(body);
        }

        @Override
        public BufferedReader getReader() {
            Charset charset = getCharacterEncoding() == null
                    ? StandardCharsets.ISO_8859_1
                    : Charset.forName(getCharacterEncoding());
            return new BufferedReader(new InputStreamReader(getInputStream(), charset));
        }

        @Override
        public int getContentLength() {
            return body.length;
        }

        @Override
        public long getContentLengthLong() {
            return body.length;
        }
    }

    private static final class CachedBodyServletInputStream extends ServletInputStream {
        private final byte[] body;
        private int position;

        private CachedBodyServletInputStream(byte[] body) {
            this.body = body;
        }

        @Override
        public int read() {
            return position < body.length ? body[position++] & 0xff : -1;
        }

        @Override
        public int read(byte[] bytes, int offset, int length) {
            if (position >= body.length) {
                return -1;
            }
            int count = Math.min(length, body.length - position);
            System.arraycopy(body, position, bytes, offset, count);
            position += count;
            return count;
        }

        @Override
        public boolean isFinished() {
            return position >= body.length;
        }

        @Override
        public boolean isReady() {
            return true;
        }

        @Override
        public void setReadListener(ReadListener readListener) {
            if (readListener == null) {
                throw new IllegalArgumentException("readListener no puede ser null");
            }
            try {
                if (isFinished()) {
                    readListener.onAllDataRead();
                } else {
                    readListener.onDataAvailable();
                }
            } catch (IOException ex) {
                readListener.onError(ex);
            }
        }
    }
}
