package uy.edu.um.proyectotic2026.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uy.edu.um.proyectotic2026.dto.BusinessDTO;
import uy.edu.um.proyectotic2026.entities.Administrator;
import uy.edu.um.proyectotic2026.entities.Business;
import uy.edu.um.proyectotic2026.entities.BusinessStatus;
import uy.edu.um.proyectotic2026.entities.Locality;
import uy.edu.um.proyectotic2026.entities.Provider;
import uy.edu.um.proyectotic2026.exceptions.EntityNotFoundException;
import uy.edu.um.proyectotic2026.exceptions.InvalidDataException;
import uy.edu.um.proyectotic2026.repository.AdministratorRepository;
import uy.edu.um.proyectotic2026.repository.BusinessRepository;
import uy.edu.um.proyectotic2026.repository.LocalityRepository;

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

    @Mock
    private LocalityRepository localityRepo;

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

    private BusinessDTO editForm() {
        BusinessDTO dto = new BusinessDTO();
        dto.setName("  Kayak Sur ");
        dto.setAddress("Rambla 123");
        dto.setLocalityId(7L);
        return dto;
    }

    @Test
    void editingAnApprovedBusinessDoesNotSendItBackToReview() throws Exception {
        business.setProvider(Provider.builder().email("kayak@demo.uy").build());
        business.setStatus(BusinessStatus.APPROVED);
        Locality locality = Locality.builder().id(7L).build();
        when(businessRepo.findById(1L)).thenReturn(Optional.of(business));
        when(localityRepo.findById(7L)).thenReturn(Optional.of(locality));

        Business result = businessService.updateBusiness(1L, editForm(), "kayak@demo.uy");

        assertEquals("Kayak Sur", result.getName());
        assertSame(locality, result.getLocality());
        assertEquals(BusinessStatus.APPROVED, result.getStatus());
    }

    @Test
    void otherProviderCannotEditTheBusiness() {
        business.setProvider(Provider.builder().email("kayak@demo.uy").build());
        when(businessRepo.findById(1L)).thenReturn(Optional.of(business));

        assertThrows(EntityNotFoundException.class,
                () -> businessService.updateBusiness(1L, editForm(), "otro@demo.uy"));
    }

    @Test
    void editingABusinessNeedsItsName() {
        BusinessDTO dto = editForm();
        dto.setName("  ");

        assertThrows(InvalidDataException.class, () -> businessService.updateBusiness(1L, dto, "kayak@demo.uy"));
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
