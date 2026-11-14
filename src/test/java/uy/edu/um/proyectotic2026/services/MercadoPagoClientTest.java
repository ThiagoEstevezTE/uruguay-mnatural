package uy.edu.um.proyectotic2026.services;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uy.edu.um.proyectotic2026.dto.ChargeResponseDTO;
import uy.edu.um.proyectotic2026.dto.MercadoPagoPaymentDTO;
import uy.edu.um.proyectotic2026.exceptions.PaymentUnavailableException;

import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

// Prueba el cliente contra un servidor falso: no sale a internet ni usa credenciales reales
class MercadoPagoClientTest {

    private HttpServer server;
    private String receivedBody;
    private String receivedAuth;
    private String receivedKey;
    private int responseStatus;
    private String responseBody;

    @BeforeEach
    void startServer() throws Exception {
        server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/v1/payments", exchange -> {
            receivedBody = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            receivedAuth = exchange.getRequestHeaders().getFirst("Authorization");
            receivedKey = exchange.getRequestHeaders().getFirst("X-Idempotency-Key");
            byte[] bytes = responseBody.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(responseStatus, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
        server.start();
    }

    @AfterEach
    void stopServer() {
        server.stop(0);
    }

    private MercadoPagoClient client() {
        return new MercadoPagoClient("token-de-prueba", "http://localhost:" + server.getAddress().getPort());
    }

    private MercadoPagoPaymentDTO data() {
        MercadoPagoPaymentDTO data = new MercadoPagoPaymentDTO();
        data.setToken("tok_1");
        data.setPaymentMethodId("master");
        data.setIssuerId("310");
        data.setInstallments(1);
        data.setPayerEmail("comprador@test.uy");
        data.setIdentificationType("CI");
        data.setIdentificationNumber("12345678");
        return data;
    }

    @Test
    void approvedPaymentSendsWhatMercadoPagoAsksFor() throws Exception {
        responseStatus = 201;
        responseBody = "{\"id\":123,\"status\":\"approved\",\"status_detail\":\"accredited\",\"card\":{\"last_four_digits\":\"0604\"}}";

        ChargeResponseDTO result = client().pay(data(), new BigDecimal("1500.00"), "Reserva UM-1");

        assertTrue(result.isApproved());
        assertEquals("123", result.getTransactionId());
        assertEquals("0604", result.getCardLastDigits());
        assertEquals("Bearer token-de-prueba", receivedAuth);
        assertNotNull(receivedKey);
        assertTrue(receivedBody.contains("\"transaction_amount\":1500.00"));
        assertTrue(receivedBody.contains("\"token\":\"tok_1\""));
        assertTrue(receivedBody.contains("\"payment_method_id\":\"master\""));
        assertTrue(receivedBody.contains("\"number\":\"12345678\""));
    }

    @Test
    void rejectedPaymentIsNotApproved() throws Exception {
        responseStatus = 201;
        responseBody = "{\"id\":124,\"status\":\"rejected\",\"status_detail\":\"cc_rejected_other_reason\"}";

        ChargeResponseDTO result = client().pay(data(), BigDecimal.TEN, "Reserva UM-1");

        assertFalse(result.isApproved());
        assertEquals("cc_rejected_other_reason", result.getMessage());
    }

    @Test
    void badCredentialsAreReportedAsUnavailable() {
        responseStatus = 401;
        responseBody = "{\"message\":\"invalid_token\"}";

        assertThrows(PaymentUnavailableException.class,
                () -> client().pay(data(), BigDecimal.TEN, "Reserva UM-1"));
    }
}