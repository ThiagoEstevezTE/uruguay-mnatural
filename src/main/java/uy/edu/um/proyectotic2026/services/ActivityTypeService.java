package uy.edu.um.proyectotic2026.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uy.edu.um.proyectotic2026.entities.ActivityType;
import uy.edu.um.proyectotic2026.exceptions.EntityNotFoundException;
import uy.edu.um.proyectotic2026.exceptions.InvalidDataException;
import uy.edu.um.proyectotic2026.repository.ActivityRepository;
import uy.edu.um.proyectotic2026.repository.ActivityTypeRepository;

import java.util.List;

// Alta, baja y modificacion de tipos de actividad (lo hace el administrador)
@Service
public class ActivityTypeService {

    @Autowired
    private ActivityTypeRepository typeRepo;

    @Autowired
    private ActivityRepository activityRepo;

    public List<ActivityType> getAll() {
        return typeRepo.findAllByOrderByNameAsc();
    }

    public ActivityType create(String name) throws InvalidDataException {
        String cleanName = validateName(name);
        return typeRepo.save(ActivityType.builder().name(cleanName).build());
    }

    public ActivityType update(Long id, String name) throws InvalidDataException, EntityNotFoundException {
        ActivityType type = typeRepo.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Tipo de actividad no encontrado"));

        String cleanName = name == null ? "" : name.trim();
        if (!cleanName.equalsIgnoreCase(type.getName()))
            cleanName = validateName(name);

        type.setName(cleanName);
        return typeRepo.save(type);
    }

    public void delete(Long id) throws InvalidDataException, EntityNotFoundException {
        if (!typeRepo.existsById(id))
            throw new EntityNotFoundException("Tipo de actividad no encontrado");

        // No se puede borrar un tipo que ya usa alguna actividad
        if (activityRepo.existsByTypesId(id))
            throw new InvalidDataException("No se puede eliminar: hay actividades con ese tipo");

        typeRepo.deleteById(id);
    }

    private String validateName(String name) throws InvalidDataException {
        if (name == null || name.trim().isEmpty())
            throw new InvalidDataException("El nombre es obligatorio");
        if (typeRepo.existsByNameIgnoreCase(name.trim()))
            throw new InvalidDataException("Ya existe un tipo de actividad con ese nombre");
        return name.trim();
    }
}
