package co.edu.uco.sigra.estudiantes.repository;

import co.edu.uco.sigra.estudiantes.entity.Estudiante;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface EstudianteRepository extends JpaRepository<Estudiante, UUID> {

    boolean existsByTipoDocumento_IdAndNumeroDocumento(UUID tipoDocumentoId, String numeroDocumento);

    boolean existsByCorreoInstitucional(String correoInstitucional);

    boolean existsByCorreoInstitucionalAndIdNot(String correoInstitucional, UUID id);

    /**
     * Busca estudiantes cuyo nombre completo o numero de documento contenga el criterio,
     * sin distinguir mayusculas de minusculas. El filtro se resuelve en la base de datos
     * con LOWER() y LIKE, no trayendo la tabla a memoria.
     *
     * <p>Cuando el criterio es nulo o viene vacio la consulta devuelve todos los
     * estudiantes, activos e inactivos, para que el listado sin filtro no necesite
     * una rama aparte en el servicio.</p>
     */
    @Query("""
            select e from Estudiante e
            where :criterio is null
               or trim(:criterio) = ''
               or lower(e.nombreCompleto) like concat('%', lower(:criterio), '%')
               or lower(e.numeroDocumento) like concat('%', lower(:criterio), '%')
            """)
    List<Estudiante> buscarPorNombreODocumento(@Param("criterio") String criterio);
}
