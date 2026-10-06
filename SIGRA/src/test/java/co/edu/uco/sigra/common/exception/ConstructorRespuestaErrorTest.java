package co.edu.uco.sigra.common.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas del constructor del cuerpo de error. Verifican el contrato documentado en
 * {@code package-info.java}: orden estable de las claves, coherencia entre {@code status} y
 * {@code error}, omisión de {@code detalles} cuando no hay nada que enumerar, y un
 * {@code timestamp} con zona.
 */
@DisplayName("Constructor del cuerpo de error")
class ConstructorRespuestaErrorTest {

    private static Map<String, Object> cuerpoDe(ResponseEntity<Map<String, Object>> respuesta) {
        Map<String, Object> cuerpo = respuesta.getBody();
        assertNotNull(cuerpo, "La respuesta de error debe traer cuerpo");
        return cuerpo;
    }

    @Nested
    @DisplayName("Orden de las claves")
    class OrdenDeLasClaves {

        @Test
        @DisplayName("Sin detalles, las claves salen en el orden del contrato")
        void ordenSinDetalles() {
            var cuerpo = cuerpoDe(ConstructorRespuestaError.construir(
                    HttpStatus.NOT_FOUND, "El recurso solicitado no existe"));

            assertIterableEquals(List.of("timestamp", "status", "error", "mensaje"), cuerpo.keySet());
        }

        @Test
        @DisplayName("Con detalles, la clave detalles va al final")
        void ordenConDetalles() {
            var cuerpo = cuerpoDe(ConstructorRespuestaError.construir(
                    HttpStatus.BAD_REQUEST, "Los datos de entrada no son válidos",
                    List.of("nombre: no puede estar vacío")));

            assertIterableEquals(List.of("timestamp", "status", "error", "mensaje", "detalles"),
                    cuerpo.keySet());
        }

        @Test
        @DisplayName("El orden es el mismo en dos llamadas con status distinto")
        void ordenEstableEntreLlamadas() {
            var primero = cuerpoDe(ConstructorRespuestaError.construir(
                    HttpStatus.CONFLICT, "El recurso ya existe"));
            var segundo = cuerpoDe(ConstructorRespuestaError.construir(
                    HttpStatus.UNSUPPORTED_MEDIA_TYPE, "El tipo de contenido no es soportado"));

            assertIterableEquals(primero.keySet(), segundo.keySet());
        }
    }

    @Nested
    @DisplayName("Coherencia entre status y error")
    class CoherenciaStatusError {

        @ParameterizedTest(name = "{0} responde status {0} y error con su propio nombre")
        @EnumSource(value = HttpStatus.class, names = {
                "BAD_REQUEST", "NOT_FOUND", "CONFLICT", "INTERNAL_SERVER_ERROR",
                "UNSUPPORTED_MEDIA_TYPE", "NOT_ACCEPTABLE"})
        @DisplayName("El campo error corresponde siempre al status")
        void errorCorrespondeAlStatus(HttpStatus status) {
            var respuesta = ConstructorRespuestaError.construir(status, "Mensaje de prueba");
            var cuerpo = cuerpoDe(respuesta);

            assertEquals(status.value(), respuesta.getStatusCode().value(),
                    "El código HTTP de la respuesta debe ser el pedido");
            assertEquals(status.value(), cuerpo.get("status"),
                    "El campo status debe traer el mismo código de la respuesta, como número");
            assertEquals(status.name(), cuerpo.get("error"),
                    "El campo error debe ser el nombre del código HTTP, no un literal escrito a mano");
        }

        @Test
        @DisplayName("El mensaje llega al cuerpo tal como se pasó")
        void mensajeIntacto() {
            var cuerpo = cuerpoDe(ConstructorRespuestaError.construir(
                    HttpStatus.BAD_REQUEST, "Falta el parámetro obligatorio 'programaId'"));

            assertEquals("Falta el parámetro obligatorio 'programaId'", cuerpo.get("mensaje"));
        }
    }

    @Nested
    @DisplayName("Clave detalles")
    class ClaveDetalles {

        @Test
        @DisplayName("Se omite cuando se pasa null")
        void seOmiteConNull() {
            var cuerpo = cuerpoDe(ConstructorRespuestaError.construir(
                    HttpStatus.NOT_FOUND, "El recurso solicitado no existe", null));

            assertFalse(cuerpo.containsKey("detalles"),
                    "Con null la clave no debe aparecer; tampoco debe aparecer con valor null");
        }

        @Test
        @DisplayName("Se omite cuando se pasa una lista vacía")
        void seOmiteConListaVacia() {
            var cuerpo = cuerpoDe(ConstructorRespuestaError.construir(
                    HttpStatus.NOT_FOUND, "El recurso solicitado no existe", List.of()));

            assertFalse(cuerpo.containsKey("detalles"),
                    "Una lista vacía no aporta nada al cliente: la clave debe omitirse");
        }

        @Test
        @DisplayName("Se omite en la sobrecarga que no recibe detalles")
        void seOmiteEnLaSobrecargaSinDetalles() {
            var cuerpo = cuerpoDe(ConstructorRespuestaError.construir(
                    HttpStatus.FORBIDDEN, "No tiene permisos suficientes"));

            assertFalse(cuerpo.containsKey("detalles"));
        }

        @Test
        @DisplayName("Aparece con todos sus elementos cuando trae contenido")
        void apareceConContenido() {
            var detalles = List.of("nombre: no puede estar vacío", "codigo: formato inválido");

            var cuerpo = cuerpoDe(ConstructorRespuestaError.construir(
                    HttpStatus.BAD_REQUEST, "Los datos de entrada no son válidos", detalles));

            assertEquals(detalles, cuerpo.get("detalles"));
        }

        @Test
        @DisplayName("El cuerpo guarda una copia: mutar la lista original no lo altera")
        void guardaUnaCopia() {
            var detalles = new ArrayList<String>();
            detalles.add("nombre: no puede estar vacío");

            var cuerpo = cuerpoDe(ConstructorRespuestaError.construir(
                    HttpStatus.BAD_REQUEST, "Los datos de entrada no son válidos", detalles));
            detalles.add("agregado después de construir la respuesta");

            assertEquals(List.of("nombre: no puede estar vacío"), cuerpo.get("detalles"));
        }
    }

    @Nested
    @DisplayName("Campo timestamp")
    class CampoTimestamp {

        @Test
        @DisplayName("Lleva zona, para que el cliente no tenga que suponerla")
        void llevaZona() {
            var cuerpo = cuerpoDe(ConstructorRespuestaError.construir(
                    HttpStatus.INTERNAL_SERVER_ERROR, "Ocurrió un error inesperado en el servidor"));

            assertInstanceOf(OffsetDateTime.class, cuerpo.get("timestamp"),
                    "El timestamp debe ser un OffsetDateTime: un LocalDateTime no lleva zona");
            assertNotNull(((OffsetDateTime) cuerpo.get("timestamp")).getOffset());
        }

        @Test
        @DisplayName("Corresponde al momento de construir la respuesta")
        void correspondeAlMomentoDeConstruir() {
            var antes = OffsetDateTime.now().minusSeconds(1);
            var cuerpo = cuerpoDe(ConstructorRespuestaError.construir(
                    HttpStatus.CONFLICT, "El recurso ya existe"));
            var despues = OffsetDateTime.now().plusSeconds(1);

            var timestamp = (OffsetDateTime) cuerpo.get("timestamp");
            assertTrue(timestamp.isAfter(antes) && timestamp.isBefore(despues),
                    "El timestamp debe caer en el instante de la llamada, no ser un valor fijo");
        }
    }
}
