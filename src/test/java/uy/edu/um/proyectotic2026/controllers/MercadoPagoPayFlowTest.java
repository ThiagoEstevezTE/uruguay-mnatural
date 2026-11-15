package uy.edu.um.proyectotic2026.controllers;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import uy.edu.um.proyectotic2026.dto.ChargeResponseDTO;
import uy.edu.um.proyectotic2026.entities.Activity;
import uy.edu.um.proyectotic2026.entities.Booking;
import uy.edu.um.proyectotic2026.entities.BookingStatus;
import uy.edu.um.proyectotic2026.repository.ActivityRepository;
import uy.edu.um.proyectotic2026.repository.BookingRepository;
import uy.edu.um.proyectotic2026.services.MercadoPagoClient;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// Con Mercado Pago prendido: se ve su formulario, el simulador no confirma y el token si
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@TestPropertySource(properties = {"payment.provider=mercadopago", "mercadopago.public-key=clave-publica-de-prueba"})
class MercadoPagoPayFlowTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ActivityRepository activityRepo;

    @Autowired
    private BookingRepository bookingRepo;

    @MockitoBean
    private MercadoPagoClient mercadoPago;

    private String newPendingBookingPayUrl() throws Exception {
        Activity kayak = activityRepo.findAll().stream()
                .filter(a -> a.getName().startsWith("Kayak")).findFirst().orElseThrow();
        Long instanceId = kayak.getUpcomingInstances().get(1).getId();
        return mvc.perform(post("/tourist/book/" + instanceId).with(csrf()).param("participants", "1"))
                .andReturn().getResponse().getRedirectedUrl();
    }

    private Booking latestBooking() {
        return bookingRepo.findByTouristEmailOrderByCreatedAtDesc("turista@demo.uy").get(0);
    }

    @Test
    @WithMockUser(username = "turista@demo.uy", roles = "TOURIST")
    void payPageShowsTheBrickAndNotTheCardInput() throws Exception {
        mvc.perform(get(newPendingBookingPayUrl()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("id=\"mp-brick\"")))
                .andExpect(content().string(containsString("clave-publica-de-prueba")))
                .andExpect(content().string(containsString("_csrf")))
                .andExpect(content().string(not(containsString("name=\"cardNumber\""))));
    }

    @Test
    @WithMockUser(username = "turista@demo.uy", roles = "TOURIST")
    void simulatorCannotConfirmWhenMercadoPagoIsOn() throws Exception {
        mvc.perform(post(newPendingBookingPayUrl()).with(csrf()).param("cardNumber", "4242424242424242"))
                .andExpect(flash().attributeExists("error"));

        assertEquals(BookingStatus.PENDING, latestBooking().getStatus());
    }

    @Test
    @WithMockUser(username = "turista@demo.uy", roles = "TOURIST")
    void approvedTokenConfirmsBooking() throws Exception {
        ChargeResponseDTO approved = new ChargeResponseDTO("approved", "123", "Pago aprobado");
        approved.setCardLastDigits("0604");
        when(mercadoPago.pay(any(), any(), anyString(), anyString())).thenReturn(approved);

        mvc.perform(post(newPendingBookingPayUrl() + "-mp").with(csrf())
                        .param("token", "tok_1").param("paymentMethodId", "master")
                        .param("installments", "1").param("payerEmail", "comprador@test.uy"))
                .andExpect(flash().attributeExists("message"));

        Booking booking = latestBooking();
        assertEquals(BookingStatus.CONFIRMED, booking.getStatus());
        assertEquals("0604", booking.getPayments().get(0).getCardLastDigits());
        assertEquals("master", booking.getPayments().get(0).getCardBrand());
    }

    @Test
    @WithMockUser(username = "turista@demo.uy", roles = "TOURIST")
    void rejectedTokenKeepsBookingPending() throws Exception {
        when(mercadoPago.pay(any(), any(), anyString(), anyString()))
                .thenReturn(new ChargeResponseDTO("rejected", "124", "cc_rejected_other_reason"));

        mvc.perform(post(newPendingBookingPayUrl() + "-mp").with(csrf())
                        .param("token", "tok_1").param("paymentMethodId", "master")
                        .param("installments", "1").param("payerEmail", "comprador@test.uy"))
                .andExpect(flash().attributeExists("error"));

        assertEquals(BookingStatus.PENDING, latestBooking().getStatus());
    }
}