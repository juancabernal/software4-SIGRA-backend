package co.edu.uco.sigra.integracion;

import co.edu.uco.sigra.common.enums.EstadoRegistro;
import co.edu.uco.sigra.facultades.entity.Facultad;
import co.edu.uco.sigra.facultades.repository.FacultadRepository;

/**
 * Facultad compartida por las pruebas de integración que necesitan guardar programas académicos
 * (todo programa exige una facultad). Se busca por nombre y se crea solo la primera vez; no se borra
 * en la limpieza de cada prueba porque otros programas de prueba pueden seguir apuntando a ella.
 */
public final class FacultadesDePrueba {

    public static final String NOMBRE = "IT Facultad Pruebas";

    private FacultadesDePrueba() {
    }

    public static Facultad compartida(FacultadRepository repositorio) {
        return repositorio.findAll().stream()
                .filter(f -> NOMBRE.equals(f.getNombre()))
                .findFirst()
                .map(f -> {
                    if (f.getEstado() != EstadoRegistro.ACTIVO) {
                        f.setEstado(EstadoRegistro.ACTIVO);
                        return repositorio.save(f);
                    }
                    return f;
                })
                .orElseGet(() -> {
                    Facultad nueva = new Facultad();
                    nueva.setNombre(NOMBRE);
                    nueva.setEstado(EstadoRegistro.ACTIVO);
                    return repositorio.save(nueva);
                });
    }
}
