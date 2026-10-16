package uy.edu.um.proyectotic2026.services;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import uy.edu.um.proyectotic2026.entities.Role;
import uy.edu.um.proyectotic2026.entities.Tourist;
import uy.edu.um.proyectotic2026.repository.UserRepository;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoginServiceTest {

    @Mock
    private UserRepository userRepo;

    @InjectMocks
    private LoginService loginService;

    @Test
    void loadsUserWithItsRole() {
        Tourist tourist = Tourist.builder()
                .email("turista@demo.uy")
                .password("hash")
                .role(Role.TOURIST)
                .build();
        when(userRepo.findByEmail("turista@demo.uy")).thenReturn(Optional.of(tourist));

        // El email se normaliza: mayusculas y espacios no importan
        UserDetails details = loginService.loadUserByUsername("  Turista@Demo.uy ");

        assertEquals("turista@demo.uy", details.getUsername());
        assertEquals("ROLE_TOURIST", details.getAuthorities().iterator().next().getAuthority());
        assertTrue(details.isEnabled());
    }

    @Test
    void inactiveUserIsDisabled() {
        Tourist tourist = Tourist.builder()
                .email("turista@demo.uy")
                .password("hash")
                .role(Role.TOURIST)
                .active(false)
                .build();
        when(userRepo.findByEmail("turista@demo.uy")).thenReturn(Optional.of(tourist));

        assertFalse(loginService.loadUserByUsername("turista@demo.uy").isEnabled());
    }

    @Test
    void unknownEmailThrows() {
        when(userRepo.findByEmail("nadie@demo.uy")).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class, () -> loginService.loadUserByUsername("nadie@demo.uy"));
    }
}
