package uy.edu.um.proyectotic2026.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import uy.edu.um.proyectotic2026.entities.*;
import uy.edu.um.proyectotic2026.repository.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

// Carga datos de ejemplo al arrancar la app, para poder mostrar el sistema funcionando.
// Las cuentas de prueba estan en el README.
@Component
public class DataLoader implements CommandLineRunner {

    public static final String DEMO_PASSWORD = "demo1234";

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
    private PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {

        // Si ya hay usuarios, la base ya tiene datos: no se carga nada de nuevo
        if (userRepo.count() > 0)
            return;

        String password = passwordEncoder.encode(DEMO_PASSWORD);

        Administrator admin = adminRepo.save(Administrator.builder()
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

        // Un prestador aprobado (ya puede publicar) y uno pendiente (para mostrar la bandeja del admin)
        Provider approved = createProvider("prestador@demo.uy", "Martín Silva", "31234567", ProviderType.INDIVIDUAL,
                "Cabalgatas del Polonio", "Cabalgatas por la costa rochense, entre dunas y lobos marinos.",
                "Ruta 10 km 264", "Cabo Polonio", password);
        Business business = approved.getBusinesses().get(0);
        business.setStatus(BusinessStatus.APPROVED);
        business.setResolutionDate(LocalDateTime.now());
        business.setResolvedBy(admin);
        providerRepo.save(approved);

        createProvider("pendiente@demo.uy", "Bodega Los Cerros SRL", "212345670018", ProviderType.COMPANY,
                "Bodega Los Cerros", "Visitas guiadas y degustación de vinos tannat en Carmelo.",
                "Camino de los Peregrinos 1450", "Carmelo", password);
    }

    private Provider createProvider(String email, String name, String document, ProviderType type,
                                    String businessName, String description, String address,
                                    String localityName, String password) {

        Locality locality = localityRepo.findByName(localityName).orElseThrow();

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
                .locality(locality)
                .requestDate(LocalDateTime.now().minusDays(2))
                .build();

        provider.addBusiness(business);
        return providerRepo.save(provider);
    }
}
