package uy.edu.um.proyectotic2026.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

// Formulario de reserva: una fila por persona que va, y las observaciones para el prestador
@Getter
@Setter
@NoArgsConstructor
public class BookingFormDTO {

    private List<ParticipantDTO> people = new ArrayList<>();
    private String observations;
}
