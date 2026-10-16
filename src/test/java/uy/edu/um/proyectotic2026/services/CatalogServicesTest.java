package uy.edu.um.proyectotic2026.services;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import uy.edu.um.proyectotic2026.entities.ActivityType;
import uy.edu.um.proyectotic2026.entities.Equipment;
import uy.edu.um.proyectotic2026.exceptions.InvalidDataException;
import uy.edu.um.proyectotic2026.repository.ActivityTypeRepository;
import uy.edu.um.proyectotic2026.repository.EquipmentRepository;

import static org.junit.jupiter.api.Assertions.*;

// ABM de tipos de actividad y equipamiento que hace el administrador
@SpringBootTest
@Transactional
class CatalogServicesTest {

    @Autowired
    private ActivityTypeService typeService;

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
