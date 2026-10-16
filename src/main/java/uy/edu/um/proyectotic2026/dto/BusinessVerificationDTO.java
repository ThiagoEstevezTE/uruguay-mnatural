package uy.edu.um.proyectotic2026.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import uy.edu.um.proyectotic2026.entities.BusinessStatus;

// Respuesta de "verificar emprendimiento": si esta aprobado por Uruguay Mnatural
@Getter
@AllArgsConstructor

public class BusinessVerificationDTO {

    private Long id;
    private String name;
    private BusinessStatus status;
    private boolean verified;

}
