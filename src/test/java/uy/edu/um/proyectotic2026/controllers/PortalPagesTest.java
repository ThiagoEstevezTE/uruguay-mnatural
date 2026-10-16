package uy.edu.um.proyectotic2026.controllers;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import uy.edu.um.proyectotic2026.repository.ActivityRepository;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// El portal se ve sin iniciar sesion (la letra: "sin necesidad de registrarse")
@SpringBootTest
@AutoConfigureMockMvc
class PortalPagesTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ActivityRepository activityRepo;

    @Test
    void homeAndSearchArePublic() throws Exception {
        mvc.perform(get("/")).andExpect(status().isOk()).andExpect(model().attributeExists("featured"));
        mvc.perform(get("/activities").param("keyword", "kayak").param("maxPrice", "5000"))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("activities"));
    }

    @Test
    void activityDetailIsPublic() throws Exception {
        Long id = activityRepo.findAll().get(0).getId();
        mvc.perform(get("/activities/" + id))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("instances"));
    }

    // Las fotos de los lugares tienen licencia libre: su pagina de creditos se ve sin iniciar sesion
    @Test
    void photoCreditsArePublic() throws Exception {
        mvc.perform(get("/credits"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Jorgestenopeico")))
                .andExpect(content().string(containsString("CC BY-SA 4.0")));
    }

    // Una actividad de un lugar con foto muestra la foto del lugar, con su texto alternativo
    @Test
    void activityOfAPlaceWithPhotoShowsThePhoto() throws Exception {
        Long id = activityRepo.findAll().stream()
                .filter(a -> a.getLocality().getName().equals("Cabo Polonio"))
                .findFirst().orElseThrow().getId();
        mvc.perform(get("/activities/" + id))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("/img/lugares/cabo-polonio.jpg")))
                .andExpect(content().string(containsString("alt=\"Playa y dunas de Cabo Polonio\"")));
    }

    @Test
    void unknownActivityReturnsNotFound() throws Exception {
        mvc.perform(get("/activities/99999"))
                .andExpect(status().isNotFound())
                .andExpect(view().name("error/not-found"));
    }
}
