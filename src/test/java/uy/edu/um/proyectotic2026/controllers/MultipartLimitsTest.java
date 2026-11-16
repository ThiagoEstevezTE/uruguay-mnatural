package uy.edu.um.proyectotic2026.controllers;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.assertNotEquals;

// El formulario de actividad manda unas 50 partes (cada casilla lleva un campo oculto) y ahora tambien fotos.
// Esto lo controla el servidor de verdad (Tomcat), por eso la prueba no usa MockMvc.
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class MultipartLimitsTest {

    @LocalServerPort
    private int port;

    @Test
    void formWithManyFieldsIsNotCutByTomcat() throws Exception {
        String boundary = "limite";
        StringBuilder body = new StringBuilder();
        for (int i = 0; i < 80; i++) {
            body.append("--").append(boundary).append("\r\n")
                    .append("Content-Disposition: form-data; name=\"_typeIds\"\r\n\r\n")
                    .append("on\r\n");
        }
        body.append("--").append(boundary).append("--\r\n");

        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/provider/activities/new"))
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                .build();
        HttpResponse<String> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());

        // Sin sesion ni CSRF la app lo frena igual (403), pero no con 413: Tomcat no corto el formulario
        assertNotEquals(413, response.statusCode());
    }
}
