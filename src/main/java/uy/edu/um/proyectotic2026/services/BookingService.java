package uy.edu.um.proyectotic2026.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uy.edu.um.proyectotic2026.dto.ChargeResponseDTO;
import uy.edu.um.proyectotic2026.dto.MercadoPagoPaymentDTO;
import uy.edu.um.proyectotic2026.dto.ParticipantDTO;
import uy.edu.um.proyectotic2026.entities.*;
import uy.edu.um.proyectotic2026.exceptions.EntityNotFoundException;
import uy.edu.um.proyectotic2026.exceptions.InvalidDataException;
import uy.edu.um.proyectotic2026.exceptions.PaymentUnavailableException;
import uy.edu.um.proyectotic2026.repository.ActivityInstanceRepository;
import uy.edu.um.proyectotic2026.repository.BookingRepository;
import uy.edu.um.proyectotic2026.repository.CancellationReasonRepository;
import uy.edu.um.proyectotic2026.repository.PaymentRepository;
import uy.edu.um.proyectotic2026.repository.TouristRepository;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

// Reservas y pagos (Acta 1, puntos 4.4 a 4.8 y 4.10)
@Service
public class BookingService {

    // La reserva pendiente se guarda 2 horas esperando el pago (Acta 1, punto 4.8)
    public static final int PAYMENT_DEADLINE_HOURS = 2;

    // Lo que entra en la columna total_amount: sin tope de personas el total podria pasarse
    private static final BigDecimal MAX_TOTAL_AMOUNT = new BigDecimal("99999999.99");

    private static final String CODE_CHARACTERS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private final SecureRandom random = new SecureRandom();

    @Autowired
    private MercadoPagoClient mercadoPago;

    @Autowired
    private BookingRepository bookingRepo;

    @Autowired
    private PaymentRepository paymentRepo;

    @Autowired
    private ActivityInstanceRepository instanceRepo;

    @Autowired
    private TouristRepository touristRepo;

    @Autowired
    private CancellationReasonRepository reasonRepo;

    @Autowired
    private PaymentGatewayClient paymentGateway;

    @Autowired
    private RefundService refundService;

    // ---------- Reservar ----------

    public ActivityInstance getInstanceToBook(Long instanceId) throws EntityNotFoundException, InvalidDataException {
        ActivityInstance instance = instanceRepo.findById(instanceId)
                .orElseThrow(() -> new EntityNotFoundException("Fecha no encontrada"));
        checkCanBook(instance);
        return instance;
    }

    // Controles para reservar una fecha: publicada, de un emprendimiento aprobado, no cancelada y sin cerrar
    private void checkCanBook(ActivityInstance instance) throws EntityNotFoundException, InvalidDataException {
        Activity activity = instance.getActivity();
        if (!activity.isPublished() || activity.getBusiness().getStatus() != BusinessStatus.APPROVED)
            throw new EntityNotFoundException("Fecha no encontrada");

        if (instance.getStatus() != InstanceStatus.SCHEDULED)
            throw new InvalidDataException("Esta fecha fue cancelada por el prestador");

        // Limite para reservar: hora de inicio menos los minutos que fijo el prestador
        if (!LocalDateTime.now().isBefore(instance.getBookingDeadline()))
            throw new InvalidDataException("Las reservas para esta fecha ya cerraron");
    }

    @Transactional
    public Booking createBooking(Long instanceId, String touristEmail, List<ParticipantDTO> people, String observations)
            throws EntityNotFoundException, InvalidDataException {

        // Se bloquea la fecha mientras se controla el cupo: dos reservas a la vez no pueden pasarse
        ActivityInstance instance = instanceRepo.findLockedById(instanceId)
                .orElseThrow(() -> new EntityNotFoundException("Fecha no encontrada"));
        checkCanBook(instance);
        Activity activity = instance.getActivity();
        Tourist tourist = touristRepo.findByEmail(touristEmail)
                .orElseThrow(() -> new EntityNotFoundException("Turista no encontrado"));

        if (people == null || people.isEmpty())
            throw new InvalidDataException("La reserva tiene que ser para al menos 1 persona");
        int participants = people.size();
        Integer maxPerBooking = activity.getMaxParticipantsPerBooking();
        if (maxPerBooking != null && participants > maxPerBooking)
            throw new InvalidDataException("Se pueden reservar hasta " + maxPerBooking + " lugares por reserva");

        BigDecimal totalAmount = activity.getPrice().multiply(BigDecimal.valueOf(participants));
        if (totalAmount.compareTo(MAX_TOTAL_AMOUNT) > 0)
            throw new InvalidDataException("El total de la reserva es demasiado alto");

        // Datos de cada persona que va (nombre, apellido, documento y edad) y edad minima de la actividad
        List<BookingParticipant> checkedPeople = checkPeople(people, activity.getMinimumAge());

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
                .totalAmount(totalAmount)
                .createdAt(now)
                .expiresAt(expiresAt)
                // La politica de devolucion vale la del momento de reservar, aunque el prestador la cambie despues
                .refundPercentage(activity.isAllowsCancellation() ? activity.getTouristRefundPercentage() : null)
                .build();
        checkedPeople.forEach(booking::addPerson);

        return bookingRepo.save(booking);
    }

    // Controla los datos de cada persona. La edad minima se controla con la edad de cada una.
    private List<BookingParticipant> checkPeople(List<ParticipantDTO> people, Integer minimumAge)
            throws InvalidDataException {
        List<BookingParticipant> result = new java.util.ArrayList<>();
        Set<String> documents = new HashSet<>();
        int number = 0;
        for (ParticipantDTO person : people) {
            number++;
            String who = "Persona " + number;
            if (person == null || isBlank(person.getFirstName()) || isBlank(person.getLastName())
                    || isBlank(person.getDocument()) || person.getAge() == null)
                throw new InvalidDataException(who + ": completá nombre, apellido, documento y edad");

            String firstName = person.getFirstName().trim();
            String lastName = person.getLastName().trim();
            String document = person.getDocument().trim();
            if (firstName.length() > 100 || lastName.length() > 100)
                throw new InvalidDataException(who + ": el nombre o el apellido es demasiado largo");
            if (!document.matches("[A-Za-z0-9.\\- ]{3,20}"))
                throw new InvalidDataException(who + ": el documento no es válido");
            if (person.getAge() < 0 || person.getAge() > 120)
                throw new InvalidDataException(who + ": la edad no es válida");
            if (minimumAge != null && person.getAge() < minimumAge)
                throw new InvalidDataException(firstName + " tiene " + person.getAge()
                        + " años y esta actividad es para mayores de " + minimumAge + " años");

            // El mismo documento dos veces en la reserva es un error de tipeo (se compara sin puntos ni guiones)
            if (!documents.add(document.replaceAll("[.\\- ]", "").toUpperCase()))
                throw new InvalidDataException("Hay dos personas con el mismo documento (" + document + ")");

            result.add(BookingParticipant.builder()
                    .firstName(firstName)
                    .lastName(lastName)
                    .document(document)
                    .age(person.getAge())
                    .build());
        }
        return result;
    }

    // Para el formulario de reserva: la primera fila viene con los datos del turista
    public ParticipantDTO touristAsParticipant(String touristEmail) throws EntityNotFoundException {
        Tourist tourist = touristRepo.findByEmail(touristEmail)
                .orElseThrow(() -> new EntityNotFoundException("Turista no encontrado"));
        int age = Period.between(tourist.getBirthDate(), LocalDate.now()).getYears();
        return new ParticipantDTO(tourist.getFirstName(), tourist.getLastName(), tourist.getDocument(), age);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
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

        Booking booking = lockPayableBooking(bookingId, touristEmail);

        String card = cardNumber == null ? "" : cardNumber.replace(" ", "");
        if (!card.matches("\\d{13,19}"))
            throw new InvalidDataException("El número de tarjeta no es válido");

        Payment payment = Payment.builder()
                .booking(booking)
                .amount(booking.getTotalAmount())
                .cardBrand(getCardBrand(card))
                .cardLastDigits(card.substring(card.length() - 4))
                .provider(PaymentProvider.SIMULATOR)
                .date(LocalDateTime.now())
                .build();

        try {
            ChargeResponseDTO response = paymentGateway.charge(card, booking.getTotalAmount(),
                    paymentDescription(booking));
            return applyPaymentResult(booking, payment, response);
        } catch (PaymentUnavailableException e) {
            savePaymentError(booking, payment, e);
            throw e;
        }
    }

    // Igual que payBooking, pero con el token que genera el formulario de Mercado Pago.
    @Transactional(noRollbackFor = {InvalidDataException.class, PaymentUnavailableException.class})
    public Booking payBookingWithMercadoPago(Long bookingId, String touristEmail, MercadoPagoPaymentDTO data)
            throws EntityNotFoundException, InvalidDataException, PaymentUnavailableException {

        Booking booking = lockPayableBooking(bookingId, touristEmail);

        if (data.getToken() == null || data.getToken().isBlank())
            throw new InvalidDataException("Faltan los datos de la tarjeta");

        Payment payment = Payment.builder()
                .booking(booking)
                .amount(booking.getTotalAmount())
                .cardBrand(data.getPaymentMethodId())
                .provider(PaymentProvider.MERCADO_PAGO)
                .date(LocalDateTime.now())
                .build();

        try {
            ChargeResponseDTO response = mercadoPago.pay(data, booking.getTotalAmount(),
                    booking.getCode(), paymentDescription(booking));
            payment.setCardLastDigits(response.getCardLastDigits());
            payment.setExternalPaymentId(response.getPaymentId());
            return applyPaymentResult(booking, payment, response);
        } catch (PaymentUnavailableException e) {
            savePaymentError(booking, payment, e);
            throw e;
        }
    }

    // Se bloquea la reserva: un doble clic en "Pagar" no puede cobrar dos veces.
    // El segundo pedido espera, y cuando sigue la reserva ya esta confirmada.
    private Booking lockPayableBooking(Long bookingId, String touristEmail)
            throws EntityNotFoundException, InvalidDataException {
        Booking booking = bookingRepo.findLockedById(bookingId)
                .orElseThrow(() -> new EntityNotFoundException("Reserva no encontrada"));
        checkOwner(booking, touristEmail);

        if (booking.getStatus() != BookingStatus.PENDING)
            throw new InvalidDataException("Esta reserva no está pendiente de pago");

        if (!LocalDateTime.now().isBefore(booking.getExpiresAt())) {
            cancel(booking, CancelledBy.SYSTEM);
            throw new InvalidDataException("Venció el plazo para pagar y la reserva se canceló");
        }
        return booking;
    }

    private String paymentDescription(Booking booking) {
        return "Reserva " + booking.getCode() + " - " + booking.getActivity().getName();
    }

    private Booking applyPaymentResult(Booking booking, Payment payment, ChargeResponseDTO response)
            throws InvalidDataException {
        payment.setExternalTransactionId(response.getTransactionId());
        payment.setMessage(response.getMessage());

        if (!response.isApproved()) {
            // Rechazado: queda registrado y la reserva sigue pendiente para reintentar
            payment.setStatus(PaymentStatus.REJECTED);
            booking.getPayments().add(payment);
            paymentRepo.save(payment);
            throw new InvalidDataException("El pago fue rechazado: " + response.getMessage()
                    + ". Podés intentar con otra tarjeta.");
        }

        payment.setStatus(PaymentStatus.APPROVED);
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.getPayments().add(payment);
        paymentRepo.save(payment);
        return bookingRepo.save(booking);
    }

    // El servicio no respondio: NO se confirma la reserva, se guarda el intento con error
    private void savePaymentError(Booking booking, Payment payment, PaymentUnavailableException e) {
        payment.setStatus(PaymentStatus.ERROR);
        payment.setMessage(e.getMessage());
        booking.getPayments().add(payment);
        paymentRepo.save(payment);
    }

    // ---------- Consultar ----------

    public List<Booking> getBookingsOfTourist(String touristEmail) {
        return bookingRepo.findByTouristEmailOrderByCreatedAtDesc(touristEmail);
    }

    // Una reserva del turista logueado. Si es de otro, es como si no existiera.
    public Booking getBookingOfTourist(Long bookingId, String touristEmail) throws EntityNotFoundException {
        Booking booking = bookingRepo.findById(bookingId)
                .orElseThrow(() -> new EntityNotFoundException("Reserva no encontrada"));
        checkOwner(booking, touristEmail);
        return booking;
    }

    private void checkOwner(Booking booking, String touristEmail) throws EntityNotFoundException {
        if (!booking.getTourist().getEmail().equals(touristEmail))
            throw new EntityNotFoundException("Reserva no encontrada");
    }

    public List<Booking> getBookingsOfProvider(String providerEmail) {
        return bookingRepo.findByInstanceActivityBusinessProviderEmailOrderByInstanceStartDateTimeAsc(providerEmail);
    }

    // ---------- Cancelar ----------

    // El turista cancela solo si la actividad lo permite y con la anticipacion minima (Acta 1, punto 4.5).
    // Si ya habia pagado, se le devuelve el porcentaje que fijo el prestador.
    // Se bloquea la reserva: un doble clic en "Cancelar" no puede devolver dos veces.
    @Transactional
    public Booking cancelByTourist(Long bookingId, String touristEmail)
            throws EntityNotFoundException, InvalidDataException {

        Booking booking = bookingRepo.findLockedById(bookingId)
                .orElseThrow(() -> new EntityNotFoundException("Reserva no encontrada"));
        checkOwner(booking, touristEmail);

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

    // El prestador cancela una fecha: se cancelan todas sus reservas activas (Acta 1, punto 4.6)
    // y a las que estaban pagadas se les devuelve el 100 %.
    @Transactional
    public int cancelInstance(Long instanceId, String providerEmail, Long reasonId)
            throws EntityNotFoundException, InvalidDataException {

        ActivityInstance instance = instanceRepo.findLockedById(instanceId)
                .orElseThrow(() -> new EntityNotFoundException("Fecha no encontrada"));
        if (!instance.getActivity().getBusiness().getProvider().getEmail().equals(providerEmail))
            throw new EntityNotFoundException("Fecha no encontrada");
        if (instance.getStatus() == InstanceStatus.CANCELLED)
            throw new InvalidDataException("Esta fecha ya estaba cancelada");
        if (!instance.getStartDateTime().isAfter(LocalDateTime.now()))
            throw new InvalidDataException("No se puede cancelar una fecha que ya pasó");

        if (reasonId == null)
            throw new InvalidDataException("Elegí el motivo de la cancelación");
        CancellationReason reason = reasonRepo.findById(reasonId)
                .orElseThrow(() -> new EntityNotFoundException("Motivo de cancelación no encontrado"));

        instance.setStatus(InstanceStatus.CANCELLED);
        instance.setCancellationDate(LocalDateTime.now());
        instance.setCancellationReason(reason);
        instanceRepo.save(instance);

        int cancelled = 0;
        for (Booking booking : bookingRepo.findLockedByInstanceId(instanceId)) {
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

    // Si la reserva estaba pagada, se devuelve la plata (las que vencen sin pagar no tienen nada que devolver)
    private Booking cancel(Booking booking, CancelledBy who) {
        booking.setStatus(BookingStatus.CANCELLED);
        booking.setCancelledBy(who);
        booking.setCancellationDate(LocalDateTime.now());
        Booking saved = bookingRepo.save(booking);
        if (who != CancelledBy.SYSTEM)
            refundService.refundCancelledBooking(saved, who);
        return saved;
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
