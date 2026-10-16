package uy.edu.um.proyectotic2026.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uy.edu.um.proyectotic2026.dto.ChargeResponseDTO;
import uy.edu.um.proyectotic2026.entities.*;
import uy.edu.um.proyectotic2026.exceptions.EntityNotFoundException;
import uy.edu.um.proyectotic2026.exceptions.InvalidDataException;
import uy.edu.um.proyectotic2026.exceptions.PaymentUnavailableException;
import uy.edu.um.proyectotic2026.repository.ActivityInstanceRepository;
import uy.edu.um.proyectotic2026.repository.BookingRepository;
import uy.edu.um.proyectotic2026.repository.PaymentRepository;
import uy.edu.um.proyectotic2026.repository.TouristRepository;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.List;

// Reservas y pagos (Acta 1, puntos 4.4 a 4.8 y 4.10)
@Service
public class BookingService {

    // La reserva pendiente se guarda 2 horas esperando el pago (Acta 1, punto 4.8)
    public static final int PAYMENT_DEADLINE_HOURS = 2;
    public static final int MAX_PARTICIPANTS = 20;

    private static final String CODE_CHARACTERS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private final SecureRandom random = new SecureRandom();

    @Autowired
    private BookingRepository bookingRepo;

    @Autowired
    private PaymentRepository paymentRepo;

    @Autowired
    private ActivityInstanceRepository instanceRepo;

    @Autowired
    private TouristRepository touristRepo;

    @Autowired
    private PaymentGatewayClient paymentGateway;

    // ---------- Reservar ----------

    public ActivityInstance getInstanceToBook(Long instanceId) throws EntityNotFoundException, InvalidDataException {
        ActivityInstance instance = instanceRepo.findById(instanceId)
                .orElseThrow(() -> new EntityNotFoundException("Fecha no encontrada"));

        Activity activity = instance.getActivity();
        if (!activity.isPublished() || activity.getBusiness().getStatus() != BusinessStatus.APPROVED)
            throw new EntityNotFoundException("Fecha no encontrada");

        if (instance.getStatus() != InstanceStatus.SCHEDULED)
            throw new InvalidDataException("Esta fecha fue cancelada por el prestador");

        // Limite para reservar: hora de inicio menos los minutos que fijo el prestador
        if (!LocalDateTime.now().isBefore(instance.getBookingDeadline()))
            throw new InvalidDataException("Las reservas para esta fecha ya cerraron");

        return instance;
    }

    @Transactional
    public Booking createBooking(Long instanceId, String touristEmail, Integer participants, String observations)
            throws EntityNotFoundException, InvalidDataException {

        ActivityInstance instance = getInstanceToBook(instanceId);
        Activity activity = instance.getActivity();
        Tourist tourist = touristRepo.findByEmail(touristEmail)
                .orElseThrow(() -> new EntityNotFoundException("Turista no encontrado"));

        if (participants == null || participants < 1)
            throw new InvalidDataException("La reserva tiene que ser para al menos 1 persona");
        if (participants > MAX_PARTICIPANTS)
            throw new InvalidDataException("Se pueden reservar hasta " + MAX_PARTICIPANTS + " lugares por reserva");

        // Edad minima: se controla con la fecha de nacimiento del turista (Acta 1, punto 4.1)
        if (activity.getMinimumAge() != null) {
            int age = Period.between(tourist.getBirthDate(), instance.getStartDateTime().toLocalDate()).getYears();
            if (age < activity.getMinimumAge())
                throw new InvalidDataException("Esta actividad es para mayores de " + activity.getMinimumAge() + " años");
        }

        // Cupo: solo si el prestador lo fijo para esta fecha (Acta 1, punto 4.3)
        if (instance.getMaxCapacity() != null) {
            int available = instance.getMaxCapacity() - getTakenPlaces(instance.getId());
            if (participants > available)
                throw new InvalidDataException(available <= 0
                        ? "No quedan lugares para esta fecha"
                        : "Solo quedan " + available + " lugares para esta fecha");
        }

        // Vence a las 2 horas, o antes si cierran las reservas de esa fecha:
        // asi no se puede pagar una actividad que ya cerro o que ya empezo
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiresAt = now.plusHours(PAYMENT_DEADLINE_HOURS);
        if (instance.getBookingDeadline().isBefore(expiresAt))
            expiresAt = instance.getBookingDeadline();

        Booking booking = Booking.builder()
                .instance(instance)
                .tourist(tourist)
                .code(generateCode())
                .participants(participants)
                .observations(observations == null || observations.isBlank() ? null : observations.trim())
                .totalAmount(activity.getPrice().multiply(BigDecimal.valueOf(participants)))
                .createdAt(now)
                .expiresAt(expiresAt)
                .build();

        return bookingRepo.save(booking);
    }

    // Lugares ocupados: reservas confirmadas o realizadas, y pendientes que todavia no vencieron
    public int getTakenPlaces(Long instanceId) {
        LocalDateTime now = LocalDateTime.now();
        return bookingRepo.findByInstanceId(instanceId).stream()
                .filter(b -> b.getStatus() == BookingStatus.CONFIRMED
                        || b.getStatus() == BookingStatus.COMPLETED
                        || (b.getStatus() == BookingStatus.PENDING && b.getExpiresAt().isAfter(now)))
                .mapToInt(Booking::getParticipants)
                .sum();
    }

    // ---------- Pagar ----------

    // El pago se hace con el servicio externo. La reserva se confirma SOLO si el pago fue aprobado (punto 4.7).
    // noRollbackFor: aunque el pago falle, el intento queda guardado en la base.
    @Transactional(noRollbackFor = {InvalidDataException.class, PaymentUnavailableException.class})
    public Booking payBooking(Long bookingId, String touristEmail, String cardNumber)
            throws EntityNotFoundException, InvalidDataException, PaymentUnavailableException {

        // Se bloquea la reserva: un doble clic en "Pagar" no puede cobrar dos veces.
        // El segundo pedido espera, y cuando sigue la reserva ya esta confirmada.
        Booking booking = bookingRepo.findLockedById(bookingId)
                .orElseThrow(() -> new EntityNotFoundException("Reserva no encontrada"));
        if (!booking.getTourist().getEmail().equals(touristEmail))
            throw new EntityNotFoundException("Reserva no encontrada");

        if (booking.getStatus() != BookingStatus.PENDING)
            throw new InvalidDataException("Esta reserva no está pendiente de pago");

        if (!LocalDateTime.now().isBefore(booking.getExpiresAt())) {
            cancel(booking, CancelledBy.SYSTEM);
            throw new InvalidDataException("Venció el plazo para pagar y la reserva se canceló");
        }

        String card = cardNumber == null ? "" : cardNumber.replace(" ", "");
        if (!card.matches("\\d{13,19}"))
            throw new InvalidDataException("El número de tarjeta no es válido");

        Payment payment = Payment.builder()
                .booking(booking)
                .amount(booking.getTotalAmount())
                .cardBrand(getCardBrand(card))
                .cardLastDigits(card.substring(card.length() - 4))
                .date(LocalDateTime.now())
                .build();

        try {
            ChargeResponseDTO response = paymentGateway.charge(card, booking.getTotalAmount(),
                    "Reserva " + booking.getCode() + " - " + booking.getActivity().getName());

            payment.setExternalTransactionId(response.getTransactionId());
            payment.setMessage(response.getMessage());

            if (response.isApproved()) {
                payment.setStatus(PaymentStatus.APPROVED);
                booking.setStatus(BookingStatus.CONFIRMED);
            } else {
                // Rechazado: queda registrado y la reserva sigue pendiente para reintentar
                payment.setStatus(PaymentStatus.REJECTED);
                booking.getPayments().add(payment);
                paymentRepo.save(payment);
                throw new InvalidDataException("El pago fue rechazado: " + response.getMessage()
                        + ". Podés intentar con otra tarjeta.");
            }
        } catch (PaymentUnavailableException e) {
            // El servicio no respondio: NO se confirma la reserva, se guarda el intento con error
            payment.setStatus(PaymentStatus.ERROR);
            payment.setMessage(e.getMessage());
            booking.getPayments().add(payment);
            paymentRepo.save(payment);
            throw e;
        }

        booking.getPayments().add(payment);
        paymentRepo.save(payment);
        return bookingRepo.save(booking);
    }

    // ---------- Consultar ----------

    public List<Booking> getBookingsOfTourist(String touristEmail) {
        return bookingRepo.findByTouristEmailOrderByCreatedAtDesc(touristEmail);
    }

    // Una reserva del turista logueado. Si es de otro, es como si no existiera.
    public Booking getBookingOfTourist(Long bookingId, String touristEmail) throws EntityNotFoundException {
        Booking booking = bookingRepo.findById(bookingId)
                .orElseThrow(() -> new EntityNotFoundException("Reserva no encontrada"));
        if (!booking.getTourist().getEmail().equals(touristEmail))
            throw new EntityNotFoundException("Reserva no encontrada");
        return booking;
    }

    public List<Booking> getBookingsOfProvider(String providerEmail) {
        return bookingRepo.findByInstanceActivityBusinessProviderEmailOrderByInstanceStartDateTimeAsc(providerEmail);
    }

    // ---------- Cancelar ----------

    // El turista cancela solo si la actividad lo permite y con la anticipacion minima (Acta 1, punto 4.5)
    @Transactional
    public Booking cancelByTourist(Long bookingId, String touristEmail)
            throws EntityNotFoundException, InvalidDataException {

        Booking booking = getBookingOfTourist(bookingId, touristEmail);

        if (booking.getStatus() != BookingStatus.PENDING && booking.getStatus() != BookingStatus.CONFIRMED)
            throw new InvalidDataException("Esta reserva no se puede cancelar");

        if (!canTouristCancel(booking))
            throw new InvalidDataException("Esta actividad no admite cancelación o ya pasó el plazo para cancelar");

        return cancel(booking, CancelledBy.TOURIST);
    }

    // Para mostrar u ocultar el boton "Cancelar" en "Mis reservas"
    public boolean canTouristCancel(Booking booking) {
        Activity activity = booking.getActivity();
        if (booking.getStatus() == BookingStatus.PENDING)
            return true;
        if (booking.getStatus() != BookingStatus.CONFIRMED || !activity.isAllowsCancellation())
            return false;
        LocalDateTime limit = booking.getInstance().getStartDateTime().minusHours(activity.getCancellationMinHours());
        return LocalDateTime.now().isBefore(limit);
    }

    // El prestador cancela una fecha: se cancelan todas sus reservas activas (Acta 1, punto 4.6).
    // La devolucion segun el motivo todavia no esta definida con el cliente.
    @Transactional
    public int cancelInstance(Long instanceId, String providerEmail)
            throws EntityNotFoundException, InvalidDataException {

        ActivityInstance instance = instanceRepo.findById(instanceId)
                .orElseThrow(() -> new EntityNotFoundException("Fecha no encontrada"));
        if (!instance.getActivity().getBusiness().getProvider().getEmail().equals(providerEmail))
            throw new EntityNotFoundException("Fecha no encontrada");
        if (instance.getStatus() == InstanceStatus.CANCELLED)
            throw new InvalidDataException("Esta fecha ya estaba cancelada");
        if (!instance.getStartDateTime().isAfter(LocalDateTime.now()))
            throw new InvalidDataException("No se puede cancelar una fecha que ya pasó");

        instance.setStatus(InstanceStatus.CANCELLED);
        instance.setCancellationDate(LocalDateTime.now());
        instanceRepo.save(instance);

        int cancelled = 0;
        for (Booking booking : bookingRepo.findByInstanceId(instanceId)) {
            if (booking.getStatus() == BookingStatus.PENDING || booking.getStatus() == BookingStatus.CONFIRMED) {
                cancel(booking, CancelledBy.PROVIDER);
                cancelled++;
            }
        }
        return cancelled;
    }

    // Cada minuto se cancelan las reservas pendientes que vencieron sin pagar,
    // y asi se liberan sus lugares (Acta 1, punto 4.8)
    @Scheduled(fixedRate = 60000)
    @Transactional
    public void cancelExpiredBookings() {
        for (Booking booking : bookingRepo.findByStatusAndExpiresAtBefore(BookingStatus.PENDING, LocalDateTime.now()))
            cancel(booking, CancelledBy.SYSTEM);
    }

    // ---------- Asistencia (check-in) ----------

    // El dia de la actividad el prestador escanea el QR (o escribe el numero de reserva)
    // y la reserva pasa a REALIZADA (Acta 1, punto 4.10)
    @Transactional
    public Booking checkIn(String code, String providerEmail) throws EntityNotFoundException, InvalidDataException {

        String cleanCode = code == null ? "" : code.trim().toUpperCase();
        Booking booking = bookingRepo.findByCode(cleanCode)
                .orElseThrow(() -> new EntityNotFoundException("No existe una reserva con el número " + cleanCode));

        if (!booking.getActivity().getBusiness().getProvider().getEmail().equals(providerEmail))
            throw new EntityNotFoundException("No existe una reserva con el número " + cleanCode);

        if (booking.getStatus() == BookingStatus.COMPLETED)
            throw new InvalidDataException("Esta reserva ya tenía la asistencia registrada");
        if (booking.getStatus() != BookingStatus.CONFIRMED)
            throw new InvalidDataException("La reserva no está confirmada (no fue pagada o se canceló)");
        if (!booking.getInstance().getStartDateTime().toLocalDate().equals(LocalDate.now()))
            throw new InvalidDataException("La asistencia se registra el día de la actividad");

        booking.setStatus(BookingStatus.COMPLETED);
        booking.setCheckinDate(LocalDateTime.now());
        return bookingRepo.save(booking);
    }

    // ---------- Metodos de ayuda ----------

    private Booking cancel(Booking booking, CancelledBy who) {
        booking.setStatus(BookingStatus.CANCELLED);
        booking.setCancelledBy(who);
        booking.setCancellationDate(LocalDateTime.now());
        return bookingRepo.save(booking);
    }

    // Numero de reserva tipo "UM-7K3F9Q" (sin letras que se confunden, como O y 0)
    private String generateCode() {
        String code;
        do {
            StringBuilder sb = new StringBuilder("UM-");
            for (int i = 0; i < 6; i++)
                sb.append(CODE_CHARACTERS.charAt(random.nextInt(CODE_CHARACTERS.length())));
            code = sb.toString();
        } while (bookingRepo.existsByCode(code));
        return code;
    }

    private String getCardBrand(String card) {
        if (card.startsWith("4"))
            return "Visa";
        if (card.startsWith("5"))
            return "Mastercard";
        if (card.startsWith("3"))
            return "American Express";
        return "Tarjeta";
    }
}
