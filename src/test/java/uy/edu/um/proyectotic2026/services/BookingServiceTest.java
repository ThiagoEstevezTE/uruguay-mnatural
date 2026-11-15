package uy.edu.um.proyectotic2026.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;
import uy.edu.um.proyectotic2026.dto.ChargeResponseDTO;
import uy.edu.um.proyectotic2026.dto.MercadoPagoPaymentDTO;
import uy.edu.um.proyectotic2026.dto.ParticipantDTO;
import uy.edu.um.proyectotic2026.entities.*;
import uy.edu.um.proyectotic2026.exceptions.EntityNotFoundException;
import uy.edu.um.proyectotic2026.exceptions.InvalidDataException;
import uy.edu.um.proyectotic2026.exceptions.PaymentUnavailableException;
import uy.edu.um.proyectotic2026.repository.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static uy.edu.um.proyectotic2026.services.TestPeople.people;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
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

    @Autowired
    private CancellationReasonRepository reasonRepo;

    @MockitoBean
    private PaymentGatewayClient paymentGateway;

    @MockitoBean
    private MercadoPagoClient mercadoPago;

    @Autowired
    private RefundService refundService;

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
        return bookingService.createBooking(instance.getId(), TOURIST, people(2), " Soy celíaca ");
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
        assertThrows(InvalidDataException.class, () -> bookingService.createBooking(instance.getId(), TOURIST, people(0), null));
    }

    @Test
    void checksMinimumAgeOfEachPerson() {
        ActivityInstance instance = newInstance(trekking, LocalDateTime.now().plusDays(5), null);   // edad minima 10
        List<ParticipantDTO> people = people(2);
        people.get(1).setAge(8);

        InvalidDataException e = assertThrows(InvalidDataException.class,
                () -> bookingService.createBooking(instance.getId(), TOURIST, people, null));
        assertTrue(e.getMessage().contains("Persona2 tiene 8 años"));
        assertTrue(e.getMessage().contains("mayores de 10"));
    }

    @Test
    void savesTheDataOfEachPerson() throws Exception {
        ActivityInstance instance = newInstance(trekking, LocalDateTime.now().plusDays(5), null);
        List<ParticipantDTO> people = List.of(
                new ParticipantDTO(" Sofía ", "Martínez", "4.123.456-7", 34),
                new ParticipantDTO("Tomás", "Martínez", "5.234.567-8", 11));

        Booking booking = bookingService.createBooking(instance.getId(), TOURIST, people, null);

        assertEquals(2, booking.getParticipants());
        assertEquals(2, booking.getPeople().size());
        assertEquals("Sofía Martínez", booking.getPeople().get(0).getFullName());
        assertEquals("4.123.456-7", booking.getPeople().get(0).getDocument());
        assertEquals(11, booking.getPeople().get(1).getAge());
    }

    @Test
    void everyPersonNeedsNameLastNameDocumentAndAge() {
        ActivityInstance instance = newInstance(trekking, LocalDateTime.now().plusDays(5), null);
        List<ParticipantDTO> people = people(2);
        people.get(1).setDocument("  ");

        InvalidDataException e = assertThrows(InvalidDataException.class,
                () -> bookingService.createBooking(instance.getId(), TOURIST, people, null));
        assertTrue(e.getMessage().startsWith("Persona 2"));
    }

    @Test
    void sameDocumentTwiceIsRejected() {
        ActivityInstance instance = newInstance(trekking, LocalDateTime.now().plusDays(5), null);
        List<ParticipantDTO> people = people(2);
        people.get(0).setDocument("4.123.456-7");
        people.get(1).setDocument("41234567");

        assertThrows(InvalidDataException.class,
                () -> bookingService.createBooking(instance.getId(), TOURIST, people, null));
    }

    @Test
    void invalidAgeIsRejected() {
        ActivityInstance instance = newInstance(trekking, LocalDateTime.now().plusDays(5), null);
        List<ParticipantDTO> people = people(1);
        people.get(0).setAge(-1);

        assertThrows(InvalidDataException.class,
                () -> bookingService.createBooking(instance.getId(), TOURIST, people, null));
    }

    @Test
    void firstRowOfTheFormHasTheTouristData() throws Exception {
        ParticipantDTO tourist = bookingService.touristAsParticipant(TOURIST);
        Tourist saved = touristRepo.findByEmail(TOURIST).orElseThrow();

        assertEquals(saved.getFirstName(), tourist.getFirstName());
        assertEquals(saved.getDocument(), tourist.getDocument());
        assertTrue(tourist.getAge() >= 18);
    }

    @Test
    void respectsMaxParticipantsSetByTheProvider() {
        trekking.setMaxParticipantsPerBooking(3);
        ActivityInstance instance = newInstance(trekking, LocalDateTime.now().plusDays(5), null);

        assertThrows(InvalidDataException.class,
                () -> bookingService.createBooking(instance.getId(), TOURIST, people(4), null));
    }

    @Test
    void withoutMaxParticipantsLargeGroupsCanBook() throws Exception {
        trekking.setMaxParticipantsPerBooking(null);
        ActivityInstance instance = newInstance(trekking, LocalDateTime.now().plusDays(5), null);

        assertEquals(30, bookingService.createBooking(instance.getId(), TOURIST, people(30), null).getParticipants());
    }

    @Test
    void totalTooHighForTheDatabaseIsRejected() {
        trekking.setMaxParticipantsPerBooking(null);
        trekking.setPrice(new BigDecimal("99999999"));
        ActivityInstance instance = newInstance(trekking, LocalDateTime.now().plusDays(5), null);

        assertThrows(InvalidDataException.class,
                () -> bookingService.createBooking(instance.getId(), TOURIST, people(2), null));
    }

    @Test
    void respectsCapacity() throws Exception {
        ActivityInstance instance = newInstance(trekking, LocalDateTime.now().plusDays(5), 3);
        bookingService.createBooking(instance.getId(), TOURIST, people(2), null);

        InvalidDataException e = assertThrows(InvalidDataException.class,
                () -> bookingService.createBooking(instance.getId(), TOURIST, people(2), null));
        assertEquals("Solo quedan 1 lugares para esta fecha", e.getMessage());
    }

    @Test
    void cannotBookAfterTheBookingLimit() {
        // El trekking cierra las reservas 120 minutos antes: empieza en 1 hora, ya cerro
        ActivityInstance instance = newInstance(trekking, LocalDateTime.now().plusHours(1), null);
        assertThrows(InvalidDataException.class, () -> bookingService.createBooking(instance.getId(), TOURIST, people(1), null));
    }

    @Test
    void cannotBookACancelledDate() {
        ActivityInstance instance = newInstance(trekking, LocalDateTime.now().plusDays(5), null);
        instance.setStatus(InstanceStatus.CANCELLED);

        assertThrows(InvalidDataException.class,
                () -> bookingService.createBooking(instance.getId(), TOURIST, people(1), null));
    }

    @Test
    void bookingExpiresWhenBookingsCloseIfThatIsBefore2Hours() throws Exception {
        // Empieza en 2 h 30 min y cierra las reservas 2 horas antes: hay 30 minutos para pagar, no 2 horas
        ActivityInstance instance = newInstance(trekking, LocalDateTime.now().plusMinutes(150), null);

        Booking booking = bookingService.createBooking(instance.getId(), TOURIST, people(1), null);

        assertEquals(instance.getBookingDeadline(), booking.getExpiresAt());
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

    @Test
    void otherTouristCannotPayTheBooking() throws Exception {
        Booking booking = newBooking();

        assertThrows(EntityNotFoundException.class,
                () -> bookingService.payBooking(booking.getId(), "lucia@demo.uy", "4242424242424242"));
        verify(paymentGateway, never()).charge(anyString(), any(), anyString());
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
        Booking booking = bookingService.createBooking(instance.getId(), TOURIST, people(1), null);
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

        CancellationReason reason = reasonRepo.findByName("Mal clima").orElseThrow();

        int cancelled = bookingService.cancelInstance(booking.getInstance().getId(), "sierras@demo.uy", reason.getId());

        assertEquals(1, cancelled);
        assertEquals(InstanceStatus.CANCELLED, booking.getInstance().getStatus());
        assertEquals(reason.getId(), booking.getInstance().getCancellationReason().getId());
        assertEquals(CancelledBy.PROVIDER, bookingRepo.findById(booking.getId()).orElseThrow().getCancelledBy());
    }

    @Test
    void otherProviderCannotCancelTheDate() throws Exception {
        Booking booking = newBooking();
        assertThrows(EntityNotFoundException.class,
                () -> bookingService.cancelInstance(booking.getInstance().getId(), "kayak@demo.uy", 1L));
    }

    @Test
    void cancellingADateNeedsAReason() throws Exception {
        Booking booking = newBooking();

        assertThrows(InvalidDataException.class,
                () -> bookingService.cancelInstance(booking.getInstance().getId(), "sierras@demo.uy", null));

        assertEquals(InstanceStatus.SCHEDULED, booking.getInstance().getStatus());
        assertEquals(BookingStatus.PENDING, booking.getStatus());
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

    // ---------- Reembolsos ----------

    // Reserva de 2 personas en el trekking (1200 c/u) pagada con el simulador. El trekking devuelve el 80 %.
    private Booking paidBooking() throws Exception {
        when(paymentGateway.charge(anyString(), any(), anyString()))
                .thenReturn(new ChargeResponseDTO("approved", "txn_pago", "Pago aprobado"));
        when(paymentGateway.refund(anyString(), any())).thenReturn("rfd_1");
        Booking booking = newBooking();
        return bookingService.payBooking(booking.getId(), TOURIST, "4242 4242 4242 4242");
    }

    @Test
    void touristCancelRefundsThePercentageSetByTheProvider() throws Exception {
        Booking booking = paidBooking();

        Booking cancelled = bookingService.cancelByTourist(booking.getId(), TOURIST);

        Refund refund = cancelled.getRefund();
        assertEquals(RefundStatus.REFUNDED, refund.getStatus());
        assertEquals(80, refund.getPercentage());
        assertEquals(0, new BigDecimal("1920.00").compareTo(refund.getAmount()));   // 80 % de 2400
        assertEquals("rfd_1", refund.getExternalRefundId());
        verify(paymentGateway).refund("txn_pago", new BigDecimal("1920.00"));
    }

    @Test
    void providerCancelRefundsEverything() throws Exception {
        Booking booking = paidBooking();
        CancellationReason reason = reasonRepo.findByName("Mal clima").orElseThrow();

        bookingService.cancelInstance(booking.getInstance().getId(), "sierras@demo.uy", reason.getId());

        Refund refund = bookingRepo.findById(booking.getId()).orElseThrow().getRefund();
        assertEquals(100, refund.getPercentage());
        assertEquals(0, new BigDecimal("2400.00").compareTo(refund.getAmount()));
        assertEquals(RefundStatus.REFUNDED, refund.getStatus());
    }

    @Test
    void unpaidBookingHasNothingToRefund() throws Exception {
        Booking booking = newBooking();

        Booking cancelled = bookingService.cancelByTourist(booking.getId(), TOURIST);

        assertNull(cancelled.getRefund());
        verify(paymentGateway, never()).refund(anyString(), any());
    }

    @Test
    void zeroPercentMeansNoRefund() throws Exception {
        trekking.setRefundPercentage(0);
        Booking booking = paidBooking();

        Booking cancelled = bookingService.cancelByTourist(booking.getId(), TOURIST);

        assertEquals(BookingStatus.CANCELLED, cancelled.getStatus());
        assertNull(cancelled.getRefund());
        verify(paymentGateway, never()).refund(anyString(), any());
    }

    @Test
    void cancellingTwiceDoesNotRefundTwice() throws Exception {
        Booking booking = paidBooking();
        bookingService.cancelByTourist(booking.getId(), TOURIST);

        assertThrows(InvalidDataException.class, () -> bookingService.cancelByTourist(booking.getId(), TOURIST));
        verify(paymentGateway, times(1)).refund(anyString(), any());
    }

    @Test
    void refundIsRetriedWhenTheServiceDoesNotAnswer() throws Exception {
        Booking booking = paidBooking();
        when(paymentGateway.refund(anyString(), any()))
                .thenThrow(new PaymentUnavailableException("El servicio de pagos no está disponible en este momento"));

        Booking cancelled = bookingService.cancelByTourist(booking.getId(), TOURIST);

        // La reserva se cancela igual y la devolucion queda en tramite
        assertEquals(BookingStatus.CANCELLED, cancelled.getStatus());
        assertEquals(RefundStatus.PENDING, cancelled.getRefund().getStatus());
        assertEquals(1, cancelled.getRefund().getAttempts());

        // Cuando el servicio vuelve, la tarea que corre cada minuto la termina
        doReturn("rfd_2").when(paymentGateway).refund(anyString(), any());
        refundService.retryPendingRefunds();

        assertEquals(RefundStatus.REFUNDED, cancelled.getRefund().getStatus());
        assertEquals("rfd_2", cancelled.getRefund().getExternalRefundId());
        assertEquals(2, cancelled.getRefund().getAttempts());
    }

    @Test
    void mercadoPagoPaymentIsRefundedInMercadoPago() throws Exception {
        ChargeResponseDTO approved = new ChargeResponseDTO("approved", "ORD01", "Pago aprobado");
        approved.setPaymentId("PAY01");
        when(mercadoPago.pay(any(), any(), anyString(), anyString())).thenReturn(approved);
        when(mercadoPago.refund(anyString(), anyString(), any(), anyBoolean(), anyString())).thenReturn("REF01");
        MercadoPagoPaymentDTO data = new MercadoPagoPaymentDTO();
        data.setToken("tok_1");
        Booking booking = bookingService.payBookingWithMercadoPago(newBooking().getId(), TOURIST, data);

        Booking cancelled = bookingService.cancelByTourist(booking.getId(), TOURIST);

        // 80 %: es una devolucion parcial, con el pago de adentro de la orden
        verify(mercadoPago).refund("ORD01", "PAY01", new BigDecimal("1920.00"), false,
                RefundService.idempotencyKey(cancelled.getRefund()));
        verify(paymentGateway, never()).refund(anyString(), any());
        assertEquals("REF01", cancelled.getRefund().getExternalRefundId());
    }

    @Test
    void providerCancelOfMercadoPagoPaymentIsATotalRefund() throws Exception {
        ChargeResponseDTO approved = new ChargeResponseDTO("approved", "ORD02", "Pago aprobado");
        approved.setPaymentId("PAY02");
        when(mercadoPago.pay(any(), any(), anyString(), anyString())).thenReturn(approved);
        when(mercadoPago.refund(anyString(), anyString(), any(), anyBoolean(), anyString())).thenReturn("REF02");
        MercadoPagoPaymentDTO data = new MercadoPagoPaymentDTO();
        data.setToken("tok_1");
        Booking booking = bookingService.payBookingWithMercadoPago(newBooking().getId(), TOURIST, data);
        CancellationReason reason = reasonRepo.findByName("Mal clima").orElseThrow();

        bookingService.cancelInstance(booking.getInstance().getId(), "sierras@demo.uy", reason.getId());

        Refund refund = bookingRepo.findById(booking.getId()).orElseThrow().getRefund();
        verify(mercadoPago).refund("ORD02", "PAY02", new BigDecimal("2400.00"), true, RefundService.idempotencyKey(refund));
    }

    @Test
    void refundKeyIsAlwaysTheSameForTheSameBookingAndLooksLikeAUuid() throws Exception {
        Booking booking = paidBooking();
        Booking cancelled = bookingService.cancelByTourist(booking.getId(), TOURIST);

        String key = RefundService.idempotencyKey(cancelled.getRefund());

        assertEquals(key, RefundService.idempotencyKey(cancelled.getRefund()));
        assertEquals(key, java.util.UUID.fromString(key).toString());
    }
}
