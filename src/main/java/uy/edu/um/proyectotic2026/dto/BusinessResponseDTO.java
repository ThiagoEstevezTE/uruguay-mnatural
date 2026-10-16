package uy.edu.um.proyectotic2026.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import uy.edu.um.proyectotic2026.entities.Business;
import uy.edu.um.proyectotic2026.entities.BusinessStatus;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor

public class BusinessResponseDTO {

    private Long id;
    private String name;
    private String description;
    private String address;
    private String locality;
    private String department;
    private BusinessStatus status;
    private LocalDateTime requestDate;

    public static BusinessResponseDTO from(Business business) {

        return new BusinessResponseDTO(
                business.getId(),
                business.getName(),
                business.getDescription(),
                business.getAddress(),
                business.getLocality().getName(),
                business.getLocality().getDepartment().getName(),
                business.getStatus(),
                business.getRequestDate()
        );

    }

}
