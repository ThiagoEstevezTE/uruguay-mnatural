package uy.edu.um.proyectotic2026.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

// Lo que se le manda al servicio de pagos externo
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor

public class ChargeRequestDTO {

    private String cardNumber;
    private BigDecimal amount;
    private String currency;
    private String description;

}
