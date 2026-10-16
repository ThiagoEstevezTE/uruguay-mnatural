package uy.edu.um.proyectotic2026.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uy.edu.um.proyectotic2026.dto.ProviderRegistrationDTO;
import uy.edu.um.proyectotic2026.entities.*;
import uy.edu.um.proyectotic2026.exceptions.EntityNotFoundException;
import uy.edu.um.proyectotic2026.exceptions.InvalidDataException;
import uy.edu.um.proyectotic2026.repository.BusinessRepository;
import uy.edu.um.proyectotic2026.repository.LocalityRepository;
import uy.edu.um.proyectotic2026.repository.ProviderRepository;
import uy.edu.um.proyectotic2026.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ProviderService {

    @Autowired
    private UserRepository userRepo;

    @Autowired
    private ProviderRepository providerRepo;

    @Autowired
    private LocalityRepository localityRepo;

    @Autowired
    private BusinessRepository businessRepo;

    private final PasswordEncoder passwordEncoder= new BCryptPasswordEncoder();

    @Transactional
    public Provider registerProvider(ProviderRegistrationDTO dto)
        throws InvalidDataException, EntityNotFoundException {

        // Datos obligatorios y formato
        validate(dto);

        // Unicidad
        String email = dto.getEmail().trim().toLowerCase();
        String document = dto.getDocument().trim();

        if (userRepo.existsByEmail(email))
            throw new InvalidDataException("Ya existe un usuario registrado con el email " + email);

        if (providerRepo.existsByDocument(document))
            throw new InvalidDataException("Ya existe un prestador registrado con el documento " + document);

        // Localidad debe existir
        Locality locality = localityRepo.findById(dto.getLocalityId())
                .orElseThrow(() -> new EntityNotFoundException("Localidad no encontrada"));

        // Armar prestador
        Provider provider = Provider.builder()
                .email(email)
                .password(passwordEncoder.encode(dto.getPassword()))
                .role(Role.PROVIDER)
                .name(dto.getName().trim())
                .document(document)
                .phone(dto.getPhone())
                .type(dto.getType())
                .build();

        // Armar emprendimiento
        Business business = Business.builder()
                .name(dto.getBusinessName().trim())
                .description(dto.getBusinessDescription())
                .address(dto.getBusinessAddress().trim())
                .phone(dto.getBusinessPhone())
                .email(dto.getBusinessEmail())
                .locality(locality)
                .requestDate(LocalDateTime.now())
                .build();

        // conectar y guardar
        provider.addBusiness(business);
        return providerRepo.save(provider);

    }

    public List<Business> getBusinessesByProvider(Long providerId) throws EntityNotFoundException {

        if (!providerRepo.existsById(providerId))
            throw new EntityNotFoundException("Prestador no encontrado");
        return businessRepo.findByProviderId(providerId);
    }

    public Provider getByEmail(String email) throws EntityNotFoundException {
        return providerRepo.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("Prestador no encontrado"));
    }

    private void validate(ProviderRegistrationDTO dto) throws InvalidDataException {

        if (isBlank(dto.getEmail()) || isBlank(dto.getPassword()) || isBlank(dto.getName())
                || isBlank(dto.getDocument()) || dto.getType() == null)
            throw new InvalidDataException("Faltan datos obligatorios del prestador");

        if (!dto.getEmail().matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$"))
            throw new InvalidDataException("El email no tiene un formato válido");

        if (dto.getPassword().length() < 8)
            throw new InvalidDataException("La contraseña debe tener al menos 8 caracteres");

        if (isBlank(dto.getBusinessName()) || isBlank(dto.getBusinessAddress())
                || dto.getLocalityId() == null)
            throw new InvalidDataException("Faltan datos obligatorios del emprendimiento");
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
