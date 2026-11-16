package uy.edu.um.proyectotic2026.controllers;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;
import uy.edu.um.proyectotic2026.entities.Activity;
import uy.edu.um.proyectotic2026.repository.ActivityPhotoRepository;
import uy.edu.um.proyectotic2026.repository.ActivityRepository;
import uy.edu.um.proyectotic2026.repository.ActivityTypeRepository;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// El prestador sube fotos al crear o editar su actividad, y se ven en las paginas
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ActivityPhotosFlowTest {

    static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 1, 2, 3};
    static final byte[] JPG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 4, 5, 6};

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ActivityRepository activityRepo;

    @Autowired
    private ActivityPhotoRepository photoRepo;

    @Autowired
    private ActivityTypeRepository typeRepo;

    @Autowired
    private EntityManager entityManager;

    private Activity demoActivity() {
        return activityRepo.findByBusinessProviderEmailOrderByNameAsc("prestador@demo.uy").get(0);
    }

    // El formulario de la actividad con los datos que ya tiene
    private MockMultipartHttpServletRequestBuilder form(String url, Activity activity) {
        MockMultipartHttpServletRequestBuilder request = multipart(url);
        request.param("businessId", activity.getBusiness().getId().toString())
                .param("name", activity.getName())
                .param("description", activity.getDescription())
                .param("localityId", activity.getLocality().getId().toString())
                .param("durationMinutes", String.valueOf(activity.getDurationMinutes()))
                .param("price", activity.getPrice().toPlainString())
                .param("typeIds", typeRepo.findAll().get(0).getId().toString());
        return request;
    }

    @Test
    @WithMockUser(username = "prestador@demo.uy", roles = "PROVIDER")
    void createsActivityWithTwoPhotosAndShowsThem() throws Exception {
        Activity base = demoActivity();

        mvc.perform(form("/provider/activities/new", base)
                        .file(new MockMultipartFile("photos", "a.png", "image/png", PNG))
                        .file(new MockMultipartFile("photos", "b.jpg", "image/jpeg", JPG))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("message", "Actividad creada. Ahora agregale fechas y horarios."));

        Activity created = activityRepo.findByBusinessProviderEmailOrderByNameAsc("prestador@demo.uy").stream()
                .filter(a -> !a.getId().equals(base.getId()) && a.getName().equals(base.getName()))
                .findFirst().orElseThrow();
        assertEquals(2, photoRepo.countByActivityId(created.getId()));

        // La foto sale con el tipo que detecto el servidor
        mvc.perform(get("/activities/" + created.getId() + "/photos/1"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("image/jpeg"))
                .andExpect(result -> assertArrayEquals(JPG, result.getResponse().getContentAsByteArray()));
        mvc.perform(get("/activities/" + created.getId() + "/photos/2")).andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "prestador@demo.uy", roles = "PROVIDER")
    void fakePhotoDoesNotCreateActivity() throws Exception {
        Activity base = demoActivity();
        long before = activityRepo.count();

        mvc.perform(form("/provider/activities/new", base)
                        .file(new MockMultipartFile("photos", "foto.jpg", "image/jpeg", "<html></html>".getBytes()))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("provider/activity-form"))
                .andExpect(model().attribute("error", "Las fotos tienen que ser JPG, PNG o WebP"));

        assertEquals(before, activityRepo.count());
    }

    @Test
    @WithMockUser(username = "prestador@demo.uy", roles = "PROVIDER")
    void editWithoutPhotosKeepsThemAndNewOnesReplace() throws Exception {
        Activity activity = demoActivity();
        String url = "/provider/activities/" + activity.getId() + "/edit";

        mvc.perform(form(url, activity).file(new MockMultipartFile("photos", "a.png", "image/png", PNG)).with(csrf()))
                .andExpect(status().is3xxRedirection());
        assertEquals(1, photoRepo.countByActivityId(activity.getId()));

        // Sin elegir fotos: el navegador manda un archivo vacio y quedan las de antes
        mvc.perform(form(url, activity).file(new MockMultipartFile("photos", "", "application/octet-stream", new byte[0]))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection());
        assertEquals(1, photoRepo.countByActivityId(activity.getId()));

        mvc.perform(form(url, activity)
                        .file(new MockMultipartFile("photos", "b.jpg", "image/jpeg", JPG))
                        .file(new MockMultipartFile("photos", "c.png", "image/png", PNG))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection());
        assertEquals(2, photoRepo.countByActivityId(activity.getId()));

        // La portada y el carrusel usan las fotos subidas (se vuelve a leer la actividad, como en cada pagina)
        entityManager.clear();
        mvc.perform(get("/activities/" + activity.getId()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("/activities/" + activity.getId() + "/photos/1")))
                .andExpect(content().string(containsString("carousel")));
    }

    @Test
    @WithMockUser(username = "kayak@demo.uy", roles = "PROVIDER")
    void otherProviderCannotChangePhotos() throws Exception {
        Activity activity = demoActivity();

        mvc.perform(form("/provider/activities/" + activity.getId() + "/edit", activity)
                        .file(new MockMultipartFile("photos", "a.png", "image/png", PNG))
                        .with(csrf()))
                .andExpect(model().attributeExists("error"));

        assertEquals(0, photoRepo.countByActivityId(activity.getId()));
    }

    @Test
    void activityWithoutPhotosKeepsPlacePhoto() throws Exception {
        Activity activity = demoActivity();
        mvc.perform(get("/activities/" + activity.getId()))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("/photos/0"))));
    }
}
