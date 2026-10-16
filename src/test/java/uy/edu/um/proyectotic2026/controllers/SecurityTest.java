package uy.edu.um.proyectotic2026.controllers;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// Verifica que cada rol solo pueda entrar a su espacio
@SpringBootTest
@AutoConfigureMockMvc
class SecurityTest {

    @Autowired
    private MockMvc mvc;

    @Test
    void publicPagesAreOpen() throws Exception {
        mvc.perform(get("/")).andExpect(status().isOk());
        mvc.perform(get("/login")).andExpect(status().isOk());
    }

    @Test
    void anonymousIsSentToLogin() throws Exception {
        mvc.perform(get("/admin"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminCanEnterAdmin() throws Exception {
        mvc.perform(get("/admin")).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "TOURIST")
    void touristCannotEnterAdminOrProvider() throws Exception {
        mvc.perform(get("/admin")).andExpect(status().isForbidden());
        mvc.perform(get("/provider")).andExpect(status().isForbidden());
    }

    @Test
    void demoAdminCanLogIn() throws Exception {
        mvc.perform(formLogin("/login").user("admin@uymnatural.uy").password("demo1234"))
                .andExpect(redirectedUrl("/panel"));
    }

    @Test
    void wrongPasswordIsRejected() throws Exception {
        mvc.perform(formLogin("/login").user("admin@uymnatural.uy").password("incorrecta"))
                .andExpect(redirectedUrl("/login?error"));
    }
}
