package co.edu.uco.sigra;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

// INTEGRATION: levanta el contexto completo contra PostgreSQL (SIGRA/.env o variables del sistema).
// No se ejecuta con "gradlew test". Ejecutar con: .\gradlew.bat integrationTest
@Tag("integration")
@SpringBootTest
class SigraContextIT {

    @Test
    void contextLoads() {
    }
}
