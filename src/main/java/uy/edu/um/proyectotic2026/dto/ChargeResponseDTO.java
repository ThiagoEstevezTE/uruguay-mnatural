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
    private String cardLastDigits;  // Solo lo completa Mercado Pago

    public ChargeResponseDTO(String status, String transactionId, String message) {
        this(status, transactionId, message, null);
    }

    public boolean isApproved() {
        return "approved".equals(status);
    }
}