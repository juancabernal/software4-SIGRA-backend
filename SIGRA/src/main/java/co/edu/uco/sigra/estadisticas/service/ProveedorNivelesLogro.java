package co.edu.uco.sigra.estadisticas.service;

import java.util.UUID;

/**
 * Puerto hacia el nivel de logro por estudiante que calcula el equipo de Estadísticas a partir de
 * las calificaciones (contexto del ciclo de negocio de RF-16). Mientras ese módulo no exista, lo
 * implementa {@code ProveedorNivelesLogroPruebaImpl} con datos de prueba insertados directamente en
 * la base de datos ({@code RegistroNivelLogroPrueba}). Cuando el cálculo real esté listo, se agrega
 * una nueva implementación de esta interfaz y {@code ConclusionServiceImpl} no cambia.
 */
public interface ProveedorNivelesLogro {

    /** Cuántos estudiantes tienen nivel de logro calculado para ese (RA, semestre). */
    long contarEvaluados(UUID resultadoAprendizajeId, UUID semestreId);

    /** De esos, cuántos están en nivel Bajo o Medio. */
    long contarEnBajoOMedio(UUID resultadoAprendizajeId, UUID semestreId);
}
