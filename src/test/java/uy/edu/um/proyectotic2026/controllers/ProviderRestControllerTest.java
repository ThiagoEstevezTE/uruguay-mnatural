package uy.edu.um.proyectotic2026.controllers;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// Los mismos casos que se probaban a mano con Postman
@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class ProviderRestControllerTest {

    @Autowired
    private MockMvc mvc;

    private String body(String email, String password, String document, long localityId) {
        return """
                {
                  "email": "%s",
                  "password": "%s",
                  "name": "Ana Perez",
                  "document": "%s",
                  "phone": "099123456",
                  "type": "INDIVIDUAL",
                  "businessName": "Kayak en la laguna",
                  "businessDescription": "Paseos en kayak",
                  "businessAddress": "Ruta 9 km 220",
                  "localityId": %d
                }
                """.formatted(email, password, document, localityId);
    }

    @Test
    void registerReturnsCreatedAndPending() throws Exception {
        mvc.perform(post("/api/v1/provider/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("Ana@Mail.com", "secreta123", "12345678", 22)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("ana@mail.com"))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.businesses[0].status").value("PENDING"))
                .andExpect(jsonPath("$.businesses[0].locality").value("Cabo Polonio"));
    }

    @Test
    void duplicatedEmailReturnsBadRequest() throws Exception {
        mvc.perform(post("/api/v1/provider/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("prestador@demo.uy", "secreta123", "99999999", 22)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shortPasswordReturnsBadRequest() throws Exception {
        mvc.perform(post("/api/v1/provider/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("otro@mail.com", "123", "87654321", 22)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void unknownLocalityReturnsNotFound() throws Exception {
        mvc.perform(post("/api/v1/provider/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("nuevo@mail.com", "secreta123", "11223344", 999)))
                .andExpect(status().isNotFound());
    }
}
