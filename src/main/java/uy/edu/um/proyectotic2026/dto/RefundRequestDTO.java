package uy.edu.um.proyectotic2026.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

// Lo que se le manda al servicio de pagos para devolver (todo o parte de) un pago
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RefundRequestDTO {

    private String transactionId;
    private BigDecimal amount;
}
