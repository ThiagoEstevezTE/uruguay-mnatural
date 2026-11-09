package uy.edu.um.proyectotic2026.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import uy.edu.um.proyectotic2026.entities.ProviderType;

@Getter
@Setter
@NoArgsConstructor

public class ProviderRegistrationDTO {

    private String email;
    private String password;
    private String confirmPassword;
    private String name;
    private String document;
    private String phone;
    private ProviderType type;


    private String businessName;
    private String businessDescription;
    private String businessAddress;
    private String businessPhone;
    private String businessEmail;
    private Long localityId;

}
