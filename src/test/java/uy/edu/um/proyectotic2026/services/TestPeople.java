package uy.edu.um.proyectotic2026.services;

import uy.edu.um.proyectotic2026.dto.ParticipantDTO;

import java.util.ArrayList;
import java.util.List;

// Personas de ejemplo para las reservas de las pruebas (todas mayores de edad y con documentos distintos)
public final class TestPeople {

    private TestPeople() {
    }

    public static List<ParticipantDTO> people(int count) {
        List<ParticipantDTO> list = new ArrayList<>();
        for (int i = 0; i < count; i++)
            list.add(new ParticipantDTO("Persona" + (i + 1), "Prueba", String.valueOf(40000000 + i), 30));
        return list;
    }
}
