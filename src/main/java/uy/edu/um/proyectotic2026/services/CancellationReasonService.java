package uy.edu.um.proyectotic2026.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uy.edu.um.proyectotic2026.entities.CancellationReason;
import uy.edu.um.proyectotic2026.exceptions.EntityNotFoundException;
import uy.edu.um.proyectotic2026.exceptions.InvalidDataException;
import uy.edu.um.proyectotic2026.repository.ActivityInstanceRepository;
import uy.edu.um.proyectotic2026.repository.CancellationReasonRepository;

import java.util.List;

// Alta, baja y modificacion de motivos de cancelacion (lo hace el administrador)
@Service
public class CancellationReasonService {

    @Autowired
    private CancellationReasonRepository reasonRepo;

    @Autowired
    private ActivityInstanceRepository instanceRepo;

    public List<CancellationReason> getAll() {
        return reasonRepo.findAllByOrderByNameAsc();
    }

    public CancellationReason create(String name) throws InvalidDataException {
        String cleanName = validateName(name);
        return reasonRepo.save(CancellationReason.builder().name(cleanName).build());
    }

    public CancellationReason update(Long id, String name) throws InvalidDataException, EntityNotFoundException {
        CancellationReason reason = reasonRepo.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Motivo de cancelación no encontrado"));

        String cleanName = name == null ? "" : name.trim();
        if (!cleanName.equalsIgnoreCase(reason.getName()))
            cleanName = validateName(name);

        reason.setName(cleanName);
        return reasonRepo.save(reason);
    }

    public void delete(Long id) throws InvalidDataException, EntityNotFoundException {
        if (!reasonRepo.existsById(id))
            throw new EntityNotFoundException("Motivo de cancelación no encontrado");

        // No se puede borrar un motivo que ya se uso: se perderia por que se cancelo esa fecha
        if (instanceRepo.existsByCancellationReasonId(id))
            throw new InvalidDataException("No se puede eliminar: hay fechas canceladas con ese motivo");

        reasonRepo.deleteById(id);
    }

    private String validateName(String name) throws InvalidDataException {
        if (name == null || name.trim().isEmpty())
            throw new InvalidDataException("El nombre es obligatorio");
        if (reasonRepo.existsByNameIgnoreCase(name.trim()))
            throw new InvalidDataException("Ya existe un motivo con ese nombre");
        return name.trim();
    }
}
