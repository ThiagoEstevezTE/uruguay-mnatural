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
        server.createContext("/v1/orders", exchange -> {
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
        data.setPaymentTypeId("credit_card");
        data.setCardLastDigits("0604");
        data.setIssuerId("310");
        data.setInstallments(1);
        data.setPayerEmail("comprador@test.uy");
        data.setIdentificationType("CI");
        data.setIdentificationNumber("12345678");
        return data;
    }

    private static final String APPROVED = "{\"id\":\"ORDTST01\",\"status\":\"processed\",\"status_detail\":\"accredited\","
            + "\"transactions\":{\"payments\":[{\"id\":\"PAY01\",\"status\":\"processed\",\"status_detail\":\"accredited\"}]}}";

    // Asi responde Mercado Pago cuando la tarjeta se rechaza (titular OTHE): 402 y la orden adentro de "data"
    private static final String REJECTED = "{\"errors\":[{\"code\":\"failed\"}],\"data\":{\"id\":\"ORDTST02\",\"status\":\"failed\","
            + "\"status_detail\":\"failed\",\"transactions\":{\"payments\":[{\"id\":\"PAY02\",\"status\":\"failed\","
            + "\"status_detail\":\"rejected_by_issuer\"}]}}}";

    @Test
    void approvedOrderSendsWhatMercadoPagoAsksFor() throws Exception {
        responseStatus = 201;
        responseBody = APPROVED;

        ChargeResponseDTO result = client().pay(data(), new BigDecimal("1500"), "UM-1", "Reserva UM-1");

        assertTrue(result.isApproved());
        assertEquals("ORDTST01", result.getTransactionId());
        assertEquals("0604", result.getCardLastDigits());
        assertEquals("Bearer token-de-prueba", receivedAuth);
        assertNotNull(receivedKey);
        assertTrue(receivedBody.contains("\"type\":\"online\""));
        assertTrue(receivedBody.contains("\"total_amount\":\"1500.00\""));
        assertTrue(receivedBody.contains("\"amount\":\"1500.00\""));
        assertTrue(receivedBody.contains("\"external_reference\":\"UM-1\""));
        assertTrue(receivedBody.contains("\"token\":\"tok_1\""));
        assertTrue(receivedBody.contains("\"id\":\"master\""));
        assertTrue(receivedBody.contains("\"type\":\"credit_card\""));
        assertTrue(receivedBody.contains("\"number\":\"12345678\""));
    }

    @Test
    void debitCardIsSentAsDebit() throws Exception {
        responseStatus = 201;
        responseBody = APPROVED;
        MercadoPagoPaymentDTO debit = data();
        debit.setPaymentTypeId("debit_card");

        client().pay(debit, BigDecimal.TEN, "UM-1", "Reserva UM-1");

        assertTrue(receivedBody.contains("\"type\":\"debit_card\""));
    }

    @Test
    void unknownCardTypeIsSentAsCredit() throws Exception {
        responseStatus = 201;
        responseBody = APPROVED;
        MercadoPagoPaymentDTO weird = data();
        weird.setPaymentTypeId("<script>");

        client().pay(weird, BigDecimal.TEN, "UM-1", "Reserva UM-1");

        assertTrue(receivedBody.contains("\"type\":\"credit_card\""));
        assertFalse(receivedBody.contains("<script>"));
    }

    @Test
    void lastDigitsThatAreNotFourNumbersAreIgnored() throws Exception {
        responseStatus = 201;
        responseBody = APPROVED;
        MercadoPagoPaymentDTO bad = data();
        bad.setCardLastDigits("12ab");

        ChargeResponseDTO result = client().pay(bad, BigDecimal.TEN, "UM-1", "Reserva UM-1");

        assertTrue(result.isApproved());
        assertNull(result.getCardLastDigits());
    }

    @Test
    void rejectedCardIsNotApprovedAndExplainsWhy() throws Exception {
        responseStatus = 402;
        responseBody = REJECTED;

        ChargeResponseDTO result = client().pay(data(), BigDecimal.TEN, "UM-1", "Reserva UM-1");

        assertFalse(result.isApproved());
        assertEquals("ORDTST02", result.getTransactionId());
        assertEquals("el banco no lo aprobó", result.getMessage());
        assertEquals("0604", result.getCardLastDigits());
    }

    @Test
    void badCredentialsAreReportedAsUnavailable() {
        responseStatus = 401;
        responseBody = "{\"message\":\"Unauthorized use of live credentials\"}";

        assertThrows(PaymentUnavailableException.class,
                () -> client().pay(data(), BigDecimal.TEN, "UM-1", "Reserva UM-1"));
    }

    @Test
    void invalidDataIsReportedAsUnavailable() {
        responseStatus = 400;
        responseBody = "{\"errors\":[{\"code\":\"invalid_properties\"}]}";

        assertThrows(PaymentUnavailableException.class,
                () -> client().pay(data(), BigDecimal.TEN, "UM-1", "Reserva UM-1"));
    }

    @Test
    void answerWithoutOrderIsReportedAsUnavailable() {
        responseStatus = 201;
        responseBody = "{}";

        assertThrows(PaymentUnavailableException.class,
                () -> client().pay(data(), BigDecimal.TEN, "UM-1", "Reserva UM-1"));
    }
}
