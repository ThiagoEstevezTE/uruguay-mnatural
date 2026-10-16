package uy.edu.um.proyectotic2026.controllers;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import uy.edu.um.proyectotic2026.dto.ChargeResponseDTO;
import uy.edu.um.proyectotic2026.entities.Activity;
import uy.edu.um.proyectotic2026.entities.Booking;
import uy.edu.um.proyectotic2026.entities.BookingStatus;
import uy.edu.um.proyectotic2026.repository.ActivityRepository;
import uy.edu.um.proyectotic2026.repository.BookingRepository;
import uy.edu.um.proyectotic2026.services.PaymentGatewayClient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// El recorrido del turista de punta a punta: reservar, pagar y ver la reserva con su QR
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class TouristFlowTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ActivityRepository activityRepo;

    @Autowired
    private BookingRepository bookingRepo;

    @MockitoBean
    private PaymentGatewayClient paymentGateway;

    @Test
    void anonymousIsAskedToLogInToBook() throws Exception {
        mvc.perform(get("/tourist/book/1")).andExpect(redirectedUrl("/login"));
    }

    @Test
    @WithMockUser(username = "turista@demo.uy", roles = "TOURIST")
    void bookAndPay() throws Exception {
        when(paymentGateway.charge(anyString(), any(), anyString()))
                .thenReturn(new ChargeResponseDTO("approved", "txn_1", "Pago aprobado"));

        Activity kayak = activityRepo.findAll().stream()
                .filter(a -> a.getName().startsWith("Kayak")).findFirst().orElseThrow();
        Long instanceId = kayak.getUpcomingInstances().get(1).getId();

        mvc.perform(get("/tourist/book/" + instanceId)).andExpect(status().isOk());

        String payUrl = mvc.perform(post("/tourist/book/" + instanceId).with(csrf())
                        .param("participants", "2").param("observations", "Uno no sabe nadar"))
                .andExpect(status().is3xxRedirection())
                .andReturn().getResponse().getRedirectedUrl();

        mvc.perform(get(payUrl)).andExpect(status().isOk());
        mvc.perform(post(payUrl).with(csrf()).param("cardNumber", "4242424242424242"))
                .andExpect(flash().attributeExists("message"));

        Booking booking = bookingRepo.findByTouristEmailOrderByCreatedAtDesc("turista@demo.uy").get(0);
        assertEquals(BookingStatus.CONFIRMED, booking.getStatus());

        mvc.perform(get("/tourist/bookings")).andExpect(status().isOk());
        mvc.perform(get("/tourist/bookings/" + booking.getId())).andExpect(status().isOk());
        mvc.perform(get("/tourist/bookings/" + booking.getId() + "/qr.png"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.IMAGE_PNG));
    }

    @Test
    @WithMockUser(username = "lucia@demo.uy", roles = "TOURIST")
    void touristCannotOpenBookingOfOther() throws Exception {
        Long id = bookingRepo.findByCode("UM-PROX7A").orElseThrow().getId();
        mvc.perform(get("/tourist/bookings/" + id)).andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "prestador@demo.uy", roles = "PROVIDER")
    void providerSeesBookingsAndChecksIn() throws Exception {
        mvc.perform(get("/provider/bookings")).andExpect(status().isOk()).andExpect(model().attributeExists("bookings"));

        // UM-H0Y2Q8 es la reserva de ejemplo de hoy
        Booking today = bookingRepo.findByCode("UM-H0Y2Q8").orElseThrow();
        if (today.getInstance().getStartDateTime().toLocalDate().equals(java.time.LocalDate.now())) {
            mvc.perform(post("/provider/bookings/check-in").with(csrf()).param("code", "UM-H0Y2Q8"))
                    .andExpect(flash().attributeExists("message"));
            assertEquals(BookingStatus.COMPLETED, bookingRepo.findByCode("UM-H0Y2Q8").orElseThrow().getStatus());
        }
    }

    @Test
    void fakeGatewaySimulatesTheThreeAnswers() throws Exception {
        String body = "{\"cardNumber\":\"%s\",\"amount\":100,\"currency\":\"UYU\",\"description\":\"test\"}";
        mvc.perform(post("/fake-gateway/api/v1/charges").contentType(MediaType.APPLICATION_JSON)
                        .content(body.formatted("4242424242424242")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("approved"));
        mvc.perform(post("/fake-gateway/api/v1/charges").contentType(MediaType.APPLICATION_JSON)
                        .content(body.formatted("4000000000000002")))
                .andExpect(status().isPaymentRequired()).andExpect(jsonPath("$.status").value("rejected"));
        mvc.perform(post("/fake-gateway/api/v1/charges").contentType(MediaType.APPLICATION_JSON)
                        .content(body.formatted("4000000000000119")))
                .andExpect(status().isServiceUnavailable());
    }
}
