package uy.edu.um.proyectotic2026.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import uy.edu.um.proyectotic2026.entities.Business;

// Datos del formulario para editar un emprendimiento
@Getter
@Setter
@NoArgsConstructor

public class BusinessDTO {

    private String name;
    private String description;
    private String address;
    private String phone;
    private String email;
    private Long localityId;

    // Carga el formulario con los datos actuales
    public static BusinessDTO from(Business business) {
        BusinessDTO dto = new BusinessDTO();
        dto.setName(business.getName());
        dto.setDescription(business.getDescription());
        dto.setAddress(business.getAddress());
        dto.setPhone(business.getPhone());
        dto.setEmail(business.getEmail());
        dto.setLocalityId(business.getLocality().getId());
        return dto;
    }
}
