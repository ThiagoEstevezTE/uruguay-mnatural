package uy.edu.um.proyectotic2026.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import uy.edu.um.proyectotic2026.entities.Provider;
import uy.edu.um.proyectotic2026.exceptions.EntityNotFoundException;
import uy.edu.um.proyectotic2026.services.ProviderService;

import java.security.Principal;

// Espacio de gestion del prestador
@Controller
@RequestMapping("/provider")
public class ProviderWebController {

    @Autowired
    private ProviderService providerService;

    @GetMapping
    public String home(Model model, Principal principal) throws EntityNotFoundException {
        // principal.getName() es el email de quien inicio sesion
        Provider provider = providerService.getByEmail(principal.getName());
        model.addAttribute("provider", provider);
        model.addAttribute("businesses", provider.getBusinesses());
        return "provider/home";
    }
}
