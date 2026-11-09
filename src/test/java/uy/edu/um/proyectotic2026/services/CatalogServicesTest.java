package uy.edu.um.proyectotic2026.services;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import uy.edu.um.proyectotic2026.entities.ActivityInstance;
import uy.edu.um.proyectotic2026.entities.ActivityType;
import uy.edu.um.proyectotic2026.entities.CancellationReason;
import uy.edu.um.proyectotic2026.entities.Equipment;
import uy.edu.um.proyectotic2026.exceptions.InvalidDataException;
import uy.edu.um.proyectotic2026.repository.ActivityInstanceRepository;
import uy.edu.um.proyectotic2026.repository.ActivityTypeRepository;
import uy.edu.um.proyectotic2026.repository.CancellationReasonRepository;
import uy.edu.um.proyectotic2026.repository.EquipmentRepository;

import static org.junit.jupiter.api.Assertions.*;

// ABM de tipos de actividad y equipamiento que hace el administrador
@SpringBootTest
@Transactional
class CatalogServicesTest {

    @Autowired
    private ActivityTypeService typeService;

    @Autowired
    private CancellationReasonService reasonService;

    @Autowired
    private CancellationReasonRepository reasonRepo;

    @Autowired
    private ActivityInstanceRepository instanceRepo;

    @Autowired
    private EquipmentService equipmentService;

    @Autowired
    private ActivityTypeRepository typeRepo;

    @Autowired
    private EquipmentRepository equipmentRepo;

    @Test
    void createUpdateAndDeleteType() throws Exception {
        ActivityType type = typeService.create("  Pesca deportiva ");
        assertEquals("Pesca deportiva", type.getName());

        typeService.update(type.getId(), "Pesca");
        assertEquals("Pesca", typeRepo.findById(type.getId()).orElseThrow().getName());

        typeService.delete(type.getId());
        assertFalse(typeRepo.existsById(type.getId()));
    }

    @Test
    void createUpdateAndDeleteCancellationReason() throws Exception {
        CancellationReason reason = reasonService.create("  Paro de transporte ");
        assertEquals("Paro de transporte", reason.getName());

        reasonService.update(reason.getId(), "Paro");
        assertEquals("Paro", reasonRepo.findById(reason.getId()).orElseThrow().getName());

        reasonService.delete(reason.getId());
        assertFalse(reasonRepo.existsById(reason.getId()));
    }

    @Test
    void duplicatedCancellationReasonIsRejected() {
        assertThrows(InvalidDataException.class, () -> reasonService.create("mal clima"));
    }

    @Test
    void cancellationReasonInUseCannotBeDeleted() {
        CancellationReason reason = reasonRepo.findByName("Mal clima").orElseThrow();
        ActivityInstance instance = instanceRepo.findAll().get(0);
        instance.setCancellationReason(reason);
        instanceRepo.save(instance);

        assertThrows(InvalidDataException.class, () -> reasonService.delete(reason.getId()));
    }

    @Test
    void duplicatedTypeIsRejected() {
        assertThrows(InvalidDataException.class, () -> typeService.create("senderismo"));
    }

    @Test
    void typeInUseCannotBeDeleted() {
        Long id = typeRepo.findByName("Cabalgata").orElseThrow().getId();
        assertThrows(InvalidDataException.class, () -> typeService.delete(id));
    }

    @Test
    void equipmentInUseCannotBeDeleted() {
        Long id = equipmentRepo.findByName("Casco").orElseThrow().getId();
        assertThrows(InvalidDataException.class, () -> equipmentService.delete(id));
    }

    @Test
    void unusedEquipmentCanBeDeleted() throws Exception {
        Equipment item = equipmentService.create("Linterna");
        equipmentService.delete(item.getId());
        assertFalse(equipmentRepo.existsById(item.getId()));
    }

    @Test
    void blankNameIsRejected() {
        assertThrows(InvalidDataException.class, () -> equipmentService.create("   "));
    }
}
