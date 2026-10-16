package uy.edu.um.proyectotic2026.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// Lo que responde el servicio de pagos externo
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor

public class ChargeResponseDTO {

    private String status;          // "approved" o "rejected"
    private String transactionId;
    private String message;

    public boolean isApproved() {
        return "approved".equals(status);
    }
}
