package uy.edu.um.proyectotic2026.controllers;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import uy.edu.um.proyectotic2026.entities.Business;
import uy.edu.um.proyectotic2026.entities.BusinessStatus;
import uy.edu.um.proyectotic2026.repository.BusinessRepository;
import uy.edu.um.proyectotic2026.repository.TouristRepository;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// Prueba los formularios de punta a punta, con la base H2 de los tests
@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class RegistrationFlowTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private TouristRepository touristRepo;

    @Autowired
    private BusinessRepository businessRepo;

    @Test
    void touristRegistersFromTheForm() throws Exception {
        mvc.perform(post("/register/tourist").with(csrf())
                        .param("firstName", "Lucía")
                        .param("lastName", "Gómez")
                        .param("document", "59876543")
                        .param("birthDate", "2001-03-20")
                        .param("email", "lucia@mail.com")
                        .param("password", "secreta123"))
                .andExpect(redirectedUrl("/login"));

        assertTrue(touristRepo.findByEmail("lucia@mail.com").isPresent());
    }

    @Test
    void differentPasswordsAreRejected() throws Exception {
        mvc.perform(post("/register/tourist").with(csrf())
                        .param("firstName", "Lucía")
                        .param("lastName", "Gómez")
                        .param("document", "59876543")
                        .param("birthDate", "2001-03-20")
                        .param("email", "lucia@mail.com")
                        .param("password", "secreta123")
                        .param("confirmPassword", "otra12345"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("error", "Las contraseñas no coinciden"))
                .andExpect(view().name("register/tourist"));

        assertTrue(touristRepo.findByEmail("lucia@mail.com").isEmpty());
    }

    @Test
    void matchingPasswordsRegister() throws Exception {
        mvc.perform(post("/register/tourist").with(csrf())
                        .param("firstName", "Lucía")
                        .param("lastName", "Gómez")
                        .param("document", "59876543")
                        .param("birthDate", "2001-03-20")
                        .param("email", "lucia@mail.com")
                        .param("password", "secreta123")
                        .param("confirmPassword", "secreta123"))
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    void formShowsErrorAndKeepsData() throws Exception {
        mvc.perform(post("/register/tourist").with(csrf())
                        .param("firstName", "Lucía")
                        .param("lastName", "Gómez")
                        .param("document", "59876543")
                        .param("birthDate", "2001-03-20")
                        .param("email", "lucia@mail.com")
                        .param("password", "123"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("error", "La contraseña debe tener al menos 8 caracteres"))
                .andExpect(view().name("register/tourist"));
    }

    @Test
    void providerFormLoadsLocalities() throws Exception {
        mvc.perform(get("/register/provider"))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("localitiesByDepartment"));
    }

    @Test
    @WithMockUser(username = "admin@uymnatural.uy", roles = "ADMIN")
    void adminApprovesPendingBusiness() throws Exception {
        Business pending = businessRepo.findByStatus(BusinessStatus.PENDING).get(0);

        mvc.perform(post("/admin/businesses/" + pending.getId() + "/approve").with(csrf()))
                .andExpect(redirectedUrl("/admin"))
                .andExpect(flash().attributeExists("message"));

        assertEquals(BusinessStatus.APPROVED, businessRepo.findById(pending.getId()).orElseThrow().getStatus());
    }

    @Test
    @WithMockUser(username = "admin@uymnatural.uy", roles = "ADMIN")
    void adminPagesRender() throws Exception {
        mvc.perform(get("/admin")).andExpect(status().isOk());
        mvc.perform(get("/admin/businesses")).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "prestador@demo.uy", roles = "PROVIDER")
    void providerHomeRenders() throws Exception {
        mvc.perform(get("/provider")).andExpect(status().isOk());
    }
}
