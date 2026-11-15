package uy.edu.um.proyectotic2026.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// Los datos de una persona que va a la actividad (una fila del formulario de reserva)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ParticipantDTO {

    private String firstName;
    private String lastName;
    private String document;
    private Integer age;
}
