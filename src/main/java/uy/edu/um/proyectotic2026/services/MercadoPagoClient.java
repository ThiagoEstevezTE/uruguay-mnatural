package uy.edu.um.proyectotic2026.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import uy.edu.um.proyectotic2026.dto.ChargeResponseDTO;
import uy.edu.um.proyectotic2026.dto.MercadoPagoPaymentDTO;
import uy.edu.um.proyectotic2026.exceptions.PaymentUnavailableException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

// Cobra con la API de ordenes de Mercado Pago (POST /v1/orders), la de Checkout API.
// Las cuentas nuevas ya no aceptan la vieja /v1/payments: responden 401 "Unauthorized use of live credentials".
// El Access Token vive solo en el servidor: nunca se manda a las pantallas.
@Service
public class MercadoPagoClient {

    private static final Logger log = LoggerFactory.getLogger(MercadoPagoClient.class);

    // Tipos de tarjeta que acepta la orden; si el formulario manda otra cosa, se cobra como credito
    private static final Set<String> CARD_TYPES = Set.of("credit_card", "debit_card", "prepaid_card");

    // Lo que significan los rechazos mas comunes, para mostrarselo al turista en castellano
    private static final Map<String, String> REJECTIONS = Map.of(
            "rejected_by_issuer", "el banco no lo aprobó",
            "insufficient_amount", "la tarjeta no tiene saldo suficiente",
            "bad_filled_security_code", "el código de seguridad está mal",
            "bad_filled_date", "la fecha de vencimiento está mal",
            "bad_filled_other", "algún dato de la tarjeta está mal");

    private final RestClient restClient;

    public MercadoPagoClient(@Value("${mercadopago.access-token:}") String accessToken,
                             @Value("${mercadopago.url:https://api.mercadopago.com}") String url) {
        // Este cliente HTTP lee el cuerpo de las respuestas de error, asi el motivo queda en el log
        HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
        JdkClientHttpRequestFactory timeouts = new JdkClientHttpRequestFactory(httpClient);
        timeouts.setReadTimeout(Duration.ofSeconds(15));

        this.restClient = RestClient.builder()
                .baseUrl(url)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .requestFactory(timeouts)
                .build();
    }

    // El monto lo pone el servidor: nunca se toma el que venga del navegador.
    public ChargeResponseDTO pay(MercadoPagoPaymentDTO data, BigDecimal amount, String reference, String description)
            throws PaymentUnavailableException {

        String total = amount.setScale(2, RoundingMode.HALF_UP).toPlainString();

        Map<String, Object> payer = new HashMap<>();
        payer.put("email", data.getPayerEmail());
        if (data.getIdentificationNumber() != null && !data.getIdentificationNumber().isBlank()) {
            payer.put("identification", Map.of(
                    "type", data.getIdentificationType(),
                    "number", data.getIdentificationNumber()));
        }

        Map<String, Object> paymentMethod = new HashMap<>();
        paymentMethod.put("id", data.getPaymentMethodId());
        paymentMethod.put("type", CARD_TYPES.contains(data.getPaymentTypeId()) ? data.getPaymentTypeId() : "credit_card");
        paymentMethod.put("token", data.getToken());
        paymentMethod.put("installments", data.getInstallments());

        Map<String, Object> body = new HashMap<>();
        body.put("type", "online");
        body.put("processing_mode", "automatic");
        body.put("total_amount", total);
        body.put("external_reference", reference);
        body.put("description", description);
        body.put("payer", payer);
        body.put("transactions", Map.of("payments", List.of(Map.of(
                "amount", total,
                "payment_method", paymentMethod))));

        try {
            Map<?, ?> response = restClient.post()
                    .uri("/v1/orders")
                    .contentType(MediaType.APPLICATION_JSON)
                    // Mercado Pago lo exige. Cada intento lleva uno nuevo, asi se puede reintentar
                    // con otra tarjeta; el doble clic ya lo frena el bloqueo de la reserva.
                    .header("X-Idempotency-Key", UUID.randomUUID().toString())
                    .body(body)
                    .exchange((request, res) -> {
                        int status = res.getStatusCode().value();
                        // 402 = la tarjeta fue rechazada: es una respuesta valida, no una caida
                        if (res.getStatusCode().is2xxSuccessful() || status == 402) {
                            return res.bodyTo(Map.class);
                        }
                        String error = new String(res.getBody().readAllBytes(), StandardCharsets.UTF_8);
                        throw new RestClientException("Mercado Pago respondio " + status + ": " + error);
                    });

            return toChargeResponse(response, data.getCardLastDigits());
        } catch (RestClientException e) {
            // Token mal puesto (401), datos invalidos (400), servicio caido o tiempo agotado
            log.error("Fallo el cobro con Mercado Pago: {}", e.getMessage());
            throw new PaymentUnavailableException("El servicio de pagos no está disponible en este momento");
        }
    }

    private ChargeResponseDTO toChargeResponse(Map<?, ?> response, String cardLastDigits) {
        // Si la tarjeta se rechaza, la orden viene adentro de "data"
        Map<?, ?> order = response != null && response.get("data") instanceof Map<?, ?> data ? data : response;
        if (order == null || order.get("id") == null) {
            throw new RestClientException("Mercado Pago respondio sin orden");
        }

        boolean approved = "processed".equals(order.get("status"));
        String detail = paymentStatusDetail(order);

        ChargeResponseDTO result = new ChargeResponseDTO(
                approved ? "approved" : "rejected",
                String.valueOf(order.get("id")),
                approved ? "Pago aprobado" : REJECTIONS.getOrDefault(detail, detail));

        // El pago adentro de la orden: hace falta para devolver solo una parte
        Map<?, ?> payment = firstPayment(order);
        if (payment != null && payment.get("id") != null) {
            result.setPaymentId(String.valueOf(payment.get("id")));
        }

        // La orden no trae los ultimos 4 numeros: los da el formulario de Mercado Pago (solo para mostrar)
        if (cardLastDigits != null && cardLastDigits.matches("\\d{4}")) {
            result.setCardLastDigits(cardLastDigits);
        }
        return result;
    }

    // El motivo esta en el pago de la orden (ej. "accredited" o "rejected_by_issuer")
    private String paymentStatusDetail(Map<?, ?> order) {
        Map<?, ?> payment = firstPayment(order);
        if (payment != null && payment.get("status_detail") != null) {
            return String.valueOf(payment.get("status_detail"));
        }
        return String.valueOf(order.get("status_detail"));
    }

    private Map<?, ?> firstPayment(Map<?, ?> order) {
        if (order.get("transactions") instanceof Map<?, ?> transactions
                && transactions.get("payments") instanceof List<?> payments
                && !payments.isEmpty()
                && payments.get(0) instanceof Map<?, ?> payment) {
            return payment;
        }
        return null;
    }

    // ---------- Devoluciones ----------

    // Devuelve plata de una orden ya cobrada (POST /v1/orders/{id}/refund).
    // Sin cuerpo, Mercado Pago devuelve todo; para devolver una parte hay que mandar el pago y el monto.
    // Devuelve el identificador de la devolucion.
    public String refund(String orderId, String paymentId, BigDecimal amount, boolean total)
            throws PaymentUnavailableException {

        Map<String, Object> body = null;
        if (!total) {
            String refundPaymentId = paymentId != null ? paymentId : findPaymentId(orderId);
            body = Map.of("transactions", List.of(Map.of(
                    "id", refundPaymentId,
                    "amount", amount.setScale(2, RoundingMode.HALF_UP).toPlainString())));
        }

        try {
            var request = restClient.post()
                    .uri("/v1/orders/{id}/refund", orderId)
                    // Si se reintenta, Mercado Pago no devuelve dos veces lo mismo dentro de la misma llamada
                    .header("X-Idempotency-Key", UUID.randomUUID().toString());
            if (body != null) {
                request = request.contentType(MediaType.APPLICATION_JSON).body(body);
            }
            Map<?, ?> response = request.exchange((req, res) -> {
                if (res.getStatusCode().is2xxSuccessful()) {
                    return res.bodyTo(Map.class);
                }
                String error = new String(res.getBody().readAllBytes(), StandardCharsets.UTF_8);
                throw new RestClientException("Mercado Pago respondio " + res.getStatusCode().value() + ": " + error);
            });
            return refundId(response, orderId);
        } catch (RestClientException e) {
            log.error("Fallo la devolucion con Mercado Pago (orden {}): {}", orderId, e.getMessage());
            throw new PaymentUnavailableException("No se pudo hacer la devolución en este momento");
        }
    }

    // Pagos viejos que no guardaron el id del pago: se lo pide a Mercado Pago (GET /v1/orders/{id})
    private String findPaymentId(String orderId) throws PaymentUnavailableException {
        try {
            Map<?, ?> order = restClient.get().uri("/v1/orders/{id}", orderId).retrieve().body(Map.class);
            Map<?, ?> payment = order == null ? null : firstPayment(order);
            if (payment == null || payment.get("id") == null) {
                throw new RestClientException("La orden " + orderId + " no tiene pagos");
            }
            return String.valueOf(payment.get("id"));
        } catch (RestClientException e) {
            log.error("No se pudo leer la orden {} de Mercado Pago: {}", orderId, e.getMessage());
            throw new PaymentUnavailableException("No se pudo hacer la devolución en este momento");
        }
    }

    // La devolucion queda en transactions.refunds de la orden; si no viene, se guarda el id de la orden
    private String refundId(Map<?, ?> response, String orderId) {
        if (response != null && response.get("transactions") instanceof Map<?, ?> transactions
                && transactions.get("refunds") instanceof List<?> refunds && !refunds.isEmpty()
                && refunds.get(refunds.size() - 1) instanceof Map<?, ?> refund && refund.get("id") != null) {
            return String.valueOf(refund.get("id"));
        }
        return orderId;
    }
}
