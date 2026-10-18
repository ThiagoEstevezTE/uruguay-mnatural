package uy.edu.um.proyectotic2026.controllers;

import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Las listas de reservas traen la valoracion de cada reserva en la misma consulta,
// no con una consulta por reserva (con Supabase cada consulta tarda)
@SpringBootTest
@AutoConfigureMockMvc
class BookingListsQueriesTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private EntityManagerFactory emf;

    @Test
    @WithMockUser(username = "turista@demo.uy", roles = "TOURIST")
    void touristBookingsUseFewQueries() throws Exception {
        long queries = countQueries("/tourist/bookings");
        assertTrue(queries <= 6, "Mis reservas hizo " + queries + " consultas");
    }

    @Test
    @WithMockUser(username = "prestador@demo.uy", roles = "PROVIDER")
    void providerBookingsUseFewQueries() throws Exception {
        long queries = countQueries("/provider/bookings");
        assertTrue(queries <= 6, "Las reservas del prestador hicieron " + queries + " consultas");
    }

    private long countQueries(String url) throws Exception {
        Statistics stats = emf.unwrap(SessionFactory.class).getStatistics();
        stats.setStatisticsEnabled(true);
        stats.clear();
        mvc.perform(get(url)).andExpect(status().isOk());
        long queries = stats.getPrepareStatementCount();
        stats.setStatisticsEnabled(false);
        return queries;
    }
}
