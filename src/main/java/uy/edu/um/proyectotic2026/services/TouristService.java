package uy.edu.um.proyectotic2026.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uy.edu.um.proyectotic2026.dto.TouristRegistrationDTO;
import uy.edu.um.proyectotic2026.entities.Role;
import uy.edu.um.proyectotic2026.entities.Tourist;
import uy.edu.um.proyectotic2026.exceptions.EntityNotFoundException;
import uy.edu.um.proyectotic2026.exceptions.InvalidDataException;
import uy.edu.um.proyectotic2026.repository.TouristRepository;
import uy.edu.um.proyectotic2026.repository.UserRepository;

import java.time.LocalDate;

@Service
public class TouristService {

    @Autowired
    private UserRepository userRepo;

    @Autowired
    private TouristRepository touristRepo;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Transactional
    public Tourist registerTourist(TouristRegistrationDTO dto) throws InvalidDataException {

        // Datos obligatorios y formato (Acta 1, punto 4.1)
        validate(dto);

        // Unicidad
        String email = dto.getEmail().trim().toLowerCase();
        String document = dto.getDocument().trim();

        if (userRepo.existsByEmail(email))
            throw new InvalidDataException("Ya existe un usuario registrado con el email " + email);

        if (touristRepo.existsByDocument(document))
            throw new InvalidDataException("Ya existe un turista registrado con el documento " + document);

        Tourist tourist = Tourist.builder()
                .email(email)
                .password(passwordEncoder.encode(dto.getPassword()))
                .role(Role.TOURIST)
                .firstName(dto.getFirstName().trim())
                .lastName(dto.getLastName().trim())
                .document(document)
                .birthDate(dto.getBirthDate())
                .build();

        return touristRepo.save(tourist);
    }

    public Tourist getByEmail(String email) throws EntityNotFoundException {
        return touristRepo.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("Turista no encontrado"));
    }

    private void validate(TouristRegistrationDTO dto) throws InvalidDataException {

        if (isBlank(dto.getEmail()) || isBlank(dto.getPassword()) || isBlank(dto.getFirstName())
                || isBlank(dto.getLastName()) || isBlank(dto.getDocument()) || dto.getBirthDate() == null)
            throw new InvalidDataException("Faltan datos obligatorios");

        if (!dto.getEmail().trim().matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$"))
            throw new InvalidDataException("El email no tiene un formato válido");

        if (dto.getPassword().length() < 8)
            throw new InvalidDataException("La contraseña debe tener al menos 8 caracteres");

        if (!dto.getBirthDate().isBefore(LocalDate.now()))
            throw new InvalidDataException("La fecha de nacimiento no es válida");
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
