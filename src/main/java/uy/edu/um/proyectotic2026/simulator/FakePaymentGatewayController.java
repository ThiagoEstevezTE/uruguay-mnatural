package uy.edu.um.proyectotic2026.simulator;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uy.edu.um.proyectotic2026.dto.ChargeRequestDTO;
import uy.edu.um.proyectotic2026.dto.ChargeResponseDTO;
import uy.edu.um.proyectotic2026.dto.RefundRequestDTO;

import java.util.UUID;

// SIMULADOR del servicio de pagos externo, solo para la demo.
// En la realidad seria otra empresa (Mercado Pago, dLocal...) con su propia API.
// La plataforma lo llama por HTTP igual que llamaria al servicio real.
//
// Tarjetas de prueba:
//   termina en 0002 -> pago rechazado (402)
//   termina en 0119 -> servicio caido (503)
//   cualquier otra valida -> pago aprobado (200)
@RestController
@RequestMapping("/fake-gateway/api/v1")
public class FakePaymentGatewayController {

    @PostMapping("/charges")
    public ResponseEntity<ChargeResponseDTO> charge(@RequestBody ChargeRequestDTO request) {

        String card = request.getCardNumber() == null ? "" : request.getCardNumber().replace(" ", "");

        if (card.endsWith("0119"))
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();

        if (card.endsWith("0002"))
            return ResponseEntity.status(HttpStatus.PAYMENT_REQUIRED)
                    .body(new ChargeResponseDTO("rejected", null, "Fondos insuficientes"));

        if (!card.matches("\\d{13,19}"))
            return ResponseEntity.status(HttpStatus.PAYMENT_REQUIRED)
                    .body(new ChargeResponseDTO("rejected", null, "Número de tarjeta inválido"));

        String transactionId = "txn_" + UUID.randomUUID().toString().substring(0, 12);
        return ResponseEntity.ok(new ChargeResponseDTO("approved", transactionId, "Pago aprobado"));
    }

    // Devolucion de un pago aprobado (todo o una parte)
    @PostMapping("/refunds")
    public ResponseEntity<ChargeResponseDTO> refund(@RequestBody RefundRequestDTO request) {
        if (request.getTransactionId() == null || request.getAmount() == null || request.getAmount().signum() <= 0)
            return ResponseEntity.badRequest().body(new ChargeResponseDTO("rejected", null, "Devolución inválida"));

        String refundId = "rfd_" + UUID.randomUUID().toString().substring(0, 12);
        return ResponseEntity.ok(new ChargeResponseDTO("approved", refundId, "Devolución aprobada"));
    }
}
