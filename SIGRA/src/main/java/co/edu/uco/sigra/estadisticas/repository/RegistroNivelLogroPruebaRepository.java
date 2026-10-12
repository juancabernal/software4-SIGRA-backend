package co.edu.uco.sigra.estadisticas.repository;

import co.edu.uco.sigra.estadisticas.entity.NivelLogro;
import co.edu.uco.sigra.estadisticas.entity.RegistroNivelLogroPrueba;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.UUID;

@Repository
public interface RegistroNivelLogroPruebaRepository extends JpaRepository<RegistroNivelLogroPrueba, UUID> {

    long countByResultadoAprendizaje_IdAndSemestre_Id(UUID resultadoAprendizajeId, UUID semestreId);

    long countByResultadoAprendizaje_IdAndSemestre_IdAndNivelLogroIn(
            UUID resultadoAprendizajeId, UUID semestreId, Collection<NivelLogro> niveles);
}
