package uy.edu.um.proyectotic2026.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import uy.edu.um.proyectotic2026.dto.ChargeResponseDTO;
import uy.edu.um.proyectotic2026.dto.MercadoPagoPaymentDTO;
import uy.edu.um.proyectotic2026.exceptions.PaymentUnavailableException;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

// Cobra con la API de Mercado Pago (POST /v1/payments).
// El Access Token vive solo en el servidor: nunca se manda a las pantallas.
@Service
public class MercadoPagoClient {

    private static final Logger log = LoggerFactory.getLogger(MercadoPagoClient.class);

    private final RestClient restClient;

    public MercadoPagoClient(@Value("${mercadopago.access-token:}") String accessToken,
                             @Value("${mercadopago.url:https://api.mercadopago.com}") String url) {
        SimpleClientHttpRequestFactory timeouts = new SimpleClientHttpRequestFactory();
        timeouts.setConnectTimeout(5000);
        timeouts.setReadTimeout(15000);

        this.restClient = RestClient.builder()
                .baseUrl(url)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .requestFactory(timeouts)
                .build();
    }

    // El monto lo pone el servidor: nunca se toma el que venga del navegador.
    public ChargeResponseDTO pay(MercadoPagoPaymentDTO data, BigDecimal amount, String description)
            throws PaymentUnavailableException {

        Map<String, Object> payer = new HashMap<>();
        payer.put("email", data.getPayerEmail());
        if (data.getIdentificationNumber() != null && !data.getIdentificationNumber().isBlank()) {
            payer.put("identification", Map.of(
                    "type", data.getIdentificationType(),
                    "number", data.getIdentificationNumber()));
        }

        Map<String, Object> body = new HashMap<>();
        body.put("transaction_amount", amount);
        body.put("token", data.getToken());
        body.put("description", description);
        body.put("installments", data.getInstallments());
        body.put("payment_method_id", data.getPaymentMethodId());
        if (data.getIssuerId() != null && !data.getIssuerId().isBlank()) {
            body.put("issuer_id", data.getIssuerId());
        }
        body.put("payer", payer);

        try {
            Map<?, ?> response = restClient.post()
                    .uri("/v1/payments")
                    .contentType(MediaType.APPLICATION_JSON)
                    // Mercado Pago lo exige. Cada intento lleva uno nuevo, asi se puede reintentar
                    // con otra tarjeta; el doble clic ya lo frena el bloqueo de la reserva.
                    .header("X-Idempotency-Key", UUID.randomUUID().toString())
                    .body(body)
                    .retrieve()
                    .body(Map.class);

            return toChargeResponse(response);
        } catch (RestClientException e) {
            // Token mal puesto (401), datos invalidos (400), servicio caido o tiempo agotado
            log.error("Fallo el cobro con Mercado Pago: {}", e.getMessage());
            throw new PaymentUnavailableException("El servicio de pagos no está disponible en este momento");
        }
    }

    private ChargeResponseDTO toChargeResponse(Map<?, ?> response) {
        String status = String.valueOf(response.get("status"));
        boolean approved = "approved".equals(status);

        ChargeResponseDTO result = new ChargeResponseDTO(
                approved ? "approved" : "rejected",
                String.valueOf(response.get("id")),
                approved ? "Pago aprobado" : String.valueOf(response.get("status_detail")));

        if (response.get("card") instanceof Map<?, ?> card && card.get("last_four_digits") != null) {
            result.setCardLastDigits(String.valueOf(card.get("last_four_digits")));
        }
        return result;
    }
}