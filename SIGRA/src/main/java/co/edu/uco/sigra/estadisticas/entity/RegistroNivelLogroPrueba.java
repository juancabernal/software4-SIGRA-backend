package co.edu.uco.sigra.estadisticas.entity;

import co.edu.uco.sigra.estudiantes.entity.Estudiante;
import co.edu.uco.sigra.resultadosaprendizaje.entity.ResultadoAprendizaje;
import co.edu.uco.sigra.semestres.entity.Semestre;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/**
 * Nivel de logro de un estudiante en un (RA, semestre), insertado como DATO DE PRUEBA.
 * <p>
 * El cálculo real del nivel de logro lo construye el equipo de Estadísticas a partir de las
 * calificaciones (módulo {@code calificaciones}, todavía sin implementar: solo tiene los
 * {@code .gitkeep}). Mientras esa integración no exista, {@code ProveedorNivelesLogroPruebaImpl} lee
 * esta tabla para poder evaluar la regla de RF-16. Se puebla a mano (o desde las pruebas) con filas
 * como las del enunciado: 10 estudiantes por (RA, semestre), cada uno con su nivel.
 * <p>
 * Pendiente de integrar: cuando el equipo de Estadísticas entregue el cálculo real, esta entidad y su
 * repositorio se retiran y se agrega una nueva implementación de
 * {@link co.edu.uco.sigra.estadisticas.service.ProveedorNivelesLogro} que consulte las estadísticas
 * reales; {@code ConclusionServiceImpl} no tiene que cambiar, porque solo depende de esa interfaz.
 */
@Entity
@Table(name = "registro_nivel_logro_prueba",
        uniqueConstraints = @UniqueConstraint(name = "uk_nivel_logro_prueba_terna",
                columnNames = {"resultado_aprendizaje_id", "semestre_id", "estudiante_id"}))
@Getter
@Setter
@NoArgsConstructor
public class RegistroNivelLogroPrueba {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "resultado_aprendizaje_id", nullable = false)
    private ResultadoAprendizaje resultadoAprendizaje;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "semestre_id", nullable = false)
    private Semestre semestre;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "estudiante_id", nullable = false)
    private Estudiante estudiante;

    @Enumerated(EnumType.STRING)
    @Column(name = "nivel_logro", nullable = false, length = 20)
    private NivelLogro nivelLogro;
}
