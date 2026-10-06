package co.edu.uco.sigra.estudiantes.repository;

import co.edu.uco.sigra.estudiantes.entity.Estudiante;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface EstudianteRepository extends JpaRepository<Estudiante, UUID> {

    boolean existsByTipoDocumento_IdAndNumeroDocumento(UUID tipoDocumentoId, String numeroDocumento);

    // Solo revisa estudiantes. La unicidad del correo entre todos los roles se valida con
    // UsuarioRepository.existsByCorreoInstitucionalIgnoreCase.
    boolean existsByCorreoInstitucional(String correoInstitucional);

    boolean existsByCorreoInstitucionalAndIdNot(String correoInstitucional, UUID id);

    /**
     * Busca estudiantes cuyo nombre completo o numero de documento contenga el criterio,
     * sin distinguir mayusculas de minusculas. El filtro se resuelve en la base de datos
     * con LOWER() y LIKE, no trayendo la tabla a memoria.
     *
     * <p>Este metodo nunca debe invocarse con criterio nulo: Hibernate no logra inferir
     * el tipo del parametro cuando el valor real es null y PostgreSQL termina rechazando
     * el bind (lower(bytea) no existe). El listado sin filtro lo resuelve el servicio
     * con {@link org.springframework.data.jpa.repository.JpaRepository#findAll()}.</p>
     */
    @Query("""
            select e from Estudiante e
            where lower(e.nombreCompleto) like concat('%', lower(:criterio), '%')
               or lower(e.numeroDocumento) like concat('%', lower(:criterio), '%')
            """)
    List<Estudiante> buscarPorNombreODocumento(@Param("criterio") String criterio);
}
