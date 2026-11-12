package co.edu.uco.sigra.estudiantes.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Situación académica de la matrícula")
public enum SituacionMateria {
    EN_CURSO,
    PROGRAMADA,
    CURSADA,
    DESVINCULADA
}
