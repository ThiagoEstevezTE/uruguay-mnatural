package uy.edu.um.proyectotic2026.controllers;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import uy.edu.um.proyectotic2026.entities.Business;
import uy.edu.um.proyectotic2026.entities.BusinessStatus;
import uy.edu.um.proyectotic2026.repository.ActivityRepository;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// Que las pantallas del prestador y del administrador se muestren sin errores
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ProviderAndAdminPagesTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ActivityRepository activityRepo;

    @Test
    @WithMockUser(username = "prestador@demo.uy", roles = "PROVIDER")
    void providerActivityPagesRender() throws Exception {
        Long id = activityRepo.findByBusinessProviderEmailOrderByNameAsc("prestador@demo.uy").get(0).getId();

        mvc.perform(get("/provider")).andExpect(status().isOk());
        mvc.perform(get("/provider/activities/new")).andExpect(status().isOk());
        mvc.perform(get("/provider/activities/" + id)).andExpect(status().isOk());
        mvc.perform(get("/provider/activities/" + id + "/edit")).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "prestador@demo.uy", roles = "PROVIDER")
    void providerEditsHisBusinessFromForm() throws Exception {
        Business business = activityRepo.findByBusinessProviderEmailOrderByNameAsc("prestador@demo.uy").get(0).getBusiness();

        mvc.perform(get("/provider/businesses/" + business.getId() + "/edit")).andExpect(status().isOk());

        mvc.perform(post("/provider/businesses/" + business.getId() + "/edit").with(csrf())
                        .param("name", "Cabalgatas del Faro")
                        .param("address", "Ruta 10 km 264")
                        .param("localityId", business.getLocality().getId().toString()))
                .andExpect(redirectedUrl("/provider"))
                .andExpect(flash().attribute("message", "Cambios guardados."));
        assertEquals(BusinessStatus.APPROVED, business.getStatus());
    }

    @Test
    @WithMockUser(username = "kayak@demo.uy", roles = "PROVIDER")
    void providerCannotEditBusinessOfOther() throws Exception {
        Long id = activityRepo.findByBusinessProviderEmailOrderByNameAsc("prestador@demo.uy").get(0).getBusiness().getId();

        mvc.perform(get("/provider/businesses/" + id + "/edit"))
                .andExpect(view().name("error/not-found"));
    }

    @Test
    @WithMockUser(username = "admin@uymnatural.uy", roles = "ADMIN")
    void adminSeesCancellationReasons() throws Exception {
        mvc.perform(get("/admin/cancellation-reasons"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Mal clima")));
    }

    @Test
    @WithMockUser(username = "prestador@demo.uy", roles = "PROVIDER")
    void providerCannotOpenCancellationReasons() throws Exception {
        mvc.perform(get("/admin/cancellation-reasons")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "kayak@demo.uy", roles = "PROVIDER")
    void providerCannotOpenActivityOfOther() throws Exception {
        Long id = activityRepo.findByBusinessProviderEmailOrderByNameAsc("prestador@demo.uy").get(0).getId();

        mvc.perform(get("/provider/activities/" + id))
                .andExpect(view().name("error/not-found"));
    }

    @Test
    @WithMockUser(username = "pendiente@demo.uy", roles = "PROVIDER")
    void pendingProviderCannotCreateActivities() throws Exception {
        mvc.perform(get("/provider/activities/new"))
                .andExpect(redirectedUrl("/provider"))
                .andExpect(flash().attributeExists("error"));
    }

    @Test
    @WithMockUser(username = "prestador@demo.uy", roles = "PROVIDER")
    void providerAddsInstanceFromForm() throws Exception {
        Long id = activityRepo.findByBusinessProviderEmailOrderByNameAsc("prestador@demo.uy").get(0).getId();

        mvc.perform(post("/provider/activities/" + id + "/instances").with(csrf())
                        .param("start", "2099-01-15T10:00")
                        .param("maxCapacity", "8"))
                .andExpect(redirectedUrl("/provider/activities/" + id))
                .andExpect(flash().attribute("message", "Fecha agregada."));
    }

    @Test
    @WithMockUser(username = "admin@uymnatural.uy", roles = "ADMIN")
    void adminCatalogPagesRender() throws Exception {
        mvc.perform(get("/admin/activity-types")).andExpect(status().isOk());
        mvc.perform(get("/admin/equipment")).andExpect(status().isOk());
    }

    // Los botones de guardar y eliminar del catalogo son solo un icono: llevan nombre para los lectores de pantalla
    @Test
    @WithMockUser(username = "admin@uymnatural.uy", roles = "ADMIN")
    void catalogIconButtonsHaveNames() throws Exception {
        mvc.perform(get("/admin/equipment"))
                .andExpect(content().string(containsString("aria-label=\"Guardar\"")))
                .andExpect(content().string(containsString("aria-label=\"Eliminar\"")));
    }
}
