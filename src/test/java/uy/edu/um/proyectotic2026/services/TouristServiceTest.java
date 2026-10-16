package uy.edu.um.proyectotic2026.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uy.edu.um.proyectotic2026.dto.TouristRegistrationDTO;
import uy.edu.um.proyectotic2026.entities.Role;
import uy.edu.um.proyectotic2026.entities.Tourist;
import uy.edu.um.proyectotic2026.exceptions.InvalidDataException;
import uy.edu.um.proyectotic2026.repository.TouristRepository;
import uy.edu.um.proyectotic2026.repository.UserRepository;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TouristServiceTest {

    @Mock
    private UserRepository userRepo;

    @Mock
    private TouristRepository touristRepo;

    @InjectMocks
    private TouristService touristService;

    private TouristRegistrationDTO dto;

    @BeforeEach
    void setUp() {
        dto = new TouristRegistrationDTO();
        dto.setEmail("Juan@Mail.com ");
        dto.setPassword("secreta123");
        dto.setFirstName("Juan");
        dto.setLastName("Pérez");
        dto.setDocument(" 12345678");
        dto.setBirthDate(LocalDate.of(2000, 1, 10));
    }

    @Test
    void registersTouristWithEncryptedPassword() throws Exception {
        when(touristRepo.save(any(Tourist.class))).thenAnswer(inv -> inv.getArgument(0));

        Tourist tourist = touristService.registerTourist(dto);

        assertEquals("juan@mail.com", tourist.getEmail());
        assertEquals("12345678", tourist.getDocument());
        assertEquals(Role.TOURIST, tourist.getRole());
        assertTrue(tourist.getPassword().startsWith("$2"));   // hash BCrypt, no la contraseña en texto
    }

    @Test
    void rejectsDuplicatedEmail() {
        when(userRepo.existsByEmail("juan@mail.com")).thenReturn(true);

        assertThrows(InvalidDataException.class, () -> touristService.registerTourist(dto));
        verify(touristRepo, never()).save(any());
    }

    @Test
    void rejectsDuplicatedDocument() {
        when(touristRepo.existsByDocument("12345678")).thenReturn(true);

        assertThrows(InvalidDataException.class, () -> touristService.registerTourist(dto));
    }

    @Test
    void rejectsMissingData() {
        dto.setLastName("  ");
        assertThrows(InvalidDataException.class, () -> touristService.registerTourist(dto));
    }

    @Test
    void rejectsShortPassword() {
        dto.setPassword("1234");
        assertThrows(InvalidDataException.class, () -> touristService.registerTourist(dto));
    }

    @Test
    void rejectsBirthDateInTheFuture() {
        dto.setBirthDate(LocalDate.now().plusDays(1));
        assertThrows(InvalidDataException.class, () -> touristService.registerTourist(dto));
    }
}
