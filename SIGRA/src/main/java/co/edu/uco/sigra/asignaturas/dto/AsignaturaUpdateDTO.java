package co.edu.uco.sigra.asignaturas.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Única modificación permitida sobre una asignatura: su nombre (código y programa son inmutables). */
public record AsignaturaUpdateDTO(
        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 100, message = "El nombre no puede superar los 100 caracteres")
        String nombre
) {}
