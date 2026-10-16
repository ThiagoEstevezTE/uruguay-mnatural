package uy.edu.um.proyectotic2026.services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import uy.edu.um.proyectotic2026.dto.ChargeRequestDTO;
import uy.edu.um.proyectotic2026.dto.ChargeResponseDTO;
import uy.edu.um.proyectotic2026.exceptions.PaymentUnavailableException;

import java.math.BigDecimal;

// Integracion con el servicio de pagos externo, por su API REST (lo pide la letra).
// La direccion del servicio se configura en application.properties (payment.gateway.url).
@Service
public class PaymentGatewayClient {

    private final RestClient restClient;

    public PaymentGatewayClient(@Value("${payment.gateway.url}") String gatewayUrl) {
        // Si el servicio tarda mas de 5 segundos, se corta: no se deja al turista esperando
        SimpleClientHttpRequestFactory timeouts = new SimpleClientHttpRequestFactory();
        timeouts.setConnectTimeout(5000);
        timeouts.setReadTimeout(5000);

        this.restClient = RestClient.builder()
                .baseUrl(gatewayUrl)
                .requestFactory(timeouts)
                .build();
    }

    // Devuelve la respuesta del servicio (aprobado o rechazado).
    // Si el servicio no esta disponible, tira PaymentUnavailableException.
    public ChargeResponseDTO charge(String cardNumber, BigDecimal amount, String description)
            throws PaymentUnavailableException {
        try {
            return restClient.post()
                    .uri("/api/v1/charges")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new ChargeRequestDTO(cardNumber, amount, "UYU", description))
                    .retrieve()
                    // 402 = pago rechazado: es una respuesta normal, no un error de conexion
                    .onStatus(status -> status.value() == 402, (request, response) -> { })
                    .body(ChargeResponseDTO.class);
        } catch (RestClientException e) {
            // Error 5xx, tiempo de espera agotado o no se pudo conectar
            throw new PaymentUnavailableException("El servicio de pagos no está disponible en este momento");
        }
    }
}
