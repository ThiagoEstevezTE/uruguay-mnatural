package uy.edu.um.proyectotic2026.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import uy.edu.um.proyectotic2026.entities.*;
import uy.edu.um.proyectotic2026.repository.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

// Carga datos de ejemplo al arrancar la app, para poder mostrar el sistema funcionando.
// Las cuentas de prueba estan en el README.
@Component
public class DataLoader implements CommandLineRunner {

    // Contraseña de todas las cuentas de ejemplo. Se puede cambiar en el .env (DEMO_PASSWORD).
    @Value("${app.demo-password}")
    private String demoPassword;

    @Autowired
    private UserRepository userRepo;

    @Autowired
    private AdministratorRepository adminRepo;

    @Autowired
    private TouristRepository touristRepo;

    @Autowired
    private ProviderRepository providerRepo;

    @Autowired
    private LocalityRepository localityRepo;

    @Autowired
    private ActivityTypeRepository typeRepo;

    @Autowired
    private EquipmentRepository equipmentRepo;

    @Autowired
    private ActivityRepository activityRepo;

    @Autowired
    private ActivityInstanceRepository instanceRepo;

    @Autowired
    private BookingRepository bookingRepo;

    @Autowired
    private RatingRepository ratingRepo;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String password;
    private Administrator admin;

    // Todo en una sola transaccion: o se cargan todos los datos o ninguno
    @Override
    @Transactional
    public void run(String... args) {

        // DEMO_PASSWORD= vacia en el .env dejaria las cuentas sin contraseña: mejor que la app no arranque
        if (demoPassword == null || demoPassword.isBlank())
            throw new IllegalStateException("DEMO_PASSWORD no puede estar vacía: borrala del .env o poné una contraseña");

        // Si ya hay usuarios, la base ya tiene datos: no se carga nada de nuevo
        if (userRepo.count() > 0)
            return;

        password = passwordEncoder.encode(demoPassword);

        loadCatalogs();
        loadUsers();
        loadActivities();
        loadBookingsAndRatings();
    }

    // Tipos de actividad y equipamiento (los ejemplos de la letra)
    private void loadCatalogs() {
        String[] types = {"Senderismo", "Trekking", "Cabalgata", "Aventura", "Gastronomía", "Enoturismo",
                "Actividad acuática", "Turismo de naturaleza", "Recorrido histórico"};
        for (String name : types)
            typeRepo.save(ActivityType.builder().name(name).build());

        String[] equipment = {"Casco", "Chaleco salvavidas", "Bicicleta", "Equipo de buceo", "Calzado deportivo",
                "Agua", "Protector solar", "Ropa de abrigo", "Kayak y remo", "Arnés"};
        for (String name : equipment)
            equipmentRepo.save(Equipment.builder().name(name).build());
    }

    private void loadUsers() {
        admin = adminRepo.save(Administrator.builder()
                .email("admin@uymnatural.uy")
                .password(password)
                .role(Role.ADMIN)
                .name("Administración Uruguay Mnatural")
                .build());

        touristRepo.save(Tourist.builder()
                .email("turista@demo.uy")
                .password(password)
                .role(Role.TOURIST)
                .firstName("Sofía")
                .lastName("Martínez")
                .document("45678901")
                .birthDate(LocalDate.of(1998, 5, 14))
                .build());

        // Prestadores aprobados (ya publican) y uno pendiente (para mostrar la bandeja del admin)
        // Otros turistas, para que haya valoraciones de varias personas
        createTourist("lucia@demo.uy", "Lucía", "Pereira", "51234567", LocalDate.of(1995, 9, 2));
        createTourist("diego@demo.uy", "Diego", "Rodríguez", "42345678", LocalDate.of(1988, 1, 23));
        createTourist("ana@demo.uy", "Ana", "Costa", "53456789", LocalDate.of(2001, 11, 30));

        createProvider("prestador@demo.uy", "Martín Silva", "31234567", ProviderType.INDIVIDUAL,
                "Cabalgatas del Polonio", "Cabalgatas por la costa rochense, entre dunas y lobos marinos.",
                "Ruta 10 km 264", "Cabo Polonio", true);

        createProvider("sierras@demo.uy", "Sierras de Minas Aventura SAS", "217654320011", ProviderType.COMPANY,
                "Sierras de Minas Aventura", "Trekking, rappel y escalada en las sierras de Lavalleja.",
                "Ruta 8 km 145", "Villa Serrana", true);

        createProvider("colonia@demo.uy", "Laura Fernández", "28765432", ProviderType.INDIVIDUAL,
                "Sabores y Calles de Colonia", "Recorridos guiados por el Barrio Histórico y degustaciones de productos locales.",
                "Calle de los Suspiros 45", "Colonia del Sacramento", true);

        createProvider("kayak@demo.uy", "Punta Kayak SRL", "219876540015", ProviderType.COMPANY,
                "Punta Kayak", "Salidas en kayak por la bahía de Punta del Este y la Isla Gorriti.",
                "Rambla Artigas y Calle 10", "Punta del Este", true);

        createProvider("pendiente@demo.uy", "Bodega Los Cerros SRL", "212345670018", ProviderType.COMPANY,
                "Bodega Los Cerros", "Visitas guiadas y degustación de vinos tannat en Carmelo.",
                "Camino de los Peregrinos 1450", "Carmelo", false);
    }

    private void loadActivities() {
        Business polonio = getBusiness("prestador@demo.uy");
        Business sierras = getBusiness("sierras@demo.uy");
        Business colonia = getBusiness("colonia@demo.uy");
        Business kayak = getBusiness("kayak@demo.uy");

        Activity a = createActivity(polonio, "Cabalgata al atardecer por las dunas", "Cabo Polonio",
                "Recorremos a caballo las dunas móviles de Cabo Polonio hasta la costa, para ver la puesta de sol "
                        + "frente al faro. Caballos mansos, aptos para principiantes, y guía baqueano durante todo el paseo.",
                150, 2500, 12, 60, true, 24, "Cabalgata", "Turismo de naturaleza");
        a.addEquipment(equipment("Casco"), true);
        a.addEquipment(equipment("Calzado deportivo"), false);
        a.addEquipment(equipment("Protector solar"), false);
        addInstances(a, LocalTime.of(18, 0), 1, 3, 5, 8, 10, 12);

        a = createActivity(polonio, "Avistaje de lobos marinos", "Cabo Polonio",
                "Caminata guiada hasta la lobería de la Isla de Torres, con explicación sobre la fauna marina del "
                        + "área protegida y el faro.",
                90, 900, null, 0, true, 2, "Turismo de naturaleza", "Senderismo");
        a.addEquipment(equipment("Agua"), false);
        addInstances(a, LocalTime.of(10, 30), 0, 1, 2, 4, 6, 9);

        a = createActivity(sierras, "Trekking al Salto del Penitente", "Villa Serrana",
                "Trekking de dificultad media por senderos de sierra hasta la cascada del Salto del Penitente. "
                        + "Incluye merienda de campo al regreso.",
                240, 1200, 10, 120, true, 48, "Trekking", "Senderismo", "Turismo de naturaleza");
        a.addEquipment(equipment("Calzado deportivo"), false);
        a.addEquipment(equipment("Agua"), false);
        a.addEquipment(equipment("Ropa de abrigo"), false);
        addInstances(a, LocalTime.of(9, 0), 2, 5, 9, 12, 16);

        a = createActivity(sierras, "Rappel en Villa Serrana", "Villa Serrana",
                "Bautismo de rappel en paredes de 25 metros con instructores certificados. Primero hay una "
                        + "práctica en piso y después el descenso.",
                180, 1800, 14, 180, false, null, "Aventura");
        a.addEquipment(equipment("Casco"), true);
        a.addEquipment(equipment("Arnés"), true);
        a.addEquipment(equipment("Calzado deportivo"), false);
        addInstances(a, LocalTime.of(14, 0), 3, 6, 10, 13);

        a = createActivity(colonia, "Recorrido por el Barrio Histórico", "Colonia del Sacramento",
                "Caminata guiada por el Barrio Histórico, Patrimonio de la Humanidad: la Calle de los Suspiros, "
                        + "el Portón de Campo, el faro y la historia de la ciudad portuguesa y española.",
                120, 800, null, 0, true, 12, "Recorrido histórico");
        addInstances(a, LocalTime.of(11, 0), 0, 1, 2, 3, 4, 5, 6, 7);

        a = createActivity(colonia, "Cata de vinos y quesos de Colonia", "Colonia del Sacramento",
                "Degustación de cinco vinos de bodegas de Colonia, con quesos artesanales de la zona y la "
                        + "explicación de un sommelier.",
                90, 2200, 18, 120, true, 24, "Gastronomía", "Enoturismo");
        addInstances(a, LocalTime.of(19, 30), 1, 4, 8, 11);

        a = createActivity(kayak, "Kayak a la Isla Gorriti", "Punta del Este",
                "Salida en kayak doble desde el Puerto de Punta del Este hasta la Isla Gorriti, con parada para "
                        + "caminar por la isla y ver los restos de las baterías históricas.",
                180, 1600, 12, 60, true, 24, "Actividad acuática", "Aventura");
        a.addEquipment(equipment("Kayak y remo"), true);
        a.addEquipment(equipment("Chaleco salvavidas"), true);
        a.addEquipment(equipment("Protector solar"), false);
        addInstances(a, LocalTime.of(9, 30), 1, 2, 5, 7, 9);
    }

    // Reservas en todos los estados y valoraciones, para mostrar el sistema funcionando
    private void loadBookingsAndRatings() {
        Activity cabalgata = activityRepo.findAll().stream()
                .filter(a -> a.getName().startsWith("Cabalgata")).findFirst().orElseThrow();
        Activity barrio = activityRepo.findAll().stream()
                .filter(a -> a.getName().startsWith("Recorrido")).findFirst().orElseThrow();
        Activity kayak = activityRepo.findAll().stream()
                .filter(a -> a.getName().startsWith("Kayak")).findFirst().orElseThrow();

        // Fechas que ya pasaron, con reservas realizadas y valoradas
        ActivityInstance pastCabalgata = addPastInstance(cabalgata, 9, LocalTime.of(18, 0));
        rate(booking(pastCabalgata, "lucia@demo.uy", 2, BookingStatus.COMPLETED, "UM-LUC1A2"), 5,
                "Una experiencia increíble. El atardecer desde las dunas es otra cosa y Martín es un genio.");
        rate(booking(pastCabalgata, "diego@demo.uy", 4, BookingStatus.COMPLETED, "UM-D1EG04"), 4,
                "Muy lindo paseo, ideal para ir en familia. Llevá protector solar.");

        ActivityInstance pastBarrio = addPastInstance(barrio, 6, LocalTime.of(11, 0));
        rate(booking(pastBarrio, "ana@demo.uy", 1, BookingStatus.COMPLETED, "UM-ANA7C5"), 5,
                "Laura sabe muchísimo de historia. Súper recomendable.");
        rate(booking(pastBarrio, "turista@demo.uy", 2, BookingStatus.COMPLETED, "UM-SOF1A9"), 4,
                "Muy buen recorrido, aprendimos un montón sobre la ciudad.");

        // Una fecha pasada que la turista de prueba todavia puede valorar
        ActivityInstance pastKayak = addPastInstance(kayak, 3, LocalTime.of(9, 30));
        booking(pastKayak, "turista@demo.uy", 2, BookingStatus.CONFIRMED, "UM-KAY4K2");

        // Hoy: reserva confirmada para mostrar el registro de asistencia (check-in) del prestador
        ActivityInstance today = addInstance(cabalgata,
                LocalDateTime.now().plusHours(2).withMinute(0).withSecond(0).withNano(0));
        booking(today, "turista@demo.uy", 2, BookingStatus.CONFIRMED, "UM-H0Y2Q8");

        // Proxima: reserva confirmada que todavia se puede cancelar
        ActivityInstance next = cabalgata.getUpcomingInstances().stream()
                .filter(i -> i.getStartDateTime().isAfter(LocalDateTime.now().plusDays(3)))
                .findFirst().orElseThrow();
        booking(next, "turista@demo.uy", 3, BookingStatus.CONFIRMED, "UM-PROX7A");
    }

    // ---------- Metodos de ayuda ----------

    private void createProvider(String email, String name, String document, ProviderType type,
                                String businessName, String description, String address,
                                String localityName, boolean approved) {

        Provider provider = Provider.builder()
                .email(email)
                .password(password)
                .role(Role.PROVIDER)
                .name(name)
                .document(document)
                .phone("099123456")
                .type(type)
                .build();

        Business business = Business.builder()
                .name(businessName)
                .description(description)
                .address(address)
                .phone("099123456")
                .email(email)
                .locality(locality(localityName))
                .requestDate(LocalDateTime.now().minusDays(approved ? 20 : 2))
                .build();

        if (approved) {
            business.setStatus(BusinessStatus.APPROVED);
            business.setResolutionDate(LocalDateTime.now().minusDays(18));
            business.setResolvedBy(admin);
        }

        provider.addBusiness(business);
        providerRepo.save(provider);
    }

    private Activity createActivity(Business business, String name, String localityName, String description,
                                    int durationMinutes, int price, Integer minimumAge, int bookingLimitMinutes,
                                    boolean allowsCancellation, Integer cancellationMinHours, String... typeNames) {

        Activity activity = Activity.builder()
                .business(business)
                .locality(locality(localityName))
                .name(name)
                .description(description)
                .durationMinutes(durationMinutes)
                .price(BigDecimal.valueOf(price))
                .minimumAge(minimumAge)
                .bookingLimitMinutes(bookingLimitMinutes)
                .allowsCancellation(allowsCancellation)
                .cancellationMinHours(cancellationMinHours)
                .build();

        for (String typeName : typeNames)
            activity.getTypes().add(typeRepo.findByName(typeName).orElseThrow());

        return activityRepo.save(activity);
    }

    // Agrega fechas a la actividad: "dentro de N dias, a tal hora"
    private void addInstances(Activity activity, LocalTime time, int... daysFromToday) {
        for (int days : daysFromToday)
            addInstance(activity, LocalDate.now().plusDays(days).atTime(time));
    }

    private ActivityInstance addInstance(Activity activity, LocalDateTime start) {
        ActivityInstance instance = ActivityInstance.builder().startDateTime(start).build();
        activity.addInstance(instance);
        return instanceRepo.save(instance);
    }

    private void createTourist(String email, String firstName, String lastName, String document, LocalDate birthDate) {
        touristRepo.save(Tourist.builder()
                .email(email)
                .password(password)
                .role(Role.TOURIST)
                .firstName(firstName)
                .lastName(lastName)
                .document(document)
                .birthDate(birthDate)
                .build());
    }

    private ActivityInstance addPastInstance(Activity activity, int daysAgo, LocalTime time) {
        return addInstance(activity, LocalDate.now().minusDays(daysAgo).atTime(time));
    }

    // Crea una reserva ya pagada (con su pago aprobado) en el estado indicado
    private Booking booking(ActivityInstance instance, String touristEmail, int participants,
                            BookingStatus status, String code) {

        LocalDateTime created = instance.getStartDateTime().minusDays(5);
        BigDecimal total = instance.getActivity().getPrice().multiply(BigDecimal.valueOf(participants));

        Booking booking = Booking.builder()
                .instance(instance)
                .tourist(touristRepo.findByEmail(touristEmail).orElseThrow())
                .code(code)
                .participants(participants)
                .totalAmount(total)
                .status(status)
                .createdAt(created)
                .expiresAt(created.plusHours(2))
                .checkinDate(status == BookingStatus.COMPLETED ? instance.getStartDateTime() : null)
                .build();

        booking.getPayments().add(Payment.builder()
                .booking(booking)
                .amount(total)
                .status(PaymentStatus.APPROVED)
                .externalTransactionId("txn_demo_" + code.substring(3).toLowerCase())
                .cardBrand("Visa")
                .cardLastDigits("4242")
                .message("Pago aprobado")
                .date(created.plusMinutes(10))
                .build());

        return bookingRepo.save(booking);
    }

    private void rate(Booking booking, int score, String comment) {
        ratingRepo.save(Rating.builder()
                .booking(booking)
                .score(score)
                .comment(comment)
                .date(booking.getInstance().getStartDateTime().plusDays(1))
                .build());
    }

    private Business getBusiness(String providerEmail) {
        return providerRepo.findByEmail(providerEmail).orElseThrow().getBusinesses().get(0);
    }

    private Locality locality(String name) {
        return localityRepo.findByName(name).orElseThrow();
    }

    private Equipment equipment(String name) {
        return equipmentRepo.findByName(name).orElseThrow();
    }
}
