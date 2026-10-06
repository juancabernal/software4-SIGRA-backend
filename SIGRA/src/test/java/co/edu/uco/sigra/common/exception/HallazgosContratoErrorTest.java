package co.edu.uco.sigra.common.exception;

import co.edu.uco.sigra.asignaturas.controller.AsignaturaController;
import co.edu.uco.sigra.asignaturas.service.AsignaturaService;
import co.edu.uco.sigra.profesores.controller.ProfesorController;
import co.edu.uco.sigra.profesores.service.ProfesorService;
import co.edu.uco.sigra.semestres.controller.SemestreController;
import co.edu.uco.sigra.semestres.service.SemestreService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import tools.jackson.databind.JsonNode;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * Documenta, con una prueba por hallazgo, el estado de los defectos que se encontraron al leer los
 * cinco {@code @ControllerAdvice} del proyecto para este cambio. El detalle entregable —severidad,
 * reproducción, dueño— vive en {@code scripts/hallazgos-manejo-errores.md}; aquí solo queda la
 * evidencia ejecutable.
 *
 * <p>De los cuatro hallazgos que tocan código ajeno, <strong>tres ya estaban resueltos</strong> para
 * cuando se llegó a escribir esta prueba, dos por un commit de otro integrante y uno como efecto
 * colateral de {@link ManejadorErroresTransversal} (commit 4 de este cambio). Solo uno sigue abierto.
 * Por eso esta clase no sigue el patrón de «prueba correcta, {@code @Disabled}, para que el dueño la
 * habilite al corregir» en los tres que ya están cerrados: documentar con {@code @Disabled} un
 * defecto que no existe sería más confuso que útil para quien lea el PR. En su lugar, cada hallazgo
 * cerrado tiene una prueba de no regresión que <strong>pasa hoy</strong> y falla si alguien revierte
 * la corrección sin querer.
 *
 * <h2>Resumen de los cinco hallazgos</h2>
 *
 * <ul>
 *   <li><strong>H-01</strong> ({@link HallazgoH01FugaDeMensajeTecnico}) — <strong>cerrado</strong>,
 *       por el commit {@code 8c32948} de Jean Paul, anterior a esta sección. Prueba de no
 *       regresión abajo.</li>
 *   <li><strong>H-02</strong> — <strong>abierto</strong>. Vive en
 *       {@code ContratoErrorProfesoresTest.HallazgoH02PrecedenciaDeProfesores} (sección 5 de este
 *       cambio) y no se repite aquí: esas pruebas ya expresan el comportamiento correcto y están
 *       {@code @Disabled} con el número de hallazgo y el dueño, listas para que las habilite al
 *       corregir.</li>
 *   <li><strong>H-03 / H-04</strong> ({@link HallazgoH03H04ContentTypeEnAsignaturasYSemestres}) —
 *       <strong>cerrados</strong>, como efecto colateral de haber agregado
 *       {@link ManejadorErroresTransversal}: ninguno de los dos advices de módulo declaraba
 *       {@code Exception}, así que el advice transversal ya cubre el {@code Content-Type} inválido
 *       en los dos sin que nadie tuviera que tocar el archivo de Juan Camilo ni el de Santiago.
 *       Pruebas de no regresión abajo.</li>
 *   <li><strong>H-05</strong> — deuda de diseño, no un defecto; no tiene prueba. Queda anotada en el
 *       {@code package-info.java} de este paquete (§6) y en el documento entregable.</li>
 * </ul>
 */
class HallazgosContratoErrorTest {

    /**
     * H-01 — {@code GlobalExceptionHandler}, dueño original Jean Paul.
     *
     * <p>El hallazgo, como se encontró al leer el advice: un fallo inesperado cuyo mensaje interno
     * trae texto técnico (por ejemplo, un error de SQL) llegaba al cliente en el campo
     * {@code detalles} del 500, en lugar de quedarse solo en el log. Violaba RNF-17.
     *
     * <p>Para cuando se escribió esta prueba, el método {@code manejarErrorGenerico} ya registraba
     * la causa con {@code log.error} y respondía un mensaje genérico sin {@code detalles} — corregido
     * en el commit {@code 8c32948}, anterior a este cambio. La prueba confirma que la corrección se
     * sostiene.
     */
    @Nested
    @WebMvcTest(controllers = ProfesorController.class)
    @AutoConfigureMockMvc(addFilters = false)
    @DisplayName("H-01 (cerrado, 8c32948): el 500 genérico no filtra el mensaje técnico interno")
    class HallazgoH01FugaDeMensajeTecnico {

        @Autowired
        private MockMvc mockMvc;

        @MockitoBean
        private ProfesorService profesorService;

        @Test
        @DisplayName("Un fallo inesperado con texto técnico en su mensaje responde 500 genérico, sin ese texto")
        void falloInesperadoNoFiltraElMensajeTecnico() throws Exception {
            given(profesorService.consultar(any()))
                    .willThrow(new RuntimeException(
                            "relation \"profesor\" does not exist at co.edu.uco.sigra.profesores."
                                    + "repository.ProfesorRepositoryImpl.buscarTodos"));

            MvcResult resultado = mockMvc.perform(get("/api/v1/profesores")).andReturn();

            JsonNode cuerpo = AsercionesContratoError.verificarContratoSinDetalles(
                    resultado, HttpStatus.INTERNAL_SERVER_ERROR);

            assertThat(cuerpo.get("mensaje").stringValue())
                    .as("El mensaje al cliente debe ser genérico, nunca el texto técnico interno")
                    .doesNotContain("relation", "does not exist", "ProfesorRepositoryImpl");
        }
    }

    /**
     * H-03 y H-04 — {@code AsignaturaExceptionHandler} (Juan Camilo) y {@code SemestreExceptionHandler}
     * (Santiago).
     *
     * <p>El hallazgo, como se encontró al leer el estado del proyecto antes de este cambio: si esos
     * dos advices declararan {@code @ExceptionHandler(Exception.class)} en {@code HIGHEST_PRECEDENCE},
     * un {@code Content-Type} no soportado en sus controladores daría 500, y ningún advice aditivo
     * podría ganarles.
     *
     * <p>Al escribir esta prueba se confirmó que, hoy, <strong>ninguno de los dos</strong> declara ese
     * catch-all. {@link ManejadorErroresTransversal} —agregado en el commit 4 de este cambio, sin que
     * nadie tocara estos dos archivos— ya cubre {@code HttpMediaTypeNotSupportedException} para todo
     * el proyecto, {@code asignaturas} y {@code semestres} incluidos. El hallazgo queda cerrado como
     * efecto colateral, y estas pruebas confirman que no vuelve a abrirse si alguien agrega el
     * catch-all que el hallazgo original advertía.
     */
    @Nested
    @DisplayName("H-03 / H-04 (cerrados por el commit 4): Content-Type inválido ya no es 500")
    class HallazgoH03H04ContentTypeEnAsignaturasYSemestres {

        @Nested
        @WebMvcTest(controllers = AsignaturaController.class)
        @AutoConfigureMockMvc(addFilters = false)
        @DisplayName("H-03: asignaturas")
        class EnAsignaturas {

            @Autowired
            private MockMvc mockMvc;

            @MockitoBean
            private AsignaturaService asignaturaService;

            @Test
            @DisplayName("Content-Type no soportado responde 415, no 500")
            void contentTypeNoSoportadoResponde415() throws Exception {
                MvcResult resultado = mockMvc.perform(post("/api/v1/asignaturas")
                                .contentType(MediaType.TEXT_PLAIN)
                                .content("esto no es json"))
                        .andReturn();

                AsercionesContratoError.verificarContratoConDetalles(
                        resultado, HttpStatus.UNSUPPORTED_MEDIA_TYPE);
            }
        }

        @Nested
        @WebMvcTest(controllers = SemestreController.class)
        @AutoConfigureMockMvc(addFilters = false)
        @DisplayName("H-04: semestres")
        class EnSemestres {

            @Autowired
            private MockMvc mockMvc;

            @MockitoBean
            private SemestreService semestreService;

            @Test
            @DisplayName("Content-Type no soportado responde 415, no 500")
            void contentTypeNoSoportadoResponde415() throws Exception {
                MvcResult resultado = mockMvc.perform(post("/api/v1/semestres")
                                .contentType(MediaType.TEXT_PLAIN)
                                .content("esto no es json"))
                        .andReturn();

                AsercionesContratoError.verificarContratoConDetalles(
                        resultado, HttpStatus.UNSUPPORTED_MEDIA_TYPE);
            }
        }
    }
}
