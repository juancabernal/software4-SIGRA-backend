package co.edu.uco.sigra.asignaturas.entity;


import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "asignatura")
@NoArgsConstructor

@Getter
@Setter
public class AsignacionDocente {
    @Id
    private String codigo;
    private String nombre;

}
