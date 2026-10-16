package uy.edu.um.proyectotic2026.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;
import uy.edu.um.proyectotic2026.dto.ChargeResponseDTO;
import uy.edu.um.proyectotic2026.entities.*;
import uy.edu.um.proyectotic2026.exceptions.EntityNotFoundException;
import uy.edu.um.proyectotic2026.exceptions.InvalidDataException;
import uy.edu.um.proyectotic2026.exceptions.PaymentUnavailableException;
import uy.edu.um.proyectotic2026.repository.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

// Reservas y pagos. El servicio de pagos externo se reemplaza por un mock:
// asi cada test decide si el pago se aprueba, se rechaza o el servicio no responde.
@SpringBootTest
@Transactional
class BookingServiceTest {

    private static final String TOURIST = "turista@demo.uy";

    @Autowired
    private BookingService bookingService;

    @Autowired
    private ActivityRepository activityRepo;

    @Autowired
    private ActivityInstanceRepository instanceRepo;

    @Autowired
    private BookingRepository bookingRepo;

    @Autowired
    private TouristRepository touristRepo;

    @MockitoBean
    private PaymentGatewayClient paymentGateway;

    private Activity trekking;

    @BeforeEach
    void setUp() {
        trekking = activityRepo.findAll().stream()
                .filter(a -> a.getName().startsWith("Trekking")).findFirst().orElseThrow();
    }

    private ActivityInstance newInstance(Activity activity, LocalDateTime start, Integer capacity) {
        ActivityInstance instance = ActivityInstance.builder().startDateTime(start).maxCapacity(capacity).build();
        activity.addInstance(instance);
        return instanceRepo.save(instance);
    }

    private Booking newBooking() throws Exception {
        ActivityInstance instance = newInstance(trekking, LocalDateTime.now().plusDays(5), null);
        return bookingService.createBooking(instance.getId(), TOURIST, 2, " Soy celíaca ");
    }

    // ---------- Reservar ----------

    @Test
    void createsPendingBookingWithTotalAndDeadline() throws Exception {
        Booking booking = newBooking();

        assertEquals(BookingStatus.PENDING, booking.getStatus());
        assertEquals(0, new BigDecimal("2400").compareTo(booking.getTotalAmount()));   // 1200 x 2
        assertEquals("Soy celíaca", booking.getObservations());
        assertTrue(booking.getCode().matches("UM-[A-Z2-9]{6}"));
        assertTrue(booking.getExpiresAt().isAfter(LocalDateTime.now().plusMinutes(119)));
    }

    @Test
    void needsAtLeastOneParticipant() {
        ActivityInstance instance = newInstance(trekking, LocalDateTime.now().plusDays(5), null);
        assertThrows(InvalidDataException.class, () -> bookingService.createBooking(instance.getId(), TOURIST, 0, null));
    }

    @Test
    void checksMinimumAge() {
        touristRepo.save(Tourist.builder().email("nene@demo.uy").password("x").role(Role.TOURIST)
                .firstName("Nene").lastName("Chico").document("60000001")
                .birthDate(LocalDate.now().minusYears(8)).build());
        ActivityInstance instance = newInstance(trekking, LocalDateTime.now().plusDays(5), null);   // edad minima 10

        InvalidDataException e = assertThrows(InvalidDataException.class,
                () -> bookingService.createBooking(instance.getId(), "nene@demo.uy", 1, null));
        assertTrue(e.getMessage().contains("mayores de 10"));
    }

    @Test
    void respectsCapacity() throws Exception {
        ActivityInstance instance = newInstance(trekking, LocalDateTime.now().plusDays(5), 3);
        bookingService.createBooking(instance.getId(), TOURIST, 2, null);

        InvalidDataException e = assertThrows(InvalidDataException.class,
                () -> bookingService.createBooking(instance.getId(), TOURIST, 2, null));
        assertEquals("Solo quedan 1 lugares para esta fecha", e.getMessage());
    }

    @Test
    void cannotBookAfterTheBookingLimit() {
        // El trekking cierra las reservas 120 minutos antes: empieza en 1 hora, ya cerro
        ActivityInstance instance = newInstance(trekking, LocalDateTime.now().plusHours(1), null);
        assertThrows(InvalidDataException.class, () -> bookingService.createBooking(instance.getId(), TOURIST, 1, null));
    }

    // ---------- Pagar ----------

    @Test
    void approvedPaymentConfirmsBooking() throws Exception {
        when(paymentGateway.charge(anyString(), any(), anyString()))
                .thenReturn(new ChargeResponseDTO("approved", "txn_123", "Pago aprobado"));
        Booking booking = newBooking();

        Booking paid = bookingService.payBooking(booking.getId(), TOURIST, "4242 4242 4242 4242");

        assertEquals(BookingStatus.CONFIRMED, paid.getStatus());
        Payment payment = paid.getPayments().get(0);
        assertEquals(PaymentStatus.APPROVED, payment.getStatus());
        assertEquals("4242", payment.getCardLastDigits());
        assertEquals("Visa", payment.getCardBrand());
        assertEquals("txn_123", payment.getExternalTransactionId());
    }

    @Test
    void rejectedPaymentKeepsBookingPending() throws Exception {
        when(paymentGateway.charge(anyString(), any(), anyString()))
                .thenReturn(new ChargeResponseDTO("rejected", null, "Fondos insuficientes"));
        Booking booking = newBooking();

        assertThrows(InvalidDataException.class,
                () -> bookingService.payBooking(booking.getId(), TOURIST, "4000000000000002"));

        Booking after = bookingRepo.findById(booking.getId()).orElseThrow();
        assertEquals(BookingStatus.PENDING, after.getStatus());
        assertEquals(PaymentStatus.REJECTED, after.getPayments().get(0).getStatus());
    }

    @Test
    void unavailableServiceDoesNotConfirm() throws Exception {
        when(paymentGateway.charge(anyString(), any(), anyString()))
                .thenThrow(new PaymentUnavailableException("El servicio de pagos no está disponible"));
        Booking booking = newBooking();

        assertThrows(PaymentUnavailableException.class,
                () -> bookingService.payBooking(booking.getId(), TOURIST, "4000000000000119"));

        Booking after = bookingRepo.findById(booking.getId()).orElseThrow();
        assertEquals(BookingStatus.PENDING, after.getStatus());
        assertEquals(PaymentStatus.ERROR, after.getPayments().get(0).getStatus());
    }

    @Test
    void invalidCardDoesNotCallTheService() throws Exception {
        Booking booking = newBooking();

        assertThrows(InvalidDataException.class, () -> bookingService.payBooking(booking.getId(), TOURIST, "1234"));
        verify(paymentGateway, never()).charge(anyString(), any(), anyString());
    }

    @Test
    void expiredBookingCannotBePaid() throws Exception {
        Booking booking = newBooking();
        booking.setExpiresAt(LocalDateTime.now().minusMinutes(1));

        assertThrows(InvalidDataException.class,
                () -> bookingService.payBooking(booking.getId(), TOURIST, "4242424242424242"));
        assertEquals(CancelledBy.SYSTEM, bookingRepo.findById(booking.getId()).orElseThrow().getCancelledBy());
    }

    @Test
    void otherTouristCannotSeeTheBooking() throws Exception {
        Booking booking = newBooking();
        assertThrows(EntityNotFoundException.class,
                () -> bookingService.getBookingOfTourist(booking.getId(), "lucia@demo.uy"));
    }

    // ---------- Cancelar ----------

    @Test
    void touristCancelsWithEnoughAnticipation() throws Exception {
        Booking booking = newBooking();
        booking.setStatus(BookingStatus.CONFIRMED);

        Booking cancelled = bookingService.cancelByTourist(booking.getId(), TOURIST);

        assertEquals(BookingStatus.CANCELLED, cancelled.getStatus());
        assertEquals(CancelledBy.TOURIST, cancelled.getCancelledBy());
    }

    @Test
    void touristCannotCancelWhenActivityDoesNotAllowIt() throws Exception {
        Activity rappel = activityRepo.findAll().stream()
                .filter(a -> a.getName().startsWith("Rappel")).findFirst().orElseThrow();
        ActivityInstance instance = newInstance(rappel, LocalDateTime.now().plusDays(5), null);
        Booking booking = bookingService.createBooking(instance.getId(), TOURIST, 1, null);
        booking.setStatus(BookingStatus.CONFIRMED);

        assertThrows(InvalidDataException.class, () -> bookingService.cancelByTourist(booking.getId(), TOURIST));
    }

    @Test
    void expiredPendingBookingsAreCancelledAutomatically() throws Exception {
        Booking booking = newBooking();
        booking.setExpiresAt(LocalDateTime.now().minusMinutes(5));
        bookingRepo.save(booking);

        bookingService.cancelExpiredBookings();

        Booking after = bookingRepo.findById(booking.getId()).orElseThrow();
        assertEquals(BookingStatus.CANCELLED, after.getStatus());
        assertEquals(CancelledBy.SYSTEM, after.getCancelledBy());
    }

    @Test
    void providerCancelsDateAndItsBookings() throws Exception {
        Booking booking = newBooking();

        int cancelled = bookingService.cancelInstance(booking.getInstance().getId(), "sierras@demo.uy");

        assertEquals(1, cancelled);
        assertEquals(InstanceStatus.CANCELLED, booking.getInstance().getStatus());
        assertEquals(CancelledBy.PROVIDER, bookingRepo.findById(booking.getId()).orElseThrow().getCancelledBy());
    }

    @Test
    void otherProviderCannotCancelTheDate() throws Exception {
        Booking booking = newBooking();
        assertThrows(EntityNotFoundException.class,
                () -> bookingService.cancelInstance(booking.getInstance().getId(), "kayak@demo.uy"));
    }

    // ---------- Asistencia ----------

    @Test
    void checkInOnTheDayCompletesTheBooking() throws Exception {
        Booking booking = newBooking();
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.getInstance().setStartDateTime(LocalDateTime.now());

        Booking done = bookingService.checkIn(" " + booking.getCode().toLowerCase() + " ", "sierras@demo.uy");

        assertEquals(BookingStatus.COMPLETED, done.getStatus());
        assertNotNull(done.getCheckinDate());
        assertThrows(InvalidDataException.class, () -> bookingService.checkIn(booking.getCode(), "sierras@demo.uy"));
    }

    @Test
    void checkInOnlyOnTheDayOfTheActivity() throws Exception {
        Booking booking = newBooking();   // es dentro de 5 dias
        booking.setStatus(BookingStatus.CONFIRMED);

        assertThrows(InvalidDataException.class, () -> bookingService.checkIn(booking.getCode(), "sierras@demo.uy"));
    }

    @Test
    void checkInOfUnpaidBookingFails() throws Exception {
        Booking booking = newBooking();
        booking.getInstance().setStartDateTime(LocalDateTime.now());

        assertThrows(InvalidDataException.class, () -> bookingService.checkIn(booking.getCode(), "sierras@demo.uy"));
    }

    @Test
    void checkInFromOtherProviderFails() throws Exception {
        Booking booking = newBooking();
        assertThrows(EntityNotFoundException.class, () -> bookingService.checkIn(booking.getCode(), "kayak@demo.uy"));
    }
}
