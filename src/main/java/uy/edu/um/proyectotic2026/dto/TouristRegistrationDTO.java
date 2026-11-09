package uy.edu.um.proyectotic2026.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor

public class TouristRegistrationDTO {

    private String email;
    private String password;
    private String confirmPassword;
    private String firstName;
    private String lastName;
    private String document;

    // El formulario manda la fecha como texto "2000-05-14"
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate birthDate;

}
