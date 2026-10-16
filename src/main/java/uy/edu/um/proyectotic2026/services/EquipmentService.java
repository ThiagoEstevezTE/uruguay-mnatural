package uy.edu.um.proyectotic2026.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uy.edu.um.proyectotic2026.entities.Equipment;
import uy.edu.um.proyectotic2026.exceptions.EntityNotFoundException;
import uy.edu.um.proyectotic2026.exceptions.InvalidDataException;
import uy.edu.um.proyectotic2026.repository.ActivityEquipmentRepository;
import uy.edu.um.proyectotic2026.repository.EquipmentRepository;

import java.util.List;

// Alta, baja y modificacion de tipos de equipamiento (lo hace el administrador)
@Service
public class EquipmentService {

    @Autowired
    private EquipmentRepository equipmentRepo;

    @Autowired
    private ActivityEquipmentRepository activityEquipmentRepo;

    public List<Equipment> getAll() {
        return equipmentRepo.findAllByOrderByNameAsc();
    }

    public Equipment create(String name) throws InvalidDataException {
        String cleanName = validateName(name);
        return equipmentRepo.save(Equipment.builder().name(cleanName).build());
    }

    public Equipment update(Long id, String name) throws InvalidDataException, EntityNotFoundException {
        Equipment equipment = equipmentRepo.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Equipamiento no encontrado"));

        String cleanName = name == null ? "" : name.trim();
        if (!cleanName.equalsIgnoreCase(equipment.getName()))
            cleanName = validateName(name);

        equipment.setName(cleanName);
        return equipmentRepo.save(equipment);
    }

    public void delete(Long id) throws InvalidDataException, EntityNotFoundException {
        if (!equipmentRepo.existsById(id))
            throw new EntityNotFoundException("Equipamiento no encontrado");

        // No se puede borrar un equipamiento que ya usa alguna actividad
        if (activityEquipmentRepo.existsByEquipmentId(id))
            throw new InvalidDataException("No se puede eliminar: hay actividades que usan ese equipamiento");

        equipmentRepo.deleteById(id);
    }

    private String validateName(String name) throws InvalidDataException {
        if (name == null || name.trim().isEmpty())
            throw new InvalidDataException("El nombre es obligatorio");
        if (equipmentRepo.existsByNameIgnoreCase(name.trim()))
            throw new InvalidDataException("Ya existe un equipamiento con ese nombre");
        return name.trim();
    }
}
