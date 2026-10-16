package uy.edu.um.proyectotic2026.controllers;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import uy.edu.um.proyectotic2026.entities.BusinessStatus;
import uy.edu.um.proyectotic2026.repository.BusinessRepository;
import uy.edu.um.proyectotic2026.repository.DepartmentRepository;

import java.time.LocalDate;
import java.time.YearMonth;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// Las 5 operaciones de la API que pide la letra
@SpringBootTest
@AutoConfigureMockMvc
class PublicApiRestControllerTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private BusinessRepository businessRepo;

    @Autowired
    private DepartmentRepository departmentRepo;

    private Long departmentId(String name) {
        return departmentRepo.findAll().stream().filter(d -> d.getName().equals(name)).findFirst().orElseThrow().getId();
    }

    @Test
    void listsOnlyApprovedBusinesses() throws Exception {
        mvc.perform(get("/api/v1/businesses"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(4)))
                .andExpect(jsonPath("$[*].status", everyItem(is("APPROVED"))));
    }

    @Test
    void verifiesBusiness() throws Exception {
        Long approved = businessRepo.findByStatus(BusinessStatus.APPROVED).get(0).getId();
        Long pending = businessRepo.findByStatus(BusinessStatus.PENDING).get(0).getId();

        mvc.perform(get("/api/v1/businesses/" + approved + "/verify")).andExpect(jsonPath("$.verified").value(true));
        mvc.perform(get("/api/v1/businesses/" + pending + "/verify")).andExpect(jsonPath("$.verified").value(false));
        mvc.perform(get("/api/v1/businesses/9999/verify")).andExpect(status().isNotFound());
    }

    @Test
    void activitiesOfADayInADepartment() throws Exception {
        // En Lavalleja el trekking tiene fecha dentro de 2 dias y el rappel no
        mvc.perform(get("/api/v1/activities")
                        .param("date", LocalDate.now().plusDays(2).toString())
                        .param("departmentId", departmentId("Lavalleja").toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name").value("Trekking al Salto del Penitente"));
    }

    @Test
    void topRatedOfTheMonth() throws Exception {
        // Las valoraciones de ejemplo son de dias pasados: se busca en el mes de la de la cabalgata
        YearMonth month = YearMonth.from(LocalDate.now().minusDays(8));
        mvc.perform(get("/api/v1/activities/top")
                        .param("departmentId", departmentId("Rocha").toString())
                        .param("month", month.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name").value("Cabalgata al atardecer por las dunas"))
                .andExpect(jsonPath("$[0].averageRating").value(4.5));
    }

    @Test
    void searchByKeyword() throws Exception {
        mvc.perform(get("/api/v1/activities/search").param("keyword", "vinos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].department").value("Colonia"));
    }

    @Test
    void apiDocumentationIsAvailable() throws Exception {
        mvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/v1/activities/top']").exists());
    }
}
