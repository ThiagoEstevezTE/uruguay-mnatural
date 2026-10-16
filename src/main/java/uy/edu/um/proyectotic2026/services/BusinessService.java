package uy.edu.um.proyectotic2026.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uy.edu.um.proyectotic2026.entities.Administrator;
import uy.edu.um.proyectotic2026.entities.Business;
import uy.edu.um.proyectotic2026.entities.BusinessStatus;
import uy.edu.um.proyectotic2026.exceptions.EntityNotFoundException;
import uy.edu.um.proyectotic2026.exceptions.InvalidDataException;
import uy.edu.um.proyectotic2026.repository.AdministratorRepository;
import uy.edu.um.proyectotic2026.repository.BusinessRepository;

import java.time.LocalDateTime;
import java.util.List;

// Bandeja de solicitudes del administrador (Acta 1, punto 4.2)
@Service
public class BusinessService {

    @Autowired
    private BusinessRepository businessRepo;

    @Autowired
    private AdministratorRepository adminRepo;

    public List<Business> getPendingBusinesses() {
        return businessRepo.findByStatusOrderByRequestDateAsc(BusinessStatus.PENDING);
    }

    public List<Business> getAllBusinesses() {
        return businessRepo.findAllByOrderByRequestDateDesc();
    }

    public Business getById(Long id) throws EntityNotFoundException {
        return businessRepo.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Emprendimiento no encontrado"));
    }

    @Transactional
    public Business approveBusiness(Long businessId, String adminEmail)
            throws EntityNotFoundException, InvalidDataException {

        Business business = getPendingBusiness(businessId);

        business.setStatus(BusinessStatus.APPROVED);
        business.setResolutionDate(LocalDateTime.now());
        business.setResolvedBy(getAdmin(adminEmail));
        business.setRejectionReason(null);
        return businessRepo.save(business);
    }

    @Transactional
    public Business rejectBusiness(Long businessId, String adminEmail, String reason)
            throws EntityNotFoundException, InvalidDataException {

        // Al rechazar hay que decirle al prestador por que
        if (reason == null || reason.trim().isEmpty())
            throw new InvalidDataException("Para rechazar una solicitud hay que indicar el motivo");

        Business business = getPendingBusiness(businessId);

        business.setStatus(BusinessStatus.REJECTED);
        business.setResolutionDate(LocalDateTime.now());
        business.setResolvedBy(getAdmin(adminEmail));
        business.setRejectionReason(reason.trim());
        return businessRepo.save(business);
    }

    // Solo se puede resolver una solicitud que todavia esta pendiente
    private Business getPendingBusiness(Long businessId) throws EntityNotFoundException, InvalidDataException {
        Business business = getById(businessId);
        if (business.getStatus() != BusinessStatus.PENDING)
            throw new InvalidDataException("La solicitud ya fue resuelta");
        return business;
    }

    private Administrator getAdmin(String email) throws EntityNotFoundException {
        return adminRepo.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("Administrador no encontrado"));
    }
}
