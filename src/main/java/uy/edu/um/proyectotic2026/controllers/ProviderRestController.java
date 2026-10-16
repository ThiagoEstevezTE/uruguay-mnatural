package uy.edu.um.proyectotic2026.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import uy.edu.um.proyectotic2026.dto.BusinessResponseDTO;
import uy.edu.um.proyectotic2026.dto.ProviderRegistrationDTO;
import uy.edu.um.proyectotic2026.dto.ProviderResponseDTO;
import uy.edu.um.proyectotic2026.entities.Provider;
import uy.edu.um.proyectotic2026.exceptions.EntityNotFoundException;
import uy.edu.um.proyectotic2026.exceptions.InvalidDataException;
import uy.edu.um.proyectotic2026.services.ProviderService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/provider")
public class ProviderRestController {

    @Autowired
    private ProviderService providerService;

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody ProviderRegistrationDTO dto) {
        try {
            Provider provider = providerService.registerProvider(dto);
            return ResponseEntity.status(HttpStatus.CREATED).body(ProviderResponseDTO.from(provider));
        } catch (InvalidDataException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    @GetMapping("/{id}/businesses")
    public ResponseEntity<?> getBusinesses(@PathVariable("id") Long id) {
        try {
            List<BusinessResponseDTO> businesses = providerService.getBusinessesByProvider(id).stream()
                    .map(BusinessResponseDTO::from)
                    .toList();
            return ResponseEntity.ok(businesses);
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }
}
