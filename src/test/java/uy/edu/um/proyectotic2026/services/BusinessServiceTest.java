package uy.edu.um.proyectotic2026.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uy.edu.um.proyectotic2026.entities.Administrator;
import uy.edu.um.proyectotic2026.entities.Business;
import uy.edu.um.proyectotic2026.entities.BusinessStatus;
import uy.edu.um.proyectotic2026.exceptions.EntityNotFoundException;
import uy.edu.um.proyectotic2026.exceptions.InvalidDataException;
import uy.edu.um.proyectotic2026.repository.AdministratorRepository;
import uy.edu.um.proyectotic2026.repository.BusinessRepository;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BusinessServiceTest {

    @Mock
    private BusinessRepository businessRepo;

    @Mock
    private AdministratorRepository adminRepo;

    @InjectMocks
    private BusinessService businessService;

    private Business business;
    private Administrator admin;

    @BeforeEach
    void setUp() {
        business = Business.builder().id(1L).name("Kayak").requestDate(LocalDateTime.now()).build();
        admin = Administrator.builder().email("admin@uymnatural.uy").name("Admin").build();
        lenient().when(adminRepo.findByEmail("admin@uymnatural.uy")).thenReturn(Optional.of(admin));
        lenient().when(businessRepo.save(any(Business.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void approveChangesStatusAndSavesWhoResolved() throws Exception {
        when(businessRepo.findById(1L)).thenReturn(Optional.of(business));

        Business result = businessService.approveBusiness(1L, "admin@uymnatural.uy");

        assertEquals(BusinessStatus.APPROVED, result.getStatus());
        assertEquals(admin, result.getResolvedBy());
        assertNotNull(result.getResolutionDate());
    }

    @Test
    void rejectSavesTheReason() throws Exception {
        when(businessRepo.findById(1L)).thenReturn(Optional.of(business));

        Business result = businessService.rejectBusiness(1L, "admin@uymnatural.uy", " Faltan datos ");

        assertEquals(BusinessStatus.REJECTED, result.getStatus());
        assertEquals("Faltan datos", result.getRejectionReason());
    }

    @Test
    void rejectWithoutReasonFails() {
        assertThrows(InvalidDataException.class,
                () -> businessService.rejectBusiness(1L, "admin@uymnatural.uy", " "));
    }

    @Test
    void cannotResolveTwice() {
        business.setStatus(BusinessStatus.APPROVED);
        when(businessRepo.findById(1L)).thenReturn(Optional.of(business));

        assertThrows(InvalidDataException.class, () -> businessService.approveBusiness(1L, "admin@uymnatural.uy"));
    }

    @Test
    void unknownBusinessFails() {
        when(businessRepo.findById(99L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> businessService.approveBusiness(99L, "admin@uymnatural.uy"));
    }
}
