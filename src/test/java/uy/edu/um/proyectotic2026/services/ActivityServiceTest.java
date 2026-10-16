package uy.edu.um.proyectotic2026.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import uy.edu.um.proyectotic2026.dto.ActivityDTO;
import uy.edu.um.proyectotic2026.entities.*;
import uy.edu.um.proyectotic2026.exceptions.EntityNotFoundException;
import uy.edu.um.proyectotic2026.exceptions.InvalidDataException;
import uy.edu.um.proyectotic2026.repository.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

// Test de integracion: usa la base H2 con los datos de ejemplo.
// @Transactional deshace todo lo que hace cada test, asi no se pisan entre si.
@SpringBootTest
@Transactional
class ActivityServiceTest {

    private static final String PROVIDER = "prestador@demo.uy";
    private static final String PENDING_PROVIDER = "pendiente@demo.uy";

    @Autowired
    private ActivityService activityService;

    @Autowired
    private ProviderRepository providerRepo;

    @Autowired
    private ActivityTypeRepository typeRepo;

    @Autowired
    private EquipmentRepository equipmentRepo;

    private ActivityDTO dto;

    @BeforeEach
    void setUp() {
        Business business = providerRepo.findByEmail(PROVIDER).orElseThrow().getBusinesses().get(0);

        dto = new ActivityDTO();
        dto.setBusinessId(business.getId());
        dto.setLocalityId(business.getLocality().getId());
        dto.setName("Paseo en carreta");
        dto.setDescription("Paseo por el pueblo");
        dto.setDurationMinutes(60);
        dto.setPrice(new BigDecimal("700"));
        dto.setTypeIds(List.of(typeRepo.findByName("Turismo de naturaleza").orElseThrow().getId()));
        dto.setProvidedEquipmentIds(List.of(equipmentRepo.findByName("Agua").orElseThrow().getId()));
    }

    @Test
    void approvedProviderCreatesActivity() throws Exception {
        Activity activity = activityService.createActivity(dto, PROVIDER);

        assertNotNull(activity.getId());
        assertEquals(1, activity.getTypes().size());
        assertTrue(activity.getEquipment().get(0).isProvidedByProvider());
        assertTrue(activity.isPublished());
    }

    @Test
    void pendingBusinessCannotPublish() {
        Business pending = providerRepo.findByEmail(PENDING_PROVIDER).orElseThrow().getBusinesses().get(0);
        dto.setBusinessId(pending.getId());

        InvalidDataException e = assertThrows(InvalidDataException.class,
                () -> activityService.createActivity(dto, PENDING_PROVIDER));
        assertTrue(e.getMessage().contains("no fue aprobado"));
    }

    @Test
    void cannotUseBusinessOfAnotherProvider() {
        assertThrows(EntityNotFoundException.class, () -> activityService.createActivity(dto, "sierras@demo.uy"));
    }

    @Test
    void typeIsRequired() {
        dto.setTypeIds(List.of());
        assertThrows(InvalidDataException.class, () -> activityService.createActivity(dto, PROVIDER));
    }

    @Test
    void negativePriceIsRejected() {
        dto.setPrice(new BigDecimal("-1"));
        assertThrows(InvalidDataException.class, () -> activityService.createActivity(dto, PROVIDER));
    }

    @Test
    void cancellationNeedsHours() {
        dto.setAllowsCancellation(true);
        dto.setCancellationMinHours(null);
        assertThrows(InvalidDataException.class, () -> activityService.createActivity(dto, PROVIDER));
    }

    @Test
    void updateReplacesEquipment() throws Exception {
        Activity activity = activityService.createActivity(dto, PROVIDER);
        dto.setProvidedEquipmentIds(List.of());
        dto.setRequiredEquipmentIds(List.of(equipmentRepo.findByName("Casco").orElseThrow().getId()));

        Activity updated = activityService.updateActivity(activity.getId(), dto, PROVIDER);

        assertEquals(1, updated.getEquipment().size());
        assertFalse(updated.getEquipment().get(0).isProvidedByProvider());
    }

    @Test
    void addsFutureInstance() throws Exception {
        Activity activity = activityService.createActivity(dto, PROVIDER);

        ActivityInstance instance = activityService.addInstance(activity.getId(),
                LocalDateTime.now().plusDays(3), 10, PROVIDER);

        assertEquals(InstanceStatus.SCHEDULED, instance.getStatus());
        assertEquals(10, instance.getMaxCapacity());
    }

    @Test
    void instanceInThePastIsRejected() throws Exception {
        Activity activity = activityService.createActivity(dto, PROVIDER);

        assertThrows(InvalidDataException.class, () -> activityService.addInstance(activity.getId(),
                LocalDateTime.now().minusHours(1), null, PROVIDER));
    }

    @Test
    void zeroCapacityIsRejected() throws Exception {
        Activity activity = activityService.createActivity(dto, PROVIDER);

        assertThrows(InvalidDataException.class, () -> activityService.addInstance(activity.getId(),
                LocalDateTime.now().plusDays(1), 0, PROVIDER));
    }

    @Test
    void otherProviderCannotSeeActivity() throws Exception {
        Activity activity = activityService.createActivity(dto, PROVIDER);

        assertThrows(EntityNotFoundException.class,
                () -> activityService.getActivityOfProvider(activity.getId(), "kayak@demo.uy"));
    }
}
