package uy.edu.um.proyectotic2026.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import uy.edu.um.proyectotic2026.entities.Business;
import uy.edu.um.proyectotic2026.entities.Provider;
import uy.edu.um.proyectotic2026.repository.BusinessRepository;
import uy.edu.um.proyectotic2026.repository.ProviderRepository;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

// Cada prestador puede tener un solo emprendimiento
@SpringBootTest
class OneBusinessPerProviderTest {

    @Autowired
    private ProviderRepository providerRepo;

    @Autowired
    private BusinessRepository businessRepo;

    @Test
    void theProviderCannotAddASecondBusiness() {
        Provider provider = Provider.builder().build();
        provider.addBusiness(Business.builder().name("Uno").build());

        assertThrows(IllegalStateException.class,
                () -> provider.addBusiness(Business.builder().name("Dos").build()));
        assertEquals(1, provider.getBusinesses().size());
    }

    @Test
    void theDatabaseRejectsASecondBusiness() {
        Provider provider = providerRepo.findAll().getFirst();
        Business first = businessRepo.findByProviderId(provider.getId()).getFirst();

        Business second = Business.builder()
                .provider(provider)
                .locality(first.getLocality())
                .name("Otro emprendimiento")
                .requestDate(LocalDateTime.now())
                .build();

        assertThrows(DataIntegrityViolationException.class, () -> businessRepo.saveAndFlush(second));
    }
}
